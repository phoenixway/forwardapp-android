package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.repository.LegacyNotesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LegacyNotesRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : LegacyNotesRepository {

    override fun observeNotes(projectId: String?): Flow<List<LegacyNote>> {
        val query = if (projectId == null) {
            database.legacyNotesQueries.getAllLegacyNotes()
        } else {
            database.legacyNotesQueries.getLegacyNotesByProject(projectId)
        }

        return query
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getNoteById(id: String): LegacyNote? = withContext(dispatcher) {
        database.legacyNotesQueries.getLegacyNoteById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insert(note: LegacyNote) = withContext(dispatcher) {
        database.legacyNotesQueries.insertLegacyNote(
            id = note.id,
            projectId = note.projectId,
            title = note.title,
            content = note.content,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
    }

    override suspend fun updateContent(
        id: String,
        title: String,
        content: String?,
        updatedAt: Long,
    ) = withContext(dispatcher) {
        database.legacyNotesQueries.updateLegacyNote(
            id = id,
            title = title,
            content = content,
            updatedAt = updatedAt,
        )
    }

    override suspend fun deleteById(id: String) = withContext(dispatcher) {
        database.legacyNotesQueries.deleteLegacyNoteById(id)
    }

    override suspend fun deleteAll(projectId: String?) = withContext(dispatcher) {
        if (projectId == null) {
            database.legacyNotesQueries.deleteAllLegacyNotes()
        } else {
            database.legacyNotesQueries.deleteLegacyNotesByProject(projectId)
        }
    }
}
