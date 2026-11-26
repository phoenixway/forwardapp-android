package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSyncScreen(onBack: () -> Unit, viewModel: DeviceSyncViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var editing by remember { mutableStateOf<WifiAutoSyncPeer?>(null) }
    var removing by remember { mutableStateOf<WifiAutoSyncPeer?>(null) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Синхронізація між пристроями") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
        })
    }) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Під’єднайте пристрої до однієї мережі Wi‑Fi. На кожному дозвольте підключення, а потім додайте адресу й ключ іншого пристрою.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Дозволити підключення до цього пристрою", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Switch(state.localConnectionEnabled, onCheckedChange = viewModel::setLocalConnectionEnabled)
                        }
                        Text("Інші ваші пристрої зможуть обмінюватися даними з цим телефоном, поки додаток працює.", style = MaterialTheme.typography.bodySmall)
                        Text("Адреса цього пристрою: ${state.localAddress?.let { "$it:${state.localPort}" } ?: "Wi‑Fi недоступний"}")
                        TextButton(onClick = viewModel::refreshLocalAddress) { Text("Оновити адресу") }
                        Text("Ключ цього пристрою: ${state.localKey}")
                        TextButton(onClick = { clipboard.setText(AnnotatedString(state.localKey)) }, enabled = state.localKey.isNotBlank()) {
                            Icon(Icons.Outlined.ContentCopy, null)
                            Text("Копіювати ключ")
                        }
                        Text("Введіть цю адресу та ключ на іншому пристрої.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Автосинхронізація", style = MaterialTheme.typography.titleMedium)
                        Text("Поки додаток відкритий, приблизно кожні 5 хвилин з увімкненими пристроями.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(state.autoSyncEnabled, onCheckedChange = viewModel::setAutoSyncEnabled)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Збережені пристрої", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { editing = WifiAutoSyncPeer("", "", "", "") }) {
                        Icon(Icons.Outlined.Add, null)
                        Text("Додати")
                    }
                }
            }
            if (state.peers.isEmpty()) item {
                Text("Поки немає пристроїв. Додайте адресу й ключ іншого телефона.", style = MaterialTheme.typography.bodyMedium)
            }
            items(state.peers, key = { it.id }) { peer ->
                DevicePeerCard(peer, state.autoSyncEnabled, state.syncingPeerId == peer.id,
                    onEdit = { editing = peer },
                    onRemove = { removing = peer },
                    onAutoSyncChange = { viewModel.setPeerAutoSync(peer, it) },
                    onSync = { viewModel.syncNow(peer) })
            }
            item { DeviceSyncJournalSection(events) }
        }
    }
    editing?.let { peer ->
        PeerEditorDialog(peer, onDismiss = { editing = null }, onSave = { edited ->
            if (edited.id.isBlank()) viewModel.newPeer(edited.name, edited.address, edited.key)
            else viewModel.savePeer(edited)
            editing = null
        })
    }
    removing?.let { peer ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Видалити пристрій?") },
            text = { Text("Адресу та ключ «${peer.name.ifBlank { peer.address }}» буде видалено з цього телефона.") },
            confirmButton = { TextButton(onClick = { viewModel.removePeer(peer); removing = null }) { Text("Видалити") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Скасувати") } },
        )
    }
}

@Composable
private fun DevicePeerCard(
    peer: WifiAutoSyncPeer,
    globalAutoSyncEnabled: Boolean,
    syncing: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onSync: () -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(peer.name.ifBlank { peer.address }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Редагувати") }
            }
            Text(peer.address, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Автосинк для цього пристрою", modifier = Modifier.weight(1f))
                Switch(peer.enabled, onCheckedChange = onAutoSyncChange)
            }
            if (peer.enabled && !globalAutoSyncEnabled) Text("Увімкніть загальну автосинхронізацію вище.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSync, enabled = !syncing && peer.address.isNotBlank() && peer.key.isNotBlank()) {
                    Icon(Icons.Outlined.Sync, null)
                    Text(if (syncing) "Синхронізація…" else "Синхронізувати")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text("Видалити") }
            }
        }
    }
}

@Composable
private fun PeerEditorDialog(peer: WifiAutoSyncPeer, onDismiss: () -> Unit, onSave: (WifiAutoSyncPeer) -> Unit) {
    var name by remember(peer.id) { mutableStateOf(peer.name) }
    var address by remember(peer.id) { mutableStateOf(peer.address) }
    var key by remember(peer.id) { mutableStateOf(peer.key) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (peer.id.isBlank()) "Додати пристрій" else "Редагувати пристрій") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Назва") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Адреса IP:порт") }, singleLine = true)
                OutlinedTextField(key, { key = it }, label = { Text("Ключ іншого пристрою") }, singleLine = true)
                Text("Адресу й ключ знайдете на іншому пристрої в цьому ж екрані.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(peer.copy(name = name, address = address.trim(), key = key.trim())) }, enabled = address.isNotBlank() && key.isNotBlank()) { Text("Зберегти") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
