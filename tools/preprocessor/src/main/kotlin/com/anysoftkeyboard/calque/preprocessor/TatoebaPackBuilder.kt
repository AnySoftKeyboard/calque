package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.ScoredWordInfo
import com.anysoftkeyboard.calque.linguistics.SentenceLevel
import com.anysoftkeyboard.calque.linguistics.SentenceScoreResult
import com.anysoftkeyboard.calque.linguistics.SentenceScorer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** How many sentences make a lesson, and how many lessons a level gets at most. */
data class TatoebaPackConfig(
    val packSize: Int = DEFAULT_PACK_SIZE,
    val packsPerLevel: Int = DEFAULT_PACKS_PER_LEVEL
) {
    init {
        require(packSize > 0) { "packSize must be positive but was $packSize" }
        require(packsPerLevel > 0) { "packsPerLevel must be positive but was $packsPerLevel" }
    }

    companion object {
        const val DEFAULT_PACK_SIZE = 8
        const val DEFAULT_PACKS_PER_LEVEL = 16
    }
}

/** What the builder did with the candidate pairs. */
data class TatoebaPackStats(
    /** Pairs given to the builder. */
    val candidates: Int,
    /** Rejected because a word is missing from the dictionary, so it would not resolve. */
    val unresolvedWord: Int,
    /** Rejected by the scorer (word count or too hard for level 2). */
    val discarded: Int,
    /** Scored sentences per level, before selection. */
    val scoredPerLevel: Map<SentenceLevel, Int>,
    /** Lessons built per level. */
    val packsPerLevel: Map<SentenceLevel, Int>,
)

/** The built lessons, in lesson-id order, with the statistics. */
data class TatoebaPacks(val lessons: List<CuratedLessonRow>, val stats: TatoebaPackStats)

/**
 * Groups scored Tatoeba sentences into short reading lessons (INGESTION.md Step 4).
 * - A sentence is scored with [SentenceScorer] against the dictionary; one with a word that is
 *   missing from the dictionary is skipped, so every shipped word resolves.
 * - Per level, [TatoebaPackConfig.packSize] times [TatoebaPackConfig.packsPerLevel] sentences are
 *   chosen by a fixed hash of the Tatoeba id (unbiased and reproducible), ordered from easiest to
 *   hardest, and cut into lessons, so a level's lessons get harder as the learner moves on. A
 *   remainder smaller than a full lesson is dropped.
 * - The lesson uses the same `{"sentences":[...]}` content as the curated lessons, so the reader
 *   needs no special case; the interlinear gloss is generated at runtime. Each sentence keeps its
 *   two Tatoeba ids, which CC BY attribution needs.
 * - Lesson ids are `tatoeba_l<level>_<nnn>` and `sequence_order` continues after the highest
 *   existing sequence of the level, so the curated lessons stay first.
 */
object TatoebaPackBuilder {
    private const val SCORE_DECIMALS = 100.0
    private const val ID_PAD = 3

    private val LEVEL_TITLES =
        mapOf(
            SentenceLevel.LEVEL_0 to "Starter Sentences",
            SentenceLevel.LEVEL_1 to "Everyday Sentences",
            SentenceLevel.LEVEL_2 to "Confident Sentences",
        )

    private val LEVEL_CONCEPTS =
        mapOf(
            SentenceLevel.LEVEL_0 to
                "Short real sentences built from very common words and cognates.",
            SentenceLevel.LEVEL_1 to "Real sentences with a wider vocabulary.",
            SentenceLevel.LEVEL_2 to "Longer real sentences with less common words.",
        )

    private class Scored(val pair: TatoebaPair, val result: SentenceScoreResult)

    /**
     * Builds the lessons for [pairs]. [lookup] resolves a lowercased word (null when unknown);
     * [firstSequence] gives the sequence number after which the lessons of a level continue.
     */
    fun build(
        pairs: List<TatoebaPair>,
        lookup: (String) -> ScoredWordInfo?,
        firstSequence: (Int) -> Int,
        config: TatoebaPackConfig = TatoebaPackConfig(),
    ): TatoebaPacks {
        val cache = HashMap<String, ScoredWordInfo?>()
        val resolve: (String) -> ScoredWordInfo? = { word -> cache.getOrPut(word) { lookup(word) } }
        var unresolved = 0
        var discarded = 0
        val byLevel = HashMap<SentenceLevel, MutableList<Scored>>()
        for (pair in pairs) {
            if (SentenceScorer.tokenizeWords(pair.spanish).any { resolve(it) == null }) {
                unresolved++
                continue
            }
            val result = SentenceScorer.scoreSentence(pair.spanish, resolve)
            val level = result.level
            if (level == null) discarded++
            else byLevel.getOrPut(level) { mutableListOf() }.add(Scored(pair, result))
        }

        val lessons = mutableListOf<CuratedLessonRow>()
        val packCounts = HashMap<SentenceLevel, Int>()
        for (level in SentenceLevel.values()) {
            val packs = packsOf(byLevel[level].orEmpty(), config)
            packCounts[level] = packs.size
            packs.forEachIndexed { index, pack ->
                lessons.add(lessonOf(level, index + 1, firstSequence(level.ordinal), pack, config))
            }
        }
        val stats =
            TatoebaPackStats(
                candidates = pairs.size,
                unresolvedWord = unresolved,
                discarded = discarded,
                scoredPerLevel = SentenceLevel.values().associateWith { byLevel[it]?.size ?: 0 },
                packsPerLevel = packCounts,
            )
        return TatoebaPacks(lessons, stats)
    }

    private fun packsOf(scored: List<Scored>, config: TatoebaPackConfig): List<List<Scored>> =
        scored
            .sortedBy { StableHash.of(it.pair.spanishId) }
            .take(config.packSize * config.packsPerLevel)
            .sortedWith(compareBy({ it.result.score }, { it.pair.spanishId }))
            .chunked(config.packSize)
            .filter { it.size == config.packSize }

    private fun lessonOf(
        level: SentenceLevel,
        number: Int,
        firstSequence: Int,
        pack: List<Scored>,
        config: TatoebaPackConfig,
    ): CuratedLessonRow {
        val numberText = number.toString().padStart(ID_PAD, '0')
        return CuratedLessonRow(
            lessonId = "tatoeba_l${level.ordinal}_$numberText",
            level = level.ordinal,
            sequenceOrder = firstSequence + number,
            title = "${LEVEL_TITLES.getValue(level)} $numberText",
            targetConcept = "${config.packSize} sentences. ${LEVEL_CONCEPTS.getValue(level)}",
            contentJson = contentJson(pack),
        )
    }

    private fun contentJson(pack: List<Scored>): String =
        buildJsonObject {
                put("source", "tatoeba")
                put(
                    "sentences",
                    buildJsonArray {
                        for (item in pack) {
                            add(
                                buildJsonObject {
                                    put("sentence_es", item.pair.spanish)
                                    put("translation_en", item.pair.english)
                                    put("tatoeba_spanish_id", item.pair.spanishId)
                                    put("tatoeba_english_id", item.pair.englishId)
                                    put("score", JsonPrimitive(roundScore(item.result.score)))
                                },
                            )
                        }
                    },
                )
            }
            .toString()

    private fun roundScore(score: Double): Double =
        Math.round(score * SCORE_DECIMALS) / SCORE_DECIMALS
}
