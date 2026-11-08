package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*

data class WorkspaceOccurrencesState(
    val loading: Boolean = true,
    val occurrences: List<WorkspaceOccurrence> = emptyList(),
    val error: String? = null,
    val workspaceAvailable: Boolean = false,
)

@HiltViewModel
class WorkspaceOccurrencesViewModel @Inject constructor(
    saved: SavedStateHandle,
    structure: BigPictureStructureRepository,
    catalog: CanonicalEntityCatalogRepository,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
    private val clipboard: NavigationClipboard = NavigationClipboard(),
) : ViewModel() {
    internal val workspaceId = if (saved.contains("workspaceId")) saved.get<String>("workspaceId").orEmpty()
        else saved.get<String>("projectId").orEmpty()
    val state = (if (workspaceId.isBlank()) flowOf(WorkspaceOccurrencesState(loading = false))
        else combine(structure.observe(), catalog.observeCatalog()) { graph, entities ->
            WorkspaceOccurrencesState(loading = false, occurrences = projectWorkspaceOccurrences(workspaceId, graph, entities),
                workspaceAvailable = entities.any { it.ref == CanonicalEntityRef.Workspace(workspaceId) })
        }).flowOn(dispatcher)
        .catch { emit(WorkspaceOccurrencesState(loading = false, error = "Не вдалося завантажити появи")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), WorkspaceOccurrencesState())

    fun copyWorkspace(): Boolean {
        if (!state.value.workspaceAvailable || state.value.loading || state.value.error != null) return false
        clipboard.copy(CanonicalEntityRef.Workspace(workspaceId))
        return true
    }

    fun cutOccurrence(placementId: String): Boolean {
        if (!state.value.workspaceAvailable || state.value.loading || state.value.error != null ||
            state.value.occurrences.none { it.placementId == placementId }) return false
        clipboard.cut(H1TypedNode.Occurrence(placementId))
        return true
    }
}
