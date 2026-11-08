package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.room.dao.DictionaryDao
import com.anysoftkeyboard.calque.data.room.entities.DictionaryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDictionaryRepositoryTest {

    private class FakeDictionaryDao(entities: List<DictionaryEntity> = emptyList()) :
        DictionaryDao {
        private val bySurface = entities.associateBy { it.surfaceForm }
        val batchSizes = mutableListOf<Int>()
        var lastPrefix: String? = null
        var prefixQueries = 0

        override suspend fun getBySurfaceForm(surfaceForm: String) = bySurface[surfaceForm]

        override suspend fun getBySurfaceForms(surfaceForms: List<String>): List<DictionaryEntity> {
            batchSizes.add(surfaceForms.size)
            return surfaceForms.mapNotNull { bySurface[it] }
        }

        override suspend fun getSurfacesForLemma(lemma: String) =
            bySurface.values.filter { it.lemma == lemma }.map { it.surfaceForm }.sorted()

        override suspend fun getEntriesForLemma(lemma: String) =
            bySurface.values.filter { it.lemma == lemma }.sortedBy { it.surfaceForm }

        override suspend fun searchByPrefix(prefix: String, limit: Int): List<DictionaryEntity> {
            lastPrefix = prefix
            prefixQueries++
            return emptyList()
        }

        override fun getBySurfaceFormFlow(surfaceForm: String): Flow<DictionaryEntity?> =
            flowOf(bySurface[surfaceForm])

        override suspend fun insertAll(entries: List<DictionaryEntity>) = Unit

        override suspend fun count() = bySurface.size
    }

    private fun entity(surface: String, lemma: String = surface) =
        DictionaryEntity(surface, lemma, "noun", null, "gloss of $surface", 1, 10)

    @Test
    fun getBySurfaceForm_normalizesCaseAndWhitespace() {
        val repo = RoomDictionaryRepository(FakeDictionaryDao(listOf(entity("hotel"))))

        val entry = repo.getBySurfaceForm("  HoTeL ")

        assertEquals("hotel", entry?.surfaceForm)
        assertTrue(entry?.isCognate == true)
    }

    @Test
    fun getBySurfaceForm_blankInputReturnsNullWithoutQuery() {
        val repo = RoomDictionaryRepository(FakeDictionaryDao(listOf(entity("hotel"))))

        assertNull(repo.getBySurfaceForm("   "))
    }

    @Test
    fun getBySurfaceForms_deduplicatesAndSkipsBlank() {
        val dao = FakeDictionaryDao(listOf(entity("hotel"), entity("casa")))
        val repo = RoomDictionaryRepository(dao)

        val result = repo.getBySurfaceForms(listOf("Hotel", "hotel", " ", "CASA", "missing"))

        assertEquals(setOf("hotel", "casa"), result.keys)
        assertEquals(listOf(3), dao.batchSizes)
    }

    @Test
    fun getBySurfaceForms_emptyInputDoesNotQuery() {
        val dao = FakeDictionaryDao()
        val repo = RoomDictionaryRepository(dao)

        assertTrue(repo.getBySurfaceForms(emptyList()).isEmpty())
        assertTrue(dao.batchSizes.isEmpty())
    }

    @Test
    fun getBySurfaceForms_splitsLargeBatchesBelowSqliteVariableLimit() {
        val words = (1..1000).map { "word$it" }
        val dao = FakeDictionaryDao(words.map { entity(it) })
        val repo = RoomDictionaryRepository(dao)

        val result = repo.getBySurfaceForms(words)

        assertEquals(1000, result.size)
        assertEquals(listOf(400, 400, 200), dao.batchSizes)
    }

    @Test
    fun searchByPrefix_escapesLikeWildcards() {
        val dao = FakeDictionaryDao()
        val repo = RoomDictionaryRepository(dao)

        repo.searchByPrefix("50%_a\\", 5)

        assertEquals("50\\%\\_a\\\\", dao.lastPrefix)
    }

    @Test
    fun searchByPrefix_invalidArgumentsReturnEmptyWithoutQuery() {
        val dao = FakeDictionaryDao()
        val repo = RoomDictionaryRepository(dao)

        assertTrue(repo.searchByPrefix("", 5).isEmpty())
        assertTrue(repo.searchByPrefix("ho", 0).isEmpty())
        assertEquals(0, dao.prefixQueries)
    }

    @Test
    fun lemmaLookups_returnAllInflections() {
        val dao =
            FakeDictionaryDao(
                listOf(entity("hablo", "hablar"), entity("hablas", "hablar"), entity("casa"))
            )
        val repo = RoomDictionaryRepository(dao)

        assertEquals(listOf("hablas", "hablo"), repo.getSurfacesForLemma("Hablar"))
        assertEquals(2, repo.getEntriesForLemma("hablar").size)
        assertTrue(repo.getSurfacesForLemma("").isEmpty())
    }
}
