package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderActions(
    private val reminderRepository: ReminderRepository,
    private val stateManager: ContextStateManager,
    private val uiState: StateFlow<ContextUiState>,
    private val showSnackbar: (String, String?) -> Unit,
    private val forceRefresh: () -> Unit,
) {
    suspend fun onSetReminderForProject(project: Context?) {
        project?.let { proj ->
            val reminders = reminderRepository.getRemindersForEntityFlow(proj.id).firstOrNull().orEmpty()
            val record =
                ActivityRecord(
                    id = proj.id,
                    text = proj.name,
                    reminderTime = reminders.firstOrNull()?.reminderTime,
                    createdAt = proj.createdAt,
                    contextId = proj.id,
                    goalId = null,
                )
            stateManager.updateState { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
        }
    }

    suspend fun onSetReminderForItem(item: BacklogItemContent) {
        when (item) {
            is BacklogItemContent.GoalItem -> {
                val entityId = item.goal.id
                val reminders = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
                val record =
                    ActivityRecord(
                        id = entityId,
                        text = item.goal.text,
                        reminderTime = reminders.firstOrNull()?.reminderTime,
                        createdAt = item.goal.createdAt,
                        contextId = item.backlogItem.contextId,
                        goalId = item.goal.id,
                    )
                stateManager.updateState { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
            }

            is BacklogItemContent.SublistItem -> {
                val entityId = item.project.id
                val reminders = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
                val record =
                    ActivityRecord(
                        id = entityId,
                        text = item.project.name,
                        reminderTime = reminders.firstOrNull()?.reminderTime,
                        createdAt = item.project.createdAt,
                        contextId = item.project.id,
                        goalId = null,
                    )
                stateManager.updateState { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
            }

            else -> Unit
        }
    }

    fun onOpenRemindersDialog(itemContent: BacklogItemContent) {
        stateManager.updateState { it.copy(showRemindersDialog = true, itemForRemindersDialog = itemContent) }
    }

    fun onDismissRemindersDialog() {
        stateManager.updateState {
            it.copy(
                recordForReminderDialog = null,
                remindersForDialog = emptyList(),
                showRemindersDialog = false,
                itemForRemindersDialog = null,
            )
        }
    }

    suspend fun onClearReminder() {
        val record = uiState.value.recordForReminderDialog ?: return
        val entityId = record.goalId ?: record.contextId ?: record.id
        reminderRepository.clearRemindersForEntity(entityId)
        onDismissRemindersDialog()
        showSnackbar("Нагадування скасовано", null)
        forceRefresh()
    }

    suspend fun onSetReminder(timestamp: Long) {
        val record = uiState.value.recordForReminderDialog ?: return
        val entityType =
            when {
                record.goalId != null -> "GOAL"
                record.contextId != null -> "PROJECT"
                else -> "TASK"
            }
        val entityId = record.goalId ?: record.contextId ?: record.id
        reminderRepository.createReminder(entityId, entityType, timestamp)
        showSnackbar(
            "Нагадування додано на ${
                SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(
                    Date(timestamp),
                )
            }",
            null,
        )
        forceRefresh()
    }

    suspend fun onRemoveReminder(reminderId: String) {
        val record = uiState.value.recordForReminderDialog ?: return
        val currentReminder = uiState.value.remindersForDialog.firstOrNull { it.id == reminderId } ?: return
        reminderRepository.removeReminder(currentReminder)

        val entityId = record.goalId ?: record.contextId ?: record.id
        val refreshed = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
        val updatedRecord = record.copy(reminderTime = refreshed.firstOrNull()?.reminderTime)
        stateManager.updateState { it.copy(remindersForDialog = refreshed, recordForReminderDialog = updatedRecord) }

        showSnackbar("Нагадування видалено", null)
        forceRefresh()
    }
}
