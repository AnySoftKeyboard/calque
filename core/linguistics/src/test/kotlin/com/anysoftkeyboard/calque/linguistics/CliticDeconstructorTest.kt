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

    private fun reading(pos: String, lemma: String, gloss: String) =
        DictionaryEntry("mira", lemma, pos, null, gloss, false, 100)

    @Test
    fun deconstruct_usesAVerbReadingOfARootWhosePrimaryReadingIsNotAVerb() {
        // "mira" is primarily a noun (a sight) but also the imperative of mirar.
        val root =
            reading("noun", "mira", "sight")
                .copy(alternatives = listOf(reading("verb", "mirar", "look")))
        val result =
            CliticDeconstructor.deconstruct("míralo", FakeLookupProvider(mapOf("mira" to root)))

        assertNotNull(result)
        assertEquals("mirar", result?.rootLemma)
        assertEquals("look + [lo]", result?.combinedEnglishGloss)
        assertEquals("verb", result?.rootEntry?.pos)
        assertEquals(listOf("lo"), result?.attachedPronouns)
    }

    @Test
    fun deconstruct_aRootWithOnlyNonVerbReadingsIsNotDeconstructed() {
        val root =
            reading("noun", "mira", "sight")
                .copy(alternatives = listOf(reading("adj", "mira", "watchful")))

        assertNull(
            CliticDeconstructor.deconstruct("míralo", FakeLookupProvider(mapOf("mira" to root)))
        )
    }

    @Test
    fun deconstruct_prefersThePrimaryReadingWhenItIsAlreadyAVerb() {
        val root =
            reading("verb", "mirar", "look")
                .copy(alternatives = listOf(reading("verb", "mirarse", "look at oneself")))

        val result =
            CliticDeconstructor.deconstruct("míralo", FakeLookupProvider(mapOf("mira" to root)))

        assertEquals("mirar", result?.rootLemma)
    }

    @Test
    fun dictionaryEntry_readingsListThePrimaryFirstWithoutNestingAlternatives() {
        val primary = reading("noun", "mira", "sight")
        val alternative = reading("verb", "mirar", "look")
        val entry = primary.copy(alternatives = listOf(alternative))

        assertEquals(listOf(primary, alternative), entry.readings)
        assertEquals(listOf(primary), primary.readings)
    }
}
