package com.romankozak.forwardappmobile.ui.screens.backlog.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color 
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "INBOX_UI_DEBUG"

@Composable
fun InboxScreen(
    records: List<InboxRecord>,
    onDelete: (String) -> Unit,
    onPromoteToGoal: (InboxRecord) -> Unit,
    onRecordClick: (InboxRecord) -> Unit,
    onCopy: (String) -> Unit,
    listState: LazyListState,
    highlightedRecordId: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Ваш інбокс порожній",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            ) {
                items(records, key = { it.id }) { record ->
                    val isHighlighted = record.id == highlightedRecordId
                    if (isHighlighted) {
                        Log.d(TAG, "Item with ID ${record.id} is being marked for highlighting.")
                    }
                    InboxItemRow(
                        record = record,
                        isHighlighted = isHighlighted,
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
                        onEdit = { onRecordClick(record) },
                        onCopy = {
                            onCopy(record.text)
                            scope.launch {
                                snackbarHostState.showSnackbar("Текст скопійовано")
                            }
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                }
            }
        }
    }
}

@Composable
fun InboxItemRow(
    record: InboxRecord,
    isHighlighted: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPromoteToGoal: () -> Unit,
    onCopy: () -> Unit,
) {
    val formatter =
        DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())

    var isExpanded by remember { mutableStateOf(false) }

    var highlightActive by remember { mutableStateOf(isHighlighted) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            Log.d(TAG, "InboxItemRow (ID: ${record.id}) received highlight=true. Starting animation.")
            highlightActive = true
            delay(2500L)
            highlightActive = false
            Log.d(TAG, "InboxItemRow (ID: ${record.id}) highlight animation finished.")
        }
    }

    val highlightColor = Color.Yellow.copy(alpha = 0.4f)

    val containerColor by animateColorAsState(
        targetValue = if (highlightActive) highlightColor else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "highlight_color_animation",
    )
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded
                }.animateContentSize()
                .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        ListItem(
            colors =
                ListItemDefaults.colors(
                    containerColor = containerColor,
                ),
            headlineContent = {
                Text(
                    text = record.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (record.text.length > 100) {
                        Text(
                            text = if (isExpanded) "Менше" else "Більше",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(top = 8.dp),
                        )
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Редагувати запис",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = onPromoteToGoal,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.MoveUp,
                                    contentDescription = "Перемістити до списку цілей",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Скопіювати текст запису",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Видалити запис",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Text(
                            text = formatter.format(Instant.ofEpochMilli(record.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            },
            trailingContent = null,
        )
    }
}
