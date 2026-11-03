package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import kotlin.system.exitProcess

/** CLI binary entry point for the Calque seed.db build-time preprocessor. */
fun main(args: Array<String>) {
    val options = parseCommandLineArgs(args)

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

private fun printUsageAndExit() {
    System.err.println(
        """
        Usage: preprocessor --db <output-path-to-seed.db> [options]

        Options:
          --kaikki <path>           Path to Kaikki Spanish JSONL dump
          --sublex <path>           Path to SUBTLEX-ESP frequency file
          --dictionary-seed <path>  Path to dictionary_seed.json
          --false-cognates <path>   Path to false_cognates_seed.json
          --curated-lessons <path>  Path to curated_lessons_seed.json
          --tatoeba <path>          Path to Tatoeba Spanish-English TSV file
          --cognate-rules <path>    Path to cognate_rules_seed.json
        """
            .trimIndent(),
    )
    exitProcess(1)
}

private fun resolveFile(path: String?): File? {
    if (path == null) return null
    val file = File(path)
    if (file.isAbsolute) return file
    val workingDir = System.getenv("BUILD_WORKING_DIRECTORY")
    return if (workingDir != null) File(workingDir, path) else file
}

private fun parseCommandLineArgs(args: Array<String>): Map<String, String> {
    val options = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        if (arg.startsWith("--") && i + 1 < args.size && !args[i + 1].startsWith("--")) {
            options[arg] = args[i + 1]
            i += 2
        } else {
            i++
        }
    }
    return options
}
