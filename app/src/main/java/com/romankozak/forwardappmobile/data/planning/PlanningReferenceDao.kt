package com.romankozak.forwardappmobile.data.planning

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningReferenceDao {
    @Query("SELECT * FROM planning_references ORDER BY id")
    fun observeAll(): Flow<List<PlanningReferenceEntity>>

    @Query("SELECT * FROM planning_references ORDER BY id")
    suspend fun getAll(): List<PlanningReferenceEntity>

    @Query("SELECT * FROM planning_references WHERE ownerKind=:kind AND ownerId=:id ORDER BY role, targetKind, targetId")
    suspend fun getForOwner(kind: String, id: String): List<PlanningReferenceEntity>

    @Query("SELECT * FROM planning_references WHERE id=:id")
    suspend fun get(id: String): PlanningReferenceEntity?

    @Upsert
    suspend fun upsert(row: PlanningReferenceEntity)

    @Query("DELETE FROM planning_references")
    suspend fun clear()

    @Query("SELECT * FROM planning_references WHERE syncedAt IS NULL ORDER BY id")
    suspend fun getUnsynced(): List<PlanningReferenceEntity>

    @Query("SELECT * FROM planning_references WHERE updatedAt > :timestamp ORDER BY id")
    suspend fun getChangedSince(timestamp: Long): List<PlanningReferenceEntity>

    @Query("UPDATE planning_references SET syncedAt=:at WHERE id=:id AND version=:version")
    suspend fun acknowledge(id: String, version: Long, at: Long)
}
