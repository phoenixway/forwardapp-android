package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
internal fun DeviceSyncLocalCard(
    state: DeviceSyncUiState,
    onConnectionChange: (Boolean) -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    onShowQr: () -> Unit,
) {
    var revealKey by remember { mutableStateOf(false) }
    val address = (state.listenerStatus as? WifiSyncStatus.ServerRunning)?.address
        ?: state.localAddress?.let { "$it:${state.localPort}" }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Цей пристрій", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = state.localConnectionEnabled, onCheckedChange = onConnectionChange)
            }
            val ready = state.listenerStatus is WifiSyncStatus.ServerRunning
            val status = when (val listener = state.listenerStatus) {
                is WifiSyncStatus.ServerRunning -> "● Готовий до підключень"
                is WifiSyncStatus.Error -> "● Помилка підключення: ${listener.message}"
                WifiSyncStatus.Syncing -> "○ Запуск приймання…"
                WifiSyncStatus.Offline -> "○ Wi‑Fi недоступний"
                WifiSyncStatus.Disabled -> "○ Приймання вимкнене"
                WifiSyncStatus.Idle -> "○ Приймання не запущене"
            }
            Text(status, style = MaterialTheme.typography.bodySmall,
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Локальна адреса", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(address ?: "Wi‑Fi недоступний", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Оновити адресу") }
                IconButton(onClick = { address?.let(onCopy) }, enabled = address != null) {
                    Icon(Icons.Outlined.ContentCopy, "Копіювати адресу")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ключ доступу", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (revealKey) state.localKey else "••••••••••••••••", style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1)
                }
                IconButton(onClick = { revealKey = !revealKey }) {
                    Icon(if (revealKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        if (revealKey) "Приховати ключ" else "Показати ключ")
                }
                IconButton(onClick = { onCopy(state.localKey) }, enabled = state.localKey.isNotBlank()) {
                    Icon(Icons.Outlined.ContentCopy, "Копіювати ключ")
                }
                IconButton(onClick = onShowQr, enabled = address != null && state.localKey.isNotBlank()) {
                    Icon(Icons.Outlined.QrCode2, "Показати QR-код підключення")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Автосинхронізація", style = MaterialTheme.typography.bodyMedium)
                    Text("Кожні 5 хв · у відкритому додатку", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.autoSyncEnabled, onCheckedChange = onAutoSyncChange)
            }
        }
    }
}
