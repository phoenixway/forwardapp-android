package com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Remove SharedTransition imports for now - they seem to be causing issues
// import androidx.compose.animation.ExperimentalSharedTransitionApi
// import androidx.compose.animation.SharedTransitionScope
// import androidx.compose.animation.rememberSharedContentState
// import androidx.compose.runtime.staticCompositionLocalOf
// import androidx.compose.animation.LocalSharedTransitionScope
// import androidx.compose.animation.LocalAnimatedVisibilityScope


private fun fuzzyMatchAndGetIndices(
    query: String,
    text: String,
): List<Int>? {
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
internal fun highlightFuzzy(
    text: String,
    query: String,
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val matchedIndices = remember(query, text) { fuzzyMatchAndGetIndices(query, text) }
    if (matchedIndices == null) return AnnotatedString(text)

    return buildAnnotatedString {
        val indicesSet = matchedIndices.toSet()
        text.forEachIndexed { index, char ->
            if (index in indicesSet) {
                withStyle(
                    style =
                        SpanStyle(
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
internal fun highlightSubstring(
    text: String,
    query: String,
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val startIdx = text.indexOf(query, ignoreCase = true)
    if (startIdx == -1) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text.substring(0, startIdx))
        withStyle(
            style =
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
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
    isFocused: Boolean = false,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                isFocused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else -> Color.Transparent
            },
        animationSpec = tween(durationMillis = 500),
        label = "Background Animation",
    )

    val indentation = (level * 24).dp

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor),
    ) {
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = indentation),
            )
        }

        Row(
            modifier =
                Modifier
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
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (project.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Згорнути/Розгорнути",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }

            Text(
                text = displayName ?: AnnotatedString(project.name),
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                    ),
                color =
                    if (isFocused) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
            )

            AnimatedVisibility(
                visible = showFocusButton,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    onClick = { onFocusRequested(project) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = if (isFocused) "Вийти з фокусу" else "Сфокусуватися",
                        tint =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }

            IconButton(
                onClick = { onMenuRequested(project) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Дії з проектом",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = indentation),
            )
        }
    }
}

@Composable
fun SwipeableProjectRow(
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
    onAddSubproject: (project: Project) -> Unit,
    onDelete: (project: Project) -> Unit,
    onEdit: (project: Project) -> Unit,
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
    isFocused: Boolean = false,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val startActionWidth = 120.dp
    val endActionWidth = 120.dp
    val startActionWidthPx = with(density) { startActionWidth.toPx() }
    val endActionWidthPx = with(density) { endActionWidth.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) }

    val startProgress by remember {
        derivedStateOf { (offsetX / startActionWidthPx).coerceIn(0f, 1f) }
    }
    val endProgress by remember {
        derivedStateOf { (-offsetX / endActionWidthPx).coerceIn(0f, 1f) }
    }

    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-endActionWidthPx, startActionWidthPx)
    }

    fun animateTo(target: Float) {
        coroutineScope.launch {
            animate(initialValue = offsetX, targetValue = target) { value, _ ->
                offsetX = value
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                    onDragStopped = { velocity ->
                        val velocityThreshold = 300f
                        val startThreshold = startActionWidthPx * 0.12f
                        val endThreshold = endActionWidthPx * 0.12f
                        when {
                            offsetX > 0f && velocity < -velocityThreshold -> animateTo(0f)
                            offsetX < 0f && velocity > velocityThreshold -> animateTo(0f)
                            offsetX >= 0f && velocity > velocityThreshold -> animateTo(startActionWidthPx)
                            offsetX <= 0f && velocity < -velocityThreshold -> animateTo(-endActionWidthPx)
                            offsetX > startThreshold -> animateTo(startActionWidthPx)
                            offsetX < -endThreshold -> animateTo(-endActionWidthPx)
                            else -> animateTo(0f)
                        }
                    },
                ),
    ) {
        fun resetSwipe() = animateTo(0f)

        if (startProgress > 0.02f) {
            val startBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            Surface(
                modifier =
                    Modifier
                        .width(startActionWidth)
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp, end = 6.dp)
                        .alpha(startProgress),
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                color = startBg,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .alpha(startProgress),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProjectSwipeActionButton(
                        icon = Icons.Default.FilterCenterFocus,
                        contentDescription = "Фокус",
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        onFocusRequested(project)
                        resetSwipe()
                    }
                    ProjectSwipeActionButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Додати підпроєкт",
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        onAddSubproject(project)
                        resetSwipe()
                    }
                }
            }
        }

        if (endProgress > 0.02f) {
            val endBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            Surface(
                modifier =
                    Modifier
                        .width(endActionWidth)
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp, start = 6.dp)
                        .alpha(endProgress),
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                color = endBg,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .alpha(endProgress),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProjectSwipeActionButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Видалити проєкт",
                        color = MaterialTheme.colorScheme.error,
                    ) {
                        onDelete(project)
                        resetSwipe()
                    }
                    ProjectSwipeActionButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Редагувати проєкт",
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        onEdit(project)
                        resetSwipe()
                    }
                }
            }
        }

        ProjectRow(
            project = project,
            level = level,
            hasChildren = hasChildren,
            onProjectClick = onProjectClick,
            onToggleExpanded = onToggleExpanded,
            onMenuRequested = onMenuRequested,
            isCurrentlyDragging = isCurrentlyDragging,
            isHovered = isHovered,
            isDraggingDown = isDraggingDown,
            isHighlighted = isHighlighted,
            showFocusButton = showFocusButton,
            onFocusRequested = onFocusRequested,
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) },
            displayName = displayName,
            isFocused = isFocused,
        )
    }
}

@Composable
private fun ProjectSwipeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
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
    modifier: Modifier = Modifier,
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
        tonalElevation = 1.dp,
    ) {
        LazyRow(
            state = lazyRowState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                Row(
                    Modifier.clickable(onClick = onClearNavigation),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            itemsIndexed(breadcrumbs) { index, item ->
                val isLast = index == breadcrumbs.size - 1

                Surface(
                    modifier = Modifier.clickable { if (!isLast) onNavigate(item) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLast) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp,
                            ),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!isLast) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(horizontal = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
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
    focusedProjectId: String?,
    longDescendantsMap: Map<String, Boolean>,
    onProjectClick: (String) -> Unit,
    onToggleExpanded: (Project) -> Unit,
    onMenuRequested: (Project) -> Unit,
    onProjectReorder: (fromId: String, toId: String, position: DropPosition) -> Unit,
    onFocusProject: (Project) -> Unit,
    onAddSubproject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onEditProject: (Project) -> Unit,
    // Add the animation scopes to the signature
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
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
    val isDeeplyNested = hasChildren && level >= 3
    val shouldShowFocusButton = hasLongDescendants || isDeeplyNested
    val isFocused = project.id == focusedProjectId

    with(sharedTransitionScope) {
        Column {
            DraggableItem(
                state = dragAndDropState,
                key = project.id,
                data = project,
                dragAfterLongPress = true,
            ) {
                val draggedItemData = dragAndDropState.draggedItem?.data
                val isDropAllowed =
                    remember(draggedItemData, project) {
                        draggedItemData == null || draggedItemData.id != project.id
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
                    SwipeableProjectRow(
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
                        onFocusRequested = onFocusProject,
                        onAddSubproject = onAddSubproject,
                        onDelete = onDeleteProject,
                        onEdit = onEditProject,
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
                            focusedProjectId = focusedProjectId,
                            longDescendantsMap = longDescendantsMap,
                            onProjectClick = onProjectClick,
                            onToggleExpanded = onToggleExpanded,
                            onMenuRequested = onMenuRequested,
                            onProjectReorder = onProjectReorder,
                            onFocusProject = onFocusProject,
                            onAddSubproject = onAddSubproject,
                            onDeleteProject = onDeleteProject,
                            onEditProject = onEditProject,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }
    }
}
