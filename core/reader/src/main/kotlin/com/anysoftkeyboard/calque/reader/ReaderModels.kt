package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.data.models.InterlinearGlossToken
import com.anysoftkeyboard.calque.data.models.UserCustomText
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.TokenSpan
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.toolbox.CliticSurgeryResult

/** Viewing mode for the interactive reading surface. */
enum class ReaderMode {
    /** Focused single-sentence progression with large typography and progressive disclosure. */
    SENTENCE_FOCUS,

    /** Continuous paragraph presentation with active-sentence highlighting. */
    PARAGRAPH,
}

/** Source of the reading material loaded into the reader. */
sealed interface ReadingSource {
    /** A curated syntactic progression lesson with predetermined glosses and grammar callouts. */
    data class Curated(val lesson: CuratedLesson) : ReadingSource

    /** A user-imported custom reading passage stored in the user library. */
    data class CustomText(val customText: UserCustomText) : ReadingSource

    /** An ad-hoc raw text passage entered or pasted into the reader. */
    data class RawText(val title: String, val content: String) : ReadingSource
}

/** Enriched sentence model with linguistic tokens, glosses, and translation scaffolding. */
data class EnrichedSentence(
    val sentenceIndex: Int,
    val textEs: String,
    val tokens: List<TokenSpan>,
    val translationEn: String?,
    val interlinearGlosses: List<InterlinearGlossToken>,
    val grammarCallout: String? = null,
)

/** Comprehensive inspection card model for a tapped token span. */
data class TokenInspection(
    val tokenSpan: TokenSpan,
    val lemma: String,
    val pos: String?,
    val englishGloss: String?,
    val cognateRuleNote: String? = null,
    val falseCognateTrap: FalseCognateEntry? = null,
    val cliticSurgery: CliticSurgeryResult? = null,
    val userStatus: UserWordStatus = UserWordStatus.NEW,
    val isStarred: Boolean = false,
)

/** Immutable state of the interactive reader session. */
data class ReaderSessionState(
    val source: ReadingSource,
    val title: String,
    val sentences: List<EnrichedSentence>,
    val currentSentenceIndex: Int = 0,
    val readerMode: ReaderMode = ReaderMode.SENTENCE_FOCUS,
    val showInterlinear: Boolean = false,
    val showTranslation: Boolean = false,
    val selectedTokenIndex: Int? = null,
    val activeInspection: TokenInspection? = null,
    val isCompleted: Boolean = false,
) {
    /** The active sentence currently being read or highlighted. */
    val currentSentence: EnrichedSentence?
        get() = sentences.getOrNull(currentSentenceIndex)

    /** Reading progress fraction from 0.0 to 1.0. */
    val progress: Float
        get() =
            if (sentences.isEmpty()) 0f else (currentSentenceIndex + 1).toFloat() / sentences.size
}

/** End-of-session statistics and vocabulary discovery recap. */
data class SessionSummary(
    val totalSentences: Int,
    val totalTokens: Int,
    val uniqueWords: Int,
    val cognatesCount: Int,
    val trapsEncounteredCount: Int,
    val newWordsDiscoveredCount: Int,
    val isCompleted: Boolean,
)
