package com.anysoftkeyboard.calque.data.user

import java.sql.Connection
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDatabaseTest {

    @Test
    fun openInMemoryDatabase_createsAllUserTablesAndIndices() {
        UserDatabase.open().use { db ->
            UserDatabase.createTables(db.connection)

            assertNotNull(db.wordStates)
            assertNotNull(db.customTexts)

            val tables = queryExistingTableNames(db.connection)
            assertTrue(tables.contains("user_word_state"))
            assertTrue(tables.contains("user_custom_texts"))
        }
    }

    private fun queryExistingTableNames(connection: Connection): Set<String> {
        val tables = mutableSetOf<String>()
        val sql = "SELECT name FROM sqlite_master WHERE type = 'table'"
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    tables.add(rs.getString("name"))
                }
            }
        }
        return tables
    }
}
