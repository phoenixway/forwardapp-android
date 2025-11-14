package com.romankozak.forwardappmobile.shared.features.attachments.domain.repository

import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.Attachment
import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.ProjectAttachment
import kotlinx.coroutines.flow.Flow

interface AttachmentsRepository {
    fun observeProjectAttachments(projectId: String): Flow<List<ProjectAttachment>>

    suspend fun getAttachmentById(id: String): Attachment?
    suspend fun findAttachmentByEntity(attachmentType: String, entityId: String): Attachment?

    suspend fun upsertAttachment(attachment: Attachment)
    suspend fun deleteAttachment(attachmentId: String)

    suspend fun linkAttachmentToProject(projectId: String, attachmentId: String, attachmentOrder: Long)
    suspend fun unlinkAttachmentFromProject(projectId: String, attachmentId: String, deleteOrphan: Boolean = true)
    suspend fun updateAttachmentOrder(projectId: String, attachmentId: String, attachmentOrder: Long)
}
