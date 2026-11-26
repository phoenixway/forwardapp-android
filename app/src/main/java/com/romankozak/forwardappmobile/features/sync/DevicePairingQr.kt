package com.romankozak.forwardappmobile.features.sync

import android.net.Uri

internal data class DevicePairingDetails(val address: String, val key: String)

/** QR contains connection details only. Scanning never saves them without confirmation. */
internal fun encodeDevicePairingQr(address: String, key: String): String = Uri.Builder()
    .scheme("forwardapp-pair")
    .authority("device")
    .appendQueryParameter("v", "1")
    .appendQueryParameter("address", address)
    .appendQueryParameter("key", key)
    .build()
    .toString()

internal fun decodeDevicePairingQr(raw: String): DevicePairingDetails? = runCatching {
    val uri = Uri.parse(raw)
    val address = uri.getQueryParameter("address")?.trim().orEmpty()
    val key = uri.getQueryParameter("key")?.trim().orEmpty()
    val port = address.substringAfterLast(':', "").toIntOrNull()
    if (uri.scheme == "forwardapp-pair" && uri.host == "device" && uri.getQueryParameter("v") == "1" &&
        address.substringBefore(':').isNotBlank() && port != null && port in 1..65535 && key.isNotBlank()) {
        DevicePairingDetails(address, key)
    } else null
}.getOrNull()
