package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName

/** Canonical recurrence-v2 logical occurrence provenance on the sync wire. */
data class RecurrenceOriginSnapshot(
    @SerializedName("seriesId") val seriesId: String,
    @SerializedName("occurrenceDayKey") val occurrenceDayKey: String,
    @SerializedName("sourceSeriesVersion") val sourceSeriesVersion: Long,
)
