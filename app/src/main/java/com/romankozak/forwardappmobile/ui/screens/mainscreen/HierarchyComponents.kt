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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.components.GoalListRow

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


/**
 * Оновлений компонент GoalListRow, який включає ваш новий дизайн
 * та логіку приховування іконки розгортання.
 */
@Composable
fun GoalListRow(
    list: GoalList,
    level: Int,
    hasChildren: Boolean,
    onListClick: (String) -> Unit,
    onToggleExpanded: (list: GoalList) -> Unit,
    onMenuRequested: (list: GoalList) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    showFocusButton: Boolean,
    onFocusRequested: (list: GoalList) -> Unit,
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
                .clickable { onListClick(list.id) }
                .alpha(if (isCurrentlyDragging) 0.6f else 1f)
                .padding(start = indentation)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Іконка розгортання/згортання
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (hasChildren && !showFocusButton) {
                    IconButton(
                        onClick = { onToggleExpanded(list) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (list.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Згорнути/Розгорнути",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }

            // Назва списку
            Text(
                text = displayName ?: AnnotatedString(list.name),
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

            // Кнопка фокусування
            AnimatedVisibility(
                visible = showFocusButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { onFocusRequested(list) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz, // <-- ЗМІНА ІКОНКИ
                        contentDescription = if (isFocused) "Вийти з фокусу" else "Сфокусуватися",
                        tint = if (isFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Іконка "Більше"
            IconButton(
                onClick = { onMenuRequested(list) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Дії зі списком",
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
    modifier: Modifier = Modifier
) {
    if (breadcrumbs.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        LazyRow(
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
                    Text(
                        text = item.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    list: GoalList,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    dragAndDropState: DragAndDropState<GoalList>,
    viewModel: GoalListViewModel,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    highlightedListId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    onNavigateToList: (String) -> Unit,
    focusedListId: String?
) {
    val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()

    val displayName = if (isSearchActive && searchQuery.isNotEmpty()) {
        if (searchQuery.length > 3) {
            highlightFuzzy(text = list.name, query = searchQuery)
        } else {
            highlightSubstring(text = list.name, query = searchQuery)
        }
    } else {
        AnnotatedString(list.name)
    }

    val shouldShowFocusButton = hasChildren && level >= settings.useBreadcrumbsAfter
    val isFocused = list.id == focusedListId

    Column {
        DraggableItem(
            state = dragAndDropState,
            key = list.id,
            data = list,
            dragAfterLongPress = true
        ) {
            val draggedItemData = dragAndDropState.draggedItem?.data
            val isDropAllowed = remember(draggedItemData, list) {
                draggedItemData == null || (draggedItemData.parentId == list.parentId)
            }

            val hoveredDropTargetKey = dragAndDropState.hoveredDropTargetKey
            val isHovered = remember(hoveredDropTargetKey, list.id) {
                isDropAllowed && (hoveredDropTargetKey == "before-${list.id}" || hoveredDropTargetKey == "after-${list.id}")
            }
            val isDraggingDown = remember(hoveredDropTargetKey, list.id) {
                isDropAllowed && hoveredDropTargetKey == "after-${list.id}"
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                GoalListRow(
                    list = list,
                    level = level,
                    hasChildren = hasChildren,
                    onListClick = { listId -> viewModel.onListClicked(listId) },
                    onToggleExpanded = { goalList -> viewModel.onToggleExpanded(goalList) },
                    onMenuRequested = { goalList -> viewModel.onMenuRequested(goalList) },
                    isCurrentlyDragging = isDragging,
                    isHovered = isHovered,
                    isDraggingDown = isDraggingDown,
                    isHighlighted = list.id == highlightedListId,
                    displayName = displayName,
                    showFocusButton = shouldShowFocusButton,
                    onFocusRequested = { onNavigateToList(it.id) },
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
                                        Modifier.dropTarget(state = dragAndDropState, key = "$position-${list.id}") {
                                            viewModel.onListReorder(it.data.id, list.id, position)
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

        if (list.isExpanded) {
            // Перевіряємо, чи є у нащадків довгі імена
            val hasLongNames = remember(list.id, childMap) {
                viewModel.hasDescendantsWithLongNames(list.id, childMap)
            }

            if (hasLongNames) {
                // Якщо так, показуємо індикатор "..."
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = ((level + 1) * 24).dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Дочірні елементи приховані",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 56.dp) // Відступ для вирівнювання з текстом
                    )
                }
            } else {
                // Якщо ні, показуємо дочірні елементи, як і раніше
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    children.forEach { child ->
                        SmartHierarchyView(
                            list = child,
                            childMap = childMap,
                            level = level + 1,
                            dragAndDropState = dragAndDropState,
                            viewModel = viewModel,
                            isSearchActive = isSearchActive,
                            planningMode = planningMode,
                            highlightedListId = highlightedListId,
                            settings = settings,
                            searchQuery = searchQuery,
                            onNavigateToList = onNavigateToList,
                            focusedListId = focusedListId
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusedListView(
    focusedListId: String,
    hierarchy: ListHierarchyData,
    dragAndDropState: DragAndDropState<GoalList>,
    viewModel: GoalListViewModel,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    highlightedListId: String?,
    settings: HierarchyDisplaySettings,
    searchQuery: String,
    onNavigateToList: (String) -> Unit,
    onFocusedHeaderClick: (String) -> Unit
) {
    val focusedList = hierarchy.allLists.find { it.id == focusedListId }
    val children = (hierarchy.childMap[focusedListId] ?: emptyList()).sortedBy { it.order }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        focusedList?.let { list ->
            item(key = "focused_header") {
                GoalListRow(
                    list = list,
                    level = 0,
                    hasChildren = children.isNotEmpty(),
                    onListClick = { onFocusedHeaderClick(list.id) },
                    onToggleExpanded = { viewModel.onToggleExpanded(it) },
                    onMenuRequested = { viewModel.onMenuRequested(it) },
                    isCurrentlyDragging = false,
                    isHovered = false,
                    isDraggingDown = false,
                    isHighlighted = list.id == highlightedListId,
                    showFocusButton = true,
                    onFocusRequested = { onNavigateToList(it.id) },
                    displayName = AnnotatedString(list.name),
                    isFocused = true
                )
                HorizontalDivider()
            }
        }

        if (children.isNotEmpty()) {
            items(children, key = { it.id }) { child ->
                SmartHierarchyView(
                    list = child,
                    childMap = hierarchy.childMap,
                    level = 0,
                    dragAndDropState = dragAndDropState,
                    viewModel = viewModel,
                    isSearchActive = isSearchActive,
                    planningMode = planningMode,
                    highlightedListId = highlightedListId,
                    settings = settings,
                    searchQuery = searchQuery,
                    onNavigateToList = onNavigateToList,
                    focusedListId = focusedListId
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
                            text = "No sublists",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}