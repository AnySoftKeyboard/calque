package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Seeds the cognate_rules table from a JSON definition. */
object CognateRulesProcessor {
    private const val ARROW_DELIMITER = "->"
    private const val PAIR_PARTS_COUNT = 2

    /**
     * Reads cognate rules from [inputStream] and inserts them into [database].
     *
     * @return Count of cognate rules inserted.
     */
    fun processRules(inputStream: InputStream, database: SeedDatabase): Int {
        val rows = parseRules(inputStream)
        database.insertCognateRulesBatch(rows)
        return rows.size
    }

    /** Parses the cognate rules seed JSON stream into a list of [CognateRuleRow] objects. */
    fun parseRules(inputStream: InputStream): List<CognateRuleRow> {
        val jsonText = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { it.readText() }
        val array = Json.parseToJsonElement(jsonText).jsonArray
        return array.mapNotNull { parseRuleElement(it.jsonObject) }
    }

    private fun parseRuleElement(obj: JsonObject): CognateRuleRow? {
        val ruleId = obj["rule_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val englishSuffix = obj["english_suffix"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val spanishSuffix = obj["spanish_suffix"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val genderRule = obj["gender_rule"]?.jsonPrimitive?.contentOrNull
        val spellingNote = obj["spelling_note"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val examplesJson = obj["examples"]?.toString() ?: "[]"

        return CognateRuleRow(
            ruleId = ruleId,
            englishSuffix = englishSuffix,
            spanishSuffix = spanishSuffix,
            genderRule = genderRule,
            spellingNote = spellingNote,
            examplesJson = examplesJson,
        )
    }

    /**
     * Parses the serialized `examples_json` array into structured [CognateRuleExample] pairs.
     * Supports both object items (`{"english": "...", "spanish": "..."}`) and arrow-delimited
     * strings (`"english -> spanish"`).
     */
    fun parseExamples(examplesJson: String): List<CognateRuleExample> {
        val array = Json.parseToJsonElement(examplesJson).jsonArray
        return array.mapNotNull { parseExampleElement(it) }
    }

    private fun parseExampleElement(elem: JsonElement): CognateRuleExample? {
        return when (elem) {
            is JsonObject -> parseObjectExample(elem)
            is JsonPrimitive -> parseArrowExample(elem.contentOrNull.orEmpty())
            else -> null
        }
    }

    private fun parseObjectExample(obj: JsonObject): CognateRuleExample? {
        val english = obj["english"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val spanish = obj["spanish"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (english.isEmpty() || spanish.isEmpty()) return null
        return CognateRuleExample(english = english, spanish = spanish)
    }

    private fun parseArrowExample(raw: String): CognateRuleExample? {
        val parts = raw.split(ARROW_DELIMITER)
        if (parts.size != PAIR_PARTS_COUNT) return null
        val english = parts[0].trim()
        val spanish = parts[1].trim()
        if (english.isEmpty() || spanish.isEmpty()) return null
        return CognateRuleExample(english = english, spanish = spanish)
    }
}
