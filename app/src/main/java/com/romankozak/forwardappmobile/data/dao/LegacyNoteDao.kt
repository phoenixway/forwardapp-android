package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LegacyNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: LegacyNoteEntity)

    @Update
    suspend fun update(note: LegacyNoteEntity)

    @Query("UPDATE notes SET syncedAt = :syncedAt WHERE id = :noteId AND version = :expectedVersion")
    suspend fun markSyncedIfVersionMatches(
        noteId: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): LegacyNoteEntity?

    @Query("SELECT * FROM notes WHERE contextId = :contextId ORDER BY updatedAt DESC")
    fun getNotesForContext(contextId: String): Flow<List<LegacyNoteEntity>>

    /**
     * Canonical Workspace BACKLOG projection by placement target identity.
     * A Note's historical contextId is not a Workspace membership predicate.
     * Room observes both tables so content and placement changes invalidate this flow.
     */
    @Query(
        """
        SELECT DISTINCT note.*
        FROM notes AS note
        INNER JOIN workspace_backlog_entries AS placement
          ON placement.targetKind = 'LEGACY_NOTE'
         AND placement.targetId = note.id
        WHERE placement.workspaceId = :workspaceId
          AND placement.isDeleted = 0
          AND note.isDeleted = 0
        ORDER BY note.updatedAt DESC, note.id
        """,
    )
    fun observePlacedNotesForWorkspace(workspaceId: String): Flow<List<LegacyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<LegacyNoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<LegacyNoteEntity>

    @Query("SELECT * FROM notes")
    fun getAllAsFlow(): Flow<List<LegacyNoteEntity>>

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("SELECT * FROM notes")
    suspend fun getAllRaw(): List<LegacyNoteEntity>
}
