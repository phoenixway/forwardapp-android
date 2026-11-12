package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.WorkspaceRelationsRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
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
    workspaces: WorkspaceDao,
) : ViewModel() {
    // Recents retains ContextDetail route arguments. Admit only its exact existing
    // canonical owner; a Context locator alone is never binding authority.
    private val workspaceIds = flow {
        val workspaceId = if (saved.contains("workspaceId")) saved.get<String>("workspaceId")
        else saved.get<String>("listId")?.takeIf { it.isNotBlank() }?.let { contextId ->
            workspaces.getContextBackedForContextId(contextId)?.takeUnless { it.isDeleted }?.id
        }
        emit(workspaceId.orEmpty())
    }
    val state = workspaceIds.flatMapLatest { workspaceId ->
        if (workspaceId.isBlank()) flowOf(WorkspaceRelationsState())
        else capabilities.observeEnabled(workspaceId).flatMapLatest { enabled ->
            if (!enabled) flowOf(WorkspaceRelationsState())
            else relations.observe(workspaceId).map { WorkspaceRelationsState(enabled = true, projection = it) }
                .onStart { emit(WorkspaceRelationsState(enabled = true, loading = true)) }
                .catch { emit(WorkspaceRelationsState(enabled = true, error = "Не вдалося завантажити зв’язки")) }
        }
    }.catch { emit(WorkspaceRelationsState(enabled = true, error = "Не вдалося завантажити зв’язки")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceRelationsState())
}
