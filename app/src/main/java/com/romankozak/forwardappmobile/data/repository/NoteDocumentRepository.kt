package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.legacy.toNoteDocument
import com.romankozak.forwardappmobile.domain.ai.events.SystemNoteUpdatedEvent
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteDocumentRepository
    @Inject
    constructor(
        private val noteDocumentDao: NoteDocumentDao,
        private val attachmentRepository: AttachmentsRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val aiEventRepository: AiEventRepository,
    ) {
        private val TAG = "NoteDocumentRepository"

        fun getDocumentsForContext(contextId: String): Flow<List<NoteDocumentEntity>> =
            noteDocumentDao.getDocumentsForContext(contextId)

        fun getAllDocumentsAsFlow(): Flow<List<NoteDocumentEntity>> = noteDocumentDao.getAllDocumentsAsFlow()

        fun getDocumentByIdFlow(id: String): Flow<NoteDocumentEntity?> = noteDocumentDao.getDocumentByIdFlow(id)

        suspend fun findDocumentByName(name: String): NoteDocumentEntity? = noteDocumentDao.findByName(name)

        @Transaction
        suspend fun createDocument(
            name: String,
            contextId: String,
            content: String? = null,
            attachmentType: String = BacklogItemTypeValues.NOTE_DOCUMENT,
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): String {
            Log.d(TAG, "createDocument called with name: $name, contextId: $contextId, content: $content")
            val now = System.currentTimeMillis()
            val resolvedName = name.trim().ifBlank { DEFAULT_NOTE_NAME }
            val resolvedContent =
                if (attachmentType == BacklogItemTypeValues.NOTE_DOCUMENT && content.isNullOrBlank()) {
                    markdownTitleContent(resolvedName)
                } else {
                    content
                }
            val document =
                NoteDocumentEntity(
                    name = resolvedName,
                    contextId = contextId,
                    content = resolvedContent,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            Log.d(TAG, "Inserting new note document: $document")
            noteDocumentDao.insertDocument(document)
            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType = attachmentType,
                entityId = document.id,
                contextId = contextId,
                ownerContextId = contextId,
                createdAt = document.createdAt,
                roleCode = roleCode,
                isSystem = isSystem,
            )
            Log.d(TAG, "createDocument finished")
            return document.id
        }

        private fun markdownTitleContent(title: String): String = "# $title\n"

        suspend fun createDetachedDocument(
            id: String,
            name: String,
            contextId: String,
            content: String? = null,
            lastCursorPosition: Int = 0,
        ): String {
            val now = System.currentTimeMillis()
            val document =
                NoteDocumentEntity(
                    id = id,
                    contextId = contextId,
                    name = name,
                    updatedAt = now,
                    content = content,
                    lastCursorPosition = lastCursorPosition,
                    syncedAt = null,
                    version = 1,
                )
            noteDocumentDao.insertDocument(document)
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
            attachmentRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.let {
                attachmentRepository.deleteAttachment(it.id)
            }
            attachmentRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.let {
                attachmentRepository.deleteAttachment(it.id)
            }
        }

        suspend fun importFromLegacy(note: LegacyNoteEntity) {
            val document = note.toNoteDocument()
            noteDocumentDao.insertDocument(document)
            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                entityId = document.id,
                contextId = document.contextId,
                ownerContextId = document.contextId,
                createdAt = document.createdAt,
            )
            recentItemsRepository.logNoteDocumentAccess(document)
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
                    ),
                )
            }
        }

        private companion object {
            private const val DEFAULT_NOTE_NAME = "Default note"
        }
    }
