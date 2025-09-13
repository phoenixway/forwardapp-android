package com.romankozak.forwardappmobile.ui.screens.editlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class EditListUiState(
    val list: GoalList? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val reminderTime: Long? = null,
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
    val isScoringEnabled: Boolean = true,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
)

@HiltViewModel
class EditListViewModel
@Inject
constructor(
    private val goalRepository: GoalRepository,
    private val alarmScheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(EditListUiState())
    val uiState: StateFlow<EditListUiState> = _uiState.asStateFlow()

    private var originalList: GoalList? = null

    init {
        viewModelScope.launch {
            val loadedList = goalRepository.getGoalListById(listId)
            originalList = loadedList
            _uiState.update {
                if (loadedList != null) {
                    it.copy(
                        list = loadedList,
                        name = loadedList.name,
                        tags = loadedList.tags ?: emptyList(),
                        reminderTime = loadedList.reminderTime,
                        scoringStatus = loadedList.scoringStatus,
                        isScoringEnabled = loadedList.scoringStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS,
                        valueImportance = loadedList.valueImportance,
                        valueImpact = loadedList.valueImpact,
                        effort = loadedList.effort,
                        cost = loadedList.cost,
                        risk = loadedList.risk,
                        weightEffort = loadedList.weightEffort,
                        weightCost = loadedList.weightCost,
                        weightRisk = loadedList.weightRisk,
                        rawScore = loadedList.rawScore
                    )
                } else {
                    it
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onTagsChange(newTags: List<String>) {
        _uiState.update { it.copy(tags = newTags) }
    }

    fun onSave(): GoalList? {
        if (_uiState.value.name.isBlank()) return null

        val state = _uiState.value
        val currentList = originalList ?: return null

        val tempGoalList = currentList.copy(
            name = state.name,
            tags = state.tags.filter { it.isNotBlank() }.map { it.trim() },
            updatedAt = System.currentTimeMillis(),
            reminderTime = state.reminderTime,
            scoringStatus = state.scoringStatus,
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk
        )

        val updatedList = GoalScoringManager.calculateScoresForList(tempGoalList)

        viewModelScope.launch {
            val oldReminderTime = originalList?.reminderTime
            val newReminderTime = updatedList.reminderTime
            if (newReminderTime != oldReminderTime) {
                if (newReminderTime != null) {
                    alarmScheduler.scheduleForList(updatedList)
                } else {
                    originalList?.let { alarmScheduler.cancelForList(it) }
                }
            }

            goalRepository.updateGoalList(updatedList)
        }
        return updatedList
    }

    fun onSetReminder(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
        }
        _uiState.update { it.copy(reminderTime = calendar.timeInMillis) }
    }

    fun onClearReminder() {
        _uiState.update { it.copy(reminderTime = null) }
    }

    fun onScoringStatusChange(newStatus: ScoringStatus) {
        _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS) }
        updateScores()
    }

    fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }
    fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }
    fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }
    fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }
    fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }
    fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }
    fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }
    fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }

    private fun onScoringParameterChange(update: (EditListUiState) -> EditListUiState) {
        _uiState.update(update)
        if (_uiState.value.scoringStatus == ScoringStatus.NOT_ASSESSED) {
            _uiState.update { it.copy(scoringStatus = ScoringStatus.ASSESSED) }
        }
        updateScores()
    }

    private fun updateScores() {
        val state = _uiState.value
        val tempList = (state.list ?: return).copy(
            scoringStatus = state.scoringStatus,
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk
        )
        val updatedList = GoalScoringManager.calculateScoresForList(tempList)
        _uiState.update { it.copy(rawScore = updatedList.rawScore) }
    }
}