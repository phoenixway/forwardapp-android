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
import com.romankozak.forwardappmobile.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

sealed class GoalEditEvent {
    data class NavigateBack(val message: String? = null) : GoalEditEvent()
    data class Navigate(val route: String) : GoalEditEvent()
}

data class GoalEditUiState(
    val goalText: TextFieldValue = TextFieldValue(""),
    val goalDescription: TextFieldValue = TextFieldValue(""),
    val relatedLinks: List<RelatedLink> = emptyList(),
    val isReady: Boolean = false,
    val isNewGoal: Boolean = true,
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
    val isDescriptionEditorOpen: Boolean = false,
    val reminderTime: Long? = null

)

@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val contextHandler: ContextHandler,
    private val alarmScheduler: AlarmScheduler,

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

    init {
        // --- ЗМІНА 1: Прибираємо прослуховування результату звідси ---
        // viewModelScope.launch {
        //     savedStateHandle.getStateFlow<String?>("selectedListId", null).collect { ... }
        // }
        // --- КІНЕЦЬ ЗМІНИ 1 ---

        viewModelScope.launch {
            contextHandler.initialize()
            if (goalId != null) {
                loadExistingGoal(goalId)
            } else {
                createNewGoal()
            }
        }
    }

    // --- ЗМІНА 2: Створюємо нову публічну функцію для обробки результату ---
    fun onListChooserResult(listId: String) {
        val TAG = "NavResultDebug"
        Log.d(TAG, "[GoalEditViewModel] onListChooserResult: Екран повідомив про результат '$listId'")
        Log.d(TAG, "[GoalEditViewModel] Current relatedLinks count: ${_uiState.value.relatedLinks.size}")
        onAddListAssociation(listId)
        Log.d(TAG, "[GoalEditViewModel] After processing - relatedLinks count: ${_uiState.value.relatedLinks.size}")

    }
    // --- КІНЕЦЬ ЗМІНИ 2 ---

    private suspend fun loadExistingGoal(goalId: String) {
        val goal = goalRepository.getGoalById(goalId)
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
                    isScoringEnabled = goal.scoringStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS,
                    reminderTime = goal.reminderTime

                )
            }
        } else {
            _events.send(GoalEditEvent.NavigateBack("Ціль не знайдено"))
        }
    }

    private fun createNewGoal() {
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
            val oldReminderTime = currentGoal?.reminderTime
            val newReminderTime = goalToSave.reminderTime

            if (newReminderTime != oldReminderTime) {
                if (newReminderTime != null) {
                    alarmScheduler.schedule(goalToSave)
                } else {
                    // Скасовуємо тільки якщо старе нагадування існувало
                    currentGoal?.let { alarmScheduler.cancel(it) }
                }
            }

            if (currentGoal != null) {
                goalRepository.updateGoal(goalToSave)
                contextHandler.syncContextsOnUpdate(oldGoal = currentGoal!!, newGoal = goalToSave)
            } else {
                initialListId ?: return@launch
                goalRepository.addGoalToList(goalToSave.text, initialListId)
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
            relatedLinks = state.relatedLinks,
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk,
            scoringStatus = state.scoringStatus,
            reminderTime = state.reminderTime

        )
    }

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

    fun onAddLinkRequest() {
        viewModelScope.launch {
            val disabledIds = _uiState.value.relatedLinks
                .filter { it.type == LinkType.GOAL_LIST }
                .joinToString(",") { it.target }
            val title = URLEncoder.encode("Додати посилання на список", "UTF-8")
            _events.send(GoalEditEvent.Navigate("list_chooser_screen/$title?disabledIds=$disabledIds"))
        }
    }

    private fun onAddListAssociation(listId: String) {
        viewModelScope.launch {
            val listName = goalRepository.getGoalListById(listId)?.name
            val newLink = RelatedLink(
                type = LinkType.GOAL_LIST,
                target = listId,
                displayName = listName
            )
            val TAG = "NavResultDebug"
            Log.d(TAG, "[GoalEditViewModel] Before update - relatedLinks count: ${_uiState.value.relatedLinks.size}")

            _uiState.update {
                if (it.relatedLinks.any { link -> link.target == listId && link.type == LinkType.GOAL_LIST }) it
                else it.copy(relatedLinks = it.relatedLinks + newLink)
            }

            Log.d(TAG, "[GoalEditViewModel] After update - relatedLinks count: ${_uiState.value.relatedLinks.size}")
        }
    }

    fun onRemoveLinkAssociation(targetToRemove: String) {
        _uiState.update {
            it.copy(relatedLinks = it.relatedLinks.filterNot { link -> link.target == targetToRemove })
        }
    }

    fun openDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = true) }
    fun closeDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = false) }
    fun onDescriptionChangeAndCloseEditor(newDescription: String) {
        _uiState.update { it.copy(
            goalDescription = it.goalDescription.copy(text = newDescription),
            isDescriptionEditorOpen = false
        )}
    }

    fun onAddWebLinkRequest() {
        viewModelScope.launch {
            _events.send(GoalEditEvent.NavigateBack("Додавання веб-посилань буде реалізовано пізніше"))
        }
    }

    fun onAddObsidianLinkRequest() {
        viewModelScope.launch {
            _events.send(GoalEditEvent.NavigateBack("Додавання Obsidian посилань буде реалізовано пізніше"))
        }
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

}