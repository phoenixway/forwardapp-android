package com.romankozak.forwardappmobile.shared.features.projects.domain

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import kotlinx.coroutines.flow.Flow

interface ProjectRepositoryCore {

    fun getAllProjectsFlow(): Flow<List<Project>>

    suspend fun getProjectById(id: String): Project?

    fun getProjectByIdFlow(id: String): Flow<Project?>

    suspend fun updateProject(project: Project)

    suspend fun updateProjects(projects: List<Project>): Int

    suspend fun deleteProjectsAndSubProjects(projectsToDelete: List<Project>)

    suspend fun createProjectWithId(
        id: String,
        name: String,
        parentId: String?,
    )

    suspend fun findProjectIdsByTag(tag: String): List<String>

    suspend fun getProjectsByType(projectType: ProjectType): List<Project>

    suspend fun getProjectsByReservedGroup(reservedGroup: String): List<Project>

    suspend fun getAllProjects(): List<Project>

    suspend fun updateProjectViewMode(
        projectId: String,
        viewMode: ProjectViewMode,
    )
}
