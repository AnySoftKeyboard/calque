package com.anysoftkeyboard.calque.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anysoftkeyboard.calque.data.room.entities.DictionaryEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for the [DictionaryEntity] table. */
@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary WHERE surface_form = :surfaceForm LIMIT 1")
    suspend fun getBySurfaceForm(surfaceForm: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary WHERE surface_form IN (:surfaceForms)")
    suspend fun getBySurfaceForms(surfaceForms: List<String>): List<DictionaryEntity>

    @Query("SELECT surface_form FROM dictionary WHERE lemma = :lemma ORDER BY surface_form ASC")
    suspend fun getSurfacesForLemma(lemma: String): List<String>

    @Query("SELECT * FROM dictionary WHERE lemma = :lemma ORDER BY surface_form ASC")
    suspend fun getEntriesForLemma(lemma: String): List<DictionaryEntity>

    @Query(
        "SELECT * FROM dictionary WHERE surface_form LIKE :prefix || '%' ESCAPE '\\' ORDER BY frequency_rank IS NULL, frequency_rank ASC, surface_form ASC LIMIT :limit"
    )
    suspend fun searchByPrefix(prefix: String, limit: Int): List<DictionaryEntity>

    @Query("SELECT * FROM dictionary WHERE surface_form = :surfaceForm LIMIT 1")
    fun getBySurfaceFormFlow(surfaceForm: String): Flow<DictionaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DictionaryEntity>)

    @Query("SELECT COUNT(*) FROM dictionary") suspend fun count(): Int
}
