package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import java.io.FileInputStream

/** Configuration for executing the build-time ingestion pipeline. */
data class PipelineConfig(
    val dbFile: File,
    val kaikkiFile: File? = null,
    val sublexFile: File? = null,
    val falseCognatesFile: File? = null,
    val curatedLessonsFile: File? = null,
    val tatoebaFile: File? = null,
    val cognateRulesFile: File? = null,
)

/** Summary of records populated during the pipeline execution. */
data class PipelineSummary(
    val dictionaryRows: Long,
    val falseCognateRows: Int,
    val curatedLessonRows: Int,
    val cognateRuleRows: Int,
)

/** Orchestrates Pipeline 1 build-time generation of seed.db as specified in docs/INGESTION.md. */
class PreprocessorPipeline(private val config: PipelineConfig) {

    /** Executes the pipeline to generate or update the SQLite seed database. */
    fun execute(): PipelineSummary {
        config.dbFile.parentFile?.mkdirs()
        val jdbcUrl = "jdbc:sqlite:${config.dbFile.absolutePath}"
        return SeedDatabase.open(jdbcUrl).use { database -> executeWithDatabase(database) }
    }

    /** Executes the pipeline stages on an already opened [SeedDatabase]. */
    fun executeWithDatabase(database: SeedDatabase): PipelineSummary {
        val sublexFrequencies = loadSublexFrequencies()
        val dictCount = processKaikki(database, sublexFrequencies)
        val falseCognatesCount = processFalseCognates(database)
        val lessonsCount = processCuratedLessons(database) + processTatoeba(database)
        val rulesCount = processRules(database)

        return PipelineSummary(
            dictionaryRows = dictCount,
            falseCognateRows = falseCognatesCount,
            curatedLessonRows = lessonsCount,
            cognateRuleRows = rulesCount,
        )
    }

    private fun loadSublexFrequencies(): Map<String, Int> {
        val file = config.sublexFile
        if (file == null || !file.exists()) return emptyMap()
        return FileInputStream(file).use { SublexReader.readFrequencies(it) }
    }

    private fun processKaikki(database: SeedDatabase, sublexFrequencies: Map<String, Int>): Long {
        val file = config.kaikkiFile
        if (file == null || !file.exists()) return 0L
        return FileInputStream(file).use {
            KaikkiProcessor.processStream(it, sublexFrequencies, database)
        }
    }

    private fun processFalseCognates(database: SeedDatabase): Int {
        val file = config.falseCognatesFile
        if (file == null || !file.exists()) return 0
        return FileInputStream(file).use { FalseCognatesExpander.expand(it, database) }
    }

    private fun processCuratedLessons(database: SeedDatabase): Int {
        val file = config.curatedLessonsFile
        if (file == null || !file.exists()) return 0
        return FileInputStream(file).use { CuratedLessonsProcessor.processLessons(it, database) }
    }

    private fun processTatoeba(database: SeedDatabase): Int {
        val file = config.tatoebaFile
        if (file == null || !file.exists()) return 0
        return FileInputStream(file).use { TatoebaProcessor.processSentences(it, database) }
    }

    private fun processRules(database: SeedDatabase): Int {
        val file = config.cognateRulesFile
        if (file == null || !file.exists()) return 0
        return FileInputStream(file).use { CognateRulesProcessor.processRules(it, database) }
    }
}
