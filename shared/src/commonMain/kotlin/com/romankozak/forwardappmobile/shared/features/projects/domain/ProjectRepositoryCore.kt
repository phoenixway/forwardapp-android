package com.romankozak.forwardappmobile.shared.features.projects.domain

import com.romankozak.forwardappmobile.shared.data.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.shared.data.database.models.ListItem
import com.romankozak.forwardappmobile.shared.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface ProjectRepositoryCore {

    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>>

    suspend fun toggleProjectManagement(
        projectId: String,
        isEnabled: Boolean,
    )

    suspend fun updateProjectStatus(
        projectId: String,
        newStatus: String,
        statusText: String?,
    )

    suspend fun addProjectComment(
        projectId: String,
        comment: String,
    )

    suspend fun updateProjectViewMode(
        projectId: String,
        viewMode: ProjectViewMode,
    )

    fun getProjectContentStream(projectId: String): Flow<List<ListItemContent>>

    suspend fun addProjectLinkToProject(
        targetProjectId: String,
        currentProjectId: String,
    ): String

    suspend fun moveListItems(
        itemIds: List<String>,
        targetProjectId: String,
    )

    suspend fun deleteListItems(
        projectId: String,
        itemIds: List<String>,
    )

    suspend fun restoreListItems(items: List<ListItem>)

    suspend fun updateListItemsOrder(items: List<ListItem>)

    suspend fun updateAttachmentOrders(
        projectId: String,
        updates: List<Pair<String, Long>>,
    )

    suspend fun doesLinkExist(
        entityId: String,
        projectId: String,
    ): Boolean

    suspend fun deleteLinkByEntityIdAndProjectId(
        entityId: String,
        projectId: String,
    )

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

    suspend fun searchGlobal(query: String): List<GlobalSearchResultItem>

    suspend fun moveProject(
        projectToMove: Project,
        newParentId: String?,
    )

    suspend fun addLinkItemToProjectFromLink(
        projectId: String,
        link: RelatedLink,
    ): String

    suspend fun findProjectIdsByTag(tag: String): List<String>

    suspend fun getProjectsByType(projectType: ProjectType): List<Project>

    suspend fun getProjectsByReservedGroup(reservedGroup: String): List<Project>

    suspend fun getAllProjects(): List<Project>

    suspend fun deleteItemByEntityId(entityId: String)

    suspend fun logProjectTimeSummaryForDate(
        projectId: String,
        dayToLog: Calendar,
    )

    suspend fun recalculateAndLogProjectTime(projectId: String)

    suspend fun calculateProjectTimeMetrics(projectId: String): ProjectTimeMetrics

    suspend fun cleanupDanglingListItems()

    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?>

    suspend fun updateProjectArtifact(artifact: ProjectArtifact)

    suspend fun createProjectArtifact(artifact: ProjectArtifact)

    suspend fun ensureChildProjectListItemsExist(projectId: String)
}