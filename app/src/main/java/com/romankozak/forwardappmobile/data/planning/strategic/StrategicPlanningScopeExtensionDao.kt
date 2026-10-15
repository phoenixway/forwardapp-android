package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.planning.StrategicPlanningScopeExtensionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategicPlanningScopeExtensionDao {
    @Query("SELECT * FROM strategic_planning_scope_extensions WHERE scopeId = :scopeId LIMIT 1")
    suspend fun get(scopeId: String): StrategicPlanningScopeExtensionEntity?

    @Query("SELECT * FROM strategic_planning_scope_extensions ORDER BY scopeId ASC")
    suspend fun getAll(): List<StrategicPlanningScopeExtensionEntity>

    @Query("SELECT * FROM strategic_planning_scope_extensions ORDER BY scopeId ASC")
    fun observeAll(): Flow<List<StrategicPlanningScopeExtensionEntity>>

    @Query("SELECT * FROM strategic_planning_scope_extensions WHERE syncedAt IS NULL ORDER BY scopeId ASC")
    suspend fun getUnsynced(): List<StrategicPlanningScopeExtensionEntity>

    @Query(
        "SELECT * FROM strategic_planning_scope_extensions " +
            "WHERE COALESCE(updatedAt, createdAt) > :timestamp ORDER BY scopeId ASC",
    )
    suspend fun getChangedSince(timestamp: Long): List<StrategicPlanningScopeExtensionEntity>

    @Query(
        "UPDATE strategic_planning_scope_extensions SET syncedAt = :syncedAt " +
            "WHERE scopeId = :scopeId AND version = :version",
    )
    suspend fun markSyncedIfVersionMatches(scopeId: String, version: Long, syncedAt: Long): Int

    @Upsert
    suspend fun upsert(entity: StrategicPlanningScopeExtensionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<StrategicPlanningScopeExtensionEntity>)
}
