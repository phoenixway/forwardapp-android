package com.romankozak.forwardappmobile.features.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.util.Locale

/** The LAN address used by the Android Wi-Fi server and shown for peer setup. */
fun currentWifiIpv4Address(context: Context): String? {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networks = listOfNotNull(connectivityManager.activeNetwork) + connectivityManager.allNetworks
        val wifiAddresses = networks.distinct().flatMap { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return@flatMap emptyList()
            connectivityManager.getLinkProperties(network)
                ?.linkAddresses
                ?.mapNotNull { it.address as? Inet4Address }
                .orEmpty()
        }
        return preferredWifiIpv4Address(wifiAddresses)
    }

    @Suppress("DEPRECATION")
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    @Suppress("DEPRECATION")
    val ipAddress = wifiManager.connectionInfo.ipAddress
    if (ipAddress == 0) return null
    return String.format(
        Locale.US,
        "%d.%d.%d.%d",
        ipAddress and 0xff,
        ipAddress shr 8 and 0xff,
        ipAddress shr 16 and 0xff,
        ipAddress shr 24 and 0xff,
    )
}

/** Prefer a LAN address when Android reports several IPv4 addresses on Wi-Fi. */
internal fun preferredWifiIpv4Address(addresses: List<Inet4Address>): String? =
    addresses
        .filterNot { it.isAnyLocalAddress || it.isLoopbackAddress || it.isMulticastAddress }
        .minByOrNull { address ->
            when {
                address.isSiteLocalAddress -> 0
                address.isLinkLocalAddress -> 2
                else -> 1
            }
        }?.hostAddress
