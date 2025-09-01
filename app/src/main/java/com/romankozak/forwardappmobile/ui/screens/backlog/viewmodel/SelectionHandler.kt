package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SelectionHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listContentFlow: StateFlow<List<ListItemContent>>,
    private val resultListener: ResultListener
) {

    interface ResultListener : BaseHandlerResultListener {
        fun updateSelectionState(selectedIds: Set<String>)
    }

    fun toggleSelection(itemId: String, currentSelection: Set<String>) {
        val newSelection = currentSelection.toMutableSet()
        if (itemId in newSelection) newSelection.remove(itemId)
        else newSelection.add(itemId)
        resultListener.updateSelectionState(newSelection)
    }

    fun selectAllItems() {
        val itemsToSelect = listContentFlow.value
            .filterNot { it is ListItemContent.LinkItem }
            .map { it.item.id }
            .toSet()
        resultListener.updateSelectionState(itemsToSelect)
    }

    fun clearSelection() {
        resultListener.updateSelectionState(emptySet())
    }
    fun toggleCompletionForSelectedGoals(selectedIds: Set<String>) {
        scope.launch {
            if (selectedIds.isEmpty()) return@launch
            val goalsToUpdate = listContentFlow.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }
                .distinctBy { it.id }

            if (goalsToUpdate.isNotEmpty()) {
                val areAllCompleted = goalsToUpdate.all { it.completed }
                val targetState = !areAllCompleted
                val updatedGoals = goalsToUpdate.map { it.copy(completed = targetState, updatedAt = System.currentTimeMillis()) }
                goalRepository.updateGoals(updatedGoals)
            }
            clearSelection()
            resultListener.forceRefresh()
        }
    }

    fun onBulkActionRequest(actionType: GoalActionType, selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) return

        val sourceGoalIds = listContentFlow.value
            .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
            .map { it.item.entityId }
            .toSet()

        resultListener.setPendingAction(actionType, selectedIds, sourceGoalIds)
    }

    fun deleteSelectedItems(selectedIds: Set<String>) {
        scope.launch {
            if (selectedIds.isEmpty()) return@launch
            goalRepository.deleteListItems(selectedIds.toList())
            clearSelection()
            resultListener.showSnackbar("Видалено елементів: ${selectedIds.size}", "Скасувати")
        }
    }
}