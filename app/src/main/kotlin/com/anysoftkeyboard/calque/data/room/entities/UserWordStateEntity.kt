package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.linguistics.UserWordStatus

/** Room entity for the mutable [user_word_state] table. */
@Entity(
    tableName = "user_word_state",
    indices = [Index(value = ["status"], name = "idx_user_word_state_status")],
)
data class UserWordStateEntity(
    @PrimaryKey @ColumnInfo(name = "lemma") val lemma: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "starred") val starred: Int,
    @ColumnInfo(name = "saved_context_sentence") val savedContextSentence: String?,
    @ColumnInfo(name = "encounter_count") val encounterCount: Int,
    @ColumnInfo(name = "last_seen_timestamp") val lastSeenTimestamp: Long,
) {
    fun toDomain(): UserWordState {
        val wordStatus =
            runCatching { UserWordStatus.valueOf(status) }.getOrDefault(UserWordStatus.NEW)
        return UserWordState(
            lemma = lemma,
            status = wordStatus,
            starred = starred == 1,
            savedContextSentence = savedContextSentence,
            encounterCount = encounterCount,
            lastSeenTimestamp = lastSeenTimestamp,
        )
    }

    companion object {
        fun fromDomain(domain: UserWordState): UserWordStateEntity {
            return UserWordStateEntity(
                lemma = domain.lemma,
                status = domain.status.name,
                starred = if (domain.starred) 1 else 0,
                savedContextSentence = domain.savedContextSentence,
                encounterCount = domain.encounterCount,
                lastSeenTimestamp = domain.lastSeenTimestamp,
            )
        }
    }
}
