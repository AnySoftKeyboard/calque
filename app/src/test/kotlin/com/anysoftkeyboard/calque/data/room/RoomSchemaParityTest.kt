package com.anysoftkeyboard.calque.data.room

import com.anysoftkeyboard.calque.data.room.entities.CognateRuleEntity
import com.anysoftkeyboard.calque.data.room.entities.CuratedLessonEntity
import com.anysoftkeyboard.calque.data.room.entities.DictionaryEntity
import com.anysoftkeyboard.calque.data.room.entities.FalseCognateEntity
import com.anysoftkeyboard.calque.data.room.entities.UserCustomTextEntity
import com.anysoftkeyboard.calque.data.room.entities.UserWordStateEntity
import com.anysoftkeyboard.calque.data.seed.SeedDatabase
import com.anysoftkeyboard.calque.data.user.UserDatabase
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying strict schema parity between the build-time SQLite DDL (defined in
 * `SeedDatabase` and `UserDatabase` via sqlite-jdbc) and the runtime AndroidX Room entity
 * definitions.
 */
class RoomSchemaParityTest {

    data class TableColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val isPrimaryKey: Boolean,
    )

    data class TableIndexInfo(
        val name: String,
        val columns: List<String>,
        val unique: Boolean,
    )

    data class EntitySchemaDefinition(
        val tableName: String,
        val entityClass: Class<*>,
        val columns: Map<String, TableColumnInfo>,
        val indices: Map<String, List<String>>,
    )

    // Room's generated schema validation (`TableInfo`) treats every Kotlin non-null primary key as
    // `NOT NULL` and compares the flag against `PRAGMA table_info`. A pre-packaged `seed.db` that
    // declares `TEXT PRIMARY KEY` (notnull = 0) is therefore rejected at runtime, so the
    // expectations below require `notNull = true` for every primary key column.
    private val seedEntitySchemas =
        listOf(
            EntitySchemaDefinition(
                tableName = "dictionary",
                entityClass = DictionaryEntity::class.java,
                columns =
                    mapOf(
                        // The key is (surface_form, lemma, pos): one row per reading.
                        "surface_form" to TableColumnInfo("surface_form", "TEXT", true, true),
                        "lemma" to TableColumnInfo("lemma", "TEXT", true, true),
                        "pos" to TableColumnInfo("pos", "TEXT", true, true),
                        "grammar_info" to TableColumnInfo("grammar_info", "TEXT", false, false),
                        "english_gloss" to TableColumnInfo("english_gloss", "TEXT", true, false),
                        "is_cognate" to TableColumnInfo("is_cognate", "INTEGER", true, false),
                        "frequency_rank" to
                            TableColumnInfo("frequency_rank", "INTEGER", false, false),
                        "reading_order" to TableColumnInfo("reading_order", "INTEGER", true, false),
                    ),
                indices = mapOf("idx_dictionary_lemma" to listOf("lemma")),
            ),
            EntitySchemaDefinition(
                tableName = "false_cognates",
                entityClass = FalseCognateEntity::class.java,
                columns =
                    mapOf(
                        "surface_form" to TableColumnInfo("surface_form", "TEXT", true, true),
                        "lemma" to TableColumnInfo("lemma", "TEXT", true, false),
                        "apparent_english" to
                            TableColumnInfo("apparent_english", "TEXT", true, false),
                        "actual_english" to TableColumnInfo("actual_english", "TEXT", true, false),
                        "real_spanish_for_apparent" to
                            TableColumnInfo("real_spanish_for_apparent", "TEXT", true, false),
                        "severity" to TableColumnInfo("severity", "TEXT", true, false),
                        "disambiguation_tip" to
                            TableColumnInfo("disambiguation_tip", "TEXT", true, false),
                        "example_es" to TableColumnInfo("example_es", "TEXT", true, false),
                        "example_en" to TableColumnInfo("example_en", "TEXT", true, false),
                    ),
                indices = mapOf("idx_false_cognates_lemma" to listOf("lemma")),
            ),
            EntitySchemaDefinition(
                tableName = "curated_lessons",
                entityClass = CuratedLessonEntity::class.java,
                columns =
                    mapOf(
                        "lesson_id" to TableColumnInfo("lesson_id", "TEXT", true, true),
                        "level" to TableColumnInfo("level", "INTEGER", true, false),
                        "sequence_order" to
                            TableColumnInfo("sequence_order", "INTEGER", true, false),
                        "title" to TableColumnInfo("title", "TEXT", true, false),
                        "target_concept" to TableColumnInfo("target_concept", "TEXT", true, false),
                        "content_json" to TableColumnInfo("content_json", "TEXT", true, false),
                    ),
                indices = mapOf("idx_curated_lessons_level" to listOf("level")),
            ),
            EntitySchemaDefinition(
                tableName = "cognate_rules",
                entityClass = CognateRuleEntity::class.java,
                columns =
                    mapOf(
                        "rule_id" to TableColumnInfo("rule_id", "TEXT", true, true),
                        "english_suffix" to TableColumnInfo("english_suffix", "TEXT", true, false),
                        "spanish_suffix" to TableColumnInfo("spanish_suffix", "TEXT", true, false),
                        "gender_rule" to TableColumnInfo("gender_rule", "TEXT", false, false),
                        "spelling_note" to TableColumnInfo("spelling_note", "TEXT", true, false),
                        "examples_json" to TableColumnInfo("examples_json", "TEXT", true, false),
                    ),
                indices = emptyMap(),
            ),
        )

    private val userEntitySchemas =
        listOf(
            EntitySchemaDefinition(
                tableName = "user_word_state",
                entityClass = UserWordStateEntity::class.java,
                columns =
                    mapOf(
                        "lemma" to TableColumnInfo("lemma", "TEXT", true, true),
                        "status" to TableColumnInfo("status", "TEXT", true, false),
                        "starred" to TableColumnInfo("starred", "INTEGER", true, false),
                        "saved_context_sentence" to
                            TableColumnInfo("saved_context_sentence", "TEXT", false, false),
                        "encounter_count" to
                            TableColumnInfo("encounter_count", "INTEGER", true, false),
                        "last_seen_timestamp" to
                            TableColumnInfo("last_seen_timestamp", "INTEGER", true, false),
                    ),
                indices = mapOf("idx_user_word_state_status" to listOf("status")),
            ),
            EntitySchemaDefinition(
                tableName = "user_custom_texts",
                entityClass = UserCustomTextEntity::class.java,
                columns =
                    mapOf(
                        "text_id" to TableColumnInfo("text_id", "INTEGER", true, true),
                        "title" to TableColumnInfo("title", "TEXT", true, false),
                        "original_spanish" to
                            TableColumnInfo("original_spanish", "TEXT", true, false),
                        "simplified_spanish" to
                            TableColumnInfo("simplified_spanish", "TEXT", false, false),
                        "readability_level" to
                            TableColumnInfo("readability_level", "INTEGER", true, false),
                        "created_timestamp" to
                            TableColumnInfo("created_timestamp", "INTEGER", true, false),
                    ),
                indices = mapOf("idx_user_custom_texts_created" to listOf("created_timestamp")),
            ),
        )

    @Test
    fun seedDatabase_schemaParityWithRoomEntities() {
        SeedDatabase.open("jdbc:sqlite::memory:").use { db ->
            SeedDatabase.createTables(db.connection)

            for (schema in seedEntitySchemas) {
                assertEntityClassFieldsMatchSchema(schema)
                assertSqliteMatchesEntitySchema(db.connection, schema)
            }
        }
    }

    @Test
    fun userDatabase_schemaParityWithRoomEntities() {
        UserDatabase.open("jdbc:sqlite::memory:").use { db ->
            UserDatabase.createTables(db.connection)

            for (schema in userEntitySchemas) {
                assertEntityClassFieldsMatchSchema(schema)
                assertSqliteMatchesEntitySchema(db.connection, schema)
            }
        }
    }

    @Test
    fun packagedSeedAsset_schemaParityWithRoomEntities() {
        val assetFile = File("app/src/main/assets/databases/seed.db")
        assertTrue("Expected packaged seed database at ${assetFile.path}", assetFile.exists())

        DriverManager.getConnection("jdbc:sqlite:${assetFile.absolutePath}").use { conn ->
            for (schema in seedEntitySchemas) {
                assertSqliteMatchesEntitySchema(conn, schema)
            }
        }
    }

    @Test
    fun packagedSeedAsset_userVersionMatchesRoomDatabaseVersion() {
        val assetFile = File("app/src/main/assets/databases/seed.db")
        assertTrue("Expected packaged seed database at ${assetFile.path}", assetFile.exists())

        DriverManager.getConnection("jdbc:sqlite:${assetFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("PRAGMA user_version").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(
                        "seed.db user_version must equal CalqueSeedDatabase @Database(version)",
                        EXPECTED_SEED_SCHEMA_VERSION,
                        rs.getInt(1),
                    )
                }
            }
        }
    }

    @Test
    fun dictionaryEntity_dataRoundTripCompatibility() {
        SeedDatabase.open("jdbc:sqlite::memory:").use { db ->
            SeedDatabase.createTables(db.connection)

            val sql =
                """
                INSERT INTO dictionary (surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank, reading_order)
                VALUES ('perro', 'perro', 'noun', 'masculine', 'dog', 0, 150, 0)
                """
                    .trimIndent()
            db.connection.createStatement().use { it.execute(sql) }

            val entry = db.dictionary.getBySurfaceForm("perro")
            assertNotNull(entry)
            assertEquals("perro", entry?.surfaceForm)
            assertEquals("dog", entry?.englishGloss)
            assertEquals(150, entry?.frequencyRank)

            val entity =
                DictionaryEntity(
                    surfaceForm = entry!!.surfaceForm,
                    lemma = entry.lemma,
                    pos = entry.pos,
                    grammarInfo = entry.grammarInfo,
                    englishGloss = entry.englishGloss,
                    isCognate = if (entry.isCognate) 1 else 0,
                    frequencyRank = entry.frequencyRank,
                    readingOrder = 0,
                )
            assertEquals("perro", entity.surfaceForm)
            assertEquals("dog", entity.englishGloss)
            assertEquals(0, entity.isCognate)
        }
    }

    @Test
    fun userWordStateEntity_dataRoundTripCompatibility() {
        UserDatabase.open("jdbc:sqlite::memory:").use { db ->
            UserDatabase.createTables(db.connection)

            val sql =
                """
                INSERT INTO user_word_state (lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp)
                VALUES ('amigo', 'MASTERED', 1, 'Mi amigo es bueno.', 5, 1700000000000)
                """
                    .trimIndent()
            db.connection.createStatement().use { it.execute(sql) }

            val state = db.wordStates.getWordState("amigo")
            assertNotNull(state)
            assertEquals("amigo", state?.lemma)
            assertTrue(state?.starred == true)
            assertEquals(5, state?.encounterCount)

            val entity =
                UserWordStateEntity(
                    lemma = state!!.lemma,
                    status = state.status.name,
                    starred = if (state.starred) 1 else 0,
                    savedContextSentence = state.savedContextSentence,
                    encounterCount = state.encounterCount,
                    lastSeenTimestamp = state.lastSeenTimestamp,
                )
            assertEquals("amigo", entity.lemma)
            assertEquals(1, entity.starred)
            assertEquals("Mi amigo es bueno.", entity.savedContextSentence)
        }
    }

    private fun assertEntityClassFieldsMatchSchema(schema: EntitySchemaDefinition) {
        val declaredFieldSnakeNames =
            schema.entityClass.declaredFields
                .filter { !it.isSynthetic && !java.lang.reflect.Modifier.isStatic(it.modifiers) }
                .map { camelToSnake(it.name) }
                .toSet()

        val expectedColumnNames = schema.columns.keys

        assertEquals(
            "Fields in Kotlin entity ${schema.entityClass.simpleName} do not match table schema columns",
            expectedColumnNames,
            declaredFieldSnakeNames,
        )
    }

    private fun assertSqliteMatchesEntitySchema(conn: Connection, schema: EntitySchemaDefinition) {
        val tableName = schema.tableName
        val sqliteColumns = querySqliteColumns(conn, tableName)
        assertTrue(
            "Table '$tableName' does not exist in SQLite database",
            sqliteColumns.isNotEmpty(),
        )

        assertEquals(
            "Column count mismatch for table '$tableName'",
            schema.columns.size,
            sqliteColumns.size,
        )

        for ((colName, expectedCol) in schema.columns) {
            val actualCol =
                sqliteColumns[colName]
                    ?: error("Column '$colName' not found in SQLite table '$tableName'")

            assertEquals(
                "Type mismatch for $tableName.$colName",
                expectedCol.type.uppercase(),
                actualCol.type.uppercase(),
            )
            assertEquals(
                "Primary key flag mismatch for $tableName.$colName",
                expectedCol.isPrimaryKey,
                actualCol.isPrimaryKey,
            )
            assertEquals(
                "NotNull flag mismatch for $tableName.$colName",
                expectedCol.notNull,
                actualCol.notNull,
            )
        }

        val sqliteIndices = querySqliteIndices(conn, tableName)
        for ((indexName, indexedColumns) in schema.indices) {
            val sqliteIndex = sqliteIndices[indexName]
            assertNotNull(
                "Expected index '$indexName' on table '$tableName' not found in SQLite",
                sqliteIndex,
            )
            assertEquals(
                "Indexed columns mismatch for '$indexName'",
                indexedColumns,
                sqliteIndex?.columns,
            )
        }
    }

    private fun querySqliteColumns(conn: Connection, table: String): Map<String, TableColumnInfo> {
        val result = mutableMapOf<String, TableColumnInfo>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info($table)").use { rs ->
                while (rs.next()) {
                    val name = rs.getString("name")
                    val type = rs.getString("type")
                    val notNull = rs.getInt("notnull") == 1
                    val pk = rs.getInt("pk") > 0
                    result[name] = TableColumnInfo(name, type, notNull, pk)
                }
            }
        }
        return result
    }

    private fun querySqliteIndices(conn: Connection, table: String): Map<String, TableIndexInfo> {
        val result = mutableMapOf<String, TableIndexInfo>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA index_list($table)").use { rs ->
                val indexNames = mutableListOf<Pair<String, Boolean>>()
                while (rs.next()) {
                    val name = rs.getString("name")
                    val unique = rs.getInt("unique") == 1
                    indexNames.add(name to unique)
                }

                for ((name, unique) in indexNames) {
                    val cols = mutableListOf<String>()
                    conn.createStatement().use { colStmt ->
                        colStmt.executeQuery("PRAGMA index_info($name)").use { colRs ->
                            while (colRs.next()) {
                                cols.add(colRs.getString("name"))
                            }
                        }
                    }
                    result[name] = TableIndexInfo(name, cols, unique)
                }
            }
        }
        return result
    }

    private companion object {
        const val EXPECTED_SEED_SCHEMA_VERSION = 2
    }

    private fun camelToSnake(str: String): String {
        val builder = java.lang.StringBuilder()
        for (char in str) {
            if (char.isUpperCase()) {
                builder.append('_').append(char.lowercaseChar())
            } else {
                builder.append(char)
            }
        }
        return builder.toString()
    }
}
