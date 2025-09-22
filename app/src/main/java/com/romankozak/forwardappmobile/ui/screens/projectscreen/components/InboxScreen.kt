package com.romankozak.forwardappmobile.ui.screens.projectscreen.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import kotlinx.coroutines.CoroutineScope
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
    onPromoteToAnotherList: (InboxRecord) -> Unit,
    onRecordClick: (InboxRecord) -> Unit,
    onCopy: (String) -> Unit,
    listState: LazyListState,
    highlightedRecordId: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var lastItemHeight by remember { mutableStateOf(0) }


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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Порожній інбокс",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Ваш інбокс порожній",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                /*contentPadding = PaddingValues(
                    top = 12.dp,
                    start = 12.dp,
                    end = 12.dp
                ),*/
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        onPromoteToAnotherList = { onPromoteToAnotherList(record) },
                        onEdit = { onRecordClick(record) },
                        onCopy = {
                            onCopy(record.text)
                            scope.launch {
                                snackbarHostState.showSnackbar("Текст скопійовано")
                            }
                        },
                        listState = listState,
                        index = index,
                        scope = scope,
                        onHeightMeasured = if (isLast) { height -> lastItemHeight = height } else null

                    )
                }

                // 🔹 Невидимий контейнер внизу для коректного розкриття останнього елемента
                item {
                    Spacer(modifier = Modifier.height(((lastItemHeight / 2)*0.45).dp))
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
    onPromoteToAnotherList: () -> Unit,
    onCopy: () -> Unit,
    listState: LazyListState,
    index: Int,
    scope: CoroutineScope,
    onHeightMeasured: ((Int) -> Unit)? = null // 🔹 новий параметр

) {
    val formatter =
        DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())

    var isExpanded by remember { mutableStateOf(false) }
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

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            scope.launch {
                //delay(200) // невелика затримка, щоб анімація висоти відпрацювала
//                bringIntoViewRequester.bringIntoView()
                //listState.animateScrollToItem(index, scrollOffset = -50)
                /*delay(200)
                listState.animateScrollBy(200f)*/



            }
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            )
            .onGloballyPositioned { layoutCoordinates ->
                onHeightMeasured?.invoke(layoutCoordinates.size.height) // передаємо висоту в px
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester) // 🔑 ось тут
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Основний текст запису
            Text(
                text = record.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Рядок під текстом з датою
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatter.format(Instant.ofEpochMilli(record.createdAt)),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ),
                        maxLines = 1,
                    )
                }
            }

            // 3. Панель з кнопками дій
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Ліва група кнопок
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SimpleIconButton(
                                icon = Icons.Outlined.Edit,
                                contentDescription = "Редагувати запис",
                                onClick = onEdit,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            SimpleIconButton(
                                icon = Icons.Outlined.MoveUp,
                                contentDescription = "Перемістити до цілей",
                                onClick = onPromoteToGoal,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            SimpleIconButton(
                                icon = Icons.Outlined.CallMade,
                                contentDescription = "Перемістити до іншого списку",
                                onClick = onPromoteToAnotherList,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Права група кнопок
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SimpleIconButton(
                                icon = Icons.Outlined.ContentCopy,
                                contentDescription = "Скопіювати текст",
                                onClick = onCopy,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            SimpleIconButton(
                                icon = Icons.Outlined.Delete,
                                contentDescription = "Видалити запис",
                                onClick = onDelete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 4. Іконка розгортання/згортання
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
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
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .then(modifier),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}