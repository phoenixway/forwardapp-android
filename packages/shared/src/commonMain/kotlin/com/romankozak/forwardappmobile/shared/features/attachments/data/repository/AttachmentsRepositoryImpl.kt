package com.romankozak.forwardappmobile.shared.features.attachments.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.Attachment
import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.ProjectAttachment
import com.romankozak.forwardappmobile.shared.features.attachments.domain.repository.AttachmentsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AttachmentsRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : AttachmentsRepository {

    override fun observeProjectAttachments(projectId: String): Flow<List<ProjectAttachment>> =
        database.attachmentsQueries.getAttachmentsForProject(projectId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAttachmentById(id: String): Attachment? = withContext(dispatcher) {
        database.attachmentsQueries.getAttachmentById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findAttachmentByEntity(attachmentType: String, entityId: String): Attachment? = withContext(dispatcher) {
        database.attachmentsQueries.findAttachmentByEntity(
            attachmentType = attachmentType,
            entityId = entityId,
        ).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertAttachment(attachment: Attachment) = withContext(dispatcher) {
        database.attachmentsQueries.insertAttachment(
            id = attachment.id,
            attachmentType = attachment.attachmentType,
            entityId = attachment.entityId,
            ownerProjectId = attachment.ownerProjectId,
            createdAt = attachment.createdAt,
            updatedAt = attachment.updatedAt,
        )
    }

    override suspend fun deleteAttachment(attachmentId: String) = withContext(dispatcher) {
        database.transaction {
            database.attachmentsQueries.deleteAllLinksForAttachment(attachmentId)
            database.attachmentsQueries.deleteAttachment(attachmentId)
        }
    }

    override suspend fun linkAttachmentToProject(
        projectId: String,
        attachmentId: String,
        attachmentOrder: Long,
    ) = withContext(dispatcher) {
        database.projectAttachmentCrossRefQueries.insertProjectAttachmentLink(
            projectId = projectId,
            attachmentId = attachmentId,
            attachmentOrder = attachmentOrder,
        )
    }

    override suspend fun unlinkAttachmentFromProject(
        projectId: String,
        attachmentId: String,
        deleteOrphan: Boolean,
    ) = withContext(dispatcher) {
        database.transaction {
            database.projectAttachmentCrossRefQueries.deleteProjectAttachmentLink(
                projectId = projectId,
                attachmentId = attachmentId,
            )

            if (deleteOrphan) {
                val remainingLinks = database.attachmentsQueries.countLinksForAttachment(attachmentId).executeAsOne()
                if (remainingLinks == 0L) {
                    database.attachmentsQueries.deleteAttachment(attachmentId)
                }
            }
        }
    }

    override suspend fun updateAttachmentOrder(
        projectId: String,
        attachmentId: String,
        attachmentOrder: Long,
    ) = withContext(dispatcher) {
        database.projectAttachmentCrossRefQueries.updateAttachmentOrder(
            projectId = projectId,
            attachmentId = attachmentId,
            attachmentOrder = attachmentOrder,
        )
    }
}
