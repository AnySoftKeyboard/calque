package com.anysoftkeyboard.calque.data.seed

import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import java.sql.Connection
import java.sql.ResultSet
import java.util.Locale

/** Repository interface for accessing false cognate trap entries. */
interface FalseCognatesRepository {
    /** Looks up a false cognate entry by surface form (case-insensitive). */
    fun getBySurfaceForm(surfaceForm: String): FalseCognateEntry?

    /**
     * Batch queries false cognate entries for a collection of surface forms. Returns a map keyed by
     * normalized lowercase surface form.
     */
    fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, FalseCognateEntry>

    /** Retrieves all false cognate forms belonging to a given lemma. */
    fun getByLemma(lemma: String): List<FalseCognateEntry>

    /** Returns all false cognate entries stored in the database. */
    fun getAll(): List<FalseCognateEntry>
}

/** SQLite-backed implementation of [FalseCognatesRepository]. */
class SqliteFalseCognatesRepository(
    private val connection: Connection,
) : FalseCognatesRepository {

    override fun getBySurfaceForm(surfaceForm: String): FalseCognateEntry? {
        val normalized = surfaceForm.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null

        val sql =
            """
            SELECT surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                   severity, disambiguation_tip, example_es, example_en
            FROM false_cognates
            WHERE surface_form = ?
            LIMIT 1
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, normalized)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapRow(rs) else null
            }
        }
    }

    override fun getBySurfaceForms(
        surfaceForms: Collection<String>
    ): Map<String, FalseCognateEntry> {
        val uniqueForms =
            surfaceForms
                .map { it.lowercase(Locale.ROOT).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        if (uniqueForms.isEmpty()) return emptyMap()

        val results = mutableMapOf<String, FalseCognateEntry>()
        for (chunk in uniqueForms.chunked(MAX_BATCH_CHUNK_SIZE)) {
            queryChunk(chunk, results)
        }
        return results
    }

    private fun queryChunk(chunk: List<String>, results: MutableMap<String, FalseCognateEntry>) {
        val placeholders = chunk.joinToString(",") { "?" }
        val sql =
            """
            SELECT surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                   severity, disambiguation_tip, example_es, example_en
            FROM false_cognates
            WHERE surface_form IN ($placeholders)
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            chunk.forEachIndexed { index, form -> stmt.setString(index + 1, form) }
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val entry = mapRow(rs)
                    results[entry.surfaceForm] = entry
                }
            }
        }
    }

    override fun getByLemma(lemma: String): List<FalseCognateEntry> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()

        val sql =
            """
            SELECT surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                   severity, disambiguation_tip, example_es, example_en
            FROM false_cognates
            WHERE lemma = ?
            ORDER BY surface_form ASC
            """
                .trimIndent()

        val results = mutableListOf<FalseCognateEntry>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, normalized)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun getAll(): List<FalseCognateEntry> {
        val sql =
            """
            SELECT surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                   severity, disambiguation_tip, example_es, example_en
            FROM false_cognates
            ORDER BY surface_form ASC
            """
                .trimIndent()

        val results = mutableListOf<FalseCognateEntry>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    private fun mapRow(rs: ResultSet): FalseCognateEntry {
        return FalseCognateEntry(
            surfaceForm = rs.getString("surface_form"),
            lemma = rs.getString("lemma"),
            apparentEnglish = rs.getString("apparent_english"),
            actualEnglish = rs.getString("actual_english"),
            realSpanishForApparent = rs.getString("real_spanish_for_apparent"),
            severity = rs.getString("severity"),
            disambiguationTip = rs.getString("disambiguation_tip"),
            exampleEs = rs.getString("example_es"),
            exampleEn = rs.getString("example_en"),
        )
    }

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
    }
}
