package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning

import com.google.gson.annotations.SerializedName

/**
 * Canonical Planning V3 scope transport row.
 *
 * syncedAt is sender-local delivery metadata. Full backup/restore preserves it,
 * while an accepted peer-merge winner resets it on the receiving device.
 */
data class PlanningScopeSnapshot(
    @SerializedName("id")
    val id: String,
    @SerializedName("kind")
    val kind: String,
    @SerializedName("lifecycle")
    val lifecycle: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("startsAt")
    val startsAt: Long?,
    @SerializedName("endsAt")
    val endsAt: Long?,
    @SerializedName("createdAt")
    val createdAt: Long,
    @SerializedName("updatedAt")
    val updatedAt: Long?,
    @SerializedName("syncedAt")
    val syncedAt: Long?,
    @SerializedName("version")
    val version: Long,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
)

/**
 * Canonical Planning V3 commitment transport row.
 *
 * Stable enum/role/provenance values travel as explicit wire strings so the
 * transport layer does not become a second domain authority.
 */
data class PlanningCommitmentSnapshot(
    @SerializedName("id")
    val id: String,
    @SerializedName("scopeId")
    val scopeId: String,
    @SerializedName("orientationId")
    val orientationId: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("order")
    val order: Long,
    @SerializedName("priorityLevel")
    val priorityLevel: String?,
    @SerializedName("status")
    val status: String,
    @SerializedName("provenanceKind")
    val provenanceKind: String,
    @SerializedName("provenanceSourceType")
    val provenanceSourceType: String?,
    @SerializedName("provenanceSourceId")
    val provenanceSourceId: String?,
    @SerializedName("createdAt")
    val createdAt: Long,
    @SerializedName("updatedAt")
    val updatedAt: Long?,
    @SerializedName("syncedAt")
    val syncedAt: Long?,
    @SerializedName("version")
    val version: Long,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
)

/** Typed Day-only extension of a canonical DAY PlanningScope. */
data class DayPlanningScopeExtensionSnapshot(
    @SerializedName("scopeId")
    val scopeId: String,
    @SerializedName("calendarDayStartMillis")
    val calendarDayStartMillis: Long,
    @SerializedName("outcome")
    val outcome: String?,
    @SerializedName("plannedBudgetMinutes")
    val plannedBudgetMinutes: Long?,
    @SerializedName("createdAt")
    val createdAt: Long,
    @SerializedName("updatedAt")
    val updatedAt: Long?,
    @SerializedName("syncedAt")
    val syncedAt: Long?,
    @SerializedName("version")
    val version: Long,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
)

/** Typed Tactical-cycle-only extension of a canonical TACTICAL_CYCLE PlanningScope. */
data class TacticalPlanningScopeExtensionSnapshot(
    @SerializedName("scopeId") val scopeId: String,
    @SerializedName("iterationType") val iterationType: String,
    @SerializedName("weekKey") val weekKey: String?,
    @SerializedName("closedAt") val closedAt: Long?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long?,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

/** Typed Strategic-arc-only extension of a canonical STRATEGIC_ARC PlanningScope. */
data class StrategicPlanningScopeExtensionSnapshot(
    @SerializedName("scopeId") val scopeId: String,
    @SerializedName("legacyArcKey") val legacyArcKey: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long?,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class PlanningScopeSyncVersion(
    val id: String,
    val version: Long,
)

data class PlanningCommitmentSyncVersion(
    val id: String,
    val version: Long,
)

data class DayPlanningScopeExtensionSyncVersion(
    val scopeId: String,
    val version: Long,
)

data class TacticalPlanningScopeExtensionSyncVersion(
    val scopeId: String,
    val version: Long,
)

data class StrategicPlanningScopeExtensionSyncVersion(
    val scopeId: String,
    val version: Long,
)
