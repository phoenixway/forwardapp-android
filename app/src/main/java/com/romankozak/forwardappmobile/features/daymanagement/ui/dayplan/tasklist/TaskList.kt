package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    onTaskLongPress: (DayTaskWithReminder) -> Unit,
    onTasksReordered: (List<DayTaskWithReminder>) -> Unit,
    onToggleTask: (String) -> Unit,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalTasks by remember(tasks) { mutableStateOf(tasks) }

    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalTasks =
                internalTasks.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            onTasksReordered(internalTasks)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (internalTasks.isEmpty()) {
            EmptyTasksState(
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
            ) {
                items(internalTasks, key = { it.dayTask.id }) { taskWithReminder ->
                    ReorderableItem(reorderableState, key = taskWithReminder.dayTask.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = elevation / 8),
                        ) {
                            val colorScheme = MaterialTheme.colorScheme
                            val itemState =
                                if (taskWithReminder.dayTask.completed) {
                                    UnifiedItemState.COMPLETED
                                } else {
                                    UnifiedItemState.DEFAULT
                                }
                            val dayTaskContainerColor =
                                when (itemState) {
                                    UnifiedItemState.COMPLETED -> colorScheme.secondaryContainer.copy(alpha = 0.28f)
                                    UnifiedItemState.DEFAULT -> colorScheme.primaryContainer.copy(alpha = 0.34f)
                                    UnifiedItemState.SELECTED -> colorScheme.surfaceContainerHighest
                                    UnifiedItemState.OVERDUE -> colorScheme.errorContainer.copy(alpha = 0.50f)
                                    UnifiedItemState.DISABLED -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            val dayTaskBorderColor =
                                when (itemState) {
                                    UnifiedItemState.COMPLETED -> colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    UnifiedItemState.DEFAULT -> colorScheme.primary.copy(alpha = 0.30f)
                                    UnifiedItemState.SELECTED -> colorScheme.primary.copy(alpha = 0.4f)
                                    UnifiedItemState.OVERDUE -> colorScheme.error.copy(alpha = 0.45f)
                                    UnifiedItemState.DISABLED -> colorScheme.outlineVariant.copy(alpha = 0.35f)
                                }
                            UnifiedListItemSurface(
                                isSelected = isDragging,
                                state = itemState,
                                containerColorOverride = dayTaskContainerColor,
                                borderColorOverride = dayTaskBorderColor,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DayTaskCard(
                                    taskWithReminder = taskWithReminder,
                                    onToggle = { onToggleTask(taskWithReminder.dayTask.id) },
                                    onLongPress = { onTaskLongPress(taskWithReminder) },
                                    dragHandleModifier = Modifier.draggableHandle(),
                                    onParentInfoClick = onParentInfoClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Checklist,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Завдань ще немає",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Натисніть кнопку '+' для додавання",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
