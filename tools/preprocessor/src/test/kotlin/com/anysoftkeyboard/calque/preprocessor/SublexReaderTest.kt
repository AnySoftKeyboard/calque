package com.anysoftkeyboard.calque.preprocessor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SublexReaderTest {

    private fun read(text: String) = SublexReader.readFrequencies(text.byteInputStream())

    @Test
    fun readFrequencies_readsTheExtractorFormatWithItsHeader() {
        val ranks = read("word\trank\tcount\nque\t1\t1404912\nde\t2\t1368419\n")

        assertEquals(mapOf("que" to 1, "de" to 2), ranks)
    }

    @Test
    fun readFrequencies_aDataRowForTheWordPalabraIsNotAHeader() {
        val ranks = read("word\trank\tcount\nque\t1\t90\npalabra\t1305\t12\nde\t1306\t11\n")

        assertEquals(mapOf("que" to 1, "palabra" to 1305, "de" to 1306), ranks)
    }

    @Test
    fun readFrequencies_palabraAsAFirstDataRowIsNotAHeaderEither() {
        val ranks = read("palabra\t7\nlibro\t8\n")

        assertEquals(mapOf("palabra" to 7, "libro" to 8), ranks)
    }

    @Test
    fun readFrequencies_aLaterHeaderLookingRowDoesNotResetTheRankColumn() {
        val ranks = read("word\tcount\trank\nque\t90\t1\nword\t80\t2\nde\t70\t3\n")

        assertEquals(mapOf("que" to 1, "word" to 2, "de" to 3), ranks)
    }

    @Test
    fun readFrequencies_acceptsAHeaderOnlyOnTheFirstLine() {
        assertEquals(mapOf("casa" to 1), read("Spelling\tRank\ncasa\t1\n"))
        assertEquals(mapOf("casa" to 1), read("palabra\trank\ncasa\t1\n"))
    }

    @Test
    fun readFrequencies_usesTheRankColumnNamedInTheHeader() {
        val ranks = read("Spelling\tFREQcount\tCD\tRank\nde\t99999\t100\t1\ncasa\t5000\t80\t150\n")

        assertEquals(mapOf("de" to 1, "casa" to 150), ranks)
    }

    @Test
    fun readFrequencies_fallsBackToTheLineNumberWhenThereIsNoRankColumn() {
        val ranks = read("de\nla\ncasa\n")

        assertEquals(mapOf("de" to 1, "la" to 2, "casa" to 3), ranks)
    }

    @Test
    fun readFrequencies_ignoresCommentsBlankLinesAndRepeatedWords() {
        val ranks = read("# comment\n\nde\t1\nde\t9\nla\t2\n")

        assertEquals(mapOf("de" to 1, "la" to 2), ranks)
    }

    @Test
    fun readFrequencies_lowercasesWords() {
        val ranks = read("Casa\t4\nÉxito\t5\n")

        assertEquals(setOf("casa", "éxito"), ranks.keys)
        assertFalse(ranks.containsKey("Casa"))
    }

    @Test
    fun readFrequencies_returnsAnEmptyMapForEmptyInput() {
        assertEquals(emptyMap<String, Int>(), read(""))
    }
}
