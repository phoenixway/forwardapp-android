package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.displayParentId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChooserUiState(
    val topLevelProjects: List<Context> = emptyList(),
    val childMap: Map<String, List<Context>> = emptyMap(),
)

@HiltViewModel
class FilterableListChooserViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
    ) : ViewModel() {
        private val TAG = "FilterChooserVM"

        private val _filterText = MutableStateFlow("")
        val filterText: StateFlow<String> = _filterText.asStateFlow()

        private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
        val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

        private val _showDescendants = MutableStateFlow(false)
        val showDescendants: StateFlow<Boolean> = _showDescendants.asStateFlow()
        private val allProjects =
            contextRepository
                .getAllContextsFlow()
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
                val allProjectsById = projects.associateBy { it.id }
                val displayParentById =
                    projects.associate { project ->
                        project.id to project.displayParentId(allProjectsById)
                    }

                if (filter.isBlank()) {
                    val fullChildMap =
                        projects
                            .mapNotNull { project ->
                                displayParentById[project.id]?.let { parentId -> parentId to project }
                            }.groupBy(
                                keySelector = { it.first },
                                valueTransform = { it.second },
                            )
                            .mapValues { (_, children) -> children.sortedBy { it.order } }
                    val fullTopLevelProjects =
                        projects
                            .filter { displayParentById[it.id] == null }
                            .sortedBy { it.order }
                    ChooserUiState(topLevelProjects = fullTopLevelProjects, childMap = fullChildMap)
                } else {
                    val matchingProjects = projects.filter { it.name.contains(filter, ignoreCase = true) }

                    val visibleIds = mutableSetOf<String>()

                    matchingProjects.forEach { matchedProject ->
                        val path = mutableSetOf<String>()
                        var current: Context? = matchedProject
                        while (current != null && current.id !in path) {
                            path.add(current.id)
                            visibleIds.add(current.id)
                            current = displayParentById[current.id]?.let { parentId -> allProjectsById[parentId] }
                        }
                    }

                    if (shouldShowDescendants) {
                        val fullChildMapForTraversal =
                            projects
                                .mapNotNull { project ->
                                    displayParentById[project.id]?.let { parentId -> parentId to project }
                                }.groupBy(
                                    keySelector = { it.first },
                                    valueTransform = { it.second },
                                )
                        val descendantsQueue = ArrayDeque(matchingProjects)

                        while (descendantsQueue.isNotEmpty()) {
                            val current = descendantsQueue.removeFirst()
                            visibleIds.add(current.id)
                            val children = fullChildMapForTraversal[current.id] ?: emptyList()
                            descendantsQueue.addAll(children)
                        }
                    }
                    val visibleProjects = projects.filter { project -> project.id in visibleIds }

                    val filteredChildMap =
                        visibleProjects
                            .mapNotNull { project ->
                                displayParentById[project.id]?.let { parentId -> parentId to project }
                            }.groupBy(
                                keySelector = { it.first },
                                valueTransform = { it.second },
                            )
                            .mapValues { entry -> entry.value.sortedBy { child -> child.order } }

                    val filteredTopLevelProjects =
                        visibleProjects
                            .filter { project -> displayParentById[project.id] == null }
                            .sortedBy { project -> project.order }

                    ChooserUiState(topLevelProjects = filteredTopLevelProjects, childMap = filteredChildMap)
                }
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
                    val projects = allProjects.value
                    val projectMap = projects.associateBy { it.id }
                    val displayParentById =
                        projects.associate { project ->
                            project.id to project.displayParentId(projectMap)
                        }
                    val matchingProjects = projects.filter { it.name.contains(text, ignoreCase = true) }

                    val idsToExpand = mutableSetOf<String>()
                    matchingProjects.forEach { project ->
                        var parentId = displayParentById[project.id]
                        while (parentId != null) {
                            idsToExpand.add(parentId)
                            parentId = displayParentById[parentId]
                        }
                    }
                    _expandedIds.value = idsToExpand
                }
            }
        }

        fun toggleShowDescendants() {
            _showDescendants.value = !_showDescendants.value
        }

        fun toggleExpanded(projectId: String) {
            _expandedIds.value =
                if (projectId in _expandedIds.value) {
                    _expandedIds.value - projectId
                } else {
                    _expandedIds.value + projectId
                }
        }

        fun addNewProject(
            id: String,
            parentId: String?,
            name: String,
        ) {
            viewModelScope.launch {
                contextRepository.createContextWithId(id, name, parentId)
            }
        }
    }
