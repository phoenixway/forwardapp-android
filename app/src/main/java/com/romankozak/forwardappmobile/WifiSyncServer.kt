package com.romankozak.forwardappmobile

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.Inet4Address
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.Locale

class WifiSyncServer(
    private val syncRepository: SyncRepository,
    private val context: Context,
) {
    private val TAG = "WifiSyncServer"
    private val DEBUG_TAG = "FWD_SYNC_TEST"
    private var server: ApplicationEngine? = null

    fun start(port: Int): Result<String> {
        stop()

        return try {
            val ipAddress = getWifiIpAddress()
            if (ipAddress == null) {
                val errorMessage = "Не вдалося отримати IP-адресу. Перевірте з'єднання з Wi-Fi."
                Log.e(TAG, "Server Start: FAILURE. $errorMessage")
                Log.e(DEBUG_TAG, "[WifiSyncServer] Start failed: $errorMessage")
                return Result.failure(Exception(errorMessage))
            }

            Log.d(TAG, "Server Start: IP address determined as: $ipAddress")
            Log.d(DEBUG_TAG, "[WifiSyncServer] Starting on $ipAddress:$port")

            Log.d(TAG, "Server Start: Attempting to start Ktor server on $ipAddress:$port")

            val engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                install(ContentNegotiation) { gson { setPrettyPrinting() } }
                routing {
                    fun dumpToFile(prefix: String, content: String) {
                        runCatching {
                            val dir = Path.of(context.filesDir.path, "sync-dumps")
                            Files.createDirectories(dir)
                            val ts = Instant.now().toEpochMilli()
                                val file = dir.resolve("$prefix-$ts.json")
                                Files.write(
                                    file,
                                    content.toByteArray(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING,
                                )
                                Files.list(dir)
                                    .filter { it.fileName.toString().startsWith(prefix) }
                                    .sorted()
                                    .toList()
                                    .let { list ->
                                        if (list.size > 5) {
                                            list.take(list.size - 5).forEach { Files.deleteIfExists(it) }
                                        }
                                    }
                            }
                        }

                        get("/status") {
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /status from $remote")
                            call.respondText("ForwardApp Mobile Server is running!")
                        }
                        get("/export") {
                            Log.d("WifiSyncServer", "Запит на /export отримано.")
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /export from $remote")
                            try {
                                val deltaSinceParam = call.request.queryParameters["deltaSince"] ?: call.request.queryParameters["since"]
                                val backupJson =
                                    if (deltaSinceParam != null) {
                                        val since = deltaSinceParam.toLongOrNull()
                                        if (since != null) {
                                            Log.d(DEBUG_TAG, "[WifiSyncServer] Serving delta since=$since")
                                            syncRepository.createDeltaBackupJsonString(since)
                                        } else {
                                            Log.w(DEBUG_TAG, "[WifiSyncServer] Invalid deltaSince param: $deltaSinceParam, falling back to full export")
                                            syncRepository.createFullBackupJsonString()
                                        }
                                    } else {
                                        syncRepository.createFullBackupJsonString()
                                    }
                                dumpToFile("export", backupJson)
                                Log.d(DEBUG_TAG, "[WifiSyncServer] /export dump head=${backupJson.take(400)}")
                                call.respondText(backupJson, ContentType.Application.Json)
                            } catch (e: Exception) {
                                Log.e("WifiSyncServer", "Помилка при створенні бекапу", e)
                                Log.e(DEBUG_TAG, "[WifiSyncServer] /export error ${e.message}", e)
                                call.respondText("Помилка сервера: ${e.message}")
                            }
                        }
                        post("/import") {
                            Log.d("WifiSyncServer", "Запит на /import отримано.")
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /import from $remote")
                            try {
                                val body = call.receiveText()
                                dumpToFile("import", body)
                                Log.d(DEBUG_TAG, "[WifiSyncServer] /import dump head=${body.take(400)}")
                                val backup = Gson().fromJson(body, FullAppBackup::class.java)
                                val db = backup.database ?: return@post call.respond(HttpStatusCode.BadRequest, "Database section is missing")

                                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                    syncRepository.applyServerChanges(db)
                                }
                                call.respond(HttpStatusCode.OK, "Import applied")
                            } catch (e: Exception) {
                                Log.e("WifiSyncServer", "Помилка при імпорті", e)
                                Log.e(DEBUG_TAG, "[WifiSyncServer] /import error ${e.message}", e)
                                call.respond(HttpStatusCode.InternalServerError, "Помилка сервера: ${e.message}")
                            }
                        }
                    }
                }
            }

            runCatching { engine.start(wait = false) }.getOrElse { bindEx ->
                if (bindEx is java.net.BindException) {
                    val msg = "Port $port busy. Stop other sync server or change port."
                    Log.e(DEBUG_TAG, "[WifiSyncServer] Bind failed: $msg", bindEx)
                    return Result.failure(bindEx)
                } else {
                    throw bindEx
                }
            }
            server = engine

            Log.i(TAG, "Server started successfully on $ipAddress:$port")
            Log.d(DEBUG_TAG, "[WifiSyncServer] Started at $ipAddress:$port")
            Result.success("$ipAddress:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Server Start: A critical error occurred", e)
            Log.e(DEBUG_TAG, "[WifiSyncServer] Critical error ${e.message}", e)
            Result.failure(e)
        }
    }

    fun stop() {
        if (server != null) {
            Log.d(TAG, "Server Stop: Stopping Ktor server.")
            Log.d(DEBUG_TAG, "[WifiSyncServer] Stopping server")
            server?.stop(1000, 2000)
            server = null
            Log.d(TAG, "Server stopped.")
            Log.d(DEBUG_TAG, "[WifiSyncServer] Server stopped")
        }
    }

    private fun getWifiIpAddress(): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return null
            }

            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is Inet4Address) {
                    return address.hostAddress
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val ipAddressInt = wifiManager.connectionInfo.ipAddress
            if (ipAddressInt == 0) return null
            return String.format(
                Locale.US,
                "%d.%d.%d.%d",
                ipAddressInt and 0xff,
                ipAddressInt shr 8 and 0xff,
                ipAddressInt shr 16 and 0xff,
                ipAddressInt shr 24 and 0xff,
            )
        }
        return null
    }
}
