package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.models.UserCustomText
import com.anysoftkeyboard.calque.data.room.dao.UserCustomTextDao
import com.anysoftkeyboard.calque.data.room.entities.UserCustomTextEntity
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomUserCustomTextRepositoryTest {

    private class FakeUserCustomTextDao : UserCustomTextDao {
        val rows = linkedMapOf<Long, UserCustomTextEntity>()
        private val nextId = AtomicLong(1L)

        override suspend fun getAll() = rows.values.sortedByDescending { it.createdTimestamp }

        override fun getAllFlow(): Flow<List<UserCustomTextEntity>> = flowOf(emptyList())

        override suspend fun getById(textId: Long) = rows[textId]

        override suspend fun insert(text: UserCustomTextEntity): Long {
            val id = if (text.textId == 0L) nextId.getAndIncrement() else text.textId
            rows[id] = text.copy(textId = id)
            return id
        }

        override suspend fun update(text: UserCustomTextEntity): Int {
            if (!rows.containsKey(text.textId)) return 0
            rows[text.textId] = text
            return 1
        }

        override suspend fun delete(textId: Long) = if (rows.remove(textId) != null) 1 else 0

        override suspend fun count() = rows.size
    }

    private val dao = FakeUserCustomTextDao()
    private val repo = RoomUserCustomTextRepository(dao)

    private fun text(title: String, created: Long, id: Long = 0L) =
        UserCustomText(
            textId = id,
            title = title,
            originalSpanish = "Hola mundo.",
            readabilityLevel = 1,
            createdTimestamp = created,
        )

    @Test
    fun insertThenGetById_roundTrips() {
        val id = repo.insert(text("Uno", 10L))

        assertTrue(id > 0L)
        assertEquals("Uno", repo.getById(id)?.title)
    }

    @Test
    fun update_existingRowReturnsTrueAndPersists() {
        val id = repo.insert(text("Uno", 10L))

        assertTrue(repo.update(text("Dos", 10L, id).copy(simplifiedSpanish = "Hola.")))

        assertEquals("Dos", repo.getById(id)?.title)
        assertEquals("Hola.", repo.getById(id)?.simplifiedSpanish)
    }

    @Test
    fun update_missingRowReturnsFalseAndDoesNotInsert() {
        assertFalse(repo.update(text("Ghost", 10L, id = 42L)))

        assertEquals(0, dao.rows.size)
        assertNull(repo.getById(42L))
    }

    @Test
    fun update_withoutPersistedIdIsRejected() {
        assertFalse(repo.update(text("NoId", 10L)))
    }

    @Test
    fun getAll_ordersNewestFirst() {
        repo.insert(text("old", 1L))
        repo.insert(text("new", 2L))

        assertEquals(listOf("new", "old"), repo.getAll().map { it.title })
    }

    @Test
    fun delete_removesRowAndReportsMissing() {
        val id = repo.insert(text("Uno", 10L))

        assertTrue(repo.delete(id))
        assertFalse(repo.delete(id))
        assertFalse(repo.delete(0L))
    }
}
