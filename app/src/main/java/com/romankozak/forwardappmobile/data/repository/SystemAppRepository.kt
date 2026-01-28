package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.sync.AttachmentsRepository // Updated import
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.core.data.models.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.SystemAppType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemAppRepository
    @Inject
    constructor(
        private val systemAppDao: SystemAppDao,
        private val contextDao: ContextDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val attachmentRepository: com.romankozak.forwardappmobile.sync.AttachmentsRepository, // Updated type
    ) {
        suspend fun getSystemApp(systemKey: String): SystemAppEntity? = systemAppDao.getBySystemKey(systemKey)

        suspend fun ensureNoteApp(
            systemKey: String,
            projectSystemKey: String,
            documentName: String,
        ): SystemAppEntity {
            val contextId =
                contextDao.getContextById("sys_$projectSystemKey")?.id
                    ?: throw IllegalStateException("System project $projectSystemKey не знайдено")

            val existingApp = systemAppDao.getBySystemKey(systemKey)
            val documentId =
                existingApp?.noteDocumentId?.let { noteDocumentId ->
                    val existingDocument = noteDocumentDao.getDocumentById(noteDocumentId)
                    existingDocument?.id ?: createNoteDocument(documentName, contextId)
                } ?: createNoteDocument(documentName, contextId)

            val systemApp =
                (
                    existingApp ?: SystemAppEntity(
                        systemKey = systemKey,
                        appType = SystemAppType.NOTE_DOCUMENT.name,
                        contextId = contextId,
                    )
                ).copy(
                    contextId = contextId,
                    noteDocumentId = documentId,
                    updatedAt = System.currentTimeMillis(),
                )

            systemAppDao.upsert(systemApp)
            return systemApp
        }

        suspend fun getSystemNote(systemKey: String): NoteDocumentEntity? =
            systemAppDao.getBySystemKey(systemKey)?.noteDocumentId?.let { noteDocumentDao.getDocumentById(it) }

        suspend fun linkSystemNoteToProject(
            systemKey: String,
            targetProjectSystemKey: String,
        ) {
            val systemApp = systemAppDao.getBySystemKey(systemKey) ?: return
            val noteId = systemApp.noteDocumentId ?: return
            val targetcontextId = contextDao.getContextById("sys_$targetProjectSystemKey")?.id ?: return

            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                entityId = noteId,
                contextId = targetcontextId,
                ownerContextId = systemApp.contextId,
            )
        }

        private suspend fun createNoteDocument(
            name: String,
            contextId: String,
        ): String {
            val noteDocument =
                NoteDocumentEntity(
                    name = name,
                    contextId = contextId,
                )
            noteDocumentDao.insertDocument(noteDocument)
            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                entityId = noteDocument.id,
                contextId = contextId,
                ownerContextId = contextId,
                createdAt = noteDocument.createdAt,
            )
            return noteDocument.id
        }
    }
