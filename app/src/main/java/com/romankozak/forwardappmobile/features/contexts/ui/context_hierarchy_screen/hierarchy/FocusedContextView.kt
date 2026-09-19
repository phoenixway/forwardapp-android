package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyPresentationItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbs

/** Renders the focused operational owner exclusively from ProjectLike presentation. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusedProjectView(
    focusedProjectId: String,
    presentationHierarchy: HierarchyPresentationData,
    orientationHierarchy: List<OrientationHierarchyItem>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
    breadcrumbs: List<BreadcrumbItem>,
    isSearchActive: Boolean,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
) {
    val focusedPresentation =
        remember(orientationHierarchy, presentationHierarchy, focusedProjectId) {
            (orientationHierarchy
                .firstOrNull { it.node.id == focusedProjectId }
                ?.node as? OrientationHierarchyNode.ProjectLike)
                ?.presentation
                ?: presentationHierarchy.allProjects.firstOrNull { it.id == focusedProjectId }
        }

    if (focusedPresentation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Focused workspace not found.")
        }
        return
    }

    FocusedPresentationProjectView(
        project = focusedPresentation,
        children =
            focusedProjectLikeItems(
                focusedProjectId = focusedProjectId,
                directChildren = directChildrenByNodeId[focusedProjectId].orEmpty(),
                presentationHierarchy = presentationHierarchy,
            ),
        breadcrumbs = breadcrumbs,
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        isSelectionMode = isSelectionMode,
        selectedContextIds = selectedContextIds,
        onEvent = onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusedPresentationProjectView(
    project: HierarchyContextPresentationNode,
    children: List<FlatHierarchyPresentationItem>,
    breadcrumbs: List<BreadcrumbItem>,
    isSearchActive: Boolean,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader(key = "focused-presentation-project-header") {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                BreadcrumbNavigation(
                    breadcrumbs = breadcrumbs,
                    onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                PresentationHierarchyRow(
                    item = FlatHierarchyPresentationItem(project = project, level = 0),
                    childCount = children.size,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isFocused = true,
                    isHighlighted = false,
                    onProjectClick = {
                        onEvent(focusedPresentationClickEvent(it, isActiveHeader = true))
                    },
                    onMenuRequested = { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) },
                    isSelectionMode = isSelectionMode,
                    isSelected = project.id in selectedContextIds,
                    onToggleSelection = { onEvent(ContextHierarchyScreenEvent.ToggleContextSelection(it)) },
                    onStartSelection = { onEvent(ContextHierarchyScreenEvent.StartContextSelection(it)) },
                )
            }
        }
        if (children.isEmpty()) {
            item(key = "empty_presentation_state") { FocusedEmptyState("No workspaces") }
        } else {
            items(children, key = { it.project.id }) { child ->
                PresentationHierarchyRow(
                    item = child,
                    childCount = 0,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isFocused = false,
                    isHighlighted = false,
                    onProjectClick = {
                        onEvent(focusedPresentationClickEvent(it, isActiveHeader = false))
                    },
                    onMenuRequested = { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) },
                    isSelectionMode = isSelectionMode,
                    isSelected = child.project.id in selectedContextIds,
                    onToggleSelection = { onEvent(ContextHierarchyScreenEvent.ToggleContextSelection(it)) },
                    onStartSelection = { onEvent(ContextHierarchyScreenEvent.StartContextSelection(it)) },
                )
            }
        }
    }
}

internal fun focusedPresentationItems(
    directChildren: List<OrientationHierarchyItem>,
): List<FlatHierarchyPresentationItem> =
    directChildren.mapNotNull { item ->
        (item.node as? OrientationHierarchyNode.ProjectLike)?.let { node ->
            FlatHierarchyPresentationItem(
                project = node.presentation,
                level = 0,
                isLinkedAppearance = node.isLinkedAppearance,
                isCanonicalWorkspace = node.isCanonicalWorkspace,
            )
        }
    }.distinctBy { it.project.id }

internal fun focusedProjectLikeItems(
    focusedProjectId: String,
    directChildren: List<OrientationHierarchyItem>,
    presentationHierarchy: HierarchyPresentationData,
): List<FlatHierarchyPresentationItem> =
    focusedPresentationItems(directChildren)
        .ifEmpty {
            presentationHierarchy.childMap[focusedProjectId]
                .orEmpty()
                .map { FlatHierarchyPresentationItem(project = it, level = 0) }
        }
        .distinctBy { it.project.id }

internal fun focusedPresentationClickEvent(
    projectId: String,
    isActiveHeader: Boolean,
): ContextHierarchyScreenEvent =
    if (isActiveHeader) {
        ContextHierarchyScreenEvent.ContextClick(projectId)
    } else {
        ContextHierarchyScreenEvent.FocusHierarchyProject(projectId)
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusedOrientationNodeView(
    focusedRoot: OrientationHierarchyItem?,
    directChildren: List<OrientationHierarchyItem>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
    orientationHierarchy: List<OrientationHierarchyItem>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    onEditBeacon: (String) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
) {
    if (focusedRoot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Focused root not found.")
        }
        return
    }
    val breadcrumbs =
        remember(orientationHierarchy, focusedRoot) {
            buildOrientationBreadcrumbs(orientationHierarchy, focusedRoot.node.id)
        }
    val projectChildren = remember(directChildren) { focusedPresentationItems(directChildren) }
    val beaconChildren = directChildren.filter { it.node is OrientationHierarchyNode.Beacon }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader(key = "focused-orientation-header") {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                BreadcrumbNavigation(
                    breadcrumbs = breadcrumbs,
                    onNavigate = { onEvent(ContextHierarchyScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(ContextHierarchyScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = {},
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                when (val node = focusedRoot.node) {
                    is OrientationHierarchyNode.Group -> BeaconGroupRootHeaderRow(
                        node = node, level = 0, childCount = directChildren.size,
                        onPasteBeacon = { onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(node.id)) },
                    )
                    is OrientationHierarchyNode.Beacon -> BeaconRootHeaderRow(
                        node = node, level = 0, childCount = directChildren.size,
                        onEditBeacon = { onEditBeacon(node.id) },
                        onDeleteBeacon = { onDeleteBeacon(node.id) },
                        onCopyBeacon = { onEvent(ContextHierarchyScreenEvent.CopyBeacon(node.id)) },
                        onCopyBeaconAsLink = { onEvent(ContextHierarchyScreenEvent.CopyBeaconAsLink(node.id)) },
                        onCutBeacon = { onEvent(ContextHierarchyScreenEvent.CutBeacon(node.id)) },
                        onPasteBeacon = { onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoBeacon(node.id)) },
                    )
                    OrientationHierarchyNode.NoGroup -> NoGroupRootHeaderRow(
                        level = 0, childCount = directChildren.size,
                        onPasteBeacon = { onEvent(ContextHierarchyScreenEvent.PasteBeaconIntoGroup(null)) },
                    )
                    OrientationHierarchyNode.NoBeacon -> NoBeaconRootHeaderRow(0, directChildren.size)
                    is OrientationHierarchyNode.ProjectLike -> Unit
                }
            }
        }
        itemsIndexed(beaconChildren, key = { index, item -> "beacon-${item.node.id}-$index" }) { _, item ->
            val node = item.node as OrientationHierarchyNode.Beacon
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
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        items(projectChildren, key = { "project-${it.project.id}" }) { item ->
            PresentationHierarchyRow(
                item = item,
                childCount = directChildrenByNodeId[item.project.id].orEmpty().size,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                isFocused = false,
                isHighlighted = item.project.id == highlightedProjectId,
                onProjectClick = {
                    onEvent(focusedPresentationClickEvent(it, isActiveHeader = false))
                },
                onMenuRequested = { onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it)) },
                isSelectionMode = isSelectionMode,
                isSelected = item.project.id in selectedContextIds,
                onToggleSelection = { onEvent(ContextHierarchyScreenEvent.ToggleContextSelection(it)) },
                onStartSelection = { onEvent(ContextHierarchyScreenEvent.StartContextSelection(it)) },
            )
        }
        if (beaconChildren.isEmpty() && projectChildren.isEmpty()) {
            item(key = "empty_state") { FocusedEmptyState("No workspaces") }
        }
    }
}

@Composable
private fun FocusedEmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.bodyLarge) }
}
