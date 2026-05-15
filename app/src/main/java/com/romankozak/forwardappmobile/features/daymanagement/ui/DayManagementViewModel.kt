package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.routes.DAY_PLAN_DATE_ARG
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayManagementState(
    val dayPlanId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedTab: DayManagementTab = DayManagementTab.DAY_PLAN,
)

sealed class DayManagementUiEvent {
    data class NavigateToProject(val projectId: String) : DayManagementUiEvent()
}

@HiltViewModel
class DayManagementViewModel
    @Inject
    constructor(
        private val dayManagementRepository: DayManagementRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DayManagementState())
        val uiState: StateFlow<DayManagementState> = _uiState.asStateFlow()

        private val _uiEvent = Channel<DayManagementUiEvent>()
        val uiEvent = _uiEvent.receiveAsFlow()

        init {
            val dateMillis: Long = savedStateHandle.get<Long>(DAY_PLAN_DATE_ARG) ?: System.currentTimeMillis()
            loadOrCreatePlan(dateMillis)
        }

        private fun loadOrCreatePlan(date: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                try {
                    val plan = dayManagementRepository.createOrUpdateDayPlan(date)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dayPlanId = plan.id,
                            selectedDate = date,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Невідома помилка",
                        )
                    }
                }
            }
        }

        fun onInboxClicked() {
            viewModelScope.launch(ioDispatcher) {
                val inboxContextId = SystemContexts.INBOX.raw
                _uiEvent.send(DayManagementUiEvent.NavigateToProject(inboxContextId))
            }
        }

        fun retryLoading() {
            loadOrCreatePlan(_uiState.value.selectedDate)
        }

        fun navigateToDate(newDate: Long) {
            loadOrCreatePlan(newDate)
        }

        fun selectTab(tab: DayManagementTab) {
            _uiState.update { currentState ->
                if (currentState.selectedTab == tab) {
                    currentState
                } else {
                    currentState.copy(selectedTab = tab)
                }
            }
        }
    }
