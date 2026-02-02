package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai

import com.google.gson.annotations.SerializedName

data class AiEventSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("payload") val payload: String
)
