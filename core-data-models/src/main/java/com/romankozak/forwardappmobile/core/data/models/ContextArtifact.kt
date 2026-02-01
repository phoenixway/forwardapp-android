package com.romankozak.forwardappmobile.core.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "context_artifacts",
    indices = [Index(value = ["contextId"])],
)
data class ContextArtifact(
    @PrimaryKey val id: String,
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String = "",
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)