package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.WorkspaceRelationsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalRelationsCapabilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class WorkspaceRelationsState(
    val enabled: Boolean = false,
    val loading: Boolean = false,
    val projection: WorkspaceRelationsProjection? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkspaceRelationsViewModel @Inject constructor(
    saved: SavedStateHandle,
    capabilities: CanonicalRelationsCapabilityRepository,
    relations: WorkspaceRelationsRepository,
) : ViewModel() {
    private val workspaceId = saved.get<String>("workspaceId").orEmpty()
    val state = (if (workspaceId.isBlank()) flowOf(WorkspaceRelationsState())
        else capabilities.observeEnabled(workspaceId).flatMapLatest { enabled ->
            if (!enabled) flowOf(WorkspaceRelationsState())
            else relations.observe(workspaceId).map { WorkspaceRelationsState(enabled = true, projection = it) }
                .onStart { emit(WorkspaceRelationsState(enabled = true, loading = true)) }
                .catch { emit(WorkspaceRelationsState(enabled = true, error = "Не вдалося завантажити зв’язки")) }
        }).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceRelationsState())
}
