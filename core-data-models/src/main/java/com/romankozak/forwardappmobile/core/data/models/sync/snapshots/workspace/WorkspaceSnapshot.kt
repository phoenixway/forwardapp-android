package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace

import com.google.gson.annotations.SerializedName

/**
 * Workspace transport contract.
 *
 * parentWorkspaceId/workspaceOrder are historical compatibility fields only.
 * Current producers emit them neutral. Raw pre-V2 Restore may still deserialize
 * historical values so H1 can be reconstructed before persistence.
 */
data class WorkspaceSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("nameOverride") val nameOverride: String?,
    @SerializedName("descriptionOverride") val descriptionOverride: String?,
    @SerializedName("parentWorkspaceId") val parentWorkspaceId: String?,
    @SerializedName("roleCode") val roleCode: String?,
    @SerializedName("workspaceOrder") val workspaceOrder: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
    @SerializedName("provenance") val provenance: String = "CONTEXT_BACKED",
    @SerializedName("sourceContextId")
    val sourceContextId: String? = if (provenance == "CONTEXT_BACKED") id else null,
)
