package com.anysoftkeyboard.calque.ui.reader

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.reader.ReaderSessionEngine

/** Screen display state for the Calque Reader tab. */
sealed interface ReaderScreenState {
    /** Browsing curriculum progression tracks and importing texts. */
    data class Curriculum(
        val lessons: List<CuratedLesson>,
    ) : ReaderScreenState

    /** Actively decoding and reading a lesson or text in the interactive reading canvas. */
    data class ActiveSession(
        val session: ReaderSessionEngine,
    ) : ReaderScreenState
}
