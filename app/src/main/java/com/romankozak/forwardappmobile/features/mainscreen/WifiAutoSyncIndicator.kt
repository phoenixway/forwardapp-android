package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.sync.WifiAutoSyncStatus
import java.text.DateFormat
import java.util.Date

@Composable
internal fun WifiAutoSyncIndicator(status: WifiAutoSyncStatus) {
    if (status == WifiAutoSyncStatus.Off) return
    val label = when (status) {
        WifiAutoSyncStatus.Off -> return
        WifiAutoSyncStatus.Paused -> "Автосинк призупинено"
        WifiAutoSyncStatus.NeedsSetup -> "Автосинк · додайте пристрій"
        WifiAutoSyncStatus.NeedsServer -> "Автосинк · увімкніть приймання"
        is WifiAutoSyncStatus.Syncing -> "Синхронізація · ${status.peerName}"
        is WifiAutoSyncStatus.Result -> {
            val time = status.at?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) }
            if (status.successful == status.total) "Автосинк ${status.successful}/${status.total} · ${time.orEmpty()}"
            else "Автосинк ${status.successful}/${status.total} · помилка"
        }
    }
    val hasError = status is WifiAutoSyncStatus.Result && status.error != null
    var showDetails by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.clickable { showDetails = true },
        shape = MaterialTheme.shapes.small,
        color = if (hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp),
                color = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Автосинхронізація") },
            text = { Text((status as? WifiAutoSyncStatus.Result)?.error ?: label) },
            confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Закрити") } },
        )
    }
}
