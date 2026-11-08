package com.anysoftkeyboard.calque.preprocessor

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
