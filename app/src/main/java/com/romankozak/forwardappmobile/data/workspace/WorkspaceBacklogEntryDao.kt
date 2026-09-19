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

    /**
     * Finds only live placements that cannot remain in the canonical runtime.
     *
     * Target existence and the historical direct-child projection rule are
     * evaluated in one indexed SQLite query. This avoids the former per-entry
     * target lookup loop while preserving the typed target rules.
     */
    @Query(
        """
        SELECT entry.*
        FROM workspace_backlog_entries AS entry
        LEFT JOIN workspaces AS target_workspace
          ON entry.targetKind = 'WORKSPACE'
         AND target_workspace.id = entry.targetId
        LEFT JOIN managed_subjects AS target_subject
          ON entry.targetKind = 'ORIENTATION'
         AND target_subject.id = entry.targetId
        LEFT JOIN orientations AS target_orientation
          ON entry.targetKind = 'ORIENTATION'
         AND target_orientation.subjectId = entry.targetId
        LEFT JOIN link_items AS target_link
          ON entry.targetKind = 'LINK_ITEM'
         AND target_link.id = entry.targetId
        LEFT JOIN notes AS target_legacy_note
          ON entry.targetKind = 'LEGACY_NOTE'
         AND target_legacy_note.id = entry.targetId
        LEFT JOIN note_documents AS target_document
          ON entry.targetKind = 'NOTE_DOCUMENT'
         AND target_document.id = entry.targetId
        LEFT JOIN checklists AS target_checklist
          ON entry.targetKind = 'CHECKLIST'
         AND target_checklist.id = entry.targetId
        LEFT JOIN music_notes AS target_music_note
          ON entry.targetKind = 'MUSIC_NOTE'
         AND target_music_note.id = entry.targetId
        WHERE entry.isDeleted = 0
          AND CASE entry.targetKind
            WHEN 'ORIENTATION' THEN
              target_subject.id IS NULL
              OR target_subject.isDeleted != 0
              OR target_subject.subjectType != 'ORIENTATION'
              OR target_orientation.subjectId IS NULL
            WHEN 'WORKSPACE' THEN
              target_workspace.id IS NULL
              OR target_workspace.isDeleted != 0
              OR target_workspace.parentWorkspaceId = entry.workspaceId
            WHEN 'LINK_ITEM' THEN
              target_link.id IS NULL OR target_link.is_deleted != 0
            WHEN 'LEGACY_NOTE' THEN
              target_legacy_note.id IS NULL OR target_legacy_note.isDeleted != 0
            WHEN 'NOTE_DOCUMENT' THEN
              target_document.id IS NULL OR target_document.isDeleted != 0
            WHEN 'CHECKLIST' THEN
              target_checklist.id IS NULL OR target_checklist.isDeleted != 0
            WHEN 'MUSIC_NOTE' THEN
              target_music_note.id IS NULL OR target_music_note.isDeleted != 0
            ELSE 1
          END
        """,
    )
    suspend fun getLiveDanglingAndStructuralEntries(): List<WorkspaceBacklogEntryEntity>

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
