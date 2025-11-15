package com.romankozak.forwardappmobile.shared.features.notes.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.core.platform.Platform
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.notes.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.notes.domain.model.Note
import com.romankozak.forwardappmobile.shared.features.notes.domain.repository.NotesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class NotesRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : NotesRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        db.notesQueries.getAllLegacyNotes()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return if (Platform.isAndroid) {
            db.notesQueries.searchNotesFts(query)
                .asFlow()
                .mapToList(dispatcher)
                .map { rows -> rows.map { it.toDomain() } }
        } else {
            db.notesQueries.searchNotesFallback(query)
                .asFlow()
                .mapToList(dispatcher)
                .map { rows -> rows.map { it.toDomain() } }
        }
    }

    override fun getNoteById(id: Long): Flow<Note?> =
        db.notesQueries.getLegacyNoteById(id.toString())
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }

    override suspend fun insertNote(title: String, content: String): Long =
        withContext(dispatcher) {
            db.notesQueries.insertLegacyNote(
                id = com.benasher44.uuid.uuid4().toString(),
                projectId = "", // TODO: get project id
                title = title,
                content = content,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            // TODO: this is not correct, insertLegacyNote does not return id
            return@withContext 0L
        }

    override suspend fun updateNote(id: Long, title: String, content: String) {
        withContext(dispatcher) {
            db.notesQueries.updateLegacyNote(
                id = id.toString(),
                title = title,
                content = content,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override suspend fun deleteNote(id: Long) {
        withContext(dispatcher) {
            db.notesQueries.deleteLegacyNoteById(id.toString())
        }
    }

    override suspend fun deleteAllNotes() {
        withContext(dispatcher) {
            db.notesQueries.deleteAllLegacyNotes()
        }
    }
}
