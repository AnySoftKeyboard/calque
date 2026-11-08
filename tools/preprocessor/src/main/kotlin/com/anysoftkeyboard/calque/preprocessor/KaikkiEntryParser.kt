package com.anysoftkeyboard.calque.preprocessor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** The classification of one line of the Kaikki dump. */
sealed interface KaikkiLine {
    /** A usable lemma entry (not yet checked against the in-scope words). */
    data class Lemma(val entry: SubsetEntry) : KaikkiLine

    /** Not valid JSON, or not a JSON object. */
    data object Malformed : KaikkiLine

    /**
     * Every sense is a variant of another word (`form_of` / `alt_of`). Usually the lemma lists the
     * form too; [alias] is the entry to keep when it does not (null when Kaikki names no target).
     */
    data class FormOf(val alias: SubsetEntry?) : KaikkiLine

    /** Unwanted part of speech, multi-word headword, or no usable gloss. */
    data object Excluded : KaikkiLine
}

/** Applies the entry-level rules of the Kaikki subset (see [KaikkiSubsetExtractor]). */
object KaikkiEntryParser {
    /** Parts of speech (Kaikki `pos`) that occur as single words in running text. */
    val KEPT_POS: Set<String> =
        setOf(
            "verb",
            "noun",
            "adj",
            "adv",
            "pron",
            "det",
            "article",
            "prep",
            "conj",
            "num",
            "contraction",
            "intj",
            "name",
            "particle",
        )

    /** Tags that mark an item of `forms` as conjugation-table metadata or an unusable form. */
    private val SKIPPED_FORM_TAGS =
        setOf(
            "table-tags",
            "inflection-template",
            "class",
            "combined-form",
            "error-unrecognized-form",
        )

    private const val COMBINED_WITH = " combined with "

    /** Sense tags that mark a sense as a spelling variant rather than a meaning of its own. */
    private val DERIVATIVE_SENSE_TAGS = setOf("form-of", "alt-of", "misspelling")

    /** Classifies one non-blank [line] of the dump. */
    fun parseLine(line: String): KaikkiLine {
        val obj =
            try {
                Json.parseToJsonElement(line) as? JsonObject
            } catch (_: IllegalArgumentException) {
                null
            }
        return if (obj == null) KaikkiLine.Malformed else parseEntry(obj)
    }

    private fun parseEntry(obj: JsonObject): KaikkiLine {
        val word = obj.string("word")?.trim().orEmpty()
        val pos = obj.string("pos").orEmpty()
        val senses = obj.objects("senses")
        val lemmaSenses = senses.filterNot(::isDerivative)
        val gloss = lemmaSenses.firstNotNullOfOrNull(::firstGloss)
        return when {
            word.isEmpty() || pos !in KEPT_POS || word.any { it.isWhitespace() } ->
                KaikkiLine.Excluded
            senses.isNotEmpty() && lemmaSenses.isEmpty() ->
                KaikkiLine.FormOf(aliasOf(word, pos, senses))
            gloss == null -> KaikkiLine.Excluded
            else -> KaikkiLine.Lemma(SubsetEntry(word, pos, gloss, parseForms(obj, word)))
        }
    }

    /**
     * The first derivative sense that names the word it is a variant of, as an alias entry.
     *
     * Kaikki sometimes puts a description in `form_of` (`agradecer combined with indirect object me
     * and direct object lo`); the part before "combined with" is the lemma. When the target is
     * still not a single word (`Organización de las Naciones Unidas` for the acronym `ONU`), the
     * entry is kept standalone, with its own gloss and no target.
     */
    private fun aliasOf(word: String, pos: String, senses: List<JsonObject>): SubsetEntry? {
        for (sense in senses) {
            val rawTarget =
                (sense.objects("form_of") + sense.objects("alt_of")).firstNotNullOfOrNull {
                    it.string("word")?.trim()?.takeIf(String::isNotEmpty)
                }
            val gloss = firstGloss(sense)
            if (rawTarget != null && gloss != null) {
                val target = rawTarget.substringBefore(COMBINED_WITH).trim()
                val single = target.takeIf { it.isNotEmpty() && it.none(Char::isWhitespace) }
                return SubsetEntry(word, pos, gloss, emptyList(), single, sense.strings("tags"))
            }
        }
        return null
    }

    private fun isDerivative(sense: JsonObject): Boolean =
        "form_of" in sense ||
            "alt_of" in sense ||
            sense.strings("tags").any { it in DERIVATIVE_SENSE_TAGS }

    private fun firstGloss(sense: JsonObject): String? =
        sense.strings("glosses").map { it.trim() }.firstOrNull { it.isNotEmpty() }

    private fun parseForms(obj: JsonObject, word: String): List<SubsetForm> {
        val forms = LinkedHashSet<SubsetForm>()
        for (item in obj.objects("forms")) {
            val form = item.string("form")?.trim().orEmpty()
            val tags = item.strings("tags")
            val usable =
                form.any { it.isLetter() } &&
                    form.none { it.isWhitespace() } &&
                    !form.equals(word, ignoreCase = true) &&
                    tags.none { it in SKIPPED_FORM_TAGS }
            if (usable) forms.add(SubsetForm(form, tags))
        }
        return forms.toList()
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.objects(key: String): List<JsonObject> =
        (this[key] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}
