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
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyPresentationItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbs

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FocusedProjectView(
    focusedProjectId: String,
    rawBackedProjectIds: Set<String>,
    presentationHierarchy: HierarchyPresentationData,
    orientationHierarchy: List<OrientationHierarchyItem>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
    breadcrumbs: List<BreadcrumbItem>,
    dragAndDropState: DragAndDropState<String>,
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
    onPasteContextLink: (String) -> Unit,
    onEditBeacon: (String) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit,
    onStartSelection: (String) -> Unit,
    onFocusProject: (String) -> Unit,
    onAddSubproject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onEditProject: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onMenuRequested: (String) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    // Focus is an operational-hierarchy concern. After Context retirement the
    // stable id may belong to a CANONICAL_ONLY WorkspaceNode rather than to a
    // live legacy Context, so resolve the already-built ProjectLike projection
    // before falling back to the legacy Context hierarchy.
    val focusedProjectNode =
        remember(orientationHierarchy, focusedProjectId) {
            orientationHierarchy
                .firstOrNull { item -> item.node.id == focusedProjectId }
                ?.node as? OrientationHierarchyNode.ProjectLike
        }
    val focusedPresentation =
        focusedProjectNode?.presentation
            ?: presentationHierarchy.allProjects.firstOrNull { it.id == focusedProjectId }
    val focusedProjectIsCanonicalWorkspace =
        focusedProjectNode?.isCanonicalWorkspace == true
    val focusedHasLegacyBacking = focusedProjectId in rawBackedProjectIds
    val canPasteIntoFocusedProject =
        focusedHasLegacyBacking &&
            !focusedProjectIsCanonicalWorkspace &&
            clipboardContextIds.isNotEmpty() &&
            focusedProjectId !in clipboardContextIds

    val children =
        remember(
            directChildrenByNodeId,
            focusedProjectId,
            presentationHierarchy,
            orientationHierarchy,
            rawBackedProjectIds,
        ) {
            val orientationChildren =
                directChildrenByNodeId[focusedProjectId].orEmpty()
                    .mapNotNull { item ->
                        (item.node as? OrientationHierarchyNode.ProjectLike)
                            ?.takeIf { it.id in rawBackedProjectIds }
                            ?.let { node ->
                                FlatHierarchyPresentationItem(
                                    project = node.presentation,
                                    level = 0,
                                    isLinkedAppearance = node.isLinkedAppearance,
                                    isCanonicalWorkspace = node.isCanonicalWorkspace,
                                )
                            }
                    }

            orientationChildren.ifEmpty {
                presentationHierarchy.childMap[focusedProjectId]
                    .orEmpty()
                    .filter { it.id in rawBackedProjectIds }
                    .map { child ->
                        val orientationNode =
                            orientationHierarchy
                                .firstOrNull { item -> item.node.id == child.id }
                                ?.node as? OrientationHierarchyNode.ProjectLike

                        FlatHierarchyPresentationItem(
                            project = orientationNode?.presentation ?: child,
                            level = 0,
                            isLinkedAppearance = orientationNode?.isLinkedAppearance == true,
                            isCanonicalWorkspace = orientationNode?.isCanonicalWorkspace == true,
                        )
                    }
            }.distinctBy { it.project.id }
        }

    fun legacyBackedChildCount(projectId: String): Int {
        val orientationCount =
            directChildrenByNodeId[projectId].orEmpty()
                .mapNotNull { child ->
                    (child.node as? OrientationHierarchyNode.ProjectLike)?.id
                }
                .distinct()
                .count { it in rawBackedProjectIds }

        return if (orientationCount > 0) {
            orientationCount
        } else {
            presentationHierarchy.childMap[projectId]
                .orEmpty()
                .count { it.id in rawBackedProjectIds }
        }
    }

    if (focusedHasLegacyBacking && focusedPresentation != null) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            stickyHeader(key = "focused-project-header") {
                Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                    BreadcrumbNavigation(
                        breadcrumbs = breadcrumbs,
                        onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                        onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                        onFocusedListMenuClick = { projectId ->
                            onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(projectId))
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusedProjectHeader(
                        project = focusedPresentation,
                        onMoreActionsClick = {
                            if (!focusedProjectIsCanonicalWorkspace) {
                                onMenuRequested(focusedPresentation.id)
                            }
                        },
                        onProjectClick = { onProjectClick(focusedPresentation.id) },
                    )
                }
            }

            if (children.isNotEmpty()) {
                itemsIndexed(children, key = { index, item -> "${item.project.id}-$index" }) { index, item ->
                    ReorderableContextRow(
                        item = item,
                        siblings = children,
                        childCount = legacyBackedChildCount(item.project.id),
                        index = index,
                        isSiblingReorderMode = isSiblingReorderMode,
                        parentContextId = focusedProjectId,
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
                                        onPasteContextLink(focusedPresentation.id)
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
    } else if (focusedPresentation != null) {
        FocusedPresentationProjectView(
            project = focusedPresentation,
            directChildren = directChildrenByNodeId[focusedProjectId].orEmpty(),
            breadcrumbs = breadcrumbs,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onEvent = onEvent,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Focused context not found.")
        }
    }
}

/**
 * Read-only focused view for a ProjectLike node without a real Context backing.
 * All mutation controls intentionally stay on the raw-Context focused path above.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusedPresentationProjectView(
    project: HierarchyContextPresentationNode,
    directChildren: List<OrientationHierarchyItem>,
    breadcrumbs: List<BreadcrumbItem>,
    isSearchActive: Boolean,
    searchQuery: String,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
) {
    val children = remember(directChildren) { focusedPresentationItems(directChildren) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader(key = "focused-presentation-project-header") {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                BreadcrumbNavigation(
                    breadcrumbs = breadcrumbs,
                    onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = {},
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                PresentationHierarchyRow(
                    item = FlatHierarchyPresentationItem(project = project, level = 0),
                    childCount = children.size,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isFocused = true,
                    isHighlighted = false,
                    onProjectClick = { projectId ->
                        onEvent(ContextHierarchyScreenEvent.FocusHierarchyProject(projectId))
                    },
                )
            }
        }
        if (children.isEmpty()) {
            item(key = "empty_read_only_state") { FocusedEmptyState(text = "No subcontexts") }
        } else {
            items(children, key = { it.project.id }) { child ->
                PresentationHierarchyRow(
                    item = child,
                    childCount = 0,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isFocused = false,
                    isHighlighted = false,
                    onProjectClick = { projectId ->
                        onEvent(ContextHierarchyScreenEvent.FocusHierarchyProject(projectId))
                    },
                )
            }
        }
    }
}

/** Pure read mapping; it intentionally has no Context reconstruction path. */
internal fun focusedPresentationItems(
    directChildren: List<OrientationHierarchyItem>,
): List<FlatHierarchyPresentationItem> =
    directChildren.mapNotNull { item ->
        (item.node as? OrientationHierarchyNode.ProjectLike)?.let { node ->
            FlatHierarchyPresentationItem(project = node.presentation, level = 0)
        }
    }.distinctBy { it.project.id }

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FocusedOrientationNodeView(
    focusedRoot: OrientationHierarchyItem?,
    directChildren: List<OrientationHierarchyItem>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
    orientationHierarchy: List<OrientationHierarchyItem>,
    rawBackedProjectIds: Set<String>,
    dragAndDropState: DragAndDropState<String>,
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
    onFocusProject: (String) -> Unit,
    onAddSubproject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onEditProject: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onMenuRequested: (String) -> Unit,
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
        remember(directChildren, rawBackedProjectIds) {
            directChildren.mapNotNull { item ->
                (item.node as? OrientationHierarchyNode.ProjectLike)
                    ?.takeIf { it.id in rawBackedProjectIds }
                    ?.let { node ->
                        FlatHierarchyPresentationItem(
                            project = node.presentation,
                            level = 0,
                            isLinkedAppearance = node.isLinkedAppearance,
                            isCanonicalWorkspace = node.isCanonicalWorkspace,
                        )
                    }
            }.distinctBy { it.project.id }
        }
    val presentationChildItems =
        remember(directChildren, rawBackedProjectIds) {
            directChildren.mapNotNull { item ->
                (item.node as? OrientationHierarchyNode.ProjectLike)
                    ?.takeIf { it.id !in rawBackedProjectIds }
                    ?.let { node ->
                        FlatHierarchyPresentationItem(
                            project = node.presentation,
                            level = 0,
                            isLinkedAppearance = node.isLinkedAppearance,
                            isCanonicalWorkspace = node.isCanonicalWorkspace,
                        )
                    }
            }.distinctBy { it.project.id }
        }
    val canReorderContextChildren =
        (
            focusedRoot.node is OrientationHierarchyNode.NoBeacon ||
                focusedRoot.node is OrientationHierarchyNode.Beacon
        ) &&
            childItems.size > 1 &&
            childItems.none { it.isCanonicalWorkspace }
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
                    is OrientationHierarchyNode.ProjectLike -> Unit
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
                    childCount =
                        directChildrenByNodeId[item.project.id]
                            .orEmpty()
                            .mapNotNull { child ->
                                (child.node as? OrientationHierarchyNode.ProjectLike)?.id
                            }
                            .distinct()
                            .count { it in rawBackedProjectIds },
                    index = index,
                    isSiblingReorderMode = isSiblingReorderMode && canReorderContextChildren,
                    parentContextId = parentContextId,
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
                    onProjectClick = { projectId ->
                        onEvent(ContextHierarchyScreenEvent.FocusHierarchyProject(projectId))
                    },
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
        if (presentationChildItems.isNotEmpty()) {
            items(presentationChildItems, key = { item -> "presentation-${item.project.id}" }) { item ->
                PresentationHierarchyRow(
                    item = item,
                    childCount = directChildrenByNodeId[item.project.id].orEmpty().size,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isFocused = false,
                    isHighlighted = item.project.id == highlightedProjectId,
                    onProjectClick = { projectId ->
                        onEvent(ContextHierarchyScreenEvent.FocusHierarchyProject(projectId))
                    },
                )
            }
        }
        if (childItems.isEmpty() && presentationChildItems.isEmpty() && beaconChildren.isEmpty()) {
            item(key = "empty_state") {
                FocusedEmptyState(text = "No contexts")
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReorderableContextRow(
    item: FlatHierarchyPresentationItem,
    siblings: List<FlatHierarchyPresentationItem>,
    childCount: Int,
    index: Int,
    isSiblingReorderMode: Boolean,
    parentContextId: String?,
    dragAndDropState: DragAndDropState<String>,
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
    onMenuRequested: (String) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    onFocusProject: (String) -> Unit,
    onAddSubproject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onEditProject: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HierarchyListItem(
            item = item,
            childCount = childCount,
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
            legacyContextActionsEnabled = !item.isCanonicalWorkspace,
            modifier = Modifier.weight(1f),
        )
        if (isSiblingReorderMode && !item.isCanonicalWorkspace) {
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
