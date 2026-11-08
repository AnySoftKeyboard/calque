package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Reads and writes the files around the Kaikki subset: word lists, the gzip, the metadata. */
object KaikkiSubsetFiles {
    private const val METADATA_FORMAT = "kaikki-lemma-subset/1"

    /** Serializes [entries] as one JSON object per line and writes a deterministic gzip file. */
    fun writeGzipJsonLines(entries: List<SubsetEntry>, output: OutputStream) {
        val text = ByteArrayOutputStream()
        for (entry in entries) {
            text.write((entry.toJson().toString() + "\n").toByteArray(Charsets.UTF_8))
        }
        DeterministicGzip.compress(text.toByteArray(), output)
    }

    /** Renders the provenance and statistics file that is committed next to the subset. */
    fun metadataJson(
        stats: KaikkiSubsetStats,
        wantedWords: Int,
        sourceInfo: Map<String, String>,
    ): String {
        val obj = buildJsonObject {
            put("format", METADATA_FORMAT)
            for ((key, value) in sourceInfo.toSortedMap()) put(key, value)
            put("wantedWords", wantedWords)
            put("linesRead", stats.linesRead)
            put("malformedLines", stats.malformedLines)
            put("formOfEntriesSkipped", stats.formOfEntries)
            put("excludedEntriesSkipped", stats.excludedEntries)
            put("outOfScopeEntriesSkipped", stats.outOfScopeEntries)
            put("duplicateEntriesMerged", stats.duplicateEntries)
            put("entries", stats.entries)
            put("aliases", stats.aliases)
            put("forms", stats.forms)
        }
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), obj) + "\n"
    }

    /** Reads a word list file: one word per line, blank lines and `#` comments ignored. */
    fun readWordList(input: InputStream): Set<String> =
        input
            .bufferedReader(Charsets.UTF_8)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map(KaikkiSubsetExtractor::normalize)
            .toSet()

    /** File-based entry point used by the `extract-kaikki` command. */
    fun extractToFiles(
        input: File,
        wantedWords: Set<String>,
        output: File,
        metadataFile: File,
        sourceInfo: Map<String, String>,
    ): KaikkiSubsetStats {
        val subset = KaikkiSubsetExtractor.extract({ input.openMaybeGzip() }, wantedWords)
        output.parentFile?.mkdirs()
        output.outputStream().buffered().use { writeGzipJsonLines(subset.entries, it) }
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(metadataJson(subset.stats, wantedWords.size, sourceInfo))
        return subset.stats
    }
}
