package com.anysoftkeyboard.calque.linguistics

import java.text.BreakIterator
import java.util.Locale

/**
 * On-device runtime text enrichment engine for the interactive reader.
 *
 * Implements Pipeline 2: On-Device Runtime Text Ingestion from docs/INGESTION.md.
 */
object ReaderEnricher {

    private data class RawSpan(
        val text: String,
        val startIndex: Int,
        val endIndex: Int,
        val isWord: Boolean,
    )

    /**
     * Segments [rawSpanishText] and enriches every token span with dictionary, false cognate,
     * clitic pronoun deconstruction, and user learning status metadata.
     *
     * @param rawSpanishText The input Spanish sentence or article passage.
     * @param lookupProvider Provider for single-pass batch database lookups.
     * @return List of [TokenSpan]s preserving the original string characters and offsets.
     */
    fun enrichText(
        rawSpanishText: String,
        lookupProvider: WordLookupProvider,
    ): List<TokenSpan> {
        if (rawSpanishText.isEmpty()) return emptyList()

        val rawSpans = segmentRawSpans(rawSpanishText)
        val uniqueWords = extractUniqueWords(rawSpans)

        val dictMap = lookupProvider.batchLookupDictionary(uniqueWords)
        val trapMap = lookupProvider.batchLookupFalseCognates(uniqueWords)
        val lemmas = dictMap.values.map { it.lemma }.toSet()
        val userStateMap = lookupProvider.batchLookupUserState(lemmas)

        return assembleEnrichedSpans(rawSpans, dictMap, trapMap, userStateMap, lookupProvider)
    }

    private fun segmentRawSpans(text: String): List<RawSpan> {
        val iterator = BreakIterator.getWordInstance(Locale("es", "ES"))
        iterator.setText(text)
        val spans = mutableListOf<RawSpan>()

        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val spanText = text.substring(start, end)
            val isWord = spanText.any { Character.isLetter(it) }
            spans.add(RawSpan(text = spanText, startIndex = start, endIndex = end, isWord = isWord))
            start = end
            end = iterator.next()
        }
        return spans
    }

    private fun extractUniqueWords(spans: List<RawSpan>): Set<String> {
        return spans.filter { it.isWord }.map { it.text.lowercase(Locale.ROOT) }.toSet()
    }

    private fun assembleEnrichedSpans(
        rawSpans: List<RawSpan>,
        dictMap: Map<String, DictionaryEntry>,
        trapMap: Map<String, FalseCognateEntry>,
        userStateMap: Map<String, UserWordStatus>,
        lookupProvider: WordLookupProvider,
    ): List<TokenSpan> {
        return rawSpans.map { span ->
            if (!span.isWord) {
                TokenSpan(
                    text = span.text,
                    normalizedWord = null,
                    startIndex = span.startIndex,
                    endIndex = span.endIndex,
                    isWord = false,
                )
            } else {
                buildWordSpan(span, dictMap, trapMap, userStateMap, lookupProvider)
            }
        }
    }

    private fun buildWordSpan(
        span: RawSpan,
        dictMap: Map<String, DictionaryEntry>,
        trapMap: Map<String, FalseCognateEntry>,
        userStateMap: Map<String, UserWordStatus>,
        lookupProvider: WordLookupProvider,
    ): TokenSpan {
        val norm = span.text.lowercase(Locale.ROOT)
        var dictEntry = dictMap[norm]
        val trapEntry = trapMap[norm]
        var cliticBreakdown: CliticBreakdown? = null

        if (dictEntry == null) {
            cliticBreakdown = CliticDeconstructor.deconstruct(norm, lookupProvider)
            if (cliticBreakdown != null) {
                dictEntry = cliticBreakdown.rootEntry
            }
        }

        val lemma = dictEntry?.lemma
        val userStatus = (if (lemma != null) userStateMap[lemma] else null) ?: UserWordStatus.NEW

        return TokenSpan(
            text = span.text,
            normalizedWord = norm,
            startIndex = span.startIndex,
            endIndex = span.endIndex,
            isWord = true,
            dictionaryEntry = dictEntry,
            falseCognateEntry = trapEntry,
            cliticBreakdown = cliticBreakdown,
            userStatus = userStatus,
        )
    }
}
