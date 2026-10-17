package com.anysoftkeyboard.calque.data.user

import com.anysoftkeyboard.calque.data.models.UserCustomText
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

/** Repository interface for managing user-imported custom texts and simplified versions. */
interface UserCustomTextRepository {
    /** Inserts a new custom text and returns its auto-generated textId. */
    fun insert(text: UserCustomText): Long

    /**
     * Updates an existing custom text by its [UserCustomText.textId]. Returns true if found and
     * updated.
     */
    fun update(text: UserCustomText): Boolean

    /** Retrieves a custom text entry by [textId]. */
    fun getById(textId: Long): UserCustomText?

    /** Retrieves all saved custom texts ordered from newest to oldest. */
    fun getAll(): List<UserCustomText>

    /** Deletes a custom text entry by [textId]. Returns true if deleted. */
    fun delete(textId: Long): Boolean
}

/** SQLite-backed implementation of [UserCustomTextRepository]. */
class SqliteUserCustomTextRepository(
    private val connection: Connection,
) : UserCustomTextRepository {

    override fun insert(text: UserCustomText): Long {
        val sql =
            """
            INSERT INTO user_custom_texts (
                title, original_spanish, simplified_spanish, readability_level, created_timestamp
            ) VALUES (?, ?, ?, ?, ?)
            """
                .trimIndent()

        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setString(PARAM_TITLE, text.title)
            stmt.setString(PARAM_ORIGINAL, text.originalSpanish)
            if (text.simplifiedSpanish != null) {
                stmt.setString(PARAM_SIMPLIFIED, text.simplifiedSpanish)
            } else {
                stmt.setNull(PARAM_SIMPLIFIED, Types.VARCHAR)
            }
            stmt.setInt(PARAM_READABILITY, text.readabilityLevel)
            stmt.setLong(PARAM_TIMESTAMP, text.createdTimestamp)
            stmt.executeUpdate()

            stmt.generatedKeys.use { rs ->
                if (rs.next()) {
                    return rs.getLong(1)
                }
            }
        }
        return 0L
    }

    override fun update(text: UserCustomText): Boolean {
        if (text.textId <= 0L) return false

        val sql =
            """
            UPDATE user_custom_texts
            SET title = ?,
                original_spanish = ?,
                simplified_spanish = ?,
                readability_level = ?,
                created_timestamp = ?
            WHERE text_id = ?
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_TITLE, text.title)
            stmt.setString(PARAM_ORIGINAL, text.originalSpanish)
            if (text.simplifiedSpanish != null) {
                stmt.setString(PARAM_SIMPLIFIED, text.simplifiedSpanish)
            } else {
                stmt.setNull(PARAM_SIMPLIFIED, Types.VARCHAR)
            }
            stmt.setInt(PARAM_READABILITY, text.readabilityLevel)
            stmt.setLong(PARAM_TIMESTAMP, text.createdTimestamp)
            stmt.setLong(PARAM_UPDATE_ID, text.textId)
            return stmt.executeUpdate() > 0
        }
    }

    override fun getById(textId: Long): UserCustomText? {
        if (textId <= 0L) return null

        val sql =
            """
            SELECT text_id, title, original_spanish, simplified_spanish, readability_level, created_timestamp
            FROM user_custom_texts
            WHERE text_id = ?
            LIMIT 1
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, textId)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapRow(rs) else null
            }
        }
    }

    override fun getAll(): List<UserCustomText> {
        val sql =
            """
            SELECT text_id, title, original_spanish, simplified_spanish, readability_level, created_timestamp
            FROM user_custom_texts
            ORDER BY created_timestamp DESC
            """
                .trimIndent()

        val results = mutableListOf<UserCustomText>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun delete(textId: Long): Boolean {
        if (textId <= 0L) return false

        val sql = "DELETE FROM user_custom_texts WHERE text_id = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, textId)
            return stmt.executeUpdate() > 0
        }
    }

    private fun mapRow(rs: ResultSet): UserCustomText {
        return UserCustomText(
            textId = rs.getLong("text_id"),
            title = rs.getString("title"),
            originalSpanish = rs.getString("original_spanish"),
            simplifiedSpanish = rs.getString("simplified_spanish"),
            readabilityLevel = rs.getInt("readability_level"),
            createdTimestamp = rs.getLong("created_timestamp"),
        )
    }

    private companion object {
        const val PARAM_TITLE = 1
        const val PARAM_ORIGINAL = 2
        const val PARAM_SIMPLIFIED = 3
        const val PARAM_READABILITY = 4
        const val PARAM_TIMESTAMP = 5
        const val PARAM_UPDATE_ID = 6
    }
}
