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
import javax.inject.Inject

@HiltViewModel
class TimeReflectionViewModel
    @Inject
    constructor(
        repository: TimeReflectionRepository,
    ) : ViewModel() {
        private val selectedPeriod = MutableStateFlow(ReflectionPeriod.DAY)
        private val recordedDayStarts = MutableStateFlow<List<Long>?>(null)

        val uiState: StateFlow<TimeReflectionUiState> =
            combine(repository.activityRecords, selectedPeriod, recordedDayStarts, repository.entityCatalog) {
                    records,
                    period,
                    starts,
                    entities,
                ->
                if (starts == null) {
                    TimeReflectionUiState(isLoading = true)
                } else {
                    TimeReflectionUiState(
                        reflection =
                            calculateTimeReflection(
                                records = records,
                                recordedDayStarts = starts,
                                period = period,
                                now = System.currentTimeMillis(),
                                entityTitles = entities.associate { (it.link.entityType to it.link.entityId) to it.title },
                            ),
                        isLoading = false,
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
    }
