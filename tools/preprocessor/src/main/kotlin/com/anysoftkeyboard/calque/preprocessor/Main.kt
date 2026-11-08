package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import kotlin.system.exitProcess

/** CLI binary entry point for the Calque seed.db build-time preprocessor. */
fun main(args: Array<String>) {
    val command = args.firstOrNull()
    if (command != null && !command.startsWith("--")) {
        runExtractCommand(command, parseCommandLineArgs(args.drop(1)))
        return
    }

    val options = parseCommandLineArgs(args.toList())

    val rawDbPath = options["--db"]
    if (rawDbPath == null) {
        printUsageAndExit()
        return
    }

    val dbFile = resolveFile(rawDbPath) ?: File(rawDbPath)
    val config =
        PipelineConfig(
            dbFile = dbFile,
            kaikkiFile = resolveFile(options["--kaikki"]),
            sublexFile = resolveFile(options["--sublex"]),
            dictionarySeedFile = resolveFile(options["--dictionary-seed"]),
            falseCognatesFile = resolveFile(options["--false-cognates"]),
            curatedLessonsFile = resolveFile(options["--curated-lessons"]),
            tatoebaFile = resolveFile(options["--tatoeba"]),
            cognateRulesFile = resolveFile(options["--cognate-rules"]),
        )

    println("Starting Calque Data Ingestion Pipeline...")
    println("Target database: ${config.dbFile.absolutePath}")

    val startTime = System.currentTimeMillis()
    val pipeline = PreprocessorPipeline(config)
    val summary = pipeline.execute()
    val elapsed = System.currentTimeMillis() - startTime

    println("Pipeline completed successfully in ${elapsed}ms:")
    println("  - Dictionary entries inserted: ${summary.dictionaryRows}")
    println("  - False cognate forms expanded: ${summary.falseCognateRows}")
    println("  - Curated lesson sentences accepted: ${summary.curatedLessonRows}")
    println("  - Cognate transformer rules seeded: ${summary.cognateRuleRows}")
}

internal fun printUsageAndExit(message: String? = null) {
    if (message != null) System.err.println("Error: $message\n")
    System.err.println(
        """
        Usage: preprocessor --db <output-path-to-seed.db> [options]
               preprocessor <extract-command> [options]

        Build options:
          --kaikki <path>           Path to Kaikki Spanish JSONL dump
          --sublex <path>           Path to SUBTLEX-ESP frequency file
          --dictionary-seed <path>  Path to dictionary_seed.json
          --false-cognates <path>   Path to false_cognates_seed.json
          --curated-lessons <path>  Path to curated_lessons_seed.json
          --tatoeba <path>          Path to Tatoeba Spanish-English TSV file
          --cognate-rules <path>    Path to cognate_rules_seed.json

        Extract commands (see tools/preprocessor/README.md):
          extract-sublex --input <SUBTLEX-ESP.xlsx> --output <ranks.tsv> [--limit 25000]
          extract-kaikki --input <kaikki.jsonl[.gz]> --ranks <ranks.tsv> --output <lemmas.jsonl.gz>
                         [--extra-words <words.txt>] [--metadata <path>]
                         [--source-last-modified <text>] [--source-etag <text>]
        """
            .trimIndent(),
    )
    exitProcess(1)
}
