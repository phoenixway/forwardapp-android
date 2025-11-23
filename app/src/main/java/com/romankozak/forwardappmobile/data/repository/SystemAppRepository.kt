package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.SystemAppEntity
import com.romankozak.forwardappmobile.data.database.models.SystemAppType
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemAppRepository @Inject constructor(
    private val systemAppDao: SystemAppDao,
    private val projectDao: ProjectDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val attachmentRepository: AttachmentRepository,
) {
    suspend fun getSystemApp(systemKey: String): SystemAppEntity? = systemAppDao.getBySystemKey(systemKey)

    suspend fun ensureNoteApp(
        systemKey: String,
        projectSystemKey: String,
        documentName: String,
    ): SystemAppEntity {
        val projectId =
            projectDao.getProjectBySystemKey(projectSystemKey)?.id
                ?: throw IllegalStateException("System project $projectSystemKey не знайдено")

        val existingApp = systemAppDao.getBySystemKey(systemKey)
        val documentId =
            existingApp?.noteDocumentId?.let { noteDocumentId ->
                val existingDocument = noteDocumentDao.getDocumentById(noteDocumentId)
                existingDocument?.id ?: createNoteDocument(documentName, projectId)
            } ?: createNoteDocument(documentName, projectId)

        val systemApp =
            (existingApp ?: SystemAppEntity(
                systemKey = systemKey,
                appType = SystemAppType.NOTE_DOCUMENT.name,
                projectId = projectId,
            )).copy(
                projectId = projectId,
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
        val targetProjectId = projectDao.getProjectBySystemKey(targetProjectSystemKey)?.id ?: return

        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
            entityId = noteId,
            projectId = targetProjectId,
            ownerProjectId = systemApp.projectId,
        )
    }

    private suspend fun createNoteDocument(
        name: String,
        projectId: String,
    ): String {
        val noteDocument =
            NoteDocumentEntity(
                name = name,
                projectId = projectId,
            )
        noteDocumentDao.insertDocument(noteDocument)
        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
            entityId = noteDocument.id,
            projectId = projectId,
            ownerProjectId = projectId,
            createdAt = noteDocument.createdAt,
        )
        return noteDocument.id
    }
}
