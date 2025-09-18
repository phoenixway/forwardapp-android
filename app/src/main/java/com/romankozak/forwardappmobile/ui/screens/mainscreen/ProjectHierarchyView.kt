package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectHierarchyView(
    modifier: Modifier = Modifier,
    hierarchy: ListHierarchyData,
    focusedProjectId: String?,
    highlightedProjectId: String?,
    searchQuery: String,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    hierarchySettings: HierarchyDisplaySettings,
    listState: LazyListState,
    longDescendantsMap: Map<String, Boolean>,
    // Lambdas for events
    onProjectClicked: (String) -> Unit,
    onToggleExpanded: (Project) -> Unit,
    onMenuRequested: (Project) -> Unit,
    onNavigateToProject: (String) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit
) {
    val dragAndDropState = rememberDragAndDropState<Project>()

    DragAndDropContainer(
        state = dragAndDropState,
        enabled = !isSearchActive,
        modifier = modifier,
    ) {
        if (focusedProjectId != null) {
            FocusedListView(
                focusedProjectId = focusedProjectId,
                hierarchy = hierarchy,
                dragAndDropState = dragAndDropState,
                isSearchActive = isSearchActive,
                planningMode = planningMode,
                highlightedProjectId = highlightedProjectId,
                settings = hierarchySettings,
                searchQuery = searchQuery,
                onNavigateToProject = onNavigateToProject,
                longDescendantsMap = longDescendantsMap,
                // Pass lambdas
                onProjectClick = onProjectClicked,
                onToggleExpanded = onToggleExpanded,
                onMenuRequested = onMenuRequested,
                onProjectReorder = onProjectReorder
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
                        // Pass lambdas
                        onProjectClick = onProjectClicked,
                        onToggleExpanded = onToggleExpanded,
                        onMenuRequested = onMenuRequested,
                        onProjectReorder = onProjectReorder
                    )
                }
            }
        }
    }
}