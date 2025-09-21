// file: ui/screens/backlog/viewmodel/ItemActionHandler.kt

package com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel

import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionDialogState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ItemActionHandler
@Inject
constructor(
    private val projectRepository: ProjectRepository,
    val scope: CoroutineScope,
    private val projectIdFlow: StateFlow<String>,
    private val resultListener: ResultListener,
) {
    interface ResultListener : BaseHandlerResultListener {
        fun isSelectionModeActive(): Boolean

        fun toggleSelection(itemId: String)
    }

    private var recentlyDeletedItems: List<ListItemContent>? = null

    private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
    val goalActionDialogState = _goalActionDialogState.asStateFlow()

    private val _showGoalTransportMenu = MutableStateFlow(false)
    val showGoalTransportMenu = _showGoalTransportMenu.asStateFlow()

    private val _itemForTransportMenu = MutableStateFlow<ListItemContent?>(null)
    val itemForTransportMenu = _itemForTransportMenu.asStateFlow()

    fun onItemClick(item: ListItemContent) {
        if (resultListener.isSelectionModeActive()) {
            resultListener.toggleSelection(item.listItem.id)
        } else {
            val currentProjectId = projectIdFlow.value
            when (item) {
                is ListItemContent.GoalItem ->
                    resultListener.requestNavigation(
                        "goal_edit_screen/$currentProjectId?goalId=${item.goal.id}",
                    )
                is ListItemContent.SublistItem ->
                    // ВИПРАВЛЕНО: Використання правильного маршруту "goal_detail_screen"
                    resultListener.requestNavigation("goal_detail_screen/${item.project.id}")
                is ListItemContent.LinkItem ->
                    resultListener.requestNavigation(BacklogViewModel.HANDLE_LINK_CLICK_ROUTE + "/${item.link.linkData.target}")
            }
        }
    }

    fun deleteItem(item: ListItemContent) {
        scope.launch {
            recentlyDeletedItems = listOf(item)
            projectRepository.deleteListItems(listOf(item.listItem.id))
            resultListener.showSnackbar("Елемент видалено", "Скасувати")
        }
    }

    fun onGoalActionInitiated(item: ListItemContent) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingActionChoice(item)
    }

    fun onDismissGoalActionDialogs() {
        _goalActionDialogState.value = GoalActionDialogState.Hidden
    }

    fun onGoalActionSelected(
        actionType: GoalActionType,
        item: ListItemContent,
    ) {
        // ВИПРАВЛЕНО: Нова функція, яка універсально обробляє дії
        onItemActionSelected(actionType, item)
    }

    fun toggleGoalCompletedWithState(
        goal: Goal,
        isChecked: Boolean,
    ) {
        scope.launch {
            val updatedGoal = goal.copy(completed = isChecked, updatedAt = System.currentTimeMillis())
            projectRepository.updateGoal(updatedGoal)
            delay(100)
            resultListener.forceRefresh()
        }
    }

    fun copyContentRequest(content: ListItemContent) {
        scope.launch {
            val (message, text) =
                when (content) {
                    is ListItemContent.GoalItem -> Pair("Текст скопійовано", content.goal.text)
                    is ListItemContent.LinkItem -> {
                        val linkText = content.link.linkData.displayName ?: content.link.linkData.target
                        Pair("Посилання скопійовано", linkText)
                    }
                    is ListItemContent.SublistItem -> Pair("Назва проекту скопійована", content.project.name)
                }

            resultListener.copyToClipboard(text)
            resultListener.showSnackbar(message, null)
        }
    }

    fun onGoalTransportInitiated(item: ListItemContent) {
        if (item is ListItemContent.GoalItem || item is ListItemContent.SublistItem) {
            _itemForTransportMenu.value = item
            _showGoalTransportMenu.value = true
        } else {
            resultListener.showSnackbar("Транспорт доступний тільки для цілей та під-проектів", null)
        }
    }

    fun onDismissGoalTransportMenu() {
        _showGoalTransportMenu.value = false
        _itemForTransportMenu.value = null
    }

    // НОВА ФУНКЦІЯ: Універсальна обробка дій над елементами. Замінює старий onGoalActionSelected
    fun onItemActionSelected(actionType: GoalActionType, item: ListItemContent) {
        when (item) {
            is ListItemContent.GoalItem -> {
                resultListener.setPendingAction(
                    actionType,
                    itemIds = setOf(item.listItem.id),
                    goalIds = setOf(item.goal.id),
                )
            }
            is ListItemContent.SublistItem -> {
                when (actionType) {
                    GoalActionType.CreateInstance -> {
                        resultListener.showSnackbar("Дія 'Створити посилання' недоступна для під-проектів.", null)
                        return
                    }
                    else -> {
                        resultListener.setPendingAction(
                            actionType,
                            itemIds = setOf(item.listItem.id),
                            goalIds = setOf(item.project.id), // Для MoveInstance це project.id, для CopyGoal це також project.id
                        )
                    }
                }
            }
            else -> {
                resultListener.showSnackbar("Ця дія недоступна для даного типу елемента.", null)
                return
            }
        }
        onDismissGoalActionDialogs()
    }

    fun undoDelete() {
        scope.launch {
            recentlyDeletedItems?.let { itemsToRestore ->
                val listItemsToRestore = itemsToRestore.map { it.listItem }
                projectRepository.restoreListItems(listItemsToRestore)
                resultListener.forceRefresh()
            }
            recentlyDeletedItems = null
        }
    }
}