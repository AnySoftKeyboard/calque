package com.anysoftkeyboard.calque.ui.reader

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.di.DefaultAppContainer
import com.anysoftkeyboard.calque.di.InMemoryCuratedLessonsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderCoordinatorTest {

    private fun createTestContainer(
        lessons: List<CuratedLesson> = emptyList()
    ): DefaultAppContainer {
        return DefaultAppContainer(
            curatedLessons = InMemoryCuratedLessonsRepository(lessons),
        )
    }

    private val sampleLesson =
        CuratedLesson(
            lessonId = "lvl0_01_svo",
            level = 0,
            sequenceOrder = 1,
            title = "Basic SVO",
            targetConcept = "Subject Verb Object",
            contentJson =
                """
                [
                  {
                    "sentenceEs": "El hotel es excelente.",
                    "translationEn": "The hotel is excellent.",
                    "interlinearGloss": [
                      {"es": "El", "en": "The"},
                      {"es": "hotel", "en": "hotel"},
                      {"es": "es", "en": "is"},
                      {"es": "excelente", "en": "excellent"}
                    ],
                    "grammarCallout": "Zero-translation cognates"
                  },
                  {
                    "sentenceEs": "La música es fantástica.",
                    "translationEn": "The music is fantastic.",
                    "interlinearGloss": [
                      {"es": "La", "en": "The"},
                      {"es": "música", "en": "music"},
                      {"es": "es", "en": "is"},
                      {"es": "fantástica", "en": "fantastic"}
                    ]
                  }
                ]
                """
                    .trimIndent(),
        )

    @Test
    fun testInitialStateLoadsCurriculum() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)

        val state = coordinator.state
        assertTrue(state is ReaderScreenState.Curriculum)
        val curriculum = state as ReaderScreenState.Curriculum
        assertEquals(1, curriculum.lessons.size)
        assertEquals("lvl0_01_svo", curriculum.lessons.first().lessonId)
    }

    @Test
    fun testStartCuratedLessonTransitionsToActiveSession() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)

        coordinator.startCuratedLesson(sampleLesson)
        val state = coordinator.state
        assertTrue(state is ReaderScreenState.ActiveSession)

        val sessionState = (state as ReaderScreenState.ActiveSession).session.state
        assertEquals("Basic SVO", sessionState.title)
        assertEquals(2, sessionState.sentences.size)
        assertEquals(0, sessionState.currentSentenceIndex)
        assertEquals("El hotel es excelente.", sessionState.currentSentence?.textEs)
    }

    @Test
    fun testStartCuratedLessonById() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)

        assertTrue(coordinator.startCuratedLessonById("lvl0_01_svo"))
        assertTrue(coordinator.state is ReaderScreenState.ActiveSession)

        assertFalse(coordinator.startCuratedLessonById("non_existent_id"))
    }

    @Test
    fun testStartRawTextSnippet() {
        val container = createTestContainer()
        val coordinator = ReaderCoordinator(container)

        coordinator.startRawText("Quick Passage", "Hola mundo. Buenos días.")
        val state = coordinator.state
        assertTrue(state is ReaderScreenState.ActiveSession)

        val sessionState = (state as ReaderScreenState.ActiveSession).session.state
        assertEquals("Quick Passage", sessionState.title)
        assertEquals(2, sessionState.sentences.size)
    }

    @Test
    fun testActiveSessionProgressionAndToggles() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)
        coordinator.startCuratedLesson(sampleLesson)

        val active = (coordinator.state as ReaderScreenState.ActiveSession).session
        assertFalse(active.state.showInterlinear)
        active.toggleInterlinear()
        assertTrue(active.state.showInterlinear)

        assertFalse(active.state.showTranslation)
        active.toggleTranslation()
        assertTrue(active.state.showTranslation)

        assertEquals(0, active.state.currentSentenceIndex)
        active.nextSentence()
        assertEquals(1, active.state.currentSentenceIndex)
        assertEquals("La música es fantástica.", active.state.currentSentence?.textEs)

        active.previousSentence()
        assertEquals(0, active.state.currentSentenceIndex)
    }

    @Test
    fun testTokenSelectionAndVaultStarredToggle() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)
        coordinator.startCuratedLesson(sampleLesson)

        val active = (coordinator.state as ReaderScreenState.ActiveSession).session
        assertNull(active.state.selectedTokenIndex)
        assertNull(active.state.activeInspection)

        val hotelIndex =
            active.state.currentSentence?.tokens?.indexOfFirst {
                it.text.equals("hotel", ignoreCase = true)
            } ?: -1
        assertTrue(hotelIndex >= 0)

        active.selectToken(hotelIndex)
        assertEquals(hotelIndex, active.state.selectedTokenIndex)
        val inspection = active.state.activeInspection
        assertNotNull(inspection)
        assertEquals("hotel", inspection?.lemma)
        assertFalse(inspection?.isStarred ?: true)

        active.toggleStarred("hotel")
        val updatedInspection = active.state.activeInspection
        assertTrue(updatedInspection?.isStarred ?: false)

        active.clearSelection()
        assertNull(active.state.selectedTokenIndex)
        assertNull(active.state.activeInspection)
    }

    @Test
    fun testCloseSessionReturnsToCurriculum() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)

        coordinator.startCuratedLesson(sampleLesson)
        assertTrue(coordinator.state is ReaderScreenState.ActiveSession)

        coordinator.closeSession()
        assertTrue(coordinator.state is ReaderScreenState.Curriculum)
    }

    @Test
    fun testListenerNotifications() {
        val container = createTestContainer(listOf(sampleLesson))
        val coordinator = ReaderCoordinator(container)

        val receivedStates = mutableListOf<ReaderScreenState>()
        val listener = { s: ReaderScreenState ->
            receivedStates.add(s)
            Unit
        }
        coordinator.addListener(listener)

        coordinator.startCuratedLesson(sampleLesson)
        coordinator.closeSession()
        coordinator.removeListener(listener)
        coordinator.startCuratedLesson(sampleLesson)

        assertEquals(2, receivedStates.size)
        assertTrue(receivedStates[0] is ReaderScreenState.ActiveSession)
        assertTrue(receivedStates[1] is ReaderScreenState.Curriculum)
    }
}
