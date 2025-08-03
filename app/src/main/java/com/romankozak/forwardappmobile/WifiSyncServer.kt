package com.romankozak.forwardappmobile

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.sync.SyncRepository
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
}