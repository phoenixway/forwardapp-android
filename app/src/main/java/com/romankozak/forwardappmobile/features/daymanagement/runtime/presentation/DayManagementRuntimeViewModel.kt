package com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementPhase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeCommand
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import com.romankozak.forwardappmobile.features.daymanagement.runtime.engine.DayManagementRuntimeTriggerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayManagementRuntimeUiState(
    val runtimeState: DayManagementRuntimeState = DayManagementRuntimeState(),
    val wakePlanDeadlineAt: Long? = null,
)

@HiltViewModel
class DayManagementRuntimeViewModel
    @Inject
    constructor(
        private val repository: DayManagementRuntimeRepository,
        private val triggerEngine: DayManagementRuntimeTriggerEngine,
    ) : ViewModel() {
        val uiState: StateFlow<DayManagementRuntimeUiState> =
            repository.state
                .map { state ->
                    DayManagementRuntimeUiState(
                        runtimeState = state,
                        wakePlanDeadlineAt = state.wokeAt?.plus(DayManagementRuntimeTriggerEngine.NO_PLAN_AFTER_WAKE_MILLIS),
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DayManagementRuntimeUiState(),
                )

        init {
            launchCommand(DayManagementRuntimeCommand.AutoCloseStaleOpenDay(now()))
        }

        fun wakeUp() {
            launchCommand(DayManagementRuntimeCommand.WakeUp(now()))
        }

        fun finalizeThemes() {
            launchCommand(DayManagementRuntimeCommand.FinalizeThemes(now()))
        }

        fun finalizeFocus() {
            launchCommand(DayManagementRuntimeCommand.FinalizeFocus(now()))
        }

        fun finalizePlan() {
            launchCommand(DayManagementRuntimeCommand.FinalizePlan(now()))
        }

        fun startImplementation() {
            launchCommand(DayManagementRuntimeCommand.ActivatePhase(DayManagementPhase.IMPLEMENTATION, now()))
        }

        fun startFinalization() {
            launchCommand(DayManagementRuntimeCommand.ActivatePhase(DayManagementPhase.FINALIZATION, now()))
        }

        fun sleep() {
            launchCommand(DayManagementRuntimeCommand.Sleep(now()))
        }

        fun startPreparation() {
            launchCommand(DayManagementRuntimeCommand.ActivatePhase(DayManagementPhase.PREPARATION, now()))
        }

        private fun launchCommand(command: DayManagementRuntimeCommand) {
            viewModelScope.launch {
                repository.apply(command)
            }
        }

        private fun now(): Long = System.currentTimeMillis()
    }
