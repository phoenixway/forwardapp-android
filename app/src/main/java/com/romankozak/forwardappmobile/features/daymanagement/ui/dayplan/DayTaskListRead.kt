package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.data.planning.day.DayTaskGoalParent
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.distinctUntilChanged

/** Metadata may arrive later, but must never gate an already-committed task-list emission. */
internal fun observeDayTaskList(
    tasks: Flow<List<DayTask>>,
    reminders: Flow<List<Reminder>>,
    goals: Flow<Map<String, DayTaskGoalParent>>,
    workspaces: Flow<List<ContextPresentation>>,
): Flow<List<DayTaskWithReminder>> = combine(tasks, reminders.onStart { emit(emptyList()) },
    goals.onStart { emit(emptyMap()) }, workspaces.onStart { emit(emptyList()) }) { rows, reminderRows, goalRows, workspaceRows ->
    val remindersById = reminderRows.filterNot { it.isDeleted }.groupBy { it.entityId }
    val projects = workspaceRows.associateBy { it.id }
    rows.map { task ->
        val parent = if (task.goalId != null) goalRows[task.goalId]?.let { ParentInfo(it.id, it.title, ParentType.GOAL, it.workspaceId) }
            else task.logicalProjectId?.let { projects[it] }?.let { ParentInfo(it.id, it.name, ParentType.PROJECT, it.id) }
        DayTaskWithReminder(task, remindersById[task.id]?.firstOrNull(), parent)
    }
}.distinctUntilChanged().flowOn(Dispatchers.Default)
