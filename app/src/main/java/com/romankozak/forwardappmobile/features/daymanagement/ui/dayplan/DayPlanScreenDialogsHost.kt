package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist.AddTaskDialog
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog

@Composable
fun DayPlanDialogsHost(
    state: DayPlanContentState,
    dialogState: DayPlanDialogState,
    overlayState: DayPlanOverlayState,
    viewModel: DayPlanViewModel,
    hapticFeedback: HapticFeedback,
) {
    RecurringTaskDialogs(dialogState = dialogState, viewModel = viewModel)
    PlanCreationDialogs(
        state = state,
        dialogState = dialogState,
        overlayState = overlayState,
        viewModel = viewModel,
    )
    SelectedTaskDialogs(
        state = state,
        dialogState = dialogState,
        overlayState = overlayState,
        viewModel = viewModel,
        hapticFeedback = hapticFeedback,
    )
}

@Composable
private fun RecurringTaskDialogs(
    dialogState: DayPlanDialogState,
    viewModel: DayPlanViewModel,
) {
    dialogState.taskToEdit?.let { taskWithReminder ->
        EditRecurringTaskDialog(
            taskWithReminder = taskWithReminder,
            onDismiss = { viewModel.dismissEditConfirmationDialog() },
            onConfirmEditSingle = viewModel::editSingleInstanceOfRecurringTask,
            onConfirmEditAll = { viewModel.editAllFutureInstancesOfRecurringTask() },
        )
    }

    dialogState.taskToDelete?.let { taskWithReminder ->
        DeleteRecurringTaskDialog(
            taskWithReminder = taskWithReminder,
            onDismiss = { viewModel.dismissDeleteConfirmationDialog() },
            onConfirmDeleteSingle = viewModel::deleteSingleInstanceOfRecurringTask,
            onConfirmDeleteAll = viewModel::deleteAllFutureInstancesOfRecurringTask,
        )
    }
}

@Composable
private fun PlanCreationDialogs(
    state: DayPlanContentState,
    dialogState: DayPlanDialogState,
    overlayState: DayPlanOverlayState,
    viewModel: DayPlanViewModel,
) {
    if (dialogState.isAddTaskDialogOpen) {
        AddTaskDialog(
            onDismissRequest = viewModel::dismissAddTaskDialog,
            onConfirm = { title, description, duration, priority, recurrenceRule, points ->
                viewModel.addTask(
                    state.initialDayPlanId,
                    title,
                    description,
                    duration,
                    priority,
                    recurrenceRule,
                    points,
                )
            },
            initialPriority = TaskPriority.MEDIUM,
        )
    }

    if (overlayState.showAddUrlDialog.value) {
        AddWebLinkDialog(
            onDismiss = { overlayState.showAddUrlDialog.value = false },
            onConfirm = { url, name ->
                viewModel.addPlanExternalLink(url, name)
                overlayState.showAddUrlDialog.value = false
            },
        )
    }

    if (overlayState.showAddObsidianDialog.value) {
        AddObsidianLinkDialog(
            onDismiss = { overlayState.showAddObsidianDialog.value = false },
            onConfirm = { noteName, displayName ->
                viewModel.addPlanObsidianLink(noteName, displayName)
                overlayState.showAddObsidianDialog.value = false
            },
        )
    }
}

@Composable
private fun SelectedTaskDialogs(
    state: DayPlanContentState,
    dialogState: DayPlanDialogState,
    overlayState: DayPlanOverlayState,
    viewModel: DayPlanViewModel,
    hapticFeedback: HapticFeedback,
) {
    dialogState.selectedTask?.let { selectedTaskWithReminder ->
        if (!dialogState.isEditTaskDialogOpen) {
            TaskOptionsBottomSheet(
                taskWithReminder = selectedTaskWithReminder,
                onDismiss = viewModel::clearSelectedTask,
                actions =
                    TaskOptionsActions(
                        onEdit = { viewModel.onEditTaskClicked(selectedTaskWithReminder) },
                        onDelete = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onDeleteTaskClicked(it)
                        },
                        onSetReminder = { overlayState.showReminderDialog.value = true },
                        onAddToToday = { viewModel.copyTaskToTodaysPlan(selectedTaskWithReminder) },
                        onAddToTacticalMissions = { viewModel.addTaskToTacticalMissions(selectedTaskWithReminder) },
                        onShowInBacklog = state.navigator.onNavigateToBacklog,
                        onMoveToTop = { viewModel.moveTaskToTop(selectedTaskWithReminder) },
                        onMoveToTomorrow = { viewModel.moveTaskToTomorrow(selectedTaskWithReminder) },
                    ),
                showAddToTodayOption = !state.uiState.isToday,
            )
        }
    }

    if (dialogState.isEditTaskDialogOpen && dialogState.selectedTask != null) {
        EditTaskBottomSheet(
            taskId = dialogState.selectedTask.dayTask.id,
            onDismissRequest = {
                viewModel.dismissEditTaskDialog()
                viewModel.clearSelectedTask()
            },
        )
    }

    if (overlayState.showReminderDialog.value && dialogState.selectedTask != null) {
        ReminderPropertiesDialog(
            onDismiss = {
                overlayState.showReminderDialog.value = false
                viewModel.clearSelectedTask()
            },
            onSetReminder = { reminderTime ->
                dialogState.selectedTask.let { viewModel.setTaskReminder(it.dayTask.id, reminderTime) }
                overlayState.showReminderDialog.value = false
                viewModel.clearSelectedTask()
            },
            onRemoveReminder = { _: String ->
                dialogState.selectedTask.let { viewModel.clearTaskReminder(it.dayTask.id) }
                overlayState.showReminderDialog.value = false
                viewModel.clearSelectedTask()
            },
            currentReminders = listOfNotNull(dialogState.selectedTask.reminder).map { it },
        )
    }
}
