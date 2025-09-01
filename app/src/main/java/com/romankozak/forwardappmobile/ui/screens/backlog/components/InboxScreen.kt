package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InboxScreen(
    records: List<InboxRecord>,
    onDelete: (String) -> Unit,
    onPromoteToGoal: (InboxRecord) -> Unit,
    onRecordClick: (InboxRecord) -> Unit,
    onCopy: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ваш інбокс порожній",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    InboxItemRow(
                        record = record,
                        onDelete = {
                            onDelete(record.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Запис видалено")
                            }
                        },
                        onPromoteToGoal = {
                            onPromoteToGoal(record)
                            scope.launch {
                                snackbarHostState.showSnackbar("Переміщено до цілей")
                            }
                        },
                        onClick = { onRecordClick(record) },
                        onCopy = {
                            onCopy(record.text)
                            scope.launch {
                                snackbarHostState.showSnackbar("Текст скопійовано")
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
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
    onCopy: () -> Unit
) {
    // Форматтер для дати
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    // Стан для розгортання/згортання тексту
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isExpanded = !isExpanded // Перемикаємо стан при натисканні
            }
            .animateContentSize()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = record.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Зменшено відстань
                ) {
                    // Показуємо "Більше/Менше" для довгих текстів
                    if (record.text.length > 100) {
                        Text(
                            text = if (isExpanded) "Менше" else "Більше",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 8.dp) // Відступ зверху
                        )
                    }
                    // Кнопки та дата в одному рядку
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопки зліва
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onPromoteToGoal,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.MoveUp,
                                    contentDescription = "Перемістити до списку цілей",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Скопіювати текст запису",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Видалити запис",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Дата справа
                        record.createdAt?.let {
                            Text(
                                text = formatter.format(Instant.ofEpochMilli(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false) // Вирівнювання справа
                            )
                        }
                    }
                }
            },
            trailingContent = null // Завжди null, кнопки переміщено вниз
        )
    }
}