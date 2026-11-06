package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.room.dao.FalseCognatesDao
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Room-backed implementation of [FalseCognatesRepository] using [FalseCognatesDao]. */
class RoomFalseCognatesRepository(
    val dao: FalseCognatesDao,
) : FalseCognatesRepository {

    override fun getBySurfaceForm(surfaceForm: String): FalseCognateEntry? {
        val normalized = surfaceForm.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null
        return runBlocking(Dispatchers.IO) { dao.getBySurfaceForm(normalized)?.toDomain() }
    }

    override fun getBySurfaceForms(
        surfaceForms: Collection<String>
    ): Map<String, FalseCognateEntry> {
        val unique =
            surfaceForms
                .map { it.lowercase(Locale.ROOT).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        if (unique.isEmpty()) return emptyMap()

        return runBlocking(Dispatchers.IO) {
            val results = mutableMapOf<String, FalseCognateEntry>()
            for (chunk in unique.chunked(MAX_BATCH_CHUNK_SIZE)) {
                val entities = dao.getBySurfaceForms(chunk)
                for (entity in entities) {
                    results[entity.surfaceForm] = entity.toDomain()
                }
            }
            results
        }
    }

    override fun getByLemma(lemma: String): List<FalseCognateEntry> {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return emptyList()
        return runBlocking(Dispatchers.IO) { dao.getByLemma(normalized).map { it.toDomain() } }
    }

    override fun getAll(): List<FalseCognateEntry> {
        return runBlocking(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }
    }

    private companion object {
        const val MAX_BATCH_CHUNK_SIZE = 400
    }
}
