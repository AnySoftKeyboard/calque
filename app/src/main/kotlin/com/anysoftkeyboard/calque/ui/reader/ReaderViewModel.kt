package com.anysoftkeyboard.calque.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.di.AppContainer
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.reader.ReaderMode
import com.anysoftkeyboard.calque.reader.ReadingSource
import com.anysoftkeyboard.calque.reader.TokenInspector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MVI ViewModel for the Reader feature screen.
 *
 * Coordinates asynchronous curriculum loading, token enrichment and sentence building on
 * [Dispatchers.Default], vault updates on [Dispatchers.IO], and exposes unidirectional [StateFlow]
 * state.
 */
class ReaderViewModel(
    private val appContainer: AppContainer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState(isLoading = true))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** In-flight token inspection; cancelled whenever the selection or sentence changes. */
    private var inspectionJob: Job? = null

    init {
        loadCurriculum()
    }

    fun onEvent(event: ReaderUiEvent) {
        when (event) {
            is ReaderUiEvent.SelectLesson -> startCuratedLesson(event.lesson)
            is ReaderUiEvent.SelectLessonById -> startCuratedLessonById(event.lessonId)
            is ReaderUiEvent.StartRawText -> startRawText(event.title, event.content)
            is ReaderUiEvent.NextSentence -> nextSentence()
            is ReaderUiEvent.PreviousSentence -> previousSentence()
            is ReaderUiEvent.JumpToSentence -> jumpToSentence(event.index)
            is ReaderUiEvent.SetReaderMode -> setReaderMode(event.mode)
            is ReaderUiEvent.SelectToken -> selectToken(event.tokenIndex)
            is ReaderUiEvent.ClearSelection -> clearSelection()
            is ReaderUiEvent.ToggleInterlinear -> toggleInterlinear()
            is ReaderUiEvent.ToggleTranslation -> toggleTranslation()
            is ReaderUiEvent.SetMirrorHeld -> setMirrorHeld(event.held)
            is ReaderUiEvent.ToggleStarred -> toggleStarred(event.lemma)
            is ReaderUiEvent.UpdateWordStatus -> updateWordStatus(event.lemma, event.status)
            is ReaderUiEvent.CloseSession -> closeSession()
            is ReaderUiEvent.RefreshCurriculum -> loadCurriculum()
        }
    }

    fun loadCurriculum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val lessons =
                withContext(ioDispatcher) { appContainer.curatedLessonsRepository.getAll() }
            _uiState.update {
                it.copy(
                    curriculumLessons = lessons,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun startCuratedLesson(lesson: CuratedLesson) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val source = ReadingSource.Curated(lesson)
            val session =
                withContext(defaultDispatcher) { appContainer.createReaderSession(source) }
            val sessionState = session.state
            _uiState.update {
                it.copy(
                    currentTab = ReaderTab.ACTIVE_SESSION,
                    activeSession =
                        ActiveSessionUiState(
                            title = sessionState.title,
                            source = source,
                            sentences = sessionState.sentences,
                        ),
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun startCuratedLessonById(lessonId: String) {
        viewModelScope.launch {
            val lesson =
                withContext(ioDispatcher) {
                    appContainer.curatedLessonsRepository.getById(lessonId)
                }
            if (lesson != null) {
                startCuratedLesson(lesson)
            } else {
                _uiState.update { it.copy(errorMessage = "Lesson '$lessonId' not found.") }
            }
        }
    }

    fun startRawText(title: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val source = ReadingSource.RawText(title = title, content = content)
            val session =
                withContext(defaultDispatcher) { appContainer.createReaderSession(source) }
            val sessionState = session.state
            _uiState.update {
                it.copy(
                    currentTab = ReaderTab.ACTIVE_SESSION,
                    activeSession =
                        ActiveSessionUiState(
                            title = title,
                            source = source,
                            sentences = sessionState.sentences,
                        ),
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun closeSession() {
        inspectionJob?.cancel()
        _uiState.update {
            it.copy(currentTab = ReaderTab.CURRICULUM, activeSession = null, errorMessage = null)
        }
        loadCurriculum()
    }

    fun nextSentence() {
        inspectionJob?.cancel()
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            val nextIdx = active.currentSentenceIndex + 1
            val updated =
                if (nextIdx < active.sentences.size) {
                    active.copy(
                        currentSentenceIndex = nextIdx,
                        selectedTokenIndex = null,
                        activeInspection = null,
                        showTranslation = false,
                    )
                } else {
                    active.copy(
                        isCompleted = true,
                        selectedTokenIndex = null,
                        activeInspection = null,
                    )
                }
            state.copy(activeSession = updated)
        }
    }

    fun previousSentence() {
        inspectionJob?.cancel()
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            if (active.currentSentenceIndex > 0) {
                state.copy(
                    activeSession =
                        active.copy(
                            currentSentenceIndex = active.currentSentenceIndex - 1,
                            selectedTokenIndex = null,
                            activeInspection = null,
                            showTranslation = false,
                        ),
                )
            } else {
                state
            }
        }
    }

    fun jumpToSentence(index: Int) {
        inspectionJob?.cancel()
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            if (index in active.sentences.indices) {
                state.copy(
                    activeSession =
                        active.copy(
                            currentSentenceIndex = index,
                            selectedTokenIndex = null,
                            activeInspection = null,
                            showTranslation = false,
                        ),
                )
            } else {
                state
            }
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            state.copy(activeSession = active.copy(readerMode = mode))
        }
    }

    fun toggleInterlinear() {
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            state.copy(activeSession = active.copy(showInterlinear = !active.showInterlinear))
        }
    }

    fun toggleTranslation() {
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            state.copy(activeSession = active.copy(showTranslation = !active.showTranslation))
        }
    }

    fun setMirrorHeld(held: Boolean) {
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            if (active.isMirrorHeld == held) return@update state
            state.copy(activeSession = active.copy(isMirrorHeld = held))
        }
    }

    fun selectToken(tokenIndex: Int) {
        val active = _uiState.value.activeSession ?: return
        if (active.selectedTokenIndex == tokenIndex) {
            clearSelection()
            return
        }
        val sentence = active.currentSentence ?: return
        val token = sentence.tokens.getOrNull(tokenIndex) ?: return

        inspectionJob?.cancel()
        inspectionJob =
            viewModelScope.launch {
                val inspection =
                    withContext(defaultDispatcher) {
                        TokenInspector.inspectToken(
                            tokenSpan = token,
                            lookupProvider = appContainer.wordLookupProvider,
                            cognateRulesRepository = appContainer.cognateRulesRepository,
                            userWordStateRepository = appContainer.userWordStateRepository,
                        )
                    }
                _uiState.update { state ->
                    val curr = state.activeSession ?: return@update state
                    state.copy(
                        activeSession =
                            curr.copy(
                                selectedTokenIndex = tokenIndex,
                                activeInspection = inspection,
                            ),
                    )
                }
            }
    }

    fun clearSelection() {
        inspectionJob?.cancel()
        _uiState.update { state ->
            val active = state.activeSession ?: return@update state
            state.copy(
                activeSession =
                    active.copy(
                        selectedTokenIndex = null,
                        activeInspection = null,
                    ),
            )
        }
    }

    fun updateWordStatus(lemma: String, status: UserWordStatus) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                appContainer.userWordStateRepository.setWordStatus(lemma, status)
            }
            _uiState.update { state ->
                val active = state.activeSession ?: return@update state
                val insp = active.activeInspection
                val updatedInsp =
                    if (insp != null && insp.lemma == lemma) {
                        insp.copy(userStatus = status)
                    } else {
                        insp
                    }
                state.copy(activeSession = active.copy(activeInspection = updatedInsp))
            }
        }
    }

    fun toggleStarred(lemma: String) {
        val active = _uiState.value.activeSession ?: return
        val contextSentence = active.currentSentence?.textEs

        viewModelScope.launch {
            val newStarred =
                withContext(ioDispatcher) {
                    val current = appContainer.userWordStateRepository.getWordState(lemma)
                    val nextStarred = !(current?.starred ?: false)
                    appContainer.userWordStateRepository.setStarred(
                        lemma,
                        nextStarred,
                        contextSentence
                    )
                    nextStarred
                }
            _uiState.update { state ->
                val curr = state.activeSession ?: return@update state
                val insp = curr.activeInspection
                val updatedInsp =
                    if (insp != null && insp.lemma == lemma) {
                        insp.copy(isStarred = newStarred)
                    } else {
                        insp
                    }
                state.copy(activeSession = curr.copy(activeInspection = updatedInsp))
            }
        }
    }
}
