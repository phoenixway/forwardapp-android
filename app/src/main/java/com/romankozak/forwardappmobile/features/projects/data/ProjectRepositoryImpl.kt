package com.romankozak.forwardappmobile.features.projects.data

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectLocalDataSource: ProjectLocalDataSource,
) : ProjectRepositoryCore {

    override fun getAllProjectsFlow(): Flow<List<Project>> =
        projectLocalDataSource
            .observeAll()
            .map { projects -> projects.map { it.withNormalizedParentId() } }

    override suspend fun getProjectById(id: String): Project? =
        projectLocalDataSource.getById(id)?.withNormalizedParentId()

    override fun getProjectByIdFlow(id: String): Flow<Project?> =
        projectLocalDataSource.observeById(id).map { project -> project?.withNormalizedParentId() }

    private fun Project.withNormalizedParentId(): Project {
        val normalizedParentId =
            parentId
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

        return if (normalizedParentId != parentId) {
            copy(parentId = normalizedParentId)
        } else {
            this
        }
    }

    override suspend fun updateProject(project: Project) {
        projectLocalDataSource.upsert(project)
    }

    override suspend fun updateProjects(projects: List<Project>): Int {
        if (projects.isEmpty()) return 0
        projectLocalDataSource.upsert(projects)
        return projects.size
    }

    override suspend fun deleteProjectsAndSubProjects(projectsToDelete: List<Project>) {
        if (projectsToDelete.isEmpty()) return
        val projectIds = projectsToDelete.map { it.id }
        projectLocalDataSource.delete(projectIds)
    }

    override suspend fun createProjectWithId(
        id: String,
        name: String,
        parentId: String?,
    ) {
        val newProject =
            Project(
                id = id,
                name = name,
                parentId = parentId,
                description = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        projectLocalDataSource.upsert(newProject)
    }

    override suspend fun findProjectIdsByTag(tag: String): List<String> = projectLocalDataSource.getIdsByTag(tag)

    override suspend fun getProjectsByType(projectType: com.romankozak.forwardappmobile.shared.data.database.models.ProjectType): List<Project> = projectLocalDataSource.getByType(projectType.name)

    override suspend fun getProjectsByReservedGroup(reservedGroup: String): List<Project> = projectLocalDataSource.getByReservedGroup(reservedGroup)

    override suspend fun getAllProjects(): List<Project> = projectLocalDataSource.getAll()

    override suspend fun updateProjectViewMode(
        projectId: String,
        viewMode: ProjectViewMode,
    ) {
        projectLocalDataSource.updateDefaultViewMode(projectId, viewMode.name)
    }
}