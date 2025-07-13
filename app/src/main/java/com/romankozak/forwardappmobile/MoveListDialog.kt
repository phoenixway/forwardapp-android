package com.romankozak.forwardappmobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MoveListDialog(
    listToMove: GoalList,
    allListsFlat: List<GoalList>,
    topLevelLists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    onDismiss: () -> Unit,
    onConfirmMove: (newParentId: String?) -> Unit
) {
    var filterText by remember { mutableStateOf("") }

    // Логіка фільтрації для показу відповідних списків при пошуку
    val filteredAndVisibleLists = remember(filterText, allListsFlat, topLevelLists) {
        if (filterText.isBlank()) {
            topLevelLists to allListsFlat.map { it.id }.toSet()
        } else {
            val lowercasedFilter = filterText.lowercase()
            val allListsById = allListsFlat.associateBy { it.id }
            val matchingIds = allListsFlat
                .filter { it.name.lowercase().contains(lowercasedFilter) }
                .map { it.id }
                .toSet()
            val visibleIds = matchingIds.toMutableSet()
            matchingIds.forEach { id ->
                var currentId: String? = id
                while (currentId != null && allListsById[currentId]?.parentId != null) {
                    val parentId = allListsById[currentId]!!.parentId!!
                    visibleIds.add(parentId)
                    currentId = parentId
                }
            }
            val filteredTops = topLevelLists.filter { it.id in visibleIds }
            filteredTops to visibleIds
        }
    }

    val listsToShow = filteredAndVisibleLists.first
    val visibleIds = filteredAndVisibleLists.second

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Перемістити '${listToMove.name}' до:", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("Фільтр списків...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    item {
                        TextButton(onClick = { onConfirmMove(null) }) {
                            Text("-> (Верхній рівень)")
                        }
                    }
                    items(listsToShow, key = { "selectable-${it.id}" }) { list ->
                        SelectableRecursiveListItem(list, childMap, 0, listToMove.id, visibleIds) { selectedParentId ->
                            onConfirmMove(selectedParentId)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text("Скасувати") }
                }
            }
        }
    }
}

@Composable
private fun SelectableRecursiveListItem(
    list: GoalList,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    idToDisable: String,
    visibleIds: Set<String>,
    onSelect: (String) -> Unit
) {
    // Не можна перемістити список в самого себе або у своїх нащадків
    val isEnabled = list.id != idToDisable && !isDescendant(list.id, idToDisable, childMap)

    if (list.id in visibleIds) {
        Column {
            TextButton(
                onClick = { onSelect(list.id) },
                enabled = isEnabled,
                modifier = Modifier.padding(start = (level * 24).dp)
            ) {
                Text(
                    text = list.name,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            val children = childMap[list.id] ?: emptyList()
            for (child in children) {
                SelectableRecursiveListItem(child, childMap, level + 1, idToDisable, visibleIds, onSelect)
            }
        }
    }
}

private fun isDescendant(
    potentialChildId: String,
    targetParentId: String,
    childMap: Map<String, List<GoalList>>
): Boolean {
    val children = childMap[targetParentId] ?: return false
    if (children.any { it.id == potentialChildId }) return true
    return children.any { isDescendant(potentialChildId, it.id, childMap) }
}