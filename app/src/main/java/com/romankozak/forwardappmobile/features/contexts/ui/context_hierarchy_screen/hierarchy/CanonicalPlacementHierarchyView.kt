package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.BigPictureNodeMenu

/** Existing focus-mode hierarchy: breadcrumbs, active unit, direct children. */
@Composable
fun ProjectHierarchyView(
    state: BigPictureUiState.Ready,
    listState: LazyListState,
    onFocus: (String?) -> Unit,
    onFocusScope: (String?) -> Unit,
    onOpen: (BigPictureNode) -> Unit,
    modifier: Modifier = Modifier,
    mutationEnabled: Boolean = false,
    onCut: (H1TypedNode) -> Unit = {},
    onRemove: (H1TypedNode) -> Unit = {},
) {
    Column(modifier.background(Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceContainerLowest,
        MaterialTheme.colorScheme.surface,
    )))) {
        val occurrenceCrumbs = state.breadcrumbs.map { node ->
            BreadcrumbItem(
                id = node.placement.id.value,
                name = node.entity?.title ?: "Сутність недоступна",
                level = node.depth,
                target = BreadcrumbTarget.OrientationNode,
                placementId = node.placement.id.value,
            )
        }
        val sectionCrumbs = state.scopeBreadcrumbs.mapIndexed { index, scope ->
            BreadcrumbItem(
                id = scope.id,
                name = scope.title,
                level = index,
                target = BreadcrumbTarget.OrientationNode,
            )
        }
        // Place presentation sections after their H1 anchor when that
        // ancestor is part of the currently focused occurrence path.
        val anchorIndex = state.scopeAnchorPlacementId?.let { anchor ->
            occurrenceCrumbs.indexOfFirst { it.placementId == anchor }
        } ?: -1
        val navigationCrumbs = if (anchorIndex >= 0) {
            occurrenceCrumbs.take(anchorIndex + 1) +
                sectionCrumbs +
                occurrenceCrumbs.drop(anchorIndex + 1)
        } else {
            sectionCrumbs + occurrenceCrumbs
        }

        BreadcrumbNavigation(
            breadcrumbs = navigationCrumbs,
            onNavigate = { if (it.placementId == null) onFocusScope(it.id) else onFocus(it.placementId) },
            onClearNavigation = { onFocus(null) },
            onFocusedListMenuClick = {},
        )
        state.activeNode?.let { active ->
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                BigPictureUnitCard(active, onClick = { onOpen(active) }, onOpen = { onOpen(active) },
                    mutationEnabled = mutationEnabled, onCut = onCut, onRemove = onRemove)
            }
            HorizontalDivider()
        }
        if (state.activeScope != null && state.activeNode == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.activeScope.title, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(16.dp))
                if (mutationEnabled) state.structure?.sections?.get(state.activeScope.id)?.let { ref ->
                    BigPictureNodeMenu(ref, mutationEnabled, onCut, onRemove)
                }
            }
        }
        val atRoot = state.activeNode == null && state.activeScope == null
        if (state.entries.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (atRoot) "У Big Picture ще немає розміщених сутностей" else "Немає потомків",
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.entries, key = { it.stableId }) { entry ->
                    when (entry) {
                        is BigPictureReadEntry.Container -> {
                            val section = entry.section
                            RootHeaderRow(
                                title = section.title,
                                subtitle = if (
                                    section.origin == BigPictureContainerOrigin.PRESENTATION
                                ) "Секція" else "Група представлення",
                                symbol = "▦",
                                tint = MaterialTheme.colorScheme.tertiary,
                                level = 0,
                                childCount = section.rootPlacementIds.size,
                                onClick = { onFocusScope(section.id) },
                                actionMenu = {
                                    if (mutationEnabled) state.structure?.sections?.get(section.id)?.let { ref ->
                                        BigPictureNodeMenu(ref, mutationEnabled, onCut, onRemove)
                                    }
                                },
                            )
                        }
                        is BigPictureReadEntry.Occurrence -> {
                            val node = entry.node
                            BigPictureUnitCard(
                                node,
                                onClick = { onFocus(node.placement.id.value) },
                                onOpen = { onOpen(node) },
                                mutationEnabled = mutationEnabled,
                                onCut = onCut,
                                onRemove = onRemove,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigPictureUnitCard(
    node: BigPictureNode, onClick: () -> Unit, onOpen: () -> Unit,
    mutationEnabled: Boolean, onCut: (H1TypedNode) -> Unit, onRemove: (H1TypedNode) -> Unit,
) {
    val entity = node.entity
    val tint = when (entity?.ref?.entityClass) {
        CanonicalEntityClass.ORIENTATION -> MaterialTheme.colorScheme.primary
        CanonicalEntityClass.ASPECT -> MaterialTheme.colorScheme.tertiary
        CanonicalEntityClass.WORKSPACE -> MaterialTheme.colorScheme.secondary
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    RootHeaderRow(
        title = entity?.title ?: "Сутність недоступна",
        subtitle = listOfNotNull(entity?.ref?.entityClass?.displayLabel(), node.placement.placementKind.name)
            .joinToString(" · "),
        symbol = when (entity?.ref?.entityClass) {
            CanonicalEntityClass.ORIENTATION -> "◎"
            CanonicalEntityClass.ASPECT -> "◇"
            CanonicalEntityClass.WORKSPACE -> "▣"
            null -> "?"
        },
        tint = tint,
        level = 0,
        childCount = node.childCount,
        onClick = onClick,
        enabled = entity != null,
        actionMenu = {
            if (mutationEnabled) BigPictureNodeMenu(H1TypedNode.Occurrence(node.placement.id.value), true, onCut, onRemove)
            IconButton(onClick = onOpen, enabled = entity != null) {
                Icon(Icons.Outlined.ArrowOutward, contentDescription = "Відкрити сутність")
            }
        },
    )
}
