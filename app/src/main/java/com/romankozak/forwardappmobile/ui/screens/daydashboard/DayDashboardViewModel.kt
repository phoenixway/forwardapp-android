package com.romankozak.forwardappmobile.ui.screens.daydashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository // <-- Імпортуємо єдиний репозиторій
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayDashboardUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTask> = emptyList(),
    val metrics: DailyMetric? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    // Обчислювані властивості для зручності
    val tasksCompleted: Int get() = tasks.count { it.completed }
    val tasksTotal: Int get() = tasks.size
    val progress: Float get() = if (tasksTotal > 0) tasksCompleted.toFloat() / tasksTotal else 0f
}

@HiltViewModel
class DayDashboardViewModel @Inject constructor(
    // ВИПРАВЛЕНО: Приймаємо один репозиторій замість трьох
    private val dayManagementRepository: DayManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDashboardUiState())
    val uiState: StateFlow<DayDashboardUiState> = _uiState.asStateFlow()

    fun loadDataForDay(dayPlanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // ВИПРАВЛЕНО: Викликаємо методи з одного dayManagementRepository
            combine(
                dayManagementRepository.getPlanByIdStream(dayPlanId),
                dayManagementRepository.getTasksForDay(dayPlanId),
                dayManagementRepository.getMetricForDayStream(dayPlanId)
            ) { plan, tasks, metrics ->
                DayDashboardUiState(
                    dayPlan = plan,
                    tasks = tasks,
                    metrics = metrics,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }
}