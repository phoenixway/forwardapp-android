package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildDirectChildrenByOrientationNodeId
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationDisplayChildMap

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyView(
    modifier: Modifier = Modifier,
    hierarchy: ContextHierarchyData,
    flattenedHierarchy: List<FlatHierarchyItem>,
    orientationHierarchy: List<OrientationHierarchyItem>,
    breadcrumbs: List<BreadcrumbItem>,
    focusedProjectId: String?,
    focusedOrientationNodeId: String?,
    highlightedProjectId: String?,
    searchQuery: String,
    isSearchActive: Boolean,
    hierarchySettings: HierarchyDisplaySettings,
    listState: LazyListState,
    longDescendantsMap: Map<String, Boolean>,
    selectedContextIds: Set<String>,
    clipboardContextIds: Set<String>,
    isSelectionMode: Boolean,
    isSiblingReorderMode: Boolean,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    onEditBeacon: (String) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
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
    val directChildrenByNodeId =
        remember(orientationHierarchy) {
            buildDirectChildrenByOrientationNodeId(orientationHierarchy)
        }
    val displayChildMap =
        remember(hierarchy.childMap, orientationHierarchy, directChildrenByNodeId) {
            buildOrientationDisplayChildMap(
                canonicalChildMap = hierarchy.childMap,
                orientationHierarchy = orientationHierarchy,
                directChildrenByNodeId = directChildrenByNodeId,
            )
        }

    DragAndDropContainer(
        state = dragAndDropState,
        enabled = !isSearchActive && !isSelectionMode && !isSiblingReorderMode,
        modifier = modifier,
    ) {
        if (focusedProjectId != null) {
            FocusedProjectView(
                focusedProjectId = focusedProjectId,
                hierarchy = hierarchy,
                displayChildMap = displayChildMap,
                directChildrenByNodeId = directChildrenByNodeId,
                breadcrumbs = breadcrumbs,
                dragAndDropState = dragAndDropState,
                isSearchActive = isSearchActive,
                highlightedProjectId = highlightedProjectId,
                settings = hierarchySettings,
                searchQuery = searchQuery,
                longDescendantsMap = longDescendantsMap,
                isSelectionMode = isSelectionMode,
                isSiblingReorderMode = isSiblingReorderMode,
                selectedContextIds = selectedContextIds,
                clipboardContextIds = clipboardContextIds,
                onEvent = onEvent,
                onEditBeacon = onEditBeacon,
                onDeleteBeacon = onDeleteBeacon,
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
        } else if (focusedOrientationNodeId != null) {
            val focusedRoot =
                remember(orientationHierarchy, focusedOrientationNodeId) {
                    orientationHierarchy.firstOrNull { it.node.id == focusedOrientationNodeId }
                }
            val directChildren =
                remember(orientationHierarchy, focusedOrientationNodeId) {
                    directChildrenByNodeId[focusedOrientationNodeId].orEmpty()
                }
            FocusedOrientationNodeView(
                focusedRoot = focusedRoot,
                directChildren = directChildren,
                directChildrenByNodeId = directChildrenByNodeId,
                orientationHierarchy = orientationHierarchy,
                displayChildMap = displayChildMap,
                dragAndDropState = dragAndDropState,
                isSearchActive = isSearchActive,
                highlightedProjectId = highlightedProjectId,
                settings = hierarchySettings,
                searchQuery = searchQuery,
                longDescendantsMap = longDescendantsMap,
                isSelectionMode = isSelectionMode,
                isSiblingReorderMode = isSiblingReorderMode,
                selectedContextIds = selectedContextIds,
                onEvent = onEvent,
                onEditBeacon = onEditBeacon,
                onDeleteBeacon = onDeleteBeacon,
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
                remember(orientationHierarchy) {
                    orientationHierarchy
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
                if (orientationHierarchy.isNotEmpty() && !isSearchActive) {
                    val rootItems = orientationHierarchy.filter { it.level == 0 }
                    val rootGroups =
                        rootItems
                            .filter { it.node is OrientationHierarchyNode.Group }
                            .distinctBy { it.node.id }
                    itemsIndexed(
                        items = rootItems,
                        key = { index, item -> "${item.node.id}-$index" },
                    ) { _, item ->
                        when (val node = item.node) {
                            is OrientationHierarchyNode.Group ->
                                RootGroupRow(
                                    item = item,
                                    rootGroups = rootGroups,
                                    isSiblingReorderMode = isSiblingReorderMode,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onEvent = onEvent,
                                )
                            is OrientationHierarchyNode.Beacon ->
                                BeaconRootHeaderRow(
                                    node = node,
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.OrientationNodeClick(node.id)) },
                                    onEditBeacon = { onEditBeacon(node.id) },
                                    onDeleteBeacon = { onDeleteBeacon(node.id) },
                                    onCopyBeacon = { onEvent(ContextHierarchyScreenEvent.CopyBeacon(node.id)) },
                                    onCopyBeaconAsLink = {
                                        onEvent(ContextHierarchyScreenEvent.CopyBeaconAsLink(node.id))
                                    },
                                    onCutBeacon = { onEvent(ContextHierarchyScreenEvent.CutBeacon(node.id)) },
                                    onPasteBeacon = {
                                        onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoBeacon(node.id))
                                    },
                                )
                            OrientationHierarchyNode.NoGroup ->
                                NoGroupRootHeaderRow(
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.OrientationNodeClick(node.id)) },
                                    onPasteBeacon = {
                                        onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(null))
                                    },
                                )
                            OrientationHierarchyNode.NoBeacon ->
                                NoBeaconRootHeaderRow(
                                    level = item.level,
                                    childCount = rootChildCounts[node.id] ?: 0,
                                    onClick = { onEvent(ContextHierarchyScreenEvent.OrientationNodeClick(node.id)) },
                                )
                            is OrientationHierarchyNode.ContextNode ->
                                HierarchyListItem(
                                    item =
                                        FlatHierarchyItem(
                                            project = node.context,
                                            level = item.level,
                                            isLinkedAppearance = node.isLinkedAppearance,
                                        ),
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

@Composable
private fun RootGroupRow(
    item: OrientationHierarchyItem,
    rootGroups: List<OrientationHierarchyItem>,
    isSiblingReorderMode: Boolean,
    childCount: Int,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
) {
    val node = item.node as OrientationHierarchyNode.Group
    val groupIndex = rootGroups.indexOfFirst { it.node.id == node.id }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BeaconGroupRootHeaderRow(
            node = node,
            level = item.level,
            childCount = childCount,
            onClick = { onEvent(ContextHierarchyScreenEvent.OrientationNodeClick(node.id)) },
            onPasteBeacon = {
                onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(node.id))
            },
            modifier = Modifier.weight(1f),
        )
        if (isSiblingReorderMode && rootGroups.size > 1) {
            RootGroupReorderControls(
                canMoveUp = groupIndex > 0,
                canMoveDown = groupIndex < rootGroups.lastIndex,
                onMoveUp = {
                    onEvent(
                        ContextHierarchyScreenEvent.ReorderOrientationGroups(
                            orderedGroupIds = moveRootItem(rootGroups, groupIndex, -1).map { it.node.id },
                        ),
                    )
                },
                onMoveDown = {
                    onEvent(
                        ContextHierarchyScreenEvent.ReorderOrientationGroups(
                            orderedGroupIds = moveRootItem(rootGroups, groupIndex, 1).map { it.node.id },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun RootGroupReorderControls(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column {
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move group up")
        }
        Spacer(modifier = Modifier.height(2.dp))
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move group down")
        }
    }
}

private fun <T> moveRootItem(
    items: List<T>,
    fromIndex: Int,
    delta: Int,
): List<T> {
    val targetIndex = (fromIndex + delta).coerceIn(items.indices)
    if (targetIndex == fromIndex) return items
    return items.toMutableList().also { mutable ->
        val moved = mutable.removeAt(fromIndex)
        mutable.add(targetIndex, moved)
    }
}
