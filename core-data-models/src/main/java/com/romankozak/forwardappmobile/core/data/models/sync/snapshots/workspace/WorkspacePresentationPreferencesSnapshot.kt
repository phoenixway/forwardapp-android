package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/**
 * Canonical Workspace presentation-preferences transport contract.
 *
 * syncedAt remains local-only and is never transported.
 */
data class WorkspacePresentationPreferencesSnapshot(
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("defaultViewModeName") val defaultViewModeName: String?,
    @SerializedName("isAttachmentsExpanded") val isAttachmentsExpanded: Boolean,
    @SerializedName("showCheckboxes") val showCheckboxes: Boolean,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
)

data class WorkspacePresentationPreferencesSyncVersion(
    val workspaceId: String,
    val version: Long,
)
