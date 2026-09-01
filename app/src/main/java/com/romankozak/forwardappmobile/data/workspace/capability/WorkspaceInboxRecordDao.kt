package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceInboxRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceInboxRecordDao {
    @Query(
        """
        SELECT * FROM workspace_inbox_records
        WHERE workspaceId = :workspaceId AND isDeleted = 0
        ORDER BY recordOrder, id
        """,
    )
    fun observeLive(workspaceId: String): Flow<List<WorkspaceInboxRecordEntity>>

    @Query(
        """
        SELECT * FROM workspace_inbox_records
        WHERE workspaceId = :workspaceId AND isDeleted = 0
        ORDER BY recordOrder, id
        """,
    )
    suspend fun getLive(workspaceId: String): List<WorkspaceInboxRecordEntity>

    @Query("SELECT * FROM workspace_inbox_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkspaceInboxRecordEntity?

    @Query("SELECT * FROM workspace_inbox_records WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<WorkspaceInboxRecordEntity>

    @Query("SELECT * FROM workspace_inbox_records")
    suspend fun getAll(): List<WorkspaceInboxRecordEntity>

    @Query("SELECT * FROM workspace_inbox_records WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<WorkspaceInboxRecordEntity>

    @Query("SELECT * FROM workspace_inbox_records WHERE updatedAt > :timestamp")
    suspend fun getChangedSince(timestamp: Long): List<WorkspaceInboxRecordEntity>

    @Query("SELECT * FROM workspace_inbox_records WHERE text LIKE :query ORDER BY createdAt DESC")
    suspend fun search(query: String): List<WorkspaceInboxRecordEntity>

    @Upsert
    suspend fun upsert(items: List<WorkspaceInboxRecordEntity>)

    @Query("DELETE FROM workspace_inbox_records")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE workspace_inbox_records
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
