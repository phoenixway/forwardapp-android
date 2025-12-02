package com.romankozak.forwardappmobile.features.attachments.data

import android.util.Log
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentWithProject
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import com.romankozak.forwardappmobile.data.sync.softDelete
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

const val ATTACHMENT_LOG_TAG = "FWD_ATTACH"

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
            Log.d(ATTACHMENT_LOG_TAG, "[ensureAttachmentForEntity] Found existing attachment: id=${existing.id}, type=$attachmentType, entity=$entityId")
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
                syncedAt = null,
                version = 1,
            )
        attachmentDao.insertAttachment(attachment)
        Log.d(ATTACHMENT_LOG_TAG, "[ensureAttachmentForEntity] Created new attachment: id=${attachment.id}, type=$attachmentType, entity=$entityId, syncedAt=${attachment.syncedAt}")
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
        
        // Check if this link already exists to prevent duplicates
        val existingLink = attachmentDao.getProjectAttachmentLink(projectId, attachment.id)
        if (existingLink == null) {
            attachmentDao.insertProjectAttachmentLink(
                ProjectAttachmentCrossRef(
                    projectId = projectId,
                    attachmentId = attachment.id,
                    attachmentOrder = -createdAt,
                ),
            )
        }
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
                updatedAt = timestamp,
                syncedAt = null,
                version = 1,
            )
        linkItemDao.insert(linkEntity)
        Log.d(ATTACHMENT_LOG_TAG, "[createLinkAttachment] Created LinkItemEntity: id=${linkEntity.id}, syncedAt=${linkEntity.syncedAt}")

        val attachment =
            AttachmentEntity(
                id = UUID.randomUUID().toString(),
                attachmentType = ListItemTypeValues.LINK_ITEM,
                entityId = linkEntity.id,
                ownerProjectId = projectId,
                createdAt = timestamp,
                updatedAt = timestamp,
                syncedAt = null,
                version = 1,
            )
        attachmentDao.insertAttachment(attachment)
        Log.d(ATTACHMENT_LOG_TAG, "[createLinkAttachment] Created AttachmentEntity: id=${attachment.id}, linkId=${linkEntity.id}, project=$projectId, syncedAt=${attachment.syncedAt}")
        
        attachmentDao.insertProjectAttachmentLink(
            ProjectAttachmentCrossRef(
                projectId = projectId,
                attachmentId = attachment.id,
                attachmentOrder = -timestamp,
                updatedAt = timestamp,
                syncedAt = null,
                version = 1,
            ),
        )
        Log.d(ATTACHMENT_LOG_TAG, "[createLinkAttachment] Created ProjectAttachmentCrossRef: project=$projectId, attachment=${attachment.id}")
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
                updatedAt = System.currentTimeMillis(),
                syncedAt = null,
                version = 1,
            ),
        )
    }

    suspend fun unlinkAttachmentFromProject(
        attachmentId: String,
        projectId: String,
    ): Boolean {
        val attachment = attachmentDao.getAttachmentById(attachmentId) ?: return false
        val now = System.currentTimeMillis()
        val link = attachmentDao.getProjectAttachmentLink(projectId, attachmentId)
        if (link != null) {
            attachmentDao.insertProjectAttachmentLink(
                link.softDelete(now),
            )
        } else {
            attachmentDao.deleteProjectAttachmentLink(projectId, attachmentId)
        }
        val remainingLinks = attachmentDao.countLinksForAttachment(attachmentId)
        val noMoreLinks = remainingLinks <= 0
        if (noMoreLinks) {
            attachmentDao.insertAttachment(
                attachment.softDelete(now),
            )
            if (attachment.attachmentType == ListItemTypeValues.LINK_ITEM) {
                linkItemDao.deleteById(attachment.entityId)
            }
        }
        return noMoreLinks
    }

    suspend fun deleteAttachment(attachmentId: String) {
        val now = System.currentTimeMillis()
        val attachment = attachmentDao.getAttachmentById(attachmentId)
        if (attachment != null) {
            attachmentDao.insertAttachment(
                attachment.softDelete(now),
            )
            // mark links deleted too
            val links = attachmentDao.getProjectAttachmentLinksForAttachment(attachmentId)
            links.forEach { link ->
                attachmentDao.insertProjectAttachmentLink(
                    link.softDelete(now),
                )
            }
        } else {
            attachmentDao.deleteAllLinksForAttachment(attachmentId)
            attachmentDao.deleteAttachment(attachmentId)
        }
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
            val now = System.currentTimeMillis()
            val existing = attachmentDao.getProjectAttachmentLink(projectId, attachmentId)
            if (existing != null) {
                attachmentDao.insertProjectAttachmentLink(
                    existing.copy(
                        attachmentOrder = order,
                        updatedAt = now,
                        syncedAt = null,
                        version = existing.version + 1,
                    ),
                )
            } else {
                attachmentDao.updateAttachmentOrder(projectId, attachmentId, order)
            }
        }
    }
}
