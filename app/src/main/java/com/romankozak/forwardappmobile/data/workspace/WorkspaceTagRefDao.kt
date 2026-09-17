package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceTagRefDao {
    @Query(
        """
        SELECT tag.*
        FROM workspace_tag_refs AS tag
        JOIN workspaces AS workspace ON workspace.id = tag.workspaceId
        WHERE tag.workspaceId = :workspaceId
          AND tag.isDeleted = 0
          AND workspace.isDeleted = 0
        ORDER BY tag.normalizedTag
        """,
    )
    suspend fun getLiveForWorkspace(workspaceId: String): List<WorkspaceTagRefEntity>

    @Query(
        """
        SELECT tag.*
        FROM workspace_tag_refs AS tag
        JOIN workspaces AS workspace ON workspace.id = tag.workspaceId
        WHERE tag.workspaceId = :workspaceId
          AND tag.isDeleted = 0
          AND workspace.isDeleted = 0
        ORDER BY tag.normalizedTag
        """,
    )
    fun observeLiveForWorkspace(workspaceId: String): Flow<List<WorkspaceTagRefEntity>>

    @Query(
        """
        SELECT *
        FROM workspace_tag_refs
        WHERE workspaceId = :workspaceId
        ORDER BY normalizedTag
        """,
    )
    suspend fun getAllForWorkspace(workspaceId: String): List<WorkspaceTagRefEntity>

    @Query(
        """
        SELECT tag.*
        FROM workspace_tag_refs AS tag
        JOIN workspaces AS workspace ON workspace.id = tag.workspaceId
        WHERE tag.normalizedTag IN (:normalizedTags)
          AND tag.isDeleted = 0
          AND workspace.isDeleted = 0
        ORDER BY tag.workspaceId, tag.normalizedTag
        """,
    )
    suspend fun findLiveByTags(normalizedTags: List<String>): List<WorkspaceTagRefEntity>

    @Query("SELECT * FROM workspace_tag_refs")
    suspend fun getAll(): List<WorkspaceTagRefEntity>

    @Query("SELECT * FROM workspace_tag_refs")
    fun observeAll(): Flow<List<WorkspaceTagRefEntity>>

    @Query("SELECT * FROM workspace_tag_refs WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<WorkspaceTagRefEntity>

    @Query("SELECT * FROM workspace_tag_refs WHERE updatedAt > :timestamp")
    suspend fun getChangedSince(timestamp: Long): List<WorkspaceTagRefEntity>

    @Upsert
    suspend fun upsert(items: List<WorkspaceTagRefEntity>)

    @Query(
        """
        UPDATE workspace_tag_refs
        SET syncedAt = :syncedAt
        WHERE workspaceId = :workspaceId
          AND normalizedTag = :normalizedTag
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        workspaceId: String,
        normalizedTag: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int
}
