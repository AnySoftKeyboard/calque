package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Seeds the curated_lessons table from a handcrafted JSON definition. */
object CuratedLessonsProcessor {
    private const val DEFAULT_LEVEL = 0
    private const val DEFAULT_SEQUENCE = 1

    /**
     * Reads curated syntactic progression lessons from [inputStream] and inserts them into
     * [database].
     *
     * @return Count of curated lessons inserted.
     */
    fun processLessons(inputStream: InputStream, database: SeedDatabase): Int {
        val rows = parseLessons(inputStream)
        database.insertCuratedLessonsBatch(rows)
        return rows.size
    }

    /** Parses the curated lessons seed JSON stream into a list of [CuratedLessonRow] objects. */
    fun parseLessons(inputStream: InputStream): List<CuratedLessonRow> {
        val jsonText = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { it.readText() }
        val array = Json.parseToJsonElement(jsonText).jsonArray
        return array.mapNotNull { parseLessonElement(it.jsonObject) }
    }

    private fun parseLessonElement(obj: JsonObject): CuratedLessonRow? {
        val lessonId = obj["lesson_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val level = obj["level"]?.jsonPrimitive?.intOrNull ?: DEFAULT_LEVEL
        val sequenceOrder = obj["sequence_order"]?.jsonPrimitive?.intOrNull ?: DEFAULT_SEQUENCE
        val title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val targetConcept = obj["target_concept"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val contentJson =
            obj["content"]?.toString()
                ?: obj["content_json"]?.jsonPrimitive?.contentOrNull
                ?: """{"sentences":[]}"""

        return CuratedLessonRow(
            lessonId = lessonId,
            level = level,
            sequenceOrder = sequenceOrder,
            title = title,
            targetConcept = targetConcept,
            contentJson = contentJson,
        )
    }

    /** Parses the serialized `content_json` payload into a list of [LessonSentence] items. */
    fun parseLessonSentences(contentJson: String): List<LessonSentence> {
        val root = Json.parseToJsonElement(contentJson).jsonObject
        val sentencesArray = root["sentences"]?.jsonArray ?: return emptyList()
        return sentencesArray.mapNotNull { parseSentenceElement(it.jsonObject) }
    }

    private fun parseSentenceElement(obj: JsonObject): LessonSentence? {
        val sentenceEs = obj["sentence_es"]?.jsonPrimitive?.contentOrNull ?: return null
        val translationEn = obj["translation_en"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val glossArray = obj["interlinear_gloss"] as? JsonArray
        val interlinearGloss = glossArray?.mapNotNull { parseGlossElement(it) }.orEmpty()
        val grammarCallout = obj["grammar_callout"]?.jsonPrimitive?.contentOrNull

        return LessonSentence(
            sentenceEs = sentenceEs,
            translationEn = translationEn,
            interlinearGloss = interlinearGloss,
            grammarCallout = grammarCallout,
        )
    }

    private fun parseGlossElement(elem: JsonElement): InterlinearGlossToken? {
        val obj = elem as? JsonObject ?: return null
        val es = obj["es"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val en = obj["en"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (es.isEmpty() || en.isEmpty()) return null
        return InterlinearGlossToken(es = es, en = en)
    }
}
