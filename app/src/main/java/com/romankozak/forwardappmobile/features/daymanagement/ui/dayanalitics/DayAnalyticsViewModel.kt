package com.romankozak.forwardappmobile.features.daymanagement.ui.dayanalitics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.ai.WeeklyInsights
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.UserAwarenessRepository
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

private const val WEEK_DAYS = 7
private const val TWO_WEEKS_DAYS = 14
private const val MONTH_DAYS = 30
private const val THREE_MONTHS_DAYS = 90
private const val STALE_THRESHOLD_MINUTES = 30
private const val MINUTES_PER_HOUR = 60
private const val MILLIS_PER_MINUTE = 1000L * 60
private const val LOW_PRODUCTIVITY_THRESHOLD = 0.3f
private const val MEDIUM_PRODUCTIVITY_THRESHOLD = 0.7f
private const val HIGH_PRODUCTIVITY_COLOR_THRESHOLD = 0.8f
private const val MEDIUM_PRODUCTIVITY_COLOR_THRESHOLD = 0.6f
private const val LOW_PRODUCTIVITY_COLOR_THRESHOLD = 0.4f
private const val VERY_HIGH_WEEKDAY_SCORE = 0.9
private const val MEDIUM_WEEKDAY_SCORE = 0.5
private const val GROWING_TREND_THRESHOLD = 0.7
private const val STABLE_TREND_THRESHOLD = 0.4

enum class TimeRange(val days: Int, val displayName: String) {
    WEEK(WEEK_DAYS, "Тиждень"),
    TWO_WEEKS(TWO_WEEKS_DAYS, "2 тижні"),
    MONTH(MONTH_DAYS, "Місяць"),
    THREE_MONTHS(THREE_MONTHS_DAYS, "3 місяці"),
}

data class DayAnalyticsUiState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val insights: WeeklyInsights? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val isTurbulentWeek: Boolean = false,
)

@HiltViewModel
class DayAnalyticsViewModel
    @Inject
    constructor(
        private val dayManagementRepository: DayManagementRepository,
        private val userAwarenessRepository: UserAwarenessRepository,
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
                    val now = Instant.now().toEpochMilli()
                    val awarenessFlags = userAwarenessRepository.getWeeklyReviewFlags(startDate, now)

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
                                    isTurbulentWeek = awarenessFlags.turbulent,
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
            val staleThreshold = STALE_THRESHOLD_MINUTES * MINUTES_PER_HOUR * MILLIS_PER_MINUTE
            return System.currentTimeMillis() - lastUpdated > staleThreshold
        }

        fun getProductivityRecommendations(): List<String> {
            val insights = _uiState.value.insights ?: return emptyList()
            val recommendations = mutableListOf<String>()

            when {
                insights.averageCompletionRate < LOW_PRODUCTIVITY_THRESHOLD -> {
                    recommendations.addAll(
                        listOf(
                            "Спробуйте створювати менше завдань на день",
                            "Розбивайте великі завдання на менші частини",
                            "Встановлюйте реалістичні терміни виконання",
                        ),
                    )
                }
                insights.averageCompletionRate < MEDIUM_PRODUCTIVITY_THRESHOLD -> {
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
                "Понеділок" to HIGH_PRODUCTIVITY_COLOR_THRESHOLD.toDouble(),
                "Вівторок" to GROWING_TREND_THRESHOLD,
                "Середа" to VERY_HIGH_WEEKDAY_SCORE,
                "Четвер" to MEDIUM_PRODUCTIVITY_COLOR_THRESHOLD.toDouble(),
                "П'ятниця" to MEDIUM_WEEKDAY_SCORE,
                "Субота" to LOW_PRODUCTIVITY_THRESHOLD.toDouble(),
                "Неділя" to STABLE_TREND_THRESHOLD,
            )
        }

        fun formatDuration(minutes: Int): String {
            val hours = minutes / MINUTES_PER_HOUR
            val remainingMinutes = minutes % MINUTES_PER_HOUR
            return when {
                hours == 0 -> "${remainingMinutes}хв"
                remainingMinutes == 0 -> "${hours}г"
                else -> "${hours}г ${remainingMinutes}хв"
            }
        }

        fun getProductivityColor(completionRate: Float): String {
            return when {
                completionRate >= HIGH_PRODUCTIVITY_COLOR_THRESHOLD -> "success"
                completionRate >= MEDIUM_PRODUCTIVITY_COLOR_THRESHOLD -> "warning"
                completionRate >= LOW_PRODUCTIVITY_COLOR_THRESHOLD -> "info"
                else -> "error"
            }
        }

        fun getProductivityTrend(): String {
            val insights = _uiState.value.insights ?: return "Недостатньо даних"

            return when {
                insights.averageCompletionRate > GROWING_TREND_THRESHOLD -> "Зростаюча тенденція"
                insights.averageCompletionRate > STABLE_TREND_THRESHOLD -> "Стабільна тенденція"
                else -> "Потребує покращення"
            }
        }
    }
