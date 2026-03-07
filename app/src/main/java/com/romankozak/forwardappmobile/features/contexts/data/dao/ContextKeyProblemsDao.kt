package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.ContextKeyProblemsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextKeyProblemsDao {
    @Query("SELECT * FROM context_key_problems WHERE context_id = :contextId LIMIT 1")
    fun observeForContext(contextId: String): Flow<ContextKeyProblemsEntity?>

    @Query("SELECT * FROM context_key_problems WHERE context_id = :contextId LIMIT 1")
    suspend fun getForContext(contextId: String): ContextKeyProblemsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContextKeyProblemsEntity)

    @Query("SELECT * FROM context_key_problems")
    suspend fun getAllRaw(): List<ContextKeyProblemsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ContextKeyProblemsEntity>)

    @Query("DELETE FROM context_key_problems")
    suspend fun deleteAll()
}
