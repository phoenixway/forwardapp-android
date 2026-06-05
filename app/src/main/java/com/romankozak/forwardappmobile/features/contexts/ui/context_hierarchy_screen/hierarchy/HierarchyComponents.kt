package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val HIERARCHY_LEVEL_INDENT_DP = 18
private const val DRAGGING_PROJECT_ALPHA = 0.6f
private const val HIERARCHY_MIN_CARD_ALPHA = 0.64f
private const val HIERARCHY_MAX_CARD_ALPHA = 0.9f
private const val SWIPE_ACTION_VISIBILITY_THRESHOLD = 0.02f
private const val SHORT_QUERY_THRESHOLD = 3
private const val ROOT_LEVEL_INDENT_DP = 18

fun fuzzyMatchAndGetIndices(
    query: String,
    text: String,
): List<Int>? {
    return when {
        query.isBlank() -> emptyList()
        text.isBlank() -> null
        else -> {
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
            matchedIndices.takeIf { queryIndex == lowerQuery.length }
        }
    }
}

@Composable
fun highlightFuzzy(
    text: String,
    query: String,
): AnnotatedString {
    val matchedIndices = remember(query, text) { fuzzyMatchAndGetIndices(query, text) }
    return if (query.isBlank() || matchedIndices == null) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
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
}

@Composable
fun highlightSubstring(
    text: String,
    query: String,
): AnnotatedString {
    val startIdx = text.indexOf(query, ignoreCase = true)
    return if (query.isBlank() || startIdx == -1) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
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
}

@Composable
fun BeaconRootHeaderRow(
    node: OrientationHierarchyNode.Beacon,
    level: Int,
    childCount: Int = 0,
    onClick: () -> Unit = {},
    onCopyBeacon: (() -> Unit)? = null,
    onCutBeacon: (() -> Unit)? = null,
    onPasteBeacon: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    RootHeaderRow(
        title = node.title,
        subtitle = "${node.relatedContextCount} contexts",
        symbol = "MB",
        tint = readinessTint(node.readinessStatus),
        level = level,
        childCount = childCount,
        showChildCountBadge = false,
        onClick = onClick,
        actionMenu = {
            BeaconHeaderActionMenu(
                onCopyBeacon = onCopyBeacon,
                onCutBeacon = onCutBeacon,
                onPasteBeacon = onPasteBeacon,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun BeaconGroupRootHeaderRow(
    node: OrientationHierarchyNode.Group,
    level: Int,
    childCount: Int = 0,
    onClick: () -> Unit = {},
    onPasteBeacon: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    RootHeaderRow(
        title = node.title,
        subtitle = "${node.beaconCount} beacons",
        symbol = "GR",
        tint = MaterialTheme.colorScheme.primary,
        level = level,
        childCount = childCount,
        onClick = onClick,
        actionMenu = {
            PasteBeaconHeaderActionMenu(onPasteBeacon = onPasteBeacon)
        },
        modifier = modifier,
    )
}

@Composable
fun NoGroupRootHeaderRow(
    level: Int,
    childCount: Int = 0,
    onClick: () -> Unit = {},
    onPasteBeacon: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    RootHeaderRow(
        title = "No group",
        subtitle = "Main beacons without a group",
        symbol = "NG",
        tint = MaterialTheme.colorScheme.secondary,
        level = level,
        childCount = childCount,
        onClick = onClick,
        actionMenu = {
            PasteBeaconHeaderActionMenu(onPasteBeacon = onPasteBeacon)
        },
        modifier = modifier,
    )
}

@Composable
fun NoBeaconRootHeaderRow(
    level: Int,
    childCount: Int = 0,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    RootHeaderRow(
        title = "No beacon",
        subtitle = "Root contexts without a main beacon",
        symbol = "NB",
        tint = MaterialTheme.colorScheme.outline,
        level = level,
        childCount = childCount,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun RootHeaderRow(
    title: String,
    subtitle: String,
    symbol: String,
    tint: Color,
    level: Int,
    childCount: Int,
    showChildCountBadge: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionMenu: (@Composable () -> Unit)? = null,
) {
    val indentation = (level * ROOT_LEVEL_INDENT_DP).dp
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = indentation, top = 4.dp, bottom = 2.dp),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        border = BorderStroke(width = 1.dp, color = tint.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = tint.copy(alpha = 0.18f),
                contentColor = tint,
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            if (showChildCountBadge && childCount > 0) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    tonalElevation = 0.dp,
                ) {
                    Text(
                        text = childCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actionMenu?.invoke()
        }
    }
}

@Composable
private fun BeaconHeaderActionMenu(
    onCopyBeacon: (() -> Unit)?,
    onCutBeacon: (() -> Unit)?,
    onPasteBeacon: (() -> Unit)?,
) {
    HeaderActionMenu(
        items =
            listOfNotNull(
                onCopyBeacon?.let { "Copy" to it },
                onCutBeacon?.let { "Cut" to it },
                onPasteBeacon?.let { "Paste here" to it },
            ),
    )
}

@Composable
private fun PasteBeaconHeaderActionMenu(onPasteBeacon: (() -> Unit)?) {
    HeaderActionMenu(
        items = listOfNotNull(onPasteBeacon?.let { "Paste beacon" to it }),
    )
}

@Composable
private fun HeaderActionMenu(items: List<Pair<String, () -> Unit>>) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Beacon actions")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun readinessTint(status: MainBeaconReadinessStatus): Color =
    when (status) {
        MainBeaconReadinessStatus.READY -> Color(0xFF2E7D32)
        MainBeaconReadinessStatus.CONDITIONAL -> Color(0xFFB26A00)
        MainBeaconReadinessStatus.BLOCKED -> Color(0xFFC62828)
        MainBeaconReadinessStatus.DEFECTED -> Color(0xFF5F6368)
    }

@Composable
fun ProjectRow(
    project: Context,
    level: Int,
    hasChildren: Boolean,
    childCount: Int,
    isLinkedAppearance: Boolean = false,
    onProjectClick: (String) -> Unit,
    onProjectFocus: (Context) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    dragHandle: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
    isFocused: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: ((String) -> Unit)? = null,
    onStartSelection: ((String) -> Unit)? = null,
) {
    val highlightColor by animateColorAsState(
        targetValue =
            when {
                isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
                isFocused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                else -> Color.Transparent
            },
        animationSpec = tween(durationMillis = 350),
        label = "Background Animation",
    )

    val baseCardColor =
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(
            alpha = (0.86f - (level * 0.06f)).coerceIn(HIERARCHY_MIN_CARD_ALPHA, HIERARCHY_MAX_CARD_ALPHA),
        )
    val containerColor =
        if (highlightColor == Color.Transparent) {
            baseCardColor
        } else {
            highlightColor
        }
    val borderColor =
        when {
            isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
            isFocused -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
        }

    val indentation = (level * HIERARCHY_LEVEL_INDENT_DP).dp
    val rowStartPadding = 14.dp

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = indentation + rowStartPadding)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .border(width = 0.8.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
    ) {
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(if (isCurrentlyDragging) DRAGGING_PROJECT_ALPHA else 1f)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection?.invoke(project.id)
                                } else {
                                    onProjectFocus(project)
                                }
                            },
                            onLongClick = {
                                if (isSelectionMode) {
                                    onToggleSelection?.invoke(project.id)
                                } else {
                                    onStartSelection?.invoke(project.id)
                                }
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection?.invoke(project.id) },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (hasChildren) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = childCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
                if (isLinkedAppearance) {
                    LinkAppearanceBadge()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = displayName ?: AnnotatedString(project.name),
                    modifier = Modifier.padding(start = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                        ),
                    color =
                        if (isFocused) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                )
            }
            dragHandle?.invoke()
            if (dragHandle == null) {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }

        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}

@Composable
fun SwipeableProjectRow(
    project: Context,
    level: Int,
    hasChildren: Boolean,
    childCount: Int,
    isLinkedAppearance: Boolean = false,
    onProjectClick: (String) -> Unit,
    onProjectFocus: (Context) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    dragHandle: @Composable (() -> Unit)? = null,
    onAddSubproject: (project: Context) -> Unit,
    onDelete: (project: Context) -> Unit,
    onEdit: (project: Context) -> Unit,
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
    isFocused: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: ((String) -> Unit)? = null,
    onStartSelection: ((String) -> Unit)? = null,
) {
    val contextId = remember(project.id) { ContextId(project.id) }
    val isSystemContext = remember(project.id) { SystemContexts.isSystem(contextId) }
    val canRenameOrMove = remember(project.id) { SystemContexts.canRenameOrMove(contextId) }
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

    val draggableState =
        rememberDraggableState { delta ->
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
                    enabled = !isSelectionMode,
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

        if (startProgress > SWIPE_ACTION_VISIBILITY_THRESHOLD) {
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
                    modifier =
                        Modifier
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
                        onProjectFocus(project)
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

        if (endProgress > SWIPE_ACTION_VISIBILITY_THRESHOLD && canRenameOrMove) {
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
                    modifier =
                        Modifier
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
            childCount = childCount,
            isLinkedAppearance = isLinkedAppearance,
            onProjectClick = onProjectClick,
            onProjectFocus = onProjectFocus,
            isCurrentlyDragging = isCurrentlyDragging,
            isHovered = isHovered,
            isDraggingDown = isDraggingDown,
            isHighlighted = isHighlighted,
            dragHandle = dragHandle,
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) },
            displayName = displayName,
            isFocused = isFocused,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onToggleSelection = onToggleSelection,
            onStartSelection = onStartSelection,
        )
    }
}

@Composable
private fun LinkAppearanceBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = "link",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ProjectSwipeActionButton(
    icon: ImageVector,
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
    @Suppress("UNUSED_VARIABLE")
    val unusedCallbacks = onFocusedListMenuClick to onOpenAsProject
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                            color =
                                if (isLast) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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

internal fun buildVisibleHierarchy(
    flattenedHierarchy: List<FlatHierarchyItem>,
    childMap: Map<String, List<Context>>,
    longDescendantsMap: Map<String, Boolean>,
): List<FlatHierarchyItem> = flattenedHierarchy

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HierarchyListItem(
    item: FlatHierarchyItem,
    childMap: Map<String, List<Context>>,
    dragAndDropState: DragAndDropState<Context>,
    isSearchActive: Boolean,
    highlightedProjectId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    focusedProjectId: String?,
    longDescendantsMap: Map<String, Boolean>,
    isSelectionMode: Boolean,
    selectedContextIds: Set<String>,
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
    @Suppress("UNUSED_VARIABLE")
    val unusedInputs = Triple(settings, longDescendantsMap, animatedVisibilityScope)
    val project = item.project
    val children = childMap[project.id].orEmpty()
    val hasChildren = children.isNotEmpty()
    val draggedProject = dragAndDropState.draggedItem?.data
    val isCurrentlyDragging = draggedProject?.id == project.id

    val displayName =
        if (isSearchActive && searchQuery.isNotEmpty()) {
            if (searchQuery.length > SHORT_QUERY_THRESHOLD) {
                highlightFuzzy(text = project.name, query = searchQuery)
            } else {
                highlightSubstring(text = project.name, query = searchQuery)
            }
        } else {
            AnnotatedString(project.name)
        }

    val isFocused = project.id == focusedProjectId
    val isSelected = project.id in selectedContextIds

    with(sharedTransitionScope) {
        val isDropAllowed =
            remember(draggedProject, project) {
                draggedProject == null || draggedProject.id != project.id
            }

        val hoveredDropTargetKey = dragAndDropState.hoveredDropTargetKey
        val isHovered =
            remember(hoveredDropTargetKey, project.id, isDropAllowed) {
                isDropAllowed &&
                    (
                        hoveredDropTargetKey == "before-${project.id}" ||
                            hoveredDropTargetKey == "after-${project.id}"
                    )
            }
        val isDraggingDown =
            remember(hoveredDropTargetKey, project.id, isDropAllowed) {
                isDropAllowed && hoveredDropTargetKey == "after-${project.id}"
            }

        Box(modifier = Modifier.fillMaxWidth()) {
            SwipeableProjectRow(
                project = project,
                level = item.level,
                hasChildren = hasChildren,
                childCount = children.size,
                isLinkedAppearance = item.isLinkedAppearance,
                onProjectClick = onProjectClick,
                onProjectFocus = onFocusProject,
                isCurrentlyDragging = isCurrentlyDragging,
                isHovered = isHovered,
                isDraggingDown = isDraggingDown,
                isHighlighted = project.id == highlightedProjectId,
                displayName = displayName,
                dragHandle =
                    if (isSelectionMode) {
                        null
                    } else {
                        {
                            DraggableItem(
                                state = dragAndDropState,
                                key = project.id,
                                data = project,
                                dragAfterLongPress = true,
                            ) {
                                IconButton(
                                    onClick = { onMenuRequested(project) },
                                    modifier = Modifier.padding(start = 2.dp).size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Дії з контекстом",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    },
                onAddSubproject = onAddSubproject,
                onDelete = onDeleteProject,
                onEdit = onEditProject,
                isFocused = isFocused,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onToggleSelection = onToggleSelection,
                onStartSelection = onStartSelection,
            )

            if (!isCurrentlyDragging && !isSelectionMode) {
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
}
