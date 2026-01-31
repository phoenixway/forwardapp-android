package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments

import com.google.gson.annotations.SerializedName

data class NoteDocumentItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("documentId") val documentId: String,
    @SerializedName("content") val content: String,
    @SerializedName("order") val order: Long,
    @SerializedName("type") val type: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)
