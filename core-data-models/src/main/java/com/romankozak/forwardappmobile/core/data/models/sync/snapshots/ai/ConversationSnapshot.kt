package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai

import com.google.gson.annotations.SerializedName

data class ConversationSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("folderId") val folderId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)