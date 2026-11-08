package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anysoftkeyboard.calque.data.models.UserCustomText

/** Room entity for the mutable [user_custom_texts] table. */
@Entity(
    tableName = "user_custom_texts",
    indices = [Index(value = ["created_timestamp"], name = "idx_user_custom_texts_created")],
)
data class UserCustomTextEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "text_id") val textId: Long = 0L,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "original_spanish") val originalSpanish: String,
    @ColumnInfo(name = "simplified_spanish") val simplifiedSpanish: String?,
    @ColumnInfo(name = "readability_level") val readabilityLevel: Int,
    @ColumnInfo(name = "created_timestamp") val createdTimestamp: Long,
) {
    fun toDomain(): UserCustomText {
        return UserCustomText(
            textId = textId,
            title = title,
            originalSpanish = originalSpanish,
            simplifiedSpanish = simplifiedSpanish,
            readabilityLevel = readabilityLevel,
            createdTimestamp = createdTimestamp,
        )
    }

    companion object {
        fun fromDomain(domain: UserCustomText): UserCustomTextEntity {
            return UserCustomTextEntity(
                textId = domain.textId,
                title = domain.title,
                originalSpanish = domain.originalSpanish,
                simplifiedSpanish = domain.simplifiedSpanish,
                readabilityLevel = domain.readabilityLevel,
                createdTimestamp = domain.createdTimestamp,
            )
        }
    }
}
