package com.romankozak.forwardappmobile.shared.features.attachments.domain.model

data class Attachment(
    val id: String,
    val attachmentType: String,
    val entityId: String,
    val ownerProjectId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
