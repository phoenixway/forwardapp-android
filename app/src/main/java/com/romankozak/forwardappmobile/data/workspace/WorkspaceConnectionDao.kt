package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceConnectionDao {
    @Query(
        """
        SELECT * FROM workspace_connections
        WHERE workspaceId = :workspaceId AND isDeleted = 0
        ORDER BY connectionOrder, id
        """,
    )
    suspend fun getLive(workspaceId: String): List<WorkspaceConnectionEntity>

    @Query("SELECT * FROM workspace_connections WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkspaceConnectionEntity?

    @Query("SELECT * FROM workspace_connections")
    suspend fun getAll(): List<WorkspaceConnectionEntity>

    @Query("SELECT * FROM workspace_connections WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<WorkspaceConnectionEntity>

    @Query("SELECT * FROM workspace_connections WHERE updatedAt > :timestamp")
    suspend fun getChangedSince(timestamp: Long): List<WorkspaceConnectionEntity>

    @Query(
        """
        SELECT w.sourceContextId AS context_id,
               c.attachmentId AS attachment_id,
               c.connectionOrder AS attachment_order,
               c.updatedAt AS updatedAt,
               c.syncedAt AS syncedAt,
               c.isDeleted AS isDeleted,
               c.version AS version
        FROM workspace_connections c
        JOIN workspaces w ON w.id = c.workspaceId
        WHERE w.sourceContextId IS NOT NULL
        """,
    )
    suspend fun getAllAsLegacyCrossRefs(): List<ContextAttachmentCrossRef>

    @Query(
        """
        SELECT w.sourceContextId AS context_id,
               c.attachmentId AS attachment_id,
               c.connectionOrder AS attachment_order,
               c.updatedAt AS updatedAt,
               c.syncedAt AS syncedAt,
               c.isDeleted AS isDeleted,
               c.version AS version
        FROM workspace_connections c
        JOIN workspaces w ON w.id = c.workspaceId
        WHERE w.sourceContextId IS NOT NULL
        """,
    )
    fun observeAllAsLegacyCrossRefs(): Flow<List<ContextAttachmentCrossRef>>

    @Upsert
    suspend fun upsert(items: List<WorkspaceConnectionEntity>)

    @Query("DELETE FROM workspace_connections")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE workspace_connections
        SET syncedAt = :syncedAt
        WHERE id = :id AND version = :expectedVersion AND syncedAt IS NULL
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int
}
