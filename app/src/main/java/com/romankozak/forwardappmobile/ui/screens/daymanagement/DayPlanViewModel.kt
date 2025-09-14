package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.NewTaskParameters
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
    private val dayManagementRepository: DayManagementRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dayPlanId: StateFlow<String?> = savedStateHandle.getStateFlow("dayPlanId", null)

    private val _uiState = MutableStateFlow(DayPlanUiState())
    val uiState: StateFlow<DayPlanUiState> = _uiState.asStateFlow()

    // UX ОНОВЛЕННЯ: StateFlow для керування видимістю діалогового вікна.
    private val _isAddTaskDialogOpen = MutableStateFlow(false)
    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    // --- Методи для керування діалогом ---
    fun openAddTaskDialog() {
        _isAddTaskDialogOpen.value = true
    }

    fun dismissAddTaskDialog() {
        _isAddTaskDialogOpen.value = false
    }
    // ------------------------------------

    init {
        viewModelScope.launch {
            dayPlanId.filterNotNull().flatMapLatest { id ->
                _uiState.update { it.copy(isLoading = true) }
                combine(
                    dayManagementRepository.getPlanByIdStream(id),
                    dayManagementRepository.getTasksForDay(id)
                ) { plan, tasks ->
                    DayPlanUiState(
                        dayPlan = plan,
                        tasks = tasks,
                        isLoading = false
                    )
                }.catch { e ->
                    emit(DayPlanUiState(isLoading = false, error = e.message))
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            val currentPlanId = dayPlanId.value ?: return@launch
            val taskParams = NewTaskParameters(
                dayPlanId = currentPlanId,
                title = title
            )
            dayManagementRepository.addTaskToDayPlan(taskParams)
            // Автоматично закриваємо діалог після додавання
            dismissAddTaskDialog()
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            dayManagementRepository.toggleTaskCompletion(taskId)
        }
    }
    fun loadDataForPlan(dayPlanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                dayManagementRepository.getPlanByIdStream(dayPlanId),
                dayManagementRepository.getTasksForDay(dayPlanId)
            ) { plan, tasks ->
                DayPlanUiState(
                    dayPlan = plan,
                    tasks = tasks,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}