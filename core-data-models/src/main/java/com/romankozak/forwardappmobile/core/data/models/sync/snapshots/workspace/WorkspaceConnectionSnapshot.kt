package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/** Canonical CONNECTIONS placement row. Attachment content stays external. */
data class WorkspaceConnectionSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("capabilityInstanceId") val capabilityInstanceId: String,
    @SerializedName("attachmentId") val attachmentId: String,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceConnectionSyncVersion(
    val id: String,
    val version: Long,
)
