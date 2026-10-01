package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.data.hierarchy.ChooserHierarchyItem
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ChooserProjection
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ReactiveHierarchyReadSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChooserUiState(
    val topLevelProjects: List<ChooserHierarchyItem> = emptyList(),
    // Keys are PlacementId.value, never Workspace target IDs.
    val childMap: Map<String, List<ChooserHierarchyItem>> = emptyMap(),
)

/**
 * Occurrence-native chooser tree. A Workspace appearing twice must retain
 * independent ancestry and expansion state. Non-Workspace parents are
 * presentation boundaries, not synthetic Workspace target parents.
 */
internal fun buildChooserUiState(
    projects: List<ChooserHierarchyItem>,
    filter: String,
    showDescendants: Boolean,
): ChooserUiState {
    val byPlacement = projects.associateBy { it.occurrence.placementId.value }
    val children = projects
        .filter { project ->
            project.occurrence.parentPlacementId?.value in byPlacement
        }
        .groupBy { project -> requireNotNull(project.occurrence.parentPlacementId).value }
        .mapValues { (_, entries) -> entries.sortedBy { it.order } }
    val roots = projects
        .filter { project ->
            project.occurrence.parentPlacementId?.value !in byPlacement
        }
        .sortedBy { it.order }

    if (filter.isBlank()) {
        return ChooserUiState(topLevelProjects = roots, childMap = children)
    }

    val matching = projects.filter { it.name.contains(filter, ignoreCase = true) }
    val visiblePlacements = mutableSetOf<String>()
    matching.forEach { match ->
        var current: ChooserHierarchyItem? = match
        while (current != null && visiblePlacements.add(current.occurrence.placementId.value)) {
            current = current.occurrence.parentPlacementId?.value?.let(byPlacement::get)
        }
    }

    if (showDescendants) {
        val queue = ArrayDeque(matching)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            children[current.occurrence.placementId.value].orEmpty().forEach { child ->
                if (visiblePlacements.add(child.occurrence.placementId.value)) {
                    queue.add(child)
                }
            }
        }
    }

    return ChooserUiState(
        topLevelProjects = roots.filter { it.occurrence.placementId.value in visiblePlacements },
        childMap = children.mapValues { (_, entries) ->
            entries.filter { it.occurrence.placementId.value in visiblePlacements }
        },
    )
}

@HiltViewModel
class FilterableListChooserViewModel
    @Inject
    constructor(
        private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
        private val canonicalV2ReactiveHierarchyReadSource: CanonicalV2ReactiveHierarchyReadSource,
        private val canonicalV2ChooserProjection: CanonicalV2ChooserProjection,
    ) : ViewModel() {
        private val TAG = "FilterChooserVM"

        private val _filterText = MutableStateFlow("")
        val filterText: StateFlow<String> = _filterText.asStateFlow()

        private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
        val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

        private val _showDescendants = MutableStateFlow(false)
        val showDescendants: StateFlow<Boolean> = _showDescendants.asStateFlow()
        private val allProjects =
            combine(
                canonicalV2ReactiveHierarchyReadSource.observe(),
                canonicalV2ReactiveHierarchyReadSource.observeCanonicalWorkspacePresentations(),
            ) { read, presentations ->
                canonicalV2ChooserProjection.project(
                    read = read,
                    workspacePresentations = presentations,
                )
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        @OptIn(FlowPreview::class)
        val chooserState: StateFlow<ChooserUiState> =
            combine(
                filterText.debounce(300),
                allProjects,
                showDescendants,
            ) { filter, projects, shouldShowDescendants ->
                buildChooserUiState(
                    projects = projects,
                    filter = filter,
                    showDescendants = shouldShowDescendants,
                )
            }.flowOn(Dispatchers.Default)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = ChooserUiState(),
                )

        fun updateFilterText(text: String) {
            _filterText.value = text
            if (text.isBlank()) {
                _expandedIds.value = emptySet()
            } else {
                viewModelScope.launch(Dispatchers.Default) {
                    val byPlacement = allProjects.value.associateBy {
                        it.occurrence.placementId.value
                    }
                    val idsToExpand = mutableSetOf<String>()
                    allProjects.value
                        .filter { it.name.contains(text, ignoreCase = true) }
                        .forEach { project ->
                            var parentId = project.occurrence.parentPlacementId?.value
                            while (parentId != null && idsToExpand.add(parentId)) {
                                parentId = byPlacement[parentId]?.occurrence?.parentPlacementId?.value
                            }
                        }
                    _expandedIds.value = idsToExpand
                }
            }
        }

        fun toggleShowDescendants() {
            _showDescendants.value = !_showDescendants.value
        }

        fun toggleExpanded(placementId: String) {
            _expandedIds.value =
                if (placementId in _expandedIds.value) {
                    _expandedIds.value - placementId
                } else {
                    _expandedIds.value + placementId
                }
        }

        suspend fun addNewProject(
            parentId: String?,
            parentPlacementId: String?,
            name: String,
        ): String? {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return null

            return canonicalWorkspaceRepository.createWithV2PrimaryAppearance(
                nameOverride = trimmed,
                descriptionOverride = null,
                parentWorkspaceId = parentId,
                parentPlacementId = parentPlacementId?.let(::PlacementId),
                roleCode = null,
            )
        }
    }
