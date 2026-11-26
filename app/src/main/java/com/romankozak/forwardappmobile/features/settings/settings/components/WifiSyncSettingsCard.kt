package com.romankozak.forwardappmobile.features.settings.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WifiSyncSettingsCard(
    serverEnabled: Boolean,
    wifiSyncPort: Int,
    peerAddress: String,
    localIpAddress: String?,
    onServerEnabledChange: (Boolean) -> Unit,
    onPortChange: (String) -> Unit,
    onPeerAddressChange: (String) -> Unit,
    onRefreshLocalAddress: () -> Unit,
    enabled: Boolean = true,
) {
    SettingsCard(
        title = "Синхронізація через Wi‑Fi",
        icon = Icons.Default.Sync,
    ) {
        Text(
            text = "Для двох Android або Android і комп’ютера: під’єднайте пристрої до однієї Wi‑Fi мережі. На пристрої, що приймає дані, увімкніть сервер і відкрийте «Приймати через Wi‑Fi» в меню імпорту / експорту, щоб побачити його адресу.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (localIpAddress == null) "IP цього телефона: Wi‑Fi недоступний"
                    else "IP цього телефона: $localIpAddress",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onRefreshLocalAddress) { Text("Оновити") }
        }
        if (localIpAddress != null) {
            Text(
                text = "Для іншого пристрою: $localIpAddress:$wifiSyncPort. Після зміни порту збережіть налаштування і перезапустіть сервер.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Приймати дані на цьому телефоні",
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
            label = "Порт цього телефона",
            helper = "Число від 1 до 65535 для сервера цього телефона. На іншому пристрої введіть адресу IP:порт, показану в «Приймати через Wi‑Fi».",
            singleLine = true,
            enabled = enabled,
        )
        AnimatedTextField(
            value = peerAddress,
            onValueChange = onPeerAddressChange,
            label = "Адреса іншого пристрою",
            helper = "IP:порт сервера на іншому Android або комп’ютері, наприклад 192.168.1.5:8080. Використовується для Push та Wi‑Fi імпорту.",
            singleLine = true,
            modifier = Modifier.padding(top = 8.dp),
            enabled = enabled,
        )
    }
}
