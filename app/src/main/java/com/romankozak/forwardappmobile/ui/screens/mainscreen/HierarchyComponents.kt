// File: com/romankozak/forwardappmobile/ui/screens/mainscreen/HierarchyComponents.kt
// ПОВНА ВЕРСІЯ З УСІМА ЗМІНАМИ

package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData

// --- Допоміжні функції для підсвічування ---
private fun fuzzyMatchAndGetIndices(query: String, text: String): List<Int>? {
    if (query.isBlank()) return emptyList()
    if (text.isBlank()) return null
    val lowerQuery = query.lowercase()
    val lowerText = text.lowercase()
    val matchedIndices = mutableListOf<Int>()
    var queryIndex = 0
    var textIndex = 0
    while (queryIndex < lowerQuery.length && textIndex < lowerText.length) {
        if (lowerQuery[queryIndex] == lowerText[textIndex]) {
            matchedIndices.add(textIndex)
            queryIndex++
        }
        textIndex++
    }
    return if (queryIndex == lowerQuery.length) matchedIndices else null
}

@Composable
internal fun highlightFuzzy(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val matchedIndices = remember(query, text) { fuzzyMatchAndGetIndices(query, text) }
    if (matchedIndices == null) return AnnotatedString(text)

    return buildAnnotatedString {
        val indicesSet = matchedIndices.toSet()
        text.forEachIndexed { index, char ->
            if (index in indicesSet) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) { append(char) }
            } else {
                append(char)
            }
        }
    }
}

@Composable
internal fun highlightSubstring(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val startIdx = text.indexOf(query, ignoreCase = true)
    if (startIdx == -1) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text.substring(0, startIdx))
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            )
        ) {
            append(text.substring(startIdx, startIdx + query.length))
        }
        append(text.substring(startIdx + query.length))
    }
}

@Composable
fun ProjectRow(
    project: Project,
    level: Int,
    hasChildren: Boolean,
    onProjectClick: (String) -> Unit,
    onToggleExpanded: (project: Project) -> Unit,
    onMenuRequested: (project: Project) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    showFocusButton: Boolean,
    onFocusRequested: (project: Project) -> Unit,
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
    isFocused: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isFocused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 500),
        label = "Background Animation",
    )

    val indentation = (level * 24).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
    ) {
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = indentation)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProjectClick(project.id) }
                .alpha(if (isCurrentlyDragging) 0.6f else 1f)
                .padding(start = indentation)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (hasChildren && !showFocusButton) {
                    IconButton(
                        onClick = { onToggleExpanded(project) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (project.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Згорнути/Розгорнути",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }

            Text(
                text = displayName ?: AnnotatedString(project.name),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isFocused) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onBackground
            )

            AnimatedVisibility(
                visible = showFocusButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { onFocusRequested(project) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = if (isFocused) "Вийти з фокусу" else "Сфокусуватися",
                        tint = if (isFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { onMenuRequested(project) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Дії з проектом",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = indentation)
            )
        }
    }
}

@Composable
fun BreadcrumbNavigation(
    breadcrumbs: List<BreadcrumbItem>,
    onNavigate: (BreadcrumbItem) -> Unit,
    onClearNavigation: () -> Unit,
    onFocusedListMenuClick: (String) -> Unit,
    onOpenAsProject: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (breadcrumbs.isEmpty()) return

    val lazyRowState = rememberLazyListState()

    LaunchedEffect(breadcrumbs) {
        if (breadcrumbs.isNotEmpty()) {
            lazyRowState.animateScrollToItem(breadcrumbs.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        LazyRow(
            state = lazyRowState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Row(
                    Modifier.clickable(onClick = onClearNavigation),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(breadcrumbs) { index, item ->
                val isLast = index == breadcrumbs.size - 1

                Surface(
                    modifier = Modifier.clickable { if (!isLast) onNavigate(item) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLast) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isLast) {
                    Spacer(modifier = Modifier.width(4.dp))

                    if (onOpenAsProject != null) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            IconButton(
                                onClick = { onOpenAsProject(item.id) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Open as project",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = { onFocusedListMenuClick(item.id) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu for ${item.name}",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isLast) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(horizontal = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun SmartHierarchyView(
    project: Project,
    childMap: Map<String, List<Project>>,
    level: Int,
    dragAndDropState: DragAndDropState<Project>,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    onNavigateToProject: (String) -> Unit,
    focusedProjectId: String?,
    longDescendantsMap: Map<String, Boolean>,
    onProjectClick: (String) -> Unit,
    onToggleExpanded: (Project) -> Unit,
    onMenuRequested: (Project) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit
) {
    val children = childMap[project.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()

    val displayName = if (isSearchActive && searchQuery.isNotEmpty()) {
        if (searchQuery.length > 3) {
            highlightFuzzy(text = project.name, query = searchQuery)
        } else {
            highlightSubstring(text = project.name, query = searchQuery)
        }
    } else {
        AnnotatedString(project.name)
    }

    val hasLongDescendants = longDescendantsMap[project.id] ?: false
    val isDeeplyNested = hasChildren && level >= settings.useBreadcrumbsAfter
    val shouldShowFocusButton = hasLongDescendants || isDeeplyNested
    val isFocused = project.id == focusedProjectId

    Column {
        DraggableItem(
            state = dragAndDropState,
            key = project.id,
            data = project,
            dragAfterLongPress = true
        ) {
            val draggedItemData = dragAndDropState.draggedItem?.data
            val isDropAllowed = remember(draggedItemData, project) {
                draggedItemData == null || (draggedItemData.parentId == project.parentId)
            }

            val hoveredDropTargetKey = dragAndDropState.hoveredDropTargetKey
            val isHovered = remember(hoveredDropTargetKey, project.id) {
                isDropAllowed && (hoveredDropTargetKey == "before-${project.id}" || hoveredDropTargetKey == "after-${project.id}")
            }
            val isDraggingDown = remember(hoveredDropTargetKey, project.id) {
                isDropAllowed && hoveredDropTargetKey == "after-${project.id}"
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                ProjectRow(
                    project = project,
                    level = level,
                    hasChildren = hasChildren,
                    onProjectClick = onProjectClick,
                    onToggleExpanded = onToggleExpanded,
                    onMenuRequested = onMenuRequested,
                    isCurrentlyDragging = isDragging,
                    isHovered = isHovered,
                    isDraggingDown = isDraggingDown,
                    isHighlighted = project.id == highlightedProjectId,
                    displayName = displayName,
                    showFocusButton = shouldShowFocusButton,
                    onFocusRequested = { onNavigateToProject(it.id) },
                    isFocused = isFocused
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
                                    } else Modifier
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
                    SmartHierarchyView(
                        project = child,
                        childMap = childMap,
                        level = level + 1,
                        dragAndDropState = dragAndDropState,
                        isSearchActive = isSearchActive,
                        planningMode = planningMode,
                        highlightedProjectId = highlightedProjectId,
                        settings = settings,
                        searchQuery = searchQuery,
                        onNavigateToProject = onNavigateToProject,
                        focusedProjectId = focusedProjectId,
                        longDescendantsMap = longDescendantsMap,
                        onProjectClick = onProjectClick,
                        onToggleExpanded = onToggleExpanded,
                        onMenuRequested = onMenuRequested,
                        onProjectReorder = onProjectReorder
                    )
                }
            }
        }
    }
}

@Composable
fun FocusedListView(
    focusedProjectId: String,
    hierarchy: ListHierarchyData,
    dragAndDropState: DragAndDropState<Project>,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    longDescendantsMap: Map<String, Boolean>,
    onNavigateToProject: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onToggleExpanded: (Project) -> Unit,
    onMenuRequested: (Project) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit
) {
    val children = (hierarchy.childMap[focusedProjectId] ?: emptyList()).sortedBy { it.order }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    settings = settings,
                    searchQuery = searchQuery,
                    onNavigateToProject = onNavigateToProject,
                    focusedProjectId = focusedProjectId,
                    longDescendantsMap = longDescendantsMap,
                    onProjectClick = onProjectClick,
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
}