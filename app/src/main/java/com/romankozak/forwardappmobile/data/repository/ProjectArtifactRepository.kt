package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectArtifactDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectArtifactRepository @Inject constructor(
    private val projectArtifactDao: ProjectArtifactDao
) {
    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?> {
        return projectArtifactDao.getArtifactForProjectStream(projectId)
    }

    suspend fun updateProjectArtifact(artifact: ProjectArtifact) {
        projectArtifactDao.update(artifact)
    }

    suspend fun createProjectArtifact(artifact: ProjectArtifact) {
        projectArtifactDao.insert(artifact)
    }
}
