package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.ScoredWordInfo
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * Manages the SQLite database operations for building the Calque seed.db database.
 *
 * Implements the table schemas defined in docs/DATA.md.
 */
class SeedDatabase(private val connection: Connection) : AutoCloseable {

    init {
        createTables()
    }

    private fun createTables() {
        val statements =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS dictionary (
                    surface_form TEXT PRIMARY KEY,
                    lemma TEXT NOT NULL,
                    pos TEXT NOT NULL,
                    grammar_info TEXT,
                    english_gloss TEXT NOT NULL,
                    is_cognate INTEGER NOT NULL,
                    frequency_rank INTEGER
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_dictionary_lemma ON dictionary(lemma)",
                """
                CREATE TABLE IF NOT EXISTS false_cognates (
                    surface_form TEXT PRIMARY KEY,
                    lemma TEXT NOT NULL,
                    apparent_english TEXT NOT NULL,
                    actual_english TEXT NOT NULL,
                    real_spanish_for_apparent TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    disambiguation_tip TEXT NOT NULL,
                    example_es TEXT NOT NULL,
                    example_en TEXT NOT NULL
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_false_cognates_lemma ON false_cognates(lemma)",
                """
                CREATE TABLE IF NOT EXISTS curated_lessons (
                    lesson_id TEXT PRIMARY KEY,
                    level INTEGER NOT NULL,
                    sequence_order INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    target_concept TEXT NOT NULL,
                    content_json TEXT NOT NULL
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_curated_lessons_level ON curated_lessons(level)",
                """
                CREATE TABLE IF NOT EXISTS cognate_rules (
                    rule_id TEXT PRIMARY KEY,
                    english_suffix TEXT NOT NULL,
                    spanish_suffix TEXT NOT NULL,
                    gender_rule TEXT,
                    spelling_note TEXT NOT NULL,
                    examples_json TEXT NOT NULL
                )
                """,
            )
        connection.createStatement().use { stmt ->
            for (sql in statements) {
                stmt.execute(sql.trimIndent())
            }
        }
    }

    private inline fun executeTransaction(action: () -> Unit) {
        val prevAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            action()
            connection.commit()
        } catch (e: SQLException) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = prevAutoCommit
        }
    }

    /** Inserts a batch of dictionary rows inside a single transaction. */
    fun insertDictionaryBatch(entries: Collection<DictionaryRow>) {
        if (entries.isEmpty()) return
        val sql =
            """
            INSERT OR REPLACE INTO dictionary (
                surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        executeTransaction {
            connection.prepareStatement(sql).use { pstmt ->
                for (entry in entries) {
                    DatabaseBinders.bindDictionaryRow(pstmt, entry)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
        }
    }

    /** Queries all surface forms registered for a given lemma. */
    fun querySurfacesForLemma(lemma: String): List<String> {
        val sql = "SELECT surface_form FROM dictionary WHERE lemma = ?"
        val surfaces = mutableListOf<String>()
        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(PARAM_QUERY_LEMMA, lemma)
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    surfaces.add(rs.getString("surface_form"))
                }
            }
        }
        return surfaces
    }

    /** Looks up word properties from the dictionary table for sentence scoring. */
    fun lookupWord(surfaceForm: String): ScoredWordInfo? {
        val sql =
            "SELECT is_cognate, frequency_rank, grammar_info FROM dictionary WHERE surface_form = ? LIMIT 1"
        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(PARAM_LOOKUP_SURFACE, surfaceForm)
            pstmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return DatabaseBinders.mapWordInfo(rs)
                }
            }
        }
        return null
    }

    /** Inserts a batch of false cognate rows inside a transaction. */
    fun insertFalseCognatesBatch(entries: Collection<FalseCognateRow>) {
        if (entries.isEmpty()) return
        val sql =
            """
            INSERT OR REPLACE INTO false_cognates (
                surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                severity, disambiguation_tip, example_es, example_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        executeTransaction {
            connection.prepareStatement(sql).use { pstmt ->
                for (entry in entries) {
                    DatabaseBinders.bindFalseCognateRow(pstmt, entry)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
        }
    }

    /** Inserts a batch of curated lessons. */
    fun insertCuratedLessonsBatch(entries: Collection<CuratedLessonRow>) {
        if (entries.isEmpty()) return
        val sql =
            """
            INSERT OR REPLACE INTO curated_lessons (
                lesson_id, level, sequence_order, title, target_concept, content_json
            ) VALUES (?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        executeTransaction {
            connection.prepareStatement(sql).use { pstmt ->
                for (entry in entries) {
                    DatabaseBinders.bindCuratedLessonRow(pstmt, entry)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
        }
    }

    /** Inserts a batch of cognate rules. */
    fun insertCognateRulesBatch(entries: Collection<CognateRuleRow>) {
        if (entries.isEmpty()) return
        val sql =
            """
            INSERT OR REPLACE INTO cognate_rules (
                rule_id, english_suffix, spanish_suffix, gender_rule, spelling_note, examples_json
            ) VALUES (?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        executeTransaction {
            connection.prepareStatement(sql).use { pstmt ->
                for (entry in entries) {
                    DatabaseBinders.bindCognateRuleRow(pstmt, entry)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
            }
        }
    }

    /** Queries all cognate rules stored in the database. */
    fun queryCognateRules(): List<CognateRuleRow> {
        val sql =
            """
            SELECT rule_id, english_suffix, spanish_suffix, gender_rule, spelling_note, examples_json
            FROM cognate_rules
            ORDER BY rowid ASC
            """
                .trimIndent()
        val rules = mutableListOf<CognateRuleRow>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    rules.add(DatabaseBinders.mapCognateRuleRow(rs))
                }
            }
        }
        return rules
    }

    override fun close() {
        connection.close()
    }

    companion object {
        const val COGNATE_FLAG_TRUE = 1

        private const val PARAM_QUERY_LEMMA = 1
        private const val PARAM_LOOKUP_SURFACE = 1

        /** Connects to an SQLite database at [jdbcUrl] or creates an in-memory database. */
        fun open(jdbcUrl: String = "jdbc:sqlite::memory:"): SeedDatabase {
            val conn = DriverManager.getConnection(jdbcUrl)
            return SeedDatabase(conn)
        }
    }
}

internal object DatabaseBinders {
    private const val PARAM_DICT_SURFACE = 1
    private const val PARAM_DICT_LEMMA = 2
    private const val PARAM_DICT_POS = 3
    private const val PARAM_DICT_GRAMMAR = 4
    private const val PARAM_DICT_GLOSS = 5
    private const val PARAM_DICT_COGNATE = 6
    private const val PARAM_DICT_FREQ = 7

    private const val PARAM_FC_SURFACE = 1
    private const val PARAM_FC_LEMMA = 2
    private const val PARAM_FC_APPARENT = 3
    private const val PARAM_FC_ACTUAL = 4
    private const val PARAM_FC_REAL_SPANISH = 5
    private const val PARAM_FC_SEVERITY = 6
    private const val PARAM_FC_TIP = 7
    private const val PARAM_FC_EX_ES = 8
    private const val PARAM_FC_EX_EN = 9

    private const val PARAM_LESSON_ID = 1
    private const val PARAM_LESSON_LEVEL = 2
    private const val PARAM_LESSON_SEQ = 3
    private const val PARAM_LESSON_TITLE = 4
    private const val PARAM_LESSON_CONCEPT = 5
    private const val PARAM_LESSON_CONTENT = 6

    private const val PARAM_RULE_ID = 1
    private const val PARAM_RULE_EN_SUFFIX = 2
    private const val PARAM_RULE_ES_SUFFIX = 3
    private const val PARAM_RULE_GENDER = 4
    private const val PARAM_RULE_SPELLING = 5
    private const val PARAM_RULE_EXAMPLES = 6

    fun bindDictionaryRow(pstmt: PreparedStatement, entry: DictionaryRow) {
        pstmt.setString(PARAM_DICT_SURFACE, entry.surfaceForm)
        pstmt.setString(PARAM_DICT_LEMMA, entry.lemma)
        pstmt.setString(PARAM_DICT_POS, entry.pos)
        if (entry.grammarInfo != null) {
            pstmt.setString(PARAM_DICT_GRAMMAR, entry.grammarInfo)
        } else {
            pstmt.setNull(PARAM_DICT_GRAMMAR, Types.VARCHAR)
        }
        pstmt.setString(PARAM_DICT_GLOSS, entry.englishGloss)
        pstmt.setInt(PARAM_DICT_COGNATE, if (entry.isCognate) SeedDatabase.COGNATE_FLAG_TRUE else 0)
        if (entry.frequencyRank != null) {
            pstmt.setInt(PARAM_DICT_FREQ, entry.frequencyRank)
        } else {
            pstmt.setNull(PARAM_DICT_FREQ, Types.INTEGER)
        }
    }

    fun mapWordInfo(rs: ResultSet): ScoredWordInfo {
        val isCognate = rs.getInt("is_cognate") == SeedDatabase.COGNATE_FLAG_TRUE
        val freqObj = rs.getObject("frequency_rank")
        val freqRank = if (freqObj != null) rs.getInt("frequency_rank") else null
        val grammar = rs.getString("grammar_info")
        return ScoredWordInfo(
            frequencyRank = freqRank,
            isCognate = isCognate,
            grammar = grammar,
        )
    }

    fun bindFalseCognateRow(pstmt: PreparedStatement, entry: FalseCognateRow) {
        pstmt.setString(PARAM_FC_SURFACE, entry.surfaceForm)
        pstmt.setString(PARAM_FC_LEMMA, entry.lemma)
        pstmt.setString(PARAM_FC_APPARENT, entry.apparentEnglish)
        pstmt.setString(PARAM_FC_ACTUAL, entry.actualEnglish)
        pstmt.setString(PARAM_FC_REAL_SPANISH, entry.realSpanishForApparent)
        pstmt.setString(PARAM_FC_SEVERITY, entry.severity)
        pstmt.setString(PARAM_FC_TIP, entry.disambiguationTip)
        pstmt.setString(PARAM_FC_EX_ES, entry.exampleEs)
        pstmt.setString(PARAM_FC_EX_EN, entry.exampleEn)
    }

    fun bindCuratedLessonRow(pstmt: PreparedStatement, entry: CuratedLessonRow) {
        pstmt.setString(PARAM_LESSON_ID, entry.lessonId)
        pstmt.setInt(PARAM_LESSON_LEVEL, entry.level)
        pstmt.setInt(PARAM_LESSON_SEQ, entry.sequenceOrder)
        pstmt.setString(PARAM_LESSON_TITLE, entry.title)
        pstmt.setString(PARAM_LESSON_CONCEPT, entry.targetConcept)
        pstmt.setString(PARAM_LESSON_CONTENT, entry.contentJson)
    }

    fun bindCognateRuleRow(pstmt: PreparedStatement, entry: CognateRuleRow) {
        pstmt.setString(PARAM_RULE_ID, entry.ruleId)
        pstmt.setString(PARAM_RULE_EN_SUFFIX, entry.englishSuffix)
        pstmt.setString(PARAM_RULE_ES_SUFFIX, entry.spanishSuffix)
        if (entry.genderRule != null) {
            pstmt.setString(PARAM_RULE_GENDER, entry.genderRule)
        } else {
            pstmt.setNull(PARAM_RULE_GENDER, Types.VARCHAR)
        }
        pstmt.setString(PARAM_RULE_SPELLING, entry.spellingNote)
        pstmt.setString(PARAM_RULE_EXAMPLES, entry.examplesJson)
    }

    fun mapCognateRuleRow(rs: ResultSet): CognateRuleRow {
        return CognateRuleRow(
            ruleId = rs.getString("rule_id"),
            englishSuffix = rs.getString("english_suffix"),
            spanishSuffix = rs.getString("spanish_suffix"),
            genderRule = rs.getString("gender_rule"),
            spellingNote = rs.getString("spelling_note"),
            examplesJson = rs.getString("examples_json"),
        )
    }
}
