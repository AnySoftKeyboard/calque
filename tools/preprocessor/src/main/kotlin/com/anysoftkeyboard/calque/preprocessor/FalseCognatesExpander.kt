package com.anysoftkeyboard.calque.preprocessor

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    fun expand(inputStream: InputStream, database: SeedDatabase): Int {
        val seeds = parseSeeds(inputStream)
        val rows = mutableListOf<FalseCognateRow>()

        for (seed in seeds) {
            val matchingSurfaces = database.querySurfacesForLemma(seed.lemma).toMutableSet()
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
        return rows.size
    }

    /** Parses the false cognates seed JSON stream into a list of [FalseCognateSeed] objects. */
    fun parseSeeds(inputStream: InputStream): List<FalseCognateSeed> {
        val jsonText = InputStreamReader(inputStream, StandardCharsets.UTF_8).use { it.readText() }
        val array = Json.parseToJsonElement(jsonText).jsonArray
        val seeds = mutableListOf<FalseCognateSeed>()

        for (elem in array) {
            val obj = elem.jsonObject
            val lemma = obj["lemma"]?.jsonPrimitive?.content ?: continue
            val apparentEnglish = obj["apparent_english"]?.jsonPrimitive?.content.orEmpty()
            val actualEnglish = obj["actual_english"]?.jsonPrimitive?.content.orEmpty()
            val realSpanish = obj["real_spanish_for_apparent"]?.jsonPrimitive?.content.orEmpty()
            val severity = obj["severity"]?.jsonPrimitive?.content ?: "HIGH_RISK"
            val tip = obj["disambiguation_tip"]?.jsonPrimitive?.content.orEmpty()
            val exampleEs = obj["example_es"]?.jsonPrimitive?.content.orEmpty()
            val exampleEn = obj["example_en"]?.jsonPrimitive?.content.orEmpty()

            seeds.add(
                FalseCognateSeed(
                    lemma = lemma,
                    apparentEnglish = apparentEnglish,
                    actualEnglish = actualEnglish,
                    realSpanishForApparent = realSpanish,
                    severity = severity,
                    disambiguationTip = tip,
                    exampleEs = exampleEs,
                    exampleEn = exampleEn,
                ),
            )
        }
        return seeds
    }
}
