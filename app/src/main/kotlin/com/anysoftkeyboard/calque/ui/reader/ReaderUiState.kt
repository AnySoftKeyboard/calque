package com.anysoftkeyboard.calque.ui.reader

import androidx.compose.runtime.Immutable
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.reader.EnrichedSentence
import com.anysoftkeyboard.calque.reader.ReaderMode
import com.anysoftkeyboard.calque.reader.ReadingSource
import com.anysoftkeyboard.calque.reader.TokenInspection

/** Sub-view destinations for the Reader screen. */
enum class ReaderTab {
    CURRICULUM,
    ACTIVE_SESSION,
}

/** Immutable state for an active reading session. */
@Immutable
data class ActiveSessionUiState(
    val title: String,
    val source: ReadingSource,
    val sentences: List<EnrichedSentence>,
    val currentSentenceIndex: Int = 0,
    val selectedTokenIndex: Int? = null,
    val activeInspection: TokenInspection? = null,
    val showInterlinear: Boolean = false,
    val showTranslation: Boolean = false,
    val isMirrorHeld: Boolean = false,
    val readerMode: ReaderMode = ReaderMode.SENTENCE_FOCUS,
    val isCompleted: Boolean = false,
) {
    val currentSentence: EnrichedSentence?
        get() = sentences.getOrNull(currentSentenceIndex)

    /**
     * Whether interlinear glosses are visible: pinned by the toggle or while the mirror is held.
     */
    val isGlossVisible: Boolean
        get() = showInterlinear || isMirrorHeld

    /**
     * Whether the natural translation is visible: pinned by the toggle or while the mirror is held.
     */
    val isTranslationVisible: Boolean
        get() = showTranslation || isMirrorHeld
}

/** Immutable MVI UI state for the Reader screen. */
@Immutable
data class ReaderUiState(
    val currentTab: ReaderTab = ReaderTab.CURRICULUM,
    val curriculumLessons: List<CuratedLesson> = emptyList(),
    val isLoading: Boolean = false,
    val activeSession: ActiveSessionUiState? = null,
    val errorMessage: String? = null,
)
