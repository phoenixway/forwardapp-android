package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*

data class WorkspaceOccurrencesState(
    val loading: Boolean = true,
    val occurrences: List<WorkspaceOccurrence> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class WorkspaceOccurrencesViewModel @Inject constructor(
    saved: SavedStateHandle,
    structure: BigPictureStructureRepository,
    catalog: CanonicalEntityCatalogRepository,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val workspaceId = saved.get<String>("workspaceId").orEmpty()
    val state = (if (workspaceId.isBlank()) flowOf(WorkspaceOccurrencesState(loading = false))
        else combine(structure.observe(), catalog.observeCatalog()) { graph, entities ->
            WorkspaceOccurrencesState(loading = false, occurrences = projectWorkspaceOccurrences(workspaceId, graph, entities))
        }).flowOn(dispatcher)
        .catch { emit(WorkspaceOccurrencesState(loading = false, error = "Не вдалося завантажити появи")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), WorkspaceOccurrencesState())
}
