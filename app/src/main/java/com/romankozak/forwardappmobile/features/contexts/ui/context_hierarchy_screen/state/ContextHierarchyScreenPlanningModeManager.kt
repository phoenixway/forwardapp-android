
package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenPlanningMode
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
        private val _planningMode =
            MutableStateFlow<ProjectHierarchyScreenPlanningMode>(
                ProjectHierarchyScreenPlanningMode.All,
            )
        val planningMode: StateFlow<PlanningMode> = _planningMode.asStateFlow()

        fun changeMode(mode: PlanningMode) {
            _planningMode.value = mode
        }
    }
