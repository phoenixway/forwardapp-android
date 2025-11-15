package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import android.net.Uri
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findDescendantsForDeletion
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.getDescendantIds
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class ProjectActionsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun addNewProject(
        id: String,
        parentId: String?,
        name: String,
        allProjects: List<Project>,
    ) = withContext(ioDispatcher) {
        if (name.isBlank()) return@withContext
        projectRepository.createProjectWithId(id, name, parentId)
        if (parentId != null) {
            val parentProject = allProjects.find { it.id == parentId }
            if (parentProject != null && !parentProject.isExpanded) {
                projectRepository.updateProject(parentProject.copy(isExpanded = true))
            }
        }
    }

    suspend fun onDeleteProjectConfirmed(
        project: Project,
        childMap: Map<String, List<Project>>,
    ) = withContext(ioDispatcher) {
        val projectsToDelete = findDescendantsForDeletion(project.id, childMap)
        projectRepository.deleteProjectsAndSubProjects(listOf(project) + projectsToDelete)
    }

    fun getMoveProjectRoute(
        project: Project,
        allProjects: List<Project>,
    ): String {
        val title = "Move '${project.name}'"
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
        val descendantIds = getDescendantIds(project.id, childMap).joinToString(",")
        val currentParentId = project.parentId ?: "root"
        val disabledIds = "${project.id}${if (descendantIds.isNotEmpty()) ",$descendantIds" else ""}"
        return "list_chooser_screen/$encodedTitle?currentParentId=$currentParentId&disabledIds=$disabledIds"
    }

    suspend fun onListChooserResult(
        newParentId: String?,
        projectBeingMovedId: String?,
        allProjects: List<Project>,
    ) = withContext(ioDispatcher) {
        val projectToMoveId = projectBeingMovedId ?: return@withContext
        val projectToMove = allProjects.find { it.id == projectToMoveId } ?: return@withContext
        val finalNewParentId = if (newParentId == "root") null else newParentId

        if (projectToMove.parentId == finalNewParentId) return@withContext

        val allowSystemMoves = settingsRepository.allowSystemProjectMovesFlow.first()
        projectRepository.moveProject(projectToMove, finalNewParentId, allowSystemMoves)

        if (finalNewParentId != null) {
            val parentProject = allProjects.find { it.id == finalNewParentId }
            if (parentProject != null && !parentProject.isExpanded) {
                projectRepository.updateProject(parentProject.copy(isExpanded = true))
            }
        }
    }

    suspend fun onProjectReorder(
        fromId: String,
        toId: String,
        position: DropPosition,
        isSearchActive: Boolean,
        allProjects: List<Project>,
    ) = withContext(ioDispatcher) {
        if (fromId == toId || isSearchActive) return@withContext

        val fromProject = allProjects.find { it.id == fromId }
        val toProject = allProjects.find { it.id == toId }

        if (fromProject == null || toProject == null || fromProject.parentId != toProject.parentId) {
            return@withContext
        }

        val parentId = fromProject.parentId
        val siblings = allProjects.filter { it.parentId == parentId }.sortedBy { it.order }
        val mutableSiblings = siblings.toMutableList()
        val fromIndex = mutableSiblings.indexOfFirst { it.id == fromId }
        val toIndex = mutableSiblings.indexOfFirst { it.id == toId }

        if (fromIndex == -1 || toIndex == -1) return@withContext

        val movedItem = mutableSiblings.removeAt(fromIndex)
        val insertionIndex =
            when {
                fromIndex < toIndex -> if (position == DropPosition.BEFORE) toIndex - 1 else toIndex
                else -> if (position == DropPosition.BEFORE) toIndex else toIndex + 1
            }
        val finalIndex = insertionIndex.coerceIn(0, mutableSiblings.size)
        mutableSiblings.add(finalIndex, movedItem)

        val projectsToUpdate =
            mutableSiblings.mapIndexed { index, project ->
                project.copy(order = index.toLong(), updatedAt = System.currentTimeMillis())
            }
        projectRepository.updateProjects(projectsToUpdate)
    }

    suspend fun collapseAllProjects(allProjects: List<Project>) = withContext(ioDispatcher) {
        val projectsToCollapse =
            allProjects
                .filter { it.isExpanded }
                .map { it.copy(isExpanded = false) }
        if (projectsToCollapse.isNotEmpty()) {
            projectRepository.updateProjects(projectsToCollapse)
        }
    }

    suspend fun onToggleExpanded(project: Project) = withContext(ioDispatcher) {
        projectRepository.updateProject(project.copy(isExpanded = !project.isExpanded))
    }

    suspend fun exportToFile() = withContext(ioDispatcher) { syncRepository.exportFullBackupToFile() }

    suspend fun onFullImportConfirmed(uri: Uri) =
        withContext(ioDispatcher) { syncRepository.importFullBackupFromFile(uri) }

    suspend fun onBottomNavExpandedChange(expanded: Boolean) =
        withContext(ioDispatcher) { settingsRepository.saveBottomNavExpanded(expanded) }
}
