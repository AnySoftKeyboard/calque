package com.anysoftkeyboard.calque.data.user

import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types
import java.util.Locale

/** Repository interface for managing user vocabulary learning progress and vault items. */
interface UserWordStateRepository {
    /** Looks up the user state for a specific base [lemma]. Returns null if never seen. */
    fun getWordState(lemma: String): UserWordState?

    /** Batch queries user states for a collection of lemmas. */
    fun batchGetWordStates(lemmas: Collection<String>): Map<String, UserWordState>

    /** Inserts or completely replaces the state of a vocabulary word. */
    fun upsertWordState(state: UserWordState)

    /**
     * Increments the encounter count for [lemma] and updates its last seen timestamp. Transitions
     * unrecorded words to [UserWordStatus.LEARNING].
     */
    fun recordEncounter(lemma: String, timestamp: Long = System.currentTimeMillis())

    /** Updates the status of a word (e.g. marking as [UserWordStatus.MASTERED]). */
    fun setWordStatus(
        lemma: String,
        status: UserWordStatus,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Stars or unstars a word in the user's Vault, optionally attaching the context sentence. */
    fun setStarred(
        lemma: String,
        starred: Boolean,
        contextSentence: String? = null,
        timestamp: Long = System.currentTimeMillis(),
    )

    /** Retrieves all words explicitly starred by the user. */
    fun getStarredWords(): List<UserWordState>

    /** Retrieves all words with a given [UserWordStatus]. */
    fun getWordsByStatus(status: UserWordStatus): List<UserWordState>

    /** Deletes user state record for [lemma]. Returns true if a record was removed. */
    fun deleteWordState(lemma: String): Boolean
}

/** Returns the [UserWordStatus] for a [lemma], defaulting to [UserWordStatus.NEW] if unrecorded. */
fun UserWordStateRepository.getWordStatus(lemma: String): UserWordStatus {
    return getWordState(lemma)?.status ?: UserWordStatus.NEW
}

/**
 * Batch queries status for a collection of lemmas, guaranteeing an entry for every lemma
 * (defaulting to [UserWordStatus.NEW] when not explicitly saved).
 */
fun UserWordStateRepository.batchGetWordStatuses(
    lemmas: Collection<String>
): Map<String, UserWordStatus> {
    val states = batchGetWordStates(lemmas)
    return lemmas.associate { rawLemma ->
        val normalized = rawLemma.lowercase(Locale.ROOT).trim()
        rawLemma to (states[normalized]?.status ?: UserWordStatus.NEW)
    }
}

/** SQLite-backed implementation of [UserWordStateRepository]. */
class SqliteUserWordStateRepository(
    private val connection: Connection,
) : UserWordStateRepository {

    override fun getWordState(lemma: String): UserWordState? {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null

        val sql =
            """
            SELECT lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            FROM user_word_state
            WHERE lemma = ?
            LIMIT 1
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, normalized)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapRow(rs) else null
            }
        }
    }

    override fun batchGetWordStates(lemmas: Collection<String>): Map<String, UserWordState> {
        val uniqueLemmas =
            lemmas.map { it.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }.distinct()
        if (uniqueLemmas.isEmpty()) return emptyMap()

        val results = mutableMapOf<String, UserWordState>()
        for (chunk in uniqueLemmas.chunked(MAX_BATCH_CHUNK_SIZE)) {
            queryChunk(connection, chunk, results)
        }
        return results
    }

    override fun upsertWordState(state: UserWordState) {
        val normalized = state.lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return

        val sql =
            """
            INSERT INTO user_word_state (
                lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(lemma) DO UPDATE SET
                status = excluded.status,
                starred = excluded.starred,
                saved_context_sentence = excluded.saved_context_sentence,
                encounter_count = excluded.encounter_count,
                last_seen_timestamp = excluded.last_seen_timestamp
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, normalized)
            stmt.setString(PARAM_2, state.status.name)
            stmt.setInt(PARAM_3, if (state.starred) FLAG_TRUE else FLAG_FALSE)
            if (state.savedContextSentence != null) {
                stmt.setString(PARAM_4, state.savedContextSentence)
            } else {
                stmt.setNull(PARAM_4, Types.VARCHAR)
            }
            stmt.setInt(PARAM_5, state.encounterCount)
            stmt.setLong(PARAM_6, state.lastSeenTimestamp)
            stmt.executeUpdate()
        }
    }

    override fun recordEncounter(lemma: String, timestamp: Long) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return

        val sql =
            """
            INSERT INTO user_word_state (
                lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            ) VALUES (?, ?, 0, NULL, 1, ?)
            ON CONFLICT(lemma) DO UPDATE SET
                encounter_count = user_word_state.encounter_count + 1,
                last_seen_timestamp = excluded.last_seen_timestamp
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, normalized)
            stmt.setString(PARAM_2, UserWordStatus.LEARNING.name)
            stmt.setLong(PARAM_3, timestamp)
            stmt.executeUpdate()
        }
    }

    override fun setWordStatus(lemma: String, status: UserWordStatus, timestamp: Long) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return

        val sql =
            """
            INSERT INTO user_word_state (
                lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            ) VALUES (?, ?, 0, NULL, 1, ?)
            ON CONFLICT(lemma) DO UPDATE SET
                status = excluded.status,
                last_seen_timestamp = excluded.last_seen_timestamp
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, normalized)
            stmt.setString(PARAM_2, status.name)
            stmt.setLong(PARAM_3, timestamp)
            stmt.executeUpdate()
        }
    }

    override fun setStarred(
        lemma: String,
        starred: Boolean,
        contextSentence: String?,
        timestamp: Long
    ) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return

        val sql =
            """
            INSERT INTO user_word_state (
                lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            ) VALUES (?, ?, ?, ?, 1, ?)
            ON CONFLICT(lemma) DO UPDATE SET
                starred = excluded.starred,
                saved_context_sentence = COALESCE(excluded.saved_context_sentence, user_word_state.saved_context_sentence),
                last_seen_timestamp = excluded.last_seen_timestamp
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, normalized)
            stmt.setString(PARAM_2, UserWordStatus.LEARNING.name)
            stmt.setInt(PARAM_3, if (starred) FLAG_TRUE else FLAG_FALSE)
            if (contextSentence != null) {
                stmt.setString(PARAM_4, contextSentence)
            } else {
                stmt.setNull(PARAM_4, Types.VARCHAR)
            }
            stmt.setLong(PARAM_5, timestamp)
            stmt.executeUpdate()
        }
    }

    override fun getStarredWords(): List<UserWordState> {
        val sql =
            """
            SELECT lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            FROM user_word_state
            WHERE starred = 1
            ORDER BY last_seen_timestamp DESC
            """
                .trimIndent()

        val results = mutableListOf<UserWordState>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun getWordsByStatus(status: UserWordStatus): List<UserWordState> {
        val sql =
            """
            SELECT lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
            FROM user_word_state
            WHERE status = ?
            ORDER BY last_seen_timestamp DESC
            """
                .trimIndent()

        val results = mutableListOf<UserWordState>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, status.name)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun deleteWordState(lemma: String): Boolean {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return false

        val sql = "DELETE FROM user_word_state WHERE lemma = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(PARAM_1, normalized)
            return stmt.executeUpdate() > 0
        }
    }

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
        const val FLAG_TRUE = 1
        const val FLAG_FALSE = 0

        const val PARAM_1 = 1
        const val PARAM_2 = 2
        const val PARAM_3 = 3
        const val PARAM_4 = 4
        const val PARAM_5 = 5
        const val PARAM_6 = 6
    }
}

private fun queryChunk(
    connection: Connection,
    chunk: List<String>,
    results: MutableMap<String, UserWordState>
) {
    val placeholders = chunk.joinToString(",") { "?" }
    val sql =
        """
        SELECT lemma, status, starred, saved_context_sentence, encounter_count, last_seen_timestamp
        FROM user_word_state
        WHERE lemma IN ($placeholders)
        """
            .trimIndent()

    connection.prepareStatement(sql).use { stmt ->
        chunk.forEachIndexed { index, lemma -> stmt.setString(index + 1, lemma) }
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                val state = mapRow(rs)
                results[state.lemma] = state
            }
        }
    }
}

private fun mapRow(rs: ResultSet): UserWordState {
    val rawStatus = rs.getString("status")
    val status = runCatching { UserWordStatus.valueOf(rawStatus) }.getOrDefault(UserWordStatus.NEW)
    return UserWordState(
        lemma = rs.getString("lemma"),
        status = status,
        starred = rs.getInt("starred") == 1,
        savedContextSentence = rs.getString("saved_context_sentence"),
        encounterCount = rs.getInt("encounter_count"),
        lastSeenTimestamp = rs.getLong("last_seen_timestamp"),
    )
}
