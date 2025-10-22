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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlanningUseCase @Inject constructor(
    val planningModeManager: PlanningModeManager,
    private val searchUseCase: SearchUseCase,
    private val settingsRepository: SettingsRepository,
) {

    private var isInitialized = false

    private val _isReadyForFiltering = MutableStateFlow(false)
    val isReadyForFiltering: StateFlow<Boolean> = _isReadyForFiltering.asStateFlow()

    private var planningSettingsStateInternal: StateFlow<PlanningSettingsState> =
        MutableStateFlow(PlanningSettingsState())
    val planningSettingsState: StateFlow<PlanningSettingsState>
        get() = planningSettingsStateInternal

    private var filterStateFlowInternal: StateFlow<FilterState> =
        MutableStateFlow(
            FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.All,
                settings = PlanningSettingsState(),
            ),
        )
    val filterStateFlow: StateFlow<FilterState>
        get() = filterStateFlowInternal

    val planningMode = planningModeManager.planningMode

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun initialize(
        scope: CoroutineScope,
        allProjectsFlat: StateFlow<List<Project>>,
    ) {
        if (isInitialized) return

        planningSettingsStateInternal =
            combine(
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
            }.stateIn(scope, SharingStarted.Lazily, PlanningSettingsState())

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
                planningSettingsStateInternal,
            ) { flatList, query, searchActive, mode, settings ->
                FilterState(
                    flatList = flatList,
                    query = query,
                    searchActive = searchActive,
                    mode = mode,
                    settings = settings,
                )
            }

        filterStateFlowInternal =
            combine(baseFilterState, _isReadyForFiltering) { state, isReady ->
                if (isReady) {
                    state
                } else {
                    state.copy(
                        query = "",
                        searchActive = false,
                        mode = PlanningMode.All,
                    )
                }
            }.stateIn(
                scope,
                SharingStarted.Lazily,
                FilterState(
                    flatList = emptyList(),
                    query = "",
                    searchActive = false,
                    mode = PlanningMode.All,
                    settings = PlanningSettingsState(),
                ),
            )

        isInitialized = true
    }

    fun markReadyForFiltering() {
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
