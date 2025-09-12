package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import android.util.Log
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
    private val resultListener: ResultListener,
) {
    private val TAG = "SelectionHandler_DEBUG"

    interface ResultListener : BaseHandlerResultListener {
        fun updateSelectionState(selectedIds: Set<String>)
    }

    fun toggleSelection(
        itemId: String,
        currentSelection: Set<String>,
    ) {
        val newSelection = currentSelection.toMutableSet()
        if (itemId in newSelection) {
            newSelection.remove(itemId)
        } else {
            newSelection.add(itemId)
        }
        resultListener.updateSelectionState(newSelection)
    }

    fun selectAllItems() {
        val itemsToSelect =
            listContentFlow.value
                .filterNot { it is ListItemContent.LinkItem }
                .map { it.item.id }
                .toSet()
        resultListener.updateSelectionState(itemsToSelect)
    }

    fun clearSelection() {
        Log.i(TAG, "ВИКЛИК: clearSelection()")
        resultListener.updateSelectionState(emptySet())
    }

    fun markSelectedAsComplete(selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) return

        Log.d(TAG, "ДІЯ: markSelectedAsComplete для ${selectedIds.size} елементів.")

        val goalsToUpdate =
            listContentFlow.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }
                .distinctBy { it.id }

        clearSelection()

        if (goalsToUpdate.isNotEmpty()) {
            scope.launch {
                val updatedGoals = goalsToUpdate.map { it.copy(completed = true, updatedAt = System.currentTimeMillis()) }
                goalRepository.updateGoals(updatedGoals)
                resultListener.showSnackbar("Позначено як виконані: ${goalsToUpdate.size}", null)
                resultListener.forceRefresh()
            }
        }
    }

    fun markSelectedAsIncomplete(selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) return

        Log.d(TAG, "ДІЯ: markSelectedAsIncomplete для ${selectedIds.size} елементів.")

        val goalsToUpdate =
            listContentFlow.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }
                .distinctBy { it.id }

        clearSelection()

        if (goalsToUpdate.isNotEmpty()) {
            scope.launch {
                val updatedGoals = goalsToUpdate.map { it.copy(completed = false, updatedAt = System.currentTimeMillis()) }
                goalRepository.updateGoals(updatedGoals)
                resultListener.showSnackbar("Знято позначку виконання: ${goalsToUpdate.size}", null)
                resultListener.forceRefresh()
            }
        }
    }

    fun onBulkActionRequest(
        actionType: GoalActionType,
        selectedIds: Set<String>,
    ) {
        if (selectedIds.isEmpty()) return

        val sourceGoalIds =
            listContentFlow.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { it.item.entityId }
                .toSet()

        resultListener.setPendingAction(actionType, selectedIds, sourceGoalIds)
    }

    fun deleteSelectedItems(selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) return
        Log.d(TAG, "ДІЯ: deleteSelectedItems для ${selectedIds.size} елементів.")

        clearSelection()

        scope.launch {
            goalRepository.deleteListItems(selectedIds.toList())
            resultListener.showSnackbar("Видалено елементів: ${selectedIds.size}", "Скасувати")
        }
    }
}
