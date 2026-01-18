package com.romankozak.forwardappmobile.features.ai.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_events")
data class AiEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val timestamp: Long,
    val payload: String,
)
