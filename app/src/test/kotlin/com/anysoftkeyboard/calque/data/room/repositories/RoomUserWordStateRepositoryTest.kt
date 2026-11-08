package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.data.room.dao.UserWordStateDao
import com.anysoftkeyboard.calque.data.room.entities.UserWordStateEntity
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomUserWordStateRepositoryTest {

    private class FakeUserWordStateDao : UserWordStateDao {
        val rows = ConcurrentHashMap<String, UserWordStateEntity>()
        val batchSizes = mutableListOf<Int>()

        override suspend fun getWordState(lemma: String) = rows[lemma]

        override fun getWordStateFlow(lemma: String): Flow<UserWordStateEntity?> =
            flowOf(rows[lemma])

        override suspend fun batchGetWordStates(lemmas: List<String>): List<UserWordStateEntity> {
            batchSizes.add(lemmas.size)
            return lemmas.mapNotNull { rows[it] }
        }

        override suspend fun upsertWordState(state: UserWordStateEntity) {
            rows[state.lemma] = state
        }

        override suspend fun getStarredWords() =
            rows.values.filter { it.starred == 1 }.sortedByDescending { it.lastSeenTimestamp }

        override fun getStarredWordsFlow(): Flow<List<UserWordStateEntity>> = flowOf(emptyList())

        override suspend fun getWordsByStatus(status: String) =
            rows.values.filter { it.status == status }.sortedByDescending { it.lastSeenTimestamp }

        override fun getWordsByStatusFlow(status: String): Flow<List<UserWordStateEntity>> =
            flowOf(emptyList())

        override suspend fun deleteWordState(lemma: String) =
            if (rows.remove(lemma) != null) 1 else 0

        override suspend fun count() = rows.size
    }

    private val dao = FakeUserWordStateDao()
    private val repo = RoomUserWordStateRepository(dao)

    @Test
    fun recordEncounter_createsLearningRowThenIncrementsAndKeepsStar() {
        repo.recordEncounter("Comer", 100L)
        val first = repo.getWordState("comer")
        assertEquals(UserWordStatus.LEARNING, first?.status)
        assertEquals(1, first?.encounterCount)
        assertEquals(100L, first?.lastSeenTimestamp)

        repo.setStarred("comer", true, "Yo como pan.", 150L)
        repo.recordEncounter("comer", 200L)

        val second = repo.getWordState("comer")
        assertEquals(2, second?.encounterCount)
        assertEquals(200L, second?.lastSeenTimestamp)
        assertTrue(second?.starred == true)
        assertEquals("Yo como pan.", second?.savedContextSentence)
    }

    @Test
    fun setWordStatus_updatesStatusPreservingStarAndEncounters() {
        repo.recordEncounter("hotel", 1L)
        repo.setStarred("hotel", true, "El hotel es bueno.", 2L)

        repo.setWordStatus("hotel", UserWordStatus.MASTERED, 3L)

        val state = repo.getWordState("hotel")
        assertEquals(UserWordStatus.MASTERED, state?.status)
        assertTrue(state?.starred == true)
        assertEquals(1, state?.encounterCount)
        assertEquals(3L, state?.lastSeenTimestamp)
    }

    @Test
    fun setWordStatus_onUnknownLemmaCreatesRow() {
        repo.setWordStatus("nuevo", UserWordStatus.MASTERED, 5L)

        assertEquals(UserWordStatus.MASTERED, repo.getWordState("nuevo")?.status)
    }

    @Test
    fun setStarred_withoutContextKeepsPreviouslySavedSentence() {
        repo.setStarred("casa", true, "Mi casa es tu casa.", 1L)

        repo.setStarred("casa", false, null, 2L)

        val state = repo.getWordState("casa")
        assertFalse(state?.starred == true)
        assertEquals("Mi casa es tu casa.", state?.savedContextSentence)
        assertTrue(repo.getStarredWords().isEmpty())
    }

    @Test
    fun setStarred_keepsSentenceFromFirstStarAcrossRestars() {
        repo.setStarred("casa", true, "Primera oración.", 1L)
        repo.setStarred("casa", false, null, 2L)

        repo.setStarred("casa", true, "Segunda oración.", 3L)

        val state = repo.getWordState("casa")
        assertTrue(state?.starred == true)
        assertEquals("Primera oración.", state?.savedContextSentence)
    }

    @Test
    fun setStarred_recordsSentenceWhenWordWasEncounteredButNeverStarred() {
        repo.recordEncounter("casa", 1L)
        assertNull(repo.getWordState("casa")?.savedContextSentence)

        repo.setStarred("casa", true, "Mi casa es tu casa.", 2L)

        assertEquals("Mi casa es tu casa.", repo.getWordState("casa")?.savedContextSentence)
    }

    @Test
    fun blankLemmaIsIgnored() {
        repo.recordEncounter("  ", 1L)
        repo.setWordStatus("", UserWordStatus.MASTERED, 1L)
        repo.setStarred(" ", true, null, 1L)

        assertEquals(0, dao.rows.size)
        assertNull(repo.getWordState(" "))
        assertFalse(repo.deleteWordState(" "))
    }

    @Test
    fun batchGetWordStates_chunksLargeRequests() {
        val lemmas = (1..900).map { "lemma$it" }
        lemmas.forEach { repo.recordEncounter(it, 1L) }
        dao.batchSizes.clear()

        val result = repo.batchGetWordStates(lemmas + "LEMMA1")

        assertEquals(900, result.size)
        assertEquals(listOf(400, 400, 100), dao.batchSizes)
    }

    @Test
    fun getWordsByStatus_andDeleteRoundTrip() {
        repo.setWordStatus("uno", UserWordStatus.MASTERED, 1L)
        repo.setWordStatus("dos", UserWordStatus.LEARNING, 2L)

        assertEquals(listOf("uno"), repo.getWordsByStatus(UserWordStatus.MASTERED).map { it.lemma })
        assertTrue(repo.deleteWordState("UNO"))
        assertFalse(repo.deleteWordState("uno"))
    }

    @Test
    fun upsertWordState_normalizesLemma() {
        repo.upsertWordState(UserWordState(lemma = "  AMIGO ", lastSeenTimestamp = 9L))

        assertNotNull(dao.rows["amigo"])
    }

    @Test
    fun unknownStoredStatus_fallsBackToNew() {
        dao.rows["raro"] = UserWordStateEntity("raro", "NOT_A_STATUS", 0, null, 0, 1L)

        assertEquals(UserWordStatus.NEW, repo.getWordState("raro")?.status)
    }

    @Test
    fun concurrentEncounters_doNotLoseUpdates() {
        val threads = 8
        val perThread = 50
        val pool = Executors.newFixedThreadPool(threads)
        val done = CountDownLatch(threads)
        repeat(threads) {
            pool.execute {
                repeat(perThread) { repo.recordEncounter("hotel", 1L) }
                done.countDown()
            }
        }

        assertTrue(done.await(30, TimeUnit.SECONDS))
        pool.shutdown()
        assertEquals(threads * perThread, repo.getWordState("hotel")?.encounterCount)
    }
}
