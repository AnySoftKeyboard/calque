package com.anysoftkeyboard.calque.preprocessor

import java.sql.ResultSet

// Dictionary-table operations of [SeedDatabase]. They are extensions in their own file to keep the
// class small; they only need the connection and the shared transaction and binding helpers.

private const val PARAM_SURFACE = 1

internal const val DICTIONARY_COLUMNS =
    "surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank"

internal const val REPLACE_DICTIONARY_SQL =
    "INSERT OR REPLACE INTO dictionary ($DICTIONARY_COLUMNS) VALUES (?, ?, ?, ?, ?, ?, ?)"

// SQLite's IS yields 0 or 1 even for a NULL grammar_info. MAX_RANK stands in for a missing
// frequency_rank so that a missing rank loses to any real rank.
private const val MAX_RANK = Int.MAX_VALUE
private const val IS_BASE = "grammar_info IS '${DictionaryGrammar.BASE}'"

private const val KEEP_BEST_DICTIONARY_SQL =
    "INSERT INTO dictionary ($DICTIONARY_COLUMNS) VALUES (?, ?, ?, ?, ?, ?, ?) " +
        "ON CONFLICT(surface_form) DO UPDATE SET lemma = excluded.lemma, pos = excluded.pos, " +
        "grammar_info = excluded.grammar_info, english_gloss = excluded.english_gloss, " +
        "is_cognate = excluded.is_cognate, frequency_rank = excluded.frequency_rank " +
        "WHERE (excluded.$IS_BASE) > (dictionary.$IS_BASE) " +
        "OR ((excluded.$IS_BASE) = (dictionary.$IS_BASE) " +
        "AND COALESCE(excluded.frequency_rank, $MAX_RANK) < " +
        "COALESCE(dictionary.frequency_rank, $MAX_RANK))"

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
 * Inserts a batch of dictionary rows, keeping the better row when a surface form already exists
 * (also within the batch). `surface_form` is the primary key, so homographs such as `fue` (ser /
 * ir) or `como` (adverb / verb of comer) compete for one row; the rule is:
 * 1. a base lemma row (`grammar_info` = [DictionaryGrammar.BASE]) beats an inflected one;
 * 2. otherwise the lower `frequency_rank` wins, and a missing rank loses to any rank;
 * 3. on a tie the row inserted first stays, so the result follows the input order.
 */
fun SeedDatabase.insertDictionaryBatchKeepingBest(entries: Collection<DictionaryRow>) {
    executeDictionaryBatch(entries, KEEP_BEST_DICTIONARY_SQL)
}

/** Returns the full dictionary row stored for [surfaceForm], or null when there is none. */
fun SeedDatabase.queryDictionaryRow(surfaceForm: String): DictionaryRow? {
    val sql = "SELECT $DICTIONARY_COLUMNS FROM dictionary WHERE surface_form = ?"
    connection.prepareStatement(sql).use { pstmt ->
        pstmt.setString(PARAM_SURFACE, surfaceForm)
        pstmt.executeQuery().use { rs ->
            return if (rs.next()) mapDictionaryRow(rs) else null
        }
    }
}

/** Returns the number of rows in the dictionary table. */
fun SeedDatabase.countDictionaryRows(): Int =
    connection.createStatement().use { stmt ->
        stmt.executeQuery("SELECT COUNT(*) FROM dictionary").use { rs ->
            rs.next()
            rs.getInt(1)
        }
    }

private fun mapDictionaryRow(rs: ResultSet): DictionaryRow {
    val freqObj = rs.getObject("frequency_rank")
    return DictionaryRow(
        surfaceForm = rs.getString("surface_form"),
        lemma = rs.getString("lemma"),
        pos = rs.getString("pos"),
        grammarInfo = rs.getString("grammar_info"),
        englishGloss = rs.getString("english_gloss"),
        isCognate = rs.getInt("is_cognate") == SeedDatabase.COGNATE_FLAG_TRUE,
        frequencyRank = if (freqObj != null) rs.getInt("frequency_rank") else null,
    )
}
