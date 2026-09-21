package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy

import com.google.gson.annotations.SerializedName

/**
 * Canonical Hierarchy V2 wire row.
 *
 * Full backup/restore preserves syncedAt. Peer merge treats syncedAt as
 * receiver-local ACK metadata and resets accepted winners to null.
 */
data class HierarchyPlacementSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("hierarchyId") val hierarchyId: String,
    @SerializedName("targetType") val targetType: String,
    @SerializedName("targetId") val targetId: String,
    @SerializedName("parentPlacementId") val parentPlacementId: String?,
    @SerializedName("placementKind") val placementKind: String,
    @SerializedName("siblingOrder") val siblingOrder: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)

data class HierarchyPlacementSyncVersion(
    val id: String,
    val version: Long,
)
