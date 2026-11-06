package com.anysoftkeyboard.calque.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anysoftkeyboard.calque.data.room.entities.CognateRuleEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for the [CognateRuleEntity] table. */
@Dao
interface CognateRulesDao {
    @Query("SELECT * FROM cognate_rules ORDER BY rule_id ASC")
    suspend fun getAll(): List<CognateRuleEntity>

    @Query("SELECT * FROM cognate_rules ORDER BY rule_id ASC")
    fun getAllFlow(): Flow<List<CognateRuleEntity>>

    @Query("SELECT * FROM cognate_rules WHERE rule_id = :ruleId LIMIT 1")
    suspend fun getById(ruleId: String): CognateRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CognateRuleEntity>)

    @Query("SELECT COUNT(*) FROM cognate_rules") suspend fun count(): Int
}
