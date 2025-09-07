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

    // --- ЗМІНЕНО ---
    // Метод toggleCompletionForSelectedGoals видалено.
    // Додано два нові методи з чіткою дією.

    /**
     * Позначає всі виділені цілі як виконані, незалежно від їхнього поточного статусу.
     */
    fun markSelectedAsComplete(selectedIds: Set<String>) {
        scope.launch {
            if (selectedIds.isEmpty()) return@launch

            val goalsToUpdate = listContentFlow.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }
                .distinctBy { it.id }

            if (goalsToUpdate.isNotEmpty()) {
                val updatedGoals = goalsToUpdate.map { it.copy(completed = true, updatedAt = System.currentTimeMillis()) }
                goalRepository.updateGoals(updatedGoals)
                resultListener.showSnackbar("Позначено як виконані: ${goalsToUpdate.size}", null)
            }

            clearSelection()
            resultListener.forceRefresh()
        }
    }

    /**
     * Знімає позначку виконання з усіх виділених цілей, незалежно від їхнього поточного статусу.
     */
    fun markSelectedAsIncomplete(selectedIds: Set<String>) {
        scope.launch {
            if (selectedIds.isEmpty()) return@launch

            val goalsToUpdate = listContentFlow.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }
                .distinctBy { it.id }

            if (goalsToUpdate.isNotEmpty()) {
                val updatedGoals = goalsToUpdate.map { it.copy(completed = false, updatedAt = System.currentTimeMillis()) }
                goalRepository.updateGoals(updatedGoals)
                resultListener.showSnackbar("Знято позначку виконання: ${goalsToUpdate.size}", null)
            }

            clearSelection()
            resultListener.forceRefresh()
        }
    }
    // --- КІНЕЦЬ ЗМІН ---

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