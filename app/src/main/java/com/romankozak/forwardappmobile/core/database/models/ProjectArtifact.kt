package com.romankozak.forwardappmobile.core.database.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_artifacts",
    indices = [Index(value = ["projectId"])]
)
data class ProjectArtifact(
    @PrimaryKey val id: String,
    val projectId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
