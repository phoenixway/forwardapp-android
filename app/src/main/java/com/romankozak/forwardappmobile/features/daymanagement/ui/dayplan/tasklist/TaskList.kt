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
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayThemesUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayThemeUsageSummary
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    actions: TaskListActions,
    lazyListState: LazyListState = rememberLazyListState(),
    dayThemesState: DayThemesUiState,
    onToggleTheme: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val orientationSources = rememberDayTaskOrientationSources()
    var internalTasks by remember(tasks) { mutableStateOf(tasks) }

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val reordered = reorderDayTaskRows(internalTasks, from.key, to.key)
            if (reordered !== internalTasks) {
                internalTasks = reordered
                actions.onTasksReordered(reordered)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (internalTasks.isEmpty()) {
            EmptyTasksState(
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            TaskListContent(
                tasks = internalTasks,
                linkedProjectTitles = linkedProjectTitles,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                lazyListState = lazyListState,
                reorderableState = reorderableState,
                actions = actions,
                dayThemesState = dayThemesState,
                onToggleTheme = onToggleTheme,
                orientationSources = orientationSources,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListContent(
    tasks: List<DayTaskWithReminder>,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    lazyListState: LazyListState,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    actions: TaskListActions,
    dayThemesState: DayThemesUiState,
    onToggleTheme: (String, String) -> Unit,
    orientationSources: DayTaskOrientationSourcesUi,
) {
    val composition = com.romankozak.forwardappmobile.features.planning.rememberPlanningCompositionList()
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (tasks.any { dayThemesState.themesFor(it.dayTask.id).isNotEmpty() }) item(key = "task-theme-summary") {
            DayThemeUsageSummary(
                label = "Завдання",
                themes = dayThemesState.themes,
                entityIds = tasks.map { it.dayTask.id },
                assignments = dayThemesState.assignments,
                minutesByEntity = tasks.associate { it.dayTask.id to (it.dayTask.estimatedDurationMinutes ?: 0L) },
            )
        }
        items(tasks, key = { it.dayTask.id }) { taskWithReminder ->
            TaskListItem(
                taskWithReminder = taskWithReminder,
                linkedProjectTitles = linkedProjectTitles,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                reorderableState = reorderableState,
                actions = actions,
                dayThemesState = dayThemesState,
                onToggleTheme = onToggleTheme,
                orientationSources = orientationSources,
                composition = composition,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.TaskListItem(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    actions: TaskListActions,
    dayThemesState: DayThemesUiState,
    onToggleTheme: (String, String) -> Unit,
    orientationSources: DayTaskOrientationSourcesUi,
    composition: com.romankozak.forwardappmobile.features.planning.PlanningCompositionListUi,
) {
    ReorderableItem(reorderableState, key = taskWithReminder.dayTask.id) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = elevation / 8),
        ) {
            Surface(modifier = Modifier.fillMaxWidth(),
                color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                shadowElevation = elevation) {
                Column {
                    DayTaskCard(
                        taskWithReminder = taskWithReminder,
                        linkedProjectTitles = linkedProjectTitles,
                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        actions =
                            DayTaskCardActions(
                                onClick = { actions.onTaskClick(taskWithReminder) },
                                onToggle = { actions.onToggleTask(taskWithReminder.dayTask.id) },
                                onLongPress = { actions.onTaskLongPress(taskWithReminder) },
                                onParentInfoClick = actions.onParentInfoClick,
                            ),
                        dragHandleModifier = Modifier.draggableHandle(),
                        themes = dayThemesState.themes,
                        selectedThemeIds = dayThemesState.assignments[taskWithReminder.dayTask.id].orEmpty(),
                        onToggleTheme = { themeId -> onToggleTheme(taskWithReminder.dayTask.id, themeId) },
                        orientationSource = orientationSources.byTask[taskWithReminder.dayTask.id],
                        relatedOrientations = orientationSources.relatedByTask[taskWithReminder.dayTask.id].orEmpty(),
                        onOpenOrientation = orientationSources.onOpen,
                        planningContext = {
                            com.romankozak.forwardappmobile.features.planning.PlanningCompositionPreview(
                                com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                                    com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.DAY_TASK,
                                    taskWithReminder.dayTask.id), composition, compact = true)
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
