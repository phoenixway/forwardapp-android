package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.repository.DailyAnalytics
import com.romankozak.forwardappmobile.data.repository.DailyLifeRepository
import com.romankozak.forwardappmobile.utils.DayManagementUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayPlanUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val plan: DayPlan? = null,
    val tasks: List<DayTask> = emptyList(),
    val analytics: DailyAnalytics? = null, // Поле для даних дашборду
    val isLoading: Boolean = true
)

@HiltViewModel
class DayPlanViewModel @Inject constructor(
    private val dailyLifeRepository: DailyLifeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateFlow: StateFlow<Long> = savedStateHandle.getStateFlow("date", System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DayPlanUiState> = dateFlow.flatMapLatest { date ->
        val dayStart = DayManagementUtils.getDayStart(date)
        val dayEnd = DayManagementUtils.getDayEnd(date)

        dailyLifeRepository.createOrUpdateDayPlan(date)

        // Використовуємо flatMapLatest для вирішення залежності від ID плану
        dailyLifeRepository.getPlanForDate(date).flatMapLatest { plan ->
            val tasksFlow = if (plan != null) dailyLifeRepository.getTasksForDay(plan.id) else flowOf(emptyList())
            // Потік для аналітики за поточний день
            val analyticsFlow = dailyLifeRepository.getDailyAnalytics(dayStart, dayEnd)

            combine(tasksFlow, analyticsFlow) { tasks, analyticsList ->
                DayPlanUiState(
                    selectedDate = date,
                    plan = plan,
                    tasks = tasks,
                    analytics = analyticsList.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DayPlanUiState()
    )

    fun addTask(title: String, priority: TaskPriority = TaskPriority.MEDIUM) {
        if (title.isBlank()) return

        viewModelScope.launch {
            uiState.value.plan?.let { plan ->
                dailyLifeRepository.addTaskToDayPlan(
                    dayPlanId = plan.id,
                    title = title,
                    priority = priority
                )
            }
        }
    }

    fun toggleTaskCompletion(task: DayTask) {
        viewModelScope.launch {
            if (!task.completed) {
                dailyLifeRepository.completeTask(task.id)
            } else {
                // TODO: Додайте логіку для "розукомплектування", якщо потрібно
            }
        }
    }
}

