package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.room.dao.DictionaryDao
import com.anysoftkeyboard.calque.data.seed.DictionaryRepository
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Room-backed implementation of [DictionaryRepository] using [DictionaryDao]. */
class RoomDictionaryRepository(
    val dao: DictionaryDao,
) : DictionaryRepository {

    override fun getBySurfaceForm(surfaceForm: String): DictionaryEntry? {
        val normalized = surfaceForm.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null
        return runBlocking(Dispatchers.IO) { dao.getBySurfaceForm(normalized)?.toDomain() }
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
                val entities = dao.getBySurfaceForms(chunk)
                for (entity in entities) {
                    results[entity.surfaceForm] = entity.toDomain()
                }
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
        return runBlocking(Dispatchers.IO) {
            dao.getEntriesForLemma(normalized).map { it.toDomain() }
        }
    }

    override fun searchByPrefix(prefix: String, limit: Int): List<DictionaryEntry> {
        val normalized = prefix.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty() || limit <= 0) return emptyList()
        val escaped = normalized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return runBlocking(Dispatchers.IO) {
            dao.searchByPrefix(escaped, limit).map { it.toDomain() }
        }
    }

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
    }
}
