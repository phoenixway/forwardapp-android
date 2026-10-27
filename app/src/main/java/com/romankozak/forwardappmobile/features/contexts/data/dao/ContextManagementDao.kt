package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextManagementDao {
    @Query(
        """
        SELECT log.*
        FROM context_execution_logs AS log
        INNER JOIN workspaces AS workspace
            ON workspace.id = log.workspaceId
        WHERE log.is_deleted = 0
          AND workspace.provenance = 'CONTEXT_BACKED'
          AND workspace.sourceContextId = :contextId
          AND workspace.isDeleted = 0
        ORDER BY log.timestamp DESC, log.id
        """,
    )
    fun getLogsForContextStream(contextId: String): Flow<List<ContextLog>>

    @Query(
        """
        SELECT log.* FROM context_execution_logs AS log
        INNER JOIN workspaces AS workspace ON workspace.id = log.workspaceId
        WHERE log.workspaceId = :workspaceId
          AND log.is_deleted = 0 AND workspace.isDeleted = 0
        ORDER BY log.timestamp DESC, log.id
        """,
    )
    fun observeWorkspaceLogs(workspaceId: String): Flow<List<ContextLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ContextLog)

    @Query("SELECT * FROM context_execution_logs")
    suspend fun getAllLogs(): List<ContextLog>

    @Query(
        """
        SELECT * FROM context_execution_logs
        ORDER BY updatedAt, id
        """,
    )
    suspend fun getCanonicalExecutionLogs(): List<ContextLog>

    @Query("SELECT * FROM context_execution_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: String): ContextLog?

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE workspaceId = :workspaceId
          AND is_deleted = 0
        ORDER BY timestamp DESC, id
        """,
    )
    suspend fun getLiveCanonicalExecutionLogsForWorkspace(
        workspaceId: String,
    ): List<ContextLog>

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE synced_at IS NULL
        ORDER BY updatedAt, id
        """,
    )
    suspend fun getUnsyncedCanonicalExecutionLogs(): List<ContextLog>

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE updatedAt IS NULL OR updatedAt > :timestamp
        ORDER BY updatedAt, id
        """,
    )
    suspend fun getCanonicalExecutionLogsChangedSince(timestamp: Long): List<ContextLog>

    @Query(
        """
        UPDATE context_execution_logs
        SET synced_at = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
          AND synced_at IS NULL
        """,
    )
    suspend fun markCanonicalExecutionLogSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ContextLog>)

    @Query("DELETE FROM context_execution_logs")
    suspend fun deleteAllLogs()

    @Query("SELECT * FROM context_execution_logs")
    suspend fun getAllLogsRaw(): List<ContextLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ContextLog>)
}
