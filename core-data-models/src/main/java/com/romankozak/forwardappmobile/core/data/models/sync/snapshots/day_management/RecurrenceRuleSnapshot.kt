package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName

data class RecurrenceRuleSnapshot(
    @SerializedName("frequency") val frequency: String,
    @SerializedName("interval") val interval: Int,
    @SerializedName("daysOfWeek") val daysOfWeek: List<String>?,
)