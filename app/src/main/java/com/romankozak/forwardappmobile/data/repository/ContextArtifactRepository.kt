package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextArtifactDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextArtifactRepository
    @Inject
    constructor(
        private val contextArtifactDao: ContextArtifactDao,
    ) {
        fun getContextArtifactStream(contextId: String): Flow<ContextArtifact?> {
            return contextArtifactDao.getArtifactForContextStream(contextId)
        }

        suspend fun updateContextArtifact(artifact: ContextArtifact) {
            contextArtifactDao.insert(artifact)
        }

        suspend fun createContextArtifact(artifact: ContextArtifact) {
            contextArtifactDao.insert(artifact)
        }
    }
