package com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import kotlinx.coroutines.flow.Flow

interface ProjectArtifactRepository {

    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?>

    suspend fun createProjectArtifact(artifact: ProjectArtifact)

    suspend fun updateProjectArtifact(artifact: ProjectArtifact)

    suspend fun deleteProjectArtifact(artifactId: String)
}
