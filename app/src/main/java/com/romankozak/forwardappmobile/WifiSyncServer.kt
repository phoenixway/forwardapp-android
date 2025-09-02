// Файл: WifiSyncServer.kt

package com.romankozak.forwardappmobile

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*
import java.net.Inet4Address
import java.util.Locale

class WifiSyncServer(
    private val syncRepository: SyncRepository,
    private val context: Context
) {
    private val TAG = "WifiSyncServer"
    private var server: ApplicationEngine? = null

    fun start(): Result<String> {
        stop()

        return try {
            val ipAddress = getWifiIpAddress()
            if (ipAddress == null) {
                val errorMessage = "Не вдалося отримати IP-адресу. Перевірте з'єднання з Wi-Fi."
                Log.e(TAG, "Server Start: FAILURE. $errorMessage")
                return Result.failure(Exception(errorMessage))
            }

            Log.d(TAG, "Server Start: IP address determined as: $ipAddress")

            val port = 8080
            Log.d(TAG, "Server Start: Attempting to start Ktor server on $ipAddress:$port")

            server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                install(ContentNegotiation) { gson { setPrettyPrinting() } }
                routing {
                    get("/status") {
                        call.respondText("ForwardApp Mobile Server is running!")
                    }
                    get("/export") {
                        Log.d("WifiSyncServer", "Запит на /export отримано.")
                        try {
                            // --- ВИПРАВЛЕНО: Повертаємо виклик методу для десктопного бекапу ---
                            val backupJson = syncRepository.createBackupJsonString()
                            // --- ВИПРАВЛЕНО: Правильно відправляємо готову JSON-стрічку ---
                            call.respondText(backupJson, ContentType.Application.Json)
                        } catch (e: Exception) {
                            Log.e("WifiSyncServer", "Помилка при створенні бекапу", e)
                            call.respondText("Помилка сервера: ${e.message}")
                        }
                    }
                }
            }.start(wait = false)

            Log.i(TAG, "Server started successfully on $ipAddress:$port")
            Result.success("$ipAddress:$port")

        } catch (e: Exception) {
            Log.e(TAG, "Server Start: A critical error occurred", e)
            Result.failure(e)
        }
    }

    fun stop() {
        if (server != null) {
            Log.d(TAG, "Server Stop: Stopping Ktor server.")
            server?.stop(1000, 2000)
            server = null
            Log.d(TAG, "Server stopped.")
        }
    }

    /**
     * Сучасний та надійний спосіб отримати IPv4-адресу пристрою в мережі Wi-Fi.
     */
    private fun getWifiIpAddress(): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return null // Не підключено до Wi-Fi
            }

            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is Inet4Address) {
                    // Повертаємо хост-адресу без додаткових символів
                    return address.hostAddress
                }
            }
        } else {
            // Застарілий метод для старих версій Android (API < 23)
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ipAddressInt = wifiManager.connectionInfo.ipAddress
            if (ipAddressInt == 0) return null
            return String.format(
                Locale.US, // Використовуємо Locale.US для правильного форматування IP
                "%d.%d.%d.%d",
                ipAddressInt and 0xff,
                ipAddressInt shr 8 and 0xff,
                ipAddressInt shr 16 and 0xff,
                ipAddressInt shr 24 and 0xff
            )
        }
        return null
    }
}

/*
package com.romankozak.forwardappmobile

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

class WifiSyncServer(
    private val syncRepository: SyncRepository,
    private val context: Context
) {
    private var server: ApplicationEngine? = null
    private val TAG = "WIFI_DEBUG"

    // ЗМІНА: Функція більше НЕ є suspend. Вона просто робить свою роботу і повертає результат.
    fun start(): Result<String> {
        stop()

        return try {
            Log.d(TAG, "Server Start: Attempting to get WifiManager.")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddressInt = wifiManager.connectionInfo.ipAddress
            Log.d(TAG, "Server Start: Raw IP integer is: $ipAddressInt")

            if (ipAddressInt == 0) {
                val errorMessage = "Не вдалося отримати IP-адресу (результат 0). Перевірте Wi-Fi з'єднання."
                Log.e(TAG, "Server Start: FAILURE. $errorMessage")
                return Result.failure(Exception(errorMessage))
            }
            val ipAddress = String.format(
                "%d.%d.%d.%d",
                ipAddressInt and 0xff,
                ipAddressInt shr 8 and 0xff,
                ipAddressInt shr 16 and 0xff,
                ipAddressInt shr 24 and 0xff
            )
            Log.d(TAG, "Server Start: IP address formatted as: $ipAddress")

            val port = 8080
            Log.d(TAG, "Server Start: Attempting to start Ktor server on $ipAddress:$port")

            server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                install(ContentNegotiation) { gson { setPrettyPrinting() } }
                routing {
                    get("/status") { call.respondText("ForwardApp Mobile Server is running!") }
                    get("/export") {
                        Log.d("WifiSyncServer", "Запит на /export отримано.")
                        try {
                            val backupJson = syncRepository.createBackupJsonString()
                            call.respond(Gson().fromJson(backupJson, Any::class.java))
                        } catch (e: Exception) {
                            Log.e("WifiSyncServer", "Помилка при створенні бекапу", e)
                            call.respondText("Помилка сервера: ${e.message}")
                        }
                    }
                }
            }.start(wait = false)

            val fullAddress = "$ipAddress:$port"
            Log.d(TAG, "Server Start: SUCCESS. Ktor server started at $fullAddress")
            Result.success(fullAddress)
        } catch (e: Exception) {
            Log.e(TAG, "Server Start: CRITICAL FAILURE in try-catch block.", e)
            Result.failure(e)
        }
    }

    fun stop() {
        if (server != null) {
            server?.stop(1000, 2000)
            server = null
            Log.d(TAG, "Server Stop: Server stopped successfully.")
        }
    }
}*/


