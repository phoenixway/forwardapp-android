package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.romankozak.forwardappmobile.ui.navigation.DAY_PLAN_DATE_ARG

data class DayManagementState(
    val dayPlanId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DayManagementViewModel @Inject constructor(
    private val dayManagementRepository: DayManagementRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayManagementState())
    val uiState: StateFlow<DayManagementState> = _uiState.asStateFlow()

    init {
        // Отримуємо дату з аргументів навігації
        val dateMillis: Long = savedStateHandle.get<Long>(DAY_PLAN_DATE_ARG) ?: System.currentTimeMillis()
        loadOrCreatePlan(dateMillis)
    }

    private fun loadOrCreatePlan(date: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Використовуємо метод з репозиторію, щоб знайти або створити план
                val plan = dayManagementRepository.createOrUpdateDayPlan(date)
                _uiState.update { it.copy(isLoading = false, dayPlanId = plan.id) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}