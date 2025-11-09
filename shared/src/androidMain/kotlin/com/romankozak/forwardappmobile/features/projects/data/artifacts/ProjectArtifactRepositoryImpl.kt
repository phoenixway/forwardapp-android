package com.romankozak.forwardappmobile.features.projects.data.artifacts

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectArtifactRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ProjectArtifactRepository {

    private val queries = db.projectArtifactsQueries

    override fun getArtifactsForProject(projectId: String): Flow<List<ProjectArtifact>> {
        return queries.getArtifactForProject(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { artifacts -> artifacts.map { it.toDomain() } }
    }

    override suspend fun insertProjectArtifact(artifact: ProjectArtifact) {
        withContext(ioDispatcher) {
            queries.insertProjectArtifact(
                id = artifact.id,
                projectId = artifact.projectId,
                content = artifact.content,
                createdAt = artifact.createdAt,
                updatedAt = artifact.updatedAt
            )
        }
    }

    override suspend fun deleteProjectArtifact(id: String) {
        withContext(ioDispatcher) {
            queries.deleteProjectArtifact(id)
        }
    }
}