package com.romankozak.forwardappmobile.shared.contracts.sync

import java.security.MessageDigest

const val WIFI_PEER_KEY_HEADER = "X-ForwardApp-Sync-Key"

/** A missing key never grants access; equality does not short-circuit on the first byte. */
fun acceptsWifiPeerKey(expected: String, provided: String?): Boolean {
    if (expected.isBlank() || provided.isNullOrBlank()) return false
    return MessageDigest.isEqual(
        expected.toByteArray(Charsets.UTF_8),
        provided.toByteArray(Charsets.UTF_8),
    )
}
