package com.anysoftkeyboard.calque.preprocessor

import java.io.File

/** Default number of most frequent SUBTLEX-ESP words kept by the extractors. */
internal const val DEFAULT_FREQUENCY_LIMIT = 25_000

/**
 * Dispatches the one-off corpus extractors. They reduce the large raw corpora to the small subsets
 * that are checked in, so the `seed.db` build never needs the raw downloads.
 */
internal fun runExtractCommand(command: String, options: Map<String, String>) {
    try {
        when (command) {
            "extract-sublex" -> extractSublex(options)
            "extract-kaikki" -> extractKaikki(options)
            else -> throw UsageException("Unknown command: $command")
        }
    } catch (e: UsageException) {
        printUsageAndExit(e.message)
    }
}

private fun extractSublex(options: Map<String, String>) {
    val input = options.requireFile("--input")
    val output = options.requireFile("--output")
    val limit = options.positiveInt("--limit", DEFAULT_FREQUENCY_LIMIT)

    output.parentFile?.mkdirs()
    val ranked =
        input.inputStream().buffered().use { source ->
            output.outputStream().buffered().use { target ->
                SublexExtractor.extract(source, target, limit)
            }
        }
    println("Wrote ${ranked.size} ranked words to ${output.path}")
    ranked.lastOrNull()?.let { println("Last rank ${it.rank}: '${it.word}' (count ${it.count})") }
}

private fun extractKaikki(options: Map<String, String>) {
    val input = options.requireFile("--input")
    val ranks = options.requireFile("--ranks")
    val output = options.requireFile("--output")
    val metadata =
        resolveFile(options["--metadata"])
            ?: File(output.path.removeSuffix(".jsonl.gz") + ".meta.json")

    val wanted =
        ranks.inputStream().buffered().use { SublexReader.readFrequencies(it).keys }.toMutableSet()
    resolveFile(options["--extra-words"])?.let { file ->
        wanted.addAll(file.inputStream().use { KaikkiSubsetFiles.readWordList(it) })
    }
    val sourceInfo =
        mapOf(
                "source" to input.name,
                "sourceLastModified" to options["--source-last-modified"],
                "sourceEtag" to options["--source-etag"],
            )
            .filterValues { it != null }
            .mapValues { it.value.orEmpty() }

    println("Extracting lemmas from ${input.path} (${wanted.size} words in scope)...")
    val stats = KaikkiSubsetFiles.extractToFiles(input, wanted, output, metadata, sourceInfo)
    println(
        "Read ${stats.linesRead} lines: kept ${stats.entries} entries with ${stats.forms} forms"
    )
    println(
        "Skipped: ${stats.formOfEntries} form-of, ${stats.excludedEntries} excluded, " +
            "${stats.outOfScopeEntries} out of scope, ${stats.malformedLines} malformed"
    )
    println("Wrote ${output.path} (${output.length()} bytes) and ${metadata.path}")
}
