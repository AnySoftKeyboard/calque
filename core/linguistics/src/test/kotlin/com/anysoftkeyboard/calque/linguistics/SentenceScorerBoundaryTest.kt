package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Boundary and formula checks for INGESTION.md Step 4: `score = words * 0.25 + rareRatio * 5.0 -
 * cognateRatio * 2.5`, `+3.0` for a complex tense, levels at 2.0 / 4.0 / 6.5 (inclusive), 4 to 14
 * words, rare when the rank is missing or above 1500.
 */
class SentenceScorerBoundaryTest {

    private val vocabulary =
        listOf(
            "el",
            "gato",
            "come",
            "pescado",
            "fresco",
            "en",
            "la",
            "casa",
            "grande",
            "de",
            "mi",
            "amigo",
            "muy",
            "viejo"
        )

    private fun sentence(wordCount: Int) = vocabulary.take(wordCount).joinToString(" ") + "."

    /** A lookup in which the first [rare] words are rare and the next [cognates] are cognates. */
    private fun lookup(
        rare: Int = 0,
        cognates: Int = 0,
        rank: Int = 100,
        grammarOfFirst: String? = null,
    ): (String) -> ScoredWordInfo? = { word ->
        val index = vocabulary.indexOf(word)
        ScoredWordInfo(
            frequencyRank = if (index < rare) RARE_RANK else rank,
            isCognate = index in rare until rare + cognates,
            grammar = if (index == 0) grammarOfFirst else null,
        )
    }

    private fun score(words: Int, lookup: (String) -> ScoredWordInfo?) =
        SentenceScorer.scoreSentence(sentence(words), lookup)

    // ----- word count ---------------------------------------------------------------------------

    @Test
    fun wordCount_fourAndFourteenAreScoredAndThreeAndFifteenAreNot() {
        assertEquals(SentenceLevel.LEVEL_0, score(4, lookup()).level)
        assertEquals(SentenceLevel.LEVEL_1, score(14, lookup()).level)
        assertNull(SentenceScorer.scoreSentence("el gato come", lookup()).level)
        assertNull(
            SentenceScorer.scoreSentence(vocabulary.joinToString(" ") + " mucho", lookup()).level
        )
    }

    @Test
    fun wordCount_aDiscardedSentenceReportsItsCountAndNoMetrics() {
        val result = SentenceScorer.scoreSentence("el gato come", lookup(rare = 3))

        assertEquals(3, result.wordCount)
        assertEquals(0.0, result.score, 0.0)
        assertEquals(0.0, result.rareWordRatio, 0.0)
    }

    // ----- formula ------------------------------------------------------------------------------

    @Test
    fun formula_lengthAloneIsAQuarterPerWord() {
        assertEquals(2.0, score(8, lookup()).score, EPSILON)
        assertEquals(3.5, score(14, lookup()).score, EPSILON)
    }

    @Test
    fun formula_rareWordsAddFivePerRatio() {
        // 10 words, 2 rare: 2.5 + 0.2 * 5.0
        assertEquals(3.5, score(10, lookup(rare = 2)).score, EPSILON)
    }

    @Test
    fun formula_cognatesSubtractTwoAndAHalfPerRatio() {
        // 10 words, 4 cognates: 2.5 - 0.4 * 2.5
        assertEquals(1.5, score(10, lookup(cognates = 4)).score, EPSILON)
    }

    @Test
    fun formula_aComplexTenseAddsExactlyThree() {
        val plain = score(8, lookup()).score
        val subjunctive = score(8, lookup(grammarOfFirst = "present subjunctive")).score

        assertEquals(3.0, subjunctive - plain, EPSILON)
    }

    @Test
    fun formula_complexTenseKeywordsMatchIgnoringCaseInEitherLanguage() {
        for (grammar in
            listOf(
                "Subjunctive",
                "CONDITIONAL",
                "subjuntivo",
                "Condicional",
                "2nd sg. conditional"
            )) {
            val result = score(8, lookup(grammarOfFirst = grammar))
            assertEquals(grammar, true, result.hasComplexTense)
        }
        for (grammar in listOf("present", "imperfect indicative", "preterite", "")) {
            assertEquals(grammar, false, score(8, lookup(grammarOfFirst = grammar)).hasComplexTense)
        }
    }

    @Test
    fun formula_theTenseBonusIsAppliedOnceNoMatterHowManyWordsHaveIt() {
        val every: (String) -> ScoredWordInfo? = { ScoredWordInfo(100, false, "subjunctive") }

        assertEquals(2.0 + 3.0, SentenceScorer.scoreSentence(sentence(8), every).score, EPSILON)
    }

    // ----- rare words ---------------------------------------------------------------------------

    @Test
    fun rare_aRankOf1500IsCommonAndOf1501IsRare() {
        assertEquals(0.0, score(10, lookup(rare = 0, rank = 1500)).rareWordRatio, 0.0)
        val allRare = score(10, lookup(rare = 0, rank = 1501))
        assertEquals(1.0, allRare.rareWordRatio, 0.0)
    }

    @Test
    fun rare_aMissingRankAndAnUnknownWordAreRare() {
        val noRank: (String) -> ScoredWordInfo? = { ScoredWordInfo(frequencyRank = null) }
        val unknown: (String) -> ScoredWordInfo? = { null }

        assertEquals(1.0, SentenceScorer.scoreSentence(sentence(8), noRank).rareWordRatio, 0.0)
        assertEquals(1.0, SentenceScorer.scoreSentence(sentence(8), unknown).rareWordRatio, 0.0)
    }

    // ----- level thresholds ---------------------------------------------------------------------

    @Test
    fun level_aScoreOfExactlyTwoIsLevelZeroAndJustOverIsLevelOne() {
        assertEquals(SentenceLevel.LEVEL_0, score(8, lookup()).level) // 2.0
        assertEquals(SentenceLevel.LEVEL_1, score(9, lookup()).level) // 2.25
    }

    @Test
    fun level_aScoreOfExactlyFourIsLevelOneAndJustOverIsLevelTwo() {
        // 10 words: 2.5 + rare * 0.5
        assertEquals(4.0, score(10, lookup(rare = 3)).score, EPSILON)
        assertEquals(SentenceLevel.LEVEL_1, score(10, lookup(rare = 3)).level)
        assertEquals(SentenceLevel.LEVEL_2, score(10, lookup(rare = 4)).level)
    }

    @Test
    fun level_aScoreOfExactlySixAndAHalfIsLevelTwoAndJustOverIsDiscarded() {
        assertEquals(6.5, score(10, lookup(rare = 8)).score, EPSILON)
        assertEquals(SentenceLevel.LEVEL_2, score(10, lookup(rare = 8)).level)
        assertNull(score(10, lookup(rare = 9)).level)
    }

    @Test
    fun level_theComplexTenseBonusCanMoveASentenceUpALevel() {
        assertEquals(SentenceLevel.LEVEL_0, score(4, lookup()).level) // 1.0
        assertEquals(
            SentenceLevel.LEVEL_1,
            score(4, lookup(grammarOfFirst = "subjunctive")).level
        ) // 4.0
    }

    @Test
    fun level_cognatesCanMoveASentenceDownALevel() {
        // 10 words, 2 rare: 2.5 + 1.0 = 3.5 (level 1); with 8 cognates: 3.5 - 2.0 = 1.5 (level 0)
        assertEquals(SentenceLevel.LEVEL_1, score(10, lookup(rare = 2)).level)
        assertEquals(SentenceLevel.LEVEL_0, score(10, lookup(rare = 2, cognates = 8)).level)
    }

    private companion object {
        const val RARE_RANK = 5000
        const val EPSILON = 1e-9
    }
}
