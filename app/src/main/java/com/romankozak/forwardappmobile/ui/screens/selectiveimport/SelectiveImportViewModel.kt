package com.romankozak.forwardappmobile.ui.screens.selectiveimport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectiveImportViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    savedStateHandle: SavedStateHandle,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectiveImportState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SelectiveImportEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    init {
        val uri = savedStateHandle.get<String>("fileUri")
        android.util.Log.d("IMPORT_SELECTIVE_INIT", "Init called, fileUri from SavedStateHandle: $uri")
        loadBackupFile(uri)
    }

    private fun loadBackupFile(fileUriString: String?) {
        viewModelScope.launch {
            android.util.Log.d("IMPORT_SELECTIVE", "loadBackupFile called with: $fileUriString")
            _uiState.update { it.copy(isLoading = true) }

            if (fileUriString == null) {
                android.util.Log.d("IMPORT_SELECTIVE", "File URI is null!")
                _uiState.update { it.copy(isLoading = false, error = "File URI not provided.") }
                return@launch
            }

            val fileUri = Uri.parse(fileUriString)
            android.util.Log.d("IMPORT_SELECTIVE", "Loading backup from URI: $fileUri")

            syncRepository.parseBackupFile(fileUri)
                .onSuccess { fullAppBackup ->
                    android.util.Log.d("IMPORT_SELECTIVE", "Backup parsed. Database is null: ${fullAppBackup.database == null}")
                    android.util.Log.d("IMPORT_SELECTIVE", "Database projects: ${fullAppBackup.database?.projects?.size ?: "N/A"}")
                    android.util.Log.d("IMPORT_SELECTIVE", "Database attachments: ${fullAppBackup.database?.attachments?.size ?: "N/A"}")
                    android.util.Log.d("IMPORT_SELECTIVE", "Database projectAttachmentCrossRefs: ${fullAppBackup.database?.projectAttachmentCrossRefs?.size ?: "N/A"}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupContent = fullAppBackup.database?.toSelectable() ?: SelectableDatabaseContent()
                        )
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("IMPORT_SELECTIVE", "Failed to parse backup", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to parse backup file: ${error.message}"
                        )
                    }
                }
        }
    }

    fun onImportClicked() {
         viewModelScope.launch {
             val currentState = _uiState.value
             val contentToImport = currentState.backupContent ?: return@launch

             val selectedProjects = contentToImport.projects.filter { it.isSelected }.map { it.item }
             val selectedGoals = contentToImport.goals.filter { it.isSelected }.map { it.item }
             val selectedThoughts = contentToImport.thoughts.filter { it.isSelected }.map { it.item }
             val selectedStats = contentToImport.stats.filter { it.isSelected }.map { it.item }
             
             android.util.Log.d("IMPORT_DEBUG", "Total projects selected: ${selectedProjects.size}")
             android.util.Log.d("IMPORT_DEBUG", "Projects with parents: ${selectedProjects.filter { it.parentId != null }.size}")
             android.util.Log.d("IMPORT_DEBUG", "Root projects (no parent): ${selectedProjects.filter { it.parentId == null }.size}")
             
             // Filter out system projects (those with systemKey) to prevent duplication
             val regularProjects = selectedProjects.filter { it.systemKey == null }
             val systemProjectsCount = selectedProjects.size - regularProjects.size
             android.util.Log.d("IMPORT_DEBUG", "Regular (non-system) projects: ${regularProjects.size}, System projects: ${systemProjectsCount}")
             
             // Get all available projects from backup to check for parent references
             val allBackupProjects = contentToImport.projects.map { it.item }
             val selectedProjectIds = selectedProjects.map { it.id }.toSet()
             val regularProjectIds = regularProjects.map { it.id }.toSet()
             
             // Build map of all projects by ID to check their system status
             val allProjectsMap = allBackupProjects.associateBy { it.id }
             
             // Recursively check if a project's entire parent chain is valid
             // A project is valid if:
             // 1. It has no parent (root level), OR
             // 2. Its parent is a system project (exists in DB), OR
             // 3. Its parent is a selected regular project whose parent is also valid
             fun isProjectValidForImport(projectId: String, visited: Set<String> = emptySet()): Boolean {
                if (projectId in visited) return false // Circular reference protection
                val project = allProjectsMap[projectId] ?: return false
                
                if (project.parentId == null) return true // Root level is valid
                
                val parentProject = allProjectsMap[project.parentId] ?: return false
                
                // Parent must be either a system project or a selected regular project
                val isParentValid = if (parentProject.systemKey != null) {
                    // System projects already exist in DB, so they're valid
                    true
                } else {
                    // Regular projects must be selected and have valid parents recursively
                    project.parentId in regularProjectIds && isProjectValidForImport(project.parentId, visited + projectId)
                }
                
                return isParentValid
             }
             
             val projectsWithValidParents = regularProjects.filter { project ->
                isProjectValidForImport(project.id)
             }
             
             // Don't import if nothing is selected
             if (selectedProjects.isEmpty() && selectedGoals.isEmpty() && selectedThoughts.isEmpty() && selectedStats.isEmpty()) {
                 _uiState.update { it.copy(error = "Нечого імпортувати. Виберіть проекти або цілі.") }
                 return@launch
             }

            // Filter list items to only those linked to selected projects or goals
             val selectedProjectIds = projectsWithValidParents.map { it.id }.toSet()
            val selectedGoalIds = selectedGoals.map { it.id }.toSet()
            val allListItems = currentState.backupContent?.allListItems ?: emptyList()
            val filteredListItems = allListItems.filter { 
                it.projectId in selectedProjectIds || it.entityId in selectedGoalIds 
            }

            // Include all attachments and their cross-refs since they should be available in the attachments library
            val allAttachments = currentState.backupContent?.allAttachments ?: emptyList()
            val allProjectAttachmentCrossRefs = currentState.backupContent?.allProjectAttachmentCrossRefs ?: emptyList()
            val filteredCrossRefs = allProjectAttachmentCrossRefs.filter { 
                it.projectId in selectedProjectIds 
            }
            


            // Include documents and checklists only if they belong to selected projects
            val allDocuments = currentState.backupContent?.allDocuments ?: emptyList()
            val filteredDocuments = allDocuments.filter { it.projectId in selectedProjectIds }
            
            val allDocumentItems = currentState.backupContent?.allDocumentItems ?: emptyList()
            val filteredDocumentIds = filteredDocuments.map { it.id }.toSet()
            val filteredDocumentItems = allDocumentItems.filter { it.listId in filteredDocumentIds }
            
            val allChecklists = currentState.backupContent?.allChecklists ?: emptyList()
            val filteredChecklists = allChecklists.filter { it.projectId in selectedProjectIds }
            
            val allChecklistItems = currentState.backupContent?.allChecklistItems ?: emptyList()
            val filteredChecklistIds = filteredChecklists.map { it.id }.toSet()
            val filteredChecklistItems = allChecklistItems.filter { it.checklistId in filteredChecklistIds }
            
            val allLinkItems = currentState.backupContent?.allLinkItems ?: emptyList()

            val databaseContent = DatabaseContent(
                projects = projectsWithValidParents,
                goals = selectedGoals,
                legacyNotes = selectedThoughts,
                activityRecords = selectedStats,
                listItems = filteredListItems,
                documents = filteredDocuments,
                documentItems = filteredDocumentItems,
                checklists = filteredChecklists,
                checklistItems = filteredChecklistItems,
                linkItemEntities = allLinkItems,
                inboxRecords = emptyList(),
                projectExecutionLogs = emptyList(),
                recentProjectEntries = emptyList(),
                attachments = allAttachments,
                projectAttachmentCrossRefs = filteredCrossRefs
            )

            _uiState.update { it.copy(isLoading = true) }

            val result = syncRepository.importSelectedData(databaseContent)

            _uiState.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                _eventChannel.send(SelectiveImportEvent.NavigateBack)
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Unknown import error") }
            }
        }
    }

    fun toggleProjectSelection(projectId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedProjects = currentState.backupContent?.projects?.map {
                if (it.item.id == projectId) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(projects = updatedProjects ?: emptyList()))
        }
    }

    fun toggleGoalSelection(goalId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedGoals = currentState.backupContent?.goals?.map {
                if (it.item.id == goalId) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(goals = updatedGoals ?: emptyList()))
        }
    }
    
    fun toggleAllSelection(entityType: EntityType, selectAll: Boolean) {
        _uiState.update { currentState ->
            val content = currentState.backupContent ?: return@update currentState
            val updatedContent = when (entityType) {
                EntityType.PROJECT -> content.copy(projects = content.projects.map { it.copy(isSelected = selectAll) })
                EntityType.GOAL -> content.copy(goals = content.goals.map { it.copy(isSelected = selectAll) })
            }
            currentState.copy(backupContent = updatedContent)
        }
    }
}

enum class EntityType {
    PROJECT,
    GOAL
}

sealed interface SelectiveImportEvent {
    object NavigateBack : SelectiveImportEvent
}
