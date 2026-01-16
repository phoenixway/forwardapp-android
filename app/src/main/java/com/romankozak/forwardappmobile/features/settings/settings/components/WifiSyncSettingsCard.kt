package com.romankozak.forwardappmobile.features.settings.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WifiSyncSettingsCard(
    serverEnabled: Boolean,
    wifiSyncPort: Int,
    desktopAddress: String,
    onServerEnabledChange: (Boolean) -> Unit,
    onPortChange: (String) -> Unit,
    onDesktopAddressChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    SettingsCard(
        title = "Wi‑Fi Sync",
        icon = Icons.Default.Sync,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Android as Wi‑Fi sync server",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = serverEnabled,
                onCheckedChange = onServerEnabledChange,
                enabled = enabled,
            )
        }
        AnimatedTextField(
            value = wifiSyncPort.toString(),
            onValueChange = onPortChange,
            label = "Server port",
            helper = "Port used for LAN sync server",
            singleLine = true,
            enabled = enabled,
        )
        AnimatedTextField(
            value = desktopAddress,
            onValueChange = onDesktopAddressChange,
            label = "Desktop client address",
            helper = "Default host:port for desktop client (e.g., 192.168.0.12:8080)",
            singleLine = true,
            modifier = Modifier.padding(top = 8.dp),
            enabled = enabled,
        )
    }
}
