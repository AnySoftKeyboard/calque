package com.anysoftkeyboard.calque.ui.reader

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.di.DefaultAppContainer
import com.anysoftkeyboard.calque.di.InMemoryCognateRulesRepository
import com.anysoftkeyboard.calque.di.InMemoryCuratedLessonsRepository
import com.anysoftkeyboard.calque.di.InMemoryDictionaryRepository
import com.anysoftkeyboard.calque.di.InMemoryFalseCognatesRepository
import com.anysoftkeyboard.calque.di.InMemoryUserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.reader.ReaderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private lateinit var testDispatcher: TestDispatcher

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

    private val dictionaryEntries =
        listOf(
            DictionaryEntry(
                surfaceForm = "hotel",
                lemma = "hotel",
                pos = "noun",
                grammarInfo = "masculine",
                englishGloss = "hotel",
                isCognate = true,
                frequencyRank = 50,
            ),
            DictionaryEntry(
                surfaceForm = "excelente",
                lemma = "excelente",
                pos = "adj",
                grammarInfo = null,
                englishGloss = "excellent",
                isCognate = true,
                frequencyRank = 100,
            ),
        )

    private val cognateRules =
        listOf(
            CognateRule(
                ruleId = "tion_to_cion",
                englishSuffix = "-tion",
                spanishSuffix = "-ción",
                genderRule = "feminine",
                spellingNote = "Accent on o",
                examplesJson = "[]",
            ),
        )

    private fun createContainer(
        lessons: List<CuratedLesson> = listOf(sampleLesson),
    ): DefaultAppContainer {
        return DefaultAppContainer(
            curatedLessons = InMemoryCuratedLessonsRepository(lessons),
            dictionary = InMemoryDictionaryRepository(dictionaryEntries),
            falseCognates = InMemoryFalseCognatesRepository(emptyList()),
            cognateRules = InMemoryCognateRulesRepository(cognateRules),
            userWordState = InMemoryUserWordStateRepository(),
        )
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsCurriculumSuccessfully() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(ReaderTab.CURRICULUM, state.currentTab)
            assertEquals(1, state.curriculumLessons.size)
            assertEquals("lvl0_01_svo", state.curriculumLessons.first().lessonId)
            assertNull(state.errorMessage)
        }

    @Test
    fun onEvent_selectLesson_startsActiveSession() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()

            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ReaderTab.ACTIVE_SESSION, state.currentTab)
            assertNotNull(state.activeSession)

            val active = state.activeSession!!
            assertEquals("Basic SVO", active.title)
            assertEquals(2, active.sentences.size)
            assertEquals(0, active.currentSentenceIndex)
            assertEquals("El hotel es excelente.", active.currentSentence?.textEs)
        }

    @Test
    fun onEvent_selectLessonById_successAndFailure() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()

            viewModel.onEvent(ReaderUiEvent.SelectLessonById("lvl0_01_svo"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ReaderTab.ACTIVE_SESSION, state.currentTab)
            assertEquals("Basic SVO", state.activeSession?.title)

            viewModel.onEvent(ReaderUiEvent.SelectLessonById("non_existent_id"))
            advanceUntilIdle()

            val errorState = viewModel.uiState.value
            assertNotNull(errorState.errorMessage)
            assertTrue(errorState.errorMessage!!.contains("non_existent_id"))
        }

    @Test
    fun onEvent_startRawText_buildsEnrichedSentences() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()

            viewModel.onEvent(
                ReaderUiEvent.StartRawText(
                    title = "Raw Spanish Snippet",
                    content = "El hotel es excelente. La música es buena.",
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ReaderTab.ACTIVE_SESSION, state.currentTab)
            val active = state.activeSession
            assertNotNull(active)
            assertEquals("Raw Spanish Snippet", active!!.title)
            assertEquals(2, active.sentences.size)
        }

    @Test
    fun onEvent_sentenceNavigation_progressionAndBounds() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            // Jump to sentence 1
            viewModel.onEvent(ReaderUiEvent.JumpToSentence(1))
            assertEquals(1, viewModel.uiState.value.activeSession?.currentSentenceIndex)

            // Previous sentence
            viewModel.onEvent(ReaderUiEvent.PreviousSentence)
            assertEquals(0, viewModel.uiState.value.activeSession?.currentSentenceIndex)

            // Previous sentence when at 0 does nothing
            viewModel.onEvent(ReaderUiEvent.PreviousSentence)
            assertEquals(0, viewModel.uiState.value.activeSession?.currentSentenceIndex)

            // Next sentence advances to 1
            viewModel.onEvent(ReaderUiEvent.NextSentence)
            assertEquals(1, viewModel.uiState.value.activeSession?.currentSentenceIndex)
            assertFalse(viewModel.uiState.value.activeSession?.isCompleted == true)

            // Next sentence when at end marks completed
            viewModel.onEvent(ReaderUiEvent.NextSentence)
            assertTrue(viewModel.uiState.value.activeSession?.isCompleted == true)
        }

    @Test
    fun onEvent_togglesAndModes() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            val activeInitial = viewModel.uiState.value.activeSession!!
            assertFalse(activeInitial.showInterlinear)
            assertFalse(activeInitial.showTranslation)

            viewModel.onEvent(ReaderUiEvent.ToggleInterlinear)
            assertTrue(viewModel.uiState.value.activeSession!!.showInterlinear)

            viewModel.onEvent(ReaderUiEvent.ToggleTranslation)
            assertTrue(viewModel.uiState.value.activeSession!!.showTranslation)

            viewModel.onEvent(ReaderUiEvent.SetReaderMode(ReaderMode.PARAGRAPH))
            assertEquals(ReaderMode.PARAGRAPH, viewModel.uiState.value.activeSession!!.readerMode)
        }

    @Test
    fun onEvent_tokenSelectionAndInspection() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            val active = viewModel.uiState.value.activeSession!!
            val hotelIndex =
                active.currentSentence!!.tokens.indexOfFirst { it.text.trim() == "hotel" }
            assertTrue("hotel token should exist", hotelIndex >= 0)

            // Select "hotel" token
            viewModel.onEvent(ReaderUiEvent.SelectToken(hotelIndex))
            advanceUntilIdle()

            val activeWithToken = viewModel.uiState.value.activeSession!!
            assertEquals(hotelIndex, activeWithToken.selectedTokenIndex)
            assertNotNull(activeWithToken.activeInspection)
            assertEquals("hotel", activeWithToken.activeInspection?.lemma)

            // Clicking the same token toggles it off
            viewModel.onEvent(ReaderUiEvent.SelectToken(hotelIndex))
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.activeSession!!.selectedTokenIndex)
            assertNull(viewModel.uiState.value.activeSession!!.activeInspection)

            // Select again and clear
            viewModel.onEvent(ReaderUiEvent.SelectToken(hotelIndex))
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.ClearSelection)
            assertNull(viewModel.uiState.value.activeSession!!.selectedTokenIndex)
        }

    @Test
    fun onEvent_starredAndWordStatusUpdates() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            val active = viewModel.uiState.value.activeSession!!
            val hotelIndex =
                active.currentSentence!!.tokens.indexOfFirst { it.text.trim() == "hotel" }
            assertTrue("hotel token should exist", hotelIndex >= 0)

            // Inspect token "hotel"
            viewModel.onEvent(ReaderUiEvent.SelectToken(hotelIndex))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.activeSession!!.activeInspection?.isStarred == true)

            // Toggle starred
            viewModel.onEvent(ReaderUiEvent.ToggleStarred("hotel"))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.activeSession!!.activeInspection?.isStarred == true)
            val savedState = container.userWordStateRepository.getWordState("hotel")
            assertNotNull(savedState)
            assertTrue(savedState?.starred == true)

            // Update word status to MASTERED
            viewModel.onEvent(ReaderUiEvent.UpdateWordStatus("hotel", UserWordStatus.MASTERED))
            advanceUntilIdle()

            assertEquals(
                UserWordStatus.MASTERED,
                viewModel.uiState.value.activeSession!!.activeInspection?.userStatus
            )
            assertEquals(
                UserWordStatus.MASTERED,
                container.userWordStateRepository.getWordState("hotel")?.status
            )
        }

    @Test
    fun onEvent_closeSession_returnsToCurriculum() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            assertEquals(ReaderTab.ACTIVE_SESSION, viewModel.uiState.value.currentTab)

            viewModel.onEvent(ReaderUiEvent.CloseSession)
            advanceUntilIdle()

            assertEquals(ReaderTab.CURRICULUM, viewModel.uiState.value.currentTab)
            assertNull(viewModel.uiState.value.activeSession)
        }

    @Test
    fun onEvent_refreshCurriculum_doesNotLeaveActiveSession() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()

            viewModel.onEvent(ReaderUiEvent.RefreshCurriculum)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ReaderTab.ACTIVE_SESSION, state.currentTab)
            assertNotNull(state.activeSession)
            assertEquals(1, state.curriculumLessons.size)
        }

    @Test
    fun onEvent_sentenceChangeWhileInspecting_dropsStaleInspection() =
        runTest(testDispatcher) {
            val container = createContainer()
            val viewModel = ReaderViewModel(container, testDispatcher, testDispatcher)
            advanceUntilIdle()
            viewModel.onEvent(ReaderUiEvent.SelectLesson(sampleLesson))
            advanceUntilIdle()
            val hotelIndex =
                viewModel.uiState.value.activeSession!!.currentSentence!!.tokens.indexOfFirst {
                    it.text.trim() == "hotel"
                }

            // The inspection coroutine has been queued but not run yet.
            viewModel.onEvent(ReaderUiEvent.SelectToken(hotelIndex))
            viewModel.onEvent(ReaderUiEvent.NextSentence)
            advanceUntilIdle()

            val active = viewModel.uiState.value.activeSession!!
            assertEquals(1, active.currentSentenceIndex)
            assertNull(active.selectedTokenIndex)
            assertNull(active.activeInspection)
        }
}
