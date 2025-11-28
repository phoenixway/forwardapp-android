package com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.flattenHierarchyWithLevels

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FocusedProjectView(
  focusedProjectId: String,
  hierarchy: ListHierarchyData,
  breadcrumbs: List<BreadcrumbItem>,
  dragAndDropState: DragAndDropState<Project>,
  isSearchActive: Boolean,
  planningMode: PlanningMode,
  highlightedProjectId: String?,
  settings: HierarchyDisplaySettings,
  searchQuery: String,
  longDescendantsMap: Map<String, Boolean>,
  onEvent: (MainScreenEvent) -> Unit,
  onFocusProject: (Project) -> Unit,
  onAddSubproject: (Project) -> Unit,
  onDeleteProject: (Project) -> Unit,
  onEditProject: (Project) -> Unit,
  onProjectClick: (String) -> Unit,
  onToggleExpanded: (Project) -> Unit,
  onMenuRequested: (Project) -> Unit,
  onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,

  ) {
  val focusedProject = hierarchy.allProjects.find { it.id == focusedProjectId }
  val children = (hierarchy.childMap[focusedProjectId] ?: emptyList()).sortedBy { it.order }

  if (focusedProject != null) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      stickyHeader(key = "focused-project-header") {
        Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
          BreadcrumbNavigation(
            breadcrumbs = breadcrumbs,
            onNavigate = { onEvent(MainScreenEvent.BreadcrumbNavigation(it)) },
            onClearNavigation = { onEvent(MainScreenEvent.ClearBreadcrumbNavigation) },
            onFocusedListMenuClick = { projectId ->
              hierarchy.allProjects
                .find { it.id == projectId }
                ?.let { onEvent(MainScreenEvent.ProjectMenuRequest(it)) }
            },
          )
        }
      }

      if (children.isNotEmpty()) {
        val flattenedChildren =
          remember(children, hierarchy.childMap) {
            flattenHierarchyWithLevels(children, hierarchy.childMap)
          }
        val visibleItems =
          remember(flattenedChildren, longDescendantsMap, hierarchy.childMap) {
            buildVisibleHierarchy(flattenedChildren, hierarchy.childMap, longDescendantsMap)
          }
        items(visibleItems, key = { it.project.id }) { item ->
          HierarchyListItem(
            item = item,
            childMap = hierarchy.childMap,
            dragAndDropState = dragAndDropState,
            isSearchActive = isSearchActive,
            planningMode = planningMode,
            highlightedProjectId = highlightedProjectId,
            settings = settings,
            searchQuery = searchQuery,
            focusedProjectId = focusedProjectId,
            longDescendantsMap = longDescendantsMap,
            onProjectClick = onProjectClick,
            onToggleExpanded = onToggleExpanded,
            onMenuRequested = onMenuRequested,
            onProjectReorder = onProjectReorder,
            onFocusProject = onFocusProject,
            onAddSubproject = onAddSubproject,
            onDeleteProject = onDeleteProject,
            onEditProject = onEditProject,
            sharedTransitionScope =  sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
          )
        }
      } else {
        item(key = "empty_state") {
          Box(
            modifier = Modifier.fillParentMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No subprojects",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  } else {

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Focused project not found.")
    }
  }
}
