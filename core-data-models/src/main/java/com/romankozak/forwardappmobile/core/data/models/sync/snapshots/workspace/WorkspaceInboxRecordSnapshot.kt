package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/** Canonical INBOX wire row. syncedAt is device-local transport state. */
data class WorkspaceInboxRecordSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("capabilityInstanceId") val capabilityInstanceId: String,
    @SerializedName("text") val text: String,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceInboxRecordSyncVersion(
    val id: String,
    val version: Long,
)
