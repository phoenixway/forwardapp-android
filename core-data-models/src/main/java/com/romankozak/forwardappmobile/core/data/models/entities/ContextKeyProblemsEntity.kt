package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_key_problems",
    indices = [Index(value = ["updated_at"])],
)
data class ContextKeyProblemsEntity(
    @PrimaryKey
    @ColumnInfo(name = "context_id")
    val contextId: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
