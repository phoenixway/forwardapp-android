package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/**
 * Canonical DIRECTION ordered-placement wire row.
 *
 * LEGACY_DIRECTION_ITEM rows are compatibility projections only.
 * CANONICAL_ONLY rows are canonical persistence authority.
 * syncedAt is transport-local state and is intentionally absent.
 */
data class WorkspaceDirectionEntrySnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("capabilityInstanceId") val capabilityInstanceId: String,
    @SerializedName("orientationId") val orientationId: String?,
    @SerializedName("targetWorkspaceId") val targetWorkspaceId: String?,
    @SerializedName("labelOverride") val labelOverride: String?,
    @SerializedName("entryOrder") val entryOrder: Long,
    @SerializedName("provenance") val provenance: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class WorkspaceDirectionEntrySyncVersion(
    val id: String,
    val version: Long,
)
