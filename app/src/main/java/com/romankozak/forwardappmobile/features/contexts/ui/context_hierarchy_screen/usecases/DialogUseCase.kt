package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
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
        private val reminderRepository: ReminderRepository,
    ) {
        val dialogState: StateFlow<DialogState> = dialogStateManager.dialogState

        private val _recordForReminderDialog = MutableStateFlow<ActivityRecord?>(null)
        val recordForReminderDialog: StateFlow<ActivityRecord?> = _recordForReminderDialog.asStateFlow()

        fun onAddProjectRequest(parentProject: Context? = null) {
            if (parentProject == null) {
                dialogStateManager.onAddNewProjectRequest()
            } else {
                dialogStateManager.onAddSubprojectRequest(parentProject)
            }
        }

        fun onMenuRequested(
            project: Context,
            canPasteContextLinks: Boolean = false,
        ) {
            dialogStateManager.onMenuRequested(project, canPasteContextLinks)
        }

        fun onDeleteRequest(project: Context) {
            dialogStateManager.onDeleteRequest(project)
        }

        fun onUtilityDialogRequest(request: UtilityDialogRequest) {
            when (request) {
                UtilityDialogRequest.About -> dialogStateManager.onShowAboutDialog()
                UtilityDialogRequest.Export -> dialogStateManager.onExportToFileRequested()
                is UtilityDialogRequest.Import -> dialogStateManager.onImportFromFileRequested(request.uri)
            }
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

sealed interface UtilityDialogRequest {
    data object About : UtilityDialogRequest

    data object Export : UtilityDialogRequest

    data class Import(val uri: Uri) : UtilityDialogRequest
}
