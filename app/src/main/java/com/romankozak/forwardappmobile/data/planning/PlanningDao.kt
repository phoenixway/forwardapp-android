package com.romankozak.forwardappmobile.data.planning

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningCommitmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningDao {
    @Query("SELECT * FROM planning_scopes WHERE id = :id LIMIT 1")
    suspend fun getScope(id: String): PlanningScopeEntity?

    @Query("SELECT * FROM planning_scopes ORDER BY id ASC")
    suspend fun getAllScopes(): List<PlanningScopeEntity>

    @Query("SELECT * FROM planning_scopes ORDER BY id ASC")
    fun observeAllScopes(): Flow<List<PlanningScopeEntity>>

    @Query("SELECT * FROM planning_scopes WHERE syncedAt IS NULL ORDER BY id ASC")
    suspend fun getUnsyncedScopes(): List<PlanningScopeEntity>

    @Query("SELECT * FROM planning_scopes WHERE COALESCE(updatedAt, createdAt) > :timestamp ORDER BY id ASC")
    suspend fun getScopesChangedSince(timestamp: Long): List<PlanningScopeEntity>

    @Query("SELECT * FROM planning_scopes WHERE id = :id LIMIT 1")
    fun observeScope(id: String): Flow<PlanningScopeEntity?>

    @Query("SELECT * FROM planning_commitments WHERE id = :id LIMIT 1")
    suspend fun getCommitment(id: String): PlanningCommitmentEntity?

    @Query("SELECT * FROM planning_commitments ORDER BY id ASC")
    suspend fun getAllCommitments(): List<PlanningCommitmentEntity>

    @Query("SELECT * FROM planning_commitments WHERE provenanceSourceType=:type AND provenanceSourceId=:sourceId AND isDeleted=0")
    suspend fun getLiveForProvenance(type: String, sourceId: String): List<PlanningCommitmentEntity>

    @Query("SELECT * FROM planning_commitments ORDER BY id ASC")
    fun observeAllCommitments(): Flow<List<PlanningCommitmentEntity>>

    @Query("SELECT * FROM planning_commitments WHERE syncedAt IS NULL ORDER BY id ASC")
    suspend fun getUnsyncedCommitments(): List<PlanningCommitmentEntity>

    @Query("SELECT * FROM planning_commitments WHERE COALESCE(updatedAt, createdAt) > :timestamp ORDER BY id ASC")
    suspend fun getCommitmentsChangedSince(timestamp: Long): List<PlanningCommitmentEntity>

    @Query(
        """
        SELECT * FROM planning_commitments
        WHERE scopeId = :scopeId
        ORDER BY role ASC, commitmentOrder ASC, id ASC
        """,
    )
    suspend fun getCommitmentsForScope(scopeId: String): List<PlanningCommitmentEntity>

    @Query(
        """
        SELECT * FROM planning_commitments
        WHERE scopeId = :scopeId AND isDeleted = 0
        ORDER BY role ASC, commitmentOrder ASC, id ASC
        """,
    )
    fun observeLiveCommitmentsForScope(scopeId: String): Flow<List<PlanningCommitmentEntity>>

    @Query(
        """
        SELECT * FROM planning_commitments
        WHERE scopeId = :scopeId AND role = :role AND isDeleted = 0
        ORDER BY commitmentOrder ASC, id ASC
        """,
    )
    suspend fun getLiveCommitmentsForRole(
        scopeId: String,
        role: String,
    ): List<PlanningCommitmentEntity>

    @Query(
        """
        SELECT * FROM planning_commitments
        WHERE scopeId = :scopeId
          AND orientationId = :orientationId
          AND role = :role
        ORDER BY isDeleted ASC, id ASC
        """,
    )
    suspend fun getParticipationRecords(
        scopeId: String,
        orientationId: String,
        role: String,
    ): List<PlanningCommitmentEntity>

    @Query(
        """
        SELECT * FROM planning_commitments
        WHERE scopeId = :scopeId AND isDeleted = 0
        ORDER BY role ASC, commitmentOrder ASC, id ASC
        """,
    )
    suspend fun getLiveCommitmentsForScope(scopeId: String): List<PlanningCommitmentEntity>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM managed_subjects AS subject
            INNER JOIN orientations AS orientation
                ON orientation.subjectId = subject.id
            WHERE subject.id = :orientationId
              AND subject.subjectType = 'ORIENTATION'
              AND subject.isDeleted = 0
        )
        """,
    )
    suspend fun isOrientationLive(orientationId: String): Boolean

    @Query(
        """
        UPDATE planning_scopes
        SET syncedAt = :syncedAt
        WHERE id = :id AND version = :version
        """,
    )
    suspend fun markScopeSyncedIfVersionMatches(
        id: String,
        version: Long,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertScope(scope: PlanningScopeEntity)

    @Upsert
    suspend fun upsertScopes(scopes: List<PlanningScopeEntity>)

    @Query(
        """
        UPDATE planning_commitments
        SET syncedAt = :syncedAt
        WHERE id = :id AND version = :version
        """,
    )
    suspend fun markCommitmentSyncedIfVersionMatches(
        id: String,
        version: Long,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertCommitment(commitment: PlanningCommitmentEntity)

    @Upsert
    suspend fun upsertCommitments(commitments: List<PlanningCommitmentEntity>)
}
