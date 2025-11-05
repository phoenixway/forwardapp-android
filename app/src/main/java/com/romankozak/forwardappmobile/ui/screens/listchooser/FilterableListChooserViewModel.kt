package com.romankozak.forwardappmobile.ui.screens.listchooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChooserUiState(
    val topLevelProjects: List<Project> = emptyList(),
    val childMap: Map<String, List<Project>> = emptyMap(),
)

@HiltViewModel
class FilterableListChooserViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
    ) : ViewModel() {
        private val TAG = "FilterChooserVM"

        private val _filterText = MutableStateFlow("")
        val filterText: StateFlow<String> = _filterText.asStateFlow()

        private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
        val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

        private val _showDescendants = MutableStateFlow(false)
        val showDescendants: StateFlow<Boolean> = _showDescendants.asStateFlow()
        private val allProjects =
            projectRepository
                .getAllProjectsFlow()
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
                if (filter.isBlank()) {
                    val fullChildMap =
                        projects
                            .filter { it.parentId != null }
                            .groupBy { it.parentId!! }
                            .mapValues { (_, children) -> children.sortedBy { it.order } }
                    val fullTopLevelProjects = projects.filter { it.parentId == null }.sortedBy { it.order }
                    ChooserUiState(topLevelProjects = fullTopLevelProjects, childMap = fullChildMap)
                } else {
                    val allProjectsById = projects.associateBy { it.id }
                    val matchingProjects = projects.filter { it.name.contains(filter, ignoreCase = true) }

                    val visibleIds = mutableSetOf<String>()

                    matchingProjects.forEach { matchedProject ->
                        val path = mutableSetOf<String>()
                        var current: Project? = matchedProject
                        while (current != null && current.id !in path) {
                            path.add(current.id)
                            visibleIds.add(current.id)
                            current = current.parentId?.let { parentId -> allProjectsById[parentId] }
                        }
                    }

                    if (shouldShowDescendants) {
                        val fullChildMapForTraversal = projects.filter { it.parentId != null }.groupBy { it.parentId!! }
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
                            .filter { project -> project.parentId != null }
                            .groupBy { project -> project.parentId!! }
                            .mapValues { entry -> entry.value.sortedBy { child -> child.order } }

                    val filteredTopLevelProjects =
                        visibleProjects
                            .filter { project -> project.parentId == null }
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
                    val matchingProjects = projects.filter { it.name.contains(text, ignoreCase = true) }

                    val idsToExpand = mutableSetOf<String>()
                    matchingProjects.forEach { project ->
                        var parentId = project.parentId
                        while (parentId != null) {
                            idsToExpand.add(parentId)
                            parentId = projectMap[parentId]?.parentId
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
                projectRepository.createProjectWithId(id, name, parentId)
            }
        }
    }
