package com.romankozak.forwardappmobile.core.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_artifacts",
    indices = [Index(value = ["contextId"])],
)
data class ContextArtifact(
    @PrimaryKey val id: String,
    val contextId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)