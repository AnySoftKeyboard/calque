package com.anysoftkeyboard.calque.ui.reader

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.reader.ReaderMode

/** Sealed interface representing user intents for the Reader screen. */
sealed interface ReaderUiEvent {
    /** Selects and starts a curated curriculum lesson. */
    data class SelectLesson(val lesson: CuratedLesson) : ReaderUiEvent

    /** Selects and starts a curated lesson by its identifier. */
    data class SelectLessonById(val lessonId: String) : ReaderUiEvent

    /** Starts an ad-hoc reading session from pasted/raw text. */
    data class StartRawText(val title: String, val content: String) : ReaderUiEvent

    /** Advances to the next sentence in the active session. */
    data object NextSentence : ReaderUiEvent

    /** Moves back to the preceding sentence in the active session. */
    data object PreviousSentence : ReaderUiEvent

    /** Jumps to a specific sentence index. */
    data class JumpToSentence(val index: Int) : ReaderUiEvent

    /** Sets the reading layout mode. */
    data class SetReaderMode(val mode: ReaderMode) : ReaderUiEvent

    /** Selects a word token to open its contextual peek sheet. */
    data class SelectToken(val tokenIndex: Int) : ReaderUiEvent

    /** Dismisses the active word peek sheet. */
    data object ClearSelection : ReaderUiEvent

    /** Toggles the interlinear literal gloss scaffolding. */
    data object ToggleInterlinear : ReaderUiEvent

    /** Toggles visibility of the natural English translation. */
    data object ToggleTranslation : ReaderUiEvent

    /** Stars or unstars a vocabulary lemma in the learner's vault. */
    data class ToggleStarred(val lemma: String) : ReaderUiEvent

    /** Updates the mastery status of a vocabulary lemma. */
    data class UpdateWordStatus(val lemma: String, val status: UserWordStatus) : ReaderUiEvent

    /** Closes the active reading session and returns to the curriculum. */
    data object CloseSession : ReaderUiEvent

    /** Refreshes curriculum lessons from storage. */
    data object RefreshCurriculum : ReaderUiEvent
}
