package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.CognateDetector
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Processor for streaming Kaikki JSONL dictionary dumps and populating the dictionary table.
 *
 * Implements Step 1 from INGESTION.md.
 */
object KaikkiProcessor {
    private const val DEFAULT_BATCH_SIZE = 5000

    /**
     * Streams the Kaikki JSONL input, extracts lemmas and inflected forms, tags cognates, resolves
     * SUBTLEX frequencies, and flattens them into the dictionary table.
     *
     * @return The count of total dictionary rows inserted.
     */
    fun processStream(
        inputStream: InputStream,
        sublexFrequencies: Map<String, Int>,
        database: SeedDatabase,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): Long {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val batch = mutableListOf<DictionaryRow>()
        var insertedRows = 0L

        for (rawLine in reader.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val rows = parseKaikkiLine(line, sublexFrequencies)
            batch.addAll(rows)

            if (batch.size >= batchSize) {
                database.insertDictionaryBatch(batch)
                insertedRows += batch.size
                batch.clear()
            }
        }

        if (batch.isNotEmpty()) {
            database.insertDictionaryBatch(batch)
            insertedRows += batch.size
            batch.clear()
        }

        return insertedRows
    }

    /** Parses a single Kaikki JSON line into base and inflected [DictionaryRow]s. */
    fun parseKaikkiLine(
        jsonLine: String,
        sublexFrequencies: Map<String, Int>,
    ): List<DictionaryRow> {
        val jsonElement = Json.parseToJsonElement(jsonLine).jsonObject
        val lemma = jsonElement["word"]?.jsonPrimitive?.content ?: return emptyList()
        val pos = jsonElement["pos"]?.jsonPrimitive?.content ?: ""
        val primaryGloss = extractPrimaryGloss(jsonElement)
        val normalizedLemma = lemma.lowercase(Locale.ROOT)
        val freqRank = sublexFrequencies[normalizedLemma]
        val isCognate = CognateDetector.detectCognate(lemma, primaryGloss)

        val results = mutableListOf<DictionaryRow>()

        // 1. Base lemma row
        results.add(
            DictionaryRow(
                surfaceForm = normalizedLemma,
                lemma = lemma,
                pos = pos,
                grammarInfo = "base",
                englishGloss = primaryGloss,
                isCognate = isCognate,
                frequencyRank = freqRank,
            ),
        )

        // 2. Expand inflected forms
        val formsArray = jsonElement["forms"]?.jsonArray
        if (formsArray != null) {
            for (formElement in formsArray) {
                val formObj = formElement.jsonObject
                val surface =
                    formObj["form"]?.jsonPrimitive?.content
                        ?: formObj["surface"]?.jsonPrimitive?.content
                        ?: continue
                val tags =
                    formObj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val grammar = if (tags.isNotEmpty()) tags.joinToString(", ") else null

                results.add(
                    DictionaryRow(
                        surfaceForm = surface.lowercase(Locale.ROOT),
                        lemma = lemma,
                        pos = pos,
                        grammarInfo = grammar,
                        englishGloss = primaryGloss,
                        isCognate = isCognate,
                        frequencyRank = freqRank,
                    ),
                )
            }
        }

        return results
    }

    private fun extractPrimaryGloss(jsonObj: kotlinx.serialization.json.JsonObject): String {
        val senses = jsonObj["senses"]?.jsonArray ?: return ""
        for (senseElement in senses) {
            val senseObj = senseElement.jsonObject
            val glosses = senseObj["glosses"]?.jsonArray
            val firstGloss = glosses?.firstOrNull()?.jsonPrimitive?.content
            if (!firstGloss.isNullOrBlank()) {
                return firstGloss
            }
        }
        return ""
    }
}
