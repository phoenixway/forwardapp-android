package com.romankozak.forwardappmobile.core.data.models.entities.ai

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("isFavorite") val isFavorite: Boolean = false,
    @SerializedName("version") val version: Long = 0L,
    @SerializedName("isDeleted") val isDeleted: Boolean = false
)
