package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот вкладення (нотатка, документ, чеклист тощо).
 */
data class AttachmentSnapshot(
    val id: String,
    val attachmentType: String,
    val entityId: String,
    val ownerContextId: String?,
    val roleCode: String?,
    val isSystem: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)