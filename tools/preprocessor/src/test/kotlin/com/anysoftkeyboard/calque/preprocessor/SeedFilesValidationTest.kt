package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.SentenceScorer
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural checks for the hand-written seed files in `data/seeds`, so a typo in the JSON fails a
 * test instead of silently shipping a broken card or lesson. Content checks that need the detector
 * or a database (example counts, cognate alignment) live in [PreprocessorTest].
 */
class SeedFilesValidationTest {

    private fun load(name: String): List<JsonObject> {
        val file = File("data/seeds/$name")
        assertTrue("Expected seed file at ${file.path}", file.exists())
        val array = Json.parseToJsonElement(file.readText()).jsonArray
        assertTrue("$name must not be empty", array.isNotEmpty())
        return array.map { it.jsonObject }
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private fun JsonObject.requireText(key: String, owner: String): String {
        val value = text(key)
        assertTrue("$owner: '$key' is missing or blank", !value.isNullOrBlank())
        return value.orEmpty()
    }

    private fun assertUnique(what: String, values: List<String>) {
        val duplicates = values.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue("Duplicate $what: $duplicates", duplicates.isEmpty())
    }

    // ----- dictionary_seed.json -----------------------------------------------------------------

    @Test
    fun dictionarySeed_everyEntryHasItsRequiredFields() {
        for (entry in load("dictionary_seed.json")) {
            val surface = entry.requireText("surface_form", "dictionary entry")
            entry.requireText("lemma", surface)
            entry.requireText("pos", surface)
            entry.requireText("english_gloss", surface)
            assertTrue(
                "$surface: is_cognate must be a boolean",
                (entry["is_cognate"] as? JsonPrimitive)?.booleanOrNull != null
            )
            val rank = (entry["frequency_rank"] as? JsonPrimitive)?.intOrNull
            assertTrue(
                "$surface: frequency_rank must be a positive integer",
                rank != null && rank > 0
            )
        }
    }

    @Test
    fun dictionarySeed_surfaceFormsAreUniqueLowercaseSingleWords() {
        val surfaces = load("dictionary_seed.json").map { it.requireText("surface_form", "entry") }

        assertUnique("surface_form", surfaces)
        for (surface in surfaces) {
            assertEquals("$surface must be lowercase", surface.lowercase(), surface)
            assertEquals("$surface must be trimmed", surface.trim(), surface)
            assertTrue("$surface must be a single word", surface.none { it.isWhitespace() })
        }
    }

    @Test
    fun dictionarySeed_usesOnlyThePartsOfSpeechTheKaikkiSubsetKeeps() {
        val allowed = KaikkiEntryParser.KEPT_POS
        for (entry in load("dictionary_seed.json")) {
            val pos = entry.requireText("pos", "entry")
            assertTrue("Unknown pos '$pos' for ${entry.text("surface_form")}", pos in allowed)
        }
    }

    @Test
    fun dictionarySeed_lemmasAreLowercaseBecauseTheProcessorLowercasesThem() {
        for (entry in load("dictionary_seed.json")) {
            val lemma = entry.requireText("lemma", "entry")
            assertEquals(lemma.lowercase(), lemma)
        }
    }

    // ----- false_cognates_seed.json -------------------------------------------------------------

    @Test
    fun falseCognatesSeed_lemmasAreUniqueAndEveryCardFieldIsFilled() {
        val seeds = load("false_cognates_seed.json")
        val fields =
            listOf(
                "lemma",
                "apparent_english",
                "actual_english",
                "real_spanish_for_apparent",
                "severity",
                "disambiguation_tip",
                "example_es",
                "example_en",
            )

        assertUnique("false cognate lemma", seeds.map { it.requireText("lemma", "seed") })
        for (seed in seeds) {
            val lemma = seed.requireText("lemma", "seed")
            fields.forEach { seed.requireText(it, lemma) }
        }
    }

    @Test
    fun falseCognatesSeed_severityIsOneOfTheTwoDocumentedValues() {
        for (seed in load("false_cognates_seed.json")) {
            assertTrue(seed.requireText("severity", "seed") in setOf("HIGH_RISK", "MILD_NUANCE"))
        }
    }

    @Test
    fun falseCognatesSeed_theCardTeachesSomethingDifferentFromTheLemma() {
        for (seed in load("false_cognates_seed.json")) {
            val lemma = seed.requireText("lemma", "seed")
            assertFalse(
                "$lemma: the real Spanish for the lookalike must not be the lemma itself",
                seed
                    .requireText("real_spanish_for_apparent", lemma)
                    .equals(lemma, ignoreCase = true),
            )
            assertFalse(
                "$lemma: the example translation must not be the Spanish example",
                seed.requireText("example_es", lemma) == seed.requireText("example_en", lemma),
            )
        }
    }

    // ----- curated_lessons_seed.json ------------------------------------------------------------

    private fun sentencesOf(lesson: JsonObject): List<JsonObject> =
        (lesson.getValue("content").jsonObject["sentences"] as JsonArray).map { it.jsonObject }

    @Test
    fun curatedLessons_idsAreUniqueAndAgreeWithTheLevel() {
        val lessons = load("curated_lessons_seed.json")
        val pattern = Regex("""lvl(\d)_(\d{2})_[a-z0-9_]+""")

        assertUnique("lesson_id", lessons.map { it.requireText("lesson_id", "lesson") })
        for (lesson in lessons) {
            val id = lesson.requireText("lesson_id", "lesson")
            val match = pattern.matchEntire(id)
            assertTrue("$id must look like lvl0_01_some_slug", match != null)
            val level = (lesson["level"] as? JsonPrimitive)?.intOrNull
            assertTrue("$id: level must be 0, 1 or 2", level in 0..2)
            assertEquals(
                "$id: the id prefix must match level",
                level,
                match!!.groupValues[1].toInt()
            )
        }
    }

    @Test
    fun curatedLessons_sequenceOrderIsUniqueWithinALevelAndTheFileIsSortedByIt() {
        val lessons = load("curated_lessons_seed.json")
        val keys =
            lessons.map {
                val level = (it["level"] as JsonPrimitive).intOrNull!!
                val sequence = (it["sequence_order"] as? JsonPrimitive)?.intOrNull
                assertTrue(
                    "${it.text("lesson_id")}: sequence_order must be a non-negative integer",
                    sequence != null && sequence >= 0
                )
                level to sequence!!
            }

        assertEquals("Each (level, sequence_order) must be unique", keys.size, keys.toSet().size)
        assertEquals(
            "The file must be sorted by level, then sequence_order",
            keys.sortedWith(compareBy({ it.first }, { it.second })),
            keys
        )
    }

    @Test
    fun curatedLessons_everyLessonHasATitleAConceptAndSentences() {
        for (lesson in load("curated_lessons_seed.json")) {
            val id = lesson.requireText("lesson_id", "lesson")
            lesson.requireText("title", id)
            lesson.requireText("target_concept", id)
            assertTrue("$id has no sentences", sentencesOf(lesson).isNotEmpty())
        }
    }

    @Test
    fun curatedLessons_everySentenceHasItsTextAndTranslationAndNoSentenceRepeats() {
        val spanish = mutableListOf<String>()
        for (lesson in load("curated_lessons_seed.json")) {
            val id = lesson.requireText("lesson_id", "lesson")
            for (sentence in sentencesOf(lesson)) {
                spanish.add(sentence.requireText("sentence_es", id))
                sentence.requireText("translation_en", id)
            }
        }

        assertUnique("sentence_es across lessons", spanish)
    }

    @Test
    fun curatedLessons_theInterlinearGlossHasOneEntryPerWordOfTheSentence() {
        for (lesson in load("curated_lessons_seed.json")) {
            for (sentence in sentencesOf(lesson)) {
                val text = sentence.requireText("sentence_es", "lesson")
                val gloss =
                    (sentence["interlinear_gloss"] as? JsonArray)?.map { it.jsonObject }.orEmpty()
                if (gloss.isEmpty()) continue

                // A gloss token may keep the punctuation of the sentence ("¿Necesita",
                // "información?").
                val glossed =
                    gloss.map { it.requireText("es", text).lowercase().trim { c -> !c.isLetter() } }
                assertEquals("Gloss words of '$text'", SentenceScorer.tokenizeWords(text), glossed)
                gloss.forEach { it.requireText("en", text) }
            }
        }
    }

    // ----- cognate_rules_seed.json --------------------------------------------------------------

    @Test
    fun cognateRules_idsAreUniqueAndTheSuffixesAreDashPrefixed() {
        val rules = load("cognate_rules_seed.json")

        assertUnique("rule_id", rules.map { it.requireText("rule_id", "rule") })
        for (rule in rules) {
            val id = rule.requireText("rule_id", "rule")
            assertTrue(
                "$id: english_suffix must start with '-'",
                rule.requireText("english_suffix", id).startsWith("-")
            )
            assertTrue(
                "$id: spanish_suffix must start with '-'",
                rule.requireText("spanish_suffix", id).startsWith("-")
            )
            rule.requireText("spelling_note", id)
        }
    }

    @Test
    fun cognateRules_everyExampleIsAPairThatEndsInItsRulesSuffixes() {
        for (rule in load("cognate_rules_seed.json")) {
            val id = rule.requireText("rule_id", "rule")
            val englishSuffix = rule.requireText("english_suffix", id).removePrefix("-")
            val spanishSuffix = rule.requireText("spanish_suffix", id).removePrefix("-")
            val examples = (rule["examples"] as JsonArray).map { it.jsonObject }

            val english = examples.map { it.requireText("english", id) }
            val spanish = examples.map { it.requireText("spanish", id) }
            assertUnique("english example in $id", english)
            assertUnique("spanish example in $id", spanish)
            english.forEach {
                assertTrue("$id: '$it' must end with -$englishSuffix", it.endsWith(englishSuffix))
            }
            spanish.forEach {
                assertTrue("$id: '$it' must end with -$spanishSuffix", it.endsWith(spanishSuffix))
            }
        }
    }

    @Test
    fun cognateRules_theGenderRuleIsEitherMissingOrFilled() {
        for (rule in load("cognate_rules_seed.json")) {
            val gender = rule["gender_rule"]
            if (gender != null && gender !is JsonNull) {
                assertFalse(
                    rule.text("rule_id") + ": gender_rule must not be blank",
                    (gender as JsonPrimitive).content.isBlank()
                )
            }
        }
    }

    // ----- inclusive language (AGENTS.md "Naming") ----------------------------------------------

    private val disallowedTerms =
        listOf("dummy", "blacklist", "whitelist", "master", "slave", "insane", "dumb")

    /**
     * The JSON keys that hold English prose; Spanish words are not scanned (a "máster" is fine).
     */
    private val englishKeys =
        setOf(
            "english_gloss",
            "translation_en",
            "grammar_callout",
            "disambiguation_tip",
            "example_en",
            "apparent_english",
            "actual_english",
            "spelling_note",
            "gender_rule",
            "title",
            "target_concept",
            "en",
            "english",
        )

    private fun englishText(element: JsonElement): List<String> =
        when (element) {
            is JsonObject ->
                element.flatMap { (key, value) ->
                    if (key in englishKeys && value is JsonPrimitive) listOf(value.content)
                    else englishText(value)
                }
            is JsonArray -> element.flatMap(::englishText)
            else -> emptyList()
        }

    private fun disallowedIn(text: String): List<String> =
        disallowedTerms.filter {
            Regex("""\b$it\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }

    @Test
    fun inclusiveLanguage_theScannerFindsTheDisallowedTermsItIsGiven() {
        assertEquals(listOf("dummy"), disallowedIn("Spanish has no dummy auxiliary"))
        assertEquals(listOf("blacklist", "whitelist"), disallowedIn("A Blacklist and a WHITELIST"))
        assertTrue(disallowedIn("A mastery of mastering").isEmpty())
    }

    @Test
    fun inclusiveLanguage_noSeedFileUsesADisallowedTermInItsEnglishText() {
        val files =
            listOf(
                "dictionary_seed.json",
                "false_cognates_seed.json",
                "curated_lessons_seed.json",
                "cognate_rules_seed.json",
            )
        for (name in files) {
            for (entry in load(name)) {
                for (text in englishText(entry)) {
                    assertTrue(
                        "$name: '$text' uses ${disallowedIn(text)}",
                        disallowedIn(text).isEmpty()
                    )
                }
            }
        }
    }
}
