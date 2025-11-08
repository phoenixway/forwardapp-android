package com.romankozak.forwardappmobile.shared.features.projects.domain

import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import kotlinx.coroutines.flow.Flow

/**
 * KMP-інтерфейс — його може використовувати і Android, і iOS.
 * Не містить залежностей від SQLDelight, Android чи CoroutineDispatcher.
 */
interface ProjectArtifactRepository {

    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?>

    suspend fun updateProjectArtifact(artifact: ProjectArtifact)

    suspend fun createProjectArtifact(artifact: ProjectArtifact)

    suspend fun deleteProjectArtifact(artifactId: String)
}

