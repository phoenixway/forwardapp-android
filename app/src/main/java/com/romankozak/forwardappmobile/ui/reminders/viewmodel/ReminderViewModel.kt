package com.romankozak.forwardappmobile.ui.reminders.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
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
    data class SimpleReminder(override val reminder: Reminder) : ReminderListItem()
}

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val projectRepository: ProjectRepository,
    private val goalRepository: GoalRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<ReminderListItem>>(emptyList())
    val reminders: StateFlow<List<ReminderListItem>> = _reminders.asStateFlow()

    private val _uiEvent = Channel<RemindersUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _showPropertiesDialog = MutableStateFlow(false)
    val showPropertiesDialog: StateFlow<Boolean> = _showPropertiesDialog

    private val _editingReminder = MutableStateFlow<Reminder?>(null)
    val editingReminder: StateFlow<Reminder?> = _editingReminder

    fun loadReminders(entityId: String? = null, entityType: String? = null) {
        viewModelScope.launch {
            val sourceFlow: Flow<List<Reminder>> = if (entityId != null && entityType != null) {
                savedStateHandle["entityId"] = entityId
                savedStateHandle["entityType"] = entityType
                reminderRepository.getRemindersForEntityFlow(entityId)
            } else {
                reminderRepository.getAllReminders()
            }

            sourceFlow.map { reminders ->
                reminders.mapNotNull { reminder ->
                    when (reminder.entityType) {
                        "GOAL" -> {
                            val goal = goalRepository.getGoalById(reminder.entityId)
                            goal?.let { ReminderListItem.GoalReminder(reminder, it) }
                        }
                        "PROJECT" -> {
                            val project = projectRepository.getProjectById(reminder.entityId)
                            project?.let { ReminderListItem.ProjectReminder(reminder, it) }
                        }
                        else -> ReminderListItem.SimpleReminder(reminder)
                    }
                }
            }.collect {
                _reminders.value = it
            }
        }
    }

    fun addReminder(reminderTime: Long) {
        val entityId: String? = savedStateHandle.get<String>("entityId")
        val entityType: String? = savedStateHandle.get<String>("entityType")
        if (entityId.isNullOrEmpty() || entityType.isNullOrEmpty()) return

        viewModelScope.launch {
            reminderRepository.createReminder(entityId, entityType, reminderTime)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.removeReminder(reminder)
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            reminderRepository.clearAllReminders()
        }
    }
    
    fun onShowPropertiesDialog() {
        _showPropertiesDialog.value = true
    }

    fun onDismissPropertiesDialog() {
        _showPropertiesDialog.value = false
        _editingReminder.value = null
    }

    fun onEditReminder(reminder: Reminder) {
        _editingReminder.value = reminder
        _showPropertiesDialog.value = true
    }

    fun showItemInProject(item: ReminderListItem) {
        viewModelScope.launch {
            when (item) {
                is ReminderListItem.GoalReminder -> {
                    val projectId = goalRepository.findProjectIdForGoal(item.goal.id)
                    if (projectId != null) {
                        _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/$projectId?goalId=${item.goal.id}"))
                    }
                }
                is ReminderListItem.ProjectReminder -> {
                    _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/${item.project.id}"))
                }
                is ReminderListItem.SimpleReminder -> {
                    // Not applicable for simple reminders
                }
            }
        }
    }
}
