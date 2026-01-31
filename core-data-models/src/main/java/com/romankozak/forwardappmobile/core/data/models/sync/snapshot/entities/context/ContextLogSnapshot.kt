package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

import com.google.gson.annotations.SerializedName

data class ContextLogSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("details") val details: String?,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)
