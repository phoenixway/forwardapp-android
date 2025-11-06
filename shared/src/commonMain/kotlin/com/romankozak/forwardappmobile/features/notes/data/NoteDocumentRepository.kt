package com.romankozak.forwardappmobile.features.notes.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.shared.database.NoteDocumentQueriesQueries
import com.romankozak.forwardappmobile.shared.database.Note_document_items
import com.romankozak.forwardappmobile.shared.database.Note_documents
import com.romankozak.forwardappmobile.shared.features.notes.data.model.LegacyNote
import com.romankozak.forwardappmobile.shared.features.notes.data.model.NoteDocument
import com.romankozak.forwardappmobile.shared.features.notes.data.model.NoteDocumentItem
import com.romankozak.forwardappmobile.shared.features.recentitems.data.RecentItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val NOTE_DOCUMENT_ATTACHMENT_TYPE = "NOTE_DOCUMENT"

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

class NoteDocumentRepository(
    private val noteQueries: NoteDocumentQueriesQueries,
    private val attachmentRepository: AttachmentRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val queryContext: CoroutineContext = EmptyCoroutineContext,
) {

    fun getDocumentsForProject(projectId: String): Flow<List<NoteDocument>> =
        noteQueries
            .getNoteDocumentsForProject(projectId, NOTE_DOCUMENT_ATTACHMENT_TYPE)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllDocumentsAsFlow(): Flow<List<NoteDocument>> =
        noteQueries
            .getAllNoteDocuments()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun getDocumentById(documentId: String): NoteDocument? =
        withContext(queryContext) {
            noteQueries
                .getNoteDocumentById(documentId)
                .executeAsOneOrNull()
                ?.toModel()
        }

    suspend fun createDocument(
        name: String,
        projectId: String,
        content: String? = null,
    ): String {
        val timestamp = nowMillis()
        val document =
            NoteDocument(
                id = uuid4().toString(),
                projectId = projectId,
                name = name.ifBlank { "New Document" },
                content = content,
                createdAt = timestamp,
                updatedAt = timestamp,
                lastCursorPosition = 0,
            )

        withContext(queryContext) {
            noteQueries.insertNoteDocument(
                id = document.id,
                projectId = document.projectId,
                name = document.name,
                createdAt = document.createdAt,
                updatedAt = document.updatedAt,
                content = document.content,
                lastCursorPosition = document.lastCursorPosition.toLong(),
            )
        }

        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = NOTE_DOCUMENT_ATTACHMENT_TYPE,
            entityId = document.id,
            projectId = projectId,
            ownerProjectId = projectId,
            createdAt = document.createdAt,
        )
        return document.id
    }

    suspend fun deleteDocument(documentId: String) {
        withContext(queryContext) { noteQueries.deleteNoteDocumentById(documentId) }
        attachmentRepository.findAttachmentByEntity(NOTE_DOCUMENT_ATTACHMENT_TYPE, documentId)?.let { attachment ->
            attachmentRepository.deleteAttachment(attachment.id)
        }
    }

    fun getDocumentItems(documentId: String): Flow<List<NoteDocumentItem>> =
        noteQueries
            .getNoteDocumentItems(documentId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun saveDocumentItem(item: NoteDocumentItem) {
        val timestamp = nowMillis()
        withContext(queryContext) {
            val existing = noteQueries.getNoteDocumentItemById(item.id).executeAsOneOrNull()
            if (existing == null) {
                noteQueries.insertNoteDocumentItem(
                    id = item.id,
                    listId = item.listId,
                    parentId = item.parentId,
                    content = item.content,
                    isCompleted = if (item.isCompleted) 1L else 0L,
                    itemOrder = item.itemOrder,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                )
            } else {
                noteQueries.updateNoteDocumentItem(
                    id = item.id,
                    listId = item.listId,
                    parentId = item.parentId,
                    content = item.content,
                    isCompleted = if (item.isCompleted) 1L else 0L,
                    itemOrder = item.itemOrder,
                    createdAt = existing.createdAt,
                    updatedAt = timestamp,
                )
            }
        }
    }

    suspend fun deleteDocumentItem(itemId: String) {
        withContext(queryContext) { noteQueries.deleteNoteDocumentItemById(itemId) }
    }

    suspend fun updateDocumentItems(items: List<NoteDocumentItem>) {
        if (items.isEmpty()) return
        withContext(queryContext) {
            noteQueries.transaction {
                items.forEach { item ->
                    noteQueries.updateNoteDocumentItem(
                        id = item.id,
                        listId = item.listId,
                        parentId = item.parentId,
                        content = item.content,
                        isCompleted = if (item.isCompleted) 1L else 0L,
                        itemOrder = item.itemOrder,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt,
                    )
                }
            }
        }
    }

    suspend fun importFromLegacy(note: LegacyNote) {
        val document = note.toDocument()
        withContext(queryContext) {
            noteQueries.insertNoteDocument(
                id = document.id,
                projectId = document.projectId,
                name = document.name,
                createdAt = document.createdAt,
                updatedAt = document.updatedAt,
                content = document.content,
                lastCursorPosition = document.lastCursorPosition.toLong(),
            )
        }
        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = NOTE_DOCUMENT_ATTACHMENT_TYPE,
            entityId = document.id,
            projectId = document.projectId,
            ownerProjectId = document.projectId,
            createdAt = document.createdAt,
        )
        recentItemsRepository.logNoteDocumentAccess(document.id, document.name)
    }

    suspend fun updateDocument(document: NoteDocument) {
        withContext(queryContext) {
            noteQueries.updateNoteDocument(
                id = document.id,
                name = document.name,
                content = document.content,
                updatedAt = document.updatedAt,
                lastCursorPosition = document.lastCursorPosition.toLong(),
            )
        }
        recentItemsRepository.updateRecentItemDisplayName(document.id, document.name)
    }

    suspend fun deleteAllDocuments() {
        withContext(queryContext) { noteQueries.deleteAllNoteDocuments() }
    }

    suspend fun deleteAllDocumentItems() {
        withContext(queryContext) { noteQueries.deleteAllNoteDocumentItems() }
    }

    suspend fun replaceAllDocuments(documents: List<NoteDocument>) {
        withContext(queryContext) {
            noteQueries.transaction {
                noteQueries.deleteAllNoteDocuments()
                documents.forEach { document ->
                    noteQueries.insertNoteDocument(
                        id = document.id,
                        projectId = document.projectId,
                        name = document.name,
                        createdAt = document.createdAt,
                        updatedAt = document.updatedAt,
                        content = document.content,
                        lastCursorPosition = document.lastCursorPosition.toLong(),
                    )
                }
            }
        }
    }

    suspend fun replaceAllDocumentItems(items: List<NoteDocumentItem>) {
        withContext(queryContext) {
            noteQueries.transaction {
                noteQueries.deleteAllNoteDocumentItems()
                items.forEach { item ->
                    noteQueries.insertNoteDocumentItem(
                        id = item.id,
                        listId = item.listId,
                        parentId = item.parentId,
                        content = item.content,
                        isCompleted = if (item.isCompleted) 1L else 0L,
                        itemOrder = item.itemOrder,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt,
                    )
                }
            }
        }
    }

    suspend fun getAllDocumentsSnapshot(): List<NoteDocument> =
        withContext(queryContext) {
            noteQueries
                .getAllNoteDocuments()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getAllDocumentItemsSnapshot(): List<NoteDocumentItem> =
        withContext(queryContext) {
            noteQueries
                .getAllNoteDocumentItems()
                .executeAsList()
                .map { it.toModel() }
        }

    private fun Note_documents.toModel(): NoteDocument =
        NoteDocument(
            id = id,
            projectId = projectId,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            content = content,
            lastCursorPosition = lastCursorPosition.toInt(),
        )

    private fun Note_document_items.toModel(): NoteDocumentItem =
        NoteDocumentItem(
            id = id,
            listId = listId,
            parentId = parentId,
            content = content,
            isCompleted = isCompleted != 0L,
            itemOrder = itemOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun LegacyNote.toDocument(): NoteDocument =
        NoteDocument(
            id = id,
            projectId = projectId,
            name = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            content = content,
            lastCursorPosition = 0,
        )
}
