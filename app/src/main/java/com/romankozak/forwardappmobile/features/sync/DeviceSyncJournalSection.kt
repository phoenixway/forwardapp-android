package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DeviceSyncJournalSection(events: List<WifiSyncEvent>, modifier: Modifier = Modifier) {
    var showAll by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<WifiSyncEvent?>(null) }
    val clipboard = LocalClipboardManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Журнал", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showAll = true }, enabled = events.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Усі записи")
            }
        }
        if (events.isEmpty()) Text("Операцій поки немає", style = MaterialTheme.typography.bodySmall)
        events.take(3).forEach { event -> JournalRow(event) { selected = event } }
    }
    if (showAll) AlertDialog(
        onDismissRequest = { showAll = false }, title = { Text("Журнал синхронізації") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            events.forEach { event -> JournalRow(event) { showAll = false; selected = event } }
        } },
        confirmButton = { TextButton(onClick = {
            clipboard.setText(AnnotatedString(events.joinToString("\n\n", transform = ::formatTechnicalEvent)))
        }) { Icon(Icons.Outlined.ContentCopy, null); Text("Копіювати журнал") } },
        dismissButton = { TextButton(onClick = { showAll = false }) { Text("Закрити") } },
    )
    selected?.let { event -> AlertDialog(
        onDismissRequest = { selected = null }, title = { Text(syncEventTitle(event)) },
        text = { Text(formatTechnicalEvent(event), style = MaterialTheme.typography.bodySmall) },
        confirmButton = { TextButton(onClick = { clipboard.setText(AnnotatedString(formatTechnicalEvent(event))) }) {
            Icon(Icons.Outlined.ContentCopy, null); Text("Копіювати діагностику")
        } },
        dismissButton = { TextButton(onClick = { selected = null }) { Text("Закрити") } },
    ) }
}

@Composable
private fun JournalRow(event: WifiSyncEvent, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SyncStatusIcon(event.outcome, null, Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(syncEventTitle(event), style = MaterialTheme.typography.bodySmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(event.peerName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(journalTime(event.startedAt), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun journalTime(value: Long): String {
    val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val pattern = if (dayFormat.format(Date(value)) == dayFormat.format(Date())) "HH:mm" else "dd.MM HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(value))
}

private fun formatTechnicalEvent(event: WifiSyncEvent): String = buildString {
    append(SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(event.startedAt)))
    append(" · ").append(event.mode)
    append(" · ").append(event.peerName).append(" (").append(event.address).append(")")
    append("\nЕтап: ").append(event.stage)
    append("\nСтан: ").append(event.outcome)
    event.finishedAt?.let { append(" · ").append((it - event.startedAt).coerceAtLeast(0) / 1000).append(" с") }
    event.detail?.let { append("\nДеталі: ").append(it) }
}
