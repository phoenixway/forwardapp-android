package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
) {
    var menuOpen by remember { mutableStateOf(false) }
    val outcome = if (syncing) "running" else latestEvent?.outcome ?: "idle"
    val statusText = if (latestEvent == null && !syncing) "Ще не синхронізовано"
        else if (syncing) "Синхронізація триває" else syncEventTitle(latestEvent!!)
    val timeText = latestEvent?.let { event ->
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.startedAt))
    }
    Card(modifier = modifier, shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column {
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Smartphone, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(peer.name.ifBlank { peer.address }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(peer.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Дії пристрою", Modifier.size(20.dp)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Редагувати") }, onClick = { menuOpen = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                        DropdownMenuItem(text = { Text("Видалити") }, onClick = { menuOpen = false; onRemove() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SyncStatusIcon(outcome, null, Modifier.size(18.dp))
                Text(statusText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = syncStatusColor(outcome), maxLines = 2)
                timeText?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stack = maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f
                if (stack) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PeerAutoSwitch(peer.enabled, onAutoSyncChange)
                    SyncPeerButton(syncing, peer, onSync, Modifier.fillMaxWidth())
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    PeerAutoSwitch(peer.enabled, onAutoSyncChange)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    SyncPeerButton(syncing, peer, onSync)
                }
            }
            if (peer.enabled && !globalAutoSyncEnabled) Text("Автозапуски призупинено глобально",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PeerAutoSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = enabled, onCheckedChange = onChange)
        Text("Авто", modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SyncPeerButton(syncing: Boolean, peer: WifiAutoSyncPeer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        enabled = !syncing && peer.address.isNotBlank() && peer.key.isNotBlank()) {
        Icon(Icons.Outlined.Sync, null, Modifier.size(18.dp))
        Text(if (syncing) "Синхронізація…" else "Синхронізувати", modifier = Modifier.padding(start = 6.dp))
    }
}
