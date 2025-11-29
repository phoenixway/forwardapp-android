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
        loadBackupFile(savedStateHandle.get<String>("fileUri"))
    }

    private fun loadBackupFile(fileUriString: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            if (fileUriString == null) {
                _uiState.update { it.copy(isLoading = false, error = "File URI not provided.") }
                return@launch
            }

            val fileUri = Uri.parse(fileUriString)

            syncRepository.parseBackupFile(fileUri)
                .onSuccess { fullAppBackup ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupContent = fullAppBackup.database.toSelectable()
                        )
                    }
                }
                .onFailure { error ->
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


            val databaseContent = DatabaseContent(
                projects = selectedProjects,
                goals = selectedGoals,
                legacyNotes = selectedThoughts,
                activityRecords = selectedStats,
                listItems = emptyList(),
                documents = emptyList(),
                documentItems = emptyList(),
                checklists = emptyList(),
                checklistItems = emptyList(),
                linkItemEntities = emptyList(),
                inboxRecords = emptyList(),
                projectExecutionLogs = emptyList(),
                recentProjectEntries = emptyList(),
                attachments = emptyList(),
                projectAttachmentCrossRefs = emptyList()
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
