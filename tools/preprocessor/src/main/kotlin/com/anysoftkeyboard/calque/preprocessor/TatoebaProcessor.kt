package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream

/**
 * Turns the Tatoeba candidate pool made by `extract-tatoeba` into reading lessons and stores them
 * in the curated_lessons table.
 *
 * Implements Step 4 from INGESTION.md; the scoring and grouping live in [TatoebaPackBuilder].
 */
object TatoebaProcessor {

    /**
     * Reads the pool (`spanishId, englishId, spanish, english` lines, already decompressed) from
     * [inputStream], builds the lessons and inserts them.
     *
     * @return Count of lessons inserted.
     */
    fun processSentences(
        inputStream: InputStream,
        database: SeedDatabase,
        config: TatoebaPackConfig = TatoebaPackConfig(),
    ): Int =
        processPairs(TatoebaCandidateFiles.readPairs(inputStream), database, config).lessons.size

    /** Builds the lessons for [pairs], inserts them and returns them with the statistics. */
    fun processPairs(
        pairs: List<TatoebaPair>,
        database: SeedDatabase,
        config: TatoebaPackConfig = TatoebaPackConfig(),
    ): TatoebaPacks {
        val highestSequence =
            database
                .queryCuratedLessons()
                .groupBy { it.level }
                .mapValues { (_, lessons) -> lessons.maxOf { it.sequenceOrder } }
        val packs =
            TatoebaPackBuilder.build(
                pairs = pairs,
                lookup = { database.lookupWord(it) },
                firstSequence = { level -> highestSequence[level] ?: 0 },
                config = config,
            )
        database.insertCuratedLessonsBatch(packs.lessons)
        return packs
    }
}
