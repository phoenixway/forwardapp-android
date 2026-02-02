package com.romankozak.forwardappmobile.core.data.models.entities.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey val id: String,
    val text: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val version: Long = 0L,
    val isDeleted: Boolean = false
)
