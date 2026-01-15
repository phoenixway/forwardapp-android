package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.data.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.data.database.models.NewTaskParameters
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

import com.romankozak.forwardappmobile.data.database.models.Reminder

data class ParentInfo(
    val id: String, // This is the ID of the goal or project
    val title: String,
    val type: ParentType,
    val projectId: String? = null // Add this for goals
)

enum class ParentType {
    GOAL, PROJECT
}

data class DayTaskWithReminder(
    val dayTask: DayTask,
    val reminder: Reminder?,
    val parentInfo: ParentInfo? = null
)

data class DayPlanUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTaskWithReminder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val isReordering: Boolean = false,
    val isToday: Boolean = true,
    val bestCompletedPoints: Int = 0,
)

sealed class DayPlanUiEvent {
    data class NavigateToEditTask(val taskId: String) : DayPlanUiEvent()
}

enum class EditingMode { SINGLE, ALL_INSTANCES }

@HiltViewModel
class DayPlanViewModel
@Inject
constructor(
    private val dayManagementRepository: DayManagementRepository,
    private val reminderRepository: com.romankozak.forwardappmobile.data.repository.ReminderRepository,
) : ViewModel() {

    init {
        Log.d("DayPlanViewModel", "DayPlanViewModel initialized.")
    }

    private val _planId = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<DayPlanUiState> = _planId
        .filterNotNull()
        .flatMapLatest { planId ->
            Log.d("DayPlanViewModel", "flatMapLatest: Processing planId: $planId")
            val dayPlanFlow = dayManagementRepository.getPlanByIdStream(planId)
                .onEach { dayPlan ->
                    Log.d("DayPlanViewModel", "dayPlanFlow: Received dayPlan: ${dayPlan?.id} for planId: $planId")
                    dayPlan?.let { dayManagementRepository.generateRecurringTasksForDate(it.date) }
                }

            val tasksFlow = dayManagementRepository.getTasksForDay(planId)
                .flatMapLatest { tasks ->
                    Log.d("DayPlanViewModel", "tasksFlow: Received ${tasks.size} tasks for planId: $planId")
                    if (tasks.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(tasks.map { task ->
                            val reminderFlow = reminderRepository.getRemindersForEntityFlow(task.id)
                            val parentInfoFlow: Flow<ParentInfo?> = if (task.goalId != null) {
                                flow {
                                    val goal = dayManagementRepository.getGoal(task.goalId!!)
                                    val projectId = goal?.let { dayManagementRepository.findProjectIdForGoal(it.id) }
                                    emit(goal?.let { ParentInfo(it.id, it.text, ParentType.GOAL, projectId) })
                                }
                            } else if (task.projectId != null) {
                                flow { emit(dayManagementRepository.getProject(task.projectId!!)) }.map { project ->
                                    project?.let { ParentInfo(it.id, it.name, ParentType.PROJECT, it.id) }
                                }
                            } else {
                                flowOf(null)
                            }
                            combine(reminderFlow, parentInfoFlow) { reminders, parentInfo ->
                                DayTaskWithReminder(task, reminders.firstOrNull(), parentInfo)
                            }
                        }) { it.toList() }
                    }
                }

            combine(dayPlanFlow, tasksFlow) { dayPlan, tasks ->
                Log.d("DayPlanViewModel", "UI State combine: dayPlanId=${dayPlan?.id}, tasksCount=${tasks.size} (before creating DayPlanUiState)")
                val highestCompletedPoints = dayManagementRepository.getHighestCompletedPointsAcrossPlans()
                DayPlanUiState(
                    dayPlan = dayPlan,
                    tasks = sortTasksWithOrder(tasks),
                    isLoading = false,
                    isRefreshing = false,
                    isToday = dayPlan?.let { isTimestampToday(it.date) } ?: true,
                    bestCompletedPoints = highestCompletedPoints,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
        .catch { e ->
            Log.e("DayPlanViewModel", "Error in uiState flow", e)
            emit(DayPlanUiState(isLoading = false, error = "Помилка завантаження даних: ${e.localizedMessage}"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DayPlanUiState(isLoading = true)
        )
    
    private val _isAddTaskDialogOpen = MutableStateFlow(false)
    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    private val _selectedTask = MutableStateFlow<DayTaskWithReminder?>(null)
    val selectedTask: StateFlow<DayTaskWithReminder?> = _selectedTask.asStateFlow()

    private val _showDeleteConfirmationDialog = MutableStateFlow<DayTaskWithReminder?>(null)
    val showDeleteConfirmationDialog: StateFlow<DayTaskWithReminder?> = _showDeleteConfirmationDialog.asStateFlow()

    private val _showEditConfirmationDialog = MutableStateFlow<DayTaskWithReminder?>(null)
    val showEditConfirmationDialog: StateFlow<DayTaskWithReminder?> = _showEditConfirmationDialog.asStateFlow()

    private var editingMode: EditingMode = EditingMode.SINGLE

    private val _uiEvent = Channel<DayPlanUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    fun loadDataForPlan(dayPlanId: String) {
        Log.d("DayPlanViewModel", "Loading data for plan: $dayPlanId")
        _planId.value = dayPlanId
    }

    fun openAddTaskDialog() {
        _isAddTaskDialogOpen.value = true
    }

    fun dismissAddTaskDialog() {
        _isAddTaskDialogOpen.value = false
    }

    fun onTaskLongPressed(taskWithReminder: DayTaskWithReminder) {
        _selectedTask.value = taskWithReminder
    }

    fun selectTask(taskWithReminder: DayTaskWithReminder) {
        _selectedTask.value = taskWithReminder
    }

    fun clearSelectedTask() {
        _selectedTask.value = null
    }

    fun dismissError() {
        // This is now handled by the main flow, but can be used for one-time events if needed
    }
    
    private fun isTimestampToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        calendar.timeInMillis = timestamp
        val otherDay = calendar.get(Calendar.DAY_OF_YEAR)
        val otherYear = calendar.get(Calendar.YEAR)
        return today == otherDay && year == otherYear
    }

    fun updateTasksOrder(dayPlanId: String, reorderedTasks: List<DayTaskWithReminder>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tasksForRepo = reorderedTasks.mapIndexed { index, taskWithReminder ->
                    taskWithReminder.dayTask.copy(order = index.toLong())
                }
                dayManagementRepository.updateTasksOrder(dayPlanId, tasksForRepo)
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error updating task order", e)
            }
        }
    }

    fun addTask(
        dayPlanId: String,
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority,
        recurrenceRule: RecurrenceRule?,
        points: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmedTitle = title.trim()
                if (trimmedTitle.isEmpty() || trimmedTitle.length > 100) {
                    // TODO: Handle validation error with a one-time event (e.g., a Channel)
                    return@launch
                }

                if (recurrenceRule != null) {
                    dayManagementRepository.addRecurringTask(
                        title = trimmedTitle,
                        description = description.trim().takeIf { it.isNotEmpty() },
                        duration = duration,
                        priority = priority,
                        recurrenceRule = recurrenceRule,
                        dayPlanId = dayPlanId,
                        points = points,
                    )
                } else {
                    val currentTasks = uiState.value.tasks
                    val maxOrder = currentTasks.maxOfOrNull { it.dayTask.order } ?: 0L
                    dayManagementRepository.addTaskToDayPlan(
                        NewTaskParameters(
                            dayPlanId = dayPlanId,
                            title = trimmedTitle,
                            description = description.trim().takeIf { it.isNotEmpty() },
                            estimatedDurationMinutes = duration?.takeIf { it > 0 && it <= 1440 },
                            priority = priority,
                            order = maxOrder + 1,
                            points = points,
                        ),
                    )
                }
                dismissAddTaskDialog()
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error adding task", e)
            }
        }
    }

    fun addGoalAsRecurringTask(goalId: String, dayPlanId: String, recurrenceRule: RecurrenceRule) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val goal = dayManagementRepository.getGoal(goalId) ?: return@launch
                val projectId = dayManagementRepository.findProjectIdForGoal(goalId)
                dayManagementRepository.addRecurringTask(
                    title = goal.text,
                    description = goal.description,
                    duration = null,
                    priority = TaskPriority.MEDIUM,
                    recurrenceRule = recurrenceRule,
                    dayPlanId = dayPlanId,
                    goalId = goalId,
                    projectId = projectId,
                )
            } catch (e: Exception) {
                 Log.e("DayPlanViewModel", "Error adding goal as recurring task", e)
            }
        }
    }

    fun onEditTaskClicked(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch {
            _uiEvent.send(DayPlanUiEvent.NavigateToEditTask(taskWithReminder.dayTask.id))
        }
    }

    fun dismissEditConfirmationDialog() {
        _showEditConfirmationDialog.value = null
    }

    fun editSingleInstanceOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            dayManagementRepository.detachFromRecurrence(taskWithReminder.dayTask.id)
            dismissEditConfirmationDialog()
            editingMode = EditingMode.SINGLE
            openEditTaskDialog()
        }
    }

    fun editAllFutureInstancesOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
        dismissEditConfirmationDialog()
        editingMode = EditingMode.ALL_INSTANCES
        openEditTaskDialog()
    }

    fun onDeleteTaskClicked(taskWithReminder: DayTaskWithReminder) {
        if (taskWithReminder.dayTask.recurringTaskId != null) {
            _showDeleteConfirmationDialog.value = taskWithReminder
        } else {
            deleteTask(taskWithReminder.dayTask.id)
        }
    }

    fun dismissDeleteConfirmationDialog() {
        _showDeleteConfirmationDialog.value = null
    }

    fun deleteSingleInstanceOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            dayManagementRepository.deleteTask(taskWithReminder.dayTask.id)
            dismissDeleteConfirmationDialog()
        }
    }

    fun deleteAllFutureInstancesOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            taskWithReminder.dayTask.recurringTaskId?.let {
                dayManagementRepository.deleteAllFutureInstancesOfRecurringTask(it, taskWithReminder.dayTask.dayPlanId)
            }
            dismissDeleteConfirmationDialog()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                dayManagementRepository.deleteTask(taskId)
                clearSelectedTask()
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error deleting task", e)
            }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val task = uiState.value.tasks.find { it.dayTask.id == taskId }?.dayTask ?: return@launch
            try {
                if (task.recurringTaskId != null) {
                    val recurringTask = dayManagementRepository.getRecurringTask(task.recurringTaskId)
                    if (recurringTask?.recurrenceRule?.frequency == RecurrenceFrequency.HOURLY) {
                        val intervalMillis = recurringTask.recurrenceRule.interval * 60 * 60 * 1000
                        val nextOccurrence = System.currentTimeMillis() + intervalMillis
                        dayManagementRepository.updateTaskNextOccurrence(taskId, nextOccurrence)
                        return@launch
                    }
                }
                dayManagementRepository.toggleTaskCompletion(taskId)
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error toggling task completion", e)
            }
        }
    }
    
    fun refreshPlan() {
        _planId.value?.let { planId ->
            // Re-emitting the same value will trigger a refresh in flatMapLatest
            _planId.value = null 
            _planId.value = planId
        }
    }
    
    private fun sortTasksWithOrder(tasks: List<DayTaskWithReminder>): List<DayTaskWithReminder> {
        return tasks.sortedWith(
            compareBy<DayTaskWithReminder> { it.dayTask.completed }
                .thenBy { it.dayTask.order }
                .thenBy { it.dayTask.title.lowercase() },
        )
    }

    private fun sortTasks(tasks: List<DayTaskWithReminder>): List<DayTaskWithReminder> {
        return tasks.sortedWith(
            compareBy<DayTaskWithReminder> { it.dayTask.completed }
                .thenBy { task ->
                    when (task.dayTask.priority) {
                        TaskPriority.CRITICAL -> 0
                        TaskPriority.HIGH -> 1
                        TaskPriority.MEDIUM -> 2
                        TaskPriority.LOW -> 3
                        TaskPriority.NONE -> 4
                    }
                }
                .thenBy { it.dayTask.dueTime ?: Long.MAX_VALUE }
                .thenBy { it.dayTask.title.lowercase() },
        )
    }
    
    fun hasOverdueTasks(): Boolean {
        val currentTime = System.currentTimeMillis()
        return uiState.value.tasks.any { taskWithReminder ->
            !taskWithReminder.dayTask.completed && taskWithReminder.dayTask.dueTime != null && taskWithReminder.dayTask.dueTime < currentTime
        }
    }
    
    fun getCompletionStats(): Triple<Int, Int, Float> {
        val tasks = uiState.value.tasks
        val completed = tasks.count { it.dayTask.completed }
        val total = tasks.size
        val percentage = if (total > 0) completed.toFloat() / total else 0f
        return Triple(completed, total, percentage)
    }

    fun sortTasksByPriority(dayPlanId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = uiState.value.tasks
                val sortedTasks = sortTasks(currentTasks)
                val tasksWithNewOrder = sortedTasks.mapIndexed { index, taskWithReminder ->
                    taskWithReminder.dayTask.copy(order = index.toLong())
                }
                dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error sorting tasks by priority", e)
            }
        }
    }
    
    fun navigateToPreviousDay() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value.dayPlan?.date?.let { currentDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val previousDate = calendar.timeInMillis
                Log.d("DayPlanViewModel", "Navigating to previous day. CurrentDate: $currentDate, PreviousDate: $previousDate")
                try {
                    val prevDayPlan = dayManagementRepository.createOrUpdateDayPlan(previousDate)
                    Log.d("DayPlanViewModel", "Previous day plan created/updated. ID: ${prevDayPlan.id}")
                    loadDataForPlan(prevDayPlan.id)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error navigating to previous day", e)
                }
            } ?: run {
                Log.w("DayPlanViewModel", "navigateToPreviousDay: dayPlan is null, cannot navigate.")
            }
        }
    }
    
    fun navigateToNextDay() {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.isToday) {
                Log.d("DayPlanViewModel", "navigateToNextDay: Currently on today's plan, cannot navigate forward.")
                return@launch
            }
            uiState.value.dayPlan?.date?.let { currentDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val nextDate = calendar.timeInMillis
                Log.d("DayPlanViewModel", "Navigating to next day. CurrentDate: $currentDate, NextDate: $nextDate")
                try {
                    val nextDayPlan = dayManagementRepository.createOrUpdateDayPlan(nextDate)
                    Log.d("DayPlanViewModel", "Next day plan created/updated. ID: ${nextDayPlan.id}")
                    loadDataForPlan(nextDayPlan.id)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error navigating to next day", e)
                }
            } ?: run {
                Log.w("DayPlanViewModel", "navigateToNextDay: dayPlan is null, cannot navigate.")
            }
        }
    }
    
    fun copyTaskToTodaysPlan(taskToCopyWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.copyTaskToTodaysPlan(taskToCopyWithReminder.dayTask)
                clearSelectedTask()
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error copying task", e)
            }
        }
    }

    fun moveTaskToTomorrow(taskToMoveWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.moveTaskToTomorrow(taskToMoveWithReminder.dayTask)
                clearSelectedTask()
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error moving task to tomorrow", e)
            }
        }
    }

    private val _isEditTaskDialogOpen = MutableStateFlow(false)
    val isEditTaskDialogOpen: StateFlow<Boolean> = _isEditTaskDialogOpen.asStateFlow()

    fun openEditTaskDialog() {
        _isEditTaskDialogOpen.value = true
    }

    fun dismissEditTaskDialog() {
        _isEditTaskDialogOpen.value = false
    }

    fun updateTask(
        taskId: String,
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority,
        points: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val taskWithReminder = selectedTask.value ?: return@launch
            val task = taskWithReminder.dayTask
            try {
                if (editingMode == EditingMode.ALL_INSTANCES && task.recurringTaskId != null) {
                    dayManagementRepository.splitRecurringTask(
                        originalTask = task,
                        newTitle = title,
                        newDescription = description,
                        newPriority = priority,
                        newDuration = duration,
                        points = task.points
                    )
                } else {
                    dayManagementRepository.updateTask(taskId, title, description, priority, duration, points)
                }
                dismissEditTaskDialog()
                clearSelectedTask()
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error updating task", e)
            }
        }
    }
    
    fun setTaskReminder(
        taskId: String,
        reminderTime: Long,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.createReminder(taskId, "TASK", reminderTime)
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error setting reminder", e)
            }
        }
    }
    
    fun clearTaskReminder(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.clearRemindersForEntity(taskId)
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error clearing reminder", e)
            }
        }
    }

    fun moveTaskToTop(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = uiState.value.tasks.toMutableList()
                currentTasks.remove(taskWithReminder)
                currentTasks.add(0, taskWithReminder.copy(dayTask = taskWithReminder.dayTask.copy(order = 0)))

                val tasksForRepo =
                    currentTasks.mapIndexed { index, tWithR ->
                        tWithR.dayTask.copy(order = index.toLong())
                    }

                dayManagementRepository.updateTasksOrder(taskWithReminder.dayTask.dayPlanId, tasksForRepo)
                clearSelectedTask()
            } catch (e: Exception) {
                Log.e("DayPlanViewModel", "Error moving task to top", e)
            }
        }
    }
}
