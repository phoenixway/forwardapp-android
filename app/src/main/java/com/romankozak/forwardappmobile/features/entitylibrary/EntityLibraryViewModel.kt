package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityQueryRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserQuery
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.filterCanonicalEntities
import com.romankozak.forwardappmobile.features.entitybrowser.*
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
        val query: CanonicalEntityQuery = CanonicalEntityQuery(),
        val availableTags: List<String> = emptyList(),
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
        catalogRepository: CanonicalEntityQueryRepository,
        presenceRepository: CanonicalHierarchyPlacementPresenceRepository,
        private val savedStateHandle: SavedStateHandle,
        @DefaultDispatcher private val computationDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val searchText = savedStateHandle.getStateFlow(SEARCH_KEY, "")
        private val filter = savedStateHandle.getStateFlow(FILTER_KEY, EntityLibraryFilter.ALL.name)
        private val sort = savedStateHandle.getStateFlow(SORT_KEY, EntityLibrarySort.NAME_ASC.name)
        private val preferences = savedStateHandle.getStateFlow(QUERY_KEY, "")
        private val scoreDimension = savedStateHandle.getStateFlow(DIMENSION_KEY, CanonicalScoreDimension.IMPORTANCE.name)
        private data class Controls(val text: String, val filter: String, val sort: String, val preferences: EntityLibraryQueryPreferences, val dimension: CanonicalScoreDimension)
        private val controls = combine(searchText, filter, sort, preferences, scoreDimension) { text, filter, sort, prefs, dimension ->
            Controls(text, filter, sort, EntityLibraryQueryPreferences.decode(prefs), CanonicalScoreDimension.entries.firstOrNull { it.name == dimension } ?: CanonicalScoreDimension.IMPORTANCE)
        }
        private fun queryFor(controls: Controls): CanonicalEntityQuery {
            val selectedFilter = EntityLibraryFilter.entries.firstOrNull { it.name == controls.filter } ?: EntityLibraryFilter.ALL
            val selectedSort = EntityLibrarySort.entries.firstOrNull { it.name == controls.sort } ?: EntityLibrarySort.NAME_ASC
            return CanonicalEntityQuery(
                text = controls.text,
                entityTypes = controls.preferences.types ?: selectedFilter.allowedClasses,
                tags = controls.preferences.tags, scoreFilters = controls.preferences.ranges,
                sort = selectedSort.toQuerySort(controls.dimension), unplaced = selectedFilter == EntityLibraryFilter.UNPLACED,
            )
        }
        /** Capture input synchronously: saving must not depend on an older background result frame. */
        fun currentQuery(): CanonicalEntityQuery = queryFor(Controls(searchText.value, filter.value, sort.value,
            EntityLibraryQueryPreferences.decode(preferences.value),
            CanonicalScoreDimension.entries.firstOrNull { it.name == scoreDimension.value } ?: CanonicalScoreDimension.IMPORTANCE))
        private data class Inventory(val items: List<CanonicalEntitySummary>, val tags: List<String>, val placed: Set<CanonicalEntityRef>)
        private val inventory = combine(catalogRepository.observeCatalog(), presenceRepository.observeVisiblePlacedTargets()) { catalog, presence ->
            val admitted = filterCanonicalEntities(catalog, CanonicalEntityBrowserQuery())
            Inventory(admitted, admitted.flatMap { it.tags }.distinct().sorted(), canonicalPlacedRefs(admitted, presence))
        }

        val uiState =
            combine(
                inventory,
                controls,
            ) { inventory, controls ->
                val selectedSort = EntityLibrarySort.entries.firstOrNull { it.name == controls.sort }
                    ?: EntityLibrarySort.NAME_ASC
                val admittedCatalog = inventory.items
                val selectedFilter =
                    EntityLibraryFilter.entries.firstOrNull { it.name == controls.filter }
                        ?: EntityLibraryFilter.ALL
                val query = queryFor(controls)
                EntityLibraryUiState.Ready(
                    items =
                        executeCanonicalEntityQuery(admittedCatalog, query, inventory.placed),
                    admittedCatalogSize = admittedCatalog.size,
                    searchText = controls.text,
                    filter = selectedFilter,
                    sort = selectedSort,
                    query = query,
                    availableTags = inventory.tags,
                ) as EntityLibraryUiState
            }.catch { emit(EntityLibraryUiState.Error(it.message ?: "Не вдалося завантажити бібліотеку")) }
                .flowOn(computationDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntityLibraryUiState.Loading)

        fun setSearchText(value: String) {
            savedStateHandle[SEARCH_KEY] = value
        }

        /** Applying a preset changes only the current query; the saved specification is never autosaved. */
        fun applyQuery(query: CanonicalEntityQuery) {
            applyFilters(query.entityTypes, query.tags, query.scoreFilters)
            savedStateHandle[SEARCH_KEY] = query.text
            savedStateHandle[FILTER_KEY] = if (query.unplaced) EntityLibraryFilter.UNPLACED.name else EntityLibraryFilter.ALL.name
            savedStateHandle[DIMENSION_KEY] = (query.sort.dimension ?: CanonicalScoreDimension.IMPORTANCE).name
            savedStateHandle[SORT_KEY] = when (query.sort.key) {
                CanonicalEntitySortKey.DEFAULT -> if (query.sort.direction == CanonicalSortDirection.ASC) EntityLibrarySort.DEFAULT else EntityLibrarySort.DEFAULT_DESC
                CanonicalEntitySortKey.TITLE -> if (query.sort.direction == CanonicalSortDirection.ASC) EntityLibrarySort.NAME_ASC else EntityLibrarySort.NAME_DESC
                CanonicalEntitySortKey.CREATED -> if (query.sort.direction == CanonicalSortDirection.ASC) EntityLibrarySort.CREATED_ASC else EntityLibrarySort.CREATED_DESC
                CanonicalEntitySortKey.UPDATED -> if (query.sort.direction == CanonicalSortDirection.ASC) EntityLibrarySort.UPDATED_ASC else EntityLibrarySort.UPDATED_DESC
                CanonicalEntitySortKey.SCORE -> if (query.sort.direction == CanonicalSortDirection.ASC) EntityLibrarySort.SCORE_ASC else EntityLibrarySort.SCORE_DESC
            }.name
        }

        fun setFilter(value: EntityLibraryFilter) {
            updatePreferences { it.copy(types = null) }
            savedStateHandle[FILTER_KEY] = value.name
        }

        fun setSort(value: EntityLibrarySort) {
            savedStateHandle[SORT_KEY] = value.name
        }

        fun setTypes(types: Set<CanonicalEntityClass>) = updatePreferences { it.copy(types = types) }
        fun applyFilters(types: Set<CanonicalEntityClass>, tags: Set<String>, ranges: Map<CanonicalScoreDimension, CanonicalScoreRange>) {
            savedStateHandle[QUERY_KEY] = EntityLibraryQueryPreferences(types,
                tags.map(::normalizeCanonicalQueryTag).filter(String::isNotEmpty).toSet(), ranges).encode()
        }
        fun setTags(tags: Set<String>) = updatePreferences { it.copy(tags = tags.map(::normalizeCanonicalQueryTag).filter(String::isNotEmpty).toSet()) }
        fun setScoreFilter(dimension: CanonicalScoreDimension, range: CanonicalScoreRange?) = updatePreferences {
            it.copy(ranges = if (range == null) it.ranges - dimension else it.ranges + (dimension to range))
        }
        fun setScoreSort(dimension: CanonicalScoreDimension, descending: Boolean) {
            savedStateHandle[DIMENSION_KEY] = dimension.name
            setSort(if (descending) EntityLibrarySort.SCORE_DESC else EntityLibrarySort.SCORE_ASC)
        }
        fun setUnplaced(enabled: Boolean) {
            val current = EntityLibraryFilter.entries.firstOrNull { it.name == filter.value } ?: EntityLibraryFilter.ALL
            updatePreferences { it.copy(types = it.types ?: current.allowedClasses) }
            savedStateHandle[FILTER_KEY] = if (enabled) EntityLibraryFilter.UNPLACED.name else EntityLibraryFilter.ALL.name
        }
        fun clearFilters() {
            savedStateHandle[FILTER_KEY] = EntityLibraryFilter.ALL.name
            savedStateHandle[QUERY_KEY] = EntityLibraryQueryPreferences().encode()
        }
        private fun updatePreferences(update: (EntityLibraryQueryPreferences) -> EntityLibraryQueryPreferences) {
            savedStateHandle[QUERY_KEY] = update(EntityLibraryQueryPreferences.decode(preferences.value)).encode()
        }

        private companion object {
            const val SEARCH_KEY = "entityLibrarySearch"
            const val FILTER_KEY = "entityLibraryFilter"
            const val SORT_KEY = "entityLibrarySort"
            const val QUERY_KEY = "entityLibraryQueryV1"
            const val DIMENSION_KEY = "entityLibraryScoreDimension"
        }
    }
