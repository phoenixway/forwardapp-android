package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RemindersUiEvent {
    data class Navigate(val route: String) : RemindersUiEvent()
}

sealed class ReminderListItem {
    abstract val reminder: Reminder

    data class GoalReminder(override val reminder: Reminder, val goal: Goal) : ReminderListItem()
    data class ProjectReminder(override val reminder: Reminder, val project: Project) : ReminderListItem()
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _itemToEdit = MutableStateFlow<ReminderListItem?>(null)
    val itemToEdit: StateFlow<ReminderListItem?> = _itemToEdit.asStateFlow()

    private val _uiEvent = Channel<RemindersUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val reminders: StateFlow<List<ReminderListItem>> = reminderRepository.getAllReminders()
        .map { reminders ->
            reminders.mapNotNull {
                when (it.entityType) {
                    "GOAL" -> {
                        val goal = projectRepository.getGoalById(it.entityId)
                        if (goal != null) {
                            ReminderListItem.GoalReminder(it, goal)
                        } else {
                            null
                        }
                    }
                    "PROJECT" -> {
                        val project = projectRepository.getProjectById(it.entityId)
                        if (project != null) {
                            ReminderListItem.ProjectReminder(it, project)
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onEditReminder(item: ReminderListItem) {
        _itemToEdit.value = item
    }

    fun onDismissEditReminder() {
        _itemToEdit.value = null
    }

    fun setReminder(id: String, timestamp: Long) {
        viewModelScope.launch {
            val item = reminders.value.find { it.reminder.id == id } ?: return@launch
            reminderRepository.createReminder(item.reminder.entityId, item.reminder.entityType, timestamp)
            onDismissEditReminder()
        }
    }

    fun clearReminder(id: String) {
        viewModelScope.launch {
            val item = reminders.value.find { it.reminder.id == id } ?: return@launch
            reminderRepository.clearRemindersForEntity(item.reminder.entityId)
            onDismissEditReminder()
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            reminders.value.forEach {
                reminderRepository.clearRemindersForEntity(it.reminder.entityId)
            }
        }
    }

    fun showItemInProject(item: ReminderListItem) {
        viewModelScope.launch {
            when (item) {
                is ReminderListItem.GoalReminder -> {
                    val projectId = projectRepository.findProjectIdForGoal(item.goal.id)
                    if (projectId != null) {
                        _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/$projectId?goalId=${item.goal.id}"))
                    }
                }
                is ReminderListItem.ProjectReminder -> {
                    _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/${item.project.id}"))
                }
            }
        }
    }

    fun deleteReminder(item: ReminderListItem) {
        viewModelScope.launch {
            reminderRepository.clearRemindersForEntity(item.reminder.entityId)
        }
    }
}
