package com.romankozak.forwardappmobile.features.sync

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncSettingsStore
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class DeviceSyncUiState(
    val peers: List<WifiAutoSyncPeer> = emptyList(),
    val autoSyncEnabled: Boolean = false,
    val localConnectionEnabled: Boolean = false,
    val listenerStatus: WifiSyncStatus = WifiSyncStatus.Disabled,
    val localAddress: String? = null,
    val localPort: Int = 8080,
    val localKey: String = "",
    val syncingPeerId: String? = null,
    val message: String? = null,
)

@HiltViewModel
class DeviceSyncViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val store: WifiAutoSyncSettingsStore,
    private val syncRepository: SyncRepository,
    private val journal: WifiSyncEventJournal,
    private val syncUseCase: SyncUseCase,
    private val application: Application,
) : ViewModel() {
    private val _state = MutableStateFlow(DeviceSyncUiState())
    val state = _state.asStateFlow()
    private val _events = MutableStateFlow<List<WifiSyncEvent>>(emptyList())
    val events = _events.asStateFlow()

    init {
        viewModelScope.launch {
            settings.ensureWifiSyncServerKey()
            settings.wifiSyncPrimaryPeerFlow.collect { refreshPeers() }
        }
        viewModelScope.launch { store.extraPeersFlow.collect { refreshPeers() } }
        viewModelScope.launch { store.primaryEnabledFlow.collect { refreshPeers() } }
        viewModelScope.launch { store.primaryNameFlow.collect { refreshPeers() } }
        viewModelScope.launch { store.enabledFlow.collect { enabled -> _state.update { it.copy(autoSyncEnabled = enabled) } } }
        viewModelScope.launch { settings.wifiSyncServerEnabledFlow.collect { enabled -> _state.update { it.copy(localConnectionEnabled = enabled) } } }
        viewModelScope.launch { settings.wifiSyncServerKeyFlow.collect { key -> _state.update { it.copy(localKey = key) } } }
        viewModelScope.launch { settings.wifiSyncPortFlow.collect { port -> _state.update { it.copy(localPort = port) } } }
        viewModelScope.launch { syncUseCase.syncUiState.collect { sync -> _state.update { it.copy(listenerStatus = sync.syncStatus) } } }
        viewModelScope.launch { journal.eventsFlow.collect { entries -> _events.value = entries } }
        refreshLocalAddress()
    }

    private suspend fun refreshPeers() {
        val (address, key) = settings.wifiSyncPrimaryPeerFlow.first()
        val primaryEnabled = store.primaryEnabledFlow.first()
        val primaryName = store.primaryNameFlow.first()
        val primary = if (address.isNotBlank() || key.isNotBlank()) {
            listOf(WifiAutoSyncPeer("primary", primaryName, address, key, primaryEnabled))
        } else emptyList()
        _state.update { it.copy(peers = primary + store.extraPeersFlow.first()) }
    }

    fun refreshLocalAddress() {
        _state.update { it.copy(localAddress = currentWifiIpv4Address(application)) }
    }

    fun refreshConnectionAddress() {
        refreshLocalAddress()
        syncUseCase.refreshWifiServerAddress()
    }

    fun setLocalConnectionEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.saveWifiSyncServerEnabled(enabled) }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            store.setEnabled(enabled)
            if (enabled) settings.saveWifiSyncServerEnabled(true)
        }
    }

    fun savePeer(peer: WifiAutoSyncPeer) = viewModelScope.launch {
        val port = peer.address.substringAfterLast(':', "").toIntOrNull()
        if (peer.address.substringBefore(':').isBlank() || port == null || port !in 1..65535 || peer.key.isBlank()) {
            _state.update { it.copy(message = "Вкажіть адресу іншого пристрою у форматі IP:порт і його ключ") }
            return@launch
        }
        runCatching {
            if (peer.id == "primary") {
                settings.saveWifiSyncPeerConnection(peer.address, peer.key)
                store.setPrimaryEnabled(peer.enabled)
                store.setPrimaryName(peer.name)
            } else {
                store.upsertExtraPeer(peer.copy(name = peer.name.ifBlank { peer.address }))
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.update { it.copy(message = error.message ?: "Не вдалося зберегти пристрій") }
        }
    }

    fun newPeer(name: String, address: String, key: String) =
        savePeer(WifiAutoSyncPeer(UUID.randomUUID().toString(), name, address, key))

    fun removePeer(peer: WifiAutoSyncPeer) = viewModelScope.launch {
        if (peer.id == "primary") settings.saveWifiSyncPeerConnection("", "")
        else store.removeExtraPeer(peer.id)
    }

    fun setPeerAutoSync(peer: WifiAutoSyncPeer, enabled: Boolean) = viewModelScope.launch {
        if (peer.id == "primary") store.setPrimaryEnabled(enabled)
        else store.setExtraPeerEnabled(peer.id, enabled)
    }

    fun syncNow(peer: WifiAutoSyncPeer) = viewModelScope.launch {
        if (_state.value.syncingPeerId != null) return@launch
        _state.update { it.copy(syncingPeerId = peer.id, message = null) }
        val eventId = journal.begin(peer, "Вручну")
        try {
            withTimeout(90_000L) { exchangeFullSnapshotWithPeer(syncRepository, peer) { journal.stage(eventId, it) } }
            journal.finish(eventId)
            _state.update { it.copy(message = "${peer.name}: синхронізовано") }
        } catch (error: TimeoutCancellationException) {
            journal.finish(eventId, error)
            _state.update { it.copy(message = "Час очікування вичерпано. Перевірте Wi‑Fi, адресу й приймання на іншому пристрої.") }
        } catch (error: CancellationException) {
            withContext(NonCancellable) { journal.finish(eventId, error) }
            throw error
        } catch (error: Exception) {
            journal.finish(eventId, error)
            _state.update { it.copy(message = wifiSyncErrorMessage(error)) }
        } finally {
            _state.update { it.copy(syncingPeerId = null) }
        }
    }
}
