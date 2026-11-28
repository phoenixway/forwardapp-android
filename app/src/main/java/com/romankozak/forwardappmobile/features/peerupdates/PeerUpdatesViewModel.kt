package com.romankozak.forwardappmobile.features.peerupdates

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.io.File

internal data class UpdatePeer(val address: PeerAddress, val manifest: PeerManifest?, val status: PeerCompatibility)

internal sealed interface PeerOperation {
    data object Idle : PeerOperation
    data class Working(val label: String, val progress: Float? = null) : PeerOperation
    data class Ready(val file: File, val manifest: PeerManifest) : PeerOperation
    data class Message(val text: String) : PeerOperation
    data object Installing : PeerOperation
}

internal class PeerUpdatesViewModel(private val app: Application) : AndroidViewModel(app) {
    private val packages = PeerPackages(app)
    private val transfer = PeerTransfer()
    private val discovery = PeerDiscovery(app)
    private val installer = PeerInstallation(app)
    private val downloads = File(app.cacheDir, "peer-download")
    private var work: Job? = null
    private var scanning: Job? = null
    private var sharing: Job? = null

    val installed = MutableStateFlow<InstalledPeerPackage?>(null)
    val operation = MutableStateFlow<PeerOperation>(PeerOperation.Idle)
    val peers = MutableStateFlow<List<UpdatePeer>>(emptyList())
    val share = MutableStateFlow<PeerShare?>(null)
    val searching = MutableStateFlow(false)
    val lanHost = MutableStateFlow<String?>(null)
    val installPrompt: StateFlow<PeerInstallPrompt?> = PeerInstallEvents.prompt
    val shareIntent = MutableStateFlow<Intent?>(null)
    val browserUrl get() = share.value?.let { "http://${it.address}/v1/apk/${it.code}" }

    init {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    downloads.deleteRecursively()
                    installed.value = packages.own()
                }
                refresh()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                operation.value = PeerOperation.Message(error.message ?: "Could not read installed APK")
            }
        }
    }

    fun refresh() {
        lanHost.value = peerLanHost(app)
        val version = installed.value?.manifest?.versionCode ?: return
        if (operation.value is PeerOperation.Ready) return
        val outcome = installer.outcome(version)
        if (installer.awaitingConfirmation()) operation.value = PeerOperation.Installing
        else if (outcome != null) operation.value = PeerOperation.Message(outcome)
        else if (operation.value == PeerOperation.Installing) operation.value = PeerOperation.Idle
    }

    fun find() {
        scanning?.cancel()
        scanning = viewModelScope.launch {
            searching.value = true
            lanHost.value = peerLanHost(app)
            val addresses = mutableMapOf<String, PeerAddress>()
            try {
                withTimeoutOrNull(15_000) {
                    discovery.discover().collect { event ->
                        val name = event.info.serviceName
                        if (event.lost) addresses.remove(name)?.let { disconnected ->
                            peers.value = peers.value.map {
                                if (it.address == disconnected) it.copy(status = PeerCompatibility.UNREACHABLE) else it
                            }
                        } else {
                            val address = withTimeoutOrNull(3_000) { discovery.resolve(event.info) }
                            if (address != null && address != share.value?.address) {
                                addresses[name] = address
                                probe(address)
                            }
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (operation.value == PeerOperation.Idle) operation.value = PeerOperation.Message("Discovery unavailable. Connect manually.")
            } finally {
                searching.value = false
            }
        }
    }

    fun connect(address: String) = execute("Connecting") {
        probe(PeerAddress.parse(address))
        operation.value = PeerOperation.Idle
    }

    private suspend fun probe(address: PeerAddress) {
        val local = installed.value ?: return
        val peer = try {
            val manifest = withContext(Dispatchers.IO) { transfer.manifest(address) }
            UpdatePeer(address, manifest, packages.status(local.manifest, manifest))
        } catch (_: TimeoutCancellationException) {
            UpdatePeer(address, null, PeerCompatibility.UNREACHABLE)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            UpdatePeer(address, null, PeerCompatibility.UNREACHABLE)
        }
        peers.value = (peers.value.filter { it.address != address } + peer).takeLast(30)
    }

    fun startSharing() {
        if (sharing?.isActive == true) return
        val pkg = installed.value ?: return
        sharing = viewModelScope.launch {
            var server: PeerShare? = null
            var advertisement: Closeable? = null
            try {
                val host = requireNotNull(peerLanHost(app)) { "Connect to Wi-Fi or enable a local hotspot" }
                val started = withContext(Dispatchers.IO) {
                    sharePeerPackage(pkg, PeerAddress.parse("$host:$PEER_PORT")).also { server = it }
                }
                advertisement = runCatching { discovery.advertise(started.address.port) {
                    if (operation.value == PeerOperation.Idle) operation.value = PeerOperation.Message("Use the address below to connect manually")
                } }.getOrNull()
                share.value = started
                delay(10 * 60_000L)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (operation.value == PeerOperation.Idle) operation.value = PeerOperation.Message(error.message ?: "Sharing failed")
            } finally {
                advertisement?.close()
                server?.close()
                share.value = null
            }
        }
    }

    fun stopSharing() { sharing?.cancel() }
    fun stopLan() {
        stopSharing()
        scanning?.cancel()
    }

    fun download(peer: UpdatePeer, code: String) = execute("Downloading", progress = 0f) {
        val manifest = requireNotNull(peer.manifest)
        val own = requireNotNull(installed.value)
        check(own.standalone) { "Split APK installations cannot be updated here" }
        check(packages.status(own.manifest, manifest) == PeerCompatibility.NEWER) { "No compatible newer version" }
        withContext(Dispatchers.IO) {
            check(downloads.isDirectory || downloads.mkdirs()) { "Unable to create download directory" }
            check(downloads.usableSpace / 2 >= manifest.size + 8_388_608) { "Not enough space for download and installation" }
            val file = File.createTempFile("peer-", ".apk", downloads)
            try {
                val digest = transfer.download(peer.address, code.trim().lowercase(), manifest, file) { done ->
                    operation.value = PeerOperation.Working("Downloading", done.toFloat() / manifest.size)
                }
                packages.verify(file, manifest, digest)
                operation.value = PeerOperation.Ready(file, manifest)
            } finally {
                if ((operation.value as? PeerOperation.Ready)?.file != file) file.delete()
            }
        }
    }

    fun installAllowed(): Boolean = installer.permitted()

    fun install() {
        val ready = operation.value as? PeerOperation.Ready ?: return
        // Keep the verified file until Android grants installation permission.
        if (!installer.permitted()) return
        operation.value = PeerOperation.Idle
        execute("Preparing Android installer") {
            try {
                withContext(Dispatchers.IO) { installer.stage(ready.file, ready.manifest) }
                refresh()
            } finally {
                ready.file.delete()
            }
        }
    }

    fun cancel() {
        when (val active = operation.value) {
            is PeerOperation.Ready -> { active.file.delete(); operation.value = PeerOperation.Idle }
            PeerOperation.Installing -> { installer.cancel(); refresh() }
            else -> work?.cancel()
        }
    }

    fun export(uri: Uri?) = execute("Exporting APK") {
        val pkg = requireNotNull(installed.value)
        check(pkg.standalone) { "Split APK installations cannot be exported" }
        withContext(Dispatchers.IO) {
            if (uri == null) {
                val dir = File(app.cacheDir, "peer-export")
                check(dir.isDirectory || dir.mkdirs()) { "Unable to create export directory" }
                val file = File(dir, "ForwardApp-${pkg.manifest.versionCode}.apk")
                try {
                    file.outputStream().use { output -> pkg.file.inputStream().use { copyApk(it, output, pkg.manifest.size) } }
                } catch (error: Throwable) {
                    file.delete()
                    throw error
                }
                val content = FileProvider.getUriForFile(app, "${app.packageName}.peer-apk", file)
                shareIntent.value = Intent(Intent.ACTION_SEND).setType(APK_MIME).putExtra(Intent.EXTRA_STREAM, content)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                try {
                    requireNotNull(app.contentResolver.openOutputStream(uri, "wt")).use { output ->
                        pkg.file.inputStream().use { copyApk(it, output, pkg.manifest.size) }
                    }
                } catch (error: Throwable) {
                    runCatching { app.contentResolver.delete(uri, null, null) }
                    throw error
                }
            }
        }
        operation.value = PeerOperation.Message(if (uri == null) "APK ready to share" else "APK exported")
    }

    private fun execute(label: String, progress: Float? = null, block: suspend () -> Unit) {
        if (work?.isActive == true || operation.value is PeerOperation.Ready || installer.awaitingConfirmation()) return
        work = viewModelScope.launch {
            operation.value = PeerOperation.Working(label, progress)
            try {
                block()
            } catch (_: TimeoutCancellationException) {
                operation.value = PeerOperation.Message("Connection timed out. Try again.")
            } catch (error: CancellationException) {
                operation.value = PeerOperation.Idle
                throw error
            } catch (error: Exception) {
                operation.value = PeerOperation.Message(error.message ?: "Operation failed")
            }
        }
    }

    override fun onCleared() {
        stopLan()
        work?.cancel()
        (operation.value as? PeerOperation.Ready)?.file?.delete()
        transfer.close()
    }
}
