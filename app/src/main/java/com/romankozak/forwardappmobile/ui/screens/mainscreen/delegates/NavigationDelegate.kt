package com.romankozak.forwardappmobile.ui.screens.mainscreen.delegates

import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.GoalListUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.PlanningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NavigationDelegate(
    private val goalRepository: GoalRepository,
    private val uiEventChannel: Channel<GoalListUiEvent>
) {
    private val _currentBreadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
    val currentBreadcrumbs = _currentBreadcrumbs.asStateFlow()

    private val _focusedListId = MutableStateFlow<String?>(null)
    val focusedListId = _focusedListId.asStateFlow()

    private val _hierarchySettings = MutableStateFlow(HierarchyDisplaySettings())
    val hierarchySettings = _hierarchySettings.asStateFlow()

    private val _collapsedAncestorsOnFocus = MutableStateFlow<Set<String>>(emptySet())

    private val _highlightedListId = MutableStateFlow<String?>(null)
    val highlightedListId: StateFlow<String?> = _highlightedListId.asStateFlow()

    suspend fun navigateToList(listId: String, hierarchy: StateFlow<ListHierarchyData>) {
        withContext(Dispatchers.Default) {
            val currentHierarchy = hierarchy.value
            val path = buildPathToList(listId, currentHierarchy)

            val collapsedIds = path
                .mapNotNull { breadcrumbItem ->
                    currentHierarchy.allLists.find { it.id == breadcrumbItem.id }
                }
                .filter { !it.isExpanded }
                .map { it.id }
                .toSet()

            _collapsedAncestorsOnFocus.value = collapsedIds
            _currentBreadcrumbs.value = path
            _focusedListId.value = listId
        }
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
        val currentPath = _currentBreadcrumbs.value
        val newPath = currentPath.take(breadcrumbItem.level + 1)
        _currentBreadcrumbs.value = newPath
        _focusedListId.value = breadcrumbItem.id
    }

    suspend fun clearNavigation(allListsFlat: StateFlow<List<GoalList>>) {
        val breadcrumbs = _currentBreadcrumbs.value
        if (breadcrumbs.isNotEmpty()) {
            val topLevelAncestorId = breadcrumbs.first().id
            val ancestorList = allListsFlat.value.find { it.id == topLevelAncestorId }

            if (ancestorList != null && !ancestorList.isExpanded) {
                goalRepository.updateGoalList(ancestorList.copy(isExpanded = true))

                allListsFlat.first { updatedLists ->
                    updatedLists.find { it.id == topLevelAncestorId }?.isExpanded == true
                }
            }
        }

        val listsToCollapseIds = _collapsedAncestorsOnFocus.value
        if (listsToCollapseIds.isNotEmpty()) {
            val listsToUpdate = allListsFlat.value
                .filter { it.id in listsToCollapseIds }
                .map { it.copy(isExpanded = false) }

            if (listsToUpdate.isNotEmpty()) {
                goalRepository.updateGoalLists(listsToUpdate)
            }
        }

        _collapsedAncestorsOnFocus.value = emptySet()
        _currentBreadcrumbs.value = emptyList()
        _focusedListId.value = null
    }

    suspend fun processRevealRequest(
        listId: String,
        allListsFlat: StateFlow<List<GoalList>>,
        planningMode: MutableStateFlow<PlanningMode>,
        isSearchActive: MutableStateFlow<Boolean>
    ) {
        isSearchActive.value = false
        planningMode.value = PlanningMode.All

        val allLists = allListsFlat.first { it.isNotEmpty() }
        val listLookup = allLists.associateBy { it.id }

        if (!listLookup.containsKey(listId)) {
            uiEventChannel.send(GoalListUiEvent.ShowToast("Could not find list."))
            return
        }

        val ancestorIds = mutableSetOf<String>()
        findAncestorsRecursive(listId, listLookup, ancestorIds, mutableSetOf())

        val listsToExpand = ancestorIds
            .mapNotNull { listLookup[it] }
            .filter { !it.isExpanded && it.id != listId }

        if (listsToExpand.isNotEmpty()) {
            goalRepository.updateGoalLists(listsToExpand.map { it.copy(isExpanded = true) })

            allListsFlat.first { updatedListState ->
                listsToExpand.all { listToExpand ->
                    updatedListState.find { it.id == listToExpand.id }?.isExpanded == true
                }
            }
        }

        val finalListState = allListsFlat.value
        val topLevel = finalListState.filter { it.parentId == null }.sortedBy { it.order }

        fun flattenHierarchy(currentLists: List<GoalList>, listMap: Map<String, List<GoalList>>): List<GoalList> {
            val result = mutableListOf<GoalList>()
            for (list in currentLists) {
                result.add(list)
                if (list.isExpanded) {
                    val children = listMap[list.id]?.sortedBy { it.order } ?: emptyList()
                    if (children.isNotEmpty()) {
                        result.addAll(flattenHierarchy(children, listMap))
                    }
                }
            }
            return result
        }

        val childrenOnly = finalListState.filter { it.parentId != null }
        val childMap = childrenOnly.groupBy { it.parentId!! }
        val displayedLists = flattenHierarchy(topLevel, childMap)

        val index = displayedLists.indexOfFirst { it.id == listId }

        if (index != -1) {
            uiEventChannel.send(GoalListUiEvent.ScrollToIndex(index))
        }

        _highlightedListId.value = listId
        delay(1500)
        if (_highlightedListId.value == listId) {
            _highlightedListId.value = null
        }
    }

    private fun buildPathToList(targetId: String, hierarchy: ListHierarchyData): List<BreadcrumbItem> {
        val path = mutableListOf<BreadcrumbItem>()
        fun findPath(lists: List<GoalList>, level: Int): Boolean {
            val sortedLists = lists.sortedBy { it.order }
            for (list in sortedLists) {
                path.add(BreadcrumbItem(list.id, list.name, level))
                if (list.id == targetId) return true
                val children = hierarchy.childMap[list.id] ?: emptyList()
                if (findPath(children, level + 1)) return true
                path.removeLastOrNull()
            }
            return false
        }
        findPath(hierarchy.topLevelLists, 0)
        return path.toList()
    }

    private fun findAncestorsRecursive(
        listId: String?,
        listLookup: Map<String, GoalList>,
        ids: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        var currentId = listId
        while (currentId != null && visited.add(currentId)) {
            ids.add(currentId)
            currentId = listLookup[currentId]?.parentId
        }
    }
}