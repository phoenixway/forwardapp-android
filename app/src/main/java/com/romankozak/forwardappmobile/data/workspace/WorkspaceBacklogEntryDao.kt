package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceBacklogEntryDao {
    @Query(
        """
        SELECT * FROM workspace_backlog_entries
        WHERE workspaceId = :workspaceId AND isDeleted = 0
        ORDER BY entryOrder, id
        """,
    )
    fun observeLive(workspaceId: String): Flow<List<WorkspaceBacklogEntryEntity>>

    @Query(
        """
        SELECT * FROM workspace_backlog_entries
        WHERE workspaceId = :workspaceId AND isDeleted = 0
        ORDER BY entryOrder, id
        """,
    )
    suspend fun getLive(workspaceId: String): List<WorkspaceBacklogEntryEntity>

    @Query("SELECT * FROM workspace_backlog_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkspaceBacklogEntryEntity?

    @Query("SELECT * FROM workspace_backlog_entries WHERE id IN (:ids)")
    suspend fun getByIds(ids: Collection<String>): List<WorkspaceBacklogEntryEntity>

    @Query(
        """
        SELECT * FROM workspace_backlog_entries
        WHERE targetKind = :targetKind
          AND targetId = :targetId
        ORDER BY workspaceId, entryOrder, id
        """,
    )
    suspend fun getByTarget(
        targetKind: String,
        targetId: String,
    ): List<WorkspaceBacklogEntryEntity>

    @Query(
        """
        SELECT * FROM workspace_backlog_entries
        WHERE targetKind = :targetKind
          AND targetId = :targetId
          AND isDeleted = 0
        ORDER BY workspaceId, entryOrder, id
        """,
    )
    suspend fun getLiveByTarget(
        targetKind: String,
        targetId: String,
    ): List<WorkspaceBacklogEntryEntity>

    @Query(
        """
        SELECT * FROM workspace_backlog_entries
        WHERE capabilityInstanceId = :capabilityInstanceId
          AND targetKind = :targetKind
          AND targetId = :targetId
        ORDER BY isDeleted ASC, updatedAt DESC, id ASC
        LIMIT 1
        """,
    )
    suspend fun getLogicalPlacement(
        capabilityInstanceId: String,
        targetKind: String,
        targetId: String,
    ): WorkspaceBacklogEntryEntity?

    @Query("SELECT * FROM workspace_backlog_entries")
    suspend fun getAll(): List<WorkspaceBacklogEntryEntity>

    @Query("SELECT * FROM workspace_backlog_entries WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<WorkspaceBacklogEntryEntity>

    @Query("SELECT * FROM workspace_backlog_entries WHERE updatedAt > :timestamp")
    suspend fun getChangedSince(timestamp: Long): List<WorkspaceBacklogEntryEntity>

    @Upsert
    suspend fun upsert(entries: List<WorkspaceBacklogEntryEntity>)

    @Query(
        """
        UPDATE workspace_backlog_entries
        SET syncedAt = :syncedAt
        WHERE id = :id AND version = :expectedVersion AND syncedAt IS NULL
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query("DELETE FROM workspace_backlog_entries")
    suspend fun deleteAll()
}
