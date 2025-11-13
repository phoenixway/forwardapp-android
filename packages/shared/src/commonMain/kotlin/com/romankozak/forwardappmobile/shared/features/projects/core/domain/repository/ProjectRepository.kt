package com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: String): Flow<Project?>
}
