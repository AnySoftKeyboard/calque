package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.data.room.dao.CuratedLessonsDao
import com.anysoftkeyboard.calque.data.seed.CuratedLessonsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Room-backed implementation of [CuratedLessonsRepository] using [CuratedLessonsDao]. */
class RoomCuratedLessonsRepository(
    val dao: CuratedLessonsDao,
) : CuratedLessonsRepository {

    override fun getAll(): List<CuratedLesson> {
        return runBlocking(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }
    }

    override fun getByLevel(level: Int): List<CuratedLesson> {
        return runBlocking(Dispatchers.IO) { dao.getByLevel(level).map { it.toDomain() } }
    }

    override fun getById(lessonId: String): CuratedLesson? {
        val trimmed = lessonId.trim()
        if (trimmed.isEmpty()) return null
        return runBlocking(Dispatchers.IO) { dao.getById(trimmed)?.toDomain() }
    }
}
