package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class PlanningUseCase @Inject constructor(
    val planningModeManager: PlanningModeManager,
    private val searchUseCase: SearchUseCase,
    private val settingsRepository: SettingsRepository,
) {

    private var isInitialized = false

    private val _isReadyForFiltering = MutableStateFlow(false)
    val isReadyForFiltering: StateFlow<Boolean> = _isReadyForFiltering.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _internalPlanningSettingsFlow = MutableStateFlow<Flow<PlanningSettingsState>>(flowOf(PlanningSettingsState()))
    @OptIn(ExperimentalCoroutinesApi::class)
    val planningSettingsState: Flow<PlanningSettingsState> = _internalPlanningSettingsFlow.flatMapLatest { it }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _internalFilterFlow = MutableStateFlow<Flow<FilterState>>(
        flowOf(
            FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
            )
        )
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    val filterStateFlow: Flow<FilterState> = _internalFilterFlow.flatMapLatest { it }
        .onEach { // Add logging
            android.util.Log.d("HierarchyDebug", ">>> filterStateFlow emitted value: flat=${it.flatList.size}")
        }

    val planningMode = planningModeManager.planningMode

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun initialize(
        scope: CoroutineScope,
        allProjectsFlat: StateFlow<List<Project>>,
    ) {
        if (isInitialized) return

        _internalPlanningSettingsFlow.value = combine(
            settingsRepository.showPlanningModesFlow,
            settingsRepository.dailyTagFlow,
            settingsRepository.mediumTagFlow,
            settingsRepository.longTagFlow,
        ) { show, daily, medium, long ->
            PlanningSettingsState(
                showModes = show,
                dailyTag = daily,
                mediumTag = medium,
                longTag = long,
            )
        }

        val debouncedSearchQuery =
            searchUseCase.searchQuery.map { it.text }.debounce(100L).distinctUntilChanged()

        val isLocalSearchActive =
            searchUseCase.subStateStack.map { stack ->
                stack.any { it is MainSubState.LocalSearch }
            }

        val baseFilterState =
            combine(
                allProjectsFlat,
                debouncedSearchQuery,
                isLocalSearchActive,
                planningMode,
                planningSettingsState,
            ) { flatList, query, searchActive, mode, settings ->
                android.util.Log.d(
                    "HierarchyDebug",
                    "baseFilterState combine flat=${flatList.size} query='$query' searchActive=$searchActive mode=$mode",
                )
                FilterState(
                    flatList = flatList,
                    query = query,
                    searchActive = searchActive,
                    mode = mode,
                    settings = settings,
                )
            }

        _internalFilterFlow.value = combine(baseFilterState, _isReadyForFiltering) { state, isReady ->
            if (isReady) {
                android.util.Log.d("HierarchyDebug", "filterStateFlow emitting READY with flat=${state.flatList.size}")
                state
            } else {
                android.util.Log.d("HierarchyDebug", "filterStateFlow emitting NOT READY with flat=${state.flatList.size}")
                state.copy(
                    flatList = emptyList(),
                    query = "",
                    searchActive = false,
                    mode = PlanningMode.All,
                )
            }
        }

        isInitialized = true
    }

    fun markReadyForFiltering() {
        android.util.Log.d("HierarchyDebug", "markReadyForFiltering called")
        _isReadyForFiltering.value = true
    }

    fun onPlanningModeChange(mode: PlanningMode) {
        if (searchUseCase.isSearchActive()) {
            searchUseCase.popToSubState(MainSubState.Hierarchy)
            searchUseCase.onToggleSearch(isActive = false)
        }
        planningModeManager.changeMode(mode)
    }

    fun toggleExpandedInPlanningMode(project: Project) {
        planningModeManager.toggleExpandedInPlanningMode(project)
    }
}