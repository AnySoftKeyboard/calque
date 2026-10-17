package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CliticSurgeryEngineTest {

    private val testLookupProvider =
        object : WordLookupProvider {
            private val dictionary =
                mapOf(
                    "explicar" to
                        DictionaryEntry(
                            surfaceForm = "explicar",
                            lemma = "explicar",
                            pos = "verb",
                            grammarInfo = "inf",
                            englishGloss = "to explain",
                            isCognate = true,
                            frequencyRank = 450,
                        ),
                    "comprar" to
                        DictionaryEntry(
                            surfaceForm = "comprar",
                            lemma = "comprar",
                            pos = "verb",
                            grammarInfo = "inf",
                            englishGloss = "to buy",
                            isCognate = false,
                            frequencyRank = 300,
                        ),
                    "diciendo" to
                        DictionaryEntry(
                            surfaceForm = "diciendo",
                            lemma = "decir",
                            pos = "verb",
                            grammarInfo = "ger",
                            englishGloss = "saying / telling",
                            isCognate = false,
                            frequencyRank = 15,
                        ),
                    "casa" to
                        DictionaryEntry(
                            surfaceForm = "casa",
                            lemma = "casa",
                            pos = "noun",
                            grammarInfo = "fem",
                            englishGloss = "house",
                            isCognate = false,
                            frequencyRank = 80,
                        ),
                )

            override fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry> =
                words.mapNotNull { w -> dictionary[w]?.let { w to it } }.toMap()

            override fun batchLookupFalseCognates(
                words: Set<String>
            ): Map<String, FalseCognateEntry> = emptyMap()

            override fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus> =
                emptyMap()

            override fun lookupSingleWord(word: String): DictionaryEntry? = dictionary[word]
        }

    @Test
    fun performSurgery_infinitiveWithTwoClitics() {
        val result = CliticSurgeryEngine.performSurgery("explicárselo", testLookupProvider)
        assertNotNull(result)
        assertEquals("explicar", result!!.rootVerb)
        assertEquals("infinitive", result.rootMood)
        assertEquals(listOf("se", "lo"), result.separatedPronouns)
        assertEquals("se lo va a explicar", result.procliticEquivalent)
        assertEquals(3, result.blocks.size) // root, se, lo
        assertEquals(PronounBlockType.ROOT_VERB, result.blocks[0].type)
        assertEquals(PronounBlockType.REFLEXIVE_PRONOUN, result.blocks[1].type)
        assertEquals(PronounBlockType.DIRECT_PRONOUN, result.blocks[2].type)
    }

    @Test
    fun performSurgery_gerundWithTwoClitics() {
        val result = CliticSurgeryEngine.performSurgery("diciéndomelo", testLookupProvider)
        assertNotNull(result)
        assertEquals("diciendo", result!!.rootVerb)
        assertEquals("gerund", result.rootMood)
        assertEquals(listOf("me", "lo"), result.separatedPronouns)
        assertEquals("me lo está diciendo", result.procliticEquivalent)
        assertEquals(PronounBlockType.INDIRECT_PRONOUN, result.blocks[1].type)
    }

    @Test
    fun performSurgery_nonVerbReturnsNull() {
        // "casa" is a noun, even if someone queried a word ending in clitic-like letters
        val result = CliticSurgeryEngine.performSurgery("casas", testLookupProvider)
        assertNull(result)
    }
}
