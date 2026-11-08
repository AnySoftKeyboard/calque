package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.stream.XMLStreamException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SublexExtractorTest {

    private val xmlHeader = """<?xml version="1.0" encoding="UTF-8"?>"""

    private fun sharedStrings(vararg items: String): String =
        "$xmlHeader<sst>${items.joinToString("") { "<si>$it</si>" }}</sst>"

    private fun text(value: String) = "<t>$value</t>"

    private fun sheet(vararg rows: String): String =
        "$xmlHeader<worksheet><sheetData>${rows.joinToString("")}</sheetData></worksheet>"

    private fun row(index: Int, vararg cells: String) =
        "<row r=\"$index\">${cells.joinToString("")}</row>"

    private fun stringCell(ref: String, sharedIndex: Int) =
        "<c r=\"$ref\" t=\"s\"><v>$sharedIndex</v></c>"

    private fun numberCell(ref: String, value: String) = "<c r=\"$ref\"><v>$value</v></c>"

    private fun workbook(sharedStrings: String, sheet: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStrings.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheet.toByteArray())
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    @Test
    fun readCounts_readsPairsFromAllColumnGroupsAndSkipsTheHeaderRow() {
        // Shared strings: 0 Word, 1 Freq, 2 de, 3 casa, 4 perro
        val strings =
            sharedStrings(text("Word"), text("Freq"), text("de"), text("casa"), text("perro"))
        val sheet =
            sheet(
                row(
                    1,
                    stringCell("A1", 0),
                    stringCell("B1", 1),
                    stringCell("F1", 0),
                    stringCell("G1", 1)
                ),
                row(
                    2,
                    stringCell("A2", 2),
                    numberCell("B2", "900"),
                    numberCell("C2", "21.5"),
                    stringCell("F2", 3),
                    numberCell("G2", "50"),
                    stringCell("K2", 4),
                    numberCell("L2", "7"),
                ),
            )

        val counts = SublexXlsxReader.readCounts(workbook(strings, sheet).inputStream())

        assertEquals(
            listOf(SublexCount("de", 900), SublexCount("casa", 50), SublexCount("perro", 7)),
            counts,
        )
    }

    @Test
    fun readCounts_concatenatesRichTextRuns() {
        val strings = sharedStrings("<r>${text("ca")}</r><r>${text("sa")}</r>")
        val sheet = sheet(row(1, stringCell("A1", 0), numberCell("B1", "12.0")))

        val counts = SublexXlsxReader.readCounts(workbook(strings, sheet).inputStream())

        assertEquals(listOf(SublexCount("casa", 12)), counts)
    }

    @Test
    fun readCounts_ignoresWordsNotFollowedByANumberInTheNextColumn() {
        val strings = sharedStrings(text("hola"), text("adiós"))
        val sheet =
            sheet(
                row(1, stringCell("A1", 0), stringCell("B1", 1)),
                // The count sits two columns after the word, so it is not a pair.
                row(2, stringCell("A2", 0), numberCell("C2", "5")),
            )

        val counts = SublexXlsxReader.readCounts(workbook(strings, sheet).inputStream())

        assertTrue(counts.isEmpty())
    }

    @Test
    fun readCounts_rejectsAWorkbookWithoutTheExpectedSheet() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStrings(text("a")).toByteArray())
            zip.closeEntry()
        }

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                SublexXlsxReader.readCounts(bytes.toByteArray().inputStream())
            }

        assertTrue(error.message!!.contains("xl/worksheets/sheet1.xml"))
    }

    @Test
    fun readCounts_doesNotResolveExternalEntities() {
        val strings =
            """<?xml version="1.0"?><!DOCTYPE sst [<!ENTITY secret SYSTEM "file:///etc/passwd">]>""" +
                "<sst><si><t>&secret;</t></si></sst>"
        val sheet = sheet(row(1, stringCell("A1", 0), numberCell("B1", "1")))

        val outcome = runCatching {
            SublexXlsxReader.readCounts(workbook(strings, sheet).inputStream())
        }

        // Either the parser refuses the entity or it leaves it unresolved; the file is never read.
        val words = outcome.getOrNull().orEmpty().map { it.word }
        assertTrue(outcome.exceptionOrNull() is XMLStreamException || words.none { "root" in it })
    }

    @Test
    fun rank_ordersByCountDescendingWithAlphabeticalTieBreak() {
        val ranked =
            SublexExtractor.rank(
                listOf(
                    SublexCount("gato", 10),
                    SublexCount("de", 500),
                    SublexCount("casa", 10),
                    SublexCount("la", 400),
                ),
                limit = 10,
            )

        assertEquals(listOf("de", "la", "casa", "gato"), ranked.map { it.word })
        assertEquals(listOf(1, 2, 3, 4), ranked.map { it.rank })
        assertEquals(500L, ranked.first().count)
    }

    @Test
    fun rank_mergesCaseVariantsAndLowercases() {
        val ranked =
            SublexExtractor.rank(
                listOf(SublexCount("Aarón", 3), SublexCount("aarón", 4), SublexCount("de", 5)),
                limit = 10,
            )

        assertEquals(listOf("aarón", "de"), ranked.map { it.word })
        assertEquals(7L, ranked.first().count)
    }

    @Test
    fun rank_normalizesDecomposedAccentsToNfc() {
        val decomposed = "cafe\u0301"
        val ranked =
            SublexExtractor.rank(listOf(SublexCount(decomposed, 2), SublexCount("café", 3)), 10)

        assertEquals(listOf("café"), ranked.map { it.word })
        assertEquals(5L, ranked.single().count)
    }

    @Test
    fun rank_dropsNonWordTokensAndNonPositiveCounts() {
        val ranked =
            SublexExtractor.rank(
                listOf(
                    SublexCount("123", 100),
                    SublexCount("¿qué", 90),
                    SublexCount("anti-aéreo", 80),
                    SublexCount("", 70),
                    SublexCount("   ", 60),
                    SublexCount("cero", 0),
                    SublexCount("negativo", -4),
                    SublexCount("pingüino", 5),
                    SublexCount("ñandú", 4),
                ),
                limit = 10,
            )

        assertEquals(listOf("pingüino", "ñandú"), ranked.map { it.word })
    }

    @Test
    fun rank_keepsOnlyTheTopLimitWords() {
        val counts = (1..10).map { SublexCount("palabra${'a' + it}", it.toLong()) }

        val ranked = SublexExtractor.rank(counts, limit = 3)

        assertEquals(3, ranked.size)
        assertEquals(listOf(10L, 9L, 8L), ranked.map { it.count })
    }

    @Test
    fun rank_returnsEverythingWhenFewerWordsThanLimit() {
        assertEquals(1, SublexExtractor.rank(listOf(SublexCount("sol", 1)), limit = 25_000).size)
    }

    @Test
    fun rank_rejectsANonPositiveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            SublexExtractor.rank(listOf(SublexCount("sol", 1)), limit = 0)
        }
    }

    @Test
    fun write_producesATsvThatSublexReaderReadsBackWithTheSameRanks() {
        val ranked = SublexExtractor.rank(listOf(SublexCount("de", 9), SublexCount("la", 4)), 10)
        val bytes = ByteArrayOutputStream()

        SublexExtractor.write(ranked, bytes)
        val text = bytes.toString(Charsets.UTF_8)
        val frequencies = SublexReader.readFrequencies(text.byteInputStream())

        assertEquals("word\trank\tcount\nde\t1\t9\nla\t2\t4\n", text)
        assertEquals(mapOf("de" to 1, "la" to 2), frequencies)
    }

    @Test
    fun extract_readsTheWorkbookEndToEnd() {
        val strings = sharedStrings(text("Word"), text("Freq"), text("hola"), text("adiós"))
        val sheet =
            sheet(
                row(1, stringCell("A1", 0), stringCell("B1", 1)),
                row(2, stringCell("A2", 2), numberCell("B2", "40")),
                row(3, stringCell("A3", 3), numberCell("B3", "90")),
            )
        val output = ByteArrayOutputStream()

        val ranked =
            SublexExtractor.extract(workbook(strings, sheet).inputStream(), output, limit = 1)

        assertEquals(listOf(RankedWord("adiós", 1, 90)), ranked)
        assertFalse(output.toString(Charsets.UTF_8).contains("hola"))
    }
}
