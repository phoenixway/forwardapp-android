package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FocusedProjectView(
    focusedProjectId: String,
    hierarchy: ContextHierarchyData,
    displayChildMap: Map<String, List<Context>>,
    beaconRootedHierarchy: List<BeaconRootedHierarchyItem>,
    breadcrumbs: List<BreadcrumbItem>,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    longDescendantsMap: Map<String, Boolean>,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
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
    val children =
        remember(beaconRootedHierarchy, focusedProjectId, displayChildMap) {
            directChildrenOfNode(beaconRootedHierarchy, focusedProjectId)
                .mapNotNull { item -> (item.node as? BeaconRootedHierarchyNode.ContextNode)?.context }
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
                items(children, key = { it.project.id }) { item ->
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
fun FocusedBeaconRootView(
    focusedRoot: BeaconRootedHierarchyItem?,
    directChildren: List<BeaconRootedHierarchyItem>,
    beaconRootedHierarchy: List<BeaconRootedHierarchyItem>,
    displayChildMap: Map<String, List<Context>>,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    longDescendantsMap: Map<String, Boolean>,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
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
        remember(beaconRootedHierarchy, focusedRoot) {
            buildBeaconBreadcrumbs(
                items = beaconRootedHierarchy,
                nodeId = focusedRoot.node.id,
            )
        }
    val childItems =
        remember(directChildren) {
            directChildren.mapNotNull { item ->
                (item.node as? BeaconRootedHierarchyNode.ContextNode)?.context?.let { context ->
                    FlatHierarchyItem(project = context, level = 0)
                }
            }
        }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader(key = "focused-beacon-header") {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                BreadcrumbNavigation(
                    breadcrumbs = rootBreadcrumb,
                    onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                when (val node = focusedRoot.node) {
                    is BeaconRootedHierarchyNode.Group ->
                        BeaconGroupRootHeaderRow(
                            node = node,
                            level = 0,
                            childCount = directChildren.size,
                        )
                    is BeaconRootedHierarchyNode.Beacon ->
                        BeaconRootHeaderRow(
                            node = node,
                            level = 0,
                            childCount = directChildren.size,
                        )
                    BeaconRootedHierarchyNode.NoGroup ->
                        NoGroupRootHeaderRow(
                            level = 0,
                            childCount = directChildren.size,
                        )
                    BeaconRootedHierarchyNode.NoBeacon ->
                        NoBeaconRootHeaderRow(
                            level = 0,
                            childCount = directChildren.size,
                        )
                    is BeaconRootedHierarchyNode.ContextNode -> Unit
                }
            }
        }

        val beaconChildren = directChildren.filter { it.node is BeaconRootedHierarchyNode.Beacon }
        if (beaconChildren.isNotEmpty()) {
            items(beaconChildren, key = { "beacon-${it.node.id}" }) { item ->
                val node = item.node as BeaconRootedHierarchyNode.Beacon
                BeaconRootHeaderRow(
                    node = node,
                    level = 0,
                    childCount =
                        directChildrenOfNode(
                            items = beaconRootedHierarchy,
                            nodeId = node.id,
                        ).size,
                    onClick = { onEvent(ContextHierarchyScreenEvent.BeaconRootClick(node.id)) },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        if (childItems.isNotEmpty()) {
            items(childItems, key = { it.project.id }) { item ->
                HierarchyListItem(
                    item = item,
                    childMap = displayChildMap,
                    dragAndDropState = dragAndDropState,
                    isSearchActive = isSearchActive,
                    highlightedProjectId = highlightedProjectId,
                    settings = settings,
                    searchQuery = searchQuery,
                    focusedProjectId = null,
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
                )
            }
        } else if (beaconChildren.isEmpty()) {
            item(key = "empty_state") {
                FocusedEmptyState(text = "No contexts")
            }
        }
    }
}

private fun buildBeaconBreadcrumbs(
    items: List<BeaconRootedHierarchyItem>,
    nodeId: String,
): List<BreadcrumbItem> {
    val nodeIndex = items.indexOfFirst { it.node.id == nodeId }
    if (nodeIndex == -1) return emptyList()
    val targetItem = items[nodeIndex]
    val ancestors = ArrayDeque<BeaconRootedHierarchyItem>()
    var expectedLevel = targetItem.level - 1
    for (index in nodeIndex - 1 downTo 0) {
        val item = items[index]
        if (item.level == expectedLevel) {
            ancestors.addFirst(item)
            expectedLevel--
        }
        if (expectedLevel < 0) break
    }
    return (ancestors + targetItem).mapIndexed { index, item ->
        BreadcrumbItem(
            id = item.node.id,
            name = item.node.title,
            level = index,
            target = BreadcrumbTarget.BeaconRoot,
        )
    }
}

internal fun directChildrenOfNode(
    items: List<BeaconRootedHierarchyItem>,
    nodeId: String,
): List<BeaconRootedHierarchyItem> = buildDirectChildrenByNodeId(items)[nodeId].orEmpty()

internal fun buildDirectChildrenByNodeId(items: List<BeaconRootedHierarchyItem>): Map<String, List<BeaconRootedHierarchyItem>> {
    val result = linkedMapOf<String, MutableList<BeaconRootedHierarchyItem>>()
    val stack = ArrayDeque<BeaconRootedHierarchyItem>()
    items.forEach { item ->
        while (stack.isNotEmpty() && stack.last().level >= item.level) {
            stack.removeLast()
        }
        stack.lastOrNull()?.let { parent ->
            result.getOrPut(parent.node.id) { mutableListOf() } += item
        }
        stack.addLast(item)
    }
    return result
}

internal fun buildDisplayChildMap(
    canonicalChildMap: Map<String, List<Context>>,
    beaconRootedHierarchy: List<BeaconRootedHierarchyItem>,
): Map<String, List<Context>> {
    val result = canonicalChildMap.mapValues { (_, children) -> children.toMutableList() }.toMutableMap()
    val directChildrenByNodeId = buildDirectChildrenByNodeId(beaconRootedHierarchy)
    beaconRootedHierarchy
        .filter { it.node is BeaconRootedHierarchyNode.ContextNode }
        .forEach { parentItem ->
            val parentContext = (parentItem.node as BeaconRootedHierarchyNode.ContextNode).context
            val children =
                directChildrenByNodeId[parentContext.id].orEmpty().mapNotNull { childItem ->
                    (childItem.node as? BeaconRootedHierarchyNode.ContextNode)?.context
                }
            if (children.isNotEmpty()) {
                val mutableChildren = result.getOrPut(parentContext.id) { mutableListOf() }
                children.forEach { child ->
                    if (mutableChildren.none { it.id == child.id }) {
                        mutableChildren += child
                    }
                }
            }
        }
    return result
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
