package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments

import com.google.gson.annotations.SerializedName

data class AttachmentSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("attachmentType") val attachmentType: String,
    @SerializedName("ownerContextId") val ownerContextId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)
