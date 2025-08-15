// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/components/FilterableListChooser.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.GoalList

/**
 * Універсальний діалог для вибору списку з ієрархії з можливістю фільтрації.
 * Логіка фільтрації та стану повністю керується з ViewModel.
 */
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
    onConfirm: (String) -> Unit,
    disabledIds: Set<String> = emptySet()
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)

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
                    items(topLevelLists, key = { it.id }) { list ->
                        RecursiveSelectableListItem(
                            list = list,
                            childMap = childMap,
                            level = 0,
                            expandedIds = expandedIds,
                            onToggleExpanded = onToggleExpanded,
                            onSelect = { selectedId ->
                                onConfirm(selectedId)
                                onDismiss() // Автоматично закриваємо після вибору
                            },
                            disabledIds = disabledIds
                        )
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
private fun RecursiveSelectableListItem(
    list: GoalList,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    disabledIds: Set<String>
) {
    val isExpanded = list.id in expandedIds
    val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isEnabled = list.id !in disabledIds

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clickable(enabled = isEnabled) { onSelect(list.id) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✨ ВИПРАВЛЕНО: Іконка рендериться тільки якщо є дочірні елементи.
            // Для елементів без дітей використовується Spacer для збереження вирівнювання.
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
                color = if (isEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
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
                    disabledIds = disabledIds
                )
            }
        }
    }
}