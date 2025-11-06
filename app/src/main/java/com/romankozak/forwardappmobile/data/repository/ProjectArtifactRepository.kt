package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.projects.data.artifacts.ProjectArtifactRepository as SharedProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.database.ProjectArtifactQueriesQueries
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectArtifactRepository
@Inject
constructor(
    projectArtifactQueries: ProjectArtifactQueriesQueries,
    ioDispatcher: CoroutineDispatcher,
) {
    private val delegate = SharedProjectArtifactRepository(projectArtifactQueries, ioDispatcher)

    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?> =
        delegate.getProjectArtifactStream(projectId)

    suspend fun updateProjectArtifact(artifact: ProjectArtifact) {
        delegate.updateProjectArtifact(artifact)
    }

    suspend fun createProjectArtifact(artifact: ProjectArtifact) {
        delegate.createProjectArtifact(artifact)
    }

    suspend fun deleteProjectArtifact(artifactId: String) {
        delegate.deleteProjectArtifact(artifactId)
    }
}
