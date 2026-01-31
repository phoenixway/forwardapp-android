package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai

import com.google.gson.annotations.SerializedName

data class AiInsightSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("isFavorite") val isFavorite: Boolean
)
