package com.romankozak.forwardappmobile.features.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevicePairingQrTest {
    @Test fun `pairing QR preserves address and key without accepting unrelated codes`() {
        val encoded = encodeDevicePairingQr("192.168.50.142:8080", "key+with/symbols")

        assertThat(decodeDevicePairingQr(encoded))
            .isEqualTo(DevicePairingDetails("192.168.50.142:8080", "key+with/symbols"))
        assertThat(decodeDevicePairingQr("https://example.com/?address=192.168.50.142:8080&key=wrong")).isNull()
        assertThat(decodeDevicePairingQr("forwardapp-pair://device?v=1&address=192.168.50.142&key=x")).isNull()
    }
}
