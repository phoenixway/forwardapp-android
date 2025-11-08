package com.romankozak.forwardappmobile.features.attachments.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.database.Attachments
import com.romankozak.forwardappmobile.shared.database.AttachmentQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.GetAttachmentsForProject
import com.romankozak.forwardappmobile.shared.database.Project_attachment_cross_ref
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.AttachmentWithProject
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemRecord
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.ProjectAttachmentCrossRef
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

    fun getAttachmentsForProject(projectId: String): Flow<List<AttachmentWithProject>> =
        database.attachmentQueriesQueries
            .getAttachmentsForProject(projectId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllAttachments(): Flow<List<AttachmentEntity>> =
        database.attachmentQueriesQueries
            .getAllAttachments()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllAttachmentLinks(): Flow<List<ProjectAttachmentCrossRef>> =
        database.attachmentQueriesQueries
            .getAllProjectAttachmentLinks()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllLinkItems(): Flow<List<LinkItemRecord>> = linkItemDataSource.observeAll()

    suspend fun findAttachmentByEntity(
        attachmentType: String,
        entityId: String,
    ): AttachmentEntity? =
        database.attachmentQueriesQueries
            .findAttachmentByEntity(attachmentType, entityId)
            .executeAsOneOrNull()
            ?.toModel()

    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity? =
        database.attachmentQueriesQueries
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
            database.attachmentQueriesQueries
                .findAttachmentByEntity(attachmentType, entityId)
                .executeAsOneOrNull()
                ?.toModel()
        if (existing != null) return existing

        val attachmentId = uuid4().toString()
        database.attachmentQueriesQueries.insertAttachment(
            id = attachmentId,
            attachment_type = attachmentType,
            entity_id = entityId,
            owner_project_id = ownerProjectId,
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
        database.attachmentQueriesQueries.insertProjectAttachmentLink(
            project_id = projectId,
            attachment_id = attachment.id,
            attachment_order = -createdAt,
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
        database.attachmentQueriesQueries.insertAttachment(
            id = attachment.id,
            attachment_type = attachment.attachmentType,
            entity_id = attachment.entityId,
            owner_project_id = attachment.ownerProjectId,
            createdAt = attachment.createdAt,
            updatedAt = attachment.updatedAt,
        )
        database.attachmentQueriesQueries.insertProjectAttachmentLink(
            project_id = projectId,
            attachment_id = attachment.id,
            attachment_order = -timestamp,
        )
        return attachment
    }

    suspend fun linkAttachmentToProject(
        attachmentId: String,
        projectId: String,
        order: Long = -currentTimeMillis(),
    ) {
        database.attachmentQueriesQueries.insertProjectAttachmentLink(
            project_id = projectId,
            attachment_id = attachmentId,
            attachment_order = order,
        )
    }

    suspend fun unlinkAttachmentFromProject(
        attachmentId: String,
        projectId: String,
    ): Boolean {
        val attachment =
            database.attachmentQueriesQueries
                .getAttachmentById(attachmentId)
                .executeAsOneOrNull()
                ?.toModel()
                ?: return false

        database.attachmentQueriesQueries.deleteProjectAttachmentLink(project_id = projectId, attachment_id = attachmentId)

        val remainingLinks =
            database.attachmentQueriesQueries
                .countLinksForAttachment(attachmentId)
                .executeAsOne()

        val noMoreLinks = remainingLinks <= 0L
        if (noMoreLinks) {
            database.attachmentQueriesQueries.deleteAttachment(attachmentId)
            if (attachment.attachmentType == LINK_ITEM_TYPE) {
                linkItemDataSource.deleteById(attachment.entityId)
            }
        }
        return noMoreLinks
    }

    suspend fun deleteAttachment(attachmentId: String) {
        val attachment =
            database.attachmentQueriesQueries
                .getAttachmentById(attachmentId)
                .executeAsOneOrNull()
                ?.toModel()
        database.attachmentQueriesQueries.deleteAllLinksForAttachment(attachmentId)
        database.attachmentQueriesQueries.deleteAttachment(attachmentId)
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
                database.attachmentQueriesQueries.updateAttachmentOrder(
                    project_id = projectId,
                    attachment_id = attachmentId,
                    attachment_order = order,
                )
            }
        }
    }

    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun Attachments.toModel(): AttachmentEntity =
        AttachmentEntity(
            id = id,
            attachmentType = attachment_type,
            entityId = entity_id,
            ownerProjectId = owner_project_id,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun Project_attachment_cross_ref.toModel(): ProjectAttachmentCrossRef =
        ProjectAttachmentCrossRef(
            projectId = project_id,
            attachmentId = attachment_id,
            attachmentOrder = attachment_order,
        )

    private fun GetAttachmentsForProject.toModel(): AttachmentWithProject =
        AttachmentWithProject(
            attachment =
                AttachmentEntity(
                    id = id,
                    attachmentType = attachment_type,
                    entityId = entity_id,
                    ownerProjectId = owner_project_id,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                ),
            projectId = project_id,
            attachmentOrder = attachment_order,
        )

    private companion object {
        private const val LINK_ITEM_TYPE = "LINK_ITEM"
    }
}
