package com.romankozak.forwardappmobile.shared.features.attachments.domain.model

data class ProjectAttachmentLink(
    val projectId: String,
    val attachmentId: String,
    val attachmentOrder: Long,
)
