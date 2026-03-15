package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo

data class TaskListActions(
    val onTaskClick: (DayTaskWithReminder) -> Unit,
    val onTaskLongPress: (DayTaskWithReminder) -> Unit,
    val onTasksReordered: (List<DayTaskWithReminder>) -> Unit,
    val onToggleTask: (String) -> Unit,
    val onParentInfoClick: (ParentInfo) -> Unit,
)
