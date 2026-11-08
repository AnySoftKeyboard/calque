package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit of [CognateDetector] against INGESTION.md Step 2, one real word pair per rule and the
 * boundaries of every threshold. Where the implementation deliberately goes beyond the pseudocode
 * (more suffix pairs, any word of the gloss, stems of two letters) the test says so.
 */
class CognateDetectorSpecTest {

    private fun assertCognate(spanish: String, gloss: String) =
        assertTrue(
            "$spanish / $gloss should be cognates",
            CognateDetector.detectCognate(spanish, gloss)
        )

    private fun assertNotCognate(spanish: String, gloss: String) =
        assertFalse(
            "$spanish / $gloss should not be cognates",
            CognateDetector.detectCognate(spanish, gloss)
        )

    // ----- the nine suffix pairs of the specification -------------------------------------------

    @Test
    fun specSuffixPairs_eachDetectsARealCognatePair() {
        val pairs =
            listOf(
                "comunicación" to "communication", // ción / tion
                "televisión" to "television", // sión / sion
                "universidad" to "university", // dad / ty
                "rápidamente" to "rapidly", // mente / ly
                "famoso" to "famous", // oso / ous
                "histórico" to "historic", // ico / ic
                "natural" to "natural", // al / al (also the identical-word rule)
                "perfecto" to "perfect", // cto / ct
                "importancia" to "importance", // ncia / nce
            )

        for ((spanish, english) in pairs) assertCognate(spanish, english)
    }

    @Test
    fun specSuffixPairs_theSuffixesMustMatchOnBothSides() {
        assertNotCognate("comunicación", "communicate") // ción needs tion
        assertNotCognate("famoso", "famously") // oso needs ous
    }

    // ----- identical words ----------------------------------------------------------------------

    @Test
    fun identicalWords_needMoreThanThreeLetters() {
        assertCognate("real", "real") // 4 letters
        assertCognate("hotel", "hotel")
        assertNotCognate("sol", "sol") // 3 letters: too short for the identical-word rule
        assertNotCognate("mal", "mal")
    }

    @Test
    fun identicalWords_matchAfterAccentStrippingOnTheSpanishSide() {
        assertCognate("café", "cafe")
        assertCognate("bebé", "bebe")
        assertNotCognate("té", "te")
    }

    // ----- stem length and edit distance --------------------------------------------------------

    @Test
    fun stemLength_threeLettersMayDifferByOne() {
        // stems "abc" and "abd": distance 1, length 3
        assertTrue(CognateDetector.matchesCognate("abcción", "abdtion"))
    }

    @Test
    fun stemLength_twoLettersMayNotDifferEvenByOne() {
        // stems "ab" and "ac": length 2 < 3, so the fuzzy rule does not apply
        assertFalse(CognateDetector.matchesCognate("abción", "actions"))
        assertFalse(CognateDetector.matchesCognate("abción", "action"))
    }

    @Test
    fun stemLength_twoEqualLettersAreAcceptedWhereTheSpecificationWouldNeedThree() {
        // Deliberate extension: an exact stem of two letters counts ("na" + ción / "na" + tion).
        assertCognate("nación", "nation")
        assertCognate("acción", "action") // "ac" + ción
    }

    @Test
    fun stemLength_oneLetterIsTooShortEvenWhenEqual() {
        // "a" + ción against "a" + tion: equal stems, but a single letter says nothing.
        assertFalse(CognateDetector.matchesCognate("ación", "ation"))
    }

    @Test
    fun editDistance_oneIsAcceptedAndTwoIsNot() {
        assertTrue(CognateDetector.matchesCognate("abcción", "abdtion")) // abc vs abd
        assertFalse(CognateDetector.matchesCognate("abcción", "adetion")) // abc vs ade
        assertEquals(1, CognateDetector.levenshteinDistance("abc", "abd"))
        assertEquals(2, CognateDetector.levenshteinDistance("abc", "ade"))
    }

    @Test
    fun editDistance_countsInsertionsAndDeletionsToo() {
        assertEquals(1, CognateDetector.levenshteinDistance("comunica", "communica"))
        assertEquals(3, CognateDetector.levenshteinDistance("kitten", "sitting"))
        assertEquals(0, CognateDetector.levenshteinDistance("", ""))
        assertEquals(4, CognateDetector.levenshteinDistance("", "casa"))
    }

    @Test
    fun accents_areStrippedFromTheSpanishStemOnly() {
        assertCognate("rápidamente", "rapidly") // stem "rápida" -> "rapida" vs "rapid"
        assertCognate("histórico", "historic")
    }

    // ----- the English gloss --------------------------------------------------------------------

    @Test
    fun gloss_parentheticalNotesAndLeadingParticlesAreIgnored() {
        assertCognate("decidir", "to decide (to make up one's mind)")
        assertCognate("animal", "an animal [zool.]")
        assertNotCognate("casa", "(a house) building")
    }

    @Test
    fun gloss_everySynonymIsTriedNotOnlyThePrimaryWord() {
        // Deliberate extension: the pseudocode checks only extract_primary_word(gloss).
        assertEquals("quickly", CognateDetector.extractPrimaryWord("quickly, rapidly"))
        assertCognate("rápidamente", "quickly, rapidly")
        assertCognate("rápidamente", "quickly; rapidly")
        assertCognate("rápidamente", "quickly / rapidly")
    }

    @Test
    fun gloss_anEmptyOrUnusableGlossIsNeverACognate() {
        assertNotCognate("comunicación", "")
        assertNotCognate("comunicación", "   ")
        assertNotCognate("comunicación", "(communication)")
    }

    // ----- suffix pairs beyond the specification ------------------------------------------------

    @Test
    fun extendedSuffixPairs_detectTheCommonVerbAndAdjectivePatterns() {
        val pairs =
            listOf(
                "realizar" to "to realize", // izar / ize
                "separar" to "to separate", // ar / ate
                "decidir" to "to decide", // ir / e
                "activo" to "active", // ivo / ive
                "necesario" to "necessary", // ario / ary
                "posible" to "possible", // ble / ble
                "doctor" to "doctor", // or / or
                "cultura" to "culture", // ura / ure
            )

        for ((spanish, english) in pairs) assertCognate(spanish, english)
    }

    // ----- words that must not match ------------------------------------------------------------

    @Test
    fun shortWords_areNeverCognates() {
        assertNotCognate("el", "el")
        assertNotCognate("de", "of")
        assertNotCognate("no", "no")
        assertFalse(CognateDetector.matchesCognate("ya", "ya"))
    }

    @Test
    fun falseFriendsAndUnrelatedWords_areNotCognates() {
        assertNotCognate("casa", "house")
        assertNotCognate("perro", "dog")
        assertNotCognate("embarazada", "embarrassed") // a classic false friend that looks close
    }

    @Test
    fun normalize_lowercasesAndTrimsNonLetterEdges() {
        assertEquals("natural", CognateDetector.normalize("  Natural.  "))
        assertEquals("café", CognateDetector.normalize("¡Café!"))
    }
}
