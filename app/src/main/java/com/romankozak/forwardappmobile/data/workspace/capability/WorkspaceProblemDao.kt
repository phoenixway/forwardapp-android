package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemAttachmentRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceProblemWorkspaceRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceProblemDao {
    @Query(
        """
        SELECT *
        FROM workspace_problems
        WHERE workspaceId = :workspaceId
          AND isDeleted = 0
        ORDER BY problemOrder, id
        """,
    )
    fun observeLiveProblems(workspaceId: String): Flow<List<WorkspaceProblemEntity>>

    @Query(
        """
        SELECT *
        FROM workspace_problem_workspace_refs AS ref
        WHERE ref.isDeleted = 0
          AND EXISTS (
              SELECT 1
              FROM workspace_problems AS problem
              WHERE problem.id = ref.problemId
                AND problem.workspaceId = :workspaceId
                AND problem.isDeleted = 0
          )
        ORDER BY ref.problemId, ref.id
        """,
    )
    fun observeLiveWorkspaceRefs(workspaceId: String): Flow<List<WorkspaceProblemWorkspaceRefEntity>>

    @Query(
        """
        SELECT *
        FROM workspace_problem_attachment_refs AS ref
        WHERE ref.isDeleted = 0
          AND EXISTS (
              SELECT 1
              FROM workspace_problems AS problem
              WHERE problem.id = ref.problemId
                AND problem.workspaceId = :workspaceId
                AND problem.isDeleted = 0
          )
        ORDER BY ref.problemId, ref.id
        """,
    )
    fun observeLiveAttachmentRefs(workspaceId: String): Flow<List<WorkspaceProblemAttachmentRefEntity>>

    @Query(
        """
        SELECT *
        FROM workspace_problems
        WHERE workspaceId = :workspaceId
          AND isDeleted = 0
        ORDER BY problemOrder, id
        """,
    )
    suspend fun getLiveProblems(workspaceId: String): List<WorkspaceProblemEntity>

    @Query("SELECT * FROM workspace_problems WHERE id = :id LIMIT 1")
    suspend fun getProblem(id: String): WorkspaceProblemEntity?

    @Query(
        """
        SELECT *
        FROM workspace_problem_workspace_refs
        WHERE problemId = :problemId
        ORDER BY id
        """,
    )
    suspend fun getWorkspaceRefs(problemId: String): List<WorkspaceProblemWorkspaceRefEntity>

    @Query(
        """
        SELECT *
        FROM workspace_problem_attachment_refs
        WHERE problemId = :problemId
        ORDER BY id
        """,
    )
    suspend fun getAttachmentRefs(problemId: String): List<WorkspaceProblemAttachmentRefEntity>

    @Query("SELECT * FROM workspace_problems")
    suspend fun getAllProblems(): List<WorkspaceProblemEntity>

    @Query("SELECT * FROM workspace_problem_workspace_refs")
    suspend fun getAllWorkspaceRefs(): List<WorkspaceProblemWorkspaceRefEntity>

    @Query("SELECT * FROM workspace_problem_attachment_refs")
    suspend fun getAllAttachmentRefs(): List<WorkspaceProblemAttachmentRefEntity>

    @Query("SELECT * FROM workspace_problems WHERE syncedAt IS NULL")
    suspend fun getUnsyncedProblems(): List<WorkspaceProblemEntity>

    @Query("SELECT * FROM workspace_problem_workspace_refs WHERE syncedAt IS NULL")
    suspend fun getUnsyncedWorkspaceRefs(): List<WorkspaceProblemWorkspaceRefEntity>

    @Query("SELECT * FROM workspace_problem_attachment_refs WHERE syncedAt IS NULL")
    suspend fun getUnsyncedAttachmentRefs(): List<WorkspaceProblemAttachmentRefEntity>

    @Query("SELECT * FROM workspace_problems WHERE updatedAt > :timestamp")
    suspend fun getProblemsChangedSince(timestamp: Long): List<WorkspaceProblemEntity>

    @Query("SELECT * FROM workspace_problem_workspace_refs WHERE updatedAt > :timestamp")
    suspend fun getWorkspaceRefsChangedSince(timestamp: Long): List<WorkspaceProblemWorkspaceRefEntity>

    @Query("SELECT * FROM workspace_problem_attachment_refs WHERE updatedAt > :timestamp")
    suspend fun getAttachmentRefsChangedSince(timestamp: Long): List<WorkspaceProblemAttachmentRefEntity>

    @Upsert
    suspend fun upsertProblems(items: List<WorkspaceProblemEntity>)

    @Upsert
    suspend fun upsertWorkspaceRefs(items: List<WorkspaceProblemWorkspaceRefEntity>)

    @Upsert
    suspend fun upsertAttachmentRefs(items: List<WorkspaceProblemAttachmentRefEntity>)

    @Query("DELETE FROM workspace_problem_attachment_refs")
    suspend fun deleteAllAttachmentRefs()

    @Query("DELETE FROM workspace_problem_workspace_refs")
    suspend fun deleteAllWorkspaceRefs()

    @Query("DELETE FROM workspace_problems")
    suspend fun deleteAllProblems()

    @Query(
        """
        UPDATE workspace_problems
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markProblemSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query(
        """
        UPDATE workspace_problem_workspace_refs
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markWorkspaceRefSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query(
        """
        UPDATE workspace_problem_attachment_refs
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markAttachmentRefSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int
}
