package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import com.romankozak.forwardappmobile.shared.features.projects.data.models.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: String): Flow<Project?>
}
