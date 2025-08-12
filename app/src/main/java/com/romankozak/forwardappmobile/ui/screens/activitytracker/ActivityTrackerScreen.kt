// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/activitytracker/ActivityTrackerScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.activitytracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// --- Головна функція екрану ---
@Composable
fun ActivityTrackerScreen(
    navController: NavController,
    viewModel: ActivityTrackerViewModel = hiltViewModel()
) {
    val log by viewModel.activityLog.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsState()
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
        // ✨ ОНОВЛЕНО: Тепер bottomBar - це Column з індикатором та панеллю вводу
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Додає відступ для системної панелі навігації (жести/кнопки)
                    .navigationBarsPadding()
                    // Додає відступ знизу, коли з'являється клавіатура
                    .imePadding()
            ) {
                InProgressIndicator(
                    ongoingActivity = lastOngoingActivity
                )
                ActivityInputBar(
                    text = inputText,
                    isActivityOngoing = lastOngoingActivity != null,
                    onTextChange = viewModel::onInputTextChanged,
                    // Замість onStartClick/onEndClick тепер єдина функція
                    onToggleStartStop = { viewModel.onToggleStartStop() },
                    onTimelessClick = viewModel::onTimelessRecordClick
                )
            }
        }
    ) { paddingValues ->
        ActivityLog(
            log = log,
            modifier = Modifier.padding(paddingValues)
        )

        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Clear Log?") },
                text = { Text("Are you sure you want to delete all activity records? This action cannot be undone.") },
                confirmButton = { Button(onClick = { viewModel.onClearLogConfirm(); showClearConfirmDialog = false }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// --- Верхня панель (без змін) ---
@Composable
private fun ActivityTrackerTopAppBar(
    onNavigateBack: () -> Unit,
    onClearLogRequest: () -> Unit,
    onExportRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Activity Tracker") },
        navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        actions = {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menu") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Export to Markdown") }, onClick = { onExportRequest(); menuExpanded = false })
                DropdownMenuItem(text = { Text("Clear log") }, onClick = { onClearLogRequest(); menuExpanded = false })
            }
        }
    )
}

// ✨ --- ОНОВЛЕНИЙ КОМПОНЕНТ ЛОГУ З НОВИМ СОРТУВАННЯМ ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityLog(log: List<ActivityRecord>, modifier: Modifier = Modifier) {
    val groupedByDate = log.groupBy { toDateHeader(it.createdAt) }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        if (log.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Log is empty. Start your first activity!")
                }
            }
        } else {
            groupedByDate.forEach { (dateHeader, records) ->
                stickyHeader {
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                        Text(text = dateHeader, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Розділяємо записи на "без часу" і "з часом"
                val (timeless, timed) = records.sortedBy { it.createdAt }.partition { it.isTimeless }

                // Спочатку відображаємо всі записи без часу
                items(timeless, key = { "timeless-${it.id}" }) { record ->
                    LogEntryItem(record)
                }

                // Потім відображаємо всі записи з часом
                items(timed, key = { "timed-${it.id}" }) { record ->
                    LogEntryItem(record)
                }
            }
        }
    }
}

// --- Один запис в лозі (без змін) ---
@Composable
private fun LogEntryItem(record: ActivityRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = when {
            record.isOngoing -> "${timeFormat.format(Date(record.startTime!!))} - Now..."
            record.isTimeless -> "•"
            else -> "${timeFormat.format(Date(record.startTime!!))} - ${timeFormat.format(Date(record.endTime!!))}"
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp),
            fontWeight = if(record.isTimeless) FontWeight.Bold else FontWeight.Normal,
            color = if (record.isOngoing) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = record.text, style = MaterialTheme.typography.bodyLarge)
    }
}

// ✨ --- НОВИЙ ІНДИКАТОР, ЩО ПОКАЗУЄТЬСЯ НАД ПАНЕЛЛЮ ВВОДУ ---
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
                    Icon(Icons.Default.HourglassTop, contentDescription = "In progress", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = ongoingActivity.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


// ✨ --- ОНОВЛЕНА ПАНЕЛЬ ВВОДУ, ЯКА ЗАВЖДИ ВИДИМА ---
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
            // Поле вводу (без змін)
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("What are you working on?") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка для запису без часу (без змін)
            IconButton(
                onClick = onTimelessClick,
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.AddComment, "Make a record", tint = MaterialTheme.colorScheme.secondary)
            }

            // ✨ ОНОВЛЕНО: Динамічна кнопка Старт/Стоп/Синхронізація
            IconButton(
                onClick = onToggleStartStop,
                enabled = text.isNotBlank() || isActivityOngoing
            ) {
                val icon: ImageVector
                val tint: Color
                val description: String

                if (isActivityOngoing) {
                    if (text.isNotBlank()) {
                        // Стан: є активна задача І є текст для нової
                        icon = Icons.Default.Sync
                        tint = MaterialTheme.colorScheme.tertiary // Нейтральний колір для "синхронізації"
                        description = "Stop current and start new activity"
                    } else {
                        // Стан: є активна задача, але немає тексту для нової
                        icon = Icons.Default.StopCircle
                        tint = MaterialTheme.colorScheme.error
                        description = "Stop activity"
                    }
                } else {
                    // Стан: немає активної задачі
                    icon = Icons.Default.PlayCircle
                    tint = MaterialTheme.colorScheme.primary
                    description = "Start activity"
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



// --- Допоміжні функції (без змін) ---
private fun toDateHeader(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun exportLogToMarkdown(log: List<ActivityRecord>): String {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val groupedByDate = log.groupBy { toDateHeader(it.createdAt) }
    return buildString {
        groupedByDate.forEach { (dateHeader, records) ->
            append("## $dateHeader\n\n")
            val (timeless, timed) = records.sortedBy { it.createdAt }.partition { it.isTimeless }
            timeless.forEach { record ->
                append("- ${record.text}\n")
            }
            timed.forEach { record ->
                val timeText = when {
                    record.isOngoing -> "`${sdfTime.format(Date(record.startTime!!))} - Now...`"
                    else -> "`${sdfTime.format(Date(record.startTime!!))} - ${sdfTime.format(Date(record.endTime!!))}`"
                }
                append("- $timeText ${record.text}\n".trim())
            }
            append("\n")
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Activity Log", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Log copied to clipboard!", Toast.LENGTH_SHORT).show()
}