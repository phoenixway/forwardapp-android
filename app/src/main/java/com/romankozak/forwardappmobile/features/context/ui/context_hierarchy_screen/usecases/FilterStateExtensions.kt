package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.PlanningMode

internal fun FilterState.withHierarchyFallback(allProjects: List<Project>): FilterState {
  if (!isReady) return this
  if (flatList.isNotEmpty()) return this
  if (allProjects.isEmpty()) return this
  if (searchActive) return this
  if (mode != PlanningMode.All) return this
  HierarchyDebugLogger.d {
    "FilterStateExtensions applying hierarchy fallback with allProjects size=${allProjects.size}"
  }
  return copy(flatList = allProjects)
}
