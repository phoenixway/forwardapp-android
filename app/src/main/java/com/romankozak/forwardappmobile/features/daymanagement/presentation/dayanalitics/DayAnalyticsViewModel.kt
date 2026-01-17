package com.romankozak.forwardappmobile.features.daymanagement.presentation.dayanalitics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.WeeklyInsights
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class TimeRange(val days: Int, val displayName: String) {
    WEEK(7, "Тиждень"),
    TWO_WEEKS(14, "2 тижні"),
    MONTH(30, "Місяць"),
    THREE_MONTHS(90, "3 місяці"),
}

data class DayAnalyticsUiState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val insights: WeeklyInsights? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val lastUpdated: Long? = null,
)

@HiltViewModel
class DayAnalyticsViewModel
    @Inject
    constructor(
        private val dayManagementRepository: DayManagementRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DayAnalyticsUiState())
        val uiState: StateFlow<DayAnalyticsUiState> = _uiState.asStateFlow()

        init {
            loadInsights(TimeRange.WEEK)
        }

        
        fun selectTimeRange(range: TimeRange) {
            if (range != _uiState.value.selectedRange) {
                _uiState.update {
                    it.copy(selectedRange = range, isLoading = true, error = null)
                }
                loadInsights(range)
            }
        }

        
        fun refreshInsights() {
            loadInsights(_uiState.value.selectedRange)
        }

        
        private fun loadInsights(range: TimeRange) {
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            selectedRange = range,
                            error = null,
                        )
                    }

                    
                    val startDate =
                        Instant.now()
                            .minus(range.days.toLong(), ChronoUnit.DAYS)
                            .toEpochMilli()

                    dayManagementRepository.getWeeklyInsights(startDate)
                        .catch { exception ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Помилка завантаження аналітики: ${exception.localizedMessage}",
                                )
                            }
                            exception.printStackTrace()
                        }
                        .collect { weeklyInsights ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    insights = weeklyInsights,
                                    lastUpdated = System.currentTimeMillis(),
                                    error = null,
                                )
                            }
                        }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Неочікувана помилка: ${e.localizedMessage}",
                        )
                    }
                    e.printStackTrace()
                }
            }
        }

        
        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        
        fun isDataStale(): Boolean {
            val lastUpdated = _uiState.value.lastUpdated ?: return true
            val staleThreshold = 30 * 60 * 1000L
            return System.currentTimeMillis() - lastUpdated > staleThreshold
        }

        

        fun getProductivityRecommendations(): List<String> {
            val insights = _uiState.value.insights ?: return emptyList()
            val recommendations = mutableListOf<String>()

            when {
                insights.averageCompletionRate < 0.3f -> {
                    recommendations.addAll(
                        listOf(
                            "Спробуйте створювати менше завдань на день",
                            "Розбивайте великі завдання на менші частини",
                            "Встановлюйте реалістичні терміни виконання",
                        ),
                    )
                }
                insights.averageCompletionRate < 0.7f -> {
                    recommendations.addAll(
                        listOf(
                            "Добре! Спробуйте покращити планування часу",
                            "Використовуйте техніку Помодоро для кращої концентрації",
                            "Розставте пріоритети для найважливіших завдань",
                        ),
                    )
                }
                else -> {
                    recommendations.addAll(
                        listOf(
                            "Відмінна продуктивність! Продовжуйте в тому ж дусі",
                            "Можете спробувати збільшити кількість завдань",
                            "Поділіться своїми методами з іншими",
                        ),
                    )
                }
            }
            return recommendations
        }

        
        fun getWeekdayStats(): Map<String, Double> {
            val insights = _uiState.value.insights ?: return emptyMap()

            
            
            return mapOf(
                "Понеділок" to 0.8,
                "Вівторок" to 0.7,
                "Середа" to 0.9,
                "Четвер" to 0.6,
                "П'ятниця" to 0.5,
                "Субота" to 0.3,
                "Неділя" to 0.4,
            )
        }

        
        fun formatDuration(minutes: Int): String {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return when {
                hours == 0 -> "${remainingMinutes}хв"
                remainingMinutes == 0 -> "${hours}г"
                else -> "${hours}г ${remainingMinutes}хв"
            }
        }

        
        fun getProductivityColor(completionRate: Float): String {
            return when {
                completionRate >= 0.8f -> "success"
                completionRate >= 0.6f -> "warning"
                completionRate >= 0.4f -> "info"
                else -> "error"
            }
        }

        
        fun getProductivityTrend(): String {
            val insights = _uiState.value.insights ?: return "Недостатньо даних"

            
            return when {
                insights.averageCompletionRate > 0.7 -> "Зростаюча тенденція"
                insights.averageCompletionRate > 0.4 -> "Стабільна тенденція"
                else -> "Потребує покращення"
            }
        }
    }
