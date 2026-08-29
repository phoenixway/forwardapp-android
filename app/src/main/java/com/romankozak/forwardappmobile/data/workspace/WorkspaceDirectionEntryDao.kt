package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.data.database.WorkspaceDirectionEntryIssueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDirectionEntryDao {
    @Query(
        """
        SELECT * FROM workspace_direction_entries
        ORDER BY workspaceId, capabilityInstanceId, entryOrder
        """,
    )
    suspend fun getAll(): List<WorkspaceDirectionEntryEntity>

    @Query("SELECT * FROM workspace_direction_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkspaceDirectionEntryEntity?

    @Query(
        """
        SELECT * FROM workspace_direction_entries
        WHERE workspaceId = :workspaceId
          AND isDeleted = 0
        ORDER BY entryOrder, id
        """,
    )
    fun observeLiveForWorkspace(workspaceId: String): Flow<List<WorkspaceDirectionEntryEntity>>

    @Query(
        """
        SELECT * FROM workspace_direction_entries
        WHERE workspaceId = :workspaceId
          AND isDeleted = 0
        ORDER BY entryOrder, id
        """,
    )
    suspend fun getLiveForWorkspace(workspaceId: String): List<WorkspaceDirectionEntryEntity>

    @Query("SELECT * FROM workspace_direction_entries WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<WorkspaceDirectionEntryEntity>

    @Query(
        """
        SELECT * FROM workspace_direction_entries
        WHERE provenance = 'LEGACY_DIRECTION_ITEM'
        ORDER BY workspaceId, capabilityInstanceId, entryOrder
        """,
    )
    suspend fun getLegacyShadows(): List<WorkspaceDirectionEntryEntity>

    @Upsert
    suspend fun upsert(items: List<WorkspaceDirectionEntryEntity>)

    @Query(
        """
        SELECT * FROM workspace_direction_entries
        WHERE syncedAt IS NULL
        ORDER BY workspaceId, capabilityInstanceId, entryOrder
        """,
    )
    suspend fun getUnsyncedForSync(): List<WorkspaceDirectionEntryEntity>

    @Query(
        """
        SELECT * FROM workspace_direction_entries
        WHERE updatedAt > :timestamp
        ORDER BY updatedAt, id
        """,
    )
    suspend fun getChangedSinceForSync(timestamp: Long): List<WorkspaceDirectionEntryEntity>

    @Query(
        """
        UPDATE workspace_direction_entries
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    )

    @Query(
        """
        SELECT * FROM workspace_direction_entry_issues
        WHERE resolvedAt IS NULL
        ORDER BY createdAt, sourceDirectionItemId, code
        """,
    )
    suspend fun getOpenIssues(): List<WorkspaceDirectionEntryIssueEntity>

    @Upsert
    suspend fun upsertIssues(items: List<WorkspaceDirectionEntryIssueEntity>)

    @Query(
        """
        UPDATE workspace_direction_entry_issues
        SET resolvedAt = :resolvedAt
        WHERE resolvedAt IS NULL
        """,
    )
    suspend fun resolveOpenIssues(resolvedAt: Long)
}
