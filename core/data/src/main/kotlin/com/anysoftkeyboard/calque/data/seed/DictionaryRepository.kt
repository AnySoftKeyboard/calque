package com.anysoftkeyboard.calque.data.seed

import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import java.sql.Connection
import java.sql.ResultSet
import java.util.Locale

/**
 * Repository interface for accessing read-only seed dictionary data.
 *
 * A spelling can have several readings (`padre` is a noun and an informal adjective). Lookups by
 * spelling return the primary reading with the other readings in [DictionaryEntry.alternatives].
 */
interface DictionaryRepository {
    /** Looks up the entry for a surface form (case-insensitive), alternatives included. */
    fun getBySurfaceForm(surfaceForm: String): DictionaryEntry?

    /**
     * Batch queries dictionary entries for a collection of surface forms. Returns a map keyed by
     * normalized lowercase surface form.
     */
    fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, DictionaryEntry>

    /** Retrieves all surface forms associated with the given base lemma. */
    fun getSurfacesForLemma(lemma: String): List<String>

    /** Retrieves the dictionary entries (one per reading, no alternatives) of the given lemma. */
    fun getEntriesForLemma(lemma: String): List<DictionaryEntry>

    /** Searches primary readings whose surface form starts with the given prefix. */
    fun searchByPrefix(prefix: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<DictionaryEntry>

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 50
    }
}

/**
 * Groups [entries] of one or more spellings into one [DictionaryEntry] per spelling: the first
 * entry of a spelling (the list must be ordered by `reading_order`) is the primary reading and the
 * rest become its alternatives.
 */
fun groupReadings(entries: List<DictionaryEntry>): Map<String, DictionaryEntry> {
    val grouped = LinkedHashMap<String, DictionaryEntry>()
    for ((surface, readings) in entries.groupBy { it.surfaceForm }) {
        grouped[surface] = readings.first().copy(alternatives = readings.drop(1))
    }
    return grouped
}

/** SQLite-backed implementation of [DictionaryRepository]. */
class SqliteDictionaryRepository(
    private val connection: Connection,
) : DictionaryRepository {

    override fun getBySurfaceForm(surfaceForm: String): DictionaryEntry? {
        val normalized = surfaceForm.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null
        return getBySurfaceForms(listOf(normalized))[normalized]
    }

    override fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, DictionaryEntry> {
        val uniqueForms =
            surfaceForms
                .map { it.lowercase(Locale.ROOT).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        if (uniqueForms.isEmpty()) return emptyMap()

        val results = LinkedHashMap<String, DictionaryEntry>()
        for (chunk in uniqueForms.chunked(MAX_BATCH_CHUNK_SIZE)) {
            results.putAll(groupReadings(queryChunk(chunk)))
        }
        return results
    }

    private fun queryChunk(chunk: List<String>): List<DictionaryEntry> {
        val placeholders = chunk.joinToString(",") { "?" }
        val sql =
            """
            SELECT $COLUMNS
            FROM dictionary
            WHERE surface_form IN ($placeholders)
            ORDER BY surface_form ASC, reading_order ASC
            """
                .trimIndent()

        val entries = mutableListOf<DictionaryEntry>()
        connection.prepareStatement(sql).use { stmt ->
            chunk.forEachIndexed { index, form -> stmt.setString(index + 1, form) }
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    entries.add(mapRow(rs))
                }
            }
        }
        return entries
    }

    override fun getSurfacesForLemma(lemma: String): List<String> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()

        val sql =
            "SELECT DISTINCT surface_form FROM dictionary WHERE lemma = ? ORDER BY surface_form ASC"
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
            SELECT $COLUMNS
            FROM dictionary
            WHERE lemma = ?
            ORDER BY surface_form ASC, reading_order ASC
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
            SELECT $COLUMNS
            FROM dictionary
            WHERE reading_order = 0 AND surface_form LIKE ? ESCAPE '\'
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
        const val COLUMNS =
            "surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank"
    }
}
