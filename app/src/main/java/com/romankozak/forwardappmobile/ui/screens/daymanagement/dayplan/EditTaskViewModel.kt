package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.database.models.DayTask
import com.romankozak.forwardappmobile.core.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.core.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class EditTaskUiState(
    val task: DayTask? = null,
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.NONE,
    val duration: Long? = null,
    val points: Int = 0,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: Set<DayOfWeek> = emptySet(),
)

sealed class EditTaskUiEvent {
    object NavigateUp : EditTaskUiEvent()
}

@HiltViewModel
class EditTaskViewModel @Inject constructor(
    private val dayManagementRepository: DayManagementRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTaskUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<EditTaskUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val taskId = savedStateHandle.get<String>("taskId")
            if (taskId != null) {
                val task = dayManagementRepository.getTaskById(taskId)
                val recurringTask = task?.recurringTaskId?.let { dayManagementRepository.getRecurringTask(it) }
                _uiState.value = EditTaskUiState(
                    task = task,
                    title = task?.title ?: "",
                    description = task?.description ?: "",
                    priority = task?.priority ?: TaskPriority.NONE,
                    duration = task?.estimatedDurationMinutes,
                    points = task?.points ?: 0,
                    isRecurring = task?.recurringTaskId != null,
                    recurrenceRule = recurringTask?.recurrenceRule,
                    recurrenceFrequency = recurringTask?.recurrenceRule?.frequency ?: RecurrenceFrequency.DAILY,
                    recurrenceInterval = recurringTask?.recurrenceRule?.interval ?: 1,
                    recurrenceDaysOfWeek = recurringTask?.recurrenceRule?.daysOfWeek?.toSet() ?: emptySet(),
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onPriorityChange(priority: TaskPriority) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun onDurationChange(duration: Long?) {
        _uiState.value = _uiState.value.copy(duration = duration)
    }

    fun onPointsChange(points: Int) {
        _uiState.value = _uiState.value.copy(points = points)
    }

    fun onRecurringChange(isRecurring: Boolean) {
        _uiState.value = _uiState.value.copy(isRecurring = isRecurring)
    }

    fun onRecurrenceFrequencyChange(frequency: RecurrenceFrequency) {
        _uiState.value = _uiState.value.copy(recurrenceFrequency = frequency)
    }

    fun onRecurrenceIntervalChange(interval: Int) {
        _uiState.value = _uiState.value.copy(recurrenceInterval = interval)
    }

    fun onRecurrenceDayOfWeekToggle(day: DayOfWeek) {
        val days = _uiState.value.recurrenceDaysOfWeek.toMutableSet()
        if (days.contains(day)) {
            days.remove(day)
        } else {
            days.add(day)
        }
        _uiState.value = _uiState.value.copy(recurrenceDaysOfWeek = days)
    }

    fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value
            val originalTask = state.task ?: return@launch

            val wasRecurring = originalTask.recurringTaskId != null
            val isRecurring = state.isRecurring

            if (isRecurring) {
                val recurrenceRule = RecurrenceRule(
                    frequency = state.recurrenceFrequency,
                    interval = state.recurrenceInterval,
                    daysOfWeek = if (state.recurrenceFrequency == RecurrenceFrequency.WEEKLY) state.recurrenceDaysOfWeek.toList() else null
                )
                if (wasRecurring) {
                    // Recurring -> Recurring: Update existing recurring task
                    dayManagementRepository.updateRecurringTaskTemplate(
                        recurringTaskId = originalTask.recurringTaskId!!,
                        title = state.title,
                        description = state.description,
                        priority = state.priority,
                        duration = state.duration
                    )
                    // Also update the current instance
                    dayManagementRepository.updateTask(
                        taskId = originalTask.id,
                        title = state.title,
                        description = state.description,
                        priority = state.priority,
                        duration = state.duration,
                        points = state.points
                    )
                } else {
                    // Non-recurring -> Recurring: Create new recurring task and link it
                    dayManagementRepository.addRecurringTask(
                        title = state.title,
                        description = state.description,
                        duration = state.duration,
                        priority = state.priority,
                        recurrenceRule = recurrenceRule,
                        dayPlanId = originalTask.dayPlanId,
                        goalId = originalTask.goalId,
                        projectId = originalTask.projectId,
                        taskType = originalTask.taskType,
                        points = state.points,
                    )
                    // The old simple task should be deleted
                    dayManagementRepository.deleteTask(originalTask.id)
                }
            } else { // Not recurring
                if (wasRecurring) {
                    // Recurring -> Non-recurring: Detach and update
                    dayManagementRepository.detachFromRecurrence(originalTask.id)
                    dayManagementRepository.updateTask(
                        taskId = originalTask.id,
                        title = state.title,
                        description = state.description,
                        priority = state.priority,
                        duration = state.duration,
                        points = state.points
                    )
                } else {
                    // Non-recurring -> Non-recurring: Just update
                    dayManagementRepository.updateTask(
                        taskId = originalTask.id,
                        title = state.title,
                        description = state.description,
                        priority = state.priority,
                        duration = state.duration,
                        points = state.points
                    )
                }
            }
            _uiEvent.send(EditTaskUiEvent.NavigateUp)
        }
    }
}
