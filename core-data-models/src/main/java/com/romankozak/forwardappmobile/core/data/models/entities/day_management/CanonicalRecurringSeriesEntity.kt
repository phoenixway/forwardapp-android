package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Android persistence representation of the canonical recurrence-v2 series.
 *
 * This table is deliberately independent from the legacy RecurringTask table.
 * Scheduling fields stay queryable while the kind-specific template is stored
 * as an opaque platform-encoded JSON payload.
 */
@Entity(
    tableName = "canonical_recurring_series",
    indices = [
        Index("kind"),
        Index("startDayKey"),
        Index("endDayKey"),
    ],
)
data class CanonicalRecurringSeriesEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("ruleFrequency") val ruleFrequency: String,
    @SerializedName("ruleInterval") val ruleInterval: Int,
    @SerializedName("ruleDaysOfWeekCsv") val ruleDaysOfWeekCsv: String? = null,
    @SerializedName("startDayKey") val startDayKey: String,
    @SerializedName("endDayKey") val endDayKey: String? = null,
    @SerializedName("templateJson") val templateJson: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long,
)
