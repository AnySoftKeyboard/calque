package com.anysoftkeyboard.calque.data.seed

import java.sql.Connection
import java.sql.DriverManager

/**
 * Encapsulates the read-only pre-packaged seed SQLite database containing [dictionary],
 * [falseCognates], [curatedLessons], and [cognateRules].
 */
class SeedDatabase(
    val connection: Connection,
) : AutoCloseable {

    val dictionary: DictionaryRepository = SqliteDictionaryRepository(connection)
    val falseCognates: FalseCognatesRepository = SqliteFalseCognatesRepository(connection)
    val curatedLessons: CuratedLessonsRepository = SqliteCuratedLessonsRepository(connection)
    val cognateRules: CognateRulesRepository = SqliteCognateRulesRepository(connection)

    override fun close() {
        connection.close()
    }

    companion object {
        private val SEED_TABLE_STATEMENTS =
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
                """
                    .trimIndent(),
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
                """
                    .trimIndent(),
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
                """
                    .trimIndent(),
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
                """
                    .trimIndent(),
            )

        /** Opens an SQLite database connection at the given [jdbcUrl]. */
        fun open(jdbcUrl: String = "jdbc:sqlite::memory:"): SeedDatabase {
            val connection = DriverManager.getConnection(jdbcUrl)
            return SeedDatabase(connection)
        }

        /**
         * Creates all seed tables and indexes defined in docs/DATA.md on the provided [connection].
         */
        fun createTables(connection: Connection) {
            connection.createStatement().use { stmt ->
                for (sql in SEED_TABLE_STATEMENTS) {
                    stmt.execute(sql)
                }
            }
        }
    }
}
