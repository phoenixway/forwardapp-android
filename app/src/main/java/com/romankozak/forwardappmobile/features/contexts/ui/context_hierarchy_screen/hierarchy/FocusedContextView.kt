package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbs

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FocusedProjectView(
    focusedProjectId: String,
    hierarchy: ContextHierarchyData,
    displayChildMap: Map<String, List<Context>>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
    breadcrumbs: List<BreadcrumbItem>,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    longDescendantsMap: Map<String, Boolean>,
    isSelectionMode: Boolean,
    isSiblingReorderMode: Boolean,
    selectedContextIds: Set<String>,
    clipboardContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    onEditBeacon: (String) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit,
    onStartSelection: (String) -> Unit,
    onFocusProject: (Context) -> Unit,
    onAddSubproject: (Context) -> Unit,
    onDeleteProject: (Context) -> Unit,
    onEditProject: (Context) -> Unit,
    onProjectClick: (String) -> Unit,
    onMenuRequested: (Context) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val focusedProject = hierarchy.allProjects.find { it.id == focusedProjectId }
    val canPasteIntoFocusedProject =
        clipboardContextIds.isNotEmpty() && focusedProjectId !in clipboardContextIds
    val children =
        remember(directChildrenByNodeId, focusedProjectId, displayChildMap) {
            directChildrenByNodeId[focusedProjectId].orEmpty()
                .mapNotNull { item -> (item.node as? OrientationHierarchyNode.ContextNode)?.context }
                .ifEmpty { displayChildMap[focusedProjectId].orEmpty() }
                .distinctBy { it.id }
                .map { child -> FlatHierarchyItem(project = child, level = 0) }
        }

    if (focusedProject != null) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            stickyHeader(key = "focused-project-header") {
                Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                    BreadcrumbNavigation(
                        breadcrumbs = breadcrumbs,
                        onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                        onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                        onFocusedListMenuClick = { projectId ->
                            hierarchy.allProjects
                                .find { it.id == projectId }
                                ?.let { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusedProjectHeader(
                        project = focusedProject,
                        onMoreActionsClick = { onMenuRequested(focusedProject) },
                        onProjectClick = { onProjectClick(focusedProject.id) },
                    )
                }
            }

            if (children.isNotEmpty()) {
                itemsIndexed(children, key = { index, item -> "${item.project.id}-$index" }) { index, item ->
                    ReorderableContextRow(
                        item = item,
                        siblings = children,
                        index = index,
                        isSiblingReorderMode = isSiblingReorderMode,
                        parentContextId = focusedProjectId,
                        displayChildMap = displayChildMap,
                        dragAndDropState = dragAndDropState,
                        isSearchActive = isSearchActive,
                        highlightedProjectId = highlightedProjectId,
                        settings = settings,
                        searchQuery = searchQuery,
                        focusedProjectId = focusedProjectId,
                        longDescendantsMap = longDescendantsMap,
                        isSelectionMode = isSelectionMode,
                        selectedContextIds = selectedContextIds,
                        onEvent = onEvent,
                        onProjectClick = onProjectClick,
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
            } else {
                item(key = "empty_state") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
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
                                text = "No subcontexts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (canPasteIntoFocusedProject) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onEvent(ContextHierarchyScreenEvent.PasteContextLink(focusedProject))
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "Вставити сюди",
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Focused context not found.")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FocusedOrientationNodeView(
    focusedRoot: OrientationHierarchyItem?,
    directChildren: List<OrientationHierarchyItem>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
    orientationHierarchy: List<OrientationHierarchyItem>,
    displayChildMap: Map<String, List<Context>>,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    longDescendantsMap: Map<String, Boolean>,
    isSelectionMode: Boolean,
    isSiblingReorderMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    onEditBeacon: (String) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit,
    onStartSelection: (String) -> Unit,
    onFocusProject: (Context) -> Unit,
    onAddSubproject: (Context) -> Unit,
    onDeleteProject: (Context) -> Unit,
    onEditProject: (Context) -> Unit,
    onProjectClick: (String) -> Unit,
    onMenuRequested: (Context) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    if (focusedRoot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Focused root not found.")
        }
        return
    }

    val rootBreadcrumb =
        remember(orientationHierarchy, focusedRoot) {
            buildOrientationBreadcrumbs(
                items = orientationHierarchy,
                nodeId = focusedRoot.node.id,
            )
        }
    val childItems =
        remember(directChildren) {
            directChildren.mapNotNull { item ->
                (item.node as? OrientationHierarchyNode.ContextNode)?.let { node ->
                    FlatHierarchyItem(
                        project = node.context,
                        level = 0,
                        isLinkedAppearance = node.isLinkedAppearance,
                    )
                }
            }.distinctBy { it.project.id }
        }
    val canReorderContextChildren =
        (
            focusedRoot.node is OrientationHierarchyNode.NoBeacon ||
                focusedRoot.node is OrientationHierarchyNode.Beacon
        ) && childItems.size > 1
    val beaconChildren =
        directChildren
            .filter { it.node is OrientationHierarchyNode.Beacon }
            .distinctBy { it.node.id }
    val canReorderBeaconChildren = beaconChildren.size > 1

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader(key = "focused-orientation-header") {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                BreadcrumbNavigation(
                    breadcrumbs = rootBreadcrumb,
                    onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                when (val node = focusedRoot.node) {
                    is OrientationHierarchyNode.Group ->
                        BeaconGroupRootHeaderRow(
                            node = node,
                            level = 0,
                            childCount = directChildren.size,
                            onPasteBeacon = {
                                onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(node.id))
                            },
                        )
                    is OrientationHierarchyNode.Beacon ->
                        BeaconRootHeaderRow(
                            node = node,
                            level = 0,
                            childCount = directChildren.size,
                            onEditBeacon = { onEditBeacon(node.id) },
                            onDeleteBeacon = { onDeleteBeacon(node.id) },
                            onCopyBeacon = { onEvent(ContextHierarchyScreenEvent.CopyBeacon(node.id)) },
                            onCopyBeaconAsLink = { onEvent(ContextHierarchyScreenEvent.CopyBeaconAsLink(node.id)) },
                            onCutBeacon = { onEvent(ContextHierarchyScreenEvent.CutBeacon(node.id)) },
                            onPasteBeacon = {
                                onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoBeacon(node.id))
                            },
                        )
                    OrientationHierarchyNode.NoGroup ->
                        NoGroupRootHeaderRow(
                            level = 0,
                            childCount = directChildren.size,
                            onPasteBeacon = {
                                onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(null))
                            },
                        )
                    OrientationHierarchyNode.NoBeacon ->
                        NoBeaconRootHeaderRow(
                            level = 0,
                            childCount = directChildren.size,
                        )
                    is OrientationHierarchyNode.ContextNode -> Unit
                }
            }
        }

        if (beaconChildren.isNotEmpty()) {
            itemsIndexed(beaconChildren, key = { index, item -> "beacon-${item.node.id}-$index" }) { _, item ->
                val index = beaconChildren.indexOfFirst { it.node.id == item.node.id }
                val node = item.node as OrientationHierarchyNode.Beacon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BeaconRootHeaderRow(
                        node = node,
                        level = 0,
                        childCount = directChildrenByNodeId[node.id].orEmpty().size,
                        onClick = { onEvent(ContextHierarchyScreenEvent.OrientationNodeClick(node.id)) },
                        onEditBeacon = { onEditBeacon(node.id) },
                        onDeleteBeacon = { onDeleteBeacon(node.id) },
                        onCopyBeacon = { onEvent(ContextHierarchyScreenEvent.CopyBeacon(node.id)) },
                        onCopyBeaconAsLink = { onEvent(ContextHierarchyScreenEvent.CopyBeaconAsLink(node.id)) },
                        onCutBeacon = { onEvent(ContextHierarchyScreenEvent.CutBeacon(node.id)) },
                        onPasteBeacon = { onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoBeacon(node.id)) },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    if (isSiblingReorderMode && canReorderBeaconChildren) {
                        ReorderControls(
                            canMoveUp = index > 0,
                            canMoveDown = index < beaconChildren.lastIndex,
                            onMoveUp = {
                                onEvent(
                                    ContextHierarchyScreenEvent.ReorderOrientationBeaconSiblings(
                                        parentNodeId = focusedRoot.node.id,
                                        orderedBeaconIds = moveItem(beaconChildren, index, -1).map { it.node.id },
                                    ),
                                )
                            },
                            onMoveDown = {
                                onEvent(
                                    ContextHierarchyScreenEvent.ReorderOrientationBeaconSiblings(
                                        parentNodeId = focusedRoot.node.id,
                                        orderedBeaconIds = moveItem(beaconChildren, index, 1).map { it.node.id },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        if (childItems.isNotEmpty()) {
            itemsIndexed(childItems, key = { index, item -> "context-${item.project.id}-$index" }) { index, item ->
                val parentContextId =
                    if (focusedRoot.node is OrientationHierarchyNode.NoBeacon) {
                        null
                    } else {
                        focusedRoot.node.id
                    }
                ReorderableContextRow(
                    item = item,
                    siblings = childItems,
                    index = index,
                    isSiblingReorderMode = isSiblingReorderMode && canReorderContextChildren,
                    parentContextId = parentContextId,
                    displayChildMap = displayChildMap,
                    dragAndDropState = dragAndDropState,
                    isSearchActive = isSearchActive,
                    highlightedProjectId = highlightedProjectId,
                    settings = settings,
                    searchQuery = searchQuery,
                    focusedProjectId = null,
                    longDescendantsMap = longDescendantsMap,
                    isSelectionMode = isSelectionMode,
                    selectedContextIds = selectedContextIds,
                    onEvent = onEvent,
                    onProjectClick = onProjectClick,
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
        if (childItems.isEmpty() && beaconChildren.isEmpty()) {
            item(key = "empty_state") {
                FocusedEmptyState(text = "No contexts")
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReorderableContextRow(
    item: FlatHierarchyItem,
    siblings: List<FlatHierarchyItem>,
    index: Int,
    isSiblingReorderMode: Boolean,
    parentContextId: String?,
    displayChildMap: Map<String, List<Context>>,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    focusedProjectId: String?,
    longDescendantsMap: Map<String, Boolean>,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    onProjectClick: (String) -> Unit,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        HierarchyListItem(
            item = item,
            childMap = displayChildMap,
            dragAndDropState = dragAndDropState,
            isSearchActive = isSearchActive,
            highlightedProjectId = highlightedProjectId,
            settings = settings,
            searchQuery = searchQuery,
            focusedProjectId = focusedProjectId,
            longDescendantsMap = longDescendantsMap,
            isSelectionMode = isSelectionMode,
            selectedContextIds = selectedContextIds,
            onProjectClick = onProjectClick,
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
            modifier = Modifier.weight(1f),
        )
        if (isSiblingReorderMode) {
            ReorderControls(
                canMoveUp = index > 0,
                canMoveDown = index < siblings.lastIndex,
                onMoveUp = {
                    onEvent(
                        ContextHierarchyScreenEvent.ReorderContextSiblings(
                            parentContextId = parentContextId,
                            orderedContextIds = moveItem(siblings, index, -1).map { it.project.id },
                        ),
                    )
                },
                onMoveDown = {
                    onEvent(
                        ContextHierarchyScreenEvent.ReorderContextSiblings(
                            parentContextId = parentContextId,
                            orderedContextIds = moveItem(siblings, index, 1).map { it.project.id },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun ReorderControls(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(modifier = Modifier.padding(end = 4.dp)) {
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
        }
    }
}

private fun <T> moveItem(
    items: List<T>,
    fromIndex: Int,
    delta: Int,
): List<T> {
    val targetIndex = (fromIndex + delta).coerceIn(items.indices)
    if (targetIndex == fromIndex) return items
    return items.toMutableList().also { mutable ->
        val item = mutable.removeAt(fromIndex)
        mutable.add(targetIndex, item)
    }
}

@Composable
private fun FocusedEmptyState(text: String) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
