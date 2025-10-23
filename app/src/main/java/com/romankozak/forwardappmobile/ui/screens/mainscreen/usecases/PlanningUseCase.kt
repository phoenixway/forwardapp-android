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

class PlanningUseCase
@Inject
constructor(
  val planningModeManager: PlanningModeManager,
  private val searchUseCase: SearchUseCase,
  private val settingsRepository: SettingsRepository,
) {
  private var isInitialized = false

  private val _isReadyForFiltering = MutableStateFlow(false)
  val isReadyForFiltering: StateFlow<Boolean> = _isReadyForFiltering.asStateFlow()

  // Використовуємо MutableStateFlow як основу
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
      )
    )
  val filterStateFlow: StateFlow<FilterState> = _filterStateFlow.asStateFlow()

  val planningMode = planningModeManager.planningMode

  @OptIn(FlowPreview::class)
  fun initialize(scope: CoroutineScope, allProjectsFlat: StateFlow<List<Project>>) {
    if (isInitialized) return
    isInitialized = true

    // Оновлюємо _planningSettingsState через .onEach
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
      }
      .onEach { settings -> _planningSettingsState.value = settings }
      .launchIn(scope)

    val debouncedSearchQuery =
      searchUseCase.searchQuery.map { it.text }.debounce(100L).distinctUntilChanged()

    val isLocalSearchActive =
      searchUseCase.subStateStack.map { stack -> stack.any { it is MainSubState.LocalSearch } }

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

    // Оновлюємо _filterStateFlow через .onEach
    combine(baseFilterState, _isReadyForFiltering) { state, isReady ->
        if (isReady) {
          android.util.Log.d(
            "HierarchyDebug",
            "filterStateFlow emitting READY with flat=${state.flatList.size}",
          )
          state
        } else {
          android.util.Log.d(
            "HierarchyDebug",
            "filterStateFlow emitting NOT READY with flat=${state.flatList.size}",
          )
          state.copy(
            flatList = emptyList(),
            query = "",
            searchActive = false,
            mode = PlanningMode.All,
          )
        }
      }
      .onEach { newState -> _filterStateFlow.value = newState }
      .launchIn(scope)
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
