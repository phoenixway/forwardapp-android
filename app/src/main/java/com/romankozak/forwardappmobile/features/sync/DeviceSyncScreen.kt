package com.romankozak.forwardappmobile.features.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSyncScreen(onBack: () -> Unit, viewModel: DeviceSyncViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var editing by remember { mutableStateOf<WifiAutoSyncPeer?>(null) }
    var removing by remember { mutableStateOf<WifiAutoSyncPeer?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var addMenuOpen by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf(false) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { raw ->
            val details = decodeDevicePairingQr(raw)
            if (details == null) scanError = true
            else editing = WifiAutoSyncPeer("", "", details.address, details.key)
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Синхронізація") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
    }, actions = {
        IconButton(onClick = { showHelp = true }) { Icon(Icons.Outlined.HelpOutline, "Довідка") }
    }) }) { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
            state.message?.let { message -> item {
                Text(message, modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } }
            if (scanError) item {
                Text("Цей QR-код не містить даних підключення ForwardApp.",
                    modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            item { DeviceSyncLocalCard(state, viewModel::setLocalConnectionEnabled,
                viewModel::refreshConnectionAddress,
                onCopy = { clipboard.setText(AnnotatedString(it)) }, onShowQr = { showQr = true }) }
            item { DeviceSyncGlobalAutoSyncRow(state.autoSyncEnabled, viewModel::setAutoSyncEnabled,
                Modifier.padding(top = 16.dp)) }
            item { Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Пристрої · ${state.peers.size}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Box {
                    TextButton(onClick = { addMenuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.Add, null); Text("Додати", modifier = Modifier.padding(start = 4.dp))
                    }
                    DropdownMenu(expanded = addMenuOpen, onDismissRequest = { addMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Сканувати QR-код") },
                            leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, null) }, onClick = {
                                addMenuOpen = false
                                scanError = false
                                scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt("Скануйте код іншого пристрою")
                                    .setBeepEnabled(false).setOrientationLocked(false))
                            })
                        DropdownMenuItem(text = { Text("Ввести адресу та ключ") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                                addMenuOpen = false
                                editing = WifiAutoSyncPeer("", "", "", "")
                            })
                    }
                }
            } }
            if (state.peers.isEmpty()) item {
                Text("Додайте інший пристрій через QR-код або введіть адресу й ключ.",
                    style = MaterialTheme.typography.bodySmall)
            }
            items(state.peers, key = { it.id }) { peer ->
                val latest = events.firstOrNull { event ->
                    event.address == peer.address && event.mode in listOf("Вручну", "Автоматично")
                }
                DeviceSyncPeerCard(peer, state.autoSyncEnabled, state.syncingPeerId == peer.id,
                    latest, onEdit = { editing = peer }, onRemove = { removing = peer },
                    onAutoSyncChange = { viewModel.setPeerAutoSync(peer, it) }, onSync = { viewModel.syncNow(peer) },
                    modifier = Modifier.padding(top = 4.dp))
            }
            item { DeviceSyncJournalSection(events, Modifier.padding(top = 16.dp, bottom = 12.dp)) }
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Як підключити пристрої") },
        text = { Text("Під’єднайте телефони до однієї мережі Wi‑Fi та увімкніть приймання на кожному. Додайте інший пристрій, відсканувавши його QR-код або ввівши адресу й ключ. Натисніть «Синхронізувати» для ручного обміну. Автосинхронізація працює, поки додаток відкритий.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Зрозуміло") } })
    if (showQr) {
        val address = (state.listenerStatus as? WifiSyncStatus.ServerRunning)?.address
            ?: state.localAddress?.let { "$it:${state.localPort}" }
        address?.let { DeviceSyncQrDialog(it, state.localKey) { showQr = false } }
    }
    editing?.let { peer -> PeerEditorDialog(peer, onDismiss = { editing = null }, onSave = { edited ->
        if (edited.id.isBlank()) viewModel.newPeer(edited.name, edited.address, edited.key)
        else viewModel.savePeer(edited)
        editing = null
    }) }
    removing?.let { peer -> AlertDialog(onDismissRequest = { removing = null },
        title = { Text("Видалити пристрій?") },
        text = { Text("Адресу та ключ «${peer.name.ifBlank { peer.address }}» буде видалено з цього телефона.") },
        confirmButton = { TextButton(onClick = { viewModel.removePeer(peer); removing = null }) { Text("Видалити") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text("Скасувати") } }) }
}
