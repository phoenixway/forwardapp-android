package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Canonical EXECUTION_LOG wire row.
 *
 * Workspace owns the row. Legacy Context identity is intentionally absent.
 */
data class CanonicalExecutionLogSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("workspaceId") val workspaceId: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("details") val details: String?,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)
