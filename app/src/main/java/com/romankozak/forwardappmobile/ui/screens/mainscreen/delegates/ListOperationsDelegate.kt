package com.romankozak.forwardappmobile.ui.screens.mainscreen.delegates

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.GoalListUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.collections.plus

class ListOperationsDelegate(
    private val goalRepository: GoalRepository,
    private val savedStateHandle: SavedStateHandle,
    private val uiEventChannel: Channel<GoalListUiEvent>
) {
    companion object {
        private const val LIST_BEING_MOVED_ID_KEY = "listBeingMovedId"
    }

    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _listChooserFilterText = MutableStateFlow("")

    private val listBeingMovedId = savedStateHandle.getStateFlow<String?>(LIST_BEING_MOVED_ID_KEY, null)

    val listChooserUserExpandedIds = _listChooserUserExpandedIds.asStateFlow()
    val listChooserFilterText = _listChooserFilterText.asStateFlow()

    suspend fun addNewList(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return

        goalRepository.createGoalListWithId(id, name, parentId)
        if (parentId != null) {
            val allLists = goalRepository.getAllGoalListsFlow().first()
            val parentList = allLists.find { it.id == parentId }
            if (parentList != null && !parentList.isExpanded) {
                goalRepository.updateGoalList(parentList.copy(isExpanded = true))
            }
        }
    }

    suspend fun onDeleteListConfirmed(list: GoalList, childMap: Map<String, List<GoalList>>) {
        val listsToDelete = findDescendantsForDeletion(list.id, childMap)
        goalRepository.deleteListsAndSubLists(listOf(list) + listsToDelete)
    }

    suspend fun onMoveListRequest(list: GoalList, allListsFlat: StateFlow<List<GoalList>>) {
        savedStateHandle[LIST_BEING_MOVED_ID_KEY] = list.id

        val title = "Перемістити '${list.name}'"
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val allLists = allListsFlat.first()
        val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
        val descendantIds = getDescendantIds(list.id, childMap).joinToString(",")
        val currentParentId = list.parentId ?: "root"
        val disabledIds = "${list.id}${if (descendantIds.isNotEmpty()) ",$descendantIds" else ""}"
        val route = "list_chooser_screen/$encodedTitle?currentParentId=$currentParentId&disabledIds=$disabledIds"

        uiEventChannel.send(GoalListUiEvent.Navigate(route))
    }

    suspend fun onListChooserResult(newParentId: String?, allListsFlat: StateFlow<List<GoalList>>) {
        val listIdToMove = listBeingMovedId.value ?: return
        val listToMove = allListsFlat.value.find { it.id == listIdToMove } ?: return
        val finalNewParentId = if (newParentId == "root") null else newParentId

        if (listToMove.parentId == finalNewParentId) {
            savedStateHandle[LIST_BEING_MOVED_ID_KEY] = null
            return
        }

        goalRepository.moveGoalList(listToMove, finalNewParentId)

        if (finalNewParentId != null) {
            val parentList = allListsFlat.value.find { it.id == finalNewParentId }
            if (parentList != null && !parentList.isExpanded) {
                goalRepository.updateGoalList(parentList.copy(isExpanded = true))
            }
        }

        savedStateHandle[LIST_BEING_MOVED_ID_KEY] = null
    }

    suspend fun onListReorder(
        fromId: String,
        toId: String,
        position: DropPosition,
        isSearchActive: Boolean,
        allListsFlat: StateFlow<List<GoalList>>
    ) {
        if (fromId == toId || isSearchActive) return

        withContext(Dispatchers.IO) {
            val allLists = allListsFlat.first()
            val fromList = allLists.find { it.id == fromId }
            val toList = allLists.find { it.id == toId }

            if (fromList == null || toList == null || fromList.parentId != toList.parentId) return@withContext

            val parentId = fromList.parentId
            val siblings = allLists.filter { it.parentId == parentId }.sortedBy { it.order }
            val mutableSiblings = siblings.toMutableList()

            val fromIndex = mutableSiblings.indexOfFirst { it.id == fromId }
            val toIndex = mutableSiblings.indexOfFirst { it.id == toId }

            if (fromIndex == -1 || toIndex == -1) return@withContext

            val movedItem = mutableSiblings.removeAt(fromIndex)
            val insertionIndex = when {
                fromIndex < toIndex -> if (position == DropPosition.BEFORE) toIndex - 1 else toIndex
                else -> if (position == DropPosition.BEFORE) toIndex else toIndex + 1
            }
            val finalIndex = insertionIndex.coerceIn(0, mutableSiblings.size)
            mutableSiblings.add(finalIndex, movedItem)

            val listsToUpdate = mutableSiblings.mapIndexed { index, list ->
                list.copy(order = index.toLong(), updatedAt = System.currentTimeMillis())
            }
            goalRepository.updateGoalLists(listsToUpdate)
        }
    }

    fun onListChooserFilterChanged(text: String) {
        _listChooserFilterText.value = text
    }

    fun onListChooserToggleExpanded(listId: String) {
        val currentIds = _listChooserUserExpandedIds.value.toMutableSet()
        if (listId in currentIds) currentIds.remove(listId) else currentIds.add(listId)
        _listChooserUserExpandedIds.value = currentIds
    }

    fun resetChooserState() {
        _listChooserFilterText.value = ""
        _listChooserUserExpandedIds.value = emptySet()
    }

    private fun findDescendantsForDeletion(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        visited: MutableSet<String> = mutableSetOf(),
    ): List<GoalList> {
        if (!visited.add(listId)) return emptyList()
        val children = childMap[listId] ?: emptyList()
        return children + children.flatMap { findDescendantsForDeletion(it.id, childMap, visited) }
    }

    private fun getDescendantIds(listId: String, childMap: Map<String, List<GoalList>>): Set<String> {
        val descendants = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(listId)
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            childMap[currentId]?.forEach { child ->
                descendants.add(child.id)
                queue.add(child.id)
            }
        }
        return descendants
    }
}