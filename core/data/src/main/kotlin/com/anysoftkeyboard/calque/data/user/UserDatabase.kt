package com.anysoftkeyboard.calque.data.user

import java.sql.Connection
import java.sql.DriverManager

/** Encapsulates the mutable user SQLite database containing [wordStates] and [customTexts]. */
class UserDatabase(
    val connection: Connection,
) : AutoCloseable {

    val wordStates: UserWordStateRepository = SqliteUserWordStateRepository(connection)
    val customTexts: UserCustomTextRepository = SqliteUserCustomTextRepository(connection)

    override fun close() {
        connection.close()
    }

    companion object {
        private val USER_TABLE_STATEMENTS =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS user_word_state (
                    lemma TEXT PRIMARY KEY,
                    status TEXT NOT NULL,
                    starred INTEGER NOT NULL,
                    saved_context_sentence TEXT,
                    encounter_count INTEGER NOT NULL,
                    last_seen_timestamp INTEGER NOT NULL
                )
                """
                    .trimIndent(),
                "CREATE INDEX IF NOT EXISTS idx_user_word_state_status ON user_word_state(status)",
                """
                CREATE TABLE IF NOT EXISTS user_custom_texts (
                    text_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    original_spanish TEXT NOT NULL,
                    simplified_spanish TEXT,
                    readability_level INTEGER NOT NULL,
                    created_timestamp INTEGER NOT NULL
                )
                """
                    .trimIndent(),
                "CREATE INDEX IF NOT EXISTS idx_user_custom_texts_created ON user_custom_texts(created_timestamp)",
            )

        /** Opens a user SQLite database connection at the given [jdbcUrl]. */
        fun open(jdbcUrl: String = "jdbc:sqlite::memory:"): UserDatabase {
            val connection = DriverManager.getConnection(jdbcUrl)
            return UserDatabase(connection)
        }

        /**
         * Creates all user tables and indexes defined in docs/DATA.md on the provided [connection].
         */
        fun createTables(connection: Connection) {
            connection.createStatement().use { stmt ->
                for (sql in USER_TABLE_STATEMENTS) {
                    stmt.execute(sql)
                }
            }
        }
    }
}
