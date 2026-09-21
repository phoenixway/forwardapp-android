package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy

import com.google.gson.annotations.SerializedName

/**
 * Wire row for exact Workspace linked-presentation provenance.
 *
 * A live row means true for that exact occurrence. Absence of a live row means
 * false. This state is deliberately independent of PlacementKind and is never
 * inherited by descendants.
 */
data class HierarchyPlacementLinkedAppearanceSnapshot(
    @SerializedName("placementId") val placementId: String,
    @SerializedName("hierarchyId") val hierarchyId: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)

data class HierarchyPlacementLinkedAppearanceSyncVersion(
    val placementId: String,
    val version: Long,
)
