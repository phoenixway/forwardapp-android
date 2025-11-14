package com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectArtifactRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ProjectArtifactRepository {

    override fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?> =
        database.projectArtifactsQueries.getArtifactForProject(projectId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.toDomain() }

    override suspend fun createProjectArtifact(artifact: ProjectArtifact) = withContext(dispatcher) {
        upsertProjectArtifact(artifact)
    }

    override suspend fun updateProjectArtifact(artifact: ProjectArtifact) = withContext(dispatcher) {
        upsertProjectArtifact(artifact)
    }

    override suspend fun deleteProjectArtifact(artifactId: String) = withContext(dispatcher) {
        database.projectArtifactsQueries.deleteProjectArtifact(artifactId)
    }

    private fun upsertProjectArtifact(artifact: ProjectArtifact) {
        database.projectArtifactsQueries.insertProjectArtifact(
            id = artifact.id,
            projectId = artifact.projectId,
            content = artifact.content,
            createdAt = artifact.createdAt,
            updatedAt = artifact.updatedAt,
        )
    }
}
