package com.romankozak.forwardappmobile.shared.core.models

/** Stable cross-client wire record. Unknown relationType is rejected by current validators. */
data class PlanningCommitmentRelationRecord(
    val id: String,
    val sourceCommitmentId: String,
    val targetCommitmentId: String,
    val relationType: String,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
