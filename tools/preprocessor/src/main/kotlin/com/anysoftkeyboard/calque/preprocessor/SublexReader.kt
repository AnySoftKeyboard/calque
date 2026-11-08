package com.anysoftkeyboard.calque.preprocessor

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Reader for SUBTLEX-ESP word frequency rankings.
 *
 * Maps lowercased Spanish words to their frequency rank (1 = most frequent).
 */
object SublexReader {
    private val DELIMITER_REGEX = Regex("""\t+|\s{2,}""")
    private const val TWO_COLUMN_COUNT = 2

    /**
     * Reads SUBTLEX-ESP data from an input stream and produces a map of words to frequency ranks.
     */
    fun readFrequencies(inputStream: InputStream): Map<String, Int> {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val frequencyMap = mutableMapOf<String, Int>()
        var lineRank = 0
        var rankColumnIndex = -1

        val validLines =
            reader
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }

        for ((index, line) in validLines.withIndex()) {
            val parts = line.split(DELIMITER_REGEX)
            val firstCol = parts.firstOrNull().orEmpty().trim().lowercase(Locale.ROOT)

            if (index == 0 && isHeader(firstCol, parts)) {
                rankColumnIndex = parts.indexOfFirst { it.trim().equals("rank", ignoreCase = true) }
            } else if (firstCol.isNotEmpty()) {
                lineRank++
                val parsedRank = extractRank(parts, rankColumnIndex)
                val finalRank = parsedRank ?: lineRank

                if (!frequencyMap.containsKey(firstCol)) {
                    frequencyMap[firstCol] = finalRank
                }
            }
        }
        return frequencyMap
    }

    private fun extractRank(parts: List<String>, rankColumnIndex: Int): Int? {
        return if (rankColumnIndex >= 0 && rankColumnIndex < parts.size) {
            parts[rankColumnIndex].trim().toIntOrNull()
        } else if (parts.size == TWO_COLUMN_COUNT) {
            parts[1].trim().toIntOrNull()
        } else {
            null
        }
    }

    /**
     * Only the first line can be a header, and only when none of its other columns is a number.
     * `palabra` is a header name but also a frequent Spanish word, so a data row such as
     * `palabra<TAB>1305` must not be mistaken for one.
     */
    private fun isHeader(candidate: String, parts: List<String>): Boolean {
        val isHeaderName = candidate == "word" || candidate == "spelling" || candidate == "palabra"
        return isHeaderName && parts.drop(1).none { it.trim().toIntOrNull() != null }
    }
}
