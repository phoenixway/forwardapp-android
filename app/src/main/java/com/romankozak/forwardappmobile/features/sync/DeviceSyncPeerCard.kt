package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DeviceSyncPeerCard(
    peer: WifiAutoSyncPeer,
    globalAutoSyncEnabled: Boolean,
    syncing: Boolean,
    latestEvent: WifiSyncEvent?,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onSync: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val statusText = when {
        syncing || latestEvent?.outcome == "running" -> "Синхронізація триває…"
        latestEvent == null -> "Ще не синхронізовано"
        latestEvent.outcome == "success" -> "Синхронізовано о ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latestEvent.finishedAt ?: latestEvent.startedAt))}"
        latestEvent.stage.contains("Підключ", true) -> "Пристрій недоступний"
        else -> "Помилка · ${latestEvent.stage}"
    }
    val statusColor = when {
        syncing || latestEvent?.outcome == "success" -> MaterialTheme.colorScheme.primary
        latestEvent?.outcome == "error" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(peer.name.ifBlank { peer.address }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Дії пристрою") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Редагувати") }, onClick = { menuOpen = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                        DropdownMenuItem(text = { Text("Видалити") }, onClick = { menuOpen = false; onRemove() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) })
                    }
                }
            }
            Text(peer.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Автосинхронізація", style = MaterialTheme.typography.bodyMedium)
                    if (peer.enabled && !globalAutoSyncEnabled) Text("Увімкніть загальну автосинхронізацію", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(peer.enabled, onCheckedChange = onAutoSyncChange)
            }
            Button(onClick = onSync, modifier = Modifier.fillMaxWidth(),
                enabled = !syncing && peer.address.isNotBlank() && peer.key.isNotBlank()) {
                Icon(Icons.Outlined.Sync, null)
                Text(if (syncing) "Синхронізація…" else "Синхронізувати")
            }
        }
    }
}
