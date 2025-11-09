package com.romankozak.forwardappmobile.features.attachments.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.database.Attachments
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.GetAttachmentsForProject
import com.romankozak.forwardappmobile.shared.database.ProjectAttachmentCrossRef
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.AttachmentWithProject
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class AttachmentRepository(
    private val database: ForwardAppDatabase,
    private val linkItemDataSource: LinkItemDataSource,
    private val queryContext: CoroutineContext = EmptyCoroutineContext,
) {
    private val attachmentsQueries = database.attachmentsQueries
    private val crossRefQueries = database.projectAttachmentCrossRefQueries

    fun getAttachmentsForProject(projectId: String): Flow<List<AttachmentWithProject>> =
        attachmentsQueries
            .getAttachmentsForProject(projectId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllAttachments(): Flow<List<AttachmentEntity>> =
        attachmentsQueries
            .getAllAttachments()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllAttachmentLinks(): Flow<List<com.romankozak.forwardappmobile.shared.features.attachments.data.model.ProjectAttachmentCrossRef>> =
        crossRefQueries
            .getAllProjectAttachmentLinks()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllLinkItems(): Flow<List<LinkItemRecord>> = linkItemDataSource.observeAll()

    suspend fun findAttachmentByEntity(
        attachmentType: String,
        entityId: String,
    ): AttachmentEntity? =
        attachmentsQueries
            .findAttachmentByEntity(attachmentType, entityId)
            .executeAsOneOrNull()
            ?.toModel()

    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity? =
        attachmentsQueries
            .getAttachmentById(attachmentId)
            .executeAsOneOrNull()
            ?.toModel()

    suspend fun ensureAttachmentForEntity(
        attachmentType: String,
        entityId: String,
        ownerProjectId: String?,
        createdAt: Long = currentTimeMillis(),
    ): AttachmentEntity {
        val existing =
            attachmentsQueries
                .findAttachmentByEntity(attachmentType, entityId)
                .executeAsOneOrNull()
                ?.toModel()
        if (existing != null) return existing

        val attachmentId = uuid4().toString()
        attachmentsQueries.insertAttachment(
            id = attachmentId,
            attachmentType = attachmentType,
            entityId = entityId,
            ownerProjectId = ownerProjectId,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        return AttachmentEntity(
            id = attachmentId,
            attachmentType = attachmentType,
            entityId = entityId,
            ownerProjectId = ownerProjectId,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    suspend fun ensureAttachmentLinkedToProject(
        attachmentType: String,
        entityId: String,
        projectId: String,
        ownerProjectId: String? = null,
        createdAt: Long = currentTimeMillis(),
    ): AttachmentEntity {
        val attachment = ensureAttachmentForEntity(attachmentType, entityId, ownerProjectId, createdAt)
        crossRefQueries.insertProjectAttachmentLink(
            projectId = projectId,
            attachmentId = attachment.id,
            attachmentOrder = -createdAt,
        )
        return attachment
    }

    suspend fun createLinkAttachment(
        projectId: String,
        link: RelatedLink,
    ): AttachmentEntity {
        val timestamp = currentTimeMillis()
        val linkItem =
            LinkItemRecord(
                id = uuid4().toString(),
                linkData = link,
                createdAt = timestamp,
            )
        linkItemDataSource.insert(linkItem)

        val attachment =
            AttachmentEntity(
                id = uuid4().toString(),
                attachmentType = LINK_ITEM_TYPE,
                entityId = linkItem.id,
                ownerProjectId = projectId,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        attachmentsQueries.insertAttachment(
            id = attachment.id,
            attachmentType = attachment.attachmentType,
            entityId = attachment.entityId,
            ownerProjectId = attachment.ownerProjectId,
            createdAt = attachment.createdAt,
            updatedAt = attachment.updatedAt,
        )
        crossRefQueries.insertProjectAttachmentLink(
            projectId = projectId,
            attachmentId = attachment.id,
            attachmentOrder = -timestamp,
        )
        return attachment
    }

    suspend fun linkAttachmentToProject(
        attachmentId: String,
        projectId: String,
        order: Long = -currentTimeMillis(),
    ) {
        crossRefQueries.insertProjectAttachmentLink(
            projectId = projectId,
            attachmentId = attachmentId,
            attachmentOrder = order,
        )
    }

    suspend fun unlinkAttachmentFromProject(
        attachmentId: String,
        projectId: String,
    ): Boolean {
        val attachment =
            attachmentsQueries
                .getAttachmentById(attachmentId)
                .executeAsOneOrNull()
                ?.toModel()
                ?: return false

        crossRefQueries.deleteProjectAttachmentLink(projectId = projectId, attachmentId = attachmentId)

        val remainingLinks =
            attachmentsQueries
                .countLinksForAttachment(attachmentId)
                .executeAsOne()

        val noMoreLinks = remainingLinks <= 0L
        if (noMoreLinks) {
            attachmentsQueries.deleteAttachment(attachmentId)
            if (attachment.attachmentType == LINK_ITEM_TYPE) {
                linkItemDataSource.deleteById(attachment.entityId)
            }
        }
        return noMoreLinks
    }

    suspend fun deleteAttachment(attachmentId: String) {
        val attachment =
            attachmentsQueries
                .getAttachmentById(attachmentId)
                .executeAsOneOrNull()
                ?.toModel()
        attachmentsQueries.deleteAllLinksForAttachment(attachmentId)
        attachmentsQueries.deleteAttachment(attachmentId)
        if (attachment != null && attachment.attachmentType == LINK_ITEM_TYPE) {
            linkItemDataSource.deleteById(attachment.entityId)
        }
    }

    suspend fun updateAttachmentOrders(
        projectId: String,
        updates: List<Pair<String, Long>>,
    ) {
        if (updates.isEmpty()) return
        database.transaction {
            updates.forEach { (attachmentId, order) ->
                crossRefQueries.updateAttachmentOrder(
                    projectId = projectId,
                    attachmentId = attachmentId,
                    attachmentOrder = order,
                )
            }
        }
    }

    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun Attachments.toModel(): AttachmentEntity =
        AttachmentEntity(
            id = id,
            attachmentType = attachmentType,
            entityId = entityId,
            ownerProjectId = ownerProjectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun ProjectAttachmentCrossRef.toModel(): com.romankozak.forwardappmobile.shared.features.attachments.data.model.ProjectAttachmentCrossRef =
        com.romankozak.forwardappmobile.shared.features.attachments.data.model.ProjectAttachmentCrossRef(
            projectId = projectId,
            attachmentId = attachmentId,
            attachmentOrder = attachmentOrder,
        )

    private fun GetAttachmentsForProject.toModel(): AttachmentWithProject =
        AttachmentWithProject(
            attachment =
            AttachmentEntity(
                id = id,
                attachmentType = attachmentType,
                entityId = entityId,
                ownerProjectId = ownerProjectId,
                createdAt = createdAt,
                updatedAt = updatedAt,
            ),
            projectId = projectId,
            attachmentOrder = attachmentOrder,
        )

    private companion object {
        private const val LINK_ITEM_TYPE = "LINK_ITEM"
    }
}