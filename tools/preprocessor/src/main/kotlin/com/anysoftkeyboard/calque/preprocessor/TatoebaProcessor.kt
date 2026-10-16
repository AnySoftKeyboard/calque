package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.SentenceLevel
import com.anysoftkeyboard.calque.linguistics.SentenceScorer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Processes Tatoeba bilingual sentence pairs, scores them using [SentenceScorer], and saves
 * accepted sentences into the curated_lessons table.
 *
 * Implements Step 4 from INGESTION.md.
 */
object TatoebaProcessor {
    private const val DEFAULT_BATCH_SIZE = 1000
    private const val TSV_PARTS_MIN = 2
    private const val INDEX_SPANISH = 0
    private const val INDEX_ENGLISH = 1
    private const val INDEX_TITLE = 2
    private const val INDEX_CONCEPT = 3
    private const val LESSON_ID_PAD = 4

    /**
     * Reads Tatoeba bilingual sentence pairs from [inputStream], scores and buckets them, and
     * inserts accepted sentences into the database.
     *
     * @return Count of curated lessons generated.
     */
    fun processSentences(
        inputStream: InputStream,
        database: SeedDatabase,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): Int {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val sequenceByLevel = mutableMapOf<SentenceLevel, Int>()
        val batch = mutableListOf<CuratedLessonRow>()
        var totalInserted = 0

        val lessonRows =
            reader
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { parseLine(it) }
                .mapNotNull { evaluateSentence(it, database, sequenceByLevel) }

        for (lessonRow in lessonRows) {
            batch.add(lessonRow)
            if (batch.size >= batchSize) {
                database.insertCuratedLessonsBatch(batch)
                totalInserted += batch.size
                batch.clear()
            }
        }

        if (batch.isNotEmpty()) {
            database.insertCuratedLessonsBatch(batch)
            totalInserted += batch.size
            batch.clear()
        }

        return totalInserted
    }

    private fun parseLine(line: String): TatoebaEntry? {
        val parts = line.split("\t")
        if (parts.size < TSV_PARTS_MIN) return null

        val spanish = parts[INDEX_SPANISH].trim()
        val english = parts[INDEX_ENGLISH].trim()
        if (spanish.isEmpty() || english.isEmpty()) return null

        val title =
            if (parts.size > INDEX_TITLE && parts[INDEX_TITLE].isNotBlank())
                parts[INDEX_TITLE].trim()
            else null
        val concept =
            if (parts.size > INDEX_CONCEPT && parts[INDEX_CONCEPT].isNotBlank())
                parts[INDEX_CONCEPT].trim()
            else null

        return TatoebaEntry(spanish, english, title, concept)
    }

    private fun evaluateSentence(
        entry: TatoebaEntry,
        database: SeedDatabase,
        sequenceByLevel: MutableMap<SentenceLevel, Int>,
    ): CuratedLessonRow? {
        val scoreResult = SentenceScorer.scoreSentence(entry.spanish) { database.lookupWord(it) }
        val level = scoreResult.level ?: return null

        val currentSeq = (sequenceByLevel[level] ?: 0) + 1
        sequenceByLevel[level] = currentSeq

        val title = entry.title ?: "Lesson $currentSeq"
        val concept = entry.targetConcept ?: "Level ${level.ordinal} Syntactic Pattern"

        val contentJson =
            buildJsonObject {
                    put("sentence_es", entry.spanish)
                    put("translation_en", entry.english)
                    put("score", scoreResult.score)
                    put("level", level.name)
                    put("word_count", scoreResult.wordCount)
                    put("rare_word_ratio", scoreResult.rareWordRatio)
                    put("cognate_ratio", scoreResult.cognateRatio)
                    put("has_complex_tense", scoreResult.hasComplexTense)
                }
                .toString()

        val lessonId = "lvl${level.ordinal}_${currentSeq.toString().padStart(LESSON_ID_PAD, '0')}"
        return CuratedLessonRow(
            lessonId = lessonId,
            level = level.ordinal,
            sequenceOrder = currentSeq,
            title = title,
            targetConcept = concept,
            contentJson = contentJson,
        )
    }

    private data class TatoebaEntry(
        val spanish: String,
        val english: String,
        val title: String?,
        val targetConcept: String?,
    )
}
