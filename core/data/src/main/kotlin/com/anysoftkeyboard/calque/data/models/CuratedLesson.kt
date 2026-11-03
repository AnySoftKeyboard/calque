package com.anysoftkeyboard.calque.data.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Represents a single token in an interlinear word-for-word literal gloss. */
data class InterlinearGlossToken(
    val es: String,
    val en: String,
)

/** Represents a structured sentence inside a curated lesson's content. */
data class LessonSentence(
    val sentenceEs: String,
    val translationEn: String,
    val interlinearGloss: List<InterlinearGlossToken> = emptyList(),
    val grammarCallout: String? = null,
)

/** Domain model for a curated reading progression lesson stored in `curated_lessons`. */
data class CuratedLesson(
    val lessonId: String,
    val level: Int,
    val sequenceOrder: Int,
    val title: String,
    val targetConcept: String,
    val contentJson: String,
) {
    /** Parses the underlying [contentJson] string into a list of [LessonSentence]s. */
    fun parseSentences(): List<LessonSentence> {
        if (contentJson.isBlank()) return emptyList()

        val parsedElement =
            runCatching { Json.parseToJsonElement(contentJson) }.getOrNull() ?: return emptyList()
        val sentencesArray: JsonArray =
            when {
                parsedElement is JsonArray -> parsedElement
                parsedElement is JsonObject ->
                    parsedElement["sentences"] as? JsonArray ?: return emptyList()
                else -> return emptyList()
            }

        return sentencesArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            parseSentenceObject(obj)
        }
    }

    private fun parseSentenceObject(obj: JsonObject): LessonSentence {
        val sentenceEs =
            obj["sentence_es"]?.jsonPrimitive?.contentOrNull
                ?: obj["sentenceEs"]?.jsonPrimitive?.contentOrNull
                ?: ""
        val translationEn =
            obj["translation_en"]?.jsonPrimitive?.contentOrNull
                ?: obj["translationEn"]?.jsonPrimitive?.contentOrNull
                ?: ""
        val grammarCallout =
            obj["grammar_callout"]?.jsonPrimitive?.contentOrNull
                ?: obj["grammarCallout"]?.jsonPrimitive?.contentOrNull

        val rawGlossArray = (obj["interlinear_gloss"] ?: obj["interlinearGloss"]) as? JsonArray

        val interlinearGloss =
            rawGlossArray?.mapNotNull { glossElem ->
                val glossObj = glossElem as? JsonObject ?: return@mapNotNull null
                val es = glossObj["es"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val en = glossObj["en"]?.jsonPrimitive?.contentOrNull.orEmpty()
                InterlinearGlossToken(es = es, en = en)
            } ?: emptyList()

        return LessonSentence(
            sentenceEs = sentenceEs,
            translationEn = translationEn,
            interlinearGloss = interlinearGloss,
            grammarCallout = grammarCallout,
        )
    }
}
