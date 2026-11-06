package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anysoftkeyboard.calque.data.models.CuratedLesson

/** Room entity for the read-only [curated_lessons] seed table. */
@Entity(
    tableName = "curated_lessons",
    indices = [Index(value = ["level"], name = "idx_curated_lessons_level")],
)
data class CuratedLessonEntity(
    @PrimaryKey @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "level") val level: Int,
    @ColumnInfo(name = "sequence_order") val sequenceOrder: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "target_concept") val targetConcept: String,
    @ColumnInfo(name = "content_json") val contentJson: String,
) {
    fun toDomain(): CuratedLesson {
        return CuratedLesson(
            lessonId = lessonId,
            level = level,
            sequenceOrder = sequenceOrder,
            title = title,
            targetConcept = targetConcept,
            contentJson = contentJson,
        )
    }

    companion object {
        fun fromDomain(lesson: CuratedLesson): CuratedLessonEntity {
            return CuratedLessonEntity(
                lessonId = lesson.lessonId,
                level = lesson.level,
                sequenceOrder = lesson.sequenceOrder,
                title = lesson.title,
                targetConcept = lesson.targetConcept,
                contentJson = lesson.contentJson,
            )
        }
    }
}
