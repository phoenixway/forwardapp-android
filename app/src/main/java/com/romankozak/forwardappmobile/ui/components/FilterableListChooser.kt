// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/components/FilterableListChooser.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.GoalList
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun FilterableListChooser(
    title: String,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    topLevelLists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String> = emptySet(),
    onAddNewList: (id: String, parentId: String?, name: String) -> Unit
) {
    var isCreatingMode by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var parentForNewList by remember { mutableStateOf<GoalList?>(null) }
    var highlightedListId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(highlightedListId) {
        if (highlightedListId != null) {
            delay(2000L)
            highlightedListId = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                val currentTitle = if (isCreatingMode) {
                    parentForNewList?.let { "Новий підсписок для '${it.name}'" } ?: "Новий список верхнього рівня"
                } else {
                    title
                }
                Text(currentTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                if (isCreatingMode) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Назва нового списку...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { isCreatingMode = false }) { Text("Назад") }
                        Button(
                            onClick = {
                                val newId = UUID.randomUUID().toString()
                                onAddNewList(newId, parentForNewList?.id, newListName)
                                highlightedListId = newId
                                isCreatingMode = false
                            },
                            enabled = newListName.isNotBlank()
                        ) {
                            Text("Створити")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = onFilterTextChanged,
                        label = { Text("Фільтр списків...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        if (filterText.isBlank()) {
                            item {
                                val isAlreadyAtRoot = currentParentId == null
                                SelectableRootItem(
                                    isEnabled = !isAlreadyAtRoot,
                                    onSelect = {
                                        onConfirm(null)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                        items(topLevelLists, key = { it.id }) { list ->
                            RecursiveSelectableListItem(
                                list = list,
                                childMap = childMap,
                                level = 0,
                                expandedIds = expandedIds,
                                onToggleExpanded = onToggleExpanded,
                                onSelect = { selectedId ->
                                    onConfirm(selectedId)
                                    onDismiss()
                                },
                                disabledIds = disabledIds,
                                highlightedListId = highlightedListId,
                                onAddSublistRequest = { parent ->
                                    parentForNewList = parent
                                    isCreatingMode = true
                                    newListName = ""
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            parentForNewList = null
                            isCreatingMode = true
                            newListName = ""
                        }) {
                            Text("Створити новий")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Скасувати") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableRootItem(
    isEnabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Корінь (верхній рівень)",
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun RecursiveSelectableListItem(
    list: GoalList,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    disabledIds: Set<String>,
    highlightedListId: String?,
    onAddSublistRequest: (parentList: GoalList) -> Unit
) {
    val isExpanded = list.id in expandedIds
    val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isEnabled = list.id !in disabledIds

    val isHighlighted = list.id == highlightedListId

    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        label = "highlight-animation"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .background(backgroundColor, RoundedCornerShape(4.dp))
                .clickable(enabled = isEnabled) { onSelect(list.id) }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggleExpanded(list.id) }
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = list.name,
                modifier = Modifier.weight(1f),
                color = if (isEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (isEnabled) {
                IconButton(onClick = { onAddSublistRequest(list) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Додати підсписок до ${list.name}"
                    )
                }
            }
        }
        if (isExpanded && hasChildren) {
            for (child in children) {
                RecursiveSelectableListItem(
                    list = child,
                    childMap = childMap,
                    level = level + 1,
                    expandedIds = expandedIds,
                    onToggleExpanded = onToggleExpanded,
                    onSelect = onSelect,
                    disabledIds = disabledIds,
                    highlightedListId = highlightedListId,
                    onAddSublistRequest = onAddSublistRequest
                )
            }
        }
    }
}