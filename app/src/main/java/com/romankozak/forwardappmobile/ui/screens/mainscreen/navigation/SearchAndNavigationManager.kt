package com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.buildPathToProject
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- NEW: Результат операції "показати локацію" ---
sealed class RevealResult {
    // Успіх: повертає ID проекту та прапорець, чи потрібен фокус-режим
    data class Success(val projectId: String, val shouldFocus: Boolean) : RevealResult()
    object Failure : RevealResult()
}

class SearchAndNavigationManager(
    private val projectRepository: ProjectRepository,
    private val scope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val uiEventChannel: Channel<ProjectUiEvent>,
    private val allProjectsFlat: StateFlow<List<Project>>
) {

    companion object {
        private const val TAG = "SNM_DEBUG"
        // --- NEW: Поріг глибини для активації фокус-режиму ---
        // Якщо у проекта 2 або більше предків (тобто він на 3-му рівні або глибше),
        // вмикається фокус-режим.
        private const val FOCUS_DEPTH_THRESHOLD = 2

        // Ключі для SavedStateHandle
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val MAX_SEARCH_HISTORY = 10
        private const val ACTIVE_SEARCH_QUERY_TEXT_KEY = "active_search_query_text"
        private const val IS_SEARCH_ACTIVE_KEY = "is_search_active"
    }

    // Прапорець, що блокує фільтрацію до завершення відновлення стану
    private val _isPendingStateRestoration = MutableStateFlow(false)
    val isPendingStateRestoration = _isPendingStateRestoration.asStateFlow()

    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    val highlightedProjectId = MutableStateFlow<String?>(null)
    val currentBreadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
    val focusedProjectId = MutableStateFlow<String?>(null)
    val hierarchySettings = MutableStateFlow(HierarchyDisplaySettings())

    val searchHistory: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(SEARCH_HISTORY_KEY, emptyList())

    init {
        initializeSearchState()
    }

    private fun initializeSearchState() {
        scope.launch {
            val savedIsActive = savedStateHandle.get<Boolean>(IS_SEARCH_ACTIVE_KEY) ?: false
            if (savedIsActive) {
                _isPendingStateRestoration.value = true
                val savedQueryText = savedStateHandle.get<String>(ACTIVE_SEARCH_QUERY_TEXT_KEY) ?: ""
                _searchQuery.value = TextFieldValue(savedQueryText, TextRange(savedQueryText.length))
                _isSearchActive.value = true
                delay(50) // Даємо UI час на оновлення
                _isPendingStateRestoration.value = false
            }
        }
    }

    fun onToggleSearch(isActive: Boolean) {
        savedStateHandle[IS_SEARCH_ACTIVE_KEY] = isActive
        _isSearchActive.value = isActive
        if (isActive) {
            val currentText = _searchQuery.value.text
            _searchQuery.value = TextFieldValue(currentText, TextRange(currentText.length))
            scope.launch { uiEventChannel.send(ProjectUiEvent.FocusSearchField) }
        } else {
            if (_searchQuery.value.text.isNotBlank()) {
                addSearchQueryToHistory(_searchQuery.value.text)
            }
            _searchQuery.value = TextFieldValue("")
        }
    }

    fun onSearchQueryChanged(query: TextFieldValue) {
        savedStateHandle[ACTIVE_SEARCH_QUERY_TEXT_KEY] = query.text
        _searchQuery.value = query
    }

    fun onSearchQueryFromHistory(query: String) {
        onSearchQueryChanged(TextFieldValue(query, TextRange(query.length)))
        onToggleSearch(true)
    }

    /**
     * --- NEW: Повністю очищує весь стан, пов'язаний з пошуком. ---
     * Використовується для гарантованого повернення до початкового стану (напр. кнопка Home).
     */
    fun clearAllSearchState() {
        savedStateHandle[IS_SEARCH_ACTIVE_KEY] = false
        savedStateHandle[ACTIVE_SEARCH_QUERY_TEXT_KEY] = ""
        _searchQuery.value = TextFieldValue("")
        _isSearchActive.value = false
    }

    /**
     * --- ENHANCED: Функція "Показати локацію", яка вирішує, чи потрібен фокус-режим. ---
     */
    suspend fun revealProjectInHierarchy(projectId: String): RevealResult {
        Log.d(TAG, "Початок 'revealProjectInHierarchy' для ID: $projectId")
        return withContext(Dispatchers.IO) {
            try {
                // 1. Негайно вимикаємо режим пошуку
                withContext(Dispatchers.Main.immediate) {
                    if (_isSearchActive.value) {
                        _isSearchActive.value = false
                        _searchQuery.value = TextFieldValue("")
                    }
                }

                // 2. Шукаємо предків та визначаємо глибину
                val projectLookup = allProjectsFlat.value.associateBy { it.id }
                val ancestorIds = mutableSetOf<String>()
                findAncestorsRecursive(projectId, projectLookup, ancestorIds, mutableSetOf())

                // 3. Вирішуємо, чи потрібен фокус-режим
                val shouldFocus = ancestorIds.size >= FOCUS_DEPTH_THRESHOLD
                Log.d(TAG, "Глибина проекту: ${ancestorIds.size}. Потрібен фокус-режим: $shouldFocus")

                // 4. Розгортаємо предків у БД, якщо потрібно
                val projectsToExpand = ancestorIds
                    .mapNotNull { projectLookup[it] }
                    .filter { !it.isExpanded }

                if (projectsToExpand.isNotEmpty()) {
                    projectRepository.updateProjects(projectsToExpand.map { it.copy(isExpanded = true) })
                    // **Критично**: Чекаємо, доки зміни з БД дійдуть до UI
                    allProjectsFlat.first { currentProjects ->
                        projectsToExpand.all { toExpand ->
                            currentProjects.find { it.id == toExpand.id }?.isExpanded == true
                        }
                    }
                    Log.d(TAG, "Всі предки гарантовано розгорнуті.")
                }

                // 5. Повністю очищуємо збережений стан пошуку
                withContext(Dispatchers.Main.immediate) {
                    clearAllSearchState()
                }

                RevealResult.Success(projectId, shouldFocus)

            } catch (e: Exception) {
                Log.e(TAG, "Помилка в revealProjectInHierarchy", e)
                RevealResult.Failure
            }
        }
    }

    fun navigateToProject(projectId: String, projectHierarchy: StateFlow<ListHierarchyData>) {
        scope.launch {
            val path = buildPathToProject(projectId, projectHierarchy.value)
            currentBreadcrumbs.value = path
            focusedProjectId.value = projectId
        }
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
        currentBreadcrumbs.update { it.take(breadcrumbItem.level + 1) }
        focusedProjectId.value = breadcrumbItem.id
    }

    fun clearNavigation() {
        focusedProjectId.value = null
        currentBreadcrumbs.value = emptyList()
    }

    private fun addSearchQueryToHistory(query: String) {
        if (query.isBlank()) return

        val currentHistory = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
        val mutableHistory = currentHistory.toMutableList()

        val existingIndex = mutableHistory.indexOfFirst { it.equals(query, ignoreCase = true) }
        if (existingIndex != -1) {
            mutableHistory.removeAt(existingIndex)
        }

        mutableHistory.add(0, query)
        val newHistory = mutableHistory.take(MAX_SEARCH_HISTORY)
        savedStateHandle[SEARCH_HISTORY_KEY] = newHistory
    }
}