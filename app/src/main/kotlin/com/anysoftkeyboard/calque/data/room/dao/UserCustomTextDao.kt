package com.anysoftkeyboard.calque.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anysoftkeyboard.calque.data.room.entities.UserCustomTextEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for the [UserCustomTextEntity] table. */
@Dao
interface UserCustomTextDao {
    @Query("SELECT * FROM user_custom_texts ORDER BY created_timestamp DESC")
    suspend fun getAll(): List<UserCustomTextEntity>

    @Query("SELECT * FROM user_custom_texts ORDER BY created_timestamp DESC")
    fun getAllFlow(): Flow<List<UserCustomTextEntity>>

    @Query("SELECT * FROM user_custom_texts WHERE text_id = :textId LIMIT 1")
    suspend fun getById(textId: Long): UserCustomTextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(text: UserCustomTextEntity): Long

    @Query("DELETE FROM user_custom_texts WHERE text_id = :textId")
    suspend fun delete(textId: Long): Int

    @Query("SELECT COUNT(*) FROM user_custom_texts") suspend fun count(): Int
}
