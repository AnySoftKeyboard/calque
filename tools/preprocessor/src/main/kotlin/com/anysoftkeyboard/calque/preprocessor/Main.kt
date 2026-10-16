package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import kotlin.system.exitProcess

/** CLI binary entry point for the Calque seed.db build-time preprocessor. */
fun main(args: Array<String>) {
    val options = parseCommandLineArgs(args)

    val dbPath = options["--db"]
    if (dbPath == null) {
        System.err.println(
            """
            Usage: preprocessor --db <output-path-to-seed.db> [options]

            Options:
              --kaikki <path>           Path to Kaikki Spanish JSONL dump
              --sublex <path>           Path to SUBTLEX-ESP frequency file
              --false-cognates <path>   Path to false_cognates_seed.json
              --curated-lessons <path>  Path to curated_lessons_seed.json
              --tatoeba <path>          Path to Tatoeba Spanish-English TSV file
              --cognate-rules <path>    Path to cognate_rules_seed.json
            """
                .trimIndent(),
        )
        exitProcess(1)
    }

    val config =
        PipelineConfig(
            dbFile = File(dbPath),
            kaikkiFile = options["--kaikki"]?.let { File(it) },
            sublexFile = options["--sublex"]?.let { File(it) },
            falseCognatesFile = options["--false-cognates"]?.let { File(it) },
            curatedLessonsFile = options["--curated-lessons"]?.let { File(it) },
            tatoebaFile = options["--tatoeba"]?.let { File(it) },
            cognateRulesFile = options["--cognate-rules"]?.let { File(it) },
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
