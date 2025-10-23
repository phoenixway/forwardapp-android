package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel scope гарантує, що MainScreenViewModel та його use-case-и ділять один екземпляр.
@ViewModelScoped
class PlanningUseCase
@Inject
constructor(
  val planningModeManager: PlanningModeManager,
  private val searchAdapter: PlanningSearchAdapter,
  private val settingsProvider: PlanningSettingsProvider,
) {
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
      )
    )
  val filterStateFlow: StateFlow<FilterState> = _filterStateFlow.asStateFlow()

  val planningMode = planningModeManager.planningMode
  private val lastNonEmptyProjects = MutableStateFlow<List<Project>>(emptyList())

  @OptIn(FlowPreview::class)
  fun initialize(scope: CoroutineScope, allProjectsFlat: StateFlow<List<Project>>) {
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
      searchAdapter.subStateStack.map { stack -> stack.any { it is MainSubState.LocalSearch } }

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
          isReady = false,
        )
      }


    baseFilterState
      .onEach { state ->
        var ready = _isReadyForFiltering.value
        if (state.flatList.isNotEmpty()) {
          android.util.Log.d(
            "HierarchyDebug",
            "PlanningUseCase storing lastNonEmptyProjects size=${state.flatList.size}",
          )
          lastNonEmptyProjects.value = state.flatList
          if (!ready) {
            android.util.Log.d(
              "HierarchyDebug",
              "PlanningUseCase marking ready due to non-empty flatList size=${state.flatList.size}",
            )
            _isReadyForFiltering.value = true
            ready = true
          }
        } else if (!ready) {
          android.util.Log.d(
            "HierarchyDebug",
            "PlanningUseCase still waiting for projects (current flatList empty)",
          )
        }

        val effectiveFlatList =
          if (
            state.flatList.isEmpty() &&
              lastNonEmptyProjects.value.isNotEmpty() &&
              !state.searchActive &&
              state.mode == PlanningMode.All &&
              ready
          ) {
            android.util.Log.d(
              "HierarchyDebug",
              "PlanningUseCase applying fallback with cached projects size=${lastNonEmptyProjects.value.size}",
            )
            lastNonEmptyProjects.value
          } else {
            state.flatList
          }

        val emitted =
          state.copy(
            flatList = effectiveFlatList,
            isReady = ready,
          )
        android.util.Log.d(
          "HierarchyDebug",
          "PlanningUseCase emitting ready=${emitted.isReady} flat=${emitted.flatList.size}",
        )
        _filterStateFlow.value = emitted
      }
      .launchIn(scope)
  }

  fun onPlanningModeChange(mode: PlanningMode) {
    if (searchAdapter.isSearchActive()) {
      searchAdapter.popToSubState(MainSubState.Hierarchy)
      searchAdapter.onToggleSearch(isActive = false)
    }
    planningModeManager.changeMode(mode)
  }

  fun toggleExpandedInPlanningMode(project: Project) {
    planningModeManager.toggleExpandedInPlanningMode(project)
  }
}