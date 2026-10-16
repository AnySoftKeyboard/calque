package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Seeds the cognate_rules table from a JSON definition. */
object CognateRulesProcessor {

    /**
     * Reads cognate rules from [inputStream] and inserts them into [database].
     *
     * @return Count of cognate rules inserted.
     */
    fun processRules(inputStream: InputStream, database: SeedDatabase): Int {
        val jsonText = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { it.readText() }
        val array = Json.parseToJsonElement(jsonText).jsonArray
        val rows = mutableListOf<CognateRuleRow>()

        for (elem in array) {
            val obj = elem.jsonObject
            val ruleId = obj["rule_id"]?.jsonPrimitive?.content ?: continue
            val englishSuffix = obj["english_suffix"]?.jsonPrimitive?.content.orEmpty()
            val spanishSuffix = obj["spanish_suffix"]?.jsonPrimitive?.content.orEmpty()
            val genderRule = obj["gender_rule"]?.jsonPrimitive?.content
            val spellingNote = obj["spelling_note"]?.jsonPrimitive?.content.orEmpty()
            val examplesJson = obj["examples"]?.toString() ?: "[]"

            rows.add(
                CognateRuleRow(
                    ruleId = ruleId,
                    englishSuffix = englishSuffix,
                    spanishSuffix = spanishSuffix,
                    genderRule = genderRule,
                    spellingNote = spellingNote,
                    examplesJson = examplesJson,
                ),
            )
        }

        database.insertCognateRulesBatch(rows)
        return rows.size
    }
}
