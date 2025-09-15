// DayPlanViewModel.kt - Updated with reordering support
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.NewTaskParameters
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayPlanUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val isReordering: Boolean = false
)

@HiltViewModel
class DayPlanViewModel @Inject constructor(
    private val dayManagementRepository: DayManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayPlanUiState())
    val uiState: StateFlow<DayPlanUiState> = _uiState.asStateFlow()

    private val _isAddTaskDialogOpen = MutableStateFlow(false)
    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    private val _selectedTask = MutableStateFlow<DayTask?>(null)
    val selectedTask: StateFlow<DayTask?> = _selectedTask.asStateFlow()

    // Кешуємо останній завантажений план для швидкого доступу
    private var currentPlanId: String? = null

    private val _tasksUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tasksUpdated: SharedFlow<Unit> = _tasksUpdated.asSharedFlow()


    fun openAddTaskDialog() {
        _isAddTaskDialogOpen.value = true
    }

    fun dismissAddTaskDialog() {
        _isAddTaskDialogOpen.value = false
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

    fun updateTasksOrder(dayPlanId: String, reorderedTasks: List<DayTask>) {
        // 1. Спочатку оновлюємо локальний стан UI негайно.
        val tasksWithNewOrder = reorderedTasks.mapIndexed { index, task ->
            task.copy(order = index.toLong())
        }

        _uiState.update { currentState ->
            currentState.copy(
                tasks = tasksWithNewOrder,
                isReordering = true
            )
        }

        // 2. Тепер запускаємо асинхронну операцію збереження.
        // Це робить ViewModel єдиним джерелом правди.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)
                // При успіху оновлюємо стан, щоб вимкнути індикатор, якщо потрібно
                _uiState.update { it.copy(isReordering = false) }
            } catch (e: Exception) {
                // При помилці - завантажуємо актуальні дані з репозиторію
                // для повернення до останнього робочого стану.
                _uiState.update { it.copy(error = "Помилка при зміні порядку завдань") }
                loadDataForPlan(dayPlanId) // Завантажуємо свіжі дані
            }
        }
    }
    fun addTask(
        dayPlanId: String,
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority
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

                val currentTasks = _uiState.value.tasks
                val maxOrder = currentTasks.maxOfOrNull { it.order } ?: 0L

                // 1. Створюємо новий об'єкт завдання
                val newTask = DayTask(
                    id = java.util.UUID.randomUUID().toString(), // Генеруємо унікальний ID
                    dayPlanId = dayPlanId,
                    title = trimmedTitle,
                    description = trimmedDescription,
                    priority = priority,
                    order = maxOrder + 1,
                    completed = false,
                    createdAt = System.currentTimeMillis()
                )

                // 2. Оновлюємо локальний стан UI, додаючи нове завдання
                _uiState.update { currentState ->
                    currentState.copy(
                        tasks = sortTasksWithOrder(currentState.tasks + newTask)
                    )
                }

                // 3. Зберігаємо завдання в репозиторії
                // Цей виклик тепер асинхронний і не блокує UI
                dayManagementRepository.addTaskToDayPlan(
                    NewTaskParameters(
                        dayPlanId = dayPlanId,
                        title = trimmedTitle,
                        description = trimmedDescription,
                        estimatedDurationMinutes = duration?.takeIf { it > 0 && it <= 1440 },
                        priority = priority,
                        order = newTask.order
                    )
                )

                // 4. Закриваємо діалог
                dismissAddTaskDialog()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Помилка при додаванні завдання: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteTask(dayPlanId: String, taskId: String) {
        viewModelScope.launch {
            try {
                dayManagementRepository.deleteTask(taskId)
                clearSelectedTask()

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

    /**
     * Перемикає статус виконання завдання
     */
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            try {
                dayManagementRepository.toggleTaskCompletion(taskId)

                // Оновлюємо локальний стан для миттєвого відгуку UI
                _uiState.update { currentState ->
                    val updatedTasks = currentState.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(completed = !task.completed)
                        } else {
                            task
                        }
                    }
                    currentState.copy(
                        tasks = sortTasksWithOrder(updatedTasks),
                        lastUpdated = System.currentTimeMillis()
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Помилка при оновленні статусу завдання: ${e.localizedMessage}")
                }
                e.printStackTrace()
            }
        }
    }

    /**
     * Завантажує дані для конкретного плану дня
     */
    fun loadDataForPlan(dayPlanId: String) {
        val isRefresh = currentPlanId == dayPlanId

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null
                )
            }

            try {
                // План
                launch {
                    dayManagementRepository.getPlanByIdStream(dayPlanId)
                        .catch { e ->
                            _uiState.update {
                                it.copy(error = "Помилка завантаження плану: ${e.localizedMessage}")
                            }
                        }
                        .collect { dayPlan ->
                            dayPlan?.let {
                                _uiState.update { currentState ->
                                    currentState.copy(dayPlan = dayPlan)
                                }
                            }
                        }
                }

                // Завдання
                launch {
                    dayManagementRepository.getTasksForDay(dayPlanId)
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = "Помилка завантаження задач: ${e.localizedMessage}"
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
                                    error = null
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
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Оновлює дані плану
     */
    fun refreshPlan() {
        currentPlanId?.let { planId ->
            loadDataForPlan(planId)
        }
    }

    /**
     * Сортує завдання з урахуванням порядку, встановленого користувачем
     * Незавершені завдання сортуються за порядком, потім завершені
     */
    private fun sortTasksWithOrder(tasks: List<DayTask>): List<DayTask> {
        return tasks.sortedWith(
            compareBy<DayTask> { it.completed }  // Спочатку незавершені
                .thenBy { it.order }             // Потім за порядком
                .thenBy { it.title.lowercase() } // І за назвою як fallback
        )
    }

    /**
     * Сортує завдання: спочатку невиконані за пріоритетом і терміном, потім виконані
     * Використовується коли потрібно застосувати стандартне сортування без врахування order
     */
    private fun sortTasks(tasks: List<DayTask>): List<DayTask> {
        return tasks.sortedWith(
            compareBy<DayTask> { it.completed }
                .thenBy { task ->
                    // Сортування за пріоритетом (вищий пріоритет - менше число)
                    when (task.priority) {
                        TaskPriority.CRITICAL -> 0
                        TaskPriority.HIGH -> 1
                        TaskPriority.MEDIUM -> 2
                        TaskPriority.LOW -> 3
                        TaskPriority.NONE -> 4
                    }
                }
                .thenBy { it.dueTime ?: Long.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
    }

    /**
     * Перевіряє чи є прострочені завдання
     */
    fun hasOverdueTasks(): Boolean {
        val currentTime = System.currentTimeMillis()
        return _uiState.value.tasks.any { task ->
            !task.completed && task.dueTime != null && task.dueTime < currentTime
        }
    }

    /**
     * Отримує статистику виконання завдань
     */
    fun getCompletionStats(): Triple<Int, Int, Float> {
        val tasks = _uiState.value.tasks
        val completed = tasks.count { it.completed }
        val total = tasks.size
        val percentage = if (total > 0) completed.toFloat() / total else 0f
        return Triple(completed, total, percentage)
    }

    /**
     * Автоматично сортує завдання за пріоритетом (скидає користувацький порядок)
     */
    fun sortTasksByPriority(dayPlanId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = _uiState.value.tasks
                val sortedTasks = sortTasks(currentTasks)

                // Присвоюємо нові порядкові номери після сортування
                val tasksWithNewOrder = sortedTasks.mapIndexed { index, task ->
                    task.copy(order = index.toLong())
                }

                dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)

                _uiState.update { currentState ->
                    currentState.copy(
                        tasks = tasksWithNewOrder,
                        lastUpdated = System.currentTimeMillis()
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
}