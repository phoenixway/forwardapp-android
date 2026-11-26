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
        return networks.distinct().firstNotNullOfOrNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return@firstNotNullOfOrNull null
            connectivityManager.getLinkProperties(network)
                ?.linkAddresses
                ?.firstNotNullOfOrNull { (it.address as? Inet4Address)?.hostAddress }
        }
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
