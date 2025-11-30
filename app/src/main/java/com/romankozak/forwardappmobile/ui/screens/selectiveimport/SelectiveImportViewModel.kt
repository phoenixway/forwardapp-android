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
                    val databaseContent = fullAppBackup.database
                    if (databaseContent == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Backup does not contain database section") }
                    } else {
                        val diff = syncRepository.createBackupDiff(databaseContent)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                backupContent = diff.toSelectable()
                            )
                        }
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

             val selectedProjects = contentToImport.projects.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedGoals = contentToImport.goals.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedLegacyNotes = contentToImport.legacyNotes.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedActivityRecords = contentToImport.activityRecords.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedListItems = contentToImport.listItems.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedDocuments = contentToImport.documents.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedChecklists = contentToImport.checklists.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedLinkItems = contentToImport.linkItems.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedInboxRecords = contentToImport.inboxRecords.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedProjectExecutionLogs = contentToImport.projectExecutionLogs.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedScripts = contentToImport.scripts.filter { it.isSelected && it.isSelectable }.map { it.item }
             val selectedAttachments = contentToImport.attachments.filter { it.isSelected && it.isSelectable }.map { it.item }
             
             android.util.Log.d("IMPORT_DEBUG", "Total projects selected: ${selectedProjects.size}")
             android.util.Log.d("IMPORT_DEBUG", "Projects with parents: ${selectedProjects.filter { it.parentId != null }.size}")
             android.util.Log.d("IMPORT_DEBUG", "Root projects (no parent): ${selectedProjects.filter { it.parentId == null }.size}")
             
             // Filter out system projects (those with systemKey) to prevent duplication
             val regularProjects = selectedProjects.filter { it.systemKey == null }
             val systemProjectsCount = selectedProjects.size - regularProjects.size
             android.util.Log.d("IMPORT_DEBUG", "Regular (non-system) projects: ${regularProjects.size}, System projects: ${systemProjectsCount}")
             
             // Get all available projects from backup to check for parent references
             val allBackupProjects = contentToImport.projects.map { it.item }
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
            
            val selectedProjectIds = projectsWithValidParents.map { it.id }.toSet()
            val selectedGoalIds = selectedGoals.map { it.id }.toSet()
            val selectedLegacyNoteIds = selectedLegacyNotes.map { it.id }.toSet()
            val selectedDocumentIds = selectedDocuments.map { it.id }.toSet()
            val selectedChecklistIds = selectedChecklists.map { it.id }.toSet()
            val selectedLinkItemIds = selectedLinkItems.map { it.id }.toSet()
            val selectedInboxRecordIds = selectedInboxRecords.map { it.id }.toSet()
            val selectedScriptIds = selectedScripts.map { it.id }.toSet()
            val selectedAttachmentIds = selectedAttachments.map { it.id }.toSet()

            // Filter list items to only those linked to selected projects, goals, documents, checklists, legacy notes, scripts, inbox records
            val allListItems = currentState.backupContent?.listItems?.map { it.item } ?: emptyList()
            val filteredListItems = allListItems.filter { listItem ->
                listItem.projectId in selectedProjectIds ||
                listItem.entityId in selectedGoalIds ||
                listItem.entityId in selectedLegacyNoteIds ||
                listItem.entityId in selectedDocumentIds ||
                listItem.entityId in selectedChecklistIds ||
                listItem.entityId in selectedScriptIds ||
                listItem.entityId in selectedInboxRecordIds
            }

            // Filter document items to only those linked to selected documents
            val allDocumentItems = currentState.backupContent?.documentItems ?: emptyList()
            val filteredDocumentItems = allDocumentItems.map { it.item }.filter { it.listId in selectedDocumentIds }
            
            // Filter checklist items to only those linked to selected checklists
            val allChecklistItems = currentState.backupContent?.checklistItems ?: emptyList()
            val filteredChecklistItems = allChecklistItems.map { it.item }.filter { it.checklistId in selectedChecklistIds }
            
            // Filter project attachment cross-refs to only those linked to selected projects and selected attachments
            val allProjectAttachmentCrossRefs = currentState.backupContent?.allProjectAttachmentCrossRefs ?: emptyList()
            val filteredCrossRefs = allProjectAttachmentCrossRefs.filter { crossRef ->
                crossRef.projectId in selectedProjectIds && crossRef.attachmentId in selectedAttachmentIds
            }

            val databaseContent = DatabaseContent(
                projects = projectsWithValidParents,
                goals = selectedGoals,
                legacyNotes = selectedLegacyNotes,
                activityRecords = selectedActivityRecords,
                listItems = filteredListItems,
                documents = selectedDocuments,
                documentItems = filteredDocumentItems,
                checklists = selectedChecklists,
                checklistItems = filteredChecklistItems,
                linkItemEntities = selectedLinkItems,
                inboxRecords = selectedInboxRecords,
                projectExecutionLogs = selectedProjectExecutionLogs,
                recentProjectEntries = emptyList(), // Not directly selectable, derived from projects
                attachments = selectedAttachments,
                projectAttachmentCrossRefs = filteredCrossRefs,
                scripts = selectedScripts
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
                if (it.item.id == projectId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(projects = updatedProjects ?: emptyList()))
        }
    }

    fun toggleGoalSelection(goalId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedGoals = currentState.backupContent?.goals?.map {
                if (it.item.id == goalId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(goals = updatedGoals ?: emptyList()))
        }
    }

    fun toggleLegacyNoteSelection(noteId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedNotes = currentState.backupContent?.legacyNotes?.map {
                if (it.item.id == noteId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(legacyNotes = updatedNotes ?: emptyList()))
        }
    }

    fun toggleActivityRecordSelection(recordId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedRecords = currentState.backupContent?.activityRecords?.map {
                if (it.item.id == recordId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(activityRecords = updatedRecords ?: emptyList()))
        }
    }

    fun toggleListItemSelection(itemId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedItems = currentState.backupContent?.listItems?.map {
                if (it.item.id == itemId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(listItems = updatedItems ?: emptyList()))
        }
    }

    fun toggleDocumentSelection(documentId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedDocuments = currentState.backupContent?.documents?.map {
                if (it.item.id == documentId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(documents = updatedDocuments ?: emptyList()))
        }
    }

    fun toggleChecklistSelection(checklistId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedChecklists = currentState.backupContent?.checklists?.map {
                if (it.item.id == checklistId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(checklists = updatedChecklists ?: emptyList()))
        }
    }

    fun toggleLinkItemSelection(linkId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedLinks = currentState.backupContent?.linkItems?.map {
                if (it.item.id == linkId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(linkItems = updatedLinks ?: emptyList()))
        }
    }

    fun toggleInboxRecordSelection(recordId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedRecords = currentState.backupContent?.inboxRecords?.map {
                if (it.item.id == recordId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(inboxRecords = updatedRecords ?: emptyList()))
        }
    }

    fun toggleProjectExecutionLogSelection(logId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedLogs = currentState.backupContent?.projectExecutionLogs?.map {
                if (it.item.id == logId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(projectExecutionLogs = updatedLogs ?: emptyList()))
        }
    }

    fun toggleScriptSelection(scriptId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedScripts = currentState.backupContent?.scripts?.map {
                if (it.item.id == scriptId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(scripts = updatedScripts ?: emptyList()))
        }
    }

    fun toggleAttachmentSelection(attachmentId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedAttachments = currentState.backupContent?.attachments?.map {
                if (it.item.id == attachmentId && it.isSelectable) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(backupContent = currentState.backupContent?.copy(attachments = updatedAttachments ?: emptyList()))
        }
    }
    
    fun toggleAllSelection(entityType: EntityType, selectAll: Boolean) {
        _uiState.update { currentState ->
            val content = currentState.backupContent ?: return@update currentState
            val updatedContent = when (entityType) {
                EntityType.PROJECT -> content.copy(projects = content.projects.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.GOAL -> content.copy(goals = content.goals.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.LEGACY_NOTE -> content.copy(legacyNotes = content.legacyNotes.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.ACTIVITY_RECORD -> content.copy(activityRecords = content.activityRecords.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.LIST_ITEM -> content.copy(listItems = content.listItems.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.DOCUMENT -> content.copy(documents = content.documents.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.CHECKLIST -> content.copy(checklists = content.checklists.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.LINK_ITEM -> content.copy(linkItems = content.linkItems.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.INBOX_RECORD -> content.copy(inboxRecords = content.inboxRecords.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.PROJECT_EXECUTION_LOG -> content.copy(projectExecutionLogs = content.projectExecutionLogs.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.SCRIPT -> content.copy(scripts = content.scripts.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
                EntityType.ATTACHMENT -> content.copy(attachments = content.attachments.map { if (it.isSelectable) it.copy(isSelected = selectAll) else it })
            }
            currentState.copy(backupContent = updatedContent)
        }
    }
}

enum class EntityType {
    PROJECT,
    GOAL,
    LEGACY_NOTE,
    ACTIVITY_RECORD,
    LIST_ITEM,
    DOCUMENT,
    CHECKLIST,
    LINK_ITEM,
    INBOX_RECORD,
    PROJECT_EXECUTION_LOG,
    SCRIPT,
    ATTACHMENT
}

sealed interface SelectiveImportEvent {
    object NavigateBack : SelectiveImportEvent
}
