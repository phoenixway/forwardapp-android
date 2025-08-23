// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaledit/GoalEditViewModel.kt ---
package com.romankozak.forwardappmobile.ui.screens.goaledit

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.utils.HierarchyFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class GoalEditEvent {
    data class NavigateBack(val message: String? = null) : GoalEditEvent()
}

// ✨ ОНОВЛЕНО: Стан тепер використовує `relatedLinks`
data class GoalEditUiState(
    val goalText: TextFieldValue = TextFieldValue(""),
    val goalDescription: TextFieldValue = TextFieldValue(""),
    val relatedLinks: List<RelatedLink> = emptyList(),
    val isReady: Boolean = false,
    val isNewGoal: Boolean = true,
    val showListChooser: Boolean = false,
    val isScoringEnabled: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val isDescriptionEditorOpen: Boolean = false
)

@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val contextHandler: ContextHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle["goalId"]
    private val initialListId: String? = savedStateHandle["listId"]

    private val _uiState = MutableStateFlow(GoalEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GoalEditEvent>()
    val events = _events.receiveAsFlow()

    private var currentGoal: Goal? = null

    val allContextNames: StateFlow<List<String>> = contextHandler.contextNamesFlow


    private val _eventChannel = Channel<GoalEditEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()


    val listHierarchy: StateFlow<ListHierarchyData> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevel = allLists.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchyData(allLists, topLevel, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())


    init {
        // Запускаємо колектор для selectedListId в окремому потоці
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("selectedListId", null).collect { listId ->
                if (listId != null) {
                    onSelectRelatedList(listId)
                    savedStateHandle["selectedListId"] = null
                }
            }
        }

        // Основну ініціалізацію запускаємо окремо
        viewModelScope.launch {
            contextHandler.initialize()
            if (goalId != null) {
                loadExistingGoal(goalId)
            } else {
                createNewGoal()
            }
        }
    }

    private suspend fun loadExistingGoal(goalId: String) {
        // Логування
        Log.d("GoalEditViewModel", "Спроба завантажити ціль з ID: $goalId")
        val goal = goalRepository.getGoalById(goalId)
        Log.d("GoalEditViewModel", "Результат завантаження: ${if (goal != null) "ЗНАЙДЕНО" else "НЕ ЗНАЙДЕНО"}")

          if (goal != null) {
              currentGoal = goal
              _uiState.update {
it.copy(
                goalText = TextFieldValue(goal.text),
                goalDescription = TextFieldValue(goal.description ?: ""),
                relatedLinks = goal.relatedLinks ?: emptyList(),
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
                displayScore = goal.displayScore,
                scoringStatus = goal.scoringStatus,
                isScoringEnabled = goal.scoringStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS
             )
              }
              } else {
              _events.send(GoalEditEvent.NavigateBack("Ціль не знайдено"))
              }
        }

    private fun createNewGoal() {
        // Логіка для створення нової цілі, яка просто ініціалізує UI
        _uiState.update {
            it.copy(
                isReady = true,
                isNewGoal = true,
                scoringStatus = ScoringStatus.NOT_ASSESSED,
                isScoringEnabled = true
            )
        }
        updateScores()
    }

    fun onSave() {
        viewModelScope.launch {
            if (_uiState.value.goalText.text.isBlank()) {
                _events.send(GoalEditEvent.NavigateBack("Назва цілі не може бути пустою"))
                return@launch
            }

            val goalFromState = buildGoalFromState(_uiState.value)
            val goalToSave = GoalScoringManager.calculateScores(goalFromState)

            if (currentGoal != null) {
                // Оновлення існуючої цілі
                goalRepository.updateGoal(goalToSave)
                contextHandler.syncContextsOnUpdate(oldGoal = currentGoal!!, newGoal = goalToSave)
            } else {
                // ✨ ВИПРАВЛЕНО: Створення нової цілі значно спрощено
                initialListId ?: return@launch // Потрібен ID списку для створення
                goalRepository.addGoalToList(goalToSave.text, initialListId)
                // Контексти та інші властивості, які могли бути в `goalToSave`,
                // будуть оброблені у `handleContextsOnCreate` або ігноруються при першому створенні
            }

            _events.send(GoalEditEvent.NavigateBack("Збережено"))
        }
    }

    private fun buildGoalFromState(state: GoalEditUiState): Goal {
        val currentTime = System.currentTimeMillis()
        val descriptionToSave = state.goalDescription.text.ifEmpty { null }

        val baseGoal = currentGoal ?: Goal(
            id = UUID.randomUUID().toString(),
            text = "",
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        return baseGoal.copy(
            text = state.goalText.text,
            description = descriptionToSave,
            updatedAt = currentTime,
            relatedLinks = state.relatedLinks, // ✨ ОНОВЛЕНО
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk,
            scoringStatus = state.scoringStatus
        )
    }

    // --- ОБРОБНИКИ ЗМІН UI ---
    fun onTextChange(newValue: TextFieldValue) = _uiState.update { it.copy(goalText = newValue) }
    fun onDescriptionChange(newValue: TextFieldValue) = _uiState.update { it.copy(goalDescription = newValue) }
    fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }
    fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }
    fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }
    fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }
    fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }
    fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }
    fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }
    fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }
    fun onScoringStatusChange(newStatus: ScoringStatus) {
        _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS) }
        updateScores()
    }

    private fun onScoringParameterChange(update: (GoalEditUiState) -> GoalEditUiState) {
        _uiState.update(update)
        if (_uiState.value.scoringStatus == ScoringStatus.NOT_ASSESSED) {
            _uiState.update { it.copy(scoringStatus = ScoringStatus.ASSESSED) }
        }
        updateScores()
    }

    private fun updateScores() {
        val tempGoal = buildGoalFromState(_uiState.value)
        val updatedGoal = GoalScoringManager.calculateScores(tempGoal)
        _uiState.update { it.copy(rawScore = updatedGoal.rawScore, displayScore = updatedGoal.displayScore) }
    }

    // --- УПРАВЛІННЯ RELATED LINKS ---
    fun onShowListChooser() = _uiState.update { it.copy(showListChooser = true) }
    fun onDismissListChooser() = _uiState.update { it.copy(showListChooser = false) }

    fun onAddListAssociation(listId: String) {
        val list = listHierarchy.value.allLists.find { it.id == listId }
        val newLink = RelatedLink(
            type = LinkType.GOAL_LIST,
            target = listId,
            displayName = list?.name
        )
        _uiState.update {
            if (it.relatedLinks.any { link -> link.target == listId && link.type == LinkType.GOAL_LIST }) it
            else it.copy(relatedLinks = it.relatedLinks + newLink)
        }
        onDismissListChooser()
    }

    fun onRemoveListAssociation(listIdToRemove: String) {
        _uiState.update {
            it.copy(relatedLinks = it.relatedLinks.filterNot { link -> link.target == listIdToRemove && link.type == LinkType.GOAL_LIST })
        }
    }

    fun addNewList(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { goalRepository.createGoalListWithId(id, name, parentId) }
    }
    fun openDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = true) }
    fun closeDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = false) }
    fun onDescriptionChangeAndCloseEditor(newDescription: String) {
        _uiState.update { it.copy(
            goalDescription = it.goalDescription.copy(text = newDescription),
            isDescriptionEditorOpen = false
        )}
    }

    fun onSelectRelatedList(listId: String) {
        val newLink = RelatedLink(
            type = LinkType.GOAL_LIST,
            target = listId,
            displayName = null // Назва буде отримана з БД пізніше, якщо потрібно
        )

        _uiState.update {
            if (it.relatedLinks.any { link -> link.target == listId && link.type == LinkType.GOAL_LIST }) it
            else it.copy(relatedLinks = it.relatedLinks + newLink)
        }
    }
}