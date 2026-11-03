package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider

/**
 * State machine managing an interactive Calque reading session.
 *
 * Coordinates sentence progression, token inspection, interlinear gloss and full translation
 * toggling, and vocabulary vault persistence.
 */
class ReaderSessionEngine(
    val source: ReadingSource,
    private val lookupProvider: WordLookupProvider,
    private val cognateRulesRepository: CognateRulesRepository? = null,
    private val userWordStateRepository: UserWordStateRepository? = null,
) {

    private val title: String =
        when (source) {
            is ReadingSource.Curated -> source.lesson.title
            is ReadingSource.CustomText -> source.customText.title
            is ReadingSource.RawText -> source.title
        }

    private var internalState =
        ReaderSessionState(
            source = source,
            title = title,
            sentences = SessionSentenceBuilder.buildSentences(source, lookupProvider),
        )

    val state: ReaderSessionState
        get() = internalState

    /** Advances to the next sentence or marks the session completed. */
    fun nextSentence() {
        val nextIdx = internalState.currentSentenceIndex + 1
        internalState =
            if (nextIdx < internalState.sentences.size) {
                internalState.copy(
                    currentSentenceIndex = nextIdx,
                    selectedTokenIndex = null,
                    activeInspection = null,
                    showTranslation = false,
                )
            } else {
                internalState.copy(
                    isCompleted = true,
                    selectedTokenIndex = null,
                    activeInspection = null,
                )
            }
    }

    /** Moves back to the preceding sentence if available. */
    fun previousSentence() {
        if (internalState.currentSentenceIndex > 0) {
            internalState =
                internalState.copy(
                    currentSentenceIndex = internalState.currentSentenceIndex - 1,
                    selectedTokenIndex = null,
                    activeInspection = null,
                    showTranslation = false,
                )
        }
    }

    /** Jumps directly to sentence at [index]. */
    fun jumpToSentence(index: Int) {
        if (index in internalState.sentences.indices) {
            internalState =
                internalState.copy(
                    currentSentenceIndex = index,
                    selectedTokenIndex = null,
                    activeInspection = null,
                    showTranslation = false,
                )
        }
    }

    /** Switches between sentence focus mode and continuous paragraph mode. */
    fun setReaderMode(mode: ReaderMode) {
        internalState = internalState.copy(readerMode = mode)
    }

    /** Toggles the interlinear word-for-word gloss overlay. */
    fun toggleInterlinear() {
        internalState = internalState.copy(showInterlinear = !internalState.showInterlinear)
    }

    /** Toggles visibility of the full sentence translation scaffolding. */
    fun toggleTranslation() {
        internalState = internalState.copy(showTranslation = !internalState.showTranslation)
    }

    /** Selects or deselects a token span in the active sentence and opens its inspection card. */
    fun selectToken(tokenIndex: Int) {
        if (internalState.selectedTokenIndex == tokenIndex) {
            clearSelection()
            return
        }

        val sentence = internalState.currentSentence ?: return
        val token = sentence.tokens.getOrNull(tokenIndex) ?: return

        val inspection =
            TokenInspector.inspectToken(
                tokenSpan = token,
                lookupProvider = lookupProvider,
                cognateRulesRepository = cognateRulesRepository,
                userWordStateRepository = userWordStateRepository,
            )

        internalState =
            internalState.copy(
                selectedTokenIndex = tokenIndex,
                activeInspection = inspection,
            )
    }

    /** Dismisses the active token selection and inspection card. */
    fun clearSelection() {
        internalState =
            internalState.copy(
                selectedTokenIndex = null,
                activeInspection = null,
            )
    }

    /** Updates the learning status of [lemma] in the vault. */
    fun updateWordStatus(lemma: String, status: UserWordStatus) {
        userWordStateRepository?.setWordStatus(lemma, status)
        val currentInspection = internalState.activeInspection
        if (currentInspection != null && currentInspection.lemma == lemma) {
            internalState =
                internalState.copy(
                    activeInspection = currentInspection.copy(userStatus = status),
                )
        }
    }

    /** Toggles the starred state of [lemma] in the user's vault. */
    fun toggleStarred(lemma: String) {
        val currentState = userWordStateRepository?.getWordState(lemma)
        val newStarred = !(currentState?.starred ?: false)
        val contextSentence = internalState.currentSentence?.textEs
        userWordStateRepository?.setStarred(lemma, newStarred, contextSentence)

        val currentInspection = internalState.activeInspection
        if (currentInspection != null && currentInspection.lemma == lemma) {
            internalState =
                internalState.copy(
                    activeInspection = currentInspection.copy(isStarred = newStarred),
                )
        }
    }
}

/** Computes summary metrics across the reading session. */
fun ReaderSessionEngine.getSessionSummary(): SessionSummary {
    val allTokens = state.sentences.flatMap { it.tokens }
    val wordTokens = allTokens.filter { it.isWord }
    val uniqueWords = wordTokens.mapNotNull { it.normalizedWord }.toSet().size
    val cognatesCount = wordTokens.count { it.dictionaryEntry?.isCognate == true }
    val trapsCount = wordTokens.count { it.falseCognateEntry != null }
    val newWordsCount = wordTokens.count { it.userStatus == UserWordStatus.NEW }

    return SessionSummary(
        totalSentences = state.sentences.size,
        totalTokens = allTokens.size,
        uniqueWords = uniqueWords,
        cognatesCount = cognatesCount,
        trapsEncounteredCount = trapsCount,
        newWordsDiscoveredCount = newWordsCount,
        isCompleted = state.isCompleted,
    )
}
