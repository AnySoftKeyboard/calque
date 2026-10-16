package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceScorerTest {
    private val delta = 0.0001

    @Test
    fun tokenizeWords_handlesSpanishPunctuationAndInvertedMarks() {
        val sentence = "¡Hola! ¿Cómo estás, amigo mío?"
        val tokens = SentenceScorer.tokenizeWords(sentence)

        assertEquals(listOf("hola", "cómo", "estás", "amigo", "mío"), tokens)
    }

    @Test
    fun scoreSentence_discardsSentencesWithFewerThanFourWords() {
        val shortSentence = "Hola, mi amigo."
        val result =
            SentenceScorer.scoreSentence(shortSentence) {
                ScoredWordInfo(frequencyRank = 100, isCognate = false, grammar = null)
            }

        assertEquals(3, result.wordCount)
        assertNull(result.level)
    }

    @Test
    fun scoreSentence_discardsSentencesWithMoreThanFourteenWords() {
        val longSentence =
            "Uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince."
        val result =
            SentenceScorer.scoreSentence(longSentence) {
                ScoredWordInfo(frequencyRank = 100, isCognate = false, grammar = null)
            }

        assertEquals(15, result.wordCount)
        assertNull(result.level)
    }

    @Test
    fun scoreSentence_assignsLevel0ForSimpleCognateSentences() {
        // 4 words, all common (freq rank <= 1500), 2 cognates, no complex tense:
        // score = 4 * 0.25 + 0 * 5.0 - (2/4) * 2.5 = 1.0 - 1.25 = -0.25 <= 2.0 -> LEVEL_0
        val sentence = "La música es fantástica."
        val dict =
            mapOf(
                "la" to ScoredWordInfo(frequencyRank = 5, isCognate = false),
                "música" to ScoredWordInfo(frequencyRank = 500, isCognate = true),
                "es" to ScoredWordInfo(frequencyRank = 10, isCognate = false),
                "fantástica" to ScoredWordInfo(frequencyRank = 1200, isCognate = true),
            )

        val result = SentenceScorer.scoreSentence(sentence) { dict[it] }

        assertEquals(4, result.wordCount)
        assertEquals(SentenceLevel.LEVEL_0, result.level)
        assertEquals(-0.25, result.score, delta)
        assertEquals(0.0, result.rareWordRatio, delta)
        assertEquals(0.5, result.cognateRatio, delta)
        assertFalse(result.hasComplexTense)
    }

    @Test
    fun scoreSentence_assignsLevel1ForModerateVocabulary() {
        // 8 words, 3 rare words (rank > 1500), 1 cognate:
        // rare ratio = 3/8 = 0.375
        // cognate ratio = 1/8 = 0.125
        // score = (8 * 0.25) + (0.375 * 5.0) - (0.125 * 2.5) = 2.0 + 1.875 - 0.3125 = 3.5625 <= 4.0
        // -> LEVEL_1
        val words = listOf("uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho")
        val sentence = words.joinToString(" ") + "."
        val dict =
            mapOf(
                "uno" to ScoredWordInfo(frequencyRank = 2000, isCognate = false),
                "dos" to ScoredWordInfo(frequencyRank = 3000, isCognate = false),
                "tres" to ScoredWordInfo(frequencyRank = 4000, isCognate = false),
                "cuatro" to ScoredWordInfo(frequencyRank = 100, isCognate = true),
                "cinco" to ScoredWordInfo(frequencyRank = 200, isCognate = false),
                "seis" to ScoredWordInfo(frequencyRank = 300, isCognate = false),
                "siete" to ScoredWordInfo(frequencyRank = 400, isCognate = false),
                "ocho" to ScoredWordInfo(frequencyRank = 500, isCognate = false),
            )

        val result = SentenceScorer.scoreSentence(sentence) { dict[it] }

        assertEquals(8, result.wordCount)
        assertEquals(SentenceLevel.LEVEL_1, result.level)
        assertEquals(3.5625, result.score, delta)
    }

    @Test
    fun scoreSentence_assignsLevel2ForAdvancedVocabularyWithoutComplexTense() {
        // 10 words, 5 rare (0.5), 0 cognates:
        // score = (10 * 0.25) + (0.5 * 5.0) - 0 = 2.5 + 2.5 = 5.0 <= 6.5 -> LEVEL_2
        val words = (1..10).map { "palabra$it" }
        val sentence = words.joinToString(" ") + "."
        val dict =
            words
                .mapIndexed { index, word ->
                    word to
                        ScoredWordInfo(
                            frequencyRank = if (index < 5) 5000 else 100,
                            isCognate = false,
                        )
                }
                .toMap()

        val result = SentenceScorer.scoreSentence(sentence) { dict[it] }

        assertEquals(10, result.wordCount)
        assertEquals(SentenceLevel.LEVEL_2, result.level)
        assertEquals(5.0, result.score, delta)
    }

    @Test
    fun scoreSentence_addsPenaltyForSubjunctiveOrConditionalTense() {
        // 5 words, 0 rare, 0 cognates:
        // base score = 5 * 0.25 = 1.25 (would be LEVEL_0)
        // with subjunctive: + 3.0 = 4.25 -> LEVEL_2 (since 4.0 < 4.25 <= 6.5)
        val sentence = "Espero que vengas pronto mañana."
        val dict =
            mapOf(
                "espero" to ScoredWordInfo(frequencyRank = 50, isCognate = false),
                "que" to ScoredWordInfo(frequencyRank = 2, isCognate = false),
                "vengas" to
                    ScoredWordInfo(
                        frequencyRank = 100,
                        isCognate = false,
                        grammar = "present subjunctive",
                    ),
                "pronto" to ScoredWordInfo(frequencyRank = 80, isCognate = false),
                "mañana" to ScoredWordInfo(frequencyRank = 60, isCognate = false),
            )

        val result = SentenceScorer.scoreSentence(sentence) { dict[it] }

        assertEquals(5, result.wordCount)
        assertTrue(result.hasComplexTense)
        assertEquals(4.25, result.score, delta)
        assertEquals(SentenceLevel.LEVEL_2, result.level)
    }

    @Test
    fun scoreSentence_discardsSentencesWithScoreExceedingMaxLevel() {
        // 14 words, all rare, no cognates, with complex tense:
        // score = 14 * 0.25 + 1.0 * 5.0 - 0 + 3.0 = 3.5 + 5.0 + 3.0 = 11.5 > 6.5 -> DISCARD
        val words = (1..14).map { "rara$it" }
        val sentence = words.joinToString(" ") + "."
        val dict =
            words
                .mapIndexed { index, word ->
                    word to
                        ScoredWordInfo(
                            frequencyRank = 10000,
                            isCognate = false,
                            grammar = if (index == 0) "conditional" else null,
                        )
                }
                .toMap()

        val result = SentenceScorer.scoreSentence(sentence) { dict[it] }

        assertNull(result.level)
        assertEquals(11.5, result.score, delta)
    }

    @Test
    fun scoreSentence_treatsUnrecognizedWordsAsRare() {
        // Words missing from lookup should default to rare
        val sentence = "Palabra desconocida misteriosa extraña."
        val result = SentenceScorer.scoreSentence(sentence) { null }

        assertEquals(4, result.wordCount)
        assertEquals(1.0, result.rareWordRatio, delta)
        assertEquals(0.0, result.cognateRatio, delta)
        // score = 4 * 0.25 + 1.0 * 5.0 = 6.0 <= 6.5 -> LEVEL_2
        assertEquals(6.0, result.score, delta)
        assertEquals(SentenceLevel.LEVEL_2, result.level)
    }
}
