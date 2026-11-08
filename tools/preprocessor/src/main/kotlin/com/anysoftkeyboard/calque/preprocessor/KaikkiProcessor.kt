package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.util.Locale

/** What [KaikkiProcessor] did with a Kaikki stream. */
data class KaikkiProcessingStats(
    val rows: Long,
    val lemmas: Long,
    val aliases: Long,
    val skippedFormOfEntries: Long,
    val excludedEntries: Long,
    val malformedLines: Long,
)

/**
 * Processor for streaming Kaikki JSONL input and populating the dictionary table.
 *
 * Implements Step 1 from INGESTION.md. The input is either the checked-in lemma subset produced by
 * `extract-kaikki` or a full Kaikki dump; the entry rules ([KaikkiEntryParser]) are the same:
 * - a lemma entry becomes a base row plus a row per usable form (conjugation-table metadata,
 *   clitic-combined and multi-word forms are skipped);
 * - an entry that only describes a variant of another word is an *alias* (`tu` = apocopic form of
 *   `tuyo`). Aliases are resolved against their target lemma once the stream has ended, so their
 *   order relative to the target does not matter. Aliases whose word has no frequency rank are
 *   skipped when [processStream] is given ranks, which keeps a full dump from being held in memory;
 * - malformed lines and unwanted entries are counted and skipped.
 */
object KaikkiProcessor {
    private const val DEFAULT_BATCH_SIZE = 5000

    /**
     * Streams the Kaikki JSONL input and flattens it into the dictionary table.
     *
     * @return The count of dictionary rows written.
     */
    fun processStream(
        inputStream: InputStream,
        sublexFrequencies: Map<String, Int>,
        database: SeedDatabase,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): Long = processStreamWithStats(inputStream, sublexFrequencies, database, batchSize).rows

    /** Like [processStream], but reports what happened to every line. */
    fun processStreamWithStats(
        inputStream: InputStream,
        sublexFrequencies: Map<String, Int>,
        database: SeedDatabase,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): KaikkiProcessingStats {
        val run = StreamRun(sublexFrequencies, database, batchSize)
        inputStream.bufferedReader(Charsets.UTF_8).forEachLine { raw ->
            val line = raw.trim()
            if (line.isNotEmpty()) run.accept(KaikkiEntryParser.parseLine(line))
        }
        return run.finish()
    }

    /** Parses a single Kaikki JSON line into its dictionary rows; aliases keep their own gloss. */
    fun parseKaikkiLine(
        jsonLine: String,
        sublexFrequencies: Map<String, Int>
    ): List<DictionaryRow> {
        val builder = KaikkiRowBuilder(sublexFrequencies)
        return when (val parsed = KaikkiEntryParser.parseLine(jsonLine.trim())) {
            is KaikkiLine.Lemma -> builder.lemmaRows(parsed.entry)
            is KaikkiLine.FormOf -> listOfNotNull(parsed.alias?.let { builder.aliasRow(it, null) })
            KaikkiLine.Malformed,
            KaikkiLine.Excluded -> emptyList()
        }
    }

    /** The state of one pass over a stream: pending batch, known lemmas, pending aliases. */
    private class StreamRun(
        private val ranks: Map<String, Int>,
        private val database: SeedDatabase,
        private val batchSize: Int,
    ) {
        private val builder = KaikkiRowBuilder(ranks)
        private val batch = mutableListOf<DictionaryRow>()
        private val lemmaInfo = HashMap<String, LemmaInfo>()
        private val aliases = mutableListOf<SubsetEntry>()
        private var rows = 0L
        private var lemmas = 0L
        private var skippedFormOf = 0L
        private var excluded = 0L
        private var malformed = 0L

        fun accept(line: KaikkiLine) {
            when (line) {
                is KaikkiLine.Lemma -> acceptLemma(line.entry)
                is KaikkiLine.FormOf -> acceptFormOf(line.alias)
                KaikkiLine.Excluded -> excluded++
                KaikkiLine.Malformed -> malformed++
            }
        }

        private fun acceptLemma(entry: SubsetEntry) {
            lemmas++
            val lemmaRows = builder.lemmaRows(entry)
            val info = LemmaInfo(entry.gloss, lemmaRows.first().isCognate)
            lemmaInfo.putIfAbsent(key(entry.word, entry.pos), info)
            lemmaInfo.putIfAbsent(key(entry.word, null), info)
            enqueue(lemmaRows)
        }

        private fun acceptFormOf(alias: SubsetEntry?) {
            if (alias != null && (ranks.isEmpty() || alias.word.lowercase(Locale.ROOT) in ranks)) {
                aliases.add(alias)
            } else {
                skippedFormOf++
            }
        }

        fun finish(): KaikkiProcessingStats {
            val resolved =
                aliases.map { alias ->
                    val target =
                        alias.formOf?.let {
                            lemmaInfo[key(it, alias.pos)] ?: lemmaInfo[key(it, null)]
                        }
                    builder.aliasRow(alias, target)
                }
            enqueue(resolved)
            flush()
            return KaikkiProcessingStats(
                rows,
                lemmas,
                aliases.size.toLong(),
                skippedFormOf,
                excluded,
                malformed
            )
        }

        private fun enqueue(newRows: List<DictionaryRow>) {
            batch.addAll(newRows)
            if (batch.size >= batchSize) flush()
        }

        private fun flush() {
            if (batch.isEmpty()) return
            database.insertDictionaryBatchKeepingBest(batch)
            rows += batch.size
            batch.clear()
        }

        private fun key(word: String, pos: String?): String =
            word.lowercase(Locale.ROOT) + if (pos == null) "" else "|$pos"
    }
}
