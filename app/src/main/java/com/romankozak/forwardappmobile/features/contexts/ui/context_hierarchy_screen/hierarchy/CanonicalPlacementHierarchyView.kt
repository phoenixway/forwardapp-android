package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureSourceDeleteTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.sourceDeleteTarget
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
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
    sourceDeleteTargets: Map<CanonicalEntityRef, BigPictureSourceDeleteTarget> = emptyMap(),
    onDeleteSource: (BigPictureSourceDeleteTarget) -> Unit = {},
    onOpenSemantic: (BigPictureNode) -> Unit = {},
) {
    Column(modifier.background(Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceContainerLowest,
        MaterialTheme.colorScheme.surface,
    )))) {
        BreadcrumbNavigation(
            breadcrumbs = state.navigationBreadcrumbs,
            onNavigate = { if (it.placementId == null) onFocusScope(it.id) else onFocus(it.placementId) },
            onClearNavigation = { onFocus(null) },
            onFocusedListMenuClick = {},
            showRootWhenEmpty = true,
            rootLabel = "Корінь",
            wrap = true,
        )
        if (state.activeNode != null || state.activeScope != null) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                val active = state.activeNode
                if (active != null) {
                    BigPictureUnitCard(
                        active, onClick = { onOpen(active) }, onOpen = { onOpen(active) },
                        mutationEnabled = mutationEnabled, onCut = onCut, onRemove = onRemove,
                        focused = true, childCount = state.entries.size,
                        onOpenSemantic = { onOpenSemantic(active) },
                        sourceDeleteTarget = sourceDeleteTarget(active, sourceDeleteTargets), onDeleteSource = onDeleteSource,
                    )
                } else state.activeScope?.let { section ->
                    BigPictureSectionCard(
                        section, state.structure?.sections?.get(section.id),
                        childCount = state.entries.size, focused = true,
                        onFocus = { onFocusScope(section.id) },
                        mutationEnabled = mutationEnabled, onCut = onCut, onRemove = onRemove,
                    )
                }
            }
            HorizontalDivider()
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
                            val ref = state.structure?.sections?.get(section.id)
                            BigPictureSectionCard(
                                section, ref,
                                childCount = ref?.let { state.structure.childCount(it) } ?: section.rootPlacementIds.size,
                                onFocus = { onFocusScope(section.id) },
                                mutationEnabled = mutationEnabled, onCut = onCut, onRemove = onRemove,
                            )
                        }
                        is BigPictureReadEntry.Occurrence -> {
                            val node = entry.node
                            BigPictureUnitCard(
                                node,
                                onClick = { onFocus(node.placement.id.value) },
                                onOpen = { onOpen(node) },
                                onOpenSemantic = { onOpenSemantic(node) },
                                mutationEnabled = mutationEnabled,
                                onCut = onCut,
                                onRemove = onRemove,
                                childCount = state.structure?.childCount(H1TypedNode.Occurrence(node.placement.id.value)) ?: node.childCount,
                                sourceDeleteTarget = sourceDeleteTarget(node, sourceDeleteTargets), onDeleteSource = onDeleteSource,
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
    focused: Boolean = false, childCount: Int = node.childCount,
    sourceDeleteTarget: BigPictureSourceDeleteTarget? = null,
    onDeleteSource: (BigPictureSourceDeleteTarget) -> Unit = {},
    onOpenSemantic: () -> Unit = {},
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
        subtitle = listOfNotNull(
            // Keep semantic class and operational cue before optional text: this row ellipsizes.
            if (node.operationalEmbodiment != null) "${entity?.ref?.entityClass?.displayLabel()} · ▣ є Workspace" else null,
            if (focused) "Поточна локація" else null,
            entity?.ref?.entityClass?.displayLabel()?.takeIf { node.operationalEmbodiment == null },
            node.placement.placementKind.name)
            .joinToString(" · "),
        symbol = when (entity?.ref?.entityClass) {
            CanonicalEntityClass.ORIENTATION -> "◎"
            CanonicalEntityClass.ASPECT -> "◇"
            CanonicalEntityClass.WORKSPACE -> "▣"
            null -> "?"
        },
        tint = tint,
        level = 0,
        childCount = childCount,
        onClick = onClick,
        enabled = entity != null,
        isFocused = focused,
        actionMenu = {
            BigPictureNodeMenu(
                H1TypedNode.Occurrence(node.placement.id.value), mutationEnabled, onCut, onRemove,
                onOpen = onOpen.takeIf { entity != null },
                openLabel = if (entity?.ref?.entityClass == CanonicalEntityClass.WORKSPACE || node.operationalEmbodiment != null)
                    "Відкрити робочий простір" else "Відкрити редактор",
                onOpenSemantic = onOpenSemantic.takeIf { node.operationalEmbodiment != null },
                semanticOpenLabel = if (entity?.ref is CanonicalEntityRef.Aspect) "Відкрити аспект" else "Відкрити орієнтир",
                sourceDeleteTarget = sourceDeleteTarget,
                onDeleteSource = onDeleteSource,
            )
        },
    )
}

@Composable
private fun BigPictureSectionCard(
    section: BigPictureContainer, ref: H1TypedNode?, childCount: Int,
    onFocus: () -> Unit, mutationEnabled: Boolean,
    onCut: (H1TypedNode) -> Unit, onRemove: (H1TypedNode) -> Unit,
    focused: Boolean = false,
) {
    RootHeaderRow(
        title = section.title,
        subtitle = if (focused) "Поточна локація · Секція" else "Секція",
        symbol = "▦", tint = MaterialTheme.colorScheme.tertiary,
        level = 0, childCount = childCount, isFocused = focused,
        onClick = onFocus,
        actionMenu = {
            if (mutationEnabled && ref != null) BigPictureNodeMenu(ref, true, onCut, onRemove)
        },
    )
}
