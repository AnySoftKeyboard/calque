package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import java.io.FileInputStream

/** Configuration for executing the build-time ingestion pipeline. */
data class PipelineConfig(
    val dbFile: File,
    val kaikkiFile: File? = null,
    val sublexFile: File? = null,
    val dictionarySeedFile: File? = null,
    val falseCognatesFile: File? = null,
    val curatedLessonsFile: File? = null,
    /** The pool written by `extract-tatoeba` (plain or `.gz`). */
    val tatoebaFile: File? = null,
    val cognateRulesFile: File? = null,
    val tatoebaPacks: TatoebaPackConfig = TatoebaPackConfig(),
)

/** Summary of records populated during the pipeline execution. */
data class PipelineSummary(
    val dictionaryRows: Long,
    val falseCognateRows: Int,
    val curatedLessonRows: Int,
    val cognateRuleRows: Int,
    val falseCognateLemmasWithoutDictionaryRows: List<String> = emptyList(),
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
        // Kaikki first: it keeps the best row per surface form. The curated dictionary seed then
        // replaces whatever Kaikki produced for the same word, so hand-written rows always win.
        val kaikkiCount = processKaikki(database, sublexFrequencies)
        val dictSeedCount = processDictionarySeed(database)
        val dictCount = dictSeedCount + kaikkiCount
        val falseCognates = processFalseCognates(database)
        val lessonsCount = processCuratedLessons(database) + processTatoeba(database)
        val rulesCount = processRules(database)
        database.optimize()

        return PipelineSummary(
            dictionaryRows = dictCount,
            falseCognateRows = falseCognates.rows,
            falseCognateLemmasWithoutDictionaryRows = falseCognates.lemmasWithoutDictionaryRows,
            curatedLessonRows = lessonsCount,
            cognateRuleRows = rulesCount,
        )
    }

    private fun loadSublexFrequencies(): Map<String, Int> {
        val file = config.sublexFile
        if (file == null || !file.exists()) return emptyMap()
        return FileInputStream(file).use { SublexReader.readFrequencies(it) }
    }

    private fun processDictionarySeed(database: SeedDatabase): Long {
        val file = config.dictionarySeedFile
        if (file == null || !file.exists()) return 0L
        return FileInputStream(file).use {
            DictionarySeedProcessor.processDictionarySeed(it, database).toLong()
        }
    }

    private fun processKaikki(database: SeedDatabase, sublexFrequencies: Map<String, Int>): Long {
        val file = config.kaikkiFile
        if (file == null || !file.exists()) return 0L
        return file.openMaybeGzip().use {
            KaikkiProcessor.processStream(it, sublexFrequencies, database)
        }
    }

    private fun processFalseCognates(database: SeedDatabase): FalseCognateExpansion {
        val file = config.falseCognatesFile
        if (file == null || !file.exists()) return FalseCognateExpansion(0, emptyList())
        return FileInputStream(file).use { FalseCognatesExpander.expandWithReport(it, database) }
    }

    private fun processCuratedLessons(database: SeedDatabase): Int {
        val file = config.curatedLessonsFile
        if (file == null || !file.exists()) return 0
        return FileInputStream(file).use { CuratedLessonsProcessor.processLessons(it, database) }
    }

    private fun processTatoeba(database: SeedDatabase): Int {
        val file = config.tatoebaFile
        if (file == null || !file.exists()) return 0
        return file.openMaybeGzip().use {
            TatoebaProcessor.processSentences(it, database, config.tatoebaPacks)
        }
    }

    private fun processRules(database: SeedDatabase): Int {
        val file = config.cognateRulesFile
        if (file == null || !file.exists()) return 0
        return FileInputStream(file).use { CognateRulesProcessor.processRules(it, database) }
    }
}
