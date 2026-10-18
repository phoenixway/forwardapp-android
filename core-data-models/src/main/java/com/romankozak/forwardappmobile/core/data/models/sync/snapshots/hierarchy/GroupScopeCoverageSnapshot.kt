package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy

/** Compatibility provenance, not H1 topology. Missing envelope means pre-coverage payload. */
data class GroupScopeCoverageSnapshot(
    val formatVersion: Int = 1,
    val records: List<GroupScopeApplicabilityRecord> = emptyList(),
)

data class GroupScopeApplicabilityRecord(
    val placementId: String,
    val hierarchyId: String,
    val mode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
)

data class GroupScopeCoverageVersion(val placementId: String, val version: Long)
