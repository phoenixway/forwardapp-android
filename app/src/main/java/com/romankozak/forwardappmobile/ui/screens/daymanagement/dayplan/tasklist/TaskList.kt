package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.CompactDayPlanHeader
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.TaskGoalItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    dayPlan: DayPlan?,
    totalPoints: Int,
    onTaskLongPress: (DayTaskWithReminder) -> Unit,
    onTasksReordered: (List<DayTaskWithReminder>) -> Unit, // Keep for now
    onToggleTask: (String) -> Unit,
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
    onSublistClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerContainerColor: Color
) {
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

        if (tasks.isEmpty()) {
            EmptyTasksState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.dayTask.id }) { task ->
                    TaskGoalItem(
                        taskWithReminder = task,
                        onToggle = { onToggleTask(task.dayTask.id) },
                        onLongPress = { onTaskLongPress(task) },
                        modifier = Modifier.animateItemPlacement()
                    )
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
            modifier = Modifier.padding(24.dp).height(80.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Завдань ще немає",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Натисніть кнопку '+'/Додати перше завдання",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
