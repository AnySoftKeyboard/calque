package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The three decompressed (or `.gz`) Tatoeba export files. */
data class TatoebaInputs(val spanish: File, val english: File, val links: File)

/** Everything one `extract-tatoeba` run needs besides the corpus files. */
data class TatoebaRun(
    val inputs: TatoebaInputs,
    val wantedWords: Set<String>,
    val maxPairs: Int,
    val output: File,
    val metadataFile: File,
    val sourceInfo: Map<String, String>,
)

/** Reads and writes the files around the Tatoeba candidates: the pair TSV and the metadata. */
object TatoebaCandidateFiles {
    private const val METADATA_FORMAT = "tatoeba-candidates/1"
    private const val PAIR_COLUMNS = 4
    private const val COLUMN_SPANISH_ID = 0
    private const val COLUMN_ENGLISH_ID = 1
    private const val COLUMN_SPANISH = 2
    private const val COLUMN_ENGLISH = 3

    /**
     * Writes `spanishId<TAB>englishId<TAB>spanish<TAB>english` lines as a deterministic gzip. The
     * ids are Tatoeba sentence ids and must be kept: CC BY needs each sentence to be attributable.
     */
    fun writeGzipTsv(pairs: List<TatoebaPair>, output: OutputStream) {
        val text = ByteArrayOutputStream()
        for (pair in pairs) {
            val line = "${pair.spanishId}\t${pair.englishId}\t${pair.spanish}\t${pair.english}\n"
            text.write(line.toByteArray(Charsets.UTF_8))
        }
        DeterministicGzip.compress(text.toByteArray(), output)
    }

    /** Parses the output of [writeGzipTsv] (already decompressed); malformed lines are skipped. */
    fun readPairs(input: InputStream): List<TatoebaPair> =
        input
            .bufferedReader(Charsets.UTF_8)
            .lineSequence()
            .mapNotNull { parsePair(it.split('\t')) }
            .toList()

    private fun parsePair(parts: List<String>): TatoebaPair? {
        val spanishId = parts.getOrNull(COLUMN_SPANISH_ID)?.toLongOrNull()
        val englishId = parts.getOrNull(COLUMN_ENGLISH_ID)?.toLongOrNull()
        return if (parts.size == PAIR_COLUMNS && spanishId != null && englishId != null) {
            TatoebaPair(spanishId, englishId, parts[COLUMN_SPANISH], parts[COLUMN_ENGLISH])
        } else {
            null
        }
    }

    /** Renders the provenance and statistics file that is committed next to the pairs. */
    fun metadataJson(
        stats: TatoebaStats,
        wantedWords: Int,
        maxPairs: Int,
        sourceInfo: Map<String, String>,
    ): String {
        val obj = buildJsonObject {
            put("format", METADATA_FORMAT)
            for ((key, value) in sourceInfo.toSortedMap()) put(key, value)
            put("wantedWords", wantedWords)
            put("maxPairs", maxPairs)
            put("spanishLinesRead", stats.spanishLinesRead)
            for (reason in TatoebaRejection.values()) {
                put("rejected_${reason.name.lowercase()}", stats.rejections[reason] ?: 0L)
            }
            put("candidates", stats.candidates)
            put("selected", stats.selected)
        }
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), obj) + "\n"
    }

    /** File-based entry point used by the `extract-tatoeba` command. */
    fun extractToFiles(run: TatoebaRun): TatoebaStats {
        val links = run.inputs.links.openMaybeGzip().use { TatoebaCandidateExtractor.readLinks(it) }
        val result =
            TatoebaCandidateExtractor.extract(
                openSpanish = { run.inputs.spanish.openMaybeGzip() },
                openEnglish = { run.inputs.english.openMaybeGzip() },
                links = links,
                wantedWords = run.wantedWords,
                maxPairs = run.maxPairs,
            )
        run.output.parentFile?.mkdirs()
        run.output.outputStream().buffered().use { writeGzipTsv(result.pairs, it) }
        run.metadataFile.parentFile?.mkdirs()
        run.metadataFile.writeText(
            metadataJson(result.stats, run.wantedWords.size, run.maxPairs, run.sourceInfo)
        )
        return result.stats
    }
}
