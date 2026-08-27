package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.search.StructuredSearchQuery
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResultItem> = emptyList(),
    val selectedTypes: Set<GlobalSearchType> = GlobalSearchType.entries.toSet(),
    val commandResults: List<OmniboxCommandResult> = emptyList(),
    val hybridCommandResults: List<OmniboxCommandResult> = emptyList(),
    val recentCommands: List<OmniboxCommandId> = emptyList(),
    val mode: OmniboxMode = OmniboxMode.DataSearch,
    val isLoading: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val modeDisplayPrefs: OmniboxModeDisplayPrefsState = OmniboxModeDisplayPrefsState(),
    val recentModeInputs: Map<OmniboxMode, List<String>> = emptyMap(),
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
        private val reminderRepository: ReminderRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        companion object {
            private const val SEARCH_HISTORY_KEY = "global_search_history"
            private const val COMMAND_HISTORY_KEY = "global_command_history"
            private const val COMMAND_USAGE_KEY = "global_command_usage"
            private const val DATA_USAGE_KEY = "global_data_usage"
            private const val FLOW_STOP_TIMEOUT_MILLIS = 5000L
            private const val MAX_SEARCH_HISTORY = 12
            private const val MAX_COMMAND_HISTORY = 12
            private const val MAX_USAGE_ITEMS = 200
            private const val SEARCH_CANDIDATES_CACHE_TTL_MS = 15_000L
            private const val HYBRID_COMMANDS_LIMIT = 4
            private const val MIN_FUZZY_DATA_SCORE = 10
            private const val MAX_FUZZY_RESULTS = 120
            private const val EMPTY_QUERY_SCORE = 0
            private const val INITIAL_LAST_MATCH_INDEX = -2
            private const val COMMAND_USAGE_BOOST_MULTIPLIER = 12
            private const val COMMAND_RECENCY_BASE_BOOST = 140
            private const val COMMAND_RECENCY_STEP = 20
            private const val COMMAND_MATCH_USAGE_BOOST_MULTIPLIER = 10
            private const val MAX_COMMAND_RESULTS = 30
            private const val DIRECT_MATCH_BASE_SCORE = 1000
            private const val CHARACTER_MATCH_SCORE = 3
            private const val CONSECUTIVE_CHARACTER_BONUS = 2
            private const val NO_MATCH_SCORE = -1
            private const val MIN_TYPO_TOLERANCE_QUERY_LENGTH = 3
            private const val SHORT_QUERY_MAX_LENGTH = 4
            private const val MEDIUM_QUERY_MAX_LENGTH = 8
            private const val SHORT_QUERY_MAX_DISTANCE = 1
            private const val MEDIUM_QUERY_MAX_DISTANCE = 2
            private const val LONG_QUERY_MAX_DISTANCE = 3
            private const val TYPO_MATCH_BASE_SCORE = 220
            private const val TYPO_DISTANCE_PENALTY = 60
            private const val PREFIX_MATCH_BONUS = 180
            private const val NEAR_PREFIX_START_INDEX = 1
            private const val NEAR_PREFIX_END_INDEX = 3
            private const val NEAR_PREFIX_BONUS = 120
            private const val DATA_USAGE_BOOST_MULTIPLIER = 14
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
                    title = "Відкрити Life Journal",
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
        private val initialQuery: String =
            runCatching { URLDecoder.decode(initialQueryRaw, "UTF-8") }.getOrDefault(initialQueryRaw)
        private val _uiState = MutableStateFlow(GlobalSearchUiState())
        val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()
        private var allSearchCandidatesCache: List<GlobalSearchResultItem>? = null
        private var allSearchCandidatesCacheUpdatedAt: Long = 0L
        private var latestUnfilteredResults: List<GlobalSearchResultItem> = emptyList()

        lateinit var enhancedNavigationManager: EnhancedNavigationManager

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                    initialValue = "",
                )

        init {
            val history = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
            val recentCommands =
                (savedStateHandle.get<List<String>>(COMMAND_HISTORY_KEY) ?: emptyList())
                    .mapNotNull { name -> runCatching { OmniboxCommandId.valueOf(name) }.getOrNull() }
            val (initialPrefixedMode, initialStrippedQuery) = parseModePrefix(initialQuery)
            val initialDisplayQuery = if (initialPrefixedMode != null) initialStrippedQuery else initialQuery
            _uiState.update {
                it.copy(
                    query = initialDisplayQuery,
                    searchHistory = history,
                    recentCommands = recentCommands,
                )
            }
            when {
                initialPrefixedMode != null -> applyMode(initialPrefixedMode, persist = false)
                initialQuery.isBlank() -> {
                    viewModelScope.launch {
                        applyMode(settingsRepository.globalSearchCurrentModeFlow.first(), persist = false)
                    }
                }
                else -> performSearch(initialQuery)
            }
            viewModelScope.launch {
                val storedTypes = settingsRepository.globalSearchSelectedTypesFlow.first()
                val resolvedTypes =
                    storedTypes
                        .mapNotNull { typeName -> GlobalSearchType.entries.firstOrNull { it.name == typeName } }
                        .toSet()
                        .ifEmpty { GlobalSearchType.entries.toSet() }
                _uiState.update { state ->
                    state.copy(
                        selectedTypes = resolvedTypes,
                        results = filterResultsBySelectedTypes(latestUnfilteredResults, resolvedTypes),
                    )
                }
            }
            viewModelScope.launch {
                combine(
                    settingsRepository.globalSearchModeDisplayPrefsFlow,
                    combine(
                        OmniboxMode.entries.map { mode ->
                            settingsRepository.globalSearchRecentInputsFlow(mode)
                        },
                    ) { inputLists ->
                        OmniboxMode.entries.zip(inputLists.toList()).toMap()
                    },
                ) { prefs, recentInputs ->
                    prefs to recentInputs
                }.collect { (prefs, recentInputs) ->
                    _uiState.update {
                        it.copy(
                            modeDisplayPrefs = prefs,
                            recentModeInputs = recentInputs,
                            searchHistory = recentInputs[OmniboxMode.DataSearch].orEmpty(),
                        )
                    }
                }
            }
        }

        fun updateSelectedTypes(types: Set<GlobalSearchType>) {
            val normalizedTypes = types.ifEmpty { GlobalSearchType.entries.toSet() }
            _uiState.update {
                it.copy(
                    selectedTypes = normalizedTypes,
                    results = filterResultsBySelectedTypes(latestUnfilteredResults, normalizedTypes),
                )
            }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchSelectedTypes(normalizedTypes)
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
            if (_uiState.value.mode == OmniboxMode.Command) {
                latestUnfilteredResults = emptyList()
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
                latestUnfilteredResults = emptyList()
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
            latestUnfilteredResults = emptyList()
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    commandResults = emptyList(),
                    hybridCommandResults = emptyList(),
                    isLoading = false,
                )
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
                        rememberRecentInput(OmniboxMode.Command, query)
                        executeCommand(commands.first().id)
                    }
                }
                OmniboxMode.QuickCatchInbox -> {
                    rememberRecentInput(OmniboxMode.QuickCatchInbox, query)
                    submitQuickCatch(query)
                }
                OmniboxMode.StartActivity -> {
                    rememberRecentInput(OmniboxMode.StartActivity, query)
                    submitStartActivity(query)
                }
                OmniboxMode.AddActivityEvent -> {
                    rememberRecentInput(OmniboxMode.AddActivityEvent, query)
                    submitAddActivityEvent(query)
                }
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
            applyMode(mode, persist = true)
        }

        private fun applyMode(
            mode: OmniboxMode,
            persist: Boolean,
        ) {
            _uiState.update { current ->
                current.copy(
                    mode = mode,
                    isLoading = false,
                    results = if (mode == OmniboxMode.DataSearch) current.results else emptyList(),
                    commandResults =
                        if (mode == OmniboxMode.Command) {
                            findCommandResults(current.query)
                        } else {
                            emptyList()
                        },
                    hybridCommandResults =
                        if (mode == OmniboxMode.DataSearch) {
                            findCommandResults(current.query).take(HYBRID_COMMANDS_LIMIT)
                        } else {
                            emptyList()
                        },
                )
            }
            if (persist) {
                viewModelScope.launch {
                    settingsRepository.saveGlobalSearchCurrentMode(mode)
                }
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

        fun openLifeJournal() {
            enhancedNavigationManager.navigate(target = NavTarget.Tracker)
        }

        fun openInbox() {
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
        }

        fun onDataResultOpened(resultUniqueId: String) {
            dataUsage[resultUniqueId] = (dataUsage[resultUniqueId] ?: 0) + 1
            trimUsageMap(dataUsage)
            savedStateHandle[DATA_USAGE_KEY] = dataUsage.toMap()
        }

        fun openAttachmentResult(result: GlobalSearchResultItem.AttachmentItem) {
            val searchResult = result.searchResult
            val target =
                when (searchResult.attachmentType) {
                    BacklogItemTypeValues.NOTE_DOCUMENT -> NavTarget.NoteDocument(id = searchResult.entityId)
                    BacklogItemTypeValues.JOURNAL_DOCUMENT -> NavTarget.JournalDocument(id = searchResult.entityId)
                    BacklogItemTypeValues.CHECKLIST -> NavTarget.Checklist(id = searchResult.entityId)
                    BacklogItemTypeValues.MUSIC_NOTE -> NavTarget.MusicNote(id = searchResult.entityId)
                    BacklogItemTypeValues.SCRIPT ->
                        NavTarget.ScriptEditor(
                            contextId = searchResult.ownerContextId,
                            scriptId = searchResult.entityId,
                        )
                    else -> null
                }
            onDataResultOpened(result.uniqueId)
            if (target != null) {
                enhancedNavigationManager.navigate(target = target)
            } else {
                searchResult.ownerContextId?.let { contextId ->
                    navigateToProjectForResult(contextId, searchResult.contextName)
                }
            }
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

        fun createContext(name: String) {
            val contextName = name.trim()
            if (contextName.isBlank()) return
            viewModelScope.launch {
                val contextId = UUID.randomUUID().toString()
                contextRepository.createContextWithId(
                    id = contextId,
                    name = contextName,
                    parentId = null,
                )
                invalidateSearchCandidatesCache()
                enhancedNavigationManager.navigateToProject(contextId, contextName)
            }
        }

        fun createDocumentFromSearch() {
            viewModelScope.launch {
                val inboxContextId = resolveInboxContextId() ?: return@launch
                enhancedNavigationManager.navigate(
                    target = NavTarget.NoteDocumentEdit(contextId = inboxContextId, documentId = null),
                )
            }
        }

        suspend fun createAttachmentFromGlobalSearch(request: NewDocumentDraft): String? {
            val inboxContextId = resolveInboxContextId() ?: return null
            return when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New note" },
                            contextId = inboxContextId,
                        )
                    contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)
                }
                is NewDocumentDraft.JournalDocument -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New journal" },
                            contextId = inboxContextId,
                            attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                        )
                    contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId =
                        musicNoteRepository.create(
                            name = request.name.ifBlank { "New music note" },
                            contextId = inboxContextId,
                        )
                    contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId =
                        checklistRepository.createChecklist(
                            name = request.name.ifBlank { "New checklist" },
                            contextId = inboxContextId,
                        )
                    contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)
                }
                is NewDocumentDraft.WebLink -> {
                    val target = request.url.trim()
                    target.takeIf { it.isNotBlank() }?.let {
                        contextRepository.addLinkItemToContextFromLink(
                            contextId = inboxContextId,
                            link =
                                RelatedLink(
                                    type = LinkType.URL,
                                    target = it,
                                    displayName = request.name.trim().ifBlank { it },
                                ),
                        )
                    }
                }
                is NewDocumentDraft.Obsidian -> {
                    val target = request.noteName.trim()
                    target.takeIf { it.isNotBlank() }?.let {
                        contextRepository.addLinkItemToContextFromLink(
                            contextId = inboxContextId,
                            link =
                                RelatedLink(
                                    type = LinkType.OBSIDIAN,
                                    target = it,
                                    displayName = request.displayName.trim().ifBlank { it },
                                    vault = request.vault,
                                ),
                        )
                    }
                }
            }
        }

        fun createReminder(reminderTime: Long) {
            viewModelScope.launch {
                reminderRepository.createReminder(
                    entityId = "manual-${UUID.randomUUID()}",
                    entityType = "REMINDER",
                    reminderTime = reminderTime,
                )
            }
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
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchRecentInputs(OmniboxMode.DataSearch, updated)
            }
        }

        fun clearSearchHistory() {
            _uiState.update { it.copy(searchHistory = emptyList()) }
            savedStateHandle[SEARCH_HISTORY_KEY] = emptyList<String>()
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchRecentInputs(OmniboxMode.DataSearch, emptyList())
            }
        }

        fun toggleCurrentModePreview() {
            val mode = _uiState.value.mode
            val updatedPrefs =
                _uiState.value.modeDisplayPrefs.updated(mode) { prefs ->
                    prefs.copy(showPreview = !prefs.showPreview)
                }
            _uiState.update { it.copy(modeDisplayPrefs = updatedPrefs) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchModePreview(mode, updatedPrefs[mode].showPreview)
            }
        }

        fun setModePreview(
            mode: OmniboxMode,
            enabled: Boolean,
        ) {
            val updatedPrefs =
                _uiState.value.modeDisplayPrefs.updated(mode) { prefs ->
                    prefs.copy(showPreview = enabled)
                }
            _uiState.update { it.copy(modeDisplayPrefs = updatedPrefs) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchModePreview(mode, enabled)
            }
        }

        fun toggleCurrentModeRecents() {
            val mode = _uiState.value.mode
            val updatedPrefs =
                _uiState.value.modeDisplayPrefs.updated(mode) { prefs ->
                    prefs.copy(showRecents = !prefs.showRecents)
                }
            _uiState.update { it.copy(modeDisplayPrefs = updatedPrefs) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchModeRecents(mode, updatedPrefs[mode].showRecents)
            }
        }

        fun setModeRecents(
            mode: OmniboxMode,
            enabled: Boolean,
        ) {
            val updatedPrefs =
                _uiState.value.modeDisplayPrefs.updated(mode) { prefs ->
                    prefs.copy(showRecents = enabled)
                }
            _uiState.update { it.copy(modeDisplayPrefs = updatedPrefs) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchModeRecents(mode, enabled)
            }
        }

        fun applyRecentInputForMode(
            mode: OmniboxMode,
            value: String,
        ) {
            setMode(mode)
            onQueryChange(value)
        }

        fun removeRecentInput(
            mode: OmniboxMode,
            value: String,
        ) {
            val updated = _uiState.value.recentModeInputs[mode].orEmpty().filterNot { it.equals(value, ignoreCase = true) }
            _uiState.update { it.copy(recentModeInputs = it.recentModeInputs + (mode to updated)) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchRecentInputs(mode, updated)
            }
        }

        fun clearRecentInputs(mode: OmniboxMode) {
            _uiState.update { it.copy(recentModeInputs = it.recentModeInputs + (mode to emptyList())) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchRecentInputs(mode, emptyList())
            }
        }

        private fun performSearch(rawQuery: String) {
            val query = rawQuery.trim()
            if (query.isBlank()) {
                latestUnfilteredResults = emptyList()
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
                val structuredQuery = StructuredSearchQuery.parse(query)
                val strictResults = contextRepository.searchGlobal("%$query%")
                val strictDistinct = strictResults.distinctBy { it.uniqueId }
                val rankedStrict =
                    if (structuredQuery.hasTags) {
                        rankStructuredResults(strictDistinct, structuredQuery)
                    } else {
                        strictDistinct
                            .mapNotNull { item ->
                                val score = fuzzyScoreForData(query, item)
                                if (score < 0) null else item to score
                            }
                            .sortedWith(
                                compareByDescending<Pair<GlobalSearchResultItem, Int>> { it.second }
                                    .thenByDescending { it.first.timestamp },
                            )
                            .map { it.first }
                    }

                val finalResults =
                    if (rankedStrict.isNotEmpty() || structuredQuery.hasTags) {
                        rankedStrict
                    } else {
                        val allCandidates = getAllSearchCandidatesForFuzzy()
                        allCandidates
                            .mapNotNull { item ->
                                val score = fuzzyScoreForData(query, item)
                                if (score < MIN_FUZZY_DATA_SCORE) null else item to score
                            }
                            .sortedWith(
                                compareByDescending<Pair<GlobalSearchResultItem, Int>> { it.second }
                                    .thenByDescending { it.first.timestamp },
                            )
                            .map { it.first }
                            .take(MAX_FUZZY_RESULTS)
                    }
                latestUnfilteredResults = finalResults
                _uiState.update {
                    it.copy(
                        results = filterResultsBySelectedTypes(finalResults, it.selectedTypes),
                        isLoading = false,
                        commandResults = emptyList(),
                        hybridCommandResults = findCommandResults(query).take(HYBRID_COMMANDS_LIMIT),
                    )
                }
            }
        }

        private fun rankStructuredResults(
            results: List<GlobalSearchResultItem>,
            query: StructuredSearchQuery,
        ): List<GlobalSearchResultItem> {
            if (query.textQuery.isBlank()) {
                return results.sortedWith(
                    compareByDescending<GlobalSearchResultItem> { it.matchedTags.size }
                        .thenByDescending { it.timestamp },
                )
            }
            return results
                .map { item -> item to fuzzyScoreForData(query.textQuery, item).coerceAtLeast(0) }
                .sortedWith(
                    compareByDescending<Pair<GlobalSearchResultItem, Int>> { it.first.matchedTags.size }
                        .thenByDescending { it.second }
                        .thenByDescending { it.first.timestamp },
                )
                .map { it.first }
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
                OmniboxCommandId.OpenContexts ->
                    enhancedNavigationManager.navigate(
                        target = NavTarget.ContextHierarchy(),
                        recordInHistory = true,
                    )
                OmniboxCommandId.OpenInbox -> openInbox()
                OmniboxCommandId.OpenTracker -> enhancedNavigationManager.navigate(target = NavTarget.Tracker)
                OmniboxCommandId.OpenReminders -> enhancedNavigationManager.navigate(target = NavTarget.Reminders)
                OmniboxCommandId.OpenSettings -> enhancedNavigationManager.navigate(target = NavTarget.Settings)
                OmniboxCommandId.OpenSearch -> enhancedNavigationManager.navigate(target = NavTarget.GlobalSearchHome)
                OmniboxCommandId.OpenAttachments ->
                    enhancedNavigationManager.navigate(target = NavTarget.AttachmentsLibrary)
                OmniboxCommandId.OpenScripts -> enhancedNavigationManager.navigate(target = NavTarget.ScriptsLibrary)
                OmniboxCommandId.OpenAiChat -> enhancedNavigationManager.navigate(target = NavTarget.Chat)
                OmniboxCommandId.OpenAiInsights -> enhancedNavigationManager.navigate(target = NavTarget.AiInsights)
                OmniboxCommandId.OpenAiLife -> enhancedNavigationManager.navigate(target = NavTarget.LifeState)
                OmniboxCommandId.OpenStructurePresets ->
                    enhancedNavigationManager.navigate(target = NavTarget.StructurePresets)
            }
        }

        private fun findCommandResults(rawQuery: String): List<OmniboxCommandResult> {
            val query = rawQuery.trim()
            if (query.isBlank()) {
                val recentOrder = _uiState.value.recentCommands.withIndex().associate { it.value to it.index }
                return commandDefinitions.map {
                    val usageBoost = (commandUsage[it.id.name] ?: 0) * COMMAND_USAGE_BOOST_MULTIPLIER
                    val recencyBoost =
                        recentOrder[it.id]?.let { idx ->
                            COMMAND_RECENCY_BASE_BOOST - idx * COMMAND_RECENCY_STEP
                        } ?: 0
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
                            score =
                                score +
                                    (commandUsage[definition.id.name] ?: 0) *
                                    COMMAND_MATCH_USAGE_BOOST_MULTIPLIER,
                        )
                    }
                }
                .sortedByDescending { it.score }
                .take(MAX_COMMAND_RESULTS)
        }

        private fun commandScore(
            query: String,
            command: OmniboxCommandDefinition,
        ): Int {
            val candidates =
                buildList {
                    add(command.title)
                    add(command.subtitle)
                    addAll(command.keywords)
                }
            val expandedQueries = expandQueryWithSynonyms(query)
            return candidates.maxOfOrNull { candidate ->
                expandedQueries.maxOfOrNull { candidateQuery ->
                    fuzzyScore(candidateQuery, candidate)
                } ?: NO_MATCH_SCORE
            } ?: NO_MATCH_SCORE
        }

        private fun fuzzyScore(
            rawQuery: String,
            rawText: String,
        ): Int {
            val query = rawQuery.lowercase().trim()
            val text = rawText.lowercase()
            val directMatchScore = directMatchScore(query, text)
            val subsequenceScore = subsequenceMatchScore(query, text)
            val typoScore =
                if (subsequenceScore == NO_MATCH_SCORE) {
                    // Typo-tolerant fallback: allow small edit distance against tokens (e.g. "нотм" -> "ноти").
                    typoToleranceScore(query, text)
                } else {
                    NO_MATCH_SCORE
                }
            return maxOf(directMatchScore, subsequenceScore, typoScore)
        }

        private fun typoToleranceScore(
            query: String,
            text: String,
        ): Int {
            val tokens = text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
            val maxDistance =
                when {
                    query.length <= SHORT_QUERY_MAX_LENGTH -> SHORT_QUERY_MAX_DISTANCE
                    query.length <= MEDIUM_QUERY_MAX_LENGTH -> MEDIUM_QUERY_MAX_DISTANCE
                    else -> LONG_QUERY_MAX_DISTANCE
                }

            var bestDistance = Int.MAX_VALUE
            for (token in tokens) {
                val lengthDelta = kotlin.math.abs(token.length - query.length)
                if (lengthDelta <= maxDistance) {
                    val distance = damerauLevenshteinDistanceBounded(query, token, maxDistance)
                    if (distance < bestDistance) {
                        bestDistance = distance
                    }
                }
            }

            val canScoreTypoMatch =
                query.length >= MIN_TYPO_TOLERANCE_QUERY_LENGTH &&
                    tokens.isNotEmpty() &&
                    bestDistance != Int.MAX_VALUE &&
                    bestDistance <= maxDistance
            return if (canScoreTypoMatch) {
                TYPO_MATCH_BASE_SCORE - bestDistance * TYPO_DISTANCE_PENALTY
            } else {
                NO_MATCH_SCORE
            }
        }

        private fun damerauLevenshteinDistanceBounded(
            a: String,
            b: String,
            maxDistance: Int,
        ): Int {
            val rows = a.length + 1
            val cols = b.length + 1
            val d = Array(rows) { IntArray(cols) }
            for (i in 0 until rows) d[i][0] = i
            for (j in 0 until cols) d[0][j] = j

            val exceedsMaxLengthDelta = kotlin.math.abs(a.length - b.length) > maxDistance
            val canComputeDistance = a != b && !exceedsMaxLengthDelta
            if (canComputeDistance) {
                for (i in 1 until rows) {
                    var rowMin = Int.MAX_VALUE
                    for (j in 1 until cols) {
                        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                        var value =
                            minOf(
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
                    if (rowMin > maxDistance) {
                        return maxDistance + 1
                    }
                }
            }
            return when {
                a == b -> 0
                exceedsMaxLengthDelta -> maxDistance + 1
                else -> d[a.length][b.length]
            }
        }

        private fun directMatchScore(
            query: String,
            text: String,
        ): Int {
            if (query.isBlank()) {
                return EMPTY_QUERY_SCORE
            }
            val directIndex = text.indexOf(query)
            return if (directIndex >= 0) {
                DIRECT_MATCH_BASE_SCORE - directIndex
            } else {
                NO_MATCH_SCORE
            }
        }

        private fun subsequenceMatchScore(
            query: String,
            text: String,
        ): Int {
            if (query.isBlank()) {
                return NO_MATCH_SCORE
            }
            var qIndex = 0
            var score = 0
            var lastMatchIndex = INITIAL_LAST_MATCH_INDEX
            for (i in text.indices) {
                if (qIndex >= query.length) {
                    break
                }
                if (text[i] == query[qIndex]) {
                    score += CHARACTER_MATCH_SCORE
                    if (i == lastMatchIndex + 1) {
                        score += CONSECUTIVE_CHARACTER_BONUS
                    }
                    lastMatchIndex = i
                    qIndex++
                }
            }
            return if (qIndex == query.length) score else NO_MATCH_SCORE
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
                        if (idx == 0) {
                            PREFIX_MATCH_BONUS
                        } else if (idx in NEAR_PREFIX_START_INDEX..NEAR_PREFIX_END_INDEX) {
                            NEAR_PREFIX_BONUS
                        } else {
                            0
                        }
                    } ?: 0
                } ?: 0
            val usageBoost = (dataUsage[item.uniqueId] ?: 0) * DATA_USAGE_BOOST_MULTIPLIER
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
                        item.searchResult.context.tags.orEmpty().joinToString(" "),
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
                        item.searchResult.searchText ?: "",
                    )
            }

        private fun filterResultsBySelectedTypes(
            results: List<GlobalSearchResultItem>,
            selectedTypes: Set<GlobalSearchType>,
        ): List<GlobalSearchResultItem> =
            results.filter { result -> selectedTypes.any { type -> type.matches(result) } }

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

        private fun rememberRecentInput(
            mode: OmniboxMode,
            value: String,
        ) {
            val normalized = value.trim()
            if (normalized.isBlank()) return
            val current = _uiState.value.recentModeInputs[mode].orEmpty()
            val updated = (listOf(normalized) + current.filterNot { it.equals(normalized, ignoreCase = true) }).take(MAX_SEARCH_HISTORY)
            _uiState.update { it.copy(recentModeInputs = it.recentModeInputs + (mode to updated)) }
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchRecentInputs(mode, updated)
            }
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
            viewModelScope.launch {
                settingsRepository.saveGlobalSearchRecentInputs(OmniboxMode.DataSearch, updated)
            }
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
