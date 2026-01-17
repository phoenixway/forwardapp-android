package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.legacy.toNoteDocument
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItemTypeValues
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import com.romankozak.forwardappmobile.domain.ai.events.SystemNoteUpdatedEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteDocumentRepository @Inject constructor(
    private val noteDocumentDao: NoteDocumentDao,
    private val attachmentRepository: AttachmentRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val aiEventRepository: AiEventRepository,
) {
    private val TAG = "NoteDocumentRepository"

    fun getDocumentsForProject(projectId: String): Flow<List<NoteDocumentEntity>> =
        noteDocumentDao.getDocumentsForProject(projectId, ListItemTypeValues.NOTE_DOCUMENT)

    fun getAllDocumentsAsFlow(): Flow<List<NoteDocumentEntity>> =
        noteDocumentDao.getAllDocumentsAsFlow()

    suspend fun findDocumentByName(name: String): NoteDocumentEntity? =
        noteDocumentDao.findByName(name)

    @Transaction
    suspend fun createDocument(
        name: String,
        projectId: String,
        content: String? = null,
        roleCode: String? = null,
        isSystem: Boolean = false,
    ): String {
        Log.d(TAG, "createDocument called with name: $name, projectId: $projectId, content: $content")
        val now = System.currentTimeMillis()
        val document = NoteDocumentEntity(name = name, projectId = projectId, content = content, updatedAt = now, syncedAt = null, version = 1)
        Log.d(TAG, "Inserting new note document: $document")
        noteDocumentDao.insertDocument(document)
        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
            entityId = document.id,
            projectId = projectId,
            ownerProjectId = projectId,
            createdAt = document.createdAt,
            roleCode = roleCode,
            isSystem = isSystem,
        )
        Log.d(TAG, "createDocument finished")
        return document.id
    }

    @Transaction
    suspend fun deleteDocument(documentId: String) {
        val now = System.currentTimeMillis()
        val existing = noteDocumentDao.getDocumentById(documentId)
        if (existing != null) {
            noteDocumentDao.insertDocument(
                existing.softDelete(now),
            )
        } else {
            noteDocumentDao.deleteDocumentById(documentId)
        }
        attachmentRepository.findAttachmentByEntity(ListItemTypeValues.NOTE_DOCUMENT, documentId)?.let {
            attachmentRepository.deleteAttachment(it.id)
        }
    }

    fun getDocumentItems(documentId: String): Flow<List<NoteDocumentItemEntity>> =
        noteDocumentDao.getItemsForDocument(documentId)

    suspend fun saveDocumentItem(item: NoteDocumentItemEntity) {
        val existingItem = noteDocumentDao.getListItemById(item.id)
        if (existingItem == null) {
            val now = System.currentTimeMillis()
            noteDocumentDao.insertListItem(
                item.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = item.version + 1,
                ),
            )
        } else {
            val now = System.currentTimeMillis()
            noteDocumentDao.updateListItem(
                item.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = existingItem.version + 1,
                ),
            )
        }
    }

    suspend fun deleteDocumentItem(itemId: String) {
        val now = System.currentTimeMillis()
        val existing = noteDocumentDao.getListItemById(itemId)
        if (existing != null) {
            noteDocumentDao.insertListItem(
                existing.softDelete(now),
            )
        } else {
            noteDocumentDao.deleteListItemById(itemId)
        }
    }

    suspend fun importFromLegacy(note: LegacyNoteEntity) {
        val document = note.toNoteDocument()
        noteDocumentDao.insertDocument(document)
        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
            entityId = document.id,
            projectId = document.projectId,
            ownerProjectId = document.projectId,
            createdAt = document.createdAt,
        )
        recentItemsRepository.logNoteDocumentAccess(document)
    }

    suspend fun updateDocumentItems(items: List<NoteDocumentItemEntity>) {
        val now = System.currentTimeMillis()
        noteDocumentDao.updateListItems(
            items.map { it.bumpSync(now) },
        )
    }

    suspend fun getDocumentById(id: String): NoteDocumentEntity? {
        Log.d(TAG, "getDocumentById called with id: $id")
        val document = noteDocumentDao.getDocumentById(id)
        Log.d(TAG, "getDocumentById returned: $document")
        return document
    }

    suspend fun updateDocument(document: NoteDocumentEntity) {
        android.util.Log.d(
            "CursorDebug",
            "Repository updating note document. lastCursorPosition: ${document.lastCursorPosition}",
        )
        Log.d(TAG, "updateDocument called with document: $document")
        val now = System.currentTimeMillis()
        noteDocumentDao.updateDocument(document.bumpSync(now))
        recentItemsRepository.updateRecentItemDisplayName(document.id, document.name)
        Log.d(TAG, "updateDocument finished")
        if (document.name == "my-life-current-state") {
            aiEventRepository.emit(
                SystemNoteUpdatedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    noteId = document.id,
                    textLength = document.content?.length ?: 0,
                )
            )
        }
    }
}
