package com.romankozak.forwardappmobile.shared.features.notes.domain.repository

import com.romankozak.forwardappmobile.shared.features.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun insertNote(title: String, content: String): Long
    suspend fun updateNote(id: Long, title: String, content: String)
    suspend fun deleteNote(id: Long)
    suspend fun deleteAllNotes()
}
