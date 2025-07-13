package com.romankozak.forwardappmobile

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.google.gson.Gson
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WifiSyncServer(
    private val syncRepository: SyncRepository,
    private val context: Context
) {
    private var server: NettyApplicationEngine? = null

    suspend fun start(): String? {
        // Зупиняємо попередній сервер, якщо він був запущений
        stop()

        return withContext(Dispatchers.IO) {
            try {
                // Отримуємо IP-адресу пристрою
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddressInt = wifiManager.connectionInfo.ipAddress
                if (ipAddressInt == 0) {
                    Log.e("WifiSyncServer", "Не вдалося отримати IP-адресу. Перевірте Wi-Fi з'єднання.")
                    return@withContext null
                }
                val ipAddress = String.format(
                    "%d.%d.%d.%d",
                    ipAddressInt and 0xff,
                    ipAddressInt shr 8 and 0xff,
                    ipAddressInt shr 16 and 0xff,
                    ipAddressInt shr 24 and 0xff
                )

                val port = 8080

                server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        gson {
                            setPrettyPrinting()
                        }
                    }
                    routing {
                        get("/status") {
                            call.respondText("ForwardApp Mobile Server is running!")
                        }
                        get("/export") {
                            Log.d("WifiSyncServer", "Запит на /export отримано.")
                            try {
                                val backupJson = syncRepository.createBackupJsonString()
                                // Відповідаємо JSON-даними
                                call.respond(Gson().fromJson(backupJson, Any::class.java))
                            } catch (e: Exception) {
                                Log.e("WifiSyncServer", "Помилка при створенні бекапу", e)
                                call.respondText("Помилка сервера: ${e.message}")
                            }
                        }
                    }
                }.start(wait = false)

                val fullAddress = "$ipAddress:$port"
                Log.d("WifiSyncServer", "Сервер запущено за адресою: $fullAddress")
                fullAddress
            } catch (e: Exception) {
                Log.e("WifiSyncServer", "Помилка запуску сервера", e)
                null
            }
        }
    }

    fun stop() {
        if (server != null) {
            server?.stop(1000, 2000)
            server = null
            Log.d("WifiSyncServer", "Сервер зупинено.")
        }
    }
}