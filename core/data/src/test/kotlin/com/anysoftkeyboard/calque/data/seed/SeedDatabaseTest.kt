package com.anysoftkeyboard.calque.data.seed

import java.sql.Connection
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDatabaseTest {

    @Test
    fun openInMemoryDatabase_createsAllSeedTablesAndIndices() {
        SeedDatabase.open().use { db ->
            SeedDatabase.createTables(db.connection)

            assertNotNull(db.dictionary)
            assertNotNull(db.falseCognates)
            assertNotNull(db.curatedLessons)
            assertNotNull(db.cognateRules)

            val tables = queryExistingTableNames(db.connection)
            assertTrue(tables.contains("dictionary"))
            assertTrue(tables.contains("false_cognates"))
            assertTrue(tables.contains("curated_lessons"))
            assertTrue(tables.contains("cognate_rules"))
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
