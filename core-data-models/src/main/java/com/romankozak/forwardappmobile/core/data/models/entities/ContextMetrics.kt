package com.romankozak.forwardappmobile.core.data.models.entities

import com.google.gson.annotations.SerializedName

data class ContextTimeMetrics(
    @SerializedName("timeToday") val timeToday: Long,
    @SerializedName("timeTotal") val timeTotal: Long,
)
