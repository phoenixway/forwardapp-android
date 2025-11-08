package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import android.net.Uri
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DialogState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.DialogStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class DialogUseCase @Inject constructor(
    private val dialogStateManager: DialogStateManager,
    private val activityRepository: ActivityRepository,
    private val reminderRepository: ReminderRepository,
    private val projectRepository: ProjectRepositoryCore,
) {
    val dialogState: StateFlow<DialogState> = dialogStateManager.dialogState

    private val _recordForReminderDialog = MutableStateFlow<ActivityRecord?>(null)
    val recordForReminderDialog: StateFlow<ActivityRecord?> = _recordForReminderDialog.asStateFlow()

    fun onAddNewProjectRequest() {
        dialogStateManager.onAddNewProjectRequest()
    }

    fun onAddSubprojectRequest(parentProject: Project) {
        dialogStateManager.onAddSubprojectRequest(parentProject)
    }

    fun onMenuRequested(project: Project) {
        dialogStateManager.onMenuRequested(project)
    }

    fun onDeleteRequest(project: Project) {
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

    fun onSetReminder(scope: CoroutineScope, timestamp: Long) =
        scope.launch {
            val record = _recordForReminderDialog.value ?: return@launch

            val entityType = when {
                record.goalId != null -> "GOAL"
                record.projectId != null -> "PROJECT"
                else -> "TASK" // Assuming ActivityRecord can also be a task
            }
            val entityId = record.goalId ?: record.projectId ?: record.id

            reminderRepository.createReminder(entityId, entityType, timestamp)

            onReminderDialogDismiss()
        }

    fun onClearReminder(scope: CoroutineScope) =
        scope.launch {
            val record = _recordForReminderDialog.value ?: return@launch

            val entityId = record.goalId ?: record.projectId ?: record.id
            reminderRepository.clearRemindersForEntity(entityId)

            onReminderDialogDismiss()
        }

    fun onSetReminderForProject(scope: CoroutineScope, project: Project) {
        scope.launch {
            val reminders = reminderRepository.getRemindersForEntityFlow(project.id).firstOrNull()
            val record = ActivityRecord(
                id = project.id,
                text = project.name,
                reminderTime = reminders?.firstOrNull()?.reminderTime,
                createdAt = project.createdAt,
                projectId = project.id,
                goalId = null,
            )
            _recordForReminderDialog.update { record }
            dialogStateManager.dismissDialog()
        }
    }

    fun setReminderForOngoingActivity(scope: CoroutineScope, lastOngoingActivity: StateFlow<ActivityRecord?>) {
        scope.launch {
            lastOngoingActivity.value?.let {
                _recordForReminderDialog.update { lastOngoingActivity.value }
            }
        }
    }
}
