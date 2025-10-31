package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LegacyNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: LegacyNoteEntity)

    @Update
    suspend fun update(note: LegacyNoteEntity)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): LegacyNoteEntity?

    @Query("SELECT * FROM notes WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getNotesForProject(projectId: String): Flow<List<LegacyNoteEntity>>

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
}
