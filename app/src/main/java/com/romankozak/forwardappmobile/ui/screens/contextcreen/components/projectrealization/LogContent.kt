package com.romankozak.forwardappmobile.ui.screens.contextcreen.components.projectrealization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ProjectLogEntryTypeValues
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

 @Composable
fun LogContent(
    logs: List<ProjectExecutionLog>,
    isManagementEnabled: Boolean,
    onEditLog: (ProjectExecutionLog) -> Unit,
    onDeleteLog: (ProjectExecutionLog) -> Unit,
) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити історію.")
        return
    }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val allTypes = remember {
        listOf(
            ProjectLogEntryTypeValues.STATUS_CHANGE,
            ProjectLogEntryTypeValues.COMMENT,
            ProjectLogEntryTypeValues.AUTOMATIC,
            ProjectLogEntryTypeValues.INSIGHT,
            ProjectLogEntryTypeValues.MILESTONE,
        )
    }
    val typeStates = remember { allTypes.associateWith { mutableStateOf(true) } }

    val filteredLogs = logs
        .filter { typeStates[it.type]?.value == true }
        .filter {
            it.description.contains(searchQuery, true) ||
                    (it.details?.contains(searchQuery, true) == true)
        }
        .sortedByDescending { it.timestamp }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(visible = showSearch) {
            FilterPanel(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                typeStates = typeStates,
                onCloseSearch = {
                    showSearch = false
                    searchQuery = ""
                }
            )
        }

        val listState = rememberLazyListState()
        LaunchedEffect(filteredLogs.firstOrNull()) {
            if (filteredLogs.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    PlaceholderContent(text = "Немає записів за вибраними фільтрами.")
                }
            } else {
                items(filteredLogs, key = { it.id ?: it.timestamp }) { log ->
                    LogEntryItem(
                        log = log,
                        onEdit = { onEditLog(log) },
                        onDelete = { onDeleteLog(log) },
                        onToggleSearch = { showSearch = !showSearch }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    typeStates: Map<String, MutableState<Boolean>>,
    onCloseSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Пошук по тексту") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            typeStates.forEach { (type, state) ->
                FilterChip(
                    selected = state.value,
                    onClick = { state.value = !state.value },
                    label = { Text(typeToLabel(type)) },
                    leadingIcon = {
                        Icon(
                            imageVector = typeToIcon(type),
                            contentDescription = null
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = typeToColor(type).copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

/* ---------------------------------------------------------- */
/*                       LOG ENTRY ITEM                        */
/* ---------------------------------------------------------- */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LogEntryItem(
    log: ProjectExecutionLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSearch: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val accent = typeToColor(log.type)
    val icon = typeToIcon(log.type)
    val smallLabel = typeToLabel(log.type)
    val timeText = formatRelativeTime(log.timestamp)
    val fullTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { expanded = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon column
            Column(
                modifier = Modifier.weight(0.1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content column
            Column(modifier = Modifier.weight(0.9f)) {
                Text(
                    text = log.description.ifEmpty { smallLabel },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = Int.MAX_VALUE
                )

                log.details?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = Int.MAX_VALUE,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fullTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                }
            }

            DropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                DropdownMenuItem(
                    text = { Text("Редагувати") },
                    onClick = {
                        expanded = false
                        onEdit()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Видалити") },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
                DropdownMenuItem(
                    text = { Text("Пошук") },
                    onClick = {
                        expanded = false
                        onToggleSearch()
                    },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
            }
        }
    }
}

/* ---------------------------------------------------------- */
/*                       UTILITIES                             */
/* ---------------------------------------------------------- */
private fun typeToColor(type: String?): Color = when (type) {
    ProjectLogEntryTypeValues.STATUS_CHANGE -> Color(0xFF00695C)
    ProjectLogEntryTypeValues.COMMENT -> Color(0xFF1565C0)
    ProjectLogEntryTypeValues.AUTOMATIC -> Color(0xFF6A1B9A)
    ProjectLogEntryTypeValues.INSIGHT -> Color(0xFFF57C00)
    ProjectLogEntryTypeValues.MILESTONE -> Color(0xFFD32F2F)
    else -> Color(0xFF546E7A)
}

private fun typeToIcon(type: String?) = when (type) {
    ProjectLogEntryTypeValues.STATUS_CHANGE -> Icons.Default.TrendingUp
    ProjectLogEntryTypeValues.COMMENT -> Icons.Default.Comment
    ProjectLogEntryTypeValues.AUTOMATIC -> Icons.Default.ReceiptLong
    ProjectLogEntryTypeValues.INSIGHT -> Icons.Default.Lightbulb
    ProjectLogEntryTypeValues.MILESTONE -> Icons.Default.Flag
    else -> Icons.Default.Info
}

private fun typeToLabel(type: String?): String = when (type) {
    ProjectLogEntryTypeValues.STATUS_CHANGE -> "Статус"
    ProjectLogEntryTypeValues.COMMENT -> "Коментар"
    ProjectLogEntryTypeValues.AUTOMATIC -> "Системний"
    ProjectLogEntryTypeValues.INSIGHT -> "Ідея"
    ProjectLogEntryTypeValues.MILESTONE -> "Віха"
    else -> "Інше"
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val absDiff = abs(diff)
    val minutes = (absDiff / 60000L).toInt()
    val hours = (absDiff / 3600000L).toInt()
    val days = (absDiff / (24 * 3600000L)).toInt()
    return when {
        absDiff < 60_000L -> "щойно"
        minutes < 60 -> "$minutes хв. тому"
        hours < 24 -> "$hours год. тому"
        days < 7 -> "$days дн. тому"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

 @Composable
internal fun PlaceholderContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Додайте перший запис, щоб відстежувати прогрес.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/* ---------------------------------------------------------- */
/*                       PREVIEW                               */
/* ---------------------------------------------------------- */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LogContentPreview() {
    val now = System.currentTimeMillis()
    val sample = listOf(
        ProjectExecutionLog(id = "1", projectId = "0", timestamp = now - 20 * 60_000, type = ProjectLogEntryTypeValues.STATUS_CHANGE, description = "Перенесено до 'Робота'", details = "Завершено перевірку"),
        ProjectExecutionLog(id = "2", projectId = "0", timestamp = now - 120 * 60_000, type = ProjectLogEntryTypeValues.COMMENT, description = "Коментар", details = "Потрібно уточнити терміни"),
        ProjectExecutionLog(id = "3", projectId = "0", timestamp = now - 3_600_000 * 5, type = ProjectLogEntryTypeValues.INSIGHT, description = "Ідея: кешування", details = "Зменшить навантаження"),
    )
    MaterialTheme {
        LogContent(sample, true, {}, {})
    }
}
