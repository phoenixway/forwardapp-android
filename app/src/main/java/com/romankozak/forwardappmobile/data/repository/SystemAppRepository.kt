package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppType
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemAppRepository
    @Inject
    constructor(
        private val systemAppDao: SystemAppDao,
        private val workspaceDao: WorkspaceDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val attachmentDao: AttachmentDao,
        private val canonicalConnectionsRepository: CanonicalConnectionsRepository,
    ) {
        suspend fun getSystemApp(systemKey: String): SystemAppEntity? =
            systemAppDao.getBySystemKey(systemKey)

        suspend fun ensureNoteApp(
            systemKey: String,
            projectSystemKey: String,
            documentName: String,
        ): SystemAppEntity {
            val workspaceId = "sys_$projectSystemKey"
            requireLiveWorkspace(workspaceId)

            val existingApp = systemAppDao.getBySystemKey(systemKey)
            require(existingApp == null || existingApp.workspaceId == workspaceId) {
                "SystemApp $systemKey already belongs to another Workspace"
            }

            val documentId =
                existingApp?.noteDocumentId?.let { noteDocumentId ->
                    val existingDocument = noteDocumentDao.getDocumentById(noteDocumentId)
                    existingDocument?.id ?: createNoteDocument(documentName, workspaceId)
                } ?: createNoteDocument(documentName, workspaceId)

            val systemApp =
                (
                    existingApp ?: SystemAppEntity(
                        systemKey = systemKey,
                        appType = SystemAppType.NOTE_DOCUMENT.name,
                        workspaceId = workspaceId,
                    )
                ).copy(
                    noteDocumentId = documentId,
                    updatedAt = System.currentTimeMillis(),
                )

            systemAppDao.upsert(systemApp)
            return systemApp
        }

        suspend fun getSystemNote(systemKey: String): NoteDocumentEntity? =
            systemAppDao.getBySystemKey(systemKey)?.noteDocumentId?.let {
                noteDocumentDao.getDocumentById(it)
            }

        suspend fun linkSystemNoteToProject(
            systemKey: String,
            targetProjectSystemKey: String,
        ) {
            val systemApp = systemAppDao.getBySystemKey(systemKey) ?: return
            val noteId = systemApp.noteDocumentId ?: return
            val targetWorkspaceId = "sys_$targetProjectSystemKey"
            requireLiveWorkspace(targetWorkspaceId)

            val attachment =
                ensureNoteAttachment(
                    entityId = noteId,
                    ownerWorkspaceId = systemApp.workspaceId,
                    createdAt = System.currentTimeMillis(),
                )
            canonicalConnectionsRepository.linkAttachment(
                workspaceId = targetWorkspaceId,
                attachmentId = attachment.id,
            )
        }

        private suspend fun createNoteDocument(
            name: String,
            workspaceId: String,
        ): String {
            val noteDocument =
                NoteDocumentEntity(
                    name = name,
                    contextId = workspaceId,
                )
            noteDocumentDao.insertDocument(noteDocument)

            ensureNoteAttachment(
                entityId = noteDocument.id,
                ownerWorkspaceId = workspaceId,
                createdAt = noteDocument.createdAt,
            )
            return noteDocument.id
        }

        private suspend fun ensureNoteAttachment(
            entityId: String,
            ownerWorkspaceId: String,
            createdAt: Long,
        ): AttachmentEntity {
            attachmentDao.findAttachmentByEntity(
                BacklogItemTypeValues.NOTE_DOCUMENT,
                entityId,
            )?.let { return it }

            val attachment =
                AttachmentEntity(
                    attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                    entityId = entityId,
                    ownerContextId = ownerWorkspaceId,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    version = 1L,
                )
            attachmentDao.insertAttachment(attachment)
            return attachment
        }

        private suspend fun requireLiveWorkspace(workspaceId: String) {
            val workspace = workspaceDao.getById(workspaceId)
            require(workspace != null && !workspace.isDeleted) {
                "System Workspace $workspaceId не знайдено або видалено"
            }
        }
    }
