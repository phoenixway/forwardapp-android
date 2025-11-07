package com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyView(
    modifier: Modifier = Modifier,
    hierarchy: ListHierarchyData,
    breadcrumbs: List<BreadcrumbItem>,
    focusedProjectId: String?,
    highlightedProjectId: String?,
    searchQuery: String,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    hierarchySettings: HierarchyDisplaySettings,
    listState: LazyListState,
    longDescendantsMap: Map<String, Boolean>,
    onEvent: (MainScreenEvent) -> Unit,
    
    onProjectClicked: (String) -> Unit,
    onToggleExpanded: (Project) -> Unit,
    onMenuRequested: (Project) -> Unit,
    onNavigateToProject: (String) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val dragAndDropState = rememberDragAndDropState<Project>()

    DragAndDropContainer(
        state = dragAndDropState,
        enabled = !isSearchActive,
        modifier = modifier,
    ) {
        if (focusedProjectId != null) {
            FocusedProjectView(
                focusedProjectId = focusedProjectId,
                hierarchy = hierarchy,
                breadcrumbs = breadcrumbs,
                dragAndDropState = dragAndDropState,
                isSearchActive = isSearchActive,
                planningMode = planningMode,
                highlightedProjectId = highlightedProjectId,
                settings = hierarchySettings,
                searchQuery = searchQuery,
                onNavigateToProject = onNavigateToProject,
                longDescendantsMap = longDescendantsMap,
                onEvent = onEvent,
                onProjectClick = onProjectClicked,
                onToggleExpanded = onToggleExpanded,
                onMenuRequested = onMenuRequested,
                onProjectReorder = onProjectReorder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(hierarchy.topLevelProjects, key = { it.id }) { topLevelProject ->
                    SmartHierarchyView(
                        project = topLevelProject,
                        childMap = hierarchy.childMap,
                        level = 0,
                        dragAndDropState = dragAndDropState,
                        isSearchActive = isSearchActive,
                        planningMode = planningMode,
                        highlightedProjectId = highlightedProjectId,
                        settings = hierarchySettings,
                        searchQuery = searchQuery,
                        onNavigateToProject = onNavigateToProject,
                        focusedProjectId = focusedProjectId,
                        longDescendantsMap = longDescendantsMap,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onProjectClick = onProjectClicked,
                        onToggleExpanded = onToggleExpanded,
                        onMenuRequested = onMenuRequested,
                        onProjectReorder = onProjectReorder,
                    )
                }
            }
        }
    }
}
