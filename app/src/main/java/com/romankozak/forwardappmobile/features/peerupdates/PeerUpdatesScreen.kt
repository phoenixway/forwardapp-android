package com.romankozak.forwardappmobile.features.peerupdates

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PeerUpdatesScreen(onBack: () -> Unit, model: PeerUpdatesViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val installed by model.installed.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val peers by model.peers.collectAsStateWithLifecycle()
    val share by model.share.collectAsStateWithLifecycle()
    val host by model.lanHost.collectAsStateWithLifecycle()
    val searching by model.searching.collectAsStateWithLifecycle()
    val prompt by model.installPrompt.collectAsStateWithLifecycle()
    val shareIntent by model.shareIntent.collectAsStateWithLifecycle()
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var failure by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<PeerDialog?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(APK_MIME)) { uri ->
        if (uri != null) model.export(uri)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (model.installAllowed()) model.install()
        else failure = "Android installation permission was not granted"
    }
    val confirmation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.refresh()
    }
    fun launchShare(intent: Intent) {
        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
            .onFailure { failure = "No app could open the share menu" }
    }
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { resumed = true; model.refresh() }
                Lifecycle.Event.ON_PAUSE -> resumed = false
                Lifecycle.Event.ON_STOP -> model.stopLan()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); model.stopLan() }
    }
    LaunchedEffect(prompt, resumed) {
        val action = prompt ?: return@LaunchedEffect
        if (resumed) {
            PeerInstallEvents.prompt.value = null
            runCatching { confirmation.launch(action.intent) }
                .onFailure { model.cancel(); failure = "Could not open Android installer" }
        }
    }
    LaunchedEffect(shareIntent) {
        val intent = shareIntent ?: return@LaunchedEffect
        model.shareIntent.value = null
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
            intent.clipData = ClipData.newRawUri("ForwardApp APK", uri)
        }
        launchShare(intent)
    }

    val busy = operation is PeerOperation.Working || operation is PeerOperation.Ready || operation == PeerOperation.Installing
    val standalone = installed?.standalone == true
    Scaffold(topBar = {
        TopAppBar(title = { Text("Peer Updates") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                installed?.let { current ->
                    Text("ForwardApp ${current.manifest.versionName}", style = MaterialTheme.typography.titleMedium)
                    Text("Version code ${current.manifest.versionCode} · ${current.manifest.variant}",
                        style = MaterialTheme.typography.bodySmall)
                    if (!current.standalone) Text("Split APK packages are unsupported.")
                }
                Text(host?.let { "LAN · $it" } ?: "No local address", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = model::find, enabled = !searching && installed != null) {
                        Text(if (searching) "Finding updates…" else "Find updates")
                    }
                    TextButton(onClick = { if (share == null) model.startSharing() else model.stopSharing() }, enabled = standalone) {
                        Text(if (share == null) "Share my version" else "Stop sharing")
                    }
                    TextButton(onClick = { dialog = PeerDialog.Export }, enabled = standalone && !busy) { Text("Export APK") }
                    TextButton(onClick = { dialog = PeerDialog.Connect }, enabled = !busy && installed != null) { Text("Connect manually") }
                }
            }
            share?.let { active ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Sharing for up to 10 minutes", style = MaterialTheme.typography.titleSmall)
                            SelectionContainer { Text("${active.address}\nShare code  ${active.code}") }
                            TextButton(onClick = {
                                model.browserUrl?.let { link ->
                                    launchShare(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link))
                                }
                            }) { Text("Share browser link") }
                        }
                    }
                }
            }
            item {
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (val state = operation) {
                    PeerOperation.Idle -> Unit
                    PeerOperation.Installing -> {
                        Text("Waiting for Android installer")
                        TextButton(onClick = model::cancel) { Text("Cancel") }
                    }
                    is PeerOperation.Working -> {
                        Text(state.label)
                        state.progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
                        TextButton(onClick = model::cancel) { Text("Cancel") }
                    }
                    is PeerOperation.Message -> Text(state.text)
                    is PeerOperation.Ready -> {
                        Text("Verified ForwardApp ${state.manifest.versionName}")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = {
                                failure = null
                                if (model.installAllowed()) model.install()
                                else runCatching {
                                    permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                                }.onFailure { failure = "Android installation settings are unavailable" }
                            }) { Text("Install update") }
                            TextButton(onClick = model::cancel) { Text("Discard") }
                        }
                    }
                }
            }
            items(peers, key = { it.address.toString() }) { peer ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(peer.manifest?.deviceName ?: peer.address.toString(), style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(peer.address.toString(), peer.manifest?.let { "${it.versionName} · ${it.versionCode} · ${it.variant}" }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall)
                        Text(peer.status.label)
                        if (peer.status == PeerCompatibility.NEWER && standalone) {
                            FilledTonalButton(onClick = { dialog = PeerDialog.Update(peer) }, enabled = !busy) { Text("Update from peer") }
                        }
                    }
                }
            }
        }
    }
    when (val current = dialog) {
        null -> Unit
        PeerDialog.Export -> AlertDialog(
            onDismissRequest = { dialog = null }, title = { Text("Export APK") },
            confirmButton = { TextButton(onClick = {
                dialog = null
                save.launch("ForwardApp-${installed?.manifest?.versionCode}.apk")
            }) { Text("Save file") } },
            dismissButton = { TextButton(onClick = { dialog = null; model.export(null) }) { Text("Share APK") } },
        )
        PeerDialog.Connect -> PeerInput("Connect manually", "IP address and port", onDismiss = { dialog = null }) {
            failure = null
            dialog = null
            model.connect(it)
        }
        is PeerDialog.Update -> PeerInput("Update from peer", "Share code", onDismiss = { dialog = null }) {
            failure = null
            dialog = null
            model.download(current.peer, it)
        }
    }
}

private sealed interface PeerDialog {
    data object Export : PeerDialog
    data object Connect : PeerDialog
    data class Update(val peer: UpdatePeer) : PeerDialog
}

@Composable
private fun PeerInput(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(input, { input = it.take(80) }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(input) }, enabled = input.isNotBlank()) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
