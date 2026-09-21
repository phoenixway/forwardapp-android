package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy

import com.google.gson.annotations.SerializedName

/**
 * Wire row for occurrence-scoped Beacon Group presentation provenance.
 *
 * A live row with null groupSubjectId is explicit NoGroup. A missing live row
 * must never be silently interpreted as NoGroup by an authoritative V2 reader.
 */
data class HierarchyPlacementGroupScopeSnapshot(
    @SerializedName("placementId") val placementId: String,
    @SerializedName("hierarchyId") val hierarchyId: String,
    @SerializedName("groupSubjectId") val groupSubjectId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)

data class HierarchyPlacementGroupScopeSyncVersion(
    val placementId: String,
    val version: Long,
)
