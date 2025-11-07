@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.activitytracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.database.models.ActivityRecord
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.data.repository.uuid4
import com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.TimePickerDialog
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.formatDuration
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView

private val ActivityRecord.isTimeless: Boolean
    get() = this.startTime == null

private val ActivityRecord.isOngoing: Boolean
    get() = this.startTime != null && this.endTime == null

@Composable
fun ActivityTrackerScreen(
    navController: NavController,
    viewModel: ActivityTrackerViewModel = hiltViewModel(),
)
{
    val groupedByDate by viewModel.groupedActivityLog.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val editingRecord by viewModel.editingRecord.collectAsStateWithLifecycle()
    val recordToDelete by viewModel.recordToDelete.collectAsStateWithLifecycle()
    val isEditingLastTimedRecord by viewModel.isEditingLastTimedRecord.collectAsStateWithLifecycle()
    val recordForReminder by viewModel.recordForReminder.collectAsStateWithLifecycle()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showMatrixSplash by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ActivityTrackerTopAppBar(
                    onNavigateBack = { navController.popBackStack() },
                    onClearLogRequest = { showClearConfirmDialog = true },
                    onExportRequest = {
                        val markdown = exportLogToMarkdown(groupedByDate.values.flatten())
                        copyToClipboard(context, markdown)
                    },
                )
            },
            bottomBar = {
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    val indicatorState = remember { com.romankozak.forwardappmobile.ui.shared.InProgressIndicatorState(isInitiallyExpanded = true) }
                    InProgressIndicator(
                        ongoingActivity = lastOngoingActivity,
                        onStopClick = viewModel::onToggleStartStop,
                        onReminderClick = { lastOngoingActivity?.let { viewModel.onSetReminder(it) } },
                        onIndicatorClick = { },
                        indicatorState = indicatorState
                    )
                    ActivityInputBar(
                        text = inputText,
                        isActivityOngoing = lastOngoingActivity != null,
                        onTextChange = viewModel::onInputTextChanged,
                        onToggleStartStop = viewModel::onToggleStartStop,
                        onTimelessClick = viewModel::onTimelessRecordClick,
                    )
                }
            },
        ) { paddingValues ->
            ActivityLog(
                groupedByDate = groupedByDate,
                modifier = Modifier.padding(paddingValues),
                onEdit = viewModel::onEditRequest,
                onRestart = viewModel::onRestartActivity,
                onDelete = viewModel::onDeleteRequest,
                onSetReminder = viewModel::onSetReminder,
            )

            editingRecord?.let { recordToEdit ->
                EditRecordDialog(
                    record = recordToEdit,
                    onDismiss = viewModel::onEditDialogDismiss,
                    onConfirm = viewModel::onRecordUpdated,
                    isLastTimedRecord = isEditingLastTimedRecord,
                )
            }

            recordToDelete?.let { record ->
                AlertDialog(
                    onDismissRequest = viewModel::onDeleteDismiss,
                    title = { Text("Видалити запис?") },
                    text = { Text("Ви впевнені, що хочете видалити запис: \"${record.text}\"?") },
                    confirmButton = { Button(onClick = viewModel::onDeleteConfirm) { Text("Видалити") } },
                    dismissButton = { TextButton(onClick = viewModel::onDeleteDismiss) { Text("Скасувати") } },
                )
            }

            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = { Text("Очистити лог?") },
                    text = { Text("Ви впевнені, що хочете видалити всі записи? Цю дію неможливо буде скасувати.") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.onClearLogConfirm()
                            showClearConfirmDialog = false
                        }) { Text("Видалити") }
                    },
                    dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("Скасувати") } },
                )
            }

            recordForReminder?.let { record ->
                ReminderPropertiesDialog(
                    onDismiss = viewModel::onReminderDialogDismiss,
onSetReminder = { time -> viewModel.onSetReminder(time) },
                    onRemoveReminder = if (record.reminderTime != null) { { viewModel.onClearReminder() } } else null,
                    currentReminders =
                        listOfNotNull(record.reminderTime).map {
                            Reminder(
                                id = uuid4(),
                                entityId = record.id,
                                entityType = "TASK",
                                reminderTime = it,
                                status = "SCHEDULED",
                                creationTime = System.currentTimeMillis(),
                                snoozeUntil = null,
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun ActivityTrackerTopAppBar(
    onNavigateBack: () -> Unit,
    onClearLogRequest: () -> Unit,
    onExportRequest: () -> Unit,
)
{
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Трекер Активності") },
        navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
        actions = {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Меню") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Експорт в Markdown") }, onClick = {
                    onExportRequest()
                    menuExpanded = false
                })
                DropdownMenuItem(text = { Text("Очистити лог") }, onClick = {
                    onClearLogRequest()
                    menuExpanded = false
                })
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityLog(
    groupedByDate: Map<String, List<ActivityRecord>>,
    modifier: Modifier = Modifier,
    onEdit: (ActivityRecord) -> Unit,
    onRestart: (ActivityRecord) -> Unit,
    onDelete: (ActivityRecord) -> Unit,
    onSetReminder: (ActivityRecord) -> Unit,
)
{
    val lazyListState = rememberLazyListState()

    LaunchedEffect(groupedByDate.values.flatten().size) {
        if (groupedByDate.isNotEmpty()) {
            lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount.coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = 12.dp),
    ) {
        if (groupedByDate.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Лог порожній. Почніть першу активність!")
                }
            }
        } else {
            groupedByDate.forEach { (dateHeader, records) ->
                stickyHeader {
                    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                    }
                }
                items(records, key = { it.id }) { record ->
                    LogEntryItem(
                        record = record,
                        onEdit = onEdit,
                        onRestart = onRestart,
                        onDelete = onDelete,
                        onSetReminder = onSetReminder,
                    )
                    if (records.last() != record) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(
    record: ActivityRecord,
    onEdit: (ActivityRecord) -> Unit,
    onRestart: (ActivityRecord) -> Unit,
    onDelete: (ActivityRecord) -> Unit,
    onSetReminder: (ActivityRecord) -> Unit,
)
{
    if (record.isTimeless) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    "Нотатка",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(record.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), fontStyle = FontStyle.Italic)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    onDelete(record)
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Видалити", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = {
                    onEdit(record)
                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Редагувати", modifier = Modifier.size(18.dp)) }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            val timeText =
                when {
                    record.isOngoing -> "${timeFormat.format(Date(record.startTime!!))} - ..."
                    else -> "${timeFormat.format(Date(record.startTime!!))} - ${timeFormat.format(Date(record.endTime!!))}"
                }
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(90.dp).padding(top = 2.dp),
                fontWeight = FontWeight.SemiBold,
                color = if (record.isOngoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(12.dp))

            TextWithBadgeLayout(
                modifier = Modifier.weight(1f),
                text = record.text,
                textStyle = MaterialTheme.typography.bodyLarge,
                badge = {
                    if (!record.isOngoing && record.endTime != null) {
                        val duration = record.endTime - record.startTime!!
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        ) {
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
            )

            Row {
                if (record.isOngoing) {
                    val isReminderSet = record.reminderTime != null
                    FilledTonalIconButton(
                        onClick = { onSetReminder(record) },
                        modifier = Modifier.size(32.dp),
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isReminderSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isReminderSet) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    ) {
                        Icon(
                            imageVector = if (isReminderSet) Icons.Default.NotificationImportant else Icons.Default.Notifications,
                            contentDescription = "Встановити нагадування",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                IconButton(onClick = {
                    onRestart(record)
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Replay, "Перезапустити", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    onDelete(record)
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Видалити", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = {
                    onEdit(record)
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Редагувати", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun TextWithBadgeLayout(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle,
    badge: @Composable () -> Unit,
)
{
    val textMeasurer = rememberTextMeasurer()
    SubcomposeLayout(modifier = modifier) { constraints ->
        val badgePlaceable = subcompose("badge", badge).firstOrNull()?.measure(Constraints())
        val badgeWidth = badgePlaceable?.width ?: 0
        val badgeHeight = badgePlaceable?.height ?: 0
        val horizontalGap = if (badgeWidth > 0) 8.dp.roundToPx() else 0

        val textLayoutResult =
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = textStyle,
                constraints = constraints,
            )
        val textPlaceable =
            subcompose("text_multi_or_single") {
                Text(
                    text,
                    style = textStyle,
                    maxLines = if (textLayoutResult.lineCount > 1) Int.MAX_VALUE else 1,
                    overflow =
                        if (textLayoutResult.lineCount >
                            1
                        ) {
                            androidx.compose.ui.text.style.TextOverflow.Clip
                        } else {
                            androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        },
                )
            }.first().measure(constraints)

        val isSingleLine = textLayoutResult.lineCount <= 1

        if (isSingleLine) {
            val textPlaceableForSingleLine =
                subcompose("text_single_line") {
                    Text(
                        text,
                        style = textStyle,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }.first().measure(
                    Constraints(
                        maxWidth = (constraints.maxWidth - horizontalGap - badgeWidth).coerceAtLeast(0),
                    ),
                )

            val totalHeight = max(textPlaceableForSingleLine.height, badgeHeight)
            layout(constraints.maxWidth, totalHeight) {
                textPlaceableForSingleLine.placeRelative(
                    0,
                    Alignment.CenterVertically.align(textPlaceableForSingleLine.height, totalHeight),
                )
                badgePlaceable?.placeRelative(
                    textPlaceableForSingleLine.width + horizontalGap,
                    Alignment.CenterVertically.align(badgeHeight, totalHeight),
                )
            }
        } else {
            val verticalGap = if (badgeHeight > 0) 6.dp.roundToPx() else 0
            val totalHeight = textPlaceable.height + verticalGap + badgeHeight
            layout(constraints.maxWidth, totalHeight) {
                textPlaceable.placeRelative(0, 0)
                badgePlaceable?.placeRelative(0, textPlaceable.height + verticalGap)
            }
        }
    }
}

@Composable
private fun ActivityInputBar(
    text: String,
    isActivityOngoing: Boolean,
    onTextChange: (String) -> Unit,
    onToggleStartStop: () -> Unit,
    onTimelessClick: () -> Unit,
)
{
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Чим ви займаєтесь?") },
                singleLine = true,
                colors =
                    TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onTimelessClick,
                enabled = text.isNotBlank(),
            ) {
                Icon(Icons.Default.AddComment, "Зробити запис", tint = MaterialTheme.colorScheme.secondary)
            }

            IconButton(
                onClick = onToggleStartStop,
                enabled = text.isNotBlank() || isActivityOngoing,
            ) {
                val icon: ImageVector
                val tint: Color
                val description: String

                if (isActivityOngoing) {
                    if (text.isNotBlank()) {
                        icon = Icons.Default.Sync
                        tint = MaterialTheme.colorScheme.tertiary
                        description = "Зупинити поточну та почати нову"
                    } else {
                        icon = Icons.Default.StopCircle
                        tint = MaterialTheme.colorScheme.error
                        description = "Зупинити"
                    }
                } else {
                    icon = Icons.Default.PlayCircle
                    tint = MaterialTheme.colorScheme.primary
                    description = "Почати"
                }

                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = tint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}



private fun exportLogToMarkdown(log: List<ActivityRecord>): String {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val groupedByDate = log.sortedByDescending { it.createdAt }.groupBy { toDateHeader(it.createdAt) }
    return buildString {
        groupedByDate.forEach { (dateHeader, records) ->
            append("## $dateHeader\n\n")
            val (timeless, timed) = records.partition { it.isTimeless }
            timeless.forEach { record ->
                append("- ${record.text}\n")
            }
            timed.forEach { record ->
                val timeText =
                    when {
                        record.isOngoing -> "`${sdfTime.format(Date(record.startTime!!))} - ...`"
                        else -> "`${sdfTime.format(Date(record.startTime!!))} - ${sdfTime.format(Date(record.endTime!!))}`"
                    }
                append("- $timeText ${record.text}\n".trim())
            }
            append("\n")
        }
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
)
{
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Activity Log", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Лог скопійовано!", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRecordDialog(
    record: ActivityRecord,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Long?) -> Unit,
    isLastTimedRecord: Boolean,
)
{
    var text by remember(record) { mutableStateOf(record.text) }
    var startTime by remember(record) { mutableStateOf(record.startTime) }
    var endTime by remember(record) { mutableStateOf(record.endTime) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати запис") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Текст запису") },
                )
                if (!record.isTimeless) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(startTime?.let { timeFormatter.format(Date(it)) } ?: "Start")
                        }
                        Text("-")
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f),
                            enabled = !record.isOngoing,
                        ) {
                            Text(endTime?.let { timeFormatter.format(Date(it)) } ?: "Зараз")
                        }
                        if (isLastTimedRecord && endTime != null) {
                            IconButton(onClick = { endTime = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Зробити поточним")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isTimeInvalid = startTime != null && endTime != null && endTime!! < startTime!!
                    if (isTimeInvalid) {
                        Toast.makeText(context, "Час закінчення не може бути раніше часу початку", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(text, startTime, endTime)
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime ?: System.currentTimeMillis(),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { newTime ->
                startTime = newTime
                showStartTimePicker = false
            },
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime ?: System.currentTimeMillis(),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { newTime ->
                endTime = newTime
                showEndTimePicker = false
            },
        )
    }
}
