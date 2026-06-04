package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyView(
    modifier: Modifier = Modifier,
    hierarchy: ContextHierarchyData,
    flattenedHierarchy: List<FlatHierarchyItem>,
    beaconRootedHierarchy: List<BeaconRootedHierarchyItem>,
    breadcrumbs: List<BreadcrumbItem>,
    focusedProjectId: String?,
    focusedBeaconNodeId: String?,
    highlightedProjectId: String?,
    searchQuery: String,
    isSearchActive: Boolean,
    hierarchySettings: HierarchyDisplaySettings,
    listState: LazyListState,
    longDescendantsMap: Map<String, Boolean>,
    selectedContextIds: Set<String>,
    isSelectionMode: Boolean,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    onProjectClicked: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onStartSelection: (String) -> Unit,
    onMenuRequested: (Context) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    onFocusProject: (Context) -> Unit,
    onAddSubproject: (Context) -> Unit,
    onDeleteProject: (Context) -> Unit,
    onEditProject: (Context) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val dragAndDropState = rememberDragAndDropState<Context>()
    val displayChildMap =
        remember(hierarchy.childMap, beaconRootedHierarchy) {
            buildDisplayChildMap(
                canonicalChildMap = hierarchy.childMap,
                beaconRootedHierarchy = beaconRootedHierarchy,
            )
        }
    val directChildrenByNodeId =
        remember(beaconRootedHierarchy) {
            buildDirectChildrenByNodeId(beaconRootedHierarchy)
        }

    DragAndDropContainer(
        state = dragAndDropState,
        enabled = !isSearchActive && !isSelectionMode,
        modifier = modifier,
    ) {
        if (focusedProjectId != null) {
            FocusedProjectView(
                focusedProjectId = focusedProjectId,
                hierarchy = hierarchy,
                displayChildMap = displayChildMap,
                beaconRootedHierarchy = beaconRootedHierarchy,
                breadcrumbs = breadcrumbs,
                dragAndDropState = dragAndDropState,
                isSearchActive = isSearchActive,
                highlightedProjectId = highlightedProjectId,
                settings = hierarchySettings,
                searchQuery = searchQuery,
                longDescendantsMap = longDescendantsMap,
                isSelectionMode = isSelectionMode,
                selectedContextIds = selectedContextIds,
                onEvent = onEvent,
                onToggleSelection = onToggleSelection,
                onStartSelection = onStartSelection,
                onProjectClick = onProjectClicked,
                onMenuRequested = onMenuRequested,
                onProjectReorder = onProjectReorder,
                onFocusProject = onFocusProject,
                onAddSubproject = onAddSubproject,
                onDeleteProject = onDeleteProject,
                onEditProject = onEditProject,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        } else if (focusedBeaconNodeId != null) {
            val focusedRoot =
                remember(beaconRootedHierarchy, focusedBeaconNodeId) {
                    beaconRootedHierarchy.firstOrNull { it.node.id == focusedBeaconNodeId }
                }
            val directChildren =
                remember(beaconRootedHierarchy, focusedBeaconNodeId) {
                    directChildrenByNodeId[focusedBeaconNodeId].orEmpty()
                }
            FocusedBeaconRootView(
                focusedRoot = focusedRoot,
                directChildren = directChildren,
                beaconRootedHierarchy = beaconRootedHierarchy,
                displayChildMap = displayChildMap,
                dragAndDropState = dragAndDropState,
                isSearchActive = isSearchActive,
                highlightedProjectId = highlightedProjectId,
                settings = hierarchySettings,
                searchQuery = searchQuery,
                longDescendantsMap = longDescendantsMap,
                isSelectionMode = isSelectionMode,
                selectedContextIds = selectedContextIds,
                onEvent = onEvent,
                onToggleSelection = onToggleSelection,
                onStartSelection = onStartSelection,
                onProjectClick = onProjectClicked,
                onMenuRequested = onMenuRequested,
                onProjectReorder = onProjectReorder,
                onFocusProject = onFocusProject,
                onAddSubproject = onAddSubproject,
                onDeleteProject = onDeleteProject,
                onEditProject = onEditProject,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        } else {
            val rootChildCounts =
                remember(beaconRootedHierarchy) {
                    beaconRootedHierarchy
                        .filter { it.level == 0 }
                        .associate { item ->
                            item.node.id to
                                directChildrenByNodeId[item.node.id].orEmpty().size
                        }
                }
            val visibleItems =
                remember(flattenedHierarchy, longDescendantsMap, hierarchy.childMap) {
                    buildVisibleHierarchy(
                        flattenedHierarchy,
                        hierarchy.childMap,
                        longDescendantsMap,
                    )
                }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (beaconRootedHierarchy.isNotEmpty() && !isSearchActive) {
                    val rootItems = beaconRootedHierarchy.filter { it.level == 0 }
                    itemsIndexed(
                        items = rootItems,
                        key = { index, item -> "${item.node.id}-$index" },
                    ) { _, item ->
                        when (val node = item.node) {
                            is BeaconRootedHierarchyNode.Group ->
                                BeaconGroupRootHeaderRow(
                                    node = node,
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.BeaconRootClick(node.id)) },
                                )
                            is BeaconRootedHierarchyNode.Beacon ->
                                BeaconRootHeaderRow(
                                    node = node,
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.BeaconRootClick(node.id)) },
                                )
                            BeaconRootedHierarchyNode.NoGroup ->
                                NoGroupRootHeaderRow(
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.BeaconRootClick(node.id)) },
                                )
                            BeaconRootedHierarchyNode.NoBeacon ->
                                NoBeaconRootHeaderRow(
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.BeaconRootClick(node.id)) },
                                )
                            is BeaconRootedHierarchyNode.ContextNode ->
                                HierarchyListItem(
                                    item = FlatHierarchyItem(project = node.context, level = item.level),
                                    childMap = displayChildMap,
                                    dragAndDropState = dragAndDropState,
                                    isSearchActive = isSearchActive,
                                    highlightedProjectId = highlightedProjectId,
                                    settings = hierarchySettings,
                                    searchQuery = searchQuery,
                                    focusedProjectId = focusedProjectId,
                                    longDescendantsMap = longDescendantsMap,
                                    isSelectionMode = isSelectionMode,
                                    selectedContextIds = selectedContextIds,
                                    onProjectClick = onProjectClicked,
                                    onToggleSelection = onToggleSelection,
                                    onStartSelection = onStartSelection,
                                    onMenuRequested = onMenuRequested,
                                    onProjectReorder = onProjectReorder,
                                    onFocusProject = onFocusProject,
                                    onAddSubproject = onAddSubproject,
                                    onDeleteProject = onDeleteProject,
                                    onEditProject = onEditProject,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                        }
                    }
                } else {
                    items(visibleItems, key = { it.project.id }) { item ->
                        HierarchyListItem(
                            item = item,
                            childMap = displayChildMap,
                            dragAndDropState = dragAndDropState,
                            isSearchActive = isSearchActive,
                            highlightedProjectId = highlightedProjectId,
                            settings = hierarchySettings,
                            searchQuery = searchQuery,
                            focusedProjectId = focusedProjectId,
                            longDescendantsMap = longDescendantsMap,
                            isSelectionMode = isSelectionMode,
                            selectedContextIds = selectedContextIds,
                            onProjectClick = onProjectClicked,
                            onToggleSelection = onToggleSelection,
                            onStartSelection = onStartSelection,
                            onMenuRequested = onMenuRequested,
                            onProjectReorder = onProjectReorder,
                            onFocusProject = onFocusProject,
                            onAddSubproject = onAddSubproject,
                            onDeleteProject = onDeleteProject,
                            onEditProject = onEditProject,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                }
            }
        }
    }
}
