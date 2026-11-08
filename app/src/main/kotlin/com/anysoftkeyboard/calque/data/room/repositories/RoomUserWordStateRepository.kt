package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.data.room.dao.UserWordStateDao
import com.anysoftkeyboard.calque.data.room.entities.UserWordStateEntity
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Room-backed implementation of [UserWordStateRepository] using [UserWordStateDao]. */
class RoomUserWordStateRepository(
    val dao: UserWordStateDao,
) : UserWordStateRepository {

    override fun getWordState(lemma: String): UserWordState? {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null
        return runBlocking(Dispatchers.IO) { dao.getWordState(normalized)?.toDomain() }
    }

    override fun batchGetWordStates(lemmas: Collection<String>): Map<String, UserWordState> {
        val unique =
            lemmas.map { it.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }.distinct()
        if (unique.isEmpty()) return emptyMap()

        return runBlocking(Dispatchers.IO) {
            val results = mutableMapOf<String, UserWordState>()
            for (chunk in unique.chunked(MAX_BATCH_CHUNK_SIZE)) {
                val entities = dao.batchGetWordStates(chunk)
                for (entity in entities) {
                    results[entity.lemma] = entity.toDomain()
                }
            }
            results
        }
    }

    override fun upsertWordState(state: UserWordState) {
        val normalized = state.lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        runBlocking(Dispatchers.IO) {
            dao.upsertWordState(UserWordStateEntity.fromDomain(state.copy(lemma = normalized)))
        }
    }

    override fun recordEncounter(lemma: String, timestamp: Long) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        readModifyWrite {
            val existing = dao.getWordState(normalized)
            if (existing != null) {
                dao.upsertWordState(
                    existing.copy(
                        encounterCount = existing.encounterCount + 1,
                        lastSeenTimestamp = timestamp,
                    )
                )
            } else {
                dao.upsertWordState(
                    UserWordStateEntity(
                        lemma = normalized,
                        status = UserWordStatus.LEARNING.name,
                        starred = 0,
                        savedContextSentence = null,
                        encounterCount = 1,
                        lastSeenTimestamp = timestamp,
                    )
                )
            }
        }
    }

    override fun setWordStatus(lemma: String, status: UserWordStatus, timestamp: Long) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        readModifyWrite {
            val existing = dao.getWordState(normalized)
            if (existing != null) {
                dao.upsertWordState(
                    existing.copy(
                        status = status.name,
                        lastSeenTimestamp = timestamp,
                    )
                )
            } else {
                dao.upsertWordState(
                    UserWordStateEntity(
                        lemma = normalized,
                        status = status.name,
                        starred = 0,
                        savedContextSentence = null,
                        encounterCount = 1,
                        lastSeenTimestamp = timestamp,
                    )
                )
            }
        }
    }

    override fun setStarred(
        lemma: String,
        starred: Boolean,
        contextSentence: String?,
        timestamp: Long,
    ) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        readModifyWrite {
            val existing = dao.getWordState(normalized)
            if (existing != null) {
                dao.upsertWordState(
                    existing.copy(
                        starred = if (starred) 1 else 0,
                        savedContextSentence = existing.savedContextSentence ?: contextSentence,
                        lastSeenTimestamp = timestamp,
                    )
                )
            } else {
                dao.upsertWordState(
                    UserWordStateEntity(
                        lemma = normalized,
                        status = UserWordStatus.LEARNING.name,
                        starred = if (starred) 1 else 0,
                        savedContextSentence = contextSentence,
                        encounterCount = 1,
                        lastSeenTimestamp = timestamp,
                    )
                )
            }
        }
    }

    override fun getStarredWords(): List<UserWordState> {
        return runBlocking(Dispatchers.IO) { dao.getStarredWords().map { it.toDomain() } }
    }

    override fun getWordsByStatus(status: UserWordStatus): List<UserWordState> {
        return runBlocking(Dispatchers.IO) {
            dao.getWordsByStatus(status.name).map { it.toDomain() }
        }
    }

    override fun deleteWordState(lemma: String): Boolean {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return false
        return runBlocking(Dispatchers.IO) { dao.deleteWordState(normalized) > 0 }
    }

    /**
     * Runs a read-then-write sequence on [Dispatchers.IO] while holding [writeLock], so concurrent
     * callers (for example a tap-triggered encounter and a star toggle) cannot lose each other's
     * update.
     */
    private fun readModifyWrite(block: suspend () -> Unit) {
        synchronized(writeLock) { runBlocking(Dispatchers.IO) { block() } }
    }

    private val writeLock = Any()

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
    }
}
