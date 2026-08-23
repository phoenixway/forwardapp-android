package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

/** Canonical wire DTO for the global ThemeDefinition authority. */
data class ThemeDefinitionSnapshot(
    val id: String,
    val title: String,
    val colorArgb: Long,
    val iconKey: String,
    val description: String,
    val carryForward: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

/** Canonical wire DTO for one ThemeDefinition materialized on one DayPlan. */
data class DayThemeSnapshot(
    val id: String,
    val themeId: String,
    val dayPlanId: String,
    val budgetPercent: Int,
    val order: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

/** One entity assignment inside a per-day canonical assignment document. */
data class DayThemeAssignmentSnapshot(
    val entityId: String,
    val dayThemeIds: List<String>,
)

/** Canonical assignment authority for one DayPlan. */
data class DayThemeAssignmentDocumentSnapshot(
    val dayPlanId: String,
    val assignments: List<DayThemeAssignmentSnapshot>,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)
