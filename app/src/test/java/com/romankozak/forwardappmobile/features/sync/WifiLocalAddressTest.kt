package com.romankozak.forwardappmobile.features.sync

import com.google.common.truth.Truth.assertThat
import java.net.Inet4Address
import java.net.InetAddress
import org.junit.Test

class WifiLocalAddressTest {
    @Test fun `LAN address wins over other Wi-Fi IPv4 addresses`() {
        assertThat(
            preferredWifiIpv4Address(
                listOf(ip("203.0.113.4"), ip("169.254.2.3"), ip("192.168.1.12")),
            ),
        ).isEqualTo("192.168.1.12")
    }

    @Test fun `public address remains usable when Wi-Fi has no private address`() {
        assertThat(preferredWifiIpv4Address(listOf(ip("127.0.0.1"), ip("203.0.113.4"))))
            .isEqualTo("203.0.113.4")
    }

    private fun ip(value: String): Inet4Address = InetAddress.getByName(value) as Inet4Address
}
