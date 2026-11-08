package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Result of expanding the false cognate seeds. */
data class FalseCognateExpansion(
    val rows: Int,
    /**
     * Seed lemmas that have no dictionary rows. They are still stored under their own lemma form,
     * but not under any inflection, which usually means the lemma is too rare for the dictionary
     * subset, or that the seed is keyed by an inflected form instead of the dictionary lemma
     * (`embarazada` instead of `embarazado`).
     */
    val lemmasWithoutDictionaryRows: List<String>,
)

/**
 * Expands curated false cognate seeds across all conjugated and inflected forms in the dictionary.
 *
 * Implements Step 3 from INGESTION.md.
 */
object FalseCognatesExpander {

    /**
     * Expands false cognates from an input stream and stores them into the database.
     *
     * @return Count of false cognate entries inserted.
     */
    fun expand(inputStream: InputStream, database: SeedDatabase): Int =
        expandWithReport(inputStream, database).rows

    /** Like [expand], but also reports the seeds that found no dictionary rows. */
    fun expandWithReport(inputStream: InputStream, database: SeedDatabase): FalseCognateExpansion {
        val seeds = parseSeeds(inputStream)
        val rows = mutableListOf<FalseCognateRow>()
        val withoutRows = mutableListOf<String>()

        for (seed in seeds) {
            val matchingSurfaces = database.querySurfacesForLemma(seed.lemma).toMutableSet()
            if (matchingSurfaces.isEmpty()) withoutRows.add(seed.lemma)
            // Ensure the lowercased base lemma is always included
            matchingSurfaces.add(seed.lemma.lowercase(Locale.ROOT))

            for (surface in matchingSurfaces) {
                rows.add(
                    FalseCognateRow(
                        surfaceForm = surface,
                        lemma = seed.lemma,
                        apparentEnglish = seed.apparentEnglish,
                        actualEnglish = seed.actualEnglish,
                        realSpanishForApparent = seed.realSpanishForApparent,
                        severity = seed.severity,
                        disambiguationTip = seed.disambiguationTip,
                        exampleEs = seed.exampleEs,
                        exampleEn = seed.exampleEn,
                    ),
                )
            }
        }

        database.insertFalseCognatesBatch(rows)
        return FalseCognateExpansion(rows.size, withoutRows)
    }

    /** Parses the false cognates seed JSON stream into a list of [FalseCognateSeed] objects. */
    fun parseSeeds(inputStream: InputStream): List<FalseCognateSeed> {
        val jsonText = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { it.readText() }
        val array = Json.parseToJsonElement(jsonText).jsonArray
        return array.mapNotNull { parseSeedElement(it.jsonObject) }
    }

    private fun parseSeedElement(obj: JsonObject): FalseCognateSeed? {
        val lemma = obj["lemma"]?.jsonPrimitive?.contentOrNull ?: return null
        val apparentEnglish = obj["apparent_english"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val actualEnglish = obj["actual_english"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val realSpanish = obj["real_spanish_for_apparent"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val severity = obj["severity"]?.jsonPrimitive?.contentOrNull ?: "HIGH_RISK"
        val tip = obj["disambiguation_tip"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val exampleEs = obj["example_es"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val exampleEn = obj["example_en"]?.jsonPrimitive?.contentOrNull.orEmpty()

        return FalseCognateSeed(
            lemma = lemma,
            apparentEnglish = apparentEnglish,
            actualEnglish = actualEnglish,
            realSpanishForApparent = realSpanish,
            severity = severity,
            disambiguationTip = tip,
            exampleEs = exampleEs,
            exampleEn = exampleEn,
        )
    }
}
