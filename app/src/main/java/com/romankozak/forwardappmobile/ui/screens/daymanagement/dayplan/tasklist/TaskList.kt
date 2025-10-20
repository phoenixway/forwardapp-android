package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.CompactDayPlanHeader
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    dayPlan: DayPlan?,
    totalPoints: Int,
    onTaskLongPress: (DayTaskWithReminder) -> Unit,
    onTasksReordered: (List<DayTaskWithReminder>) -> Unit,
    onToggleTask: (String) -> Unit,
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
    onSublistClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerContainerColor: Color
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalTasks by remember(tasks) { mutableStateOf(tasks) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        internalTasks = internalTasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onTasksReordered(internalTasks)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(modifier = modifier.fillMaxSize()) {
        CompactDayPlanHeader(
            dayPlan = dayPlan,
            totalPoints = totalPoints,
            onNavigateToPreviousDay = onNavigateToPreviousDay,
            onNavigateToNextDay = onNavigateToNextDay,
            isNextDayNavigationEnabled = isNextDayNavigationEnabled,
            onSettingsClick = onSettingsClick,
            containerColor = headerContainerColor
        )

        if (internalTasks.isEmpty()) {
            EmptyTasksState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(internalTasks, key = { it.dayTask.id }) { taskWithReminder ->
                    ReorderableItem(reorderableState, key = taskWithReminder.dayTask.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        Surface(shadowElevation = elevation, shape = MaterialTheme.shapes.medium) {
                            TaskGoalItem(
                                taskWithReminder = taskWithReminder,
                                onToggle = { onToggleTask(taskWithReminder.dayTask.id) },
                                onLongPress = { onTaskLongPress(taskWithReminder) },
                                dragHandle = {
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = "Перетягнути",
                                        modifier = Modifier
                                            .draggableHandle()
                                            .padding(end = 8.dp)
                                            .size(24.dp),
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskGoalItem(
    taskWithReminder: DayTaskWithReminder,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    val task = taskWithReminder.dayTask
    val reminder = taskWithReminder.reminder
    val goalContent = ListItemContent.GoalItem(goal = task.toGoal(), listItem = task.toListItem(), reminders = listOfNotNull(reminder))

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(end = 8.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    GoalItem(
                        goal = goalContent.goal,
                        obsidianVaultName = "",
                        onCheckedChange = { _ -> },
                        onItemClick = { },
                        onLongClick = { onLongPress() },
                        onTagClick = { },
                        onRelatedLinkClick = { },
                        modifier = Modifier,
                        emojiToHide = null,
                        contextMarkerToEmojiMap = emptyMap(),
                        isSelected = false,
                        reminders = listOfNotNull(reminder),
                        endAction = { }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                dragHandle()
                IconButton(onClick = onLongPress, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Більше опцій",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun DayTask.toGoal(): Goal {
  return Goal(
    id = this.id,
    text = this.title,
    description = this.description,
    completed = this.completed,
    scoringStatus = ScoringStatusValues.NOT_ASSESSED,
    displayScore = 0,
    relatedLinks = null,
    createdAt = this.createdAt,
    updatedAt = this.createdAt,
  )
}

fun DayTask.toListItem(): ListItem {
  return ListItem(
    id = this.id,
    projectId = this.projectId ?: this.dayPlanId,
    itemType = this.taskType ?: ListItemTypeValues.GOAL,
    entityId = this.entityId ?: this.goalId ?: this.id,
    order = this.order,
  )
}