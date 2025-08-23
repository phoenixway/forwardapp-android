// File: FilterableListChooserViewModel.kt
package com.romankozak.forwardappmobile.ui.screens.listchooser

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChooserUiState(
    val topLevelLists: List<GoalList> = emptyList(),
    val childMap: Map<String, List<GoalList>> = emptyMap()
)

@HiltViewModel
class FilterableListChooserViewModel @Inject constructor(
    private val goalListRepository: GoalRepository
) : ViewModel() {

    private val TAG = "FilterChooserVM"

    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    // --- ПОЧАТОК ЗМІН 1: Новий стан для опції "показувати нащадків" ---
    private val _showDescendants = MutableStateFlow(false)
    val showDescendants: StateFlow<Boolean> = _showDescendants.asStateFlow()
    // --- КІНЕЦЬ ЗМІН 1 ---

    private val allLists = goalListRepository.getAllGoalListsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(FlowPreview::class)
    val chooserState: StateFlow<ChooserUiState> = combine(
        filterText.debounce(300),
        allLists,
        showDescendants // --- ЗМІНА 2: Додаємо новий стан в combine ---
    ) { filter, lists, shouldShowDescendants -> // <-- і в параметри лямбди
        if (filter.isBlank()) {
            val fullChildMap = lists.filter { it.parentId != null }
                .groupBy { it.parentId!! }
                .mapValues { (_, children) -> children.sortedBy { it.order } }
            val fullTopLevelLists = lists.filter { it.parentId == null }.sortedBy { it.order }
            ChooserUiState(topLevelLists = fullTopLevelLists, childMap = fullChildMap)
        } else {
            val allListsById = lists.associateBy { it.id }
            val matchingLists = lists.filter { it.name.contains(filter, ignoreCase = true) }

            val visibleIds = mutableSetOf<String>()

            // Крок 1: Завжди знаходимо самих збігів та їхніх батьків (шлях догори)
            matchingLists.forEach { matchedList ->
                val path = mutableSetOf<String>()
                var current: GoalList? = matchedList
                while (current != null && current.id !in path) {
                    path.add(current.id)
                    visibleIds.add(current.id)
                    current = current.parentId?.let { parentId -> allListsById[parentId] }
                }
            }

            // --- ПОЧАТОК ЗМІН 3: Логіка для показу нащадків ---
            // Крок 2: Якщо опція увімкнена, знаходимо всіх нащадків для кожного збігу (шлях донизу)
            if (shouldShowDescendants) {
                val fullChildMapForTraversal = lists.filter { it.parentId != null }.groupBy { it.parentId!! }
                val descendantsQueue = ArrayDeque(matchingLists)

                while (descendantsQueue.isNotEmpty()) {
                    val current = descendantsQueue.removeFirst()
                    visibleIds.add(current.id)
                    val children = fullChildMapForTraversal[current.id] ?: emptyList()
                    descendantsQueue.addAll(children)
                }
            }
            // --- КІНЕЦЬ ЗМІН 3 ---

            val visibleLists = lists.filter { list -> list.id in visibleIds }

            val filteredChildMap = visibleLists
                .filter { list -> list.parentId != null }
                .groupBy { list -> list.parentId!! }
                .mapValues { entry -> entry.value.sortedBy { child -> child.order } }

            val filteredTopLevelLists = visibleLists
                .filter { list -> list.parentId == null }
                .sortedBy { list -> list.order }

            ChooserUiState(topLevelLists = filteredTopLevelLists, childMap = filteredChildMap)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChooserUiState()
        )


    fun updateFilterText(text: String) {
        _filterText.value = text
        // Логіка розгортання залишається без змін, вона працює коректно
        if (text.isBlank()) {
            _expandedIds.value = emptySet()
        } else {
            viewModelScope.launch(Dispatchers.Default) {
                val lists = allLists.value
                val listMap = lists.associateBy { it.id }
                val matchingLists = lists.filter { it.name.contains(text, ignoreCase = true) }

                val idsToExpand = mutableSetOf<String>()
                matchingLists.forEach { list ->
                    var parentId = list.parentId
                    while (parentId != null) {
                        idsToExpand.add(parentId)
                        parentId = listMap[parentId]?.parentId
                    }
                }
                _expandedIds.value = idsToExpand
            }
        }
    }

    // --- ПОЧАТОК ЗМІН 4: Функція для перемикання опції ---
    fun toggleShowDescendants() {
        _showDescendants.value = !_showDescendants.value
    }
    // --- КІНЕЦЬ ЗМІН 4 ---

    fun toggleExpanded(listId: String) {
        _expandedIds.value = if (listId in _expandedIds.value) {
            _expandedIds.value - listId
        } else {
            _expandedIds.value + listId
        }
    }

    fun addNewList(id: String, parentId: String?, name: String) {
        viewModelScope.launch {
            goalListRepository.createGoalListWithId(id, name, parentId)
        }
    }
}