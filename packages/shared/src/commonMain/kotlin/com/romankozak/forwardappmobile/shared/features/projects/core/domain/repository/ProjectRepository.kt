package com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: String): Flow<Project?>
    fun getChildProjects(parentId: String): Flow<List<Project>>
    fun searchProjects(query: String): Flow<List<Project>>
    suspend fun upsertProject(project: Project)
    suspend fun deleteProject(id: String)
    suspend fun updateProjectCompleted(projectId: String, isCompleted: Boolean)
}
