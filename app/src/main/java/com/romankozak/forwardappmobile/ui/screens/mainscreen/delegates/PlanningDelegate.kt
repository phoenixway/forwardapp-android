package com.romankozak.forwardappmobile.ui.screens.mainscreen.delegates

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.screens.mainscreen.GoalListUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.PlanningSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlanningDelegate(
    private val settingsRepo: SettingsRepository,
    private val contextHandler: ContextHandler,
    private val uiEventChannel: Channel<GoalListUiEvent>,
    private val viewModelScope: CoroutineScope
) {
    private val _planningMode = MutableStateFlow<PlanningMode>(PlanningMode.All)
    val planningMode = _planningMode.asStateFlow()

    private val _expandedInDailyMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInMediumMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInLongMode = MutableStateFlow<Set<String>?>(null)

    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()

    val planningSettingsState: StateFlow<PlanningSettingsState> =
        combine(
            settingsRepo.showPlanningModesFlow,
            settingsRepo.dailyTagFlow,
            settingsRepo.mediumTagFlow,
            settingsRepo.longTagFlow,
        ) { show, daily, medium, long ->
            PlanningSettingsState(show, daily, medium, long)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = PlanningSettingsState(),
        )

    val allContextsForDialog: StateFlow<List<UiContext>> =
        contextHandler.allContextsFlow
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val obsidianVaultName: StateFlow<String> =
        settingsRepo.obsidianVaultNameFlow
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), "")

    suspend fun initialize() {
        contextHandler.initialize()
    }

    fun onPlanningModeChange(
        mode: PlanningMode,
        isSearchActive: MutableStateFlow<Boolean>,
        searchQuery: MutableStateFlow<TextFieldValue>
    ) {
        if (isSearchActive.value) {
            isSearchActive.value = false
            searchQuery.value = TextFieldValue("")
        }
        _planningMode.value = mode
    }

    fun getExpandedState(mode: PlanningMode): StateFlow<Set<String>?> {
        return when (mode) {
            is PlanningMode.Daily -> _expandedInDailyMode.asStateFlow()
            is PlanningMode.Medium -> _expandedInMediumMode.asStateFlow()
            is PlanningMode.Long -> _expandedInLongMode.asStateFlow()
            else -> MutableStateFlow<Set<String>?>(null).asStateFlow()
        }
    }

    fun updateExpandedState(mode: PlanningMode, expandedIds: Set<String>) {
        when (mode) {
            is PlanningMode.Daily -> _expandedInDailyMode.value = expandedIds
            is PlanningMode.Medium -> _expandedInMediumMode.value = expandedIds
            is PlanningMode.Long -> _expandedInLongMode.value = expandedIds
            else -> Unit
        }
    }

    suspend fun onContextSelected(contextName: String, allListsFlat: StateFlow<List<GoalList>>) {
        val targetTag = contextHandler.getContextTag(contextName)
        if (targetTag.isNullOrBlank()) {
            uiEventChannel.send(GoalListUiEvent.ShowToast("Тег для контексту '$contextName' не знайдено або порожній"))
            return
        }

        val targetList = allListsFlat.value.find { it.tags?.contains(targetTag) == true }
        if (targetList != null) {
            uiEventChannel.send(GoalListUiEvent.NavigateToDetails(targetList.id))
        } else {
            uiEventChannel.send(GoalListUiEvent.ShowToast("Список з тегом '#$targetTag' не знайдено"))
        }
    }

    suspend fun saveSettings(
        show: Boolean,
        daily: String,
        medium: String,
        long: String,
        vaultName: String,
    ) {
        settingsRepo.saveShowPlanningModes(show)
        settingsRepo.saveDailyTag(daily.trim())
        settingsRepo.saveMediumTag(medium.trim())
        settingsRepo.saveLongTag(long.trim())
        settingsRepo.saveObsidianVaultName(vaultName.trim())
    }

    suspend fun saveAllContexts(updatedContexts: List<UiContext>) {
        val customContextsToSave = updatedContexts.filter { !it.isReserved }
        settingsRepo.saveCustomContexts(customContextsToSave)
        contextHandler.initialize()
    }

    fun onShowRecentLists() { _showRecentListsSheet.value = true }
    fun onDismissRecentLists() { _showRecentListsSheet.value = false }

    suspend fun onDayPlanClicked() {
        val today = System.currentTimeMillis()
        uiEventChannel.send(GoalListUiEvent.NavigateToDayPlan(today))
    }

    suspend fun onRecentListSelected(listId: String) {
        onDismissRecentLists()
        uiEventChannel.send(GoalListUiEvent.NavigateToDetails(listId))
    }

    fun hasDescendantsWithLongNames(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        allListsFlat: List<GoalList>,
        characterLimit: Int = 35
    ): Boolean {
        val queue = ArrayDeque<String>()
        childMap[listId]?.forEach { queue.add(it.id) }

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val list = allListsFlat.find { it.id == currentId }

            if (list != null) {
                if (list.name.length > characterLimit) {
                    return true
                }
                childMap[currentId]?.forEach { queue.add(it.id) }
            }
        }
        return false
    }
}