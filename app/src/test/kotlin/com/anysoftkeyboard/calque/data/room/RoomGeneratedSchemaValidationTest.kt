package com.anysoftkeyboard.calque.data.room

import androidx.room.RoomOpenDelegate
import androidx.room.util.TableInfo
import com.anysoftkeyboard.calque.data.seed.SeedDatabase
import com.anysoftkeyboard.calque.data.user.UserDatabase
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the schema creation and validation code that Room generates (`*_Impl.createOpenDelegate()`)
 * against real SQLite files on a plain JVM, through a JDBC-backed
 * [androidx.sqlite.SQLiteConnection].
 *
 * This is the same check Room performs at runtime when it opens a database, including one copied
 * from `createFromAsset`. A mismatch (for example a nullable primary key in the packaged `seed.db`)
 * would otherwise only surface on a device as an `IllegalStateException: Migration didn't properly
 * handle`.
 */
class RoomGeneratedSchemaValidationTest {

    private val seedTables =
        listOf("dictionary", "false_cognates", "curated_lessons", "cognate_rules")
    private val userTables = listOf("user_word_state", "user_custom_texts")

    private fun openDelegate(databaseClass: String): RoomOpenDelegate {
        val implClass = Class.forName("${databaseClass}_Impl")
        val database = implClass.getDeclaredConstructor().newInstance()
        val method = implClass.getDeclaredMethod("createOpenDelegate")
        method.isAccessible = true
        return method.invoke(database) as RoomOpenDelegate
    }

    private fun seedDelegate() = openDelegate(CalqueSeedDatabase::class.java.name)

    private fun userDelegate() = openDelegate(CalqueUserDatabase::class.java.name)

    private fun inMemory(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun validate(
        delegate: RoomOpenDelegate,
        connection: Connection,
    ): RoomOpenDelegate.ValidationResult =
        delegate.onValidateSchema(JdbcSqliteConnection(connection))

    private fun assertValid(result: RoomOpenDelegate.ValidationResult) {
        assertTrue("Room schema validation failed:\n${result.expectedFoundMsg}", result.isValid)
    }

    private fun readTable(connection: Connection, table: String): TableInfo =
        TableInfo.read(JdbcSqliteConnection(connection), table)

    @Test
    fun harness_roomCreatedSeedSchema_passesRoomValidation() {
        inMemory().use { conn ->
            val delegate = seedDelegate()
            delegate.createAllTables(JdbcSqliteConnection(conn))

            assertValid(validate(delegate, conn))
        }
    }

    @Test
    fun harness_roomCreatedUserSchema_passesRoomValidation() {
        inMemory().use { conn ->
            val delegate = userDelegate()
            delegate.createAllTables(JdbcSqliteConnection(conn))

            assertValid(validate(delegate, conn))
        }
    }

    @Test
    fun buildTimeSeedDdl_passesRoomValidation() {
        SeedDatabase.open("jdbc:sqlite::memory:").use { db ->
            SeedDatabase.createTables(db.connection)

            assertValid(validate(seedDelegate(), db.connection))
        }
    }

    @Test
    fun buildTimeUserDdl_passesRoomValidation() {
        UserDatabase.open("jdbc:sqlite::memory:").use { db ->
            UserDatabase.createTables(db.connection)

            assertValid(validate(userDelegate(), db.connection))
        }
    }

    @Test
    fun packagedSeedAsset_passesRoomValidation() {
        val assetFile = File("app/src/main/assets/databases/seed.db")
        assertTrue("Expected packaged seed database at ${assetFile.path}", assetFile.exists())

        DriverManager.getConnection("jdbc:sqlite:${assetFile.absolutePath}").use { conn ->
            assertValid(validate(seedDelegate(), conn))
        }
    }

    @Test
    fun packagedSeedAsset_userVersionMatchesRoomDelegateVersion() {
        val assetFile = File("app/src/main/assets/databases/seed.db")
        assertTrue("Expected packaged seed database at ${assetFile.path}", assetFile.exists())

        DriverManager.getConnection("jdbc:sqlite:${assetFile.absolutePath}").use { conn ->
            val userVersion =
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("PRAGMA user_version").use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
            assertEquals(seedDelegate().version, userVersion)
        }
    }

    @Test
    fun buildTimeSeedTables_areIdenticalToRoomCreatedTables() {
        inMemory().use { roomConn ->
            seedDelegate().createAllTables(JdbcSqliteConnection(roomConn))
            SeedDatabase.open("jdbc:sqlite::memory:").use { buildTime ->
                SeedDatabase.createTables(buildTime.connection)

                for (table in seedTables) {
                    assertEquals(
                        "Table '$table' differs between Room and build-time DDL",
                        readTable(roomConn, table),
                        readTable(buildTime.connection, table),
                    )
                }
            }
        }
    }

    @Test
    fun buildTimeUserTables_areIdenticalToRoomCreatedTables() {
        inMemory().use { roomConn ->
            userDelegate().createAllTables(JdbcSqliteConnection(roomConn))
            UserDatabase.open("jdbc:sqlite::memory:").use { buildTime ->
                UserDatabase.createTables(buildTime.connection)

                for (table in userTables) {
                    assertEquals(
                        "Table '$table' differs between Room and build-time DDL",
                        readTable(roomConn, table),
                        readTable(buildTime.connection, table),
                    )
                }
            }
        }
    }

    @Test
    fun nullablePrimaryKey_isRejectedByRoomValidation() {
        // Regression guard: this is how seed.db was packaged before the DDL fix.
        inMemory().use { conn ->
            val delegate = seedDelegate()
            delegate.createAllTables(JdbcSqliteConnection(conn))
            conn.createStatement().use { stmt ->
                stmt.execute("DROP TABLE cognate_rules")
                stmt.execute(
                    """
                    CREATE TABLE cognate_rules (
                        rule_id TEXT PRIMARY KEY,
                        english_suffix TEXT NOT NULL,
                        spanish_suffix TEXT NOT NULL,
                        gender_rule TEXT,
                        spelling_note TEXT NOT NULL,
                        examples_json TEXT NOT NULL
                    )
                    """
                        .trimIndent()
                )
            }

            val result = validate(delegate, conn)

            assertFalse("Nullable primary key must fail Room validation", result.isValid)
            assertTrue(result.expectedFoundMsg!!.contains("cognate_rules"))
        }
    }

    @Test
    fun missingIndex_isRejectedByRoomValidation() {
        inMemory().use { conn ->
            val delegate = seedDelegate()
            delegate.createAllTables(JdbcSqliteConnection(conn))
            conn.createStatement().use { it.execute("DROP INDEX idx_dictionary_lemma") }

            val result = validate(delegate, conn)

            assertFalse("Missing index must fail Room validation", result.isValid)
            assertTrue(result.expectedFoundMsg!!.contains("dictionary"))
        }
    }

    @Test
    fun missingColumn_isRejectedByRoomValidation() {
        inMemory().use { conn ->
            val delegate = userDelegate()
            delegate.createAllTables(JdbcSqliteConnection(conn))
            conn.createStatement().use {
                it.execute("ALTER TABLE user_word_state DROP COLUMN saved_context_sentence")
            }

            val result = validate(delegate, conn)

            assertFalse("Missing column must fail Room validation", result.isValid)
            assertTrue(result.expectedFoundMsg!!.contains("user_word_state"))
        }
    }

    @Test
    fun delegatesDeclareExpectedSchemaVersionAndDistinctIdentityHashes() {
        assertEquals(1, seedDelegate().version)
        assertEquals(1, userDelegate().version)
        assertTrue(seedDelegate().identityHash != userDelegate().identityHash)
    }
}
