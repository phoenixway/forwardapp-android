package com.romankozak.forwardappmobile.shared.features.attachments.domain.model

data class ProjectAttachment(
    val attachment: Attachment,
    val projectId: String,
    val attachmentOrder: Long,
)
