package com.anysoftkeyboard.calque.reader

import java.text.BreakIterator
import java.util.Locale

/**
 * Segments continuous Spanish text into distinct sentences.
 *
 * Honors Spanish orthographic conventions (e.g., inverted question marks '¿' and exclamation marks
 * '¡') and filters out empty or whitespace-only sentences.
 */
object SentenceSegmenter {

    private val ABBREVIATIONS =
        setOf(
            "sr.",
            "sra.",
            "srta.",
            "dr.",
            "dra.",
            "prof.",
            "ej.",
            "pág.",
            "etc.",
            "art.",
            "cap.",
        )

    /**
     * Splits [text] into a list of trimmed sentence strings.
     *
     * @param text The input Spanish passage.
     * @return List of non-empty sentences.
     */
    fun segment(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val iterator = BreakIterator.getSentenceInstance(Locale("es", "ES"))
        iterator.setText(trimmed)

        val rawSentences = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val sentence = trimmed.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                rawSentences.add(sentence)
            }
            start = end
            end = iterator.next()
        }

        return mergeAbbreviationSplits(rawSentences)
    }

    private fun mergeAbbreviationSplits(sentences: List<String>): List<String> {
        if (sentences.size <= 1) return sentences

        val merged = mutableListOf<String>()
        var current = sentences[0]

        for (i in 1 until sentences.size) {
            val next = sentences[i]
            if (endsWithAbbreviation(current)) {
                current = "$current $next"
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun endsWithAbbreviation(sentence: String): Boolean {
        val lower = sentence.lowercase(Locale.ROOT)
        return ABBREVIATIONS.any { lower.endsWith(it) }
    }
}
