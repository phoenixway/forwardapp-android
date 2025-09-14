package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.NewTaskParameters
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayPlanUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DayPlanViewModel @Inject constructor(
    private val dayManagementRepository: DayManagementRepository
) : ViewModel() {

    // ВИДАЛЕНО: Внутрішній стан для ID більше не потрібен, оскільки ID буде передаватися напряму.
    // private val _currentPlanId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(DayPlanUiState())
    val uiState: StateFlow<DayPlanUiState> = _uiState.asStateFlow()

    private val _isAddTaskDialogOpen = MutableStateFlow(false)
    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    private val _selectedTask = MutableStateFlow<DayTask?>(null)
    val selectedTask: StateFlow<DayTask?> = _selectedTask.asStateFlow()

    fun openAddTaskDialog() { _isAddTaskDialogOpen.value = true }
    fun dismissAddTaskDialog() { _isAddTaskDialogOpen.value = false }
    fun selectTask(task: DayTask) { _selectedTask.value = task }
    fun clearSelectedTask() { _selectedTask.value = null }
    fun dismissError() { _uiState.update { it.copy(error = null) } }

    /**
     * ВИПРАВЛЕНО: Функція тепер приймає dayPlanId як параметр.
     */
    fun addTask(dayPlanId: String, title: String, description: String, duration: Long?, priority: TaskPriority) {
        viewModelScope.launch {
            try {
                val taskParams = NewTaskParameters(
                    dayPlanId = dayPlanId,
                    title = title,
                    description = description.takeIf { it.isNotBlank() },
                    estimatedDurationMinutes = duration,
                    priority = priority
                )
                dayManagementRepository.addTaskToDayPlan(taskParams)
                dismissAddTaskDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка при додаванні: ${e.localizedMessage}") }
                e.printStackTrace()
            }
        }
    }

    /**
     * ВИПРАВЛЕНО: Функція тепер приймає dayPlanId як параметр.
     */
    fun deleteTask(dayPlanId: String, taskId: String) {
        viewModelScope.launch {
            try {
                dayManagementRepository.deleteTask(taskId) // Репозиторій сам знайде planId з завдання
                clearSelectedTask()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Помилка при видаленні: ${e.localizedMessage}") }
            }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            dayManagementRepository.toggleTaskCompletion(taskId)
        }
    }

    fun loadDataForPlan(dayPlanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            dayManagementRepository.getTasksForDay(dayPlanId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks.sortedWith(
                                compareBy({ t -> t.completed }, { t -> t.dueTime ?: Long.MAX_VALUE })
                            )
                        )
                    }
                }
        }
    }
}