package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_artifacts")
data class ProjectArtifact(
    @PrimaryKey val id: String,
    val projectId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
