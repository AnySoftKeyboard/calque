package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.linguistics.CliticBreakdown
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.TokenSpan
import org.junit.Assert.assertEquals
import org.junit.Test

class InterlinearAlignerTest {

    @Test
    fun generateGloss_alignsWithTokens() {
        val entry1 = createDictEntry("el", "the")
        val entry2 = createDictEntry("libro", "book; volume")

        val tokens =
            listOf(
                createWordToken("El", entry = entry1),
                TokenSpan(" ", null, 2, 3, false),
                createWordToken("libro", entry = entry2),
                TokenSpan(".", null, 8, 9, false),
            )

        val glosses = InterlinearAligner.generateGloss(tokens)

        assertEquals(4, glosses.size)
        assertEquals("the", glosses[0].en)
        assertEquals("", glosses[1].en)
        assertEquals("book", glosses[2].en)
        assertEquals("", glosses[3].en)
    }

    @Test
    fun generateGloss_prioritizesFalseCognateActualMeaning() {
        val trap =
            FalseCognateEntry(
                surfaceForm = "carpeta",
                lemma = "carpeta",
                apparentEnglish = "carpet",
                actualEnglish = "folder / binder",
                realSpanishForApparent = "alfombra",
                severity = "HIGH_RISK",
                disambiguationTip = "Carpeta is a binder.",
                exampleEs = "En la carpeta.",
                exampleEn = "In the folder.",
            )

        val tokens = listOf(createWordToken("carpeta", trap = trap))
        val glosses = InterlinearAligner.generateGloss(tokens)
        assertEquals("folder", glosses[0].en)
    }

    @Test
    fun generateGloss_usesCliticCombinedGloss() {
        val rootEntry = createDictEntry("dar", "to give")
        val clitic =
            CliticBreakdown(
                rootSurface = "dár",
                rootLemma = "dar",
                attachedPronouns = listOf("me", "lo"),
                combinedEnglishGloss = "give it to me",
                rootEntry = rootEntry,
            )

        val tokens = listOf(createWordToken("dármelo", clitic = clitic))
        val glosses = InterlinearAligner.generateGloss(tokens)
        assertEquals("give it to me", glosses[0].en)
    }

    private fun createDictEntry(lemma: String, gloss: String) =
        DictionaryEntry(
            surfaceForm = lemma,
            lemma = lemma,
            pos = "word",
            grammarInfo = null,
            englishGloss = gloss,
            isCognate = false,
            frequencyRank = 1,
        )

    private fun createWordToken(
        text: String,
        entry: DictionaryEntry? = null,
        trap: FalseCognateEntry? = null,
        clitic: CliticBreakdown? = null,
    ) =
        TokenSpan(
            text = text,
            normalizedWord = text.lowercase(),
            startIndex = 0,
            endIndex = text.length,
            isWord = true,
            dictionaryEntry = entry,
            falseCognateEntry = trap,
            cliticBreakdown = clitic,
        )
}
