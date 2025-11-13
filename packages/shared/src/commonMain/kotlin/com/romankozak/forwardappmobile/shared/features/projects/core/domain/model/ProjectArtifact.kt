package com.romankozak.forwardappmobile.shared.features.projects.core.domain.model

data class ProjectArtifact(
    val id: String,
    val projectId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
