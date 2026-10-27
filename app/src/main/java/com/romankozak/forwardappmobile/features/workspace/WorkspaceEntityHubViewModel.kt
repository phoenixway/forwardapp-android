package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.WorkspaceRelationsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalRelationsCapabilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

/** Dashboard preview is available without enabling the full Related entities capability. */
@HiltViewModel
class WorkspaceEntityHubViewModel @Inject constructor(
    saved: SavedStateHandle,
    relations: WorkspaceRelationsRepository,
    capabilities: CanonicalRelationsCapabilityRepository,
) : ViewModel() {
    private val id = saved.get<String>("workspaceId").orEmpty()
    val state = (if (id.isBlank()) flowOf(WorkspaceRelationsState()) else
        combine(relations.observe(id), capabilities.observeEnabled(id)) { projection, enabled ->
            WorkspaceRelationsState(enabled = enabled, projection = projection)
        }.onStart { emit(WorkspaceRelationsState(loading = true)) }
            .catch { emit(WorkspaceRelationsState(error = "Не вдалося завантажити зв’язки")) })
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceRelationsState(loading = true))
}
