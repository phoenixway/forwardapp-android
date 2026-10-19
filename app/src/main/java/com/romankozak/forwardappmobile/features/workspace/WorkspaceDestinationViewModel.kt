package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjection
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WorkspaceDestinationUiState {
    data object Loading : WorkspaceDestinationUiState

    data class Ready(
        val workspaceId: String,
        val title: String,
        val description: String?,
        val roleCode: String?,
        val compatibilityContextId: String?,
    ) : WorkspaceDestinationUiState

    data class Unavailable(val message: String) : WorkspaceDestinationUiState
}

internal fun WorkspaceCompatibilityProjection.toDestinationState(): WorkspaceDestinationUiState {
    if (workspace.isDeleted) {
        return WorkspaceDestinationUiState.Unavailable("Робочий простір недоступний")
    }
    val liveSourceContext = sourceContext?.takeUnless { it.isDeleted }
    val compatibleContextId =
        liveSourceContext
            ?.takeIf { it.id == workspace.sourceContextId && it.id == workspace.id }
            ?.id
    return WorkspaceDestinationUiState.Ready(
        workspaceId = workspace.id,
        title = workspace.nameOverride?.trim()?.takeIf(String::isNotEmpty) ?: workspace.id,
        description = workspace.descriptionOverride?.trim()?.takeIf(String::isNotEmpty),
        roleCode = workspace.roleCode,
        compatibilityContextId = compatibleContextId,
    )
}

@HiltViewModel
class WorkspaceDestinationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: WorkspaceCompatibilityProjectionRepository,
    ) : ViewModel() {
        private val workspaceId = savedStateHandle.get<String>("workspaceId").orEmpty()
        private val _uiState = MutableStateFlow<WorkspaceDestinationUiState>(WorkspaceDestinationUiState.Loading)
        val uiState: StateFlow<WorkspaceDestinationUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                if (workspaceId.isBlank()) {
                    _uiState.value = WorkspaceDestinationUiState.Unavailable("Некоректний ідентифікатор робочого простору")
                    return@launch
                }
                try {
                    // Compatibility evidence is optional. Subsequent scalar/lifecycle updates
                    // come from this exact Workspace row, never from Context or a full graph.
                    val initial = repository.get(workspaceId)
                    _uiState.value = initial?.toDestinationState()
                        ?: WorkspaceDestinationUiState.Unavailable("Робочий простір недоступний")
                    if (initial != null) repository.observeWorkspace(workspaceId).collect { workspace ->
                        _uiState.value = workspace?.let {
                            initial.copy(workspace = it).toDestinationState()
                        } ?: WorkspaceDestinationUiState.Unavailable("Робочий простір недоступний")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _uiState.value = WorkspaceDestinationUiState.Unavailable("Не вдалося завантажити робочий простір")
                }
            }
        }
    }
