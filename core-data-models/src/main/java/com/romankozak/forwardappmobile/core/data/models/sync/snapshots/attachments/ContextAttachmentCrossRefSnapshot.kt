package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments

import com.google.gson.annotations.SerializedName

/**
 * Снапшот зв'язку між контекстом та вкладенням.
 */
data class ContextAttachmentCrossRefSnapshot(
    @SerializedName("contextId") val contextId: String,
    @SerializedName("attachmentId") val attachmentId: String,
    @SerializedName("attachmentOrder") val attachmentOrder: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)