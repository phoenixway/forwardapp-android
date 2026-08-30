package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/**
 * Canonical KEY_PROBLEMS wire rows.
 *
 * syncedAt is device-local transport state and is intentionally absent.
 */
data class WorkspaceProblemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("capabilityInstanceId") val capabilityInstanceId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceProblemWorkspaceRefSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("problemId") val problemId: String,
    @SerializedName("targetWorkspaceId") val targetWorkspaceId: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceProblemAttachmentRefSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("problemId") val problemId: String,
    @SerializedName("attachmentId") val attachmentId: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceProblemSyncVersion(
    val id: String,
    val version: Long,
)

data class WorkspaceProblemWorkspaceRefSyncVersion(
    val id: String,
    val version: Long,
)

data class WorkspaceProblemAttachmentRefSyncVersion(
    val id: String,
    val version: Long,
)
