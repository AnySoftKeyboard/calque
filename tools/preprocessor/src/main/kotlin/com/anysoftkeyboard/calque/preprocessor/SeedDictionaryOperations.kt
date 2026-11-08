package com.anysoftkeyboard.calque.preprocessor

// Dictionary-table operations of [SeedDatabase]. They are extensions in their own file to keep the
// class small; they only need the connection and the shared transaction and binding helpers.
//
// A spelling can have several readings (`padre` is a noun and an informal adjective, `fue` is a
// form of ser and of ir). The table keeps all of them, keyed by (surface_form, lemma, pos), and
// `reading_order` numbers them per spelling: 0 is the primary reading, 1 and up are alternatives.

private const val PARAM_SURFACE = 1
private const val PARAM_LEMMA = 2
private const val PARAM_POS = 3
private const val PARAM_SEQ = 4

// Placeholder positions of the reading lookup: surface, pos, lemma (null check), lemma.
private const val FIND_SURFACE = 1
private const val FIND_POS = 2
private const val FIND_LEMMA_NULL_CHECK = 3
private const val FIND_LEMMA = 4
private const val RESULT_LEMMA = 1
private const val RESULT_POS = 2

/** Curated dictionary-seed rows are numbered after the reading overrides, so overrides win. */
private const val CURATED_SEED_OFFSET = 1_000_000

internal const val DICTIONARY_COLUMNS =
    "surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank"

internal const val REPLACE_DICTIONARY_SQL =
    "INSERT OR REPLACE INTO dictionary ($DICTIONARY_COLUMNS, reading_order) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, 0)"

// MAX_RANK stands in for a missing frequency_rank so that a missing rank loses to any real rank.
private const val MAX_RANK = Int.MAX_VALUE

// Row priority: a lemma row (2) beats an inflected row (1), which beats a proper noun (0). A NULL
// grammar_info falls to the ELSE branch.
private fun priority(table: String) =
    "CASE $table.grammar_info WHEN '${DictionaryGrammar.BASE}' THEN 2 " +
        "WHEN '${DictionaryGrammar.PROPER_NOUN}' THEN 0 ELSE 1 END"

/**
 * The order of the readings of one spelling: the stored `reading_order` first (set by
 * [finalizeReadings]); before that, and for ties, a lemma row beats an inflected row, a better
 * frequency rank beats a worse one (a missing rank is worst), and the rest is decided by part of
 * speech and lemma so that the result never depends on the order rows were inserted in.
 */
internal val READING_ORDER_SQL =
    "reading_order, ${priority("dictionary")} DESC, frequency_rank IS NULL, frequency_rank, " +
        "pos, lemma"

private val KEEP_BEST_DICTIONARY_SQL =
    "INSERT INTO dictionary ($DICTIONARY_COLUMNS, reading_order) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, 0) " +
        "ON CONFLICT(surface_form, lemma, pos) DO UPDATE SET " +
        "grammar_info = excluded.grammar_info, english_gloss = excluded.english_gloss, " +
        "is_cognate = excluded.is_cognate, frequency_rank = excluded.frequency_rank " +
        "WHERE (${priority("excluded")}) > (${priority("dictionary")}) " +
        "OR ((${priority("excluded")}) = (${priority("dictionary")}) " +
        "AND COALESCE(excluded.frequency_rank, $MAX_RANK) < " +
        "COALESCE(dictionary.frequency_rank, $MAX_RANK))"

private const val CREATE_CURATED_SQL =
    "CREATE TEMP TABLE IF NOT EXISTS curated_readings (" +
        "surface_form TEXT NOT NULL, lemma TEXT NOT NULL, pos TEXT NOT NULL, " +
        "seq INTEGER NOT NULL, PRIMARY KEY (surface_form, lemma, pos))"

private const val UPSERT_CURATED_SQL =
    "INSERT OR REPLACE INTO curated_readings (surface_form, lemma, pos, seq) VALUES (?, ?, ?, ?)"

/** Runs [sql] once per entry inside a single transaction. */
internal fun SeedDatabase.executeDictionaryBatch(entries: Collection<DictionaryRow>, sql: String) {
    if (entries.isEmpty()) return

    DatabaseBinders.executeTransaction(connection) {
        connection.prepareStatement(sql).use { pstmt ->
            for (entry in entries) {
                DatabaseBinders.bindDictionaryRow(pstmt, entry)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
    }
}

/**
 * Inserts a batch of dictionary rows. Rows for the same spelling but a different lemma or part of
 * speech are different readings and are all kept. Two rows for the same (spelling, lemma, part of
 * speech) compete and the better one stays:
 * 1. a lemma row (`grammar_info` = [DictionaryGrammar.BASE]) beats an inflected row, which beats a
 *    proper noun ([DictionaryGrammar.PROPER_NOUN]);
 * 2. otherwise the lower `frequency_rank` wins, and a missing rank loses to any rank;
 * 3. on a tie the row inserted first stays.
 */
fun SeedDatabase.insertDictionaryBatchKeepingBest(entries: Collection<DictionaryRow>) {
    executeDictionaryBatch(entries, KEEP_BEST_DICTIONARY_SQL)
}

/**
 * Inserts hand-written rows that must win. A row replaces the stored row with the same key and
 * becomes a primary reading of its spelling (several curated rows keep their file order); the other
 * stored readings stay as alternatives. Needs [finalizeReadings] afterwards.
 */
fun SeedDatabase.insertCuratedDictionaryBatch(entries: Collection<DictionaryRow>) {
    if (entries.isEmpty()) return
    DatabaseBinders.executeTransaction(connection) {
        connection.createStatement().use { it.execute(CREATE_CURATED_SQL) }
        connection.prepareStatement(REPLACE_DICTIONARY_SQL).use { insert ->
            connection.prepareStatement(UPSERT_CURATED_SQL).use { curated ->
                entries.forEachIndexed { index, row ->
                    DatabaseBinders.bindDictionaryRow(insert, row)
                    insert.executeUpdate()
                    val seq = CURATED_SEED_OFFSET + index
                    bindCurated(curated, row.surfaceForm, row.lemma, row.pos, seq)
                    curated.executeUpdate()
                }
            }
        }
    }
}

/** A statement about which reading of [surfaceForm] should be primary: its part of speech. */
data class ReadingPreference(val surfaceForm: String, val pos: String, val lemma: String? = null)

/**
 * Makes the stored reading described by each of [preferences] the primary reading of its spelling,
 * in list order, ahead of the dictionary seed. Returns the preferences that matched no stored
 * reading. Needs [finalizeReadings] afterwards.
 */
fun SeedDatabase.promoteReadings(preferences: List<ReadingPreference>): List<ReadingPreference> {
    val unmatched = mutableListOf<ReadingPreference>()
    DatabaseBinders.executeTransaction(connection) {
        connection.createStatement().use { it.execute(CREATE_CURATED_SQL) }
        val find =
            "SELECT lemma, pos FROM dictionary WHERE surface_form = ? AND pos = ? " +
                "AND (? IS NULL OR lemma = ?) ORDER BY $READING_ORDER_SQL LIMIT 1"
        connection.prepareStatement(find).use { select ->
            connection.prepareStatement(UPSERT_CURATED_SQL).use { curated ->
                preferences.forEachIndexed { index, preference ->
                    val match = findReading(select, preference)
                    if (match == null) {
                        unmatched.add(preference)
                    } else {
                        bindCurated(
                            curated,
                            preference.surfaceForm,
                            match.first,
                            match.second,
                            index
                        )
                        curated.executeUpdate()
                    }
                }
            }
        }
    }
    return unmatched
}

private fun findReading(
    select: java.sql.PreparedStatement,
    preference: ReadingPreference,
): Pair<String, String>? {
    select.setString(FIND_SURFACE, preference.surfaceForm)
    select.setString(FIND_POS, preference.pos)
    select.setString(FIND_LEMMA_NULL_CHECK, preference.lemma)
    select.setString(FIND_LEMMA, preference.lemma)
    select.executeQuery().use { rs ->
        return if (rs.next()) rs.getString(RESULT_LEMMA) to rs.getString(RESULT_POS) else null
    }
}

private fun bindCurated(
    statement: java.sql.PreparedStatement,
    surface: String,
    lemma: String,
    pos: String,
    seq: Int,
) {
    statement.setString(PARAM_SURFACE, surface)
    statement.setString(PARAM_LEMMA, lemma)
    statement.setString(PARAM_POS, pos)
    statement.setInt(PARAM_SEQ, seq)
}

private const val DELETE_COMPETING_PROPER_NOUNS_SQL =
    "DELETE FROM dictionary WHERE grammar_info = '${DictionaryGrammar.PROPER_NOUN}' " +
        "AND EXISTS (SELECT 1 FROM dictionary other WHERE other.surface_form = " +
        "dictionary.surface_form AND other.grammar_info IS NOT '${DictionaryGrammar.PROPER_NOUN}')"

// Kaikki lists the other gender of a word among its forms (`madre` lists `padre`, `aceituno` lists
// `aceituna`). When the spelling is a lemma in its own right, that reading is noise next to it.
private const val DELETE_GENDER_COUNTERPARTS_SQL =
    "DELETE FROM dictionary WHERE surface_form <> lemma " +
        "AND grammar_info IN ('masculine', 'feminine') " +
        "AND EXISTS (SELECT 1 FROM dictionary own WHERE own.surface_form = " +
        "dictionary.surface_form AND own.grammar_info = '${DictionaryGrammar.BASE}')"

// Numbers the readings of each spelling: curated ones first (in the order they were curated), then
// by the same rule READING_ORDER_SQL uses.
private val NUMBER_READINGS_SQL =
    "UPDATE dictionary SET reading_order = ranked.n FROM (" +
        "SELECT d.surface_form AS s, d.lemma AS l, d.pos AS p, " +
        "ROW_NUMBER() OVER (PARTITION BY d.surface_form ORDER BY c.seq IS NULL, c.seq, " +
        "${priority("d")} DESC, d.frequency_rank IS NULL, d.frequency_rank, d.pos, d.lemma) - 1 " +
        "AS n FROM dictionary d LEFT JOIN curated_readings c " +
        "ON c.surface_form = d.surface_form AND c.lemma = d.lemma AND c.pos = d.pos) AS ranked " +
        "WHERE dictionary.surface_form = ranked.s AND dictionary.lemma = ranked.l " +
        "AND dictionary.pos = ranked.p"

/**
 * Finishes the dictionary once every source has been inserted: proper nouns that share a spelling
 * with a common word are dropped (`Amigo`, a surname, is noise next to `amigo`), so are readings
 * that only say "the masculine / feminine of another lemma" for a spelling that is a lemma itself
 * (`padre` as a form of `madre`), then the readings of every spelling are numbered, curated
 * readings first. Safe to call more than once.
 */
fun SeedDatabase.finalizeReadings() {
    DatabaseBinders.executeTransaction(connection) {
        connection.createStatement().use { statement ->
            statement.execute(CREATE_CURATED_SQL)
            statement.executeUpdate(DELETE_COMPETING_PROPER_NOUNS_SQL)
            statement.executeUpdate(DELETE_GENDER_COUNTERPARTS_SQL)
            statement.executeUpdate(NUMBER_READINGS_SQL)
        }
    }
}
