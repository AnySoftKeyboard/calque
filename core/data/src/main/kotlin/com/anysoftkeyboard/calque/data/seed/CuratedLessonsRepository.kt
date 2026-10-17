package com.anysoftkeyboard.calque.data.seed

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import java.sql.Connection
import java.sql.ResultSet

/** Repository interface for accessing curated syntactic progression lessons. */
interface CuratedLessonsRepository {
    /** Retrieves all curated lessons, ordered by level and sequence order. */
    fun getAll(): List<CuratedLesson>

    /**
     * Retrieves curated lessons for a specific difficulty level (0, 1, 2, ...), ordered by sequence
     * order.
     */
    fun getByLevel(level: Int): List<CuratedLesson>

    /** Retrieves a curated lesson by its unique identifier (e.g. "lvl0_00_cognate_unlock"). */
    fun getById(lessonId: String): CuratedLesson?
}

/** SQLite-backed implementation of [CuratedLessonsRepository]. */
class SqliteCuratedLessonsRepository(
    private val connection: Connection,
) : CuratedLessonsRepository {

    override fun getAll(): List<CuratedLesson> {
        val sql =
            """
            SELECT lesson_id, level, sequence_order, title, target_concept, content_json
            FROM curated_lessons
            ORDER BY level ASC, sequence_order ASC
            """
                .trimIndent()

        val results = mutableListOf<CuratedLesson>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun getByLevel(level: Int): List<CuratedLesson> {
        val sql =
            """
            SELECT lesson_id, level, sequence_order, title, target_concept, content_json
            FROM curated_lessons
            WHERE level = ?
            ORDER BY sequence_order ASC
            """
                .trimIndent()

        val results = mutableListOf<CuratedLesson>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, level)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun getById(lessonId: String): CuratedLesson? {
        val trimmed = lessonId.trim()
        if (trimmed.isEmpty()) return null

        val sql =
            """
            SELECT lesson_id, level, sequence_order, title, target_concept, content_json
            FROM curated_lessons
            WHERE lesson_id = ?
            LIMIT 1
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, trimmed)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapRow(rs) else null
            }
        }
    }

    private fun mapRow(rs: ResultSet): CuratedLesson {
        return CuratedLesson(
            lessonId = rs.getString("lesson_id"),
            level = rs.getInt("level"),
            sequenceOrder = rs.getInt("sequence_order"),
            title = rs.getString("title"),
            targetConcept = rs.getString("target_concept"),
            contentJson = rs.getString("content_json"),
        )
    }
}
