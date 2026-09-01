package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/** Canonical BACKLOG placement row. Target content remains externally owned. */
data class WorkspaceBacklogEntrySnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("capabilityInstanceId") val capabilityInstanceId: String,
    @SerializedName("targetKind") val targetKind: String,
    @SerializedName("targetId") val targetId: String,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceBacklogEntrySyncVersion(
    val id: String,
    val version: Long,
)
