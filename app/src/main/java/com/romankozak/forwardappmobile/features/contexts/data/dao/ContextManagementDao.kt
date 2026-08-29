package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextManagementDao {
    @Query("SELECT * FROM context_execution_logs WHERE contextId = :contextId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getLogsForContextStream(contextId: String): Flow<List<ContextLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ContextLog)

    @Update
    suspend fun updateLog(log: ContextLog)

    @Delete
    suspend fun deleteLog(log: ContextLog)

    @Query("SELECT * FROM context_execution_logs")
    suspend fun getAllLogs(): List<ContextLog>

    // Legacy Context sync/backup must never serialize canonical-only rows.
    @Query("SELECT * FROM context_execution_logs WHERE contextId IS NOT NULL")
    suspend fun getLegacyContextLogs(): List<ContextLog>

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE contextId IS NULL AND workspaceId IS NOT NULL
        ORDER BY updatedAt, id
        """,
    )
    suspend fun getCanonicalExecutionLogs(): List<ContextLog>

    @Query("SELECT * FROM context_execution_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: String): ContextLog?

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE contextId IS NULL
          AND workspaceId = :workspaceId
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
        WHERE contextId IS NULL
          AND workspaceId IS NOT NULL
          AND synced_at IS NULL
        ORDER BY updatedAt, id
        """,
    )
    suspend fun getUnsyncedCanonicalExecutionLogs(): List<ContextLog>

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE contextId IS NULL
          AND workspaceId IS NOT NULL
          AND (updatedAt IS NULL OR updatedAt > :timestamp)
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
          AND contextId IS NULL
          AND workspaceId IS NOT NULL
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

    @Query(
        """
        SELECT * FROM context_execution_logs
        WHERE contextId = :contextId AND is_deleted = 0
        ORDER BY timestamp DESC
        LIMIT -1 OFFSET :keepCount
        """,
    )
    suspend fun getLogsForContextBeyondKeepCount(
        contextId: String,
        keepCount: Int,
    ): List<ContextLog>

    @Query(
        "SELECT DISTINCT contextId FROM context_execution_logs " +
            "WHERE contextId IS NOT NULL AND workspaceId IS NULL",
    )
    suspend fun getContextIdsWithoutWorkspaceOwner(): List<String>

    @Query(
        """
        UPDATE context_execution_logs
        SET workspaceId = :workspaceId
        WHERE workspaceId IS NULL AND contextId = :contextId
        """,
    )
    suspend fun assignWorkspaceOwnerForContext(
        contextId: String,
        workspaceId: String,
    ): Int
}
