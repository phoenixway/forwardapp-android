package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName

/** Canonical wire DTO for the global ThemeDefinition authority. */
data class ThemeDefinitionSnapshot(
    @SerializedName(value = "id", alternate = ["a"]) val id: String,
    @SerializedName(value = "title", alternate = ["b"]) val title: String,
    @SerializedName(value = "colorArgb", alternate = ["c"]) val colorArgb: Long,
    @SerializedName(value = "iconKey", alternate = ["d"]) val iconKey: String,
    @SerializedName(value = "description", alternate = ["e"]) val description: String,
    @SerializedName(value = "carryForward", alternate = ["f"]) val carryForward: Boolean,
    @SerializedName(value = "archived", alternate = ["g"]) val archived: Boolean,
    @SerializedName(value = "createdAt", alternate = ["h"]) val createdAt: Long,
    @SerializedName(value = "updatedAt", alternate = ["i"]) val updatedAt: Long,
    @SerializedName(value = "syncedAt", alternate = ["j"]) val syncedAt: Long?,
    @SerializedName(value = "version", alternate = ["k"]) val version: Long,
    @SerializedName(value = "isDeleted", alternate = ["l"]) val isDeleted: Boolean,
)

/** Canonical wire DTO for one ThemeDefinition materialized on one DayPlan. */
data class DayThemeSnapshot(
    @SerializedName(value = "id", alternate = ["a"]) val id: String,
    @SerializedName(value = "themeId", alternate = ["b"]) val themeId: String,
    @SerializedName(value = "dayPlanId", alternate = ["c"]) val dayPlanId: String,
    @SerializedName(value = "budgetPercent", alternate = ["d"]) val budgetPercent: Int,
    @SerializedName(value = "order", alternate = ["e"]) val order: Long,
    @SerializedName(value = "isActive", alternate = ["f"]) val isActive: Boolean,
    @SerializedName(value = "createdAt", alternate = ["g"]) val createdAt: Long,
    @SerializedName(value = "updatedAt", alternate = ["h"]) val updatedAt: Long,
    @SerializedName(value = "syncedAt", alternate = ["i"]) val syncedAt: Long?,
    @SerializedName(value = "version", alternate = ["j"]) val version: Long,
    @SerializedName(value = "isDeleted", alternate = ["k"]) val isDeleted: Boolean,
)

/** One entity assignment inside a per-day canonical assignment document. */
data class DayThemeAssignmentSnapshot(
    @SerializedName(value = "entityId", alternate = ["a"]) val entityId: String,
    @SerializedName(value = "dayThemeIds", alternate = ["b"]) val dayThemeIds: List<String>,
)

/** Canonical assignment authority for one DayPlan. */
data class DayThemeAssignmentDocumentSnapshot(
    @SerializedName(value = "dayPlanId", alternate = ["a"]) val dayPlanId: String,
    @SerializedName(value = "assignments", alternate = ["b"]) val assignments: List<DayThemeAssignmentSnapshot>,
    @SerializedName(value = "createdAt", alternate = ["c"]) val createdAt: Long,
    @SerializedName(value = "updatedAt", alternate = ["d"]) val updatedAt: Long,
    @SerializedName(value = "syncedAt", alternate = ["e"]) val syncedAt: Long?,
    @SerializedName(value = "version", alternate = ["f"]) val version: Long,
    @SerializedName(value = "isDeleted", alternate = ["g"]) val isDeleted: Boolean,
)
