package com.romankozak.forwardappmobile.features.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.BuildConfig
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SubjectConversionSnapshotAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import com.romankozak.forwardappmobile.shared.contracts.sync.WIFI_PEER_KEY_HEADER
import com.romankozak.forwardappmobile.shared.contracts.sync.acceptsWifiPeerKey
import com.romankozak.forwardappmobile.sync.SyncRepository
import com.romankozak.forwardappmobile.sync.LongDeserializer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_SYNC_IMPORT_TAG = "DaySyncImport"

class WifiSyncServer(
    private val syncRepository: SyncRepository,
    private val context: Context,
    private val dayManagementRepository: DayManagementRepository,
    private val settingsRepository: SettingsRepository,
    private val eventJournal: WifiSyncEventJournal,
) {
    private val TAG = "WifiSyncServer"
    private val DEBUG_TAG = "FWD_SYNC_TEST"
    private var server: ApplicationEngine? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapterFactory(SubjectConversionSnapshotAdapter())
            .registerTypeAdapter(Long::class.java, LongDeserializer())
            .setPrettyPrinting()
            .create()
    }

    fun start(port: Int): Result<String> {
        stop()

        return try {
            val ipAddress = currentWifiIpv4Address(context)
            if (ipAddress == null) {
                val errorMessage = "Не вдалося отримати IP-адресу. Перевірте з'єднання з Wi-Fi."
                Log.e(TAG, "Server Start: FAILURE. $errorMessage")
                Log.e(DEBUG_TAG, "[WifiSyncServer] Start failed: $errorMessage")
                return Result.failure(Exception(errorMessage))
            }

            Log.d(TAG, "Server Start: IP address determined as: $ipAddress")
            Log.d(DEBUG_TAG, "[WifiSyncServer] Starting on $ipAddress:$port")

            Log.d(TAG, "Server Start: Attempting to start Ktor server on $ipAddress:$port")

            if (!isPortFree(port, ipAddress)) {
                val msg = "Port $port is already in use. Stop other sync server or pick another port."
                val exception = BindException(msg)
                Log.e(DEBUG_TAG, "[WifiSyncServer] Port check failed: $msg", exception)
                return Result.failure(exception)
            }

            val engine =
                embeddedServer(CIO, port = port, host = ipAddress) {
                    install(ContentNegotiation) {
                        gson {
                            registerTypeAdapterFactory(com.romankozak.forwardappmobile.core.data.models.sync.SubjectConversionSnapshotAdapter())
                            setPrettyPrinting()
                        }
                    }
                    routing {
                        fun dumpToFileDebug(
                            prefix: String,
                            content: String,
                        ) {
                            if (!BuildConfig.DEBUG) return
                            runCatching {
                                // Використовуємо File для сумісності з API 29
                                val dir = File(context.filesDir, "sync-dumps")

                                if (!dir.exists()) {
                                    dir.mkdirs() // Створюємо папку, якщо її немає
                                }

                                val timestamp =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(
                                        Date(),
                                    )
                                val fileName = "${prefix}_$timestamp.json"
                                val file = File(dir, fileName)

                                file.writeText(content)
                                Log.d("WifiSyncServer", "Dump saved: ${file.absolutePath}")
                            }.onFailure { e ->
                                Log.e("WifiSyncServer", "Failed to dump sync data", e)
                            }
                        }

                        get("/status") {
                            if (!acceptsWifiPeerKey(settingsRepository.wifiSyncServerKeyFlow.first(), call.request.headers[WIFI_PEER_KEY_HEADER])) {
                                call.respond(HttpStatusCode.Unauthorized)
                                return@get
                            }
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /status from $remote")
                            call.respondText("ForwardApp Mobile Server is running!")
                        }
                        get("/export") {
                            if (!acceptsWifiPeerKey(settingsRepository.wifiSyncServerKeyFlow.first(), call.request.headers[WIFI_PEER_KEY_HEADER])) {
                                call.respond(HttpStatusCode.Unauthorized)
                                return@get
                            }
                            Log.d("WifiSyncServer", "Запит на /export отримано.")
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /export from $remote")
                            val eventId = eventJournal.begin(WifiAutoSyncPeer("incoming-export", "Запит іншого пристрою", remote, ""), "Вхідний експорт")
                            eventJournal.stage(eventId, "Підготовка даних")
                            try {
                                val deltaSinceParam =
                                    call.request.queryParameters["deltaSince"]
                                        ?: call.request.queryParameters["since"]
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] /export deltaSinceParam=$deltaSinceParam",
                                )
                                val rawBackupJson =
                                    if (deltaSinceParam != null) {
                                        val since = deltaSinceParam.toLongOrNull()
                                        if (since != null) {
                                            Log.d(
                                                DEBUG_TAG,
                                                "[WifiSyncServer] Serving DELTA since=$since",
                                            )
                                            val deltaJson =
                                                syncRepository.createDeltaBackupJsonString(since)
                                            Log.d(
                                                DEBUG_TAG,
                                                "[WifiSyncServer] Delta JSON size=${deltaJson.length}",
                                            )
                                            deltaJson
                                        } else {
                                            Log.w(
                                                DEBUG_TAG,
                                                "[WifiSyncServer] Invalid deltaSince param: $deltaSinceParam, falling back to full export",
                                            )
                                            syncRepository.createFullBackupJsonString()
                                        }
                                    } else {
                                        Log.d(
                                            DEBUG_TAG,
                                            "[WifiSyncServer] No deltaSince param, serving FULL export",
                                        )
                                        syncRepository.createFullBackupJsonString()
                                    }
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] DEBUG_MARK_EXPORT_READY len=${rawBackupJson.length}",
                                )
                                dumpToFileDebug("export", rawBackupJson)
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] /export COMPLETE, JSON size=${rawBackupJson.length} bytes",
                                )
                                call.respondText(rawBackupJson, ContentType.Application.Json)
                                eventJournal.finish(eventId)
                            } catch (e: Exception) {
                                eventJournal.finish(eventId, e)
                                Log.e("WifiSyncServer", "Помилка при створенні бекапу", e)
                                Log.e(DEBUG_TAG, "[WifiSyncServer] /export error ${e.message}", e)
                                call.respondText("SYNC_EXPORT_FAILED", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                            }
                        }
                        post("/import") {
                            if (!acceptsWifiPeerKey(settingsRepository.wifiSyncServerKeyFlow.first(), call.request.headers[WIFI_PEER_KEY_HEADER])) {
                                call.respond(HttpStatusCode.Unauthorized)
                                return@post
                            }
                            Log.d("WifiSyncServer", "Запит на /import отримано.")
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /import from $remote")
                            val eventId = eventJournal.begin(WifiAutoSyncPeer("incoming-import", "Запит іншого пристрою", remote, ""), "Вхідний імпорт")
                            eventJournal.stage(eventId, "Злиття даних")
                            try {
                                val body = call.receiveText()
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] DEBUG_MARK_RECEIVED_IMPORT len=${body.length}",
                                )
                                dumpToFileDebug("import", body)
                                if (BuildConfig.DEBUG) {
                                    Log.d(DEBUG_TAG, "[WifiSyncServer] /import dump head=${body.take(400)}")
                                }
                                // Reject TASK recurrence-v1 while the payload is still raw JSON. Typed
                                // compatibility DTOs intentionally still know these old fields, so checking
                                // only after Gson parsing would allow legacy semantics to survive silently.
                                com.romankozak.forwardappmobile.sync.requireNoLegacyTaskRecurrenceV1(body)

                                // Canonical Wi-Fi import authority is snapshotBundle. Do not deserialize
                                // legacy entity envelopes are intentionally not accepted here: stale shapes can
                                // contain fields that are invalid for current Room entities and must not be
                                // allowed to block an otherwise valid canonical snapshot import.
                                val rawRoot =
                                    com.google.gson.JsonParser.parseString(body).asJsonObject
                                val rawSnapshot = rawRoot.get("snapshotBundle")
                                if (rawSnapshot == null || rawSnapshot.isJsonNull) {
                                    eventJournal.finish(eventId, IllegalArgumentException("Canonical snapshotBundle section is required"))
                                    return@post call.respond(
                                        HttpStatusCode.Companion.BadRequest,
                                        "Canonical snapshotBundle section is required",
                                    )
                                }

                                val canonicalSnapshot =
                                    gson.fromJson(
                                        rawSnapshot,
                                        com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle::class.java,
                                    )
                                val backup =
                                    FullAppBackup(
                                        backupSchemaVersion =
                                            rawRoot.get("backupSchemaVersion")
                                                ?.takeUnless { it.isJsonNull }
                                                ?.asInt
                                                ?: 2,
                                        settings = null,
                                        snapshotBundle = canonicalSnapshot,
                                    )
                                Log.e(DAY_SYNC_IMPORT_TAG, "server received ${backup.describeDayImportPayload(body.length)}")
                                val snapshotBundle = backup.snapshotBundle
                                Log.i(
                                    "ForwardSync",
                                    "wifi import received bytes=${body.length} hasSnapshot=${snapshotBundle != null} " +
                                        "snapshotPlans=${snapshotBundle?.dayPlans?.size ?: 0} " +
                                        "snapshotFocus=${snapshotBundle?.dayFocusItems?.size ?: 0} " +
                                        "snapshotTasks=${snapshotBundle?.dayTasks?.size ?: 0} " +
                                        "snapshotRuntime=${snapshotBundle?.dayManagementRuntimeState != null} " +
                                        "snapshotRuntimePhase=${snapshotBundle?.dayManagementRuntimeState?.currentPhase} " +
                                        "snapshotRuntimeSleepAt=${snapshotBundle?.dayManagementRuntimeState?.sleepAt}",
                                )
                                if (snapshotBundle == null) {
                                    eventJournal.finish(eventId, IllegalArgumentException("Canonical snapshotBundle section is required"))
                                    return@post call.respond(
                                        HttpStatusCode.Companion.BadRequest,
                                        "Canonical snapshotBundle section is required",
                                    )
                                }

                                withContext(NonCancellable) {
                                    syncRepository.applyServerChanges(snapshotBundle).getOrThrow()
                                    Log.i("ForwardSync", "wifi canonical snapshot import applied")
                                }
                                eventJournal.finish(eventId)
                                call.respond(HttpStatusCode.Companion.OK, "Import applied")
                            } catch (e: Exception) {
                                eventJournal.finish(eventId, e)
                                Log.e("WifiSyncServer", "Помилка при імпорті", e)
                                Log.e(DEBUG_TAG, "[WifiSyncServer] /import error ${e.message}", e)
                                call.respond(
                                    HttpStatusCode.Companion.InternalServerError,
                                    "Помилка сервера: ${e.message}",
                                )
                            }
                        }
                    }
                }

            runCatching { engine.start(wait = false) }.getOrElse { bindEx ->
                if (bindEx is BindException) {
                    val msg = "Port $port busy. Stop other sync server or change port."
                    Log.e(DEBUG_TAG, "[WifiSyncServer] Bind failed: $msg", bindEx)
                    return Result.failure(bindEx)
                } else {
                    throw bindEx
                }
            }
            server = engine
            acquireBackgroundServerLocks()

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
        releaseBackgroundServerLocks()
    }

    @Suppress("DEPRECATION")
    private fun acquireBackgroundServerLocks() {
        val appContext = context.applicationContext
        if (wakeLock?.isHeld != true) {
            runCatching {
                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock =
                    powerManager
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${appContext.packageName}:WifiSyncServer")
                        .apply {
                            setReferenceCounted(false)
                            acquire()
                        }
                Log.d(DEBUG_TAG, "[WifiSyncServer] CPU wake lock acquired")
            }.onFailure { error ->
                Log.e(DEBUG_TAG, "[WifiSyncServer] CPU wake lock failed: ${error.message}", error)
            }
        }

        if (wifiLock?.isHeld != true) {
            runCatching {
                val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiLock =
                    wifiManager
                        .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "${appContext.packageName}:WifiSyncServer")
                        .apply {
                            setReferenceCounted(false)
                            acquire()
                        }
                Log.d(DEBUG_TAG, "[WifiSyncServer] Wi-Fi high-performance lock acquired")
            }.onFailure { error ->
                Log.e(DEBUG_TAG, "[WifiSyncServer] Wi-Fi lock failed: ${error.message}", error)
            }
        }
    }

    private fun releaseBackgroundServerLocks() {
        runCatching {
            wifiLock?.takeIf { it.isHeld }?.release()
        }.onFailure { error ->
            Log.w(DEBUG_TAG, "[WifiSyncServer] Wi-Fi lock release failed: ${error.message}")
        }
        wifiLock = null

        runCatching {
            wakeLock?.takeIf { it.isHeld }?.release()
        }.onFailure { error ->
            Log.w(DEBUG_TAG, "[WifiSyncServer] CPU wake lock release failed: ${error.message}")
        }
        wakeLock = null
        Log.d(DEBUG_TAG, "[WifiSyncServer] Background server locks released")
    }

    private fun isPortFree(port: Int, host: String): Boolean {
        return runCatching {
            ServerSocket().use { socket ->
                socket.bind(InetSocketAddress(host, port))
                true
            }
        }.getOrElse { throwable ->
            Log.w(DEBUG_TAG, "[WifiSyncServer] Port $port busy: ${throwable.message}")
            false
        }
    }
}


private fun FullAppBackup.describeDayImportPayload(bodyLength: Int): String {
    val plans = snapshotBundle?.dayPlans.orEmpty()
    val tasks = snapshotBundle?.dayTasks.orEmpty()
    val focus = snapshotBundle?.dayFocusItems.orEmpty()

    return "bytes=$bodyLength snapshotPlans=${plans.size} " +
        "snapshotTasks=${tasks.size} snapshotFocus=${focus.size}"
}
