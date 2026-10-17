package com.anysoftkeyboard.calque.data.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Represents a single English-to-Spanish word pair in a cognate transformation rule. */
data class CognateRuleExample(
    val english: String,
    val spanish: String,
)

/** Domain model for a cognate transformation rule stored in `cognate_rules`. */
data class CognateRule(
    val ruleId: String,
    val englishSuffix: String,
    val spanishSuffix: String,
    val genderRule: String?,
    val spellingNote: String,
    val examplesJson: String,
) {
    /** Parses the underlying [examplesJson] string into a list of [CognateRuleExample]s. */
    fun parseExamples(): List<CognateRuleExample> {
        if (examplesJson.isBlank()) return emptyList()

        val parsedElement =
            runCatching { Json.parseToJsonElement(examplesJson) }.getOrNull() ?: return emptyList()
        val examplesArray: JsonArray =
            when {
                parsedElement is JsonArray -> parsedElement
                parsedElement is JsonObject ->
                    parsedElement["examples"] as? JsonArray ?: return emptyList()
                else -> return emptyList()
            }

        return examplesArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val english = obj["english"]?.jsonPrimitive?.contentOrNull ?: ""
            val spanish = obj["spanish"]?.jsonPrimitive?.contentOrNull ?: ""
            CognateRuleExample(english = english, spanish = spanish)
        }
    }
}
