package com.anysoftkeyboard.calque.preprocessor

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** One inflected form of a [SubsetEntry]: the surface text and Kaikki's grammar tags for it. */
data class SubsetForm(val form: String, val tags: List<String>) {
    internal fun toJson(): JsonObject = buildJsonObject {
        put("form", form)
        putJsonArray("tags") { tags.forEach { add(JsonPrimitive(it)) } }
    }
}

/**
 * A trimmed Kaikki entry.
 *
 * Most entries are lemmas: the first real gloss and the usable inflected [forms]. An *alias* has a
 * non-null [formOf]: a word Kaikki only lists as a variant of another lemma (`tu` = "apocopic form
 * of tuyo", `dame` = "imperative of dar combined with me") that no lemma entry lists as a form. It
 * has no forms of its own; [gloss] is Kaikki's description of the variant and [tags] its tags. A
 * variant whose target is not a single word (`ONU` = "acronym of Organización de las Naciones
 * Unidas") is kept as a standalone entry: no [formOf], no forms, the description as [gloss].
 */
data class SubsetEntry(
    val word: String,
    val pos: String,
    val gloss: String,
    val forms: List<SubsetForm>,
    val formOf: String? = null,
    val tags: List<String> = emptyList(),
) {
    /** Serializes with Kaikki's own field names so `KaikkiProcessor` can read it. */
    fun toJson(): JsonObject = buildJsonObject {
        put("word", word)
        put("pos", pos)
        putJsonArray("senses") { add(senseJson()) }
        if (forms.isNotEmpty()) put("forms", buildJsonArray { forms.forEach { add(it.toJson()) } })
    }

    private fun senseJson(): JsonObject = buildJsonObject {
        putJsonArray("glosses") { add(JsonPrimitive(gloss)) }
        if (tags.isNotEmpty()) putJsonArray("tags") { tags.forEach { add(JsonPrimitive(it)) } }
        if (formOf != null) {
            putJsonArray("form_of") { add(buildJsonObject { put("word", formOf) }) }
        }
    }
}

/** What happened to the lines of the dump. Every non-blank line lands in exactly one counter. */
data class KaikkiSubsetStats(
    val linesRead: Long,
    val malformedLines: Long,
    val formOfEntries: Long,
    val excludedEntries: Long,
    val outOfScopeEntries: Long,
    val duplicateEntries: Long,
    val entries: Int,
    val aliases: Int,
    val forms: Int,
)

/** The extracted entries and aliases, sorted by word then part of speech, with the statistics. */
data class KaikkiSubset(val entries: List<SubsetEntry>, val stats: KaikkiSubsetStats)
