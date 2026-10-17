package com.anysoftkeyboard.calque.data.user

import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserWordStateRepositoryTest {

    private lateinit var database: UserDatabase

    @Before
    fun setUp() {
        database = UserDatabase.open()
        UserDatabase.createTables(database.connection)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getWordState_unrecordedWordReturnsNullAndDefaultStatusNew() {
        assertNull(database.wordStates.getWordState("hablar"))
        assertEquals(UserWordStatus.NEW, database.wordStates.getWordStatus("hablar"))
    }

    @Test
    fun recordEncounter_createsEntryWithStatusLearningAndIncrements() {
        val timestamp1 = 1000L
        val timestamp2 = 2000L

        database.wordStates.recordEncounter("hablar", timestamp = timestamp1)
        val state1 = database.wordStates.getWordState("hablar")
        assertNotNull(state1)
        assertEquals("hablar", state1?.lemma)
        assertEquals(UserWordStatus.LEARNING, state1?.status)
        assertFalse(state1!!.starred)
        assertEquals(1, state1.encounterCount)
        assertEquals(timestamp1, state1.lastSeenTimestamp)

        // Second encounter increments count and updates timestamp
        database.wordStates.recordEncounter("hablar", timestamp = timestamp2)
        val state2 = database.wordStates.getWordState("hablar")
        assertNotNull(state2)
        assertEquals(2, state2?.encounterCount)
        assertEquals(timestamp2, state2?.lastSeenTimestamp)
    }

    @Test
    fun setWordStatus_updatesStatusWithoutResettingEncounters() {
        database.wordStates.recordEncounter("comer", timestamp = 500L)
        database.wordStates.setWordStatus("comer", UserWordStatus.MASTERED, timestamp = 1500L)

        val state = database.wordStates.getWordState("comer")
        assertNotNull(state)
        assertEquals(UserWordStatus.MASTERED, state?.status)
        assertEquals(1, state?.encounterCount)
        assertEquals(1500L, state?.lastSeenTimestamp)
    }

    @Test
    fun setStarred_addsToVaultWithContextSentence() {
        database.wordStates.setStarred(
            lemma = "éxito",
            starred = true,
            contextSentence = "El concierto fue un gran éxito.",
            timestamp = 3000L,
        )

        val state = database.wordStates.getWordState("éxito")
        assertNotNull(state)
        assertTrue(state!!.starred)
        assertEquals("El concierto fue un gran éxito.", state.savedContextSentence)

        val starredList = database.wordStates.getStarredWords()
        assertEquals(1, starredList.size)
        assertEquals("éxito", starredList[0].lemma)

        // Unstarring retains the context sentence
        database.wordStates.setStarred(
            lemma = "éxito",
            starred = false,
            timestamp = 4000L,
        )
        val unstarred = database.wordStates.getWordState("éxito")
        assertFalse(unstarred!!.starred)
        assertEquals("El concierto fue un gran éxito.", unstarred.savedContextSentence)
        assertTrue(database.wordStates.getStarredWords().isEmpty())
    }

    @Test
    fun batchQueries_returnExpectedStatesAndStatuses() {
        database.wordStates.upsertWordState(
            UserWordState(
                lemma = "vivir",
                status = UserWordStatus.LEARNING,
                starred = false,
                encounterCount = 3,
                lastSeenTimestamp = 100L,
            )
        )
        database.wordStates.upsertWordState(
            UserWordState(
                lemma = "saber",
                status = UserWordStatus.MASTERED,
                starred = true,
                encounterCount = 10,
                lastSeenTimestamp = 200L,
            )
        )

        val states = database.wordStates.batchGetWordStates(listOf("vivir", "saber", "desconocido"))
        assertEquals(2, states.size)
        assertTrue(states.containsKey("vivir"))
        assertTrue(states.containsKey("saber"))

        val statuses =
            database.wordStates.batchGetWordStatuses(listOf("vivir", "saber", "desconocido"))
        assertEquals(3, statuses.size)
        assertEquals(UserWordStatus.LEARNING, statuses["vivir"])
        assertEquals(UserWordStatus.MASTERED, statuses["saber"])
        assertEquals(UserWordStatus.NEW, statuses["desconocido"])
    }

    @Test
    fun getWordsByStatus_filtersCorrectly() {
        database.wordStates.upsertWordState(
            UserWordState(lemma = "uno", status = UserWordStatus.LEARNING)
        )
        database.wordStates.upsertWordState(
            UserWordState(lemma = "dos", status = UserWordStatus.LEARNING)
        )
        database.wordStates.upsertWordState(
            UserWordState(lemma = "tres", status = UserWordStatus.MASTERED)
        )

        val learning = database.wordStates.getWordsByStatus(UserWordStatus.LEARNING)
        assertEquals(2, learning.size)

        val mastered = database.wordStates.getWordsByStatus(UserWordStatus.MASTERED)
        assertEquals(1, mastered.size)
        assertEquals("tres", mastered[0].lemma)
    }

    @Test
    fun deleteWordState_removesRecord() {
        database.wordStates.recordEncounter("borrar")
        assertTrue(database.wordStates.deleteWordState("borrar"))
        assertNull(database.wordStates.getWordState("borrar"))
        assertFalse(database.wordStates.deleteWordState("borrar"))
    }
}
