package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.models.ContextArtifact
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectArtifactDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectArtifactRepository @Inject constructor(
    private val projectArtifactDao: ProjectArtifactDao
) {
    fun getProjectArtifactStream(projectId: String): Flow<ContextArtifact?> {
        return projectArtifactDao.getArtifactForProjectStream(projectId)
    }

    suspend fun updateProjectArtifact(artifact: ContextArtifact) {
        projectArtifactDao.update(artifact)
    }

    suspend fun createProjectArtifact(artifact: ContextArtifact) {
        projectArtifactDao.insert(artifact)
    }
}
