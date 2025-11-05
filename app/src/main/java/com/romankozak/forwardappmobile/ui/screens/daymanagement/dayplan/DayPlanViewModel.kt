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
    private val _uiState = MutableStateFlow(DayPlanUiState())
    val uiState: StateFlow<DayPlanUiState> = _uiState.asStateFlow()

    private val _isAddTaskDialogOpen = MutableStateFlow(false)
    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    private val _selectedTask = MutableStateFlow<DayTaskWithReminder?>(null)
    val selectedTask: StateFlow<DayTaskWithReminder?> = _selectedTask.asStateFlow()

    private val _showDeleteConfirmationDialog = MutableStateFlow<DayTaskWithReminder?>(null)
    val showDeleteConfirmationDialog: StateFlow<DayTaskWithReminder?> = _showDeleteConfirmationDialog.asStateFlow()

    private val _showEditConfirmationDialog = MutableStateFlow<DayTaskWithReminder?>(null)
    val showEditConfirmationDialog: StateFlow<DayTaskWithReminder?> = _showEditConfirmationDialog.asStateFlow()

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

                reorderedTasks: List<DayTaskWithReminder>,

            ) {

                val tasksForRepo = reorderedTasks.mapIndexed { index, taskWithReminder ->

                    taskWithReminder.dayTask.copy(order = index.toLong())

                }

        

                _uiState.update { currentState ->

                    currentState.copy(

                        tasks = reorderedTasks.mapIndexed { index, taskWithReminder -> taskWithReminder.copy(dayTask = taskWithReminder.dayTask.copy(order = index.toLong())) },

                        isReordering = true,

                    )

                }

        

                viewModelScope.launch(Dispatchers.IO) {

                    try {

                        dayManagementRepository.updateTasksOrder(dayPlanId, tasksForRepo)

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
                        dayPlanId = dayPlanId,
                        points = points,
                    )
                } else {
                    Firebase.crashlytics.log("Adding single task: ${trimmedTitle}")
                    val currentTasks = _uiState.value.tasks
                    val maxOrder = currentTasks.maxOfOrNull { it.dayTask.order } ?: 0L
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
                val projectId = dayManagementRepository.findProjectIdForGoal(goalId)
                dayManagementRepository.addRecurringTask(
                    title = goal.text,
                    description = goal.description,
                    duration = null, // Goals don't have duration
                    priority = TaskPriority.MEDIUM, // Or map from goal importance
                    recurrenceRule = recurrenceRule,
                    dayPlanId = dayPlanId,
                    goalId = goalId,
                    projectId = projectId,
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
        val TAG = "DELETE_RECURRING_DEBUG"
        Log.d(TAG, "onDeleteTaskClicked called for task: ${taskWithReminder.dayTask.title}")
        Log.d(TAG, "Task recurringTaskId: ${taskWithReminder.dayTask.recurringTaskId}")
        if (taskWithReminder.dayTask.recurringTaskId != null) {
            Log.d(TAG, "recurringTaskId is not null, showing confirmation dialog")
            _showDeleteConfirmationDialog.value = taskWithReminder
        } else {
            Log.d(TAG, "recurringTaskId is null, deleting single task")
            deleteTask(taskWithReminder.dayTask.dayPlanId, taskWithReminder.dayTask.id)
        }
    }

    fun dismissDeleteConfirmationDialog() {
        _showDeleteConfirmationDialog.value = null
    }

    fun deleteSingleInstanceOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            dayManagementRepository.deleteTask(taskWithReminder.dayTask.id)
            _tasksUpdated.tryEmit(Unit)
            dismissDeleteConfirmationDialog()
        }
    }

    fun deleteAllFutureInstancesOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
        val TAG = "DELETE_RECURRING_DEBUG"
        Log.d(TAG, "ViewModel.deleteAllFutureInstancesOfRecurringTask called for task: ${taskWithReminder.dayTask.title}")
        Log.d(TAG, "Task recurringTaskId: ${taskWithReminder.dayTask.recurringTaskId}")
        viewModelScope.launch(Dispatchers.IO) {
            taskWithReminder.dayTask.recurringTaskId?.let {
                Log.d(TAG, "recurringTaskId is not null, calling repository")
                dayManagementRepository.deleteAllFutureInstancesOfRecurringTask(it, taskWithReminder.dayTask.dayPlanId)
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
            val taskWithReminder = _uiState.value.tasks.find { it.dayTask.id == taskId } ?: return@launch
            val task = taskWithReminder.dayTask

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
                        currentState.tasks.map { tWithR ->
                            if (tWithR.dayTask.id == taskId) {
                                tWithR.copy(dayTask = tWithR.dayTask.copy(completed = !tWithR.dayTask.completed))
                            } else {
                                tWithR
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
                        .flatMapLatest { tasks ->
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
                                            project?.let { ParentInfo(it.id, it.name, ParentType.PROJECT, it.id) } // Project ID is its own ID
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
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = "Помилка завантаження задач: ${e.localizedMessage}",
                                )
                            }
                        }
                        .collect { tasksWithReminders ->
                            val highestCompletedPoints = dayManagementRepository.getHighestCompletedPointsAcrossPlans()
                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    tasks = sortTasksWithOrder(tasksWithReminders),
                                    bestCompletedPoints = highestCompletedPoints,
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
        return _uiState.value.tasks.any { taskWithReminder ->
            !taskWithReminder.dayTask.completed && taskWithReminder.dayTask.dueTime != null && taskWithReminder.dayTask.dueTime < currentTime
        }
    }

    
    fun getCompletionStats(): Triple<Int, Int, Float> {
        val tasks = _uiState.value.tasks
        val completed = tasks.count { it.dayTask.completed }
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
                    sortedTasks.mapIndexed { index, taskWithReminder ->
                        taskWithReminder.dayTask.copy(order = index.toLong())
                    }

                dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)

                _uiState.update { currentState ->
                    currentState.copy(
                        tasks = sortedTasks.mapIndexed { index, taskWithReminder -> taskWithReminder.copy(dayTask = taskWithReminder.dayTask.copy(order = index.toLong())) },
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

    
    fun copyTaskToTodaysPlan(taskToCopyWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.copyTaskToTodaysPlan(taskToCopyWithReminder.dayTask)
                clearSelectedTask()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка копіювання завдання: ${e.localizedMessage}") }
            }
        }
    }

    fun moveTaskToTomorrow(taskToMoveWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.moveTaskToTomorrow(taskToMoveWithReminder.dayTask)
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
            val taskWithReminder = selectedTask.value ?: return@launch
            val task = taskWithReminder.dayTask

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

    
    fun setTaskReminder(
        taskId: String,
        reminderTime: Long,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.createReminder(taskId, "TASK", reminderTime)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка встановлення нагадування") }
            }
        }
    }

    
    fun clearTaskReminder(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.clearRemindersForEntity(taskId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка скасування нагадування") }
            }
        }
    }

    fun moveTaskToTop(taskWithReminder: DayTaskWithReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = _uiState.value.tasks.toMutableList()
                currentTasks.remove(taskWithReminder)
                currentTasks.add(0, taskWithReminder.copy(dayTask = taskWithReminder.dayTask.copy(order = 0)))

                val tasksForRepo =
                    currentTasks.mapIndexed { index, tWithR ->
                        tWithR.dayTask.copy(order = index.toLong())
                    }

                dayManagementRepository.updateTasksOrder(taskWithReminder.dayTask.dayPlanId, tasksForRepo)

                _uiState.update { currentState ->
                    currentState.copy(
                        tasks = sortTasksWithOrder(currentTasks),
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
