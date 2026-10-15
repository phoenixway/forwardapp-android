package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.planning.DayPlanningScopeExtensionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayPlanningScopeExtensionDao {
    @Query("SELECT * FROM day_planning_scope_extensions WHERE scopeId = :scopeId LIMIT 1")
    suspend fun get(scopeId: String): DayPlanningScopeExtensionEntity?

    @Query("SELECT * FROM day_planning_scope_extensions WHERE scopeId = :scopeId LIMIT 1")
    fun observe(scopeId: String): Flow<DayPlanningScopeExtensionEntity?>

    @Query("SELECT * FROM day_planning_scope_extensions ORDER BY scopeId ASC")
    suspend fun getAll(): List<DayPlanningScopeExtensionEntity>

    @Query("SELECT * FROM day_planning_scope_extensions ORDER BY scopeId ASC")
    fun observeAll(): Flow<List<DayPlanningScopeExtensionEntity>>

    @Query("SELECT * FROM day_planning_scope_extensions WHERE syncedAt IS NULL ORDER BY scopeId ASC")
    suspend fun getUnsynced(): List<DayPlanningScopeExtensionEntity>

    @Query(
        "SELECT * FROM day_planning_scope_extensions " +
            "WHERE COALESCE(updatedAt, createdAt) > :timestamp ORDER BY scopeId ASC",
    )
    suspend fun getChangedSince(timestamp: Long): List<DayPlanningScopeExtensionEntity>

    @Query(
        "UPDATE day_planning_scope_extensions SET syncedAt = :syncedAt " +
            "WHERE scopeId = :scopeId AND version = :version",
    )
    suspend fun markSyncedIfVersionMatches(
        scopeId: String,
        version: Long,
        syncedAt: Long,
    ): Int

    @Upsert
    suspend fun upsert(entity: DayPlanningScopeExtensionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DayPlanningScopeExtensionEntity>)
}
