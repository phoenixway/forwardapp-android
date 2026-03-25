package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.shouldShowHierarchyFocusButton

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LegacySmartHierarchyView(
    project: Context,
    childMap: Map<String, List<Context>>,
    level: Int,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    onNavigateToProject: (String) -> Unit,
    focusedProjectId: String?,
    longDescendantsMap: Map<String, Boolean>,
    onProjectClick: (String) -> Unit,
    onToggleExpanded: (Context) -> Unit,
    onMenuRequested: (Context) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val children = childMap[project.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()

    val displayName =
        if (isSearchActive && searchQuery.isNotEmpty()) {
            if (searchQuery.length > 3) {
                highlightFuzzy(text = project.name, query = searchQuery)
            } else {
                highlightSubstring(text = project.name, query = searchQuery)
            }
        } else {
            AnnotatedString(project.name)
        }

    val hasLongDescendants = longDescendantsMap[project.id] ?: false
    val shouldShowFocusButton =
        shouldShowHierarchyFocusButton(
            hasChildren = hasChildren,
            level = level,
            hasOverflowingDescendants = hasLongDescendants,
        )
    val isFocused = project.id == focusedProjectId

    Column {
        DraggableItem(
            state = dragAndDropState,
            key = project.id,
            data = project,
            dragAfterLongPress = false,
        ) {
            val draggedItemData = dragAndDropState.draggedItem?.data
            val isDropAllowed =
                remember(draggedItemData, project) {
                    draggedItemData == null || (draggedItemData.parentId == project.parentId)
                }

            val hoveredDropTargetKey = dragAndDropState.hoveredDropTargetKey
            val isHovered =
                remember(hoveredDropTargetKey, project.id) {
                    isDropAllowed && (hoveredDropTargetKey == "before-${project.id}" || hoveredDropTargetKey == "after-${project.id}")
                }
            val isDraggingDown =
                remember(hoveredDropTargetKey, project.id) {
                    isDropAllowed && hoveredDropTargetKey == "after-${project.id}"
                }

            Box(modifier = Modifier.fillMaxWidth()) {
                ProjectRow(
                    project = project,
                    level = level,
                    hasChildren = hasChildren,
                    childCount = children.size,
                    onProjectClick = onProjectClick,
                    onProjectFocus = { onNavigateToProject(it.id) },
                    isCurrentlyDragging = isDragging,
                    isHovered = isHovered,
                    isDraggingDown = isDraggingDown,
                    isHighlighted = project.id == highlightedProjectId,
                    displayName = displayName,
                    isFocused = isFocused,
                )

                if (!isDragging) {
                    Column(modifier = Modifier.matchParentSize()) {
                        val dropModifier = { position: DropPosition ->
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .then(
                                    if (isDropAllowed) {
                                        Modifier.dropTarget(state = dragAndDropState, key = "$position-${project.id}") {
                                            onProjectReorder(it.data.id, project.id, position)
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                        }
                        Box(modifier = dropModifier(DropPosition.BEFORE))
                        Box(modifier = dropModifier(DropPosition.AFTER))
                    }
                }
            }
        }
        if (project.isExpanded && !shouldShowFocusButton) {
            Column(modifier = Modifier.padding(start = 24.dp)) {
                children.forEach { child ->
                    LegacySmartHierarchyView(
                        project = child,
                        childMap = childMap,
                        level = level + 1,
                        dragAndDropState = dragAndDropState,
                        isSearchActive = isSearchActive,
                        highlightedProjectId = highlightedProjectId,
                        settings = settings,
                        searchQuery = searchQuery,
                        onNavigateToProject = onNavigateToProject,
                        focusedProjectId = focusedProjectId,
                        longDescendantsMap = longDescendantsMap,
                        onProjectClick = onProjectClick,
                        onToggleExpanded = onToggleExpanded,
                        onMenuRequested = onMenuRequested,
                        onProjectReorder = onProjectReorder,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            }
        }
    }
}
