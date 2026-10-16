package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CliticDeconstructorTest {

    private class FakeLookupProvider(
        private val dictionary: Map<String, DictionaryEntry>,
    ) : WordLookupProvider {
        override fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry> =
            words.mapNotNull { dictionary[it]?.let { entry -> it to entry } }.toMap()

        override fun batchLookupFalseCognates(words: Set<String>): Map<String, FalseCognateEntry> =
            emptyMap()

        override fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus> =
            emptyMap()

        override fun lookupSingleWord(word: String): DictionaryEntry? = dictionary[word]
    }

    private val provider =
        FakeLookupProvider(
            mapOf(
                "diciendo" to
                    DictionaryEntry(
                        surfaceForm = "diciendo",
                        lemma = "decir",
                        pos = "verb",
                        grammarInfo = "gerund",
                        englishGloss = "saying",
                        isCognate = false,
                        frequencyRank = 100,
                    ),
                "compra" to
                    DictionaryEntry(
                        surfaceForm = "compra",
                        lemma = "comprar",
                        pos = "verb",
                        grammarInfo = "imperative",
                        englishGloss = "buy",
                        isCognate = false,
                        frequencyRank = 200,
                    ),
                "dando" to
                    DictionaryEntry(
                        surfaceForm = "dando",
                        lemma = "dar",
                        pos = "verb",
                        grammarInfo = "gerund",
                        englishGloss = "giving",
                        isCognate = false,
                        frequencyRank = 80,
                    ),
                "cama" to
                    DictionaryEntry(
                        surfaceForm = "cama",
                        lemma = "cama",
                        pos = "noun",
                        grammarInfo = "noun fem",
                        englishGloss = "bed",
                        isCognate = false,
                        frequencyRank = 300,
                    ),
            ),
        )

    @Test
    fun deconstruct_compoundClitics_restoresAccentAndSplitsPronouns() {
        // diciéndomelo -> diciendo + me + lo
        val result = CliticDeconstructor.deconstruct("diciéndomelo", provider)
        assertNotNull(result)
        assertEquals("diciendo", result?.rootSurface)
        assertEquals("decir", result?.rootLemma)
        assertEquals(listOf("me", "lo"), result?.attachedPronouns)
        assertEquals("saying + [me + lo]", result?.combinedEnglishGloss)
    }

    @Test
    fun deconstruct_singleClitic_restoresAccentAndStems() {
        // cómpralo -> compra + lo
        val result = CliticDeconstructor.deconstruct("cómpralo", provider)
        assertNotNull(result)
        assertEquals("compra", result?.rootSurface)
        assertEquals(listOf("lo"), result?.attachedPronouns)
        assertEquals("buy + [lo]", result?.combinedEnglishGloss)
    }

    @Test
    fun deconstruct_compoundCliticWithNos() {
        // dándonoslo -> dando + nos + lo
        val result = CliticDeconstructor.deconstruct("dándonoslo", provider)
        assertNotNull(result)
        assertEquals("dando", result?.rootSurface)
        assertEquals(listOf("nos", "lo"), result?.attachedPronouns)
    }

    @Test
    fun deconstruct_nounEndingInPronoun_notDeconstructed() {
        // "cama" ends with "me"? No, but test an unknown word that is not a verb
        val result = CliticDeconstructor.deconstruct("camelo", provider)
        assertNull(result)
    }

    @Test
    fun deconstruct_nonMatchingWord_returnsNull() {
        val result = CliticDeconstructor.deconstruct("árbol", provider)
        assertNull(result)
    }
}
