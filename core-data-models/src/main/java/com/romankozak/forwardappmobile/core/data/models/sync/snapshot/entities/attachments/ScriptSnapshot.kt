package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments

import com.google.gson.annotations.SerializedName

data class ScriptSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)
