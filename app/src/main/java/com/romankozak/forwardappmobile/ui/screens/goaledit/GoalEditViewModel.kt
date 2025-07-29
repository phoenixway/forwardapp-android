package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class GoalEditEvent {
    data class NavigateBack(val message: String? = null) : GoalEditEvent()
}

data class GoalEditUiState(
    // Основні дані
    val goalText: String = "",
    val goalDescription: String = "",
    val associatedLists: List<GoalList> = emptyList(),

    // Стан UI
    val isReady: Boolean = false,
    val isNewGoal: Boolean = true,
    val showListChooser: Boolean = false,

    // Метадані
    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    // --- Поля для нової Системи Б ---
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,

    // --- Розраховані значення для UI ---
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
)

@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle["goalId"]
    private val initialListId: String? = savedStateHandle["listId"]

    private val _uiState = MutableStateFlow(GoalEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GoalEditEvent>()
    val events = _events.receiveAsFlow()

    private var currentGoal: Goal? = null

    val listHierarchy: StateFlow<ListHierarchyData> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevel = allLists.filter { it.parentId == null }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchyData(allLists, topLevel, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    init {
        viewModelScope.launch {
            if (goalId != null) {
                loadExistingGoal(goalId)
            } else {
                createNewGoal()
            }
        }
    }

    private suspend fun loadExistingGoal(goalId: String) {
        val goal = goalRepository.getGoalById(goalId)
        if (goal != null) {
            currentGoal = goal
            val associatedIds = goal.associatedListIds ?: emptyList()
            val lists = if (associatedIds.isNotEmpty()) goalRepository.getListsByIds(associatedIds) else emptyList()

            _uiState.update {
                it.copy(
                    goalText = goal.text,
                    goalDescription = goal.description ?: "",
                    associatedLists = lists,
                    isReady = true,
                    isNewGoal = false,
                    createdAt = goal.createdAt,
                    updatedAt = goal.updatedAt,
                    valueImportance = goal.valueImportance,
                    valueImpact = goal.valueImpact,
                    effort = goal.effort,
                    cost = goal.cost,
                    risk = goal.risk,
                    weightEffort = goal.weightEffort,
                    weightCost = goal.weightCost,
                    weightRisk = goal.weightRisk,
                    rawScore = goal.rawScore,
                    displayScore = goal.displayScore
                )
            }
        } else {
            _events.send(GoalEditEvent.NavigateBack("Ціль не знайдено"))
        }
    }

    private fun createNewGoal() {
        _uiState.update {
            it.copy(isReady = true, isNewGoal = true)
        }
        updateScores()
    }

    // --- Функції для оновлення параметрів ---
    fun onTextChange(newText: String) = _uiState.update { it.copy(goalText = newText) }
    fun onDescriptionChange(newDescription: String) = _uiState.update { it.copy(goalDescription = newDescription) }
    fun onValueImportanceChange(value: Float) = updateState { it.copy(valueImportance = value) }
    fun onValueImpactChange(value: Float) = updateState { it.copy(valueImpact = value) }
    fun onEffortChange(value: Float) = updateState { it.copy(effort = value) }
    fun onCostChange(value: Float) = updateState { it.copy(cost = value) }
    fun onRiskChange(value: Float) = updateState { it.copy(risk = value) }
    fun onWeightEffortChange(value: Float) = updateState { it.copy(weightEffort = value) }
    fun onWeightCostChange(value: Float) = updateState { it.copy(weightCost = value) }
    fun onWeightRiskChange(value: Float) = updateState { it.copy(weightRisk = value) }

    private fun updateState(update: (GoalEditUiState) -> GoalEditUiState) {
        _uiState.update(update)
        updateScores() // Перерахунок при кожній зміні
    }

    private fun updateScores() {
        val tempGoal = buildGoalFromState(_uiState.value)
        val updatedGoal = GoalScoringManager.calculateScores(tempGoal)
        _uiState.update {
            it.copy(
                rawScore = updatedGoal.rawScore,
                displayScore = updatedGoal.displayScore
            )
        }
    }

    fun onAddListAssociation(listId: String) {
        viewModelScope.launch {
            if (_uiState.value.associatedLists.any { it.id == listId }) return@launch
            val listToAdd = listHierarchy.value.allLists.find { it.id == listId }
            if (listToAdd != null) {
                _uiState.update {
                    it.copy(associatedLists = it.associatedLists + listToAdd)
                }
            }
        }
        onDismissListChooser()
    }

    fun onRemoveListAssociation(listId: String) {
        _uiState.update {
            it.copy(associatedLists = it.associatedLists.filterNot { list -> list.id == listId })
        }
    }

    fun onShowListChooser() = _uiState.update { it.copy(showListChooser = true) }
    fun onDismissListChooser() = _uiState.update { it.copy(showListChooser = false) }

    private fun buildGoalFromState(state: GoalEditUiState): Goal {
        val currentTime = System.currentTimeMillis()
        return currentGoal?.copy(
            text = state.goalText,
            description = state.goalDescription.ifBlank { null },
            updatedAt = currentTime,
            associatedListIds = state.associatedLists.map { it.id },
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk
        ) ?: Goal(
            id = UUID.randomUUID().toString(),
            text = state.goalText,
            description = state.goalDescription.ifBlank { null },
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime,
            tags = null,
            associatedListIds = state.associatedLists.map { it.id },
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk
        )
    }

    fun onSave() {
        viewModelScope.launch {
            if (_uiState.value.goalText.isBlank()) {
                _events.send(GoalEditEvent.NavigateBack("Назва цілі не може бути пустою"))
                return@launch
            }

            // Завжди перераховуємо перед збереженням
            val goalToSave = GoalScoringManager.calculateScores(buildGoalFromState(_uiState.value))

            if (currentGoal != null) {
                goalRepository.updateGoal(goalToSave)
            } else {
                val listIdForNewGoal = initialListId
                if (listIdForNewGoal == null) {
                    _events.send(GoalEditEvent.NavigateBack("Не вдалося створити ціль: невідомий список."))
                    return@launch
                }

                val finalGoal = if (goalToSave.associatedListIds.isNullOrEmpty()){
                    goalToSave.copy(associatedListIds = listOf(listIdForNewGoal))
                } else goalToSave

                goalRepository.insertGoal(finalGoal)

                val order = goalRepository.getGoalCountInList(listIdForNewGoal).toLong()
                val newInstance = GoalInstance(
                    instanceId = UUID.randomUUID().toString(),
                    goalId = finalGoal.id,
                    listId = listIdForNewGoal,
                    order = order
                )
                goalRepository.insertInstance(newInstance)
            }
            _events.send(GoalEditEvent.NavigateBack("Збережено"))
        }
    }
}