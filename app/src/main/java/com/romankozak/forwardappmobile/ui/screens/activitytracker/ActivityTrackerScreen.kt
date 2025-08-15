// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/activitytracker/ActivityTrackerScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.activitytracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.BorderStroke // ✨ ВИПРАВЛЕНО: Додано відсутній імпорт

@Composable
fun ActivityTrackerScreen(
    navController: NavController,
    viewModel: ActivityTrackerViewModel = hiltViewModel()
) {
    val log by viewModel.activityLog.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsState()
    val editingRecord by viewModel.editingRecord.collectAsState() // ✨ НОВИЙ СТАН
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            ActivityTrackerTopAppBar(
                onNavigateBack = { navController.popBackStack() },
                onClearLogRequest = { showClearConfirmDialog = true },
                onExportRequest = {
                    val markdown = exportLogToMarkdown(log)
                    copyToClipboard(context, markdown)
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                InProgressIndicator(
                    ongoingActivity = lastOngoingActivity
                )
                ActivityInputBar(
                    text = inputText,
                    isActivityOngoing = lastOngoingActivity != null,
                    onTextChange = viewModel::onInputTextChanged,
                    onToggleStartStop = { viewModel.onToggleStartStop() },
                    onTimelessClick = viewModel::onTimelessRecordClick
                )
            }
        }
    ) { paddingValues ->
        ActivityLog(
            log = log,
            modifier = Modifier.padding(paddingValues),
            onEdit = viewModel::onEditRequest, // Лямбда залишається та сама, але її дія тепер інша
            onRestart = viewModel::onRestartActivity
        )

        // ✨ ОНОВЛЕНО: Логіка відображення діалогу редагування
        editingRecord?.let { recordToEdit ->
            EditRecordDialog(
                record = recordToEdit,
                onDismiss = viewModel::onEditDialogDismiss,
                onConfirm = viewModel::onRecordUpdated
            )
        }

        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Очистити лог?") },
                text = { Text("Ви впевнені, що хочете видалити всі записи? Цю дію неможливо буде скасувати.") },
                confirmButton = { Button(onClick = { viewModel.onClearLogConfirm(); showClearConfirmDialog = false }) { Text("Видалити") } },
                dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("Скасувати") } }
            )
        }
    }
}

@Composable
private fun ActivityTrackerTopAppBar(
    onNavigateBack: () -> Unit,
    onClearLogRequest: () -> Unit,
    onExportRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Трекер Активності") },
        navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
        actions = {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Меню") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Експорт в Markdown") }, onClick = { onExportRequest(); menuExpanded = false })
                DropdownMenuItem(text = { Text("Очистити лог") }, onClick = { onClearLogRequest(); menuExpanded = false })
            }
        }
    )
}

// ✨ ОНОВЛЕНО: Компонент логу тепер приймає лямбди для дій та використовує reverseLayout
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityLog(
    log: List<ActivityRecord>,
    modifier: Modifier = Modifier,
    onEdit: (ActivityRecord) -> Unit,
    onRestart: (ActivityRecord) -> Unit
) {
    val groupedByDate = log.groupBy { toDateHeader(it.createdAt) }
    val lazyListState = rememberLazyListState()

    // Автоматична прокрутка до низу при додаванні нового елемента
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = 12.dp),
        // reverseLayout = true показує елементи знизу вверх, що природно для логу
        reverseLayout = true
    ) {
        if (log.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Лог порожній. Почніть першу активність!")
                }
            }
        } else {
            groupedByDate.forEach { (dateHeader, records) ->
                // Порядок відображення записів всередині групи не змінюється, бо сортування вже зроблене в DAO
                items(records, key = { it.id }) { record ->
                    LogEntryItem(
                        record = record,
                        onEdit = onEdit,
                        onRestart = onRestart
                    )
                    if (records.last() != record) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
                stickyHeader {
                    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ✨ ОНОВЛЕНО: LogEntryItem тепер підтримує різні стани (нотатка, активність) та дії
@Composable
private fun LogEntryItem(
    record: ActivityRecord,
    onEdit: (ActivityRecord) -> Unit,
    onRestart: (ActivityRecord) -> Unit
) {
    if (record.isTimeless) {
        // --- Відображення для нотаток ---
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Notes,
                    contentDescription = "Нотатка",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = record.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    fontStyle = FontStyle.Italic
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onEdit(record) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Редагувати", modifier = Modifier.size(18.dp))
                }
            }
        }
    } else {
        // --- Відображення для активностей ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeText = when {
                record.isOngoing -> "${timeFormat.format(Date(record.startTime!!))} - ..."
                else -> "${timeFormat.format(Date(record.startTime!!))} - ${timeFormat.format(Date(record.endTime!!))}"
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(90.dp),
                fontWeight = FontWeight.SemiBold,
                color = if (record.isOngoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = record.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))

            Row {
                IconButton(onClick = { onRestart(record) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Replay, "Перезапустити", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onEdit(record) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Редагувати", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}


@Composable
private fun InProgressIndicator(
    ongoingActivity: ActivityRecord?
) {
    AnimatedVisibility(
        visible = ongoingActivity != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        if (ongoingActivity != null) {
            var elapsedTime by remember { mutableStateOf(System.currentTimeMillis() - (ongoingActivity.startTime ?: 0L)) }

            LaunchedEffect(key1 = ongoingActivity.id) {
                while (true) {
                    elapsedTime = System.currentTimeMillis() - (ongoingActivity.startTime ?: 0L)
                    delay(1000L)
                }
            }

            val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
            val timeString = if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = "В процесі", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ongoingActivity.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
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
    onTimelessClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Чим ви займаєтесь?") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onTimelessClick,
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.AddComment, "Зробити запис", tint = MaterialTheme.colorScheme.secondary)
            }

            IconButton(
                onClick = onToggleStartStop,
                enabled = text.isNotBlank() || isActivityOngoing
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
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun toDateHeader(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun exportLogToMarkdown(log: List<ActivityRecord>): String {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    // Оскільки сортування тепер ASC, для експорту його треба розвернути, щоб новіші були зверху
    val groupedByDate = log.sortedByDescending { it.createdAt }.groupBy { toDateHeader(it.createdAt) }
    return buildString {
        groupedByDate.forEach { (dateHeader, records) ->
            append("## $dateHeader\n\n")
            val (timeless, timed) = records.partition { it.isTimeless }
            timeless.forEach { record ->
                append("- ${record.text}\n")
            }
            timed.forEach { record ->
                val timeText = when {
                    record.isOngoing -> "`${sdfTime.format(Date(record.startTime!!))} - ...`"
                    else -> "`${sdfTime.format(Date(record.startTime!!))} - ${sdfTime.format(Date(record.endTime!!))}`"
                }
                append("- $timeText ${record.text}\n".trim())
            }
            append("\n\n")
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Activity Log", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Лог скопійовано!", Toast.LENGTH_SHORT).show()
}


@Composable
private fun EditRecordDialog(
    record: ActivityRecord,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(record) { mutableStateOf(record.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати запис") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Текст запису") }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}