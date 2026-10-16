package com.anysoftkeyboard.calque.linguistics

import java.text.BreakIterator
import java.util.Locale

/** Difficulty tier for curated reading lessons. */
enum class SentenceLevel {
    LEVEL_0,
    LEVEL_1,
    LEVEL_2,
}

/**
 * Word properties required for sentence scoring.
 *
 * @property frequencyRank Subtitle frequency rank (lower number = more common, e.g. 1..1500).
 * @property isCognate Whether this word is a transparent cognate with English.
 * @property grammar Morphological and grammatical tags (e.g., tense, mood).
 */
data class ScoredWordInfo(
    val frequencyRank: Int? = null,
    val isCognate: Boolean = false,
    val grammar: String? = null,
)

/** Result of scoring a Spanish sentence for lesson suitability. */
data class SentenceScoreResult(
    val level: SentenceLevel?,
    val score: Double,
    val wordCount: Int,
    val rareWordRatio: Double,
    val cognateRatio: Double,
    val hasComplexTense: Boolean,
)

/**
 * Sentence readability scorer and level bucketer.
 *
 * Implements Step 4 from INGESTION.md to evaluate sentence difficulty based on length, vocabulary
 * rarity (SUBTLEX-ESP frequency), cognate transparency, and grammatical complexity.
 */
object SentenceScorer {
    const val MIN_WORD_COUNT = 4
    const val MAX_WORD_COUNT = 14
    const val RARE_WORD_FREQ_THRESHOLD = 1500

    const val LEVEL_0_MAX_SCORE = 2.0
    const val LEVEL_1_MAX_SCORE = 4.0
    const val LEVEL_2_MAX_SCORE = 6.5

    private const val LENGTH_WEIGHT = 0.25
    private const val RARE_RATIO_WEIGHT = 5.0
    private const val COGNATE_RATIO_WEIGHT = 2.5
    private const val COMPLEX_TENSE_PENALTY = 3.0

    private val COMPLEX_TENSE_KEYWORDS =
        listOf("subjunctive", "conditional", "subjuntivo", "condicional")

    /**
     * Tokenizes a Spanish sentence into lowercased word tokens using Unicode-aware BreakIterator.
     */
    fun tokenizeWords(sentence: String, locale: Locale = Locale("es", "ES")): List<String> {
        val iterator = BreakIterator.getWordInstance(locale)
        iterator.setText(sentence)
        val words = mutableListOf<String>()

        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val token = sentence.substring(start, end).trim()
            if (token.isNotEmpty() && token.any { it.isLetter() }) {
                words.add(token.lowercase(locale))
            }
            start = end
            end = iterator.next()
        }
        return words
    }

    /**
     * Scores a Spanish sentence using the provided word lookup function.
     *
     * @param sentence The raw Spanish sentence to evaluate.
     * @param wordLookup Function providing [ScoredWordInfo] for a given lowercased word.
     * @return [SentenceScoreResult] containing the computed score, metrics, and level (or null if
     *   discarded).
     */
    fun scoreSentence(
        sentence: String,
        wordLookup: (String) -> ScoredWordInfo?,
    ): SentenceScoreResult {
        val words = tokenizeWords(sentence)
        val wordCount = words.size

        if (wordCount < MIN_WORD_COUNT || wordCount > MAX_WORD_COUNT) {
            return SentenceScoreResult(
                level = null,
                score = 0.0,
                wordCount = wordCount,
                rareWordRatio = 0.0,
                cognateRatio = 0.0,
                hasComplexTense = false,
            )
        }

        val metrics = calculateMetrics(words, wordLookup)
        var score =
            (wordCount * LENGTH_WEIGHT) + (metrics.rareWordRatio * RARE_RATIO_WEIGHT) -
                (metrics.cognateRatio * COGNATE_RATIO_WEIGHT)

        if (metrics.hasComplexTense) {
            score += COMPLEX_TENSE_PENALTY
        }

        return SentenceScoreResult(
            level = determineLevel(score),
            score = score,
            wordCount = wordCount,
            rareWordRatio = metrics.rareWordRatio,
            cognateRatio = metrics.cognateRatio,
            hasComplexTense = metrics.hasComplexTense,
        )
    }

    private fun calculateMetrics(
        words: List<String>,
        wordLookup: (String) -> ScoredWordInfo?,
    ): IntermediateMetrics {
        var rareWordCount = 0
        var cognateCount = 0
        var hasComplexTense = false

        for (word in words) {
            val info = wordLookup(word)
            if (info == null || isRare(info.frequencyRank)) {
                rareWordCount++
            }
            if (info?.isCognate == true) {
                cognateCount++
            }
            if (info?.grammar != null && isComplexTense(info.grammar)) {
                hasComplexTense = true
            }
        }

        return IntermediateMetrics(
            rareWordRatio = rareWordCount.toDouble() / words.size,
            cognateRatio = cognateCount.toDouble() / words.size,
            hasComplexTense = hasComplexTense,
        )
    }

    private fun isRare(frequencyRank: Int?): Boolean {
        return frequencyRank == null || frequencyRank > RARE_WORD_FREQ_THRESHOLD
    }

    private fun determineLevel(score: Double): SentenceLevel? {
        return when {
            score <= LEVEL_0_MAX_SCORE -> SentenceLevel.LEVEL_0
            score <= LEVEL_1_MAX_SCORE -> SentenceLevel.LEVEL_1
            score <= LEVEL_2_MAX_SCORE -> SentenceLevel.LEVEL_2
            else -> null
        }
    }

    private fun isComplexTense(grammar: String): Boolean {
        return COMPLEX_TENSE_KEYWORDS.any { keyword ->
            grammar.contains(keyword, ignoreCase = true)
        }
    }

    private data class IntermediateMetrics(
        val rareWordRatio: Double,
        val cognateRatio: Double,
        val hasComplexTense: Boolean,
    )
}
