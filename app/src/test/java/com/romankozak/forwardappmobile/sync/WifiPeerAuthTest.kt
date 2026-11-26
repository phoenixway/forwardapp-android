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
        settings.saveWifiSyncPeerKey("peer-secret")
        try {
            settings.restoreFromMap(mapOf("wifi_sync_server_key" to "injected", "wifi_sync_peer_key" to "injected"))
            assertThat(settings.wifiSyncServerKeyFlow.first()).isEqualTo(serverKey)
            assertThat(settings.wifiSyncPeerKeyFlow.first()).isEqualTo("peer-secret")
            assertThat(settings.getSettingsMap()).doesNotContainKey("wifi_sync_server_key")
            assertThat(settings.getSettingsMap()).doesNotContainKey("wifi_sync_peer_key")
            settings.rotateWifiSyncServerKey()
            assertThat(settings.wifiSyncServerKeyFlow.first()).isNotEqualTo(serverKey)
        } finally {
            settings.saveWifiSyncPeerKey("")
        }
    }
}
