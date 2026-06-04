package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

data class ContextParentLinkSnapshot(
    @SerializedName("parentContextId") val parentContextId: String,
    @SerializedName("childContextId") val childContextId: String,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long?,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)
