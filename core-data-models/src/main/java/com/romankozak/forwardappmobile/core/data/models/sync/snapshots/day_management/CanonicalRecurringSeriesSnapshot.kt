package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Canonical recurrence-v2 wire representation.
 *
 * This is deliberately independent from both the legacy RecurringTask model
 * and Android Room persistence details such as templateJson.
 */
data class CanonicalRecurrenceRuleSnapshot(
    @SerializedName("frequency") val frequency: String,
    @SerializedName("interval") val interval: Int,
    @SerializedName("daysOfWeek") val daysOfWeek: List<String>?,
)

data class CanonicalRecurringSeriesSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("rule") val rule: CanonicalRecurrenceRuleSnapshot,
    @SerializedName("startDayKey") val startDayKey: String,
    @SerializedName("endDayKey") val endDayKey: String?,
    @SerializedName("template") val template: JsonElement,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)
