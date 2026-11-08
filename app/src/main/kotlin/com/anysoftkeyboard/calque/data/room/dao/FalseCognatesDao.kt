package com.anysoftkeyboard.calque.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anysoftkeyboard.calque.data.room.entities.FalseCognateEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for the [FalseCognateEntity] table. */
@Dao
interface FalseCognatesDao {
    @Query("SELECT * FROM false_cognates WHERE surface_form = :surfaceForm LIMIT 1")
    suspend fun getBySurfaceForm(surfaceForm: String): FalseCognateEntity?

    @Query("SELECT * FROM false_cognates WHERE surface_form IN (:surfaceForms)")
    suspend fun getBySurfaceForms(surfaceForms: List<String>): List<FalseCognateEntity>

    @Query("SELECT * FROM false_cognates WHERE lemma = :lemma")
    suspend fun getByLemma(lemma: String): List<FalseCognateEntity>

    @Query("SELECT * FROM false_cognates ORDER BY surface_form ASC")
    suspend fun getAll(): List<FalseCognateEntity>

    @Query("SELECT * FROM false_cognates ORDER BY surface_form ASC")
    fun getAllFlow(): Flow<List<FalseCognateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FalseCognateEntity>)

    @Query("SELECT COUNT(*) FROM false_cognates") suspend fun count(): Int
}
