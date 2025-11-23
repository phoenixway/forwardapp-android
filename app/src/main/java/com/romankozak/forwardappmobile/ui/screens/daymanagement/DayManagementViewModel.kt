
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ProjectType
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.routes.DAY_PLAN_DATE_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayManagementState(
    val dayPlanId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDate: Long = System.currentTimeMillis(),
)

sealed class DayManagementUiEvent {
    data class NavigateToProject(val projectId: String) : DayManagementUiEvent()
}

@HiltViewModel
class DayManagementViewModel
@Inject
constructor(
    private val dayManagementRepository: DayManagementRepository,
    private val projectRepository: ProjectRepository,
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
            val projects = projectRepository.getAllProjects()
            val specialProject = projects.find { it.projectType == ProjectType.SYSTEM }
            if (specialProject != null) {
                val inboxProject = projects.find { it.reservedGroup == ReservedGroup.Inbox && it.parentId == specialProject.id }
                inboxProject?.let {
                    _uiEvent.send(DayManagementUiEvent.NavigateToProject(it.id))
                }
            }
        }
    }

    fun retryLoading() {
        loadOrCreatePlan(_uiState.value.selectedDate)
    }

    fun navigateToDate(newDate: Long) {
        loadOrCreatePlan(newDate)
    }
}
