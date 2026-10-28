package com.romankozak.forwardappmobile.data.planning

import androidx.room.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningCommitmentRelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningCommitmentRelationDao {
    @Query("SELECT * FROM planning_commitment_relations ORDER BY relationOrder,id")
    suspend fun getAll(): List<PlanningCommitmentRelationEntity>
    @Query("SELECT * FROM planning_commitment_relations ORDER BY relationOrder,id")
    fun observeAll(): Flow<List<PlanningCommitmentRelationEntity>>
    @Query("SELECT * FROM planning_commitment_relations WHERE targetCommitmentId=:id AND isDeleted=0 ORDER BY relationOrder,id")
    fun observeIncoming(id: String): Flow<List<PlanningCommitmentRelationEntity>>
    @Query("SELECT * FROM planning_commitment_relations WHERE sourceCommitmentId=:id AND isDeleted=0 ORDER BY relationOrder,id")
    fun observeOutgoing(id: String): Flow<List<PlanningCommitmentRelationEntity>>
    @Query("SELECT * FROM planning_commitment_relations WHERE id=:id")
    suspend fun get(id: String): PlanningCommitmentRelationEntity?
    @Query("SELECT * FROM planning_commitment_relations WHERE syncedAt IS NULL ORDER BY id")
    suspend fun getUnsynced(): List<PlanningCommitmentRelationEntity>
    @Query("SELECT * FROM planning_commitment_relations WHERE COALESCE(updatedAt,createdAt)>:timestamp ORDER BY id")
    suspend fun changedSince(timestamp: Long): List<PlanningCommitmentRelationEntity>
    @Query("UPDATE planning_commitment_relations SET syncedAt=:at WHERE id=:id AND version=:version")
    suspend fun acknowledge(id: String, version: Long, at: Long)
    @Upsert suspend fun upsert(rows: List<PlanningCommitmentRelationEntity>)
    @Query("DELETE FROM planning_commitment_relations") suspend fun clear()
}
