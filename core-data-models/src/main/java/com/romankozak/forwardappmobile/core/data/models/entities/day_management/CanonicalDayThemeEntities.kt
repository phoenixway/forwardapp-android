package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/** Global canonical ThemeDefinition persistence. */
@Entity(tableName = "theme_definitions")
data class ThemeDefinitionEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("colorArgb") val colorArgb: Long,
    @SerializedName("iconKey") val iconKey: String,
    @SerializedName("description") val description: String,
    @SerializedName("carryForward") val carryForward: Boolean,
    @SerializedName("archived") val archived: Boolean,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

/** One canonical ThemeDefinition materialized on one DayPlan. */
@Entity(
    tableName = "day_themes",
    indices = [
        Index("themeId"),
        Index("dayPlanId"),
        Index(value = ["dayPlanId", "themeId"], unique = true),
    ],
)
data class CanonicalDayThemeEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("themeId") val themeId: String,
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("budgetPercent") val budgetPercent: Int,
    @SerializedName("order") val order: Long,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

/**
 * Atomic per-day canonical assignment document.
 *
 * Assignment rows deliberately remain JSON here. The canonical wire contract is
 * atomic per day and does not require Room-level child normalization.
 */
@Entity(tableName = "day_theme_assignment_documents")
data class DayThemeAssignmentDocumentEntity(
    @PrimaryKey @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("assignmentsJson") val assignmentsJson: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)
