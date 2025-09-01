package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.InboxRecord

@Composable
fun InboxScreen(
    records: List<InboxRecord>,
    onDelete: (String) -> Unit,
    onPromoteToGoal: (InboxRecord) -> Unit,
    onRecordClick: (InboxRecord) -> Unit, // Для редагування
    onCopy: (String) -> Unit // <-- ДОДАНО: Нова дія для копіювання
) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Інбокс порожній", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(records, key = { it.id }) { record ->
                InboxItemRow(
                    record = record,
                    onDelete = { onDelete(record.id) },
                    onPromoteToGoal = { onPromoteToGoal(record) },
                    onClick = { onRecordClick(record) },
                    onCopy = { onCopy(record.text) } // <-- ДОДАНО
                )
                HorizontalDivider() // Оновлено з Divider()
            }
        }
    }
}

@Composable
fun InboxItemRow(
    record: InboxRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPromoteToGoal: () -> Unit,
    onCopy: () -> Unit // <-- ДОДАНО
) {
    ListItem(
        headlineContent = { Text(record.text) },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            Row {
                // Дія 1: Перемістити в список
                IconButton(onClick = onPromoteToGoal) {
                    Icon(Icons.Outlined.MoveUp, contentDescription = "Перемістити в список")
                }
                // Дія 2: Копіювати (нова)
                IconButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Копіювати текст")
                }
                // Дія 3: Видалити
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Видалити")
                }
            }
        }
    )
}