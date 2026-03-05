package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResultItem> = emptyList(),
    val commandResults: List<OmniboxCommandResult> = emptyList(),
    val mode: OmniboxMode = OmniboxMode.DataSearch,
    val isLoading: Boolean = false,
    val searchHistory: List<String> = emptyList(),
)

enum class OmniboxMode {
    DataSearch,
    Command,
    QuickCatchInbox,
    StartActivity,
}

enum class OmniboxCommandId {
    OpenContexts,
    OpenInbox,
    OpenTracker,
    OpenReminders,
    OpenSettings,
    OpenSearch,
    OpenAttachments,
    OpenScripts,
    OpenAiChat,
    OpenAiInsights,
    OpenAiLife,
    OpenStructurePresets,
}

data class OmniboxCommandResult(
    val id: OmniboxCommandId,
    val title: String,
    val subtitle: String,
    val score: Int,
)

private data class OmniboxCommandDefinition(
    val id: OmniboxCommandId,
    val title: String,
    val subtitle: String,
    val keywords: List<String>,
)

@HiltViewModel
class GlobalSearchViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val inboxRepository: InboxRepository,
        private val activityRepository: ActivityRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        companion object {
        private const val SEARCH_HISTORY_KEY = "global_search_history"
        private const val MAX_SEARCH_HISTORY = 12
    }

        private val commandDefinitions =
            listOf(
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenContexts,
                    title = "Відкрити ієрархію контекстів",
                    subtitle = "Екран Contexts",
                    keywords = listOf("contexts", "hierarchy", "контексти", "ієрархія", "goal lists"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenInbox,
                    title = "Відкрити Inbox",
                    subtitle = "Контекст inbox у режимі INBOX",
                    keywords = listOf("inbox", "capture", "вхідні", "інбокс"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenTracker,
                    title = "Відкрити Activity Tracker",
                    subtitle = "Екран активностей",
                    keywords = listOf("tracker", "activity", "активності", "трекер"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenReminders,
                    title = "Відкрити Reminders",
                    subtitle = "Екран нагадувань",
                    keywords = listOf("reminders", "нагадування"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenSettings,
                    title = "Відкрити Settings",
                    subtitle = "Налаштування застосунку",
                    keywords = listOf("settings", "налаштування", "preferences"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenSearch,
                    title = "Відкрити Search Home",
                    subtitle = "Головний екран пошуку",
                    keywords = listOf("search", "пошук", "global search"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenAttachments,
                    title = "Відкрити Attachments Library",
                    subtitle = "Бібліотека вкладень",
                    keywords = listOf("attachments", "вкладення", "library"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenScripts,
                    title = "Відкрити Scripts Library",
                    subtitle = "Бібліотека скриптів",
                    keywords = listOf("scripts", "автоматизація", "скрипти"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenAiChat,
                    title = "Відкрити AI Chat",
                    subtitle = "Робочий чат",
                    keywords = listOf("ai chat", "chat", "чати", "чат"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenAiInsights,
                    title = "Відкрити AI Insights",
                    subtitle = "Аналітика",
                    keywords = listOf("ai insights", "insights", "аналітика"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenAiLife,
                    title = "Відкрити AI Life",
                    subtitle = "Life-management",
                    keywords = listOf("ai life", "life", "life-management", "lifestate"),
                ),
                OmniboxCommandDefinition(
                    id = OmniboxCommandId.OpenStructurePresets,
                    title = "Відкрити Structure Presets",
                    subtitle = "Пресети структури",
                    keywords = listOf("presets", "structure", "пресети", "структура"),
                ),
            )

        private val initialQueryRaw: String = savedStateHandle["query"] ?: ""
        private val initialQuery: String = runCatching { URLDecoder.decode(initialQueryRaw, "UTF-8") }.getOrDefault(initialQueryRaw)
        private val _uiState = MutableStateFlow(GlobalSearchUiState())
        val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()
        private var searchJob: Job? = null

        lateinit var enhancedNavigationManager: EnhancedNavigationManager

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        init {
            val history = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
            _uiState.update { it.copy(query = initialQuery, searchHistory = history) }
            if (initialQuery.isNotBlank()) {
                performSearch(initialQuery)
            }
        }

        fun goBackToRevealProject(projectId: String) {
            enhancedNavigationManager.goBackWithResult("project_to_reveal", projectId)
        }

        fun onQueryChange(value: String) {
            _uiState.update { it.copy(query = value) }
            searchJob?.cancel()
            if (_uiState.value.mode == OmniboxMode.Command) {
                val commandResults = findCommandResults(value)
                _uiState.update { it.copy(commandResults = commandResults, results = emptyList(), isLoading = false) }
                return
            }
            if (_uiState.value.mode != OmniboxMode.DataSearch) {
                _uiState.update { it.copy(results = emptyList(), commandResults = emptyList(), isLoading = false) }
                return
            }
            searchJob =
                viewModelScope.launch {
                    delay(280)
                    performSearch(value)
                }
        }

        fun onSubmitSearch() {
            val query = _uiState.value.query
            when (_uiState.value.mode) {
                OmniboxMode.DataSearch -> {
                    addSearchQueryToHistory(query)
                    performSearch(query)
                }
                OmniboxMode.Command -> {
                    val commands = findCommandResults(query)
                    if (commands.isNotEmpty()) {
                        executeCommand(commands.first().id)
                    }
                }
                OmniboxMode.QuickCatchInbox -> submitQuickCatch(query)
                OmniboxMode.StartActivity -> submitStartActivity(query)
            }
        }

        fun setMode(mode: OmniboxMode) {
            _uiState.update { current ->
                current.copy(
                    mode = mode,
                    isLoading = false,
                    results = if (mode == OmniboxMode.DataSearch) current.results else emptyList(),
                    commandResults = if (mode == OmniboxMode.Command) findCommandResults(current.query) else emptyList(),
                )
            }
            if (mode == OmniboxMode.DataSearch && _uiState.value.query.isNotBlank()) {
                performSearch(_uiState.value.query)
            }
        }

        fun cycleMode(forward: Boolean) {
            val allModes = OmniboxMode.entries
            val currentIndex = allModes.indexOf(_uiState.value.mode).coerceAtLeast(0)
            val nextIndex =
                if (forward) {
                    (currentIndex + 1) % allModes.size
                } else {
                    (currentIndex - 1 + allModes.size) % allModes.size
                }
            setMode(allModes[nextIndex])
        }

        fun onCommandClick(commandId: OmniboxCommandId) {
            executeCommand(commandId)
        }

        fun onSelectHistoryQuery(query: String) {
            _uiState.update { it.copy(query = query) }
            addSearchQueryToHistory(query)
            performSearch(query)
        }

        fun removeSearchHistoryEntry(query: String) {
            if (query.isBlank()) return
            val updated = _uiState.value.searchHistory.filterNot { it.equals(query, ignoreCase = true) }
            _uiState.update { it.copy(searchHistory = updated) }
            savedStateHandle[SEARCH_HISTORY_KEY] = updated
        }

        fun clearSearchHistory() {
            _uiState.update { it.copy(searchHistory = emptyList()) }
            savedStateHandle[SEARCH_HISTORY_KEY] = emptyList<String>()
        }

        private fun performSearch(rawQuery: String) {
            val query = rawQuery.trim()
            if (query.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), isLoading = false, commandResults = emptyList()) }
                return
            }

            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val results = contextRepository.searchGlobal("%$query%")
                val distinctResults = results.distinctBy { it.uniqueId }
                _uiState.update {
                    it.copy(results = distinctResults, isLoading = false, commandResults = emptyList())
                }
            }
        }

        private fun submitQuickCatch(rawQuery: String) {
            val text = rawQuery.trim()
            if (text.isBlank()) return
            viewModelScope.launch {
                inboxRepository.addInboxRecord(text = text, contextId = SystemContexts.INBOX.raw)
                _uiState.update { it.copy(query = "") }
                enhancedNavigationManager.navigate(
                    target =
                        NavTarget.ContextDetail(
                            contextId = SystemContexts.INBOX.raw,
                            initialViewMode = "INBOX",
                        ),
                    recordInHistory = true,
                    historyTitle = "Inbox",
                )
            }
        }

        private fun submitStartActivity(rawQuery: String) {
            val text = rawQuery.trim()
            if (text.isBlank()) return
            viewModelScope.launch {
                activityRepository.startActivity(text, System.currentTimeMillis())
                _uiState.update { it.copy(query = "") }
                enhancedNavigationManager.navigate(target = NavTarget.Tracker)
            }
        }

        private fun executeCommand(commandId: OmniboxCommandId) {
            when (commandId) {
                OmniboxCommandId.OpenContexts -> enhancedNavigationManager.navigate(target = NavTarget.ContextHierarchy, recordInHistory = true)
                OmniboxCommandId.OpenInbox ->
                    enhancedNavigationManager.navigate(
                        target =
                            NavTarget.ContextDetail(
                                contextId = SystemContexts.INBOX.raw,
                                initialViewMode = "INBOX",
                            ),
                        recordInHistory = true,
                        historyTitle = "Inbox",
                    )
                OmniboxCommandId.OpenTracker -> enhancedNavigationManager.navigate(target = NavTarget.Tracker)
                OmniboxCommandId.OpenReminders -> enhancedNavigationManager.navigate(target = NavTarget.Reminders)
                OmniboxCommandId.OpenSettings -> enhancedNavigationManager.navigate(target = NavTarget.Settings)
                OmniboxCommandId.OpenSearch -> enhancedNavigationManager.navigate(target = NavTarget.GlobalSearchHome)
                OmniboxCommandId.OpenAttachments -> enhancedNavigationManager.navigate(target = NavTarget.AttachmentsLibrary)
                OmniboxCommandId.OpenScripts -> enhancedNavigationManager.navigate(target = NavTarget.ScriptsLibrary)
                OmniboxCommandId.OpenAiChat -> enhancedNavigationManager.navigate(target = NavTarget.Chat)
                OmniboxCommandId.OpenAiInsights -> enhancedNavigationManager.navigate(target = NavTarget.AiInsights)
                OmniboxCommandId.OpenAiLife -> enhancedNavigationManager.navigate(target = NavTarget.LifeState)
                OmniboxCommandId.OpenStructurePresets -> enhancedNavigationManager.navigate(target = NavTarget.StructurePresets)
            }
        }

        private fun findCommandResults(rawQuery: String): List<OmniboxCommandResult> {
            val query = rawQuery.trim()
            if (query.isBlank()) {
                return commandDefinitions.map {
                    OmniboxCommandResult(
                        id = it.id,
                        title = it.title,
                        subtitle = it.subtitle,
                        score = 0,
                    )
                }
            }
            return commandDefinitions
                .mapNotNull { definition ->
                    val score = commandScore(query, definition)
                    if (score < 0) {
                        null
                    } else {
                        OmniboxCommandResult(
                            id = definition.id,
                            title = definition.title,
                            subtitle = definition.subtitle,
                            score = score,
                        )
                    }
                }
                .sortedByDescending { it.score }
                .take(30)
        }

        private fun commandScore(
            query: String,
            command: OmniboxCommandDefinition,
        ): Int {
            val candidates = buildList {
                add(command.title)
                add(command.subtitle)
                addAll(command.keywords)
            }
            return candidates.maxOfOrNull { fuzzyScore(query, it) } ?: -1
        }

        private fun fuzzyScore(
            rawQuery: String,
            rawText: String,
        ): Int {
            val query = rawQuery.lowercase().trim()
            val text = rawText.lowercase()
            if (query.isBlank()) return 0

            val directIndex = text.indexOf(query)
            if (directIndex >= 0) {
                return 1000 - directIndex
            }

            var qIndex = 0
            var score = 0
            var lastMatchIndex = -2
            for (i in text.indices) {
                if (qIndex >= query.length) break
                if (text[i] == query[qIndex]) {
                    score += 3
                    if (i == lastMatchIndex + 1) score += 2
                    lastMatchIndex = i
                    qIndex++
                }
            }
            return if (qIndex == query.length) score else -1
        }

        private fun addSearchQueryToHistory(rawQuery: String) {
            val query = rawQuery.trim()
            if (query.isBlank()) return

            val currentHistory = _uiState.value.searchHistory
            val withoutCurrent = currentHistory.filterNot { it.equals(query, ignoreCase = true) }
            val updated = (listOf(query) + withoutCurrent).take(MAX_SEARCH_HISTORY)

            _uiState.update { it.copy(searchHistory = updated) }
            savedStateHandle[SEARCH_HISTORY_KEY] = updated
        }

        fun navigateToProjectForResult(
            projectId: String,
            projectName: String?,
        ) {
            viewModelScope.launch {
                val finalProjectName = projectName ?: contextRepository.getContextById(projectId)?.name ?: "Context"
                enhancedNavigationManager.navigateToProject(projectId, finalProjectName)
            }
        }
    }
