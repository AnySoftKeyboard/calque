package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderEnricherTest {

    private class TestLookupProvider : WordLookupProvider {
        val dict =
            mapOf(
                "el" to DictionaryEntry("el", "el", "article", "masc sing", "the", false, 1),
                "profesor" to
                    DictionaryEntry(
                        "profesor",
                        "profesor",
                        "noun",
                        "masc sing",
                        "professor",
                        true,
                        500
                    ),
                "está" to DictionaryEntry("está", "estar", "verb", "3rd sing pres", "is", false, 5),
                "diciendo" to
                    DictionaryEntry("diciendo", "decir", "verb", "gerund", "saying", false, 50),
                "la" to DictionaryEntry("la", "la", "article", "fem sing", "the", false, 2),
                "carpeta" to
                    DictionaryEntry("carpeta", "carpeta", "noun", "fem sing", "folder", false, 300),
            )

        val falseCognates =
            mapOf(
                "carpeta" to
                    FalseCognateEntry(
                        surfaceForm = "carpeta",
                        lemma = "carpeta",
                        apparentEnglish = "carpet",
                        actualEnglish = "folder",
                        realSpanishForApparent = "alfombra",
                        severity = "HIGH_RISK",
                        disambiguationTip = "A folder, not a carpet",
                        exampleEs = "La carpeta azul.",
                        exampleEn = "The blue folder.",
                    ),
            )

        val userStates =
            mapOf(
                "profesor" to UserWordStatus.LEARNING,
                "carpeta" to UserWordStatus.MASTERED,
            )

        override fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry> =
            words.mapNotNull { dict[it]?.let { entry -> it to entry } }.toMap()

        override fun batchLookupFalseCognates(words: Set<String>): Map<String, FalseCognateEntry> =
            words.mapNotNull { falseCognates[it]?.let { trap -> it to trap } }.toMap()

        override fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus> =
            lemmas.mapNotNull { userStates[it]?.let { status -> it to status } }.toMap()

        override fun lookupSingleWord(word: String): DictionaryEntry? = dict[word]
    }

    @Test
    fun enrichText_emptyInput_returnsEmptyList() {
        val provider = TestLookupProvider()
        val spans = ReaderEnricher.enrichText("", provider)
        assertTrue(spans.isEmpty())
    }

    @Test
    fun enrichText_enrichesWordsPunctuationAndClitics() {
        val provider = TestLookupProvider()
        val text = "El profesor está diciéndomelo en la carpeta."
        val spans = ReaderEnricher.enrichText(text, provider)

        assertFalse(spans.isEmpty())

        // Verify reconstructing full text preserves exact characters
        val reconstructed = spans.joinToString("") { it.text }
        assertEquals(text, reconstructed)

        // Check "El"
        val elSpan = spans.first { it.text == "El" }
        assertTrue(elSpan.isWord)
        assertEquals("el", elSpan.normalizedWord)
        assertEquals(0, elSpan.startIndex)
        assertEquals(2, elSpan.endIndex)
        assertEquals("the", elSpan.dictionaryEntry?.englishGloss)
        assertEquals(UserWordStatus.NEW, elSpan.userStatus)

        // Check "profesor" (user state is LEARNING)
        val profSpan = spans.first { it.text == "profesor" }
        assertEquals(UserWordStatus.LEARNING, profSpan.userStatus)
        assertEquals(true, profSpan.dictionaryEntry?.isCognate)

        // Check "diciéndomelo" (deconstructed via clitic surgery)
        val cliticSpan = spans.first { it.text == "diciéndomelo" }
        assertTrue(cliticSpan.isWord)
        assertNotNull(cliticSpan.cliticBreakdown)
        assertEquals("diciendo", cliticSpan.cliticBreakdown?.rootSurface)
        assertEquals(listOf("me", "lo"), cliticSpan.cliticBreakdown?.attachedPronouns)
        assertEquals("decir", cliticSpan.dictionaryEntry?.lemma)

        // Check "carpeta" (false cognate, user state MASTERED)
        val trapSpan = spans.first { it.text == "carpeta" }
        assertNotNull(trapSpan.falseCognateEntry)
        assertEquals("carpet", trapSpan.falseCognateEntry?.apparentEnglish)
        assertEquals(UserWordStatus.MASTERED, trapSpan.userStatus)

        // Check punctuation span "."
        val periodSpan = spans.last()
        assertFalse(periodSpan.isWord)
        assertNull(periodSpan.dictionaryEntry)
        assertNull(periodSpan.normalizedWord)
        assertEquals(".", periodSpan.text)
    }
}
