package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserQuery
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.filterCanonicalEntities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn

enum class EntityLibraryFilter(val allowedClasses: Set<CanonicalEntityClass>) {
    ALL(CanonicalEntityClass.entries.toSet()),
    ORIENTATIONS(setOf(CanonicalEntityClass.ORIENTATION)),
    ASPECTS(setOf(CanonicalEntityClass.ASPECT)),
    WORKSPACES(setOf(CanonicalEntityClass.WORKSPACE)),
    UNPLACED(CanonicalEntityClass.entries.toSet()),
}

sealed interface EntityLibraryUiState {
    data object Loading : EntityLibraryUiState

    data class Ready(
        val items: List<CanonicalEntitySummary>,
        val admittedCatalogSize: Int,
        val searchText: String,
        val filter: EntityLibraryFilter,
        val sort: EntityLibrarySort = EntityLibrarySort.NAME_ASC,
    ) : EntityLibraryUiState {
        val isCatalogEmpty: Boolean get() = admittedCatalogSize == 0
        val hasNoMatches: Boolean get() = admittedCatalogSize > 0 && items.isEmpty()
    }

    data class Error(val message: String) : EntityLibraryUiState
}

sealed interface EntityLibraryDestination {
    data class SemanticEditor(val subjectId: String) : EntityLibraryDestination

    data class Workspace(val workspaceId: String) : EntityLibraryDestination
}

fun entityLibraryDestination(ref: CanonicalEntityRef): EntityLibraryDestination =
    when (ref) {
        is CanonicalEntityRef.Orientation -> EntityLibraryDestination.SemanticEditor(ref.id)
        is CanonicalEntityRef.Aspect -> EntityLibraryDestination.SemanticEditor(ref.id)
        is CanonicalEntityRef.Workspace -> EntityLibraryDestination.Workspace(ref.id)
    }

@HiltViewModel
class EntityLibraryViewModel
    @Inject
    constructor(
        catalogRepository: CanonicalEntityCatalogRepository,
        presenceRepository: CanonicalHierarchyPlacementPresenceRepository,
        private val savedStateHandle: SavedStateHandle,
        @DefaultDispatcher private val computationDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val searchText = savedStateHandle.getStateFlow(SEARCH_KEY, "")
        private val filter = savedStateHandle.getStateFlow(FILTER_KEY, EntityLibraryFilter.ALL.name)
        private val sort = savedStateHandle.getStateFlow(SORT_KEY, EntityLibrarySort.NAME_ASC.name)

        val uiState =
            combine(
                catalogRepository.observeCatalog(),
                presenceRepository.observeVisiblePlacedTargets(),
                searchText,
                filter,
                sort,
            ) { catalog, placedTargets, text, filterName, sortName ->
                val selectedSort = EntityLibrarySort.entries.firstOrNull { it.name == sortName }
                    ?: EntityLibrarySort.NAME_ASC
                val admittedCatalog = filterCanonicalEntities(catalog, CanonicalEntityBrowserQuery())
                val selectedFilter =
                    EntityLibraryFilter.entries.firstOrNull { it.name == filterName }
                        ?: EntityLibraryFilter.ALL
                val scopeCatalog =
                    if (selectedFilter == EntityLibraryFilter.UNPLACED) {
                        deriveUnplacedCanonicalEntities(admittedCatalog, placedTargets)
                    } else {
                        admittedCatalog
                    }
                EntityLibraryUiState.Ready(
                    items =
                        sortLibraryEntities(
                            filterCanonicalEntities(
                                scopeCatalog,
                                CanonicalEntityBrowserQuery(
                                    text = text,
                                    allowedClasses = selectedFilter.allowedClasses,
                                ),
                            ),
                            selectedSort,
                        ),
                    admittedCatalogSize = admittedCatalog.size,
                    searchText = text,
                    filter = selectedFilter,
                    sort = selectedSort,
                ) as EntityLibraryUiState
            }.catch { emit(EntityLibraryUiState.Error(it.message ?: "Не вдалося завантажити бібліотеку")) }
                .flowOn(computationDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntityLibraryUiState.Loading)

        fun setSearchText(value: String) {
            savedStateHandle[SEARCH_KEY] = value
        }

        fun setFilter(value: EntityLibraryFilter) {
            savedStateHandle[FILTER_KEY] = value.name
        }

        fun setSort(value: EntityLibrarySort) {
            savedStateHandle[SORT_KEY] = value.name
        }

        private companion object {
            const val SEARCH_KEY = "entityLibrarySearch"
            const val FILTER_KEY = "entityLibraryFilter"
            const val SORT_KEY = "entityLibrarySort"
        }
    }
