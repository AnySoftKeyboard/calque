package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** What applying the reading overrides did. */
data class ReadingOverridesResult(
    val requested: Int,
    /** Overrides that matched no stored reading; a typo or a word missing from the dictionary. */
    val unmatched: List<ReadingPreference>,
)

/**
 * Applies `reading_overrides.json`: a list of `{"surface_form", "pos", "lemma"?}` that name the
 * reading that should be primary for a spelling with several readings. It exists because the
 * automatic order (lemma rows first, then frequency rank, then part of speech) is right for only
 * about two thirds of the common ambiguous words, and a hand-written list for the few hundred most
 * frequent ones is cheaper than a tagger. Only an existing reading can be promoted, so an override
 * never invents a gloss: the glosses come from Kaikki or from the dictionary seed.
 */
object ReadingOverridesProcessor {

    /** Parses the overrides JSON array; entries without `surface_form` or `pos` are skipped. */
    fun parse(inputStream: InputStream): List<ReadingPreference> {
        val text = inputStream.bufferedReader(Charsets.UTF_8).readText()
        return Json.parseToJsonElement(text).jsonArray.mapNotNull { parseEntry(it.jsonObject) }
    }

    private fun parseEntry(obj: JsonObject): ReadingPreference? {
        val surface = obj.text("surface_form")?.lowercase(Locale.ROOT) ?: return null
        val pos = obj.text("pos") ?: return null
        return ReadingPreference(surface, pos, obj.text("lemma"))
    }

    /** Promotes the readings described in [inputStream] and reports the ones that did not match. */
    fun apply(inputStream: InputStream, database: SeedDatabase): ReadingOverridesResult {
        val preferences = parse(inputStream)
        return ReadingOverridesResult(preferences.size, database.promoteReadings(preferences))
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
