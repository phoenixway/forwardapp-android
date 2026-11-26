package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DeviceSyncJournalSection(events: List<WifiSyncEvent>) {
    var showDetails by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Журнал синхронізації", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showDetails = true }) { Text("More") }
            }
            if (events.isEmpty()) Text("Операцій поки немає", style = MaterialTheme.typography.bodySmall)
            events.take(3).forEach { event ->
                Text("${eventTime(event.startedAt)} · ${event.peerName}: ${eventSummary(event)}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Технічний журнал синхронізації") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (events.isEmpty()) Text("Операцій поки немає")
                    events.forEach { event ->
                        Text(formatTechnicalEvent(event), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(events.joinToString("\n\n", transform = ::formatTechnicalEvent)))
                }, enabled = events.isNotEmpty()) {
                    Icon(Icons.Outlined.ContentCopy, null)
                    Text("Копіювати")
                }
            },
            dismissButton = { TextButton(onClick = { showDetails = false }) { Text("Закрити") } },
        )
    }
}

private fun eventTime(time: Long): String =
    SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(time))

private fun eventSummary(event: WifiSyncEvent): String = when (event.outcome) {
    "success" -> "завершено"
    "error" -> "помилка · ${event.stage}"
    "cancelled" -> "перервано · ${event.stage}"
    else -> "триває · ${event.stage}"
}

private fun formatTechnicalEvent(event: WifiSyncEvent): String = buildString {
    append(eventTime(event.startedAt))
    append(" · ").append(event.mode)
    append(" · ").append(event.peerName).append(" (").append(event.address).append(")")
    append("\nЕтап: ").append(event.stage)
    append("\nСтан: ").append(eventSummary(event))
    event.finishedAt?.let { append(" · ").append((it - event.startedAt).coerceAtLeast(0) / 1000).append(" с") }
    event.detail?.let { append("\nДеталі: ").append(it) }
}
