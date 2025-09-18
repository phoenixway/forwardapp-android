package com.romankozak.forwardappmobile.ui.screens.mainscreen.actions

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DialogState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findDescendantsForDeletion
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.getDescendantIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Обробляє дії користувача над проектами
 */
class ProjectActionsHandler(
    private val projectRepository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val viewModelScope: CoroutineScope,
    private val allProjectsFlat: StateFlow<List<Project>>,
    private val uiEventChannel: Channel<ProjectUiEvent>,
    private val dialogState: MutableStateFlow<DialogState>
) {

    fun addNewProject(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            projectRepository.createProjectWithId(id, name, parentId)
            if (parentId != null) {
                val parentProject = allProjectsFlat.value.find { it.id == parentId }
                if (parentProject != null && !parentProject.isExpanded) {
                    projectRepository.updateProject(parentProject.copy(isExpanded = true))
                }
            }
        }
    }

    fun onDeleteProjectConfirmed(project: Project, childMap: Map<String, List<Project>>) {
        viewModelScope.launch {
            val projectsToDelete = findDescendantsForDeletion(project.id, childMap)
            projectRepository.deleteProjectsAndSubProjects(listOf(project) + projectsToDelete)
            dialogState.value = DialogState.Hidden
        }
    }

    fun onMoveProjectRequest(project: Project, savedStateHandle: SavedStateHandle) {
        dialogState.value = DialogState.Hidden
        savedStateHandle["projectBeingMovedId"] = project.id
        viewModelScope.launch {
            val title = "Перемістити '${project.name}'"
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val allProjects = allProjectsFlat.first()
            val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            val descendantIds = getDescendantIds(project.id, childMap).joinToString(",")
            val currentParentId = project.parentId ?: "root"
            val disabledIds = "${project.id}${if (descendantIds.isNotEmpty()) ",$descendantIds" else ""}"
            val route = "project_chooser_screen/$encodedTitle?currentParentId=$currentParentId&disabledIds=$disabledIds"
            uiEventChannel.send(ProjectUiEvent.Navigate(route))
        }
    }

    fun onListChooserResult(
        newParentId: String?,
        projectBeingMovedId: StateFlow<String?>,
        savedStateHandle: SavedStateHandle
    ) {
        viewModelScope.launch {
            val projectToMoveId = projectBeingMovedId.value ?: return@launch
            val projectToMove = allProjectsFlat.value.find { it.id == projectToMoveId } ?: return@launch
            val finalNewParentId = if (newParentId == "root") null else newParentId

            if (projectToMove.parentId == finalNewParentId) {
                savedStateHandle["projectBeingMovedId"] = null
                return@launch
            }

            projectRepository.moveProject(projectToMove, finalNewParentId)

            if (finalNewParentId != null) {
                val parentProject = allProjectsFlat.value.find { it.id == finalNewParentId }
                if (parentProject != null && !parentProject.isExpanded) {
                    projectRepository.updateProject(parentProject.copy(isExpanded = true))
                }
            }
            savedStateHandle["projectBeingMovedId"] = null
        }
    }

    fun onProjectReorder(fromId: String, toId: String, position: DropPosition, isSearchActive: Boolean) {
        if (fromId == toId || isSearchActive) return
        viewModelScope.launch(Dispatchers.IO) {
            val allProjects = allProjectsFlat.first()
            val fromProject = allProjects.find { it.id == fromId }
            val toProject = allProjects.find { it.id == toId }

            if (fromProject == null || toProject == null || fromProject.parentId != toProject.parentId) return@launch

            val parentId = fromProject.parentId
            val siblings = allProjects.filter { it.parentId == parentId }.sortedBy { it.order }
            val mutableSiblings = siblings.toMutableList()
            val fromIndex = mutableSiblings.indexOfFirst { it.id == fromId }
            val toIndex = mutableSiblings.indexOfFirst { it.id == toId }

            if (fromIndex == -1 || toIndex == -1) return@launch

            val movedItem = mutableSiblings.removeAt(fromIndex)
            val insertionIndex = when {
                fromIndex < toIndex -> if (position == DropPosition.BEFORE) toIndex - 1 else toIndex
                else -> if (position == DropPosition.BEFORE) toIndex else toIndex + 1
            }
            val finalIndex = insertionIndex.coerceIn(0, mutableSiblings.size)
            mutableSiblings.add(finalIndex, movedItem)

            val projectsToUpdate = mutableSiblings.mapIndexed { index, project ->
                project.copy(order = index.toLong(), updatedAt = System.currentTimeMillis())
            }
            projectRepository.updateProjects(projectsToUpdate)
        }
    }

    fun collapseAllProjects() {
        viewModelScope.launch {
            val projectsToCollapse = allProjectsFlat.value
                .filter { it.isExpanded }
                .map { it.copy(isExpanded = false) }
            if (projectsToCollapse.isNotEmpty()) {
                projectRepository.updateProjects(projectsToCollapse)
            }
        }
    }

    fun onToggleExpanded(project: Project) {
        viewModelScope.launch {
            projectRepository.updateProject(project.copy(isExpanded = !project.isExpanded))
        }
    }

    fun exportToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepository.exportFullBackupToFile()
            result.onSuccess { message ->
                uiEventChannel.send(ProjectUiEvent.ShowToast(message))
            }.onFailure { error ->
                uiEventChannel.send(ProjectUiEvent.ShowToast("Export error: ${error.message}"))
            }
        }
    }

    fun onFullImportConfirmed(uri: Uri) {
        dialogState.value = DialogState.Hidden
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepository.importFullBackupFromFile(uri)
            withContext(Dispatchers.Main) {
                result.onSuccess { message ->
                    uiEventChannel.send(ProjectUiEvent.ShowToast(message))
                }.onFailure { error ->
                    uiEventChannel.send(ProjectUiEvent.ShowToast("Import error: ${error.message}"))
                }
            }
        }
    }

    fun onBottomNavExpandedChange(expanded: Boolean, currentExpanded: Boolean) {
        if (currentExpanded == expanded) return
        viewModelScope.launch {
            settingsRepository.saveBottomNavExpanded(expanded)
        }
    }
}