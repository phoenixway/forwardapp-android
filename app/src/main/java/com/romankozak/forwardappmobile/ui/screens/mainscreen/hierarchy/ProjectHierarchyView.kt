package com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.FocusedProjectHeader
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode

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
            val focusedProject = hierarchy.allProjects.find { it.id == focusedProjectId }
            val children = (hierarchy.childMap[focusedProjectId] ?: emptyList()).sortedBy { it.order }

            if (focusedProject != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    stickyHeader(key = "focused-project-header") {
                        FocusedProjectHeader(
                            project = focusedProject,
                            onMoreActionsClick = { onMenuRequested(focusedProject) }
                        )
                    }

                    if (children.isNotEmpty()) {
                        items(children, key = { it.id }) { child ->
                            SmartHierarchyView(
                                project = child,
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
                                onProjectClick = onProjectClicked,
                                onToggleExpanded = onToggleExpanded,
                                onMenuRequested = onMenuRequested,
                                onProjectReorder = onProjectReorder
                            )
                        }
                    } else {
                        item(key = "empty_state") {
                            Box(
                                modifier = Modifier.fillParentMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.Inbox,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No subprojects",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Handle case where focused project is not found (should not happen)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Focused project not found.")
                }
            }
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