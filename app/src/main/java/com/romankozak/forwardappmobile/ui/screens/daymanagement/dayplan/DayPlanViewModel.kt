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

data class DayPlanUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val isReordering: Boolean = false,
    val isToday: Boolean = true,
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(DayPlanUiState())
    val uiState: StateFlow<DayPlanUiState> = _uiState.asStateFlow()

    private val _isAddTaskDialogOpen = MutableStateFlow(false)
    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    private val _selectedTask = MutableStateFlow<DayTask?>(null)
    val selectedTask: StateFlow<DayTask?> = _selectedTask.asStateFlow()

    private val _showDeleteConfirmationDialog = MutableStateFlow<DayTask?>(null)
    val showDeleteConfirmationDialog: StateFlow<DayTask?> = _showDeleteConfirmationDialog.asStateFlow()

    private val _showEditConfirmationDialog = MutableStateFlow<DayTask?>(null)
    val showEditConfirmationDialog: StateFlow<DayTask?> = _showEditConfirmationDialog.asStateFlow()

    private var editingMode: EditingMode = EditingMode.SINGLE

    
    private var currentPlanId: String? = null

    private val _tasksUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tasksUpdated: SharedFlow<Unit> = _tasksUpdated.asSharedFlow()

    private val _uiEvent = Channel<DayPlanUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun openAddTaskDialog() {
        _isAddTaskDialogOpen.value = true
    }

    fun dismissAddTaskDialog() {
        _isAddTaskDialogOpen.value = false
    }

    fun onTaskLongPressed(task: DayTask) {
        _selectedTask.value = task
    }

    fun selectTask(task: DayTask) {
        _selectedTask.value = task
    }

    fun clearSelectedTask() {
        _selectedTask.value = null
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
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

    fun updateTasksOrder(
        dayPlanId: String,
        reorderedTasks: List<DayTask>,
    ) {
        
        val tasksWithNewOrder =
            reorderedTasks.mapIndexed { index, task ->
                task.copy(order = index.toLong())
            }

        _uiState.update { currentState ->
            currentState.copy(
                tasks = tasksWithNewOrder,
                isReordering = true,
            )
        }

        
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)
                
                _uiState.update { it.copy(isReordering = false) }
            } catch (e: Exception) {
                
                
                _uiState.update { it.copy(error = "Помилка при зміні порядку завдань") }
                loadDataForPlan(dayPlanId)
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
                val trimmedDescription = description.trim().takeIf { it.isNotEmpty() }

                if (trimmedTitle.isEmpty() || trimmedTitle.length > 100) {
                    _uiState.update {
                        it.copy(error = "Назва завдання повинна містити від 1 до 100 символів")
                    }
                    return@launch
                }

                if (recurrenceRule != null) {
                    Firebase.crashlytics.log("Adding recurring task: ${trimmedTitle}")
                    dayManagementRepository.addRecurringTask(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        duration = duration,
                        priority = priority,
                        recurrenceRule = recurrenceRule,
                        dayPlanId = dayPlanId
                    )
                } else {
                    Firebase.crashlytics.log("Adding single task: ${trimmedTitle}")
                    val currentTasks = _uiState.value.tasks
                    val maxOrder = currentTasks.maxOfOrNull { it.order } ?: 0L
                    dayManagementRepository.addTaskToDayPlan(
                        NewTaskParameters(
                            dayPlanId = dayPlanId,
                            title = trimmedTitle,
                            description = trimmedDescription,
                            estimatedDurationMinutes = duration?.takeIf { it > 0 && it <= 1440 },
                            priority = priority,
                            order = maxOrder + 1,
                            points = points,
                        ),
                    )
                }

                
                dismissAddTaskDialog()
                loadDataForPlan(dayPlanId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Помилка при додаванні завдання: ${e.localizedMessage}",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun addGoalAsRecurringTask(goalId: String, dayPlanId: String, recurrenceRule: RecurrenceRule) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val goal = dayManagementRepository.getGoal(goalId) ?: return@launch
                dayManagementRepository.addRecurringTask(
                    title = goal.text,
                    description = goal.description,
                    duration = null, // Goals don't have duration
                    priority = TaskPriority.MEDIUM, // Or map from goal importance
                    recurrenceRule = recurrenceRule,
                    dayPlanId = dayPlanId,
                    goalId = goalId
                )
            } catch (e: Exception) {
                 _uiState.update {
                    it.copy(
                        error = "Помилка при додаванні повторюваної цілі: ${e.localizedMessage}",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onEditTaskClicked(task: DayTask) {
        viewModelScope.launch {
            _uiEvent.send(DayPlanUiEvent.NavigateToEditTask(task.id))
        }
    }

    fun dismissEditConfirmationDialog() {
        _showEditConfirmationDialog.value = null
    }

    fun editSingleInstanceOfRecurringTask(task: DayTask) {
        viewModelScope.launch(Dispatchers.IO) {
            dayManagementRepository.detachFromRecurrence(task.id)
            dismissEditConfirmationDialog()
            editingMode = EditingMode.SINGLE
            openEditTaskDialog()
        }
    }

    fun editAllFutureInstancesOfRecurringTask(task: DayTask) {
        dismissEditConfirmationDialog()
        editingMode = EditingMode.ALL_INSTANCES
        openEditTaskDialog()
    }

    fun onDeleteTaskClicked(task: DayTask) {
        val TAG = "DELETE_RECURRING_DEBUG"
        Log.d(TAG, "onDeleteTaskClicked called for task: ${task.title}")
        Log.d(TAG, "Task recurringTaskId: ${task.recurringTaskId}")
        if (task.recurringTaskId != null) {
            Log.d(TAG, "recurringTaskId is not null, showing confirmation dialog")
            _showDeleteConfirmationDialog.value = task
        } else {
            Log.d(TAG, "recurringTaskId is null, deleting single task")
            deleteTask(task.dayPlanId, task.id)
        }
    }

    fun dismissDeleteConfirmationDialog() {
        _showDeleteConfirmationDialog.value = null
    }

    fun deleteSingleInstanceOfRecurringTask(task: DayTask) {
        viewModelScope.launch(Dispatchers.IO) {
            dayManagementRepository.deleteTask(task.id)
            _tasksUpdated.tryEmit(Unit)
            dismissDeleteConfirmationDialog()
        }
    }

    fun deleteAllFutureInstancesOfRecurringTask(task: DayTask) {
        val TAG = "DELETE_RECURRING_DEBUG"
        Log.d(TAG, "ViewModel.deleteAllFutureInstancesOfRecurringTask called for task: ${task.title}")
        Log.d(TAG, "Task recurringTaskId: ${task.recurringTaskId}")
        viewModelScope.launch(Dispatchers.IO) {
            task.recurringTaskId?.let {
                Log.d(TAG, "recurringTaskId is not null, calling repository")
                dayManagementRepository.deleteAllFutureInstancesOfRecurringTask(it, task.dayPlanId)
                _tasksUpdated.tryEmit(Unit)
            }
            dismissDeleteConfirmationDialog()
        }
    }

    fun deleteTask(
        dayPlanId: String,
        taskId: String,
    ) {
        Firebase.crashlytics.log("Deleting task with id: $taskId from plan: $dayPlanId")
        viewModelScope.launch {
            try {
                dayManagementRepository.deleteTask(taskId)
                clearSelectedTask()
                _tasksUpdated.tryEmit(Unit)

                _uiState.update {
                    it.copy(lastUpdated = System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Помилка при видаленні завдання: ${e.localizedMessage}")
                }
                e.printStackTrace()
            }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val task = _uiState.value.tasks.find { it.id == taskId } ?: return@launch

            if (task.recurringTaskId != null) {
                val recurringTask = dayManagementRepository.getRecurringTask(task.recurringTaskId)
                if (recurringTask?.recurrenceRule?.frequency == RecurrenceFrequency.HOURLY) {
                    val intervalMillis = recurringTask.recurrenceRule.interval * 60 * 60 * 1000
                    val nextOccurrence = System.currentTimeMillis() + intervalMillis
                    dayManagementRepository.updateTaskNextOccurrence(taskId, nextOccurrence)
                    loadDataForPlan(task.dayPlanId) // Refresh data
                    return@launch
                }
            }

            try {
                
                _uiState.update { currentState ->
                    val updatedTasks =
                        currentState.tasks.map { t ->
                            if (t.id == taskId) {
                                t.copy(completed = !t.completed)
                            } else {
                                t
                            }
                        }
                    currentState.copy(
                        tasks = sortTasksWithOrder(updatedTasks),
                        lastUpdated = System.currentTimeMillis(),
                    )
                }

                
                
                dayManagementRepository.toggleTaskCompletion(taskId)
            } catch (e: Exception) {
                
                _uiState.update {
                    it.copy(error = "Помилка при оновленні статусу завдання: ${e.localizedMessage}")
                }
                e.printStackTrace()
            }
        }
    }

    
    fun loadDataForPlan(dayPlanId: String) {
        val isRefresh = currentPlanId == dayPlanId

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null,
                )
            }

            try {
                
                launch {
                    dayManagementRepository.getPlanByIdStream(dayPlanId)
                        .catch { e ->
                            _uiState.update {
                                it.copy(error = "Помилка завантаження плану: ${e.localizedMessage}")
                            }
                        }
                        .collect { dayPlan ->
                            dayPlan?.let {
                                dayManagementRepository.generateRecurringTasksForDate(it.date)
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        dayPlan = dayPlan,
                                        
                                        isToday = isTimestampToday(dayPlan.date),
                                    )
                                }
                            }
                        }
                }

                
                launch {
                    dayManagementRepository.getTasksForDay(dayPlanId)
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = "Помилка завантаження задач: ${e.localizedMessage}",
                                )
                            }
                        }
                        .collect { tasks ->
                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    tasks = sortTasksWithOrder(tasks),
                                    lastUpdated = System.currentTimeMillis(),
                                    error = null,
                                )
                            }
                            currentPlanId = dayPlanId
                        }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Помилка: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    
    fun refreshPlan() {
        currentPlanId?.let { planId ->
            loadDataForPlan(planId)
        }
    }

    
    private fun sortTasksWithOrder(tasks: List<DayTask>): List<DayTask> {
        return tasks.sortedWith(
            compareBy<DayTask> { it.completed }
                .thenBy { it.order }
                .thenBy { it.title.lowercase() },
        )
    }

    
    private fun sortTasks(tasks: List<DayTask>): List<DayTask> {
        return tasks.sortedWith(
            compareBy<DayTask> { it.completed }
                .thenBy { task ->
                    
                    when (task.priority) {
                        TaskPriority.CRITICAL -> 0
                        TaskPriority.HIGH -> 1
                        TaskPriority.MEDIUM -> 2
                        TaskPriority.LOW -> 3
                        TaskPriority.NONE -> 4
                    }
                }
                .thenBy { it.dueTime ?: Long.MAX_VALUE }
                .thenBy { it.title.lowercase() },
        )
    }

    
    fun hasOverdueTasks(): Boolean {
        val currentTime = System.currentTimeMillis()
        return _uiState.value.tasks.any { task ->
            !task.completed && task.dueTime != null && task.dueTime < currentTime
        }
    }

    
    fun getCompletionStats(): Triple<Int, Int, Float> {
        val tasks = _uiState.value.tasks
        val completed = tasks.count { it.completed }
        val total = tasks.size
        val percentage = if (total > 0) completed.toFloat() / total else 0f
        return Triple(completed, total, percentage)
    }

    
    fun sortTasksByPriority(dayPlanId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = _uiState.value.tasks
                val sortedTasks = sortTasks(currentTasks)

                
                val tasksWithNewOrder =
                    sortedTasks.mapIndexed { index, task ->
                        task.copy(order = index.toLong())
                    }

                dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)

                _uiState.update { currentState ->
                    currentState.copy(
                        tasks = tasksWithNewOrder,
                        lastUpdated = System.currentTimeMillis(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Помилка при сортуванні завдань: ${e.localizedMessage}")
                }
                e.printStackTrace()
            }
        }
    }

    
    fun navigateToPreviousDay() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.dayPlan?.date?.let { currentDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val previousDate = calendar.timeInMillis

                try {
                    
                    
                    val prevPlanId = dayManagementRepository.getPlanIdForDate(previousDate)
                    if (prevPlanId != null) {
                        loadDataForPlan(prevPlanId)
                    } else {
                        _uiState.update { it.copy(error = "План на попередній день не знайдено") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Помилка при переході на попередній день") }
                }
            }
        }
    }

    
    fun navigateToNextDay() {
        viewModelScope.launch(Dispatchers.IO) {
            
            if (_uiState.value.isToday) return@launch

            _uiState.value.dayPlan?.date?.let { currentDate ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val nextDate = calendar.timeInMillis

                try {
                    
                    
                    val nextPlanId = dayManagementRepository.getPlanIdForDate(nextDate)
                    if (nextPlanId != null) {
                        loadDataForPlan(nextPlanId)
                    } else {
                        _uiState.update { it.copy(error = "План на наступний день не знайдено") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Помилка при переході на наступний день") }
                }
            }
        }
    }

    
    fun copyTaskToTodaysPlan(taskToCopy: DayTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                
                
                
                dayManagementRepository.copyTaskToTodaysPlan(taskToCopy)
                clearSelectedTask()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка копіювання завдання: ${e.localizedMessage}") }
            }
        }
    }

    fun moveTaskToTomorrow(taskToMove: DayTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.moveTaskToTomorrow(taskToMove)
                clearSelectedTask()
                _tasksUpdated.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка перенесення завдання: ${e.localizedMessage}") }
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
            val task = selectedTask.value ?: return@launch

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
            loadDataForPlan(task.dayPlanId)
        }
    }

    
    // fun setTaskReminder(
    //     taskId: String,
    //     reminderTime: Long,
    // ) {
    //     viewModelScope.launch(Dispatchers.IO) {
    //         try {
    //             dayManagementRepository.setTaskReminder(taskId, reminderTime)
    //         } catch (e: Exception) {
    //             _uiState.update { it.copy(error = "Помилка встановлення нагадування") }
    //         }
    //     }
    // }

    // 
    // fun clearTaskReminder(taskId: String) {
    //     viewModelScope.launch(Dispatchers.IO) {
    //         try {
    //             dayManagementRepository.clearTaskReminder(taskId)
    //         } catch (e: Exception) {
    //             _uiState.update { it.copy(error = "Помилка скасування нагадування") }
    //         }
    //     }
    // }

    fun moveTaskToTop(task: DayTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = _uiState.value.tasks.toMutableList()
                currentTasks.remove(task)
                currentTasks.add(0, task.copy(order = 0))

                val tasksWithNewOrder =
                    currentTasks.mapIndexed { index, t ->
                        t.copy(order = index.toLong())
                    }

                dayManagementRepository.updateTasksOrder(task.dayPlanId, tasksWithNewOrder)

                _uiState.update { currentState ->
                    currentState.copy(
                        tasks = sortTasksWithOrder(tasksWithNewOrder),
                        lastUpdated = System.currentTimeMillis(),
                    )
                }
                clearSelectedTask()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Помилка при переміщенні завдання: ${e.localizedMessage}")
                }
                e.printStackTrace()
            }
        }
    }
}