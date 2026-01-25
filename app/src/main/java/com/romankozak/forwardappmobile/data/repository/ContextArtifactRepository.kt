package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.models.ContextArtifact
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextArtifactDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextArtifactRepository @Inject constructor(
    private val contextArtifactDao: ContextArtifactDao
) {
    fun getProjectArtifactStream(projectId: String): Flow<ContextArtifact?> {
        return contextArtifactDao.getArtifactForProjectStream(projectId)
    }

    suspend fun updateProjectArtifact(artifact: ContextArtifact) {
        contextArtifactDao.update(artifact)
    }

    suspend fun createProjectArtifact(artifact: ContextArtifact) {
        contextArtifactDao.insert(artifact)
    }
}
