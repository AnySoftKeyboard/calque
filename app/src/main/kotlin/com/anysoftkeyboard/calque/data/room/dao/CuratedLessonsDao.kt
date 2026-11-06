package com.anysoftkeyboard.calque.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anysoftkeyboard.calque.data.room.entities.CuratedLessonEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for the [CuratedLessonEntity] table. */
@Dao
interface CuratedLessonsDao {
    @Query("SELECT * FROM curated_lessons ORDER BY level ASC, sequence_order ASC")
    suspend fun getAll(): List<CuratedLessonEntity>

    @Query("SELECT * FROM curated_lessons ORDER BY level ASC, sequence_order ASC")
    fun getAllFlow(): Flow<List<CuratedLessonEntity>>

    @Query("SELECT * FROM curated_lessons WHERE lesson_id = :lessonId LIMIT 1")
    suspend fun getById(lessonId: String): CuratedLessonEntity?

    @Query("SELECT * FROM curated_lessons WHERE level = :level ORDER BY sequence_order ASC")
    suspend fun getByLevel(level: Int): List<CuratedLessonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<CuratedLessonEntity>)

    @Query("SELECT COUNT(*) FROM curated_lessons") suspend fun count(): Int
}
