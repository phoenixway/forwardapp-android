package com.romankozak.forwardappmobile.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.shared.contracts.sync.acceptsWifiPeerKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WifiPeerAuthTest {
    @Test fun `only the current nonempty key grants access`() {
        assertThat(acceptsWifiPeerKey("", "")).isFalse()
        assertThat(acceptsWifiPeerKey("secret", null)).isFalse()
        assertThat(acceptsWifiPeerKey("secret", "secreu")).isFalse()
        assertThat(acceptsWifiPeerKey("secret", "secret")).isTrue()
    }

    @Test fun `restore cannot replace local pairing keys`() = runBlocking<Unit> {
        val settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
        val serverKey = settings.ensureWifiSyncServerKey()
        val oldAddress = settings.desktopSyncAddressFlow.first()
        val oldPeerKey = settings.wifiSyncPeerKeyFlow.first()
        val oldPort = settings.wifiSyncPortFlow.first()
        val oldServerEnabled = settings.wifiSyncServerEnabledFlow.first()
        settings.saveWifiSyncPeerConnection("192.168.1.20:8080", "peer-secret")
        settings.saveWifiSyncPort(8081)
        settings.saveWifiSyncServerEnabled(false)
        try {
            settings.restoreFromMap(mapOf(
                "wifi_sync_server_key" to "injected",
                "wifi_sync_peer_key" to "injected",
                "desktop_sync_address" to "attacker:8080",
                "wifi_sync_port" to "9999",
                "wifi_sync_server_enabled" to "true",
            ))
            assertThat(settings.wifiSyncServerKeyFlow.first()).isEqualTo(serverKey)
            assertThat(settings.wifiSyncPeerKeyFlow.first()).isEqualTo("peer-secret")
            assertThat(settings.desktopSyncAddressFlow.first()).isEqualTo("192.168.1.20:8080")
            assertThat(settings.wifiSyncPortFlow.first()).isEqualTo(8081)
            assertThat(settings.wifiSyncServerEnabledFlow.first()).isFalse()
            assertThat(settings.getSettingsMap()).doesNotContainKey("wifi_sync_server_key")
            assertThat(settings.getSettingsMap()).doesNotContainKey("wifi_sync_peer_key")
            settings.rotateWifiSyncServerKey()
            assertThat(settings.wifiSyncServerKeyFlow.first()).isNotEqualTo(serverKey)
        } finally {
            settings.saveWifiSyncPeerConnection(oldAddress, oldPeerKey)
            settings.saveWifiSyncPort(oldPort)
            settings.saveWifiSyncServerEnabled(oldServerEnabled)
        }
    }
}
