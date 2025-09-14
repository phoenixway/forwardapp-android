package com.romankozak.forwardappmobile.ui.screens.daydashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.repository.DailyMetricRepository // Припустимо, що є репозиторії
import com.romankozak.forwardappmobile.data.repository.DayPlanRepository
import com.romankozak.forwardappmobile.data.repository.DayTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Цей UiState буде єдиним джерелом даних для екрану
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
    // Використовуємо репозиторії як абстракцію над DAO
    private val dayPlanRepository: DayPlanRepository,
    private val dayTaskRepository: DayTaskRepository,
    private val dailyMetricRepository: DailyMetricRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDashboardUiState())
    val uiState: StateFlow<DayDashboardUiState> = _uiState.asStateFlow()

    fun loadDataForDay(dayPlanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Використовуємо combine для одночасного отримання всіх даних для дня
            // Це гарантує, що UI оновиться лише тоді, коли всі дані будуть готові
            combine(
                dayPlanRepository.getPlanByIdStream(dayPlanId), // Потрібно додати цей метод в DAO/Repo
                dayTaskRepository.getTasksForDay(dayPlanId),
                dailyMetricRepository.getMetricForDayStream(dayPlanId) // і цей
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