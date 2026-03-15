package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode

internal fun FilterState.withHierarchyFallback(allProjects: List<Context>): FilterState {
    val shouldApplyHierarchyFallback = isReady &&
        flatList.isEmpty() &&
        allProjects.isNotEmpty() &&
        !searchActive &&
        mode == PlanningMode.All

    return if (shouldApplyHierarchyFallback) {
        HierarchyDebugLogger.d {
            "FilterStateExtensions applying hierarchy fallback with allProjects size=${allProjects.size}"
        }
        copy(flatList = allProjects)
    } else {
        this
    }
}
