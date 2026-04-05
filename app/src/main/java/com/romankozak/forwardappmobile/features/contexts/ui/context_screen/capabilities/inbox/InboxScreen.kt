package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    isSelectionMode: Boolean,
    selectedRecordIds: Set<String>,
    canPaste: Boolean,
    onToggleSelection: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCopySelected: () -> Int,
    onCutSelected: () -> Int,
    onPaste: ((Int) -> Unit) -> Unit,
    listState: LazyListState,
    highlightedRecordId: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var lastItemHeight by remember { mutableStateOf(0) }

    LaunchedEffect(highlightedRecordId, records) {
        val recordId = highlightedRecordId ?: return@LaunchedEffect
        val index =
            snapshotFlow { records.indexOfFirst { it.id == recordId } }
                .map { it }
                .filter { it >= 0 }
                .first()
        listState.scrollToItem(index)
        listState.animateScrollToItem(index)
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                InboxSelectionTopBar(
                    selectedCount = selectedRecordIds.size,
                    areAllSelected = records.isNotEmpty() && selectedRecordIds.size == records.size,
                    canPaste = canPaste,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll,
                    onCopySelected = {
                        val copied = onCopySelected()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (copied > 0) "Скопійовано записів: $copied" else "Немає вибраних записів",
                            )
                        }
                    },
                    onCutSelected = {
                        val cut = onCutSelected()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (cut > 0) "Вирізано записів: $cut" else "Немає вибраних записів",
                            )
                        }
                    },
                    onPaste = {
                        onPaste { insertedCount ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (insertedCount > 0) {
                                        "Додано записів в інбокс: $insertedCount"
                                    } else {
                                        "Немає валідних елементів для вставки"
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Порожній інбокс",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "Ваш інбокс порожній",
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
            ) {
                itemsIndexed(records, key = { _, item -> item.id }) { index, record ->
                    val isLast = index == records.lastIndex

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
                        isSelectionMode = isSelectionMode,
                        isSelected = record.id in selectedRecordIds,
                        onToggleSelection = { onToggleSelection(record.id) },
                        onLongPress = { onLongPress(record.id) },
                        listState = listState,
                        index = index,
                        scope = scope,
                        onHeightMeasured = if (isLast) { height -> lastItemHeight = height } else null,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(((lastItemHeight / 2) * 0.45).dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InboxItemRow(
    record: InboxRecord,
    isHighlighted: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPromoteToGoal: () -> Unit,
    onCopy: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    listState: LazyListState,
    index: Int,
    scope: CoroutineScope,
    onHeightMeasured: ((Int) -> Unit)? = null,
) {
    val formatter =
        DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())

    var highlightActive by remember { mutableStateOf(isHighlighted) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            highlightActive = true
            delay(2500L)
            highlightActive = false
        }
    }

    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    val normalColor = MaterialTheme.colorScheme.surface
    val containerColor by animateColorAsState(
        targetValue = if (highlightActive) highlightColor else normalColor,
        animationSpec = tween(durationMillis = 500),
        label = "highlight_color_animation",
    )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelection()
                        } else {
                            onEdit()
                        }
                    },
                    onLongClick = onLongPress,
                )
                .animateContentSize()
                .border(
                    width = 1.dp,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        },
                    shape = MaterialTheme.shapes.medium,
                )
                .onGloballyPositioned { layoutCoordinates ->
                    onHeightMeasured?.invoke(layoutCoordinates.size.height)
                },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = "Вибрати запис",
                        )
                    }
                }
            }
            Text(
                text = record.text,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatter.format(Instant.ofEpochMilli(record.createdAt)),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        ),
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (!isSelectionMode) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SimpleIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Редагувати запис",
                            onClick = onEdit,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        SimpleIconButton(
                            icon = Icons.Outlined.MoveUp,
                            contentDescription = "Перемістити до цілей",
                            onClick = onPromoteToGoal,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SimpleIconButton(
                            icon = Icons.Outlined.ContentCopy,
                            contentDescription = "Скопіювати текст",
                            onClick = onCopy,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        SimpleIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Видалити запис",
                            onClick = onDelete,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxSelectionTopBar(
    selectedCount: Int,
    areAllSelected: Boolean,
    canPaste: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCopySelected: () -> Unit,
    onCutSelected: () -> Unit,
    onPaste: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(6.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Закрити режим вибору",
                )
            }

            Text(
                text = "$selectedCount вибрано",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onSelectAll,
                enabled = !areAllSelected,
            ) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = "Вибрати все",
                )
            }

            IconButton(onClick = onCopySelected) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Копіювати вибране",
                )
            }

            IconButton(onClick = onCutSelected) {
                Icon(
                    imageVector = Icons.Filled.ContentCut,
                    contentDescription = "Вирізати вибране",
                )
            }

            IconButton(
                onClick = onPaste,
                enabled = canPaste,
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = "Вставити",
                )
            }
        }
    }
}

@Composable
fun SimpleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(36.dp)
                .then(modifier),
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = tint,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}
