package com.romankozak.forwardappmobile.features.projects.data.artifacts

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Project_artifacts
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ProjectArtifactRepository(
    private val database: ForwardAppDatabase,
    private val queryContext: CoroutineContext = EmptyCoroutineContext,
) {

    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?> =
        database.projectArtifactsQueries
            .getArtifactForProject(projectId)
            .asFlow()
            .mapToOneOrNull(queryContext)
            .map { row -> row?.toModel() }

    suspend fun updateProjectArtifact(artifact: ProjectArtifact) {
        withContext(queryContext) { upsertProjectArtifact(artifact) }
    }

    suspend fun createProjectArtifact(artifact: ProjectArtifact) {
        withContext(queryContext) { upsertProjectArtifact(artifact) }
    }

    suspend fun deleteProjectArtifact(artifactId: String) {
        withContext(queryContext) { database.projectArtifactsQueries.deleteProjectArtifact(artifactId) }
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

    private fun Project_artifacts.toModel(): ProjectArtifact =
        ProjectArtifact(
            id = id,
            projectId = projectId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}