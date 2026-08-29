package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.database.WorkspaceBootstrapIssueEntity
import com.romankozak.forwardappmobile.data.database.WorkspaceBootstrapStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY parentWorkspaceId, workspaceOrder")
    fun observeAll(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces")
    suspend fun getAll(): List<WorkspaceEntity>

    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkspaceEntity?

    @Query(
        """
        SELECT * FROM workspaces
        WHERE id = :contextId
          AND sourceContextId = :contextId
          AND provenance = 'CONTEXT_BACKED'
        LIMIT 1
        """,
    )
    suspend fun getContextBackedForContextId(contextId: String): WorkspaceEntity?

    @Upsert
    suspend fun upsert(items: List<WorkspaceEntity>)

    @Query("UPDATE workspaces SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("SELECT * FROM workspace_bootstrap_state WHERE id = 1 LIMIT 1")
    suspend fun getBootstrapState(): WorkspaceBootstrapStateEntity?

    @Upsert
    suspend fun upsertBootstrapState(state: WorkspaceBootstrapStateEntity)

    @Query("SELECT * FROM workspace_bootstrap_issues WHERE resolvedAt IS NULL")
    suspend fun getOpenBootstrapIssues(): List<WorkspaceBootstrapIssueEntity>

    @Query("SELECT * FROM workspace_bootstrap_issues WHERE resolvedAt IS NULL ORDER BY createdAt, contextId")
    fun observeOpenBootstrapIssues(): Flow<List<WorkspaceBootstrapIssueEntity>>

    @Upsert
    suspend fun upsertBootstrapIssues(items: List<WorkspaceBootstrapIssueEntity>)

    @Query("UPDATE workspace_bootstrap_issues SET resolvedAt = :resolvedAt WHERE resolvedAt IS NULL")
    suspend fun resolveOpenBootstrapIssues(resolvedAt: Long)
}
