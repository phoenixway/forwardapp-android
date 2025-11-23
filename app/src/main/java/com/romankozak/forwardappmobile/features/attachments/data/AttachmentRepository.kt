package com.romankozak.forwardappmobile.features.attachments.data

import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentWithProject
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val linkItemDao: LinkItemDao,
) {

    fun getAttachmentsForProject(projectId: String): Flow<List<AttachmentWithProject>> =
        attachmentDao.getAttachmentsForProject(projectId)

    fun getAllAttachments(): Flow<List<AttachmentEntity>> = attachmentDao.getAllAttachmentsFlow()

    fun getAllAttachmentLinks(): Flow<List<ProjectAttachmentCrossRef>> =
        attachmentDao.getAllProjectAttachmentLinksFlow()

    fun getAllLinkItems(): Flow<List<LinkItemEntity>> = linkItemDao.getAllEntitiesAsFlow()

    fun getAttachmentLibraryItems(): Flow<List<com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentLibraryQueryResult>> =
        attachmentDao.getAttachmentLibraryItems()

    suspend fun findAttachmentByEntity(
        attachmentType: String,
        entityId: String,
    ): AttachmentEntity? =
        attachmentDao.findAttachmentByEntity(attachmentType, entityId)

    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity? =
        attachmentDao.getAttachmentById(attachmentId)

    suspend fun ensureAttachmentForEntity(
        attachmentType: String,
        entityId: String,
        ownerProjectId: String?,
        createdAt: Long = System.currentTimeMillis(),
    ): AttachmentEntity {
        val existing = attachmentDao.findAttachmentByEntity(attachmentType, entityId)
        if (existing != null) {
            return existing
        }

        val attachment =
            AttachmentEntity(
                id = UUID.randomUUID().toString(),
                attachmentType = attachmentType,
                entityId = entityId,
                ownerProjectId = ownerProjectId,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        attachmentDao.insertAttachment(attachment)
        return attachment
    }

    suspend fun ensureAttachmentLinkedToProject(
        attachmentType: String,
        entityId: String,
        projectId: String,
        ownerProjectId: String? = null,
        createdAt: Long = System.currentTimeMillis(),
    ): AttachmentEntity {
        val attachment = ensureAttachmentForEntity(attachmentType, entityId, ownerProjectId, createdAt)
        attachmentDao.insertProjectAttachmentLink(
            ProjectAttachmentCrossRef(
                projectId = projectId,
                attachmentId = attachment.id,
                attachmentOrder = -createdAt,
            ),
        )
        return attachment
    }

    suspend fun createLinkAttachment(
        projectId: String,
        link: RelatedLink,
    ): AttachmentEntity {
        val timestamp = System.currentTimeMillis()
        val linkEntity =
            LinkItemEntity(
                id = UUID.randomUUID().toString(),
                linkData = link,
                createdAt = timestamp,
            )
        linkItemDao.insert(linkEntity)

        val attachment =
            AttachmentEntity(
                id = UUID.randomUUID().toString(),
                attachmentType = ListItemTypeValues.LINK_ITEM,
                entityId = linkEntity.id,
                ownerProjectId = projectId,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        attachmentDao.insertAttachment(attachment)
        attachmentDao.insertProjectAttachmentLink(
            ProjectAttachmentCrossRef(
                projectId = projectId,
                attachmentId = attachment.id,
                attachmentOrder = -timestamp,
            ),
        )
        return attachment
    }

    suspend fun linkAttachmentToProject(
        attachmentId: String,
        projectId: String,
        order: Long = -System.currentTimeMillis(),
    ) {
        attachmentDao.insertProjectAttachmentLink(
            ProjectAttachmentCrossRef(
                projectId = projectId,
                attachmentId = attachmentId,
                attachmentOrder = order,
            ),
        )
    }

    suspend fun unlinkAttachmentFromProject(
        attachmentId: String,
        projectId: String,
    ): Boolean {
        val attachment = attachmentDao.getAttachmentById(attachmentId) ?: return false
        attachmentDao.deleteProjectAttachmentLink(projectId, attachmentId)
        val remainingLinks = attachmentDao.countLinksForAttachment(attachmentId)
        val noMoreLinks = remainingLinks <= 0
        if (noMoreLinks) {
            attachmentDao.deleteAttachment(attachmentId)
            if (attachment.attachmentType == ListItemTypeValues.LINK_ITEM) {
                linkItemDao.deleteById(attachment.entityId)
            }
        }
        return noMoreLinks
    }

    suspend fun deleteAttachment(attachmentId: String) {
        val attachment = attachmentDao.getAttachmentById(attachmentId)
        attachmentDao.deleteAllLinksForAttachment(attachmentId)
        attachmentDao.deleteAttachment(attachmentId)
        if (attachment != null && attachment.attachmentType == ListItemTypeValues.LINK_ITEM) {
            linkItemDao.deleteById(attachment.entityId)
        }
    }

    suspend fun updateAttachmentOrders(
        projectId: String,
        updates: List<Pair<String, Long>>,
    ) {
        if (updates.isEmpty()) return
        updates.forEach { (attachmentId, order) ->
            attachmentDao.updateAttachmentOrder(projectId, attachmentId, order)
        }
    }
}
