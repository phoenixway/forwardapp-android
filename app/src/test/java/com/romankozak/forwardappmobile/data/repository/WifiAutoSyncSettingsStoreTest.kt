package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WifiAutoSyncSettingsStoreTest {
    @Test fun `per-device auto sync switches exclude only their own peer`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsRepository(context)
        val store = WifiAutoSyncSettingsStore(context, settings)
        val priorAddress = settings.desktopSyncAddressFlow.first()
        val priorKey = settings.wifiSyncPeerKeyFlow.first()
        val priorPrimaryEnabled = store.primaryEnabledFlow.first()
        val priorPrimaryName = store.primaryNameFlow.first()
        val priorEnabled = store.enabledFlow.first()
        val priorExtras = store.extraPeersFlow.first()
        try {
            settings.saveWifiSyncPeerConnection("192.168.1.2:8080", "first-key")
            store.setPrimaryEnabled(false)
            store.upsertExtraPeer(WifiAutoSyncPeer("second", "Phone B", "192.168.1.3:8080", "second-key"))

            assertThat(store.peersFlow.first().map { it.id }).containsExactly("second")
            store.setPrimaryEnabled(true)
            store.setExtraPeerEnabled("second", false)
            assertThat(store.peersFlow.first().map { it.id }).containsExactly("primary")
        } finally {
            store.save(priorEnabled, priorExtras)
            store.setPrimaryEnabled(priorPrimaryEnabled)
            store.setPrimaryName(priorPrimaryName)
            settings.saveWifiSyncPeerConnection(priorAddress, priorKey)
        }
    }

    @Test fun `configured primary and additional peers stay local and independent`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsRepository(context)
        val store = WifiAutoSyncSettingsStore(context, settings)
        val priorEnabled = store.enabledFlow.first()
        val priorExtras = store.extraPeersFlow.first()
        val priorAddress = settings.desktopSyncAddressFlow.first()
        val priorKey = settings.wifiSyncPeerKeyFlow.first()
        try {
            settings.saveDesktopSyncAddress("192.168.1.2:8080")
            settings.saveWifiSyncPeerKey("first-key")
            store.save(true, listOf(WifiAutoSyncPeer("second", "Phone B", "192.168.1.3:8080", "second-key")))

            assertThat(store.enabledFlow.first()).isTrue()
            assertThat(store.peersFlow.first().map { it.address })
                .containsExactly("192.168.1.2:8080", "192.168.1.3:8080").inOrder()
            assertThat(settings.getSettingsMap().values.joinToString()).doesNotContain("second-key")
            settings.restoreFromMap(mapOf("wifi_sync_auto_enabled" to "false"))
            assertThat(store.enabledFlow.first()).isTrue()
            assertThat(store.extraPeersFlow.first().single().key).isEqualTo("second-key")
        } finally {
            store.save(priorEnabled, priorExtras)
            settings.saveDesktopSyncAddress(priorAddress)
            settings.saveWifiSyncPeerKey(priorKey)
        }
    }
}
