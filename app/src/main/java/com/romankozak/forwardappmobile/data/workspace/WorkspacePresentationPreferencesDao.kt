package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspacePresentationPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspacePresentationPreferencesDao {
    @Query(
        """
        SELECT preference.*
        FROM workspace_presentation_preferences AS preference
        JOIN workspaces AS workspace ON workspace.id = preference.workspaceId
        WHERE preference.workspaceId = :workspaceId
          AND workspace.isDeleted = 0
        LIMIT 1
        """,
    )
    suspend fun getForLiveWorkspace(
        workspaceId: String,
    ): WorkspacePresentationPreferencesEntity?

    @Query(
        """
        SELECT preference.*
        FROM workspace_presentation_preferences AS preference
        JOIN workspaces AS workspace ON workspace.id = preference.workspaceId
        WHERE preference.workspaceId = :workspaceId
          AND workspace.isDeleted = 0
        LIMIT 1
        """,
    )
    fun observeForLiveWorkspace(
        workspaceId: String,
    ): Flow<WorkspacePresentationPreferencesEntity?>

    @Query("SELECT * FROM workspace_presentation_preferences WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getRaw(workspaceId: String): WorkspacePresentationPreferencesEntity?

    @Query("SELECT * FROM workspace_presentation_preferences ORDER BY workspaceId")
    suspend fun getAll(): List<WorkspacePresentationPreferencesEntity>

    @Upsert
    suspend fun upsert(item: WorkspacePresentationPreferencesEntity)

    @Query("SELECT * FROM workspace_presentation_preferences WHERE syncedAt IS NULL ORDER BY workspaceId")
    suspend fun getUnsynced(): List<WorkspacePresentationPreferencesEntity>

    @Query(
        "SELECT * FROM workspace_presentation_preferences " +
            "WHERE updatedAt > :timestamp ORDER BY workspaceId",
    )
    suspend fun getChangedSince(timestamp: Long): List<WorkspacePresentationPreferencesEntity>

    @Query(
        """
        UPDATE workspace_presentation_preferences
        SET syncedAt = :syncedAt
        WHERE workspaceId = :workspaceId
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        workspaceId: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int
}
