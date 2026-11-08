package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeReadingsLabelTest {

    private fun reading(pos: String, lemma: String, gloss: String) =
        DictionaryEntry("x", lemma, pos, null, gloss, false, null)

    @Test
    fun of_noAlternativesGivesNoLabel() {
        assertNull(AlternativeReadingsLabel.of(emptyList(), "padre"))
    }

    @Test
    fun of_spellsOutThePartOfSpeechAndOmitsTheLemmaWhenItIsTheSame() {
        val label =
            AlternativeReadingsLabel.of(
                listOf(reading("adj", "padre", "cool, acceptable")),
                "padre"
            )

        assertEquals("adjective: cool, acceptable", label)
    }

    @Test
    fun of_showsTheLemmaOfAReadingThatBelongsToAnotherWord() {
        val label = AlternativeReadingsLabel.of(listOf(reading("verb", "ir", "to go")), "ser")

        assertEquals("verb (ir): to go", label)
    }

    @Test
    fun of_theLemmaComparisonIgnoresCase() {
        val label = AlternativeReadingsLabel.of(listOf(reading("noun", "Padre", "father")), "padre")

        assertEquals("noun: father", label)
    }

    @Test
    fun of_joinsReadingsWithABulletInTheGivenOrder() {
        val label =
            AlternativeReadingsLabel.of(
                listOf(reading("adj", "padre", "cool"), reading("verb", "padrar", "to sire")),
                "padre",
            )

        assertEquals("adjective: cool • verb (padrar): to sire", label)
    }

    @Test
    fun of_showsAtMostThreeAndCountsTheRest() {
        val many = (1..6).map { reading("noun", "w$it", "gloss $it") }

        val label = AlternativeReadingsLabel.of(many, "primary")!!

        assertEquals(3, label.substringBefore("+").split(" • ").count { it.isNotBlank() })
        assertTrue(label.endsWith("+3 more"))
    }

    @Test
    fun of_dropsReadingsThatLookIdenticalToTheReader() {
        val twins = listOf(reading("noun", "padre", "father"), reading("noun", "padre", "father"))

        assertEquals("noun: father", AlternativeReadingsLabel.of(twins, "padre"))
    }

    @Test
    fun of_exactlyThreeAlternativesHaveNoMoreSuffix() {
        val three = (1..3).map { reading("noun", "w$it", "gloss $it") }

        assertTrue(!AlternativeReadingsLabel.of(three, "primary")!!.contains("more"))
    }

    @Test
    fun posName_knownAbbreviationsAreSpelledOutAndUnknownOnesKept() {
        assertEquals("adverb", AlternativeReadingsLabel.posName("adv"))
        assertEquals("interjection", AlternativeReadingsLabel.posName("intj"))
        assertEquals("noun", AlternativeReadingsLabel.posName("noun"))
        assertEquals("particle", AlternativeReadingsLabel.posName("particle"))
    }

    @Test
    fun shorten_keepsOnlyTheFirstSense() {
        assertEquals("to talk", AlternativeReadingsLabel.shorten("to talk; to speak"))
    }

    @Test
    fun shorten_cutsALongGlossOnAWordBoundaryWithAnEllipsis() {
        val long = "to look at (to try to see, to pay attention to something near you)"

        val shortened = AlternativeReadingsLabel.shorten(long)

        assertTrue(shortened.endsWith("…"))
        assertTrue(shortened.length <= AlternativeReadingsLabel.MAX_GLOSS_LENGTH + 1)
        assertTrue(long.startsWith(shortened.removeSuffix("…")))
        assertTrue(!shortened.removeSuffix("…").endsWith(" "))
    }

    @Test
    fun shorten_aGlossThatFitsIsReturnedUnchanged() {
        assertEquals("friend", AlternativeReadingsLabel.shorten("friend"))
    }
}
