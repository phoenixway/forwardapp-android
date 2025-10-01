package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ReminderStatus
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = projectRepository.getAllGoalsFlow()
        .map { goals ->
            goals
                .filter { it.reminderTime != null }
                .map { Reminder(it, it.reminderStatus) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun snoozeReminder(goalId: String) {
        viewModelScope.launch {
            projectRepository.getGoalById(goalId)?.let {
                val snoozedGoal = it.copy(
                    reminderStatus = ReminderStatus.SNOOZED,
                    snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000 // 15 minutes
                )
                projectRepository.updateGoal(snoozedGoal)
                alarmScheduler.schedule(snoozedGoal)
            }
        }
    }

    fun completeReminder(goalId: String) {
        viewModelScope.launch {
            projectRepository.getGoalById(goalId)?.let {
                val completedGoal = it.copy(reminderStatus = ReminderStatus.COMPLETED)
                projectRepository.updateGoal(completedGoal)
            }
        }
    }

    fun dismissReminder(goalId: String) {
        viewModelScope.launch {
            projectRepository.getGoalById(goalId)?.let {
                val dismissedGoal = it.copy(reminderTime = null)
                projectRepository.updateGoal(dismissedGoal)
            }
        }
    }

    fun clearCompletedReminders() {
        viewModelScope.launch {
            val completedGoals = reminders.value
                .filter { it.status == ReminderStatus.COMPLETED }
                .map { it.goal.copy(reminderTime = null, reminderStatus = ReminderStatus.ACTIVE) }
            projectRepository.updateGoals(completedGoals)
        }
    }
}
