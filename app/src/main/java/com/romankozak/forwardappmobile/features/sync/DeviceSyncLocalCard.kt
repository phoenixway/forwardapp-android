package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DeviceSyncLocalCard(
    state: DeviceSyncUiState,
    onConnectionChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    onShowQr: () -> Unit,
) {
    var revealKey by remember { mutableStateOf(false) }
    val address = (state.listenerStatus as? WifiSyncStatus.ServerRunning)?.address
        ?: state.localAddress?.let { "$it:${state.localPort}" }
    val outcome = when (state.listenerStatus) {
        is WifiSyncStatus.ServerRunning -> "success"
        is WifiSyncStatus.Error -> "error"
        WifiSyncStatus.Syncing -> "running"
        else -> "idle"
    }
    val status = when (state.listenerStatus) {
        is WifiSyncStatus.ServerRunning -> "Готовий до підключень"
        is WifiSyncStatus.Error -> "Помилка приймання"
        WifiSyncStatus.Syncing -> "Запуск приймання…"
        WifiSyncStatus.Offline -> "Wi‑Fi недоступний"
        WifiSyncStatus.Disabled -> "Приймання вимкнене"
        WifiSyncStatus.Idle -> "Приймання не запущене"
    }
    Card(shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Smartphone, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Цей пристрій", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SyncStatusIcon(outcome, null, Modifier.size(16.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = syncStatusColor(outcome))
                    }
                }
                Switch(checked = state.localConnectionEnabled, onCheckedChange = onConnectionChange)
            }
            (state.listenerStatus as? WifiSyncStatus.Error)?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            TechnicalValueRow("Локальна адреса", address ?: "Wi‑Fi недоступний",
                firstAction = { IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, "Оновити адресу", Modifier.size(20.dp))
                } },
                copyAction = { IconButton(onClick = { address?.let(onCopy) }, enabled = address != null) {
                    Icon(Icons.Outlined.ContentCopy, "Копіювати адресу", Modifier.size(20.dp))
                } })
            TechnicalValueRow("Ключ доступу", when {
                state.localKey.isBlank() -> "—"
                revealKey -> state.localKey
                else -> "••••••••••••••••"
            }, firstAction = { IconButton(onClick = { revealKey = !revealKey }, enabled = state.localKey.isNotBlank()) {
                Icon(if (revealKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    if (revealKey) "Приховати ключ" else "Показати ключ", Modifier.size(20.dp))
            } }, copyAction = { IconButton(onClick = { onCopy(state.localKey) }, enabled = state.localKey.isNotBlank()) {
                Icon(Icons.Outlined.ContentCopy, "Копіювати ключ", Modifier.size(20.dp))
            } })
            TextButton(onClick = onShowQr, modifier = Modifier.heightIn(min = 48.dp),
                enabled = address != null && state.localKey.isNotBlank()) {
                Icon(Icons.Outlined.QrCode2, null, Modifier.size(18.dp))
                Text("QR підключення", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun TechnicalValueRow(
    label: String,
    value: String,
    firstAction: @Composable () -> Unit,
    copyAction: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        firstAction()
        copyAction()
    }
}

@Composable
internal fun DeviceSyncGlobalAutoSyncRow(enabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Автосинхронізація", style = MaterialTheme.typography.bodyMedium)
                    Text("Кожні 5 хв · у відкритому додатку", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onChange)
            }
        }
    }
}
