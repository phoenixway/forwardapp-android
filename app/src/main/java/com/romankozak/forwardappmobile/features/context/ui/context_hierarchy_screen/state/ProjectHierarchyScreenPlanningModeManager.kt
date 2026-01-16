

package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.state

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectHierarchyScreenPlanningMode
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


typealias PlanningModeManager = ProjectHierarchyScreenPlanningModeManager

@ViewModelScoped
class ProjectHierarchyScreenPlanningModeManager
    @Inject
    constructor() {
        private val _planningMode = MutableStateFlow<ProjectHierarchyScreenPlanningMode>(ProjectHierarchyScreenPlanningMode.All)
        val planningMode: StateFlow<PlanningMode> = _planningMode.asStateFlow()

        private val _expandedInDailyMode = MutableStateFlow<Set<String>>(emptySet())
        val expandedInDailyMode: StateFlow<Set<String>> = _expandedInDailyMode.asStateFlow()

        private val _expandedInMediumMode = MutableStateFlow<Set<String>>(emptySet())
        val expandedInMediumMode: StateFlow<Set<String>> = _expandedInMediumMode.asStateFlow()

        private val _expandedInLongMode = MutableStateFlow<Set<String>>(emptySet())
        val expandedInLongMode: StateFlow<Set<String>> = _expandedInLongMode.asStateFlow()

        fun changeMode(mode: PlanningMode) {
            _planningMode.value = mode
        }

        fun toggleExpandedInPlanningMode(project: Project) {
            val currentStateFlow =
                when (_planningMode.value) {
                    PlanningMode.Today -> _expandedInDailyMode
                    PlanningMode.Medium -> _expandedInMediumMode
                    PlanningMode.Long -> _expandedInLongMode
                    else -> return
                }

            val currentExpanded = currentStateFlow.value.toMutableSet()
            if (project.isExpanded) {
                currentExpanded.remove(project.id)
            } else {
                currentExpanded.add(project.id)
            }
            currentStateFlow.value = currentExpanded
        }

        fun resetExpansionStates() {
            _expandedInDailyMode.value = emptySet()
            _expandedInMediumMode.value = emptySet()
            _expandedInLongMode.value = emptySet()
        }
    }
