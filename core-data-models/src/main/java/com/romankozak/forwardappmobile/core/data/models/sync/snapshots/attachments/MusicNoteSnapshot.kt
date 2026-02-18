package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments

import com.google.gson.annotations.SerializedName

data class MusicNoteSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("contextId") val contextId: String?,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)
