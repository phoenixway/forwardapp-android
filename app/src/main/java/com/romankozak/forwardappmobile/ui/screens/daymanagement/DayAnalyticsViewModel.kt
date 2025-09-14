package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.WeeklyInsights
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class TimeRange(val days: Int, val displayName: String) {
    WEEK(7, "Тиждень"),
    MONTH(30, "Місяць")
}

data class DayAnalyticsUiState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val insights: WeeklyInsights? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DayAnalyticsViewModel @Inject constructor(
    private val dayManagementRepository: DayManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayAnalyticsUiState())
    val uiState: StateFlow<DayAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadInsights(TimeRange.WEEK)
    }

    fun selectTimeRange(range: TimeRange) {
        if (range != _uiState.value.selectedRange) {
            loadInsights(range)
        }
    }

    private fun loadInsights(range: TimeRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRange = range) }

            // Розраховуємо дату початку
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -range.days)
            val startDate = calendar.timeInMillis

            dayManagementRepository.getWeeklyInsights(startDate).collect { weeklyInsights ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        insights = weeklyInsights
                    )
                }
            }
        }
    }
}