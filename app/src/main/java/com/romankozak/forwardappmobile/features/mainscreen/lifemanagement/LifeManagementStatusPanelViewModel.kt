package com.romankozak.forwardappmobile.features.mainscreen.lifemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATUS_FLOW_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class LifeManagementStatusPanelViewModel
    @Inject
    constructor(
        private val repository: LifeManagementStatusRepository,
    ) : ViewModel() {
        val statuses: StateFlow<List<LifeManagementLevelStatus>> =
            repository.observeStatuses().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATUS_FLOW_STOP_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )

        init {
            viewModelScope.launch {
                repository.ensureDefaults()
            }
        }

        fun save(update: LifeManagementLevelStatusUpdate) {
            viewModelScope.launch {
                repository.updateStatus(update)
            }
        }
    }
