package com.romankozak.forwardappmobile.features.daymanagement.ui.daydashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayDashboardUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTask> = emptyList(),
    val metrics: DailyMetric? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val tasksCompleted: Int get() = tasks.count { it.completed }
    val tasksTotal: Int get() = tasks.size
    val progress: Float get() = if (tasksTotal > 0) tasksCompleted.toFloat() / tasksTotal else 0f
}

@HiltViewModel
class DayDashboardViewModel
    @Inject
    constructor(
        private val dayManagementRepository: DayManagementRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DayDashboardUiState())
        val uiState: StateFlow<DayDashboardUiState> = _uiState.asStateFlow()

        fun loadDataForDay(dayPlanId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                combine(
                    dayManagementRepository.getPlanByIdStream(dayPlanId),
                    dayManagementRepository.getTasksForDay(dayPlanId),
                    dayManagementRepository.getMetricForDayStream(dayPlanId),
                ) { plan, tasks, metrics ->
                    DayDashboardUiState(
                        dayPlan = plan,
                        tasks = tasks,
                        metrics = metrics,
                        isLoading = false,
                    )
                }.catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }.collect { combinedState ->
                    _uiState.value = combinedState
                }
            }
        }
    }
