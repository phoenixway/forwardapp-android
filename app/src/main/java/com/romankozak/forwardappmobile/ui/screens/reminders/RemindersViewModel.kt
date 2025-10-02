package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.dao.ReminderInfoDao
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ReminderInfo
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

sealed class RemindersUiEvent {
    data class Navigate(val route: String) : RemindersUiEvent()
}

data class ReminderItem(
    val goal: Goal,
    val reminderInfo: ReminderInfo?
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val alarmScheduler: AlarmScheduler,
    private val reminderInfoDao: ReminderInfoDao
) : ViewModel() {

    private val _goalToEdit = MutableStateFlow<Goal?>(null)
    val goalToEdit: StateFlow<Goal?> = _goalToEdit.asStateFlow()

    private val _uiEvent = Channel<RemindersUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val reminders: StateFlow<List<ReminderItem>> = projectRepository.getAllGoalsFlow()
        .map { goals ->
            goals
                .filter { it.reminderTime != null }
        }
        .combine(reminderInfoDao.getAllReminderInfoFlow()) { goals, reminderInfos ->
            goals.map { goal ->
                ReminderItem(
                    goal = goal,
                    reminderInfo = reminderInfos.find { it.goalId == goal.id }
                )
            }
        }
        .map { items -> items.sortedBy { it.goal.reminderTime } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onEditReminder(goal: Goal) {
        _goalToEdit.value = goal
    }

    fun onDismissEditReminder() {
        _goalToEdit.value = null
    }

    fun setReminderForGoal(goalId: String, timestamp: Long) {
        viewModelScope.launch {
            val goal = projectRepository.getGoalById(goalId)
            if (goal != null) {
                val updatedGoal = goal.copy(reminderTime = timestamp)
                projectRepository.updateGoal(updatedGoal)
                alarmScheduler.schedule(updatedGoal)
            }
            onDismissEditReminder()
        }
    }

    fun clearReminderForGoal(goalId: String) {
        viewModelScope.launch {
            val goal = projectRepository.getGoalById(goalId)
            if (goal != null) {
                val updatedGoal = goal.copy(reminderTime = null)
                projectRepository.updateGoal(updatedGoal)
                alarmScheduler.cancel(updatedGoal)
            }
            onDismissEditReminder()
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            val goalsWithReminders = reminders.value.map { it.goal }
            goalsWithReminders.forEach { goal ->
                alarmScheduler.cancel(goal)
            }
            val updatedGoals = goalsWithReminders.map { it.copy(reminderTime = null) }
            projectRepository.updateGoals(updatedGoals)
        }
    }

    fun showGoalInProject(goal: Goal) {
        viewModelScope.launch {
            val projectId = projectRepository.findProjectIdForGoal(goal.id)
            if (projectId != null) {
                _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/$projectId?goalId=${goal.id}"))
            }
        }
    }
}
