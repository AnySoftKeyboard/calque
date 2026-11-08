package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.OutputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/** A SUBTLEX-ESP word with its raw occurrence count in the subtitle corpus. */
data class SublexCount(val word: String, val count: Long)

/** A word with its 1-based frequency rank (1 = most frequent) and the count it was ranked by. */
data class RankedWord(val word: String, val rank: Int, val count: Long)

/**
 * Reads the official SUBTLEX-ESP workbook (`SUBTLEX-ESP.xlsx`, published on OSF by Cuetos et al.).
 *
 * The workbook has one sheet in which the alphabetical word list is laid out in several side by
 * side column groups (word, count, per-million, log). Every shared-string cell that is directly
 * followed by a numeric cell is read as a `word, count` pair, which makes the reader independent of
 * how many groups the sheet has.
 */
object SublexXlsxReader {
    private const val SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml"
    private const val SHEET_ENTRY = "xl/worksheets/sheet1.xml"
    private const val ALPHABET_SIZE = 26
    private const val SHARED_STRING_TYPE = "s"
    private const val NUMBER_TYPE = "n"

    /** Reads every `word, count` pair of the workbook in [xlsx]. */
    fun readCounts(xlsx: InputStream): List<SublexCount> {
        val entries = readRequiredEntries(xlsx)
        val sharedStrings = parseSharedStrings(entries.getValue(SHARED_STRINGS_ENTRY))
        return parseSheet(entries.getValue(SHEET_ENTRY), sharedStrings)
    }

    private fun readRequiredEntries(xlsx: InputStream): Map<String, ByteArray> {
        val wanted = setOf(SHARED_STRINGS_ENTRY, SHEET_ENTRY)
        val found = mutableMapOf<String, ByteArray>()
        val zip = ZipInputStream(xlsx)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name in wanted) found[entry.name] = zip.readBytes()
            entry = zip.nextEntry
        }
        val missing = wanted - found.keys
        require(missing.isEmpty()) { "Not a SUBTLEX-ESP workbook, missing: ${missing.sorted()}" }
        return found
    }

    private fun newXmlReader(bytes: ByteArray): XMLStreamReader {
        val factory = XMLInputFactory.newInstance()
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        return factory.createXMLStreamReader(bytes.inputStream())
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val reader = newXmlReader(bytes)
        val strings = mutableListOf<String>()
        val current = StringBuilder()
        var inText = false
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT ->
                    when (reader.localName) {
                        "si" -> current.setLength(0)
                        "t" -> inText = true
                    }
                XMLStreamConstants.CHARACTERS,
                XMLStreamConstants.CDATA -> if (inText) current.append(reader.text)
                XMLStreamConstants.END_ELEMENT ->
                    when (reader.localName) {
                        "t" -> inText = false
                        "si" -> strings.add(current.toString())
                    }
            }
        }
        return strings
    }

    private class Cell(val column: Int, val type: String, val value: String)

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<SublexCount> {
        val reader = newXmlReader(bytes)
        val state = SheetState(sharedStrings)
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> state.onStart(reader)
                XMLStreamConstants.CHARACTERS -> state.value?.append(reader.text)
                XMLStreamConstants.END_ELEMENT -> state.onEnd(reader.localName)
            }
        }
        return state.counts
    }

    /** Mutable cursor over the sheet XML: the cell being read and the cells of the current row. */
    private class SheetState(private val sharedStrings: List<String>) {
        val counts = mutableListOf<SublexCount>()
        var value: StringBuilder? = null
        private val rowCells = mutableListOf<Cell>()
        private var column = 0
        private var type = NUMBER_TYPE

        fun onStart(reader: XMLStreamReader) {
            when (reader.localName) {
                "row" -> rowCells.clear()
                "c" -> {
                    column = columnIndex(reader.getAttributeValue(null, "r").orEmpty())
                    type = reader.getAttributeValue(null, "t") ?: NUMBER_TYPE
                    value = null
                }
                "v" -> value = StringBuilder()
            }
        }

        fun onEnd(name: String) {
            when (name) {
                "c" -> value?.let { rowCells.add(Cell(column, type, it.toString())) }
                "row" -> collectPairs(rowCells, sharedStrings, counts)
            }
        }
    }

    private fun collectPairs(
        cells: List<Cell>,
        sharedStrings: List<String>,
        out: MutableList<SublexCount>,
    ) {
        for (i in 0 until cells.size - 1) {
            val wordCell = cells[i]
            val countCell = cells[i + 1]
            val isPair =
                wordCell.type == SHARED_STRING_TYPE &&
                    countCell.type == NUMBER_TYPE &&
                    countCell.column == wordCell.column + 1
            if (!isPair) continue
            val word = wordCell.value.toIntOrNull()?.let { sharedStrings.getOrNull(it) }
            val count = countCell.value.toDoubleOrNull()?.toLong()
            if (word != null && count != null) out.add(SublexCount(word, count))
        }
    }

    /** Converts the letters of a cell reference such as `AB12` to a 1-based column index. */
    private fun columnIndex(reference: String): Int =
        reference
            .takeWhile { it.isLetter() }
            .fold(0) { acc, letter -> acc * ALPHABET_SIZE + (letter.uppercaseChar() - 'A' + 1) }
}

/**
 * Turns SUBTLEX-ESP counts into the compact `word<TAB>rank<TAB>count` rank file that [SublexReader]
 * and the other extractors consume.
 */
object SublexExtractor {
    private val WORD_REGEX = Regex("""\p{L}+""")

    /**
     * Ranks words by descending [SublexCount.count] (ties broken alphabetically so the result is
     * deterministic) and keeps the top [limit]. Words are NFC-normalized and lowercased; case
     * variants are merged and tokens that are not purely letters (numbers, punctuation, hyphenated
     * forms) are dropped.
     */
    fun rank(counts: List<SublexCount>, limit: Int): List<RankedWord> {
        require(limit > 0) { "limit must be positive but was $limit" }
        val merged = HashMap<String, Long>()
        for (entry in counts) {
            val word = normalize(entry.word)
            if (entry.count > 0 && WORD_REGEX.matches(word))
                merged.merge(word, entry.count, Long::plus)
        }
        return merged.entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .take(limit)
            .mapIndexed { index, entry -> RankedWord(entry.key, index + 1, entry.value) }
    }

    /** Writes [ranked] as UTF-8 TSV with a `word, rank, count` header and `\n` line endings. */
    fun write(ranked: List<RankedWord>, output: OutputStream) {
        val text = buildString {
            append("word\trank\tcount\n")
            for (word in ranked) append("${word.word}\t${word.rank}\t${word.count}\n")
        }
        output.write(text.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    /** Reads the workbook in [xlsx], ranks it and writes the top [limit] words to [output]. */
    fun extract(xlsx: InputStream, output: OutputStream, limit: Int): List<RankedWord> {
        val ranked = rank(SublexXlsxReader.readCounts(xlsx), limit)
        write(ranked, output)
        return ranked
    }

    private fun normalize(word: String): String =
        Normalizer.normalize(word.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
}
