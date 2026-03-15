package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state.PlanningModeManager
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

// ViewModel scope гарантує, що MainScreenViewModel та його use-case-и ділять один екземпляр.
@ViewModelScoped
class PlanningUseCase
    @Inject
    constructor(
        val planningModeManager: PlanningModeManager,
        private val searchAdapter: PlanningSearchAdapter,
        private val settingsProvider: PlanningSettingsProvider,
    ) {
        private companion object {
            private const val HIERARCHY_DEBUG_TAG = "HierarchyDebug"
        }

        private var isInitialized = false

        private val _isReadyForFiltering = MutableStateFlow(false)
        val isReadyForFiltering: StateFlow<Boolean> = _isReadyForFiltering.asStateFlow()

        private val _planningSettingsState = MutableStateFlow(PlanningSettingsState())
        val planningSettingsState: StateFlow<PlanningSettingsState> = _planningSettingsState.asStateFlow()

        private val _filterStateFlow =
            MutableStateFlow(
                FilterState(
                    flatList = emptyList(),
                    query = "",
                    searchActive = false,
                    mode = PlanningMode.All,
                    settings = PlanningSettingsState(),
                    isReady = false,
                ),
            )
        val filterStateFlow: StateFlow<FilterState> = _filterStateFlow.asStateFlow()

        val planningMode = planningModeManager.planningMode
        private val lastNonEmptyProjects = MutableStateFlow<List<Context>>(emptyList())

        private fun shouldUseCachedProjects(
            state: FilterState,
            ready: Boolean,
            cachedProjects: List<Context>,
        ): Boolean =
            state.flatList.isEmpty() &&
                cachedProjects.isNotEmpty() &&
                !state.searchActive &&
                state.mode == PlanningMode.All &&
                ready

        @OptIn(FlowPreview::class)
        fun initialize(
            scope: CoroutineScope,
            allProjectsFlat: StateFlow<List<Context>>,
        ) {
            if (isInitialized) return
            isInitialized = true

            // Оновлюємо _planningSettingsState через .onEach
            combine(
                settingsProvider.showPlanningModesFlow,
                settingsProvider.dailyTagFlow,
                settingsProvider.mediumTagFlow,
                settingsProvider.longTagFlow,
            ) { show, daily, medium, long ->
                PlanningSettingsState(
                    showModes = show,
                    dailyTag = daily,
                    mediumTag = medium,
                    longTag = long,
                )
            }
                .onEach { settings -> _planningSettingsState.value = settings }
                .launchIn(scope)

            val debouncedSearchQuery =
                searchAdapter.searchQuery.map { it.text }.debounce(100L).distinctUntilChanged()

            val isLocalSearchActive =
                searchAdapter.subStateStack.map { stack -> stack.any { it is ProjectHierarchyScreenSubState.LocalSearch } }

            val baseFilterState =
                combine(
                    allProjectsFlat,
                    debouncedSearchQuery,
                    isLocalSearchActive,
                    planningMode,
                    planningSettingsState,
                ) { flatList, query, searchActive, mode, settings ->
                    Log.d(
                        HIERARCHY_DEBUG_TAG,
                        "baseFilterState combine flat=${flatList.size} query='$query' searchActive=$searchActive mode=$mode",
                    )
                    FilterState(
                        flatList = flatList,
                        query = query,
                        searchActive = searchActive,
                        mode = mode,
                        settings = settings,
                        isReady = false,
                    )
                }

            baseFilterState
                .onEach { state ->
                    var ready = _isReadyForFiltering.value
                    if (!ready) {
                        Log.d(HIERARCHY_DEBUG_TAG, "PlanningUseCase marking ready on first emission.")
                        _isReadyForFiltering.value = true
                        ready = true
                    }

                    if (state.flatList.isNotEmpty()) {
                        Log.d(
                            HIERARCHY_DEBUG_TAG,
                            "PlanningUseCase storing lastNonEmptyProjects size=${state.flatList.size}",
                        )
                        lastNonEmptyProjects.value = state.flatList
                    }

                    val cachedProjects = lastNonEmptyProjects.value
                    val effectiveFlatList =
                        if (shouldUseCachedProjects(state = state, ready = ready, cachedProjects = cachedProjects)) {
                            Log.d(
                                HIERARCHY_DEBUG_TAG,
                                "PlanningUseCase applying fallback with cached projects size=${cachedProjects.size}",
                            )
                            cachedProjects
                        } else {
                            state.flatList
                        }

                    val emitted =
                        state.copy(
                            flatList = effectiveFlatList,
                            isReady = ready,
                        )
                    Log.d(
                        HIERARCHY_DEBUG_TAG,
                        "PlanningUseCase emitting ready=${emitted.isReady} flat=${emitted.flatList.size}",
                    )
                    _filterStateFlow.value = emitted
                }
                .launchIn(scope)
        }

        fun onPlanningModeChange(mode: PlanningMode) {
            if (searchAdapter.isSearchActive()) {
                searchAdapter.popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
                searchAdapter.onToggleSearch(isActive = false)
            }
            planningModeManager.changeMode(mode)
        }

        fun toggleExpandedInPlanningMode(project: Context) {
            planningModeManager.toggleExpandedInPlanningMode(project)
        }
    }

typealias ProjectHierarchyScreenPlanningUseCase = PlanningUseCase
