package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.linguistics.ReaderEnricher
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider

/** Builds and enriches [EnrichedSentence] lists for the [ReaderSessionEngine]. */
internal object SessionSentenceBuilder {

    /** Builds sentences for a [ReadingSource]. */
    fun buildSentences(
        source: ReadingSource,
        lookupProvider: WordLookupProvider,
    ): List<EnrichedSentence> =
        when (source) {
            is ReadingSource.Curated -> buildCuratedSentences(source.lesson, lookupProvider)
            is ReadingSource.CustomText ->
                buildCustomSentences(source.customText.originalSpanish, lookupProvider)
            is ReadingSource.RawText -> buildCustomSentences(source.content, lookupProvider)
        }

    private fun buildCuratedSentences(
        lesson: CuratedLesson,
        lookupProvider: WordLookupProvider,
    ): List<EnrichedSentence> {
        val lessonSentences = lesson.parseSentences()
        return lessonSentences.mapIndexed { index, item ->
            val tokens = ReaderEnricher.enrichText(item.sentenceEs, lookupProvider)
            val glosses =
                if (item.interlinearGloss.isNotEmpty()) {
                    item.interlinearGloss
                } else {
                    InterlinearAligner.generateGloss(tokens)
                }

            EnrichedSentence(
                sentenceIndex = index,
                textEs = item.sentenceEs,
                tokens = tokens,
                translationEn = item.translationEn.ifEmpty { null },
                interlinearGlosses = glosses,
                grammarCallout = item.grammarCallout,
            )
        }
    }

    private fun buildCustomSentences(
        rawText: String,
        lookupProvider: WordLookupProvider,
    ): List<EnrichedSentence> {
        val sentences = SentenceSegmenter.segment(rawText)
        return sentences.mapIndexed { index, sentenceText ->
            val tokens = ReaderEnricher.enrichText(sentenceText, lookupProvider)
            val glosses = InterlinearAligner.generateGloss(tokens)

            EnrichedSentence(
                sentenceIndex = index,
                textEs = sentenceText,
                tokens = tokens,
                translationEn = null,
                interlinearGlosses = glosses,
                grammarCallout = null,
            )
        }
    }
}
