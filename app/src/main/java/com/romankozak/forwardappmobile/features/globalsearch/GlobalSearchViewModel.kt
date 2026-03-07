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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.Locale
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResultItem> = emptyList(),
    val commandResults: List<OmniboxCommandResult> = emptyList(),
    val hybridCommandResults: List<OmniboxCommandResult> = emptyList(),
    val recentCommands: List<OmniboxCommandId> = emptyList(),
    val mode: OmniboxMode = OmniboxMode.DataSearch,
    val isLoading: Boolean = false,
    val searchHistory: List<String> = emptyList(),
)

enum class OmniboxMode {
    DataSearch,
    Command,
    QuickCatchInbox,
    StartActivity,
    AddActivityEvent,
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
            private const val COMMAND_HISTORY_KEY = "global_command_history"
            private const val COMMAND_USAGE_KEY = "global_command_usage"
            private const val DATA_USAGE_KEY = "global_data_usage"
            private const val MAX_SEARCH_HISTORY = 12
            private const val MAX_COMMAND_HISTORY = 12
            private const val MAX_USAGE_ITEMS = 200
            private const val SEARCH_CANDIDATES_CACHE_TTL_MS = 15_000L
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

        private val querySynonyms =
            mapOf(
                "context" to listOf("contexts", "контекст", "контексти", "ієрархія"),
                "contexts" to listOf("context", "контексти", "ієрархія"),
                "inbox" to listOf("capture", "вхідні", "інбокс"),
                "tracker" to listOf("activity", "активність", "активності", "трекер"),
                "reminders" to listOf("reminder", "нагадування"),
                "attachments" to listOf("attachment", "вкладення"),
                "scripts" to listOf("script", "скрипти", "автоматизація"),
                "notes" to listOf("note", "ноти", "нотатки"),
                "note" to listOf("notes", "нота", "ноти"),
            )

        private var commandUsage: MutableMap<String, Int> =
            savedStateHandle.get<Map<String, Int>>(COMMAND_USAGE_KEY)?.toMutableMap() ?: mutableMapOf()
        private var dataUsage: MutableMap<String, Int> =
            savedStateHandle.get<Map<String, Int>>(DATA_USAGE_KEY)?.toMutableMap() ?: mutableMapOf()

        private val initialQueryRaw: String = savedStateHandle["query"] ?: ""
        private val initialQuery: String = runCatching { URLDecoder.decode(initialQueryRaw, "UTF-8") }.getOrDefault(initialQueryRaw)
        private val _uiState = MutableStateFlow(GlobalSearchUiState())
        val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()
        private var searchJob: Job? = null
        private var allSearchCandidatesCache: List<GlobalSearchResultItem>? = null
        private var allSearchCandidatesCacheUpdatedAt: Long = 0L

        lateinit var enhancedNavigationManager: EnhancedNavigationManager

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        init {
            val history = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
            val recentCommands =
                (savedStateHandle.get<List<String>>(COMMAND_HISTORY_KEY) ?: emptyList())
                    .mapNotNull { name -> runCatching { OmniboxCommandId.valueOf(name) }.getOrNull() }
            _uiState.update {
                it.copy(
                    query = initialQuery,
                    searchHistory = history,
                    recentCommands = recentCommands,
                )
            }
            if (initialQuery.isNotBlank()) {
                performSearch(initialQuery)
            }
        }

        fun goBackToRevealProject(projectId: String) {
            enhancedNavigationManager.goBackWithResult("project_to_reveal", projectId)
        }

        fun onQueryChange(value: String) {
            val (prefixedMode, strippedQuery) = parseModePrefix(value)
            if (prefixedMode != null && prefixedMode != _uiState.value.mode) {
                setMode(prefixedMode)
            }
            _uiState.update { it.copy(query = strippedQuery) }
            searchJob?.cancel()
            if (_uiState.value.mode == OmniboxMode.Command) {
                val commandResults = findCommandResults(strippedQuery)
                _uiState.update {
                    it.copy(
                        commandResults = commandResults,
                        hybridCommandResults = emptyList(),
                        results = emptyList(),
                        isLoading = false,
                    )
                }
                return
            }
            if (_uiState.value.mode != OmniboxMode.DataSearch) {
                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        commandResults = emptyList(),
                        hybridCommandResults = emptyList(),
                        isLoading = false,
                    )
                }
                return
            }
            val hybridCommands = findCommandResults(strippedQuery).take(4)
            _uiState.update { it.copy(hybridCommandResults = hybridCommands, commandResults = emptyList()) }
            searchJob =
                viewModelScope.launch {
                    delay(280)
                    performSearch(strippedQuery)
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
                OmniboxMode.AddActivityEvent -> submitAddActivityEvent(query)
            }
        }

        fun onSubmitSearch(selectedCommandIndex: Int?) {
            if (_uiState.value.mode == OmniboxMode.Command && selectedCommandIndex != null) {
                val commands = _uiState.value.commandResults
                if (selectedCommandIndex in commands.indices) {
                    executeCommand(commands[selectedCommandIndex].id)
                    return
                }
            }
            if (_uiState.value.mode == OmniboxMode.DataSearch && selectedCommandIndex != null) {
                val commands = _uiState.value.hybridCommandResults
                if (selectedCommandIndex in commands.indices) {
                    executeCommand(commands[selectedCommandIndex].id)
                    return
                }
            }
            onSubmitSearch()
        }

        fun setMode(mode: OmniboxMode) {
            _uiState.update { current ->
                current.copy(
                    mode = mode,
                    isLoading = false,
                    results = if (mode == OmniboxMode.DataSearch) current.results else emptyList(),
                    commandResults = if (mode == OmniboxMode.Command) findCommandResults(current.query) else emptyList(),
                    hybridCommandResults = if (mode == OmniboxMode.DataSearch) findCommandResults(current.query).take(4) else emptyList(),
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

        fun onDataResultOpened(resultUniqueId: String) {
            dataUsage[resultUniqueId] = (dataUsage[resultUniqueId] ?: 0) + 1
            trimUsageMap(dataUsage)
            savedStateHandle[DATA_USAGE_KEY] = dataUsage.toMap()
        }

        fun quickCatchCurrentQuery() {
            submitQuickCatch(_uiState.value.query)
        }

        fun startActivityFromCurrentQuery() {
            submitStartActivity(_uiState.value.query)
        }

        fun addActivityEventFromCurrentQuery() {
            submitAddActivityEvent(_uiState.value.query)
        }

        fun runBestCommandForCurrentQuery() {
            val best = findCommandResults(_uiState.value.query).firstOrNull() ?: return
            executeCommand(best.id)
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
                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        isLoading = false,
                        commandResults = emptyList(),
                        hybridCommandResults = emptyList(),
                    )
                }
                return
            }

            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val strictResults = contextRepository.searchGlobal("%$query%")
                val strictDistinct = strictResults.distinctBy { it.uniqueId }
                val rankedStrict =
                    strictDistinct
                        .mapNotNull { item ->
                            val score = fuzzyScoreForData(query, item)
                            if (score < 0) null else item to score
                        }
                        .sortedWith(compareByDescending<Pair<GlobalSearchResultItem, Int>> { it.second }.thenByDescending { it.first.timestamp })
                        .map { it.first }

                val finalResults =
                    if (rankedStrict.isNotEmpty()) {
                        rankedStrict
                    } else {
                        val allCandidates = getAllSearchCandidatesForFuzzy()
                        allCandidates
                            .mapNotNull { item ->
                                val score = fuzzyScoreForData(query, item)
                                if (score < 10) null else item to score
                            }
                            .sortedWith(compareByDescending<Pair<GlobalSearchResultItem, Int>> { it.second }.thenByDescending { it.first.timestamp })
                            .map { it.first }
                            .take(120)
                    }
                _uiState.update {
                    it.copy(
                        results = finalResults,
                        isLoading = false,
                        commandResults = emptyList(),
                        hybridCommandResults = findCommandResults(query).take(4),
                    )
                }
            }
        }

        private fun submitQuickCatch(rawQuery: String) {
            val text = rawQuery.trim()
            if (text.isBlank()) return
            viewModelScope.launch {
                val inboxContextId = resolveInboxContextId() ?: return@launch
                inboxRepository.addInboxRecord(text = text, contextId = inboxContextId)
                invalidateSearchCandidatesCache()
                _uiState.update { it.copy(query = "") }
            }
        }

        private fun submitStartActivity(rawQuery: String) {
            val text = rawQuery.trim()
            if (text.isBlank()) return
            viewModelScope.launch {
                activityRepository.startActivity(text, System.currentTimeMillis())
                invalidateSearchCandidatesCache()
                _uiState.update { it.copy(query = "") }
                enhancedNavigationManager.navigate(target = NavTarget.Tracker)
            }
        }

        private fun submitAddActivityEvent(rawQuery: String) {
            val text = rawQuery.trim()
            if (text.isBlank()) return
            viewModelScope.launch {
                activityRepository.addCompletedActivity(
                    text = text,
                    xpGained = null,
                    antyXp = null,
                )
                invalidateSearchCandidatesCache()
                _uiState.update { it.copy(query = "") }
                enhancedNavigationManager.navigate(target = NavTarget.Tracker)
            }
        }

        private fun executeCommand(commandId: OmniboxCommandId) {
            rememberCommandExecuted(commandId)
            when (commandId) {
                OmniboxCommandId.OpenContexts -> enhancedNavigationManager.navigate(target = NavTarget.ContextHierarchy, recordInHistory = true)
                OmniboxCommandId.OpenInbox ->
                    viewModelScope.launch {
                        val inboxContextId = resolveInboxContextId() ?: return@launch
                        enhancedNavigationManager.navigate(
                            target =
                                NavTarget.ContextDetail(
                                    contextId = inboxContextId,
                                    initialViewMode = "INBOX",
                                ),
                            recordInHistory = true,
                            historyTitle = "Inbox",
                        )
                    }
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
                val recentOrder = _uiState.value.recentCommands.withIndex().associate { it.value to it.index }
                return commandDefinitions.map {
                    val usageBoost = (commandUsage[it.id.name] ?: 0) * 12
                    val recencyBoost = recentOrder[it.id]?.let { idx -> 140 - idx * 20 } ?: 0
                    OmniboxCommandResult(
                        id = it.id,
                        title = it.title,
                        subtitle = it.subtitle,
                        score = usageBoost + recencyBoost,
                    )
                }.sortedByDescending { it.score }
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
                            score = score + (commandUsage[definition.id.name] ?: 0) * 10,
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
            val expandedQueries = expandQueryWithSynonyms(query)
            return candidates.maxOfOrNull { candidate ->
                expandedQueries.maxOfOrNull { candidateQuery -> fuzzyScore(candidateQuery, candidate) } ?: -1
            } ?: -1
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
            if (qIndex == query.length) return score

            // Typo-tolerant fallback: allow small edit distance against tokens (e.g. "нотм" -> "ноти").
            val typoScore = typoToleranceScore(query, text)
            return if (typoScore >= 0) typoScore else -1
        }

        private fun typoToleranceScore(
            query: String,
            text: String,
        ): Int {
            if (query.length < 3) return -1
            val tokens = text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return -1

            val maxDistance =
                when {
                    query.length <= 4 -> 1
                    query.length <= 8 -> 2
                    else -> 3
                }

            var bestDistance = Int.MAX_VALUE
            for (token in tokens) {
                if (kotlin.math.abs(token.length - query.length) > maxDistance) continue
                val distance = damerauLevenshteinDistanceBounded(query, token, maxDistance)
                if (distance < bestDistance) {
                    bestDistance = distance
                    if (bestDistance == 0) break
                }
            }

            if (bestDistance == Int.MAX_VALUE || bestDistance > maxDistance) return -1
            return 220 - bestDistance * 60
        }

        private fun damerauLevenshteinDistanceBounded(
            a: String,
            b: String,
            maxDistance: Int,
        ): Int {
            if (a == b) return 0
            if (kotlin.math.abs(a.length - b.length) > maxDistance) return maxDistance + 1

            val rows = a.length + 1
            val cols = b.length + 1
            val d = Array(rows) { IntArray(cols) }
            for (i in 0 until rows) d[i][0] = i
            for (j in 0 until cols) d[0][j] = j

            for (i in 1 until rows) {
                var rowMin = Int.MAX_VALUE
                for (j in 1 until cols) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    var value = minOf(
                        d[i - 1][j] + 1,
                        d[i][j - 1] + 1,
                        d[i - 1][j - 1] + cost,
                    )
                    if (
                        i > 1 && j > 1 &&
                        a[i - 1] == b[j - 2] &&
                        a[i - 2] == b[j - 1]
                    ) {
                        value = minOf(value, d[i - 2][j - 2] + 1)
                    }
                    d[i][j] = value
                    rowMin = minOf(rowMin, value)
                }
                if (rowMin > maxDistance) return maxDistance + 1
            }
            return d[a.length][b.length]
        }

        private suspend fun getAllSearchCandidatesForFuzzy(): List<GlobalSearchResultItem> {
            val now = System.currentTimeMillis()
            allSearchCandidatesCache?.let { cached ->
                if (now - allSearchCandidatesCacheUpdatedAt <= SEARCH_CANDIDATES_CACHE_TTL_MS) {
                    return cached
                }
            }
            val allCandidates = contextRepository.searchGlobal("%%").distinctBy { it.uniqueId }
            allSearchCandidatesCache = allCandidates
            allSearchCandidatesCacheUpdatedAt = now
            return allCandidates
        }

        private fun invalidateSearchCandidatesCache() {
            allSearchCandidatesCache = null
            allSearchCandidatesCacheUpdatedAt = 0L
        }

        private fun fuzzyScoreForData(
            query: String,
            item: GlobalSearchResultItem,
        ): Int {
            val fields = searchableFields(item)
            val expandedQueries = expandQueryWithSynonyms(query)
            val best =
                fields.maxOfOrNull { field ->
                    expandedQueries.maxOfOrNull { variant -> fuzzyScore(variant, field) } ?: -1
                } ?: -1
            if (best < 0) return -1
            val prefixBonus =
                fields.maxOfOrNull { field ->
                    expandedQueries.maxOfOrNull { variant ->
                        val idx = field.lowercase(Locale.getDefault()).indexOf(variant.lowercase(Locale.getDefault()))
                        if (idx == 0) 180 else if (idx in 1..3) 120 else 0
                    } ?: 0
                } ?: 0
            val usageBoost = (dataUsage[item.uniqueId] ?: 0) * 14
            return best + prefixBonus + usageBoost
        }

        private fun searchableFields(item: GlobalSearchResultItem): List<String> =
            when (item) {
                is GlobalSearchResultItem.GoalItem ->
                    listOf(
                        item.goal.text,
                        item.goal.description ?: "",
                        item.projectName,
                        item.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.LinkItem ->
                    listOf(
                        item.searchResult.link.linkData.displayName ?: "",
                        item.searchResult.link.linkData.target,
                        item.searchResult.contextName,
                        item.searchResult.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.SubcontextItem ->
                    listOf(
                        item.searchResult.subcontext.name,
                        item.searchResult.parentContextName,
                        item.searchResult.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.ContextItem ->
                    listOf(
                        item.searchResult.context.name,
                        item.searchResult.context.description ?: "",
                        item.searchResult.pathSegments.joinToString(" "),
                    )
                is GlobalSearchResultItem.ActivityItem ->
                    listOf(
                        item.record.text,
                        item.record.noteText ?: "",
                    )
                is GlobalSearchResultItem.InboxItem ->
                    listOf(item.record.text)
                is GlobalSearchResultItem.AttachmentItem ->
                    listOf(
                        item.searchResult.title,
                        item.searchResult.subtitle ?: "",
                        item.searchResult.contextName ?: "",
                    )
            }

        private suspend fun resolveInboxContextId(): String? {
            val allContexts = contextRepository.getAllContextsFlow().first()
            return allContexts.firstOrNull { it.id == SystemContexts.INBOX.raw }?.id
                ?: allContexts.firstOrNull {
                    it.name.equals("Inbox", ignoreCase = true) && it.id != SystemContexts.TODAY.raw
                }?.id
        }

        private fun parseModePrefix(rawValue: String): Pair<OmniboxMode?, String> {
            if (rawValue.isBlank()) return null to rawValue
            val first = rawValue.first()
            val stripped = rawValue.drop(1).trimStart()
            return when (first) {
                '>' -> OmniboxMode.Command to stripped
                '/' -> OmniboxMode.DataSearch to stripped
                '+' -> OmniboxMode.QuickCatchInbox to stripped
                '!' -> OmniboxMode.StartActivity to stripped
                '=' -> OmniboxMode.AddActivityEvent to stripped
                else -> null to rawValue
            }
        }

        private fun expandQueryWithSynonyms(query: String): Set<String> {
            val normalized = query.trim().lowercase(Locale.getDefault())
            if (normalized.isBlank()) return setOf(normalized)
            val directSynonyms = querySynonyms[normalized].orEmpty()
            val reverseSynonyms =
                querySynonyms
                    .filterValues { values -> values.any { it.equals(normalized, ignoreCase = true) } }
                    .keys
                    .toList()
            return (listOf(normalized) + directSynonyms + reverseSynonyms).toSet()
        }

        private fun rememberCommandExecuted(commandId: OmniboxCommandId) {
            commandUsage[commandId.name] = (commandUsage[commandId.name] ?: 0) + 1
            trimUsageMap(commandUsage)
            savedStateHandle[COMMAND_USAGE_KEY] = commandUsage.toMap()

            val currentHistory = _uiState.value.recentCommands
            val updated = (listOf(commandId) + currentHistory.filterNot { it == commandId }).take(MAX_COMMAND_HISTORY)
            _uiState.update { it.copy(recentCommands = updated) }
            savedStateHandle[COMMAND_HISTORY_KEY] = updated.map { it.name }
        }

        private fun trimUsageMap(usage: MutableMap<String, Int>) {
            if (usage.size <= MAX_USAGE_ITEMS) return
            val top =
                usage.entries
                    .sortedByDescending { it.value }
                    .take(MAX_USAGE_ITEMS)
                    .associate { it.key to it.value }
            usage.clear()
            usage.putAll(top)
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
