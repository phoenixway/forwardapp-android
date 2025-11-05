package com.romankozak.forwardappmobile.shared.features.attachments.data.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

data class AttachmentEntity(
    val id: String = uuid4().toString(),
    val attachmentType: String,
    val entityId: String,
    val ownerProjectId: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

data class ProjectAttachmentCrossRef(
    val projectId: String,
    val attachmentId: String,
    val attachmentOrder: Long = -nowMillis(),
)

data class AttachmentWithProject(
    val attachment: AttachmentEntity,
    val projectId: String?,
    val attachmentOrder: Long?,
)
