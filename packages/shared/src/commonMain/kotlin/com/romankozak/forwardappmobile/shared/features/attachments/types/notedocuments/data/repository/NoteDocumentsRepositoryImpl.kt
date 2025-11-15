package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocument
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocumentItem
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteDocumentsRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : NoteDocumentsRepository {

    override fun observeDocuments(projectId: String?): Flow<List<NoteDocument>> {
        val query = if (projectId == null) {
            database.noteDocumentsQueries.getAllNoteDocuments()
        } else {
            database.noteDocumentsQueries.getNoteDocumentsForProject(projectId)
        }

        return query
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeDocumentItems(documentId: String): Flow<List<NoteDocumentItem>> =
        database.noteDocumentItemsQueries.getNoteDocumentItems(documentId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getDocumentById(id: String): NoteDocument? = withContext(dispatcher) {
        database.noteDocumentsQueries.getNoteDocumentById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getDocumentItemById(id: String): NoteDocumentItem? = withContext(dispatcher) {
        database.noteDocumentItemsQueries.getNoteDocumentItemById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertDocument(document: NoteDocument) = withContext(dispatcher) {
        database.noteDocumentsQueries.insertNoteDocument(
            id = document.id,
            projectId = document.projectId,
            name = document.name,
            content = document.content,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt,
            lastCursorPosition = document.lastCursorPosition,
        )
    }

    override suspend fun updateDocumentContent(
        id: String,
        name: String,
        content: String?,
        updatedAt: Long,
        lastCursorPosition: Long,
    ) = withContext(dispatcher) {
        database.noteDocumentsQueries.updateNoteDocument(
            id = id,
            name = name,
            content = content,
            updatedAt = updatedAt,
            lastCursorPosition = lastCursorPosition,
        )
    }

    override suspend fun upsertDocumentItem(item: NoteDocumentItem) = withContext(dispatcher) {
        database.noteDocumentItemsQueries.insertNoteDocumentItem(
            id = item.id,
            documentId = item.documentId,
            parentId = item.parentId,
            content = item.content,
            isCompleted = item.isCompleted,
            itemOrder = item.order,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
    }

    override suspend fun deleteDocumentById(id: String) = withContext(dispatcher) {
        database.transaction {
            database.noteDocumentItemsQueries.deleteNoteDocumentItemsByDocument(id)
            database.noteDocumentsQueries.deleteNoteDocumentById(id)
        }
    }

    override suspend fun deleteDocuments(projectId: String?) = withContext(dispatcher) {
        database.transaction {
            if (projectId == null) {
                database.noteDocumentItemsQueries.deleteAllNoteDocumentItems()
                database.noteDocumentsQueries.deleteAllNoteDocuments()
            } else {
                database.noteDocumentItemsQueries.deleteNoteDocumentItemsByProject(projectId)
                database.noteDocumentsQueries.deleteNoteDocumentsByProject(projectId)
            }
        }
    }

    override suspend fun deleteDocumentItemById(id: String) = withContext(dispatcher) {
        database.noteDocumentItemsQueries.deleteNoteDocumentItemById(id)
    }

    override suspend fun deleteDocumentItemsByDocument(documentId: String) = withContext(dispatcher) {
        database.noteDocumentItemsQueries.deleteNoteDocumentItemsByDocument(documentId)
    }

    override suspend fun deleteDocumentItemsByIds(ids: List<String>) = withContext(dispatcher) {
        if (ids.isEmpty()) return@withContext
        database.noteDocumentItemsQueries.deleteNoteDocumentItemsByIds(ids)
    }
}
