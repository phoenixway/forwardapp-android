package com.romankozak.forwardappmobile.features.settings.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer

@Composable
fun WifiSyncSettingsCard(
    serverEnabled: Boolean,
    wifiSyncPort: Int,
    peerAddress: String,
    serverKey: String,
    peerKey: String,
    autoSyncEnabled: Boolean,
    extraPeers: List<WifiAutoSyncPeer>,
    localIpAddress: String?,
    onServerEnabledChange: (Boolean) -> Unit,
    onPortChange: (String) -> Unit,
    onPeerAddressChange: (String) -> Unit,
    onPeerKeyChange: (String) -> Unit,
    onAutoSyncEnabledChange: (Boolean) -> Unit,
    onAddExtraPeer: () -> Unit,
    onUpdateExtraPeer: (WifiAutoSyncPeer) -> Unit,
    onRemoveExtraPeer: (String) -> Unit,
    onRotateServerKey: () -> Unit,
    onRefreshLocalAddress: () -> Unit,
    enabled: Boolean = true,
) {
    val clipboard = LocalClipboardManager.current
    SettingsCard(
        title = "Синхронізація через Wi‑Fi",
        icon = Icons.Default.Sync,
    ) {
        Text(
            text = "Для двох Android або Android і комп’ютера: під’єднайте пристрої до однієї Wi‑Fi мережі. Дозвольте підключення на телефоні, який має приймати дані. Його адресу й ключ можна подивитися в «Sync between devices» у меню More.",
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
                text = "Адреса цього телефона для іншого пристрою: $localIpAddress:$wifiSyncPort. Після зміни мережі оновіть адресу на екрані синхронізації.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "Ключ цього телефона: $serverKey",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row {
            TextButton(onClick = { clipboard.setText(AnnotatedString(serverKey)) }, enabled = serverKey.isNotBlank()) {
                Text("Копіювати ключ")
            }
            TextButton(onClick = onRotateServerKey, enabled = enabled) { Text("Змінити ключ") }
        }
        Text(
            text = "Введіть цей ключ на іншому пристрої для доступу до цього телефона. Зміна ключа відключить пристрої зі старим ключем.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Дозволити підключення до цього телефона",
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
            helper = "Число від 1 до 65535. Разом з IP утворює адресу цього телефона, яку потрібно ввести на іншому пристрої.",
            singleLine = true,
            enabled = enabled,
        )
        AnimatedTextField(
            value = peerAddress,
            onValueChange = onPeerAddressChange,
            label = "Адреса іншого пристрою",
            helper = "Адреса іншого Android або комп’ютера у форматі IP:порт, наприклад 192.168.1.5:8080. Використовується для Push та Wi‑Fi імпорту.",
            singleLine = true,
            modifier = Modifier.padding(top = 8.dp),
            enabled = enabled,
        )
        AnimatedTextField(
            value = peerKey,
            onValueChange = onPeerKeyChange,
            label = "Ключ іншого пристрою",
            helper = "Скопіюйте ключ з налаштувань Wi‑Fi синхронізації пристрою за вказаною адресою. Збережіть налаштування перед Push або імпортом.",
            singleLine = true,
            modifier = Modifier.padding(top = 8.dp),
            enabled = enabled,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Автосинхронізація", style = MaterialTheme.typography.titleMedium)
                Text("Поки ForwardApp відкритий. Обмін із кожним пристроєм окремо кожні 5 хвилин.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = autoSyncEnabled, onCheckedChange = onAutoSyncEnabledChange, enabled = enabled)
        }
        Text(
            "Основний пристрій — адреса й ключ вище. Для двостороннього обміну дозвольте підключення на кожному телефоні. Збережіть налаштування.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        extraPeers.forEach { peer ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Додатковий пристрій", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { onRemoveExtraPeer(peer.id) }) { Text("Видалити") }
                }
                AnimatedTextField(
                    value = peer.name,
                    onValueChange = { onUpdateExtraPeer(peer.copy(name = it)) },
                    label = "Назва пристрою",
                    helper = "Наприклад, другий телефон",
                    singleLine = true,
                    enabled = enabled,
                )
                AnimatedTextField(
                    value = peer.address,
                    onValueChange = { onUpdateExtraPeer(peer.copy(address = it)) },
                    label = "Адреса IP:порт",
                    helper = "Адреса з «Приймати через Wi‑Fi» на іншому пристрої",
                    singleLine = true,
                    enabled = enabled,
                )
                AnimatedTextField(
                    value = peer.key,
                    onValueChange = { onUpdateExtraPeer(peer.copy(key = it)) },
                    label = "Ключ пристрою",
                    helper = "Ключ із налаштувань того пристрою",
                    singleLine = true,
                    enabled = enabled,
                )
            }
        }
        TextButton(onClick = onAddExtraPeer, enabled = enabled) { Text("Додати пристрій") }
    }
}
