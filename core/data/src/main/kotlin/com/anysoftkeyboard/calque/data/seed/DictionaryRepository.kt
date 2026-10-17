package com.anysoftkeyboard.calque.data.seed

import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import java.sql.Connection
import java.sql.ResultSet
import java.util.Locale

/** Repository interface for accessing read-only seed dictionary data. */
interface DictionaryRepository {
    /** Looks up a single dictionary entry by surface form (case-insensitive). */
    fun getBySurfaceForm(surfaceForm: String): DictionaryEntry?

    /**
     * Batch queries dictionary entries for a collection of surface forms. Returns a map keyed by
     * normalized lowercase surface form.
     */
    fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, DictionaryEntry>

    /** Retrieves all surface forms associated with the given base lemma. */
    fun getSurfacesForLemma(lemma: String): List<String>

    /** Retrieves all dictionary entries associated with the given base lemma. */
    fun getEntriesForLemma(lemma: String): List<DictionaryEntry>

    /** Searches dictionary entries starting with the given surface form prefix. */
    fun searchByPrefix(prefix: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<DictionaryEntry>

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 50
    }
}

/** SQLite-backed implementation of [DictionaryRepository]. */
class SqliteDictionaryRepository(
    private val connection: Connection,
) : DictionaryRepository {

    override fun getBySurfaceForm(surfaceForm: String): DictionaryEntry? {
        val normalized = surfaceForm.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null

        val sql =
            """
            SELECT surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank
            FROM dictionary
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

    override fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, DictionaryEntry> {
        val uniqueForms =
            surfaceForms
                .map { it.lowercase(Locale.ROOT).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        if (uniqueForms.isEmpty()) return emptyMap()

        val results = mutableMapOf<String, DictionaryEntry>()
        for (chunk in uniqueForms.chunked(MAX_BATCH_CHUNK_SIZE)) {
            queryChunk(chunk, results)
        }
        return results
    }

    private fun queryChunk(chunk: List<String>, results: MutableMap<String, DictionaryEntry>) {
        val placeholders = chunk.joinToString(",") { "?" }
        val sql =
            """
            SELECT surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank
            FROM dictionary
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

    override fun getSurfacesForLemma(lemma: String): List<String> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()

        val sql = "SELECT surface_form FROM dictionary WHERE lemma = ? ORDER BY surface_form ASC"
        val surfaces = mutableListOf<String>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, normalized)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    surfaces.add(rs.getString("surface_form"))
                }
            }
        }
        return surfaces
    }

    override fun getEntriesForLemma(lemma: String): List<DictionaryEntry> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()

        val sql =
            """
            SELECT surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank
            FROM dictionary
            WHERE lemma = ?
            ORDER BY surface_form ASC
            """
                .trimIndent()

        val entries = mutableListOf<DictionaryEntry>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, normalized)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    entries.add(mapRow(rs))
                }
            }
        }
        return entries
    }

    override fun searchByPrefix(prefix: String, limit: Int): List<DictionaryEntry> {
        val normalized = prefix.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty() || limit <= 0) return emptyList()

        val escapedPrefix = normalized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val sql =
            """
            SELECT surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank
            FROM dictionary
            WHERE surface_form LIKE ? ESCAPE '\'
            ORDER BY frequency_rank IS NULL, frequency_rank ASC, surface_form ASC
            LIMIT ?
            """
                .trimIndent()

        val entries = mutableListOf<DictionaryEntry>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, "$escapedPrefix%")
            stmt.setInt(2, limit)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    entries.add(mapRow(rs))
                }
            }
        }
        return entries
    }

    private fun mapRow(rs: ResultSet): DictionaryEntry {
        val freqObj = rs.getObject("frequency_rank")
        val freqRank = if (freqObj != null) rs.getInt("frequency_rank") else null
        return DictionaryEntry(
            surfaceForm = rs.getString("surface_form"),
            lemma = rs.getString("lemma"),
            pos = rs.getString("pos"),
            grammarInfo = rs.getString("grammar_info"),
            englishGloss = rs.getString("english_gloss"),
            isCognate = rs.getInt("is_cognate") == 1,
            frequencyRank = freqRank,
        )
    }

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
    }
}
