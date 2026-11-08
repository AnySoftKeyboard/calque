package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.room.dao.DictionaryDao
import com.anysoftkeyboard.calque.data.room.entities.DictionaryEntity
import com.anysoftkeyboard.calque.data.seed.DictionaryRepository
import com.anysoftkeyboard.calque.data.seed.groupReadings
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Room-backed implementation of [DictionaryRepository] using [DictionaryDao]. A spelling's primary
 * reading is the entity with the lowest `reading_order`; the others are its alternatives.
 */
class RoomDictionaryRepository(
    val dao: DictionaryDao,
) : DictionaryRepository {

    override fun getBySurfaceForm(surfaceForm: String): DictionaryEntry? {
        val normalized = surfaceForm.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null
        return runBlocking(Dispatchers.IO) {
            groupReadings(inReadingOrder(dao.getReadingsBySurfaceForm(normalized)))[normalized]
        }
    }

    override fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, DictionaryEntry> {
        val unique =
            surfaceForms
                .map { it.lowercase(Locale.ROOT).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        if (unique.isEmpty()) return emptyMap()

        return runBlocking(Dispatchers.IO) {
            val results = mutableMapOf<String, DictionaryEntry>()
            for (chunk in unique.chunked(MAX_BATCH_CHUNK_SIZE)) {
                results.putAll(groupReadings(inReadingOrder(dao.getBySurfaceForms(chunk))))
            }
            results
        }
    }

    override fun getSurfacesForLemma(lemma: String): List<String> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()
        return runBlocking(Dispatchers.IO) { dao.getSurfacesForLemma(normalized) }
    }

    override fun getEntriesForLemma(lemma: String): List<DictionaryEntry> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()
        return runBlocking(Dispatchers.IO) { inReadingOrder(dao.getEntriesForLemma(normalized)) }
    }

    override fun searchByPrefix(prefix: String, limit: Int): List<DictionaryEntry> {
        val normalized = prefix.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty() || limit <= 0) return emptyList()
        val escaped = normalized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return runBlocking(Dispatchers.IO) {
            dao.searchByPrefix(escaped, limit).map { it.toDomain() }
        }
    }

    /** Sorts by spelling, then reading, so the result does not depend on the DAO's own order. */
    private fun inReadingOrder(entities: List<DictionaryEntity>): List<DictionaryEntry> =
        entities.sortedWith(compareBy({ it.surfaceForm }, { it.readingOrder })).map {
            it.toDomain()
        }

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
    }
}
