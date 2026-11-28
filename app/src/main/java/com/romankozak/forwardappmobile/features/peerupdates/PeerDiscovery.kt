package com.romankozak.forwardappmobile.features.peerupdates

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.romankozak.forwardappmobile.features.sync.currentWifiIpv4Address
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.coroutines.resume

/** Wi-Fi client first, then a private IPv4 address on a Wi-Fi hotspot interface. */
internal fun peerLanHost(context: Context): String? = currentWifiIpv4Address(context) ?: runCatching {
    NetworkInterface.getNetworkInterfaces().asSequence()
        .filter { it.isUp && it.name.matches(Regex("(wlan|swlan|ap|softap)[0-9]+")) }
        .flatMap { it.inetAddresses.asSequence() }.filterIsInstance<Inet4Address>()
        .firstOrNull { it.isSiteLocalAddress }?.hostAddress
}.getOrNull()

internal class PeerDiscovery(context: Context) {
    private val manager = context.getSystemService(NsdManager::class.java)

    fun advertise(port: Int, onError: () -> Unit): Closeable {
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, error: Int) = onError()
            override fun onUnregistrationFailed(info: NsdServiceInfo, error: Int) = Unit
        }
        manager.registerService(NsdServiceInfo().apply {
            serviceName = "ForwardApp"
            serviceType = PEER_SERVICE
            this.port = port
        }, NsdManager.PROTOCOL_DNS_SD, listener)
        return Closeable { runCatching { manager.unregisterService(listener) } }
    }

    data class Event(val info: NsdServiceInfo, val lost: Boolean)

    fun discover() = callbackFlow {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) = Unit
            override fun onDiscoveryStopped(type: String) = close().let { Unit }
            override fun onStartDiscoveryFailed(type: String, error: Int) {
                close(IllegalStateException("Discovery unavailable. Connect manually."))
            }
            override fun onStopDiscoveryFailed(type: String, error: Int) { close() }
            override fun onServiceFound(info: NsdServiceInfo) { trySend(Event(info, false)) }
            override fun onServiceLost(info: NsdServiceInfo) { trySend(Event(info, true)) }
        }
        manager.discoverServices(PEER_SERVICE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { manager.stopServiceDiscovery(listener) } }
    }

    @Suppress("DEPRECATION")
    suspend fun resolve(info: NsdServiceInfo): PeerAddress? = suspendCancellableCoroutine { continuation ->
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, error: Int) { continuation.resume(null) }
            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = info.host as? Inet4Address
                continuation.resume(host?.hostAddress?.let { runCatching { PeerAddress.parse("$it:${info.port}") }.getOrNull() })
            }
        }
        try {
            manager.resolveService(info, listener)
        } catch (_: IllegalArgumentException) {
            continuation.resume(null)
        }
    }
}
