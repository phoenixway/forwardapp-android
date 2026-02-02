package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments

/**
 * Снапшот зв'язку між контекстом та вкладенням.
 */
data class ContextAttachmentCrossRefSnapshot(
    val contextId: String,
    val attachmentId: String,
    val attachmentOrder: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)