package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Seeds the dictionary table from a structured JSON definition. */
object DictionarySeedProcessor {

    /**
     * Reads dictionary entries from [inputStream] and inserts them into [database].
     *
     * @return Count of dictionary entries inserted.
     */
    fun processDictionarySeed(inputStream: InputStream, database: SeedDatabase): Int {
        val rows = parseDictionary(inputStream)
        database.insertDictionaryBatch(rows)
        return rows.size
    }

    /** Parses the dictionary seed JSON stream into a list of [DictionaryRow] objects. */
    fun parseDictionary(inputStream: InputStream): List<DictionaryRow> {
        val jsonText = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { it.readText() }
        val array = Json.parseToJsonElement(jsonText).jsonArray
        return array.mapNotNull { parseDictionaryElement(it.jsonObject) }
    }

    private fun parseDictionaryElement(obj: JsonObject): DictionaryRow? {
        val surfaceForm =
            obj["surface_form"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: return null
        val lemma =
            obj["lemma"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: surfaceForm
        val pos = obj["pos"]?.jsonPrimitive?.contentOrNull ?: "noun"
        val grammarInfo = obj["grammar_info"]?.jsonPrimitive?.contentOrNull
        val englishGloss = obj["english_gloss"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val isCognate = obj["is_cognate"]?.jsonPrimitive?.booleanOrNull ?: false
        val frequencyRank = obj["frequency_rank"]?.jsonPrimitive?.intOrNull

        return DictionaryRow(
            surfaceForm = surfaceForm,
            lemma = lemma,
            pos = pos,
            grammarInfo = grammarInfo,
            englishGloss = englishGloss,
            isCognate = isCognate,
            frequencyRank = frequencyRank,
        )
    }
}
