package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state.DialogStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class DialogUseCase
    @Inject
    constructor(
        private val dialogStateManager: DialogStateManager,
        private val activityRepository: ActivityRepository,
        private val reminderRepository: ReminderRepository,
        private val contextRepository: ContextRepository,
    ) {
        val dialogState: StateFlow<DialogState> = dialogStateManager.dialogState

        private val _recordForReminderDialog = MutableStateFlow<ActivityRecord?>(null)
        val recordForReminderDialog: StateFlow<ActivityRecord?> = _recordForReminderDialog.asStateFlow()

        fun onAddNewProjectRequest() {
            dialogStateManager.onAddNewProjectRequest()
        }

        fun onAddSubprojectRequest(parentProject: Context) {
            dialogStateManager.onAddSubprojectRequest(parentProject)
        }

        fun onMenuRequested(project: Context) {
            dialogStateManager.onMenuRequested(project)
        }

        fun onDeleteRequest(project: Context) {
            dialogStateManager.onDeleteRequest(project)
        }

        fun onShowAboutDialog() {
            dialogStateManager.onShowAboutDialog()
        }

        fun onImportFromFileRequested(uri: Uri) {
            dialogStateManager.onImportFromFileRequested(uri)
        }

        fun dismissDialog() {
            dialogStateManager.dismissDialog()
        }

        fun onReminderDialogDismiss() {
            _recordForReminderDialog.update { null }
        }

        fun onSetReminder(
            scope: CoroutineScope,
            timestamp: Long,
        ) = scope.launch {
            val record = _recordForReminderDialog.value ?: return@launch

            val entityType =
                when {
                    record.goalId != null -> "GOAL"
                    record.contextId != null -> "PROJECT"
                    else -> "TASK" // Assuming ActivityRecord can also be a task
                }
            val entityId = record.goalId ?: record.contextId ?: record.id

            reminderRepository.createReminder(entityId, entityType, timestamp)

            onReminderDialogDismiss()
        }

        fun onClearReminder(scope: CoroutineScope) =
            scope.launch {
                val record = _recordForReminderDialog.value ?: return@launch

                val entityId = record.goalId ?: record.contextId ?: record.id
                reminderRepository.clearRemindersForEntity(entityId)

                onReminderDialogDismiss()
            }

        fun onSetReminderForProject(
            scope: CoroutineScope,
            project: Context,
        ) {
            scope.launch {
                val reminders = reminderRepository.getRemindersForEntityFlow(project.id).firstOrNull()
                val record =
                    ActivityRecord(
                        id = project.id,
                        text = project.name,
                        reminderTime = reminders?.firstOrNull()?.reminderTime,
                        createdAt = project.createdAt,
                        contextId = project.id,
                        goalId = null,
                    )
                _recordForReminderDialog.update { record }
                dialogStateManager.dismissDialog()
            }
        }

        fun setReminderForOngoingActivity(
            scope: CoroutineScope,
            lastOngoingActivity: StateFlow<ActivityRecord?>,
        ) {
            scope.launch {
                lastOngoingActivity.value?.let {
                    _recordForReminderDialog.update { lastOngoingActivity.value }
                }
            }
        }
    }
