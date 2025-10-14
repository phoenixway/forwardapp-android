package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Project

sealed class RemindersUiEvent {
    data class Navigate(val route: String) : RemindersUiEvent()
}

sealed class ReminderListItem {
    abstract val id: String
    abstract val name: String
    abstract val reminderTime: Long?

    data class GoalReminder(val goal: Goal) : ReminderListItem() {
        override val id: String = goal.id
        override val name: String = goal.text
        override val reminderTime: Long? = goal.reminderTime
    }
    data class ProjectReminder(val project: Project) : ReminderListItem() {
        override val id: String = project.id
        override val name: String = project.name
        override val reminderTime: Long? = project.reminderTime
    }
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _itemToEdit = MutableStateFlow<ReminderListItem?>(null)
    val itemToEdit: StateFlow<ReminderListItem?> = _itemToEdit.asStateFlow()

    private val _uiEvent = Channel<RemindersUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val reminders: StateFlow<List<ReminderListItem>> = projectRepository.getAllGoalsFlow()
        .map { goals -> goals.filter { it.reminderTime != null } }
        .combine(projectRepository.getAllProjectsFlow().map { projects -> projects.filter { it.reminderTime != null } }) { goals, projects ->
            val goalReminders = goals.map { ReminderListItem.GoalReminder(it) }
            val projectReminders = projects.map { ReminderListItem.ProjectReminder(it) }
            (goalReminders + projectReminders).sortedBy { it.reminderTime }
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
            // TODO: Refactor with ReminderRepository
        }
    }

    fun clearReminder(id: String) {
        viewModelScope.launch {
            // TODO: Refactor with ReminderRepository
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            // TODO: Refactor with ReminderRepository
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
            // TODO: Refactor with ReminderRepository
        }
    }
}
