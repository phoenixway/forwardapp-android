// File: com/romankozak/forwardappmobile/ui/screens/mainscreen/HierarchyComponents.kt
// ПОВНА ВИПРАВЛЕНА ВЕРСІЯ

package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.min

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


// --- Нові UI Компоненти ---

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
    onNavigateToList: (String) -> Unit
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

    Column {
        DraggableItem(
            state = dragAndDropState,
            key = list.id,
            data = list,
            dragAfterLongPress = true
        ) { // ВИПРАВЛЕНО: Лямбда БЕЗ параметра isDragging
            // isDragging тепер доступний напряму з DraggableItemScope

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
                // GoalListRow тепер знаходиться всередині Box, щоб накласти на нього drop targets
                GoalListRow(
                    list = list,
                    level = level,
                    hasChildren = hasChildren,
                    onListClick = { listId ->
                        if (hasChildren && (level >= settings.maxCollapsibleLevels)) {
                            onNavigateToList(listId)
                        } else {
                            viewModel.onListClicked(listId)
                        }
                    },
                    onToggleExpanded = { goalList -> viewModel.onToggleExpanded(goalList) },
                    onMenuRequested = { goalList -> viewModel.onMenuRequested(goalList) },
                    isCurrentlyDragging = isDragging, // ВИПРАВЛЕНО: isDragging тепер доступний напряму
                    isHovered = isHovered,
                    isDraggingDown = isDraggingDown,
                    isHighlighted = list.id == highlightedListId,
                    displayName = displayName
                )

                // Drop targets для переміщення
                if (!isDragging) {
                    Column(modifier = Modifier.matchParentSize()) {
                        val dropModifier = { position: DropPosition ->
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .then(
                                    if (isDropAllowed) {
                                        Modifier.dropTarget(
                                            state = dragAndDropState,
                                            key = "$position-${list.id}"
                                        ) { draggedItemState ->
                                            viewModel.onListReorder(
                                                fromId = draggedItemState.data.id,
                                                toId = list.id,
                                                position = position
                                            )
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

        // Рекурсивно показуємо дочірні елементи
        if (list.isExpanded && level < settings.maxCollapsibleLevels) {
            // Додаємо відступ для дочірніх елементів
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
                        onNavigateToList = onNavigateToList
                    )
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
    onNavigateToList: (String) -> Unit
) {
    val focusedList = hierarchy.allLists.find { it.id == focusedListId }
    val children = (hierarchy.childMap[focusedListId] ?: emptyList()).sortedBy { it.order }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        focusedList?.let { list ->
            item(key = "focused_header") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider()
            }
        }

        if (children.isNotEmpty()) {
            items(children, key = { it.id }) { child ->
                // У сфокусованому режимі кожен елемент також є Draggable
                SmartHierarchyView(
                    list = child,
                    childMap = hierarchy.childMap,
                    level = 0, // Починаємо з 0 у фокусованому виді
                    dragAndDropState = dragAndDropState,
                    viewModel = viewModel,
                    isSearchActive = isSearchActive,
                    planningMode = planningMode,
                    highlightedListId = highlightedListId,
                    settings = settings,
                    searchQuery = searchQuery,
                    onNavigateToList = onNavigateToList
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