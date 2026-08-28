package com.romankozak.forwardappmobile.features.activitytracker.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class TimeReflectionViewModel
    @Inject
    constructor(
        repository: TimeReflectionRepository,
    ) : ViewModel() {
        private val selectedPeriod = MutableStateFlow(ReflectionPeriod.DAY)
        private val selectedDayStart = MutableStateFlow<Long?>(null)
        private val recordedDayStarts = MutableStateFlow<List<Long>?>(null)

        val uiState: StateFlow<TimeReflectionUiState> =
            combine(repository.activityRecords, selectedPeriod, recordedDayStarts, repository.entityCatalog, selectedDayStart) {
                    records,
                    period,
                    starts,
                    entities,
                    requestedDayStart,
                ->
                if (starts == null) {
                    TimeReflectionUiState(isLoading = true)
                } else {
                    val now = System.currentTimeMillis()
                    val availableStarts = starts.filter { it <= now }.distinct().sorted()
                    val anchor = requestedDayStart?.takeIf(availableStarts::contains) ?: availableStarts.lastOrNull()
                    val anchorIndex = anchor?.let(availableStarts::indexOf) ?: -1
                    TimeReflectionUiState(
                        reflection =
                            calculateTimeReflection(
                                records = records,
                                recordedDayStarts = availableStarts,
                                period = period,
                                now = now,
                                entityTitles = entities.associate { (it.link.entityType to it.link.entityId) to it.title },
                                anchorDayStart = anchor,
                            ),
                        isLoading = false,
                        availableDayStarts = availableStarts,
                        selectedDayStart = anchor,
                        hasPreviousDay = anchorIndex > 0,
                        hasNextDay = anchorIndex >= 0 && anchorIndex < availableStarts.lastIndex,
                        isLatestDay = anchorIndex < 0 || anchorIndex == availableStarts.lastIndex,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TimeReflectionUiState(),
            )

        init {
            viewModelScope.launch {
                recordedDayStarts.value = repository.getRecordedDayStarts()
            }
        }

        fun selectPeriod(period: ReflectionPeriod) {
            selectedPeriod.value = period
        }

        fun selectPreviousDay() {
            moveSelectedDay(-1)
        }

        fun selectNextDay() {
            moveSelectedDay(1)
        }

        fun selectLatestDay() {
            selectedDayStart.value = null
        }

        fun selectCalendarDay(utcDateMillis: Long) {
            val targetDate = Instant.ofEpochMilli(utcDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
            val zone = ZoneId.systemDefault()
            val matchingStart =
                recordedDayStarts.value.orEmpty().lastOrNull { start ->
                    Instant.ofEpochMilli(start).atZone(zone).toLocalDate() == targetDate
                }
            selectedDayStart.value = matchingStart?.takeUnless {
                it == recordedDayStarts.value.orEmpty().filter { start -> start <= System.currentTimeMillis() }.maxOrNull()
            }
        }

        private fun moveSelectedDay(offset: Int) {
            val starts = recordedDayStarts.value.orEmpty().filter { it <= System.currentTimeMillis() }.distinct().sorted()
            if (starts.isEmpty()) return
            val current = selectedDayStart.value ?: starts.last()
            val nextIndex = (starts.indexOf(current) + offset).coerceIn(0, starts.lastIndex)
            selectedDayStart.value = starts[nextIndex].takeUnless { nextIndex == starts.lastIndex }
        }
    }
