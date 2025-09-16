package com.romankozak.forwardappmobile.ui.screens.mainscreen.delegates

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.screens.mainscreen.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.GoalListUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.SearchResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchDelegate(
    private val savedStateHandle: SavedStateHandle,
    private val uiEventChannel: Channel<GoalListUiEvent>
) {
    companion object {
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val MAX_SEARCH_HISTORY = 10
    }

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()

    val searchHistory: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(SEARCH_HISTORY_KEY, emptyList())

    suspend fun onToggleSearch(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (isActive) {
            val currentText = _searchQuery.value.text
            _searchQuery.value = TextFieldValue(currentText, TextRange(0, currentText.length))
            uiEventChannel.send(GoalListUiEvent.FocusSearchField)
        } else {
            val query = _searchQuery.value.text
            if (query.isNotBlank()) {
                addSearchQueryToHistory(query)
            }
            _searchQuery.value = TextFieldValue("")
        }
    }

    fun onSearchQueryChanged(query: TextFieldValue) {
        _searchQuery.value = query
    }

    fun onSearchQueryFromHistory(query: String) {
        _searchQuery.value = TextFieldValue(query)
    }

    suspend fun updateSearchResults(
        query: String,
        isSearchActive: Boolean,
        allLists: List<GoalList>,
        fullHierarchy: ListHierarchyData
    ) {
        if (isSearchActive && query.isNotBlank()) {
            val matchingLists = if (query.length > 3) {
                allLists.filter { fuzzyMatch(query, it.name) }
            } else {
                allLists.filter { it.name.contains(query, ignoreCase = true) }
            }

            val results = matchingLists.map { list ->
                SearchResult(
                    list = list,
                    path = buildPathToList(list.id, fullHierarchy)
                )
            }
            _searchResults.value = results.sortedBy { it.list.name }
        } else {
            _searchResults.value = emptyList()
        }
    }

    suspend fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            addSearchQueryToHistory(query)
            uiEventChannel.send(GoalListUiEvent.NavigateToGlobalSearch(query))
            onDismissSearchDialog()
        }
    }

    fun onShowSearchDialog() { _showSearchDialog.value = true }
    fun onDismissSearchDialog() { _showSearchDialog.value = false }

    private fun addSearchQueryToHistory(query: String) {
        val currentHistory = savedStateHandle[SEARCH_HISTORY_KEY] ?: emptyList<String>()
        val mutableHistory = currentHistory.toMutableList()

        val existingIndex = mutableHistory.indexOfFirst { it.lowercase() == query.lowercase() }
        if (existingIndex != -1) {
            mutableHistory.removeAt(existingIndex)
        }

        mutableHistory.add(0, query)
        val newHistory = mutableHistory.take(MAX_SEARCH_HISTORY)
        savedStateHandle[SEARCH_HISTORY_KEY] = newHistory
    }

    private fun fuzzyMatch(query: String, text: String): Boolean {
        if (query.isBlank()) return true
        if (text.isBlank()) return false
        val lowerQuery = query.lowercase()
        val lowerText = text.lowercase()
        var queryIndex = 0
        var textIndex = 0
        while (queryIndex < lowerQuery.length && textIndex < lowerText.length) {
            if (lowerQuery[queryIndex] == lowerText[textIndex]) {
                queryIndex++
            }
            textIndex++
        }
        return queryIndex == lowerQuery.length
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
}