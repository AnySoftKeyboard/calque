package com.anysoftkeyboard.calque.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anysoftkeyboard.calque.data.room.entities.UserWordStateEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for the [UserWordStateEntity] table. */
@Dao
interface UserWordStateDao {
    @Query("SELECT * FROM user_word_state WHERE lemma = :lemma LIMIT 1")
    suspend fun getWordState(lemma: String): UserWordStateEntity?

    @Query("SELECT * FROM user_word_state WHERE lemma = :lemma LIMIT 1")
    fun getWordStateFlow(lemma: String): Flow<UserWordStateEntity?>

    @Query("SELECT * FROM user_word_state WHERE lemma IN (:lemmas)")
    suspend fun batchGetWordStates(lemmas: List<String>): List<UserWordStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordState(state: UserWordStateEntity)

    @Query("SELECT * FROM user_word_state WHERE starred = 1 ORDER BY last_seen_timestamp DESC")
    suspend fun getStarredWords(): List<UserWordStateEntity>

    @Query("SELECT * FROM user_word_state WHERE starred = 1 ORDER BY last_seen_timestamp DESC")
    fun getStarredWordsFlow(): Flow<List<UserWordStateEntity>>

    @Query("SELECT * FROM user_word_state WHERE status = :status ORDER BY last_seen_timestamp DESC")
    suspend fun getWordsByStatus(status: String): List<UserWordStateEntity>

    @Query("SELECT * FROM user_word_state WHERE status = :status ORDER BY last_seen_timestamp DESC")
    fun getWordsByStatusFlow(status: String): Flow<List<UserWordStateEntity>>

    @Query("DELETE FROM user_word_state WHERE lemma = :lemma")
    suspend fun deleteWordState(lemma: String): Int

    @Query("SELECT COUNT(*) FROM user_word_state") suspend fun count(): Int
}
