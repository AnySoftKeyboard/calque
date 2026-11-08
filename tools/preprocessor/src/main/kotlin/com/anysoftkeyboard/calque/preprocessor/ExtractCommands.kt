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
            "extract-tatoeba" -> extractTatoeba(options)
            "extract-seed-words" -> extractSeedWords(options)
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

    val wanted = readWantedWords(ranks, resolveFile(options["--extra-words"]))
    val sourceInfo = sourceInfo(options, input.name)

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

private fun extractTatoeba(options: Map<String, String>) {
    val inputs =
        TatoebaInputs(
            spanish = options.requireFile("--spanish"),
            english = options.requireFile("--english"),
            links = options.requireFile("--links"),
        )
    val output = options.requireFile("--output")
    val metadata =
        resolveFile(options["--metadata"])
            ?: File(output.path.removeSuffix(".tsv.gz") + ".meta.json")
    val maxPairs = options.positiveInt("--max-pairs", TatoebaCandidateExtractor.DEFAULT_MAX_PAIRS)
    val wanted = readWantedWords(options.requireFile("--ranks"), null)
    val sourceInfo = sourceInfo(options, inputs.spanish.name)

    println("Selecting Tatoeba candidates (${wanted.size} words in scope, max $maxPairs)...")
    val stats =
        TatoebaCandidateFiles.extractToFiles(
            TatoebaRun(inputs, wanted, maxPairs, output, metadata, sourceInfo)
        )
    println("Read ${stats.spanishLinesRead} Spanish sentences: ${stats.candidates} candidates")
    println("Rejected: ${stats.rejections.entries.joinToString { "${it.key}=${it.value}" }}")
    println("Wrote ${stats.selected} pairs to ${output.path} (${output.length()} bytes)")
}

private fun extractSeedWords(options: Map<String, String>) {
    val output = options.requireFile("--output")
    val words =
        options.requireFile("--curated-lessons").inputStream().use { lessons ->
            options.requireFile("--false-cognates").inputStream().use { falseCognates ->
                options.requireFile("--dictionary-seed").inputStream().use { dictionary ->
                    SeedWordsExtractor.collect(lessons, falseCognates, dictionary)
                }
            }
        }
    output.parentFile?.mkdirs()
    output.outputStream().buffered().use { SeedWordsExtractor.write(words, it) }
    println("Wrote ${words.size} seed words to ${output.path}")
}

/** The words in the rank file, plus the optional extra word list. */
private fun readWantedWords(ranks: File, extraWords: File?): Set<String> {
    val words =
        ranks.inputStream().buffered().use { SublexReader.readFrequencies(it).keys }.toMutableSet()
    extraWords?.let { file ->
        words.addAll(file.inputStream().use { KaikkiSubsetFiles.readWordList(it) })
    }
    return words
}

/** Provenance recorded in the metadata files: the source name and optional HTTP headers. */
private fun sourceInfo(options: Map<String, String>, sourceName: String): Map<String, String> =
    listOfNotNull(
            "source" to sourceName,
            options["--source-last-modified"]?.let { "sourceLastModified" to it },
            options["--source-etag"]?.let { "sourceEtag" to it },
        )
        .toMap()
