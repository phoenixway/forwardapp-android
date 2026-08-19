package com.romankozak.forwardappmobile.features.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.BuildConfig
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.sync.SyncRepository
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
import java.io.File
import java.net.BindException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DAY_SYNC_IMPORT_TAG = "DaySyncImport"

class WifiSyncServer(
    private val syncRepository: SyncRepository,
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val dayManagementRepository: DayManagementRepository,
) {
    private val TAG = "WifiSyncServer"
    private val DEBUG_TAG = "FWD_SYNC_TEST"
    private var server: ApplicationEngine? = null

    private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .create()
    }

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

            if (!isPortFree(port)) {
                val msg = "Port $port is already in use. Stop other sync server or pick another port."
                val exception = BindException(msg)
                Log.e(DEBUG_TAG, "[WifiSyncServer] Port check failed: $msg", exception)
                return Result.failure(exception)
            }

            val engine =
                embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        gson {
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
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /status from $remote")
                            call.respondText("ForwardApp Mobile Server is running!")
                        }
                        get("/export") {
                            Log.d("WifiSyncServer", "Запит на /export отримано.")
                            val remote = call.request.local.remoteHost
                            Log.d(DEBUG_TAG, "[WifiSyncServer] /export from $remote")
                            try {
                                val deltaSinceParam =
                                    call.request.queryParameters["deltaSince"]
                                        ?: call.request.queryParameters["since"]
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] /export deltaSinceParam=$deltaSinceParam",
                                )
                                val systemKeyStats =
                                    runCatching {
                                        val projects =
                                            syncRepository.createFullBackupJsonString().let { json ->
                                                gson.fromJson(
                                                    json,
                                                    FullAppBackup::class.java,
                                                ).database?.projects ?: emptyList()
                                            }
                                        val missing = projects.count { !SystemContexts.isSystem(ContextId(it.id)) }
                                        val total = projects.size
                                        "systemKeys=${total - missing}/$total"
                                    }.getOrElse { "systemKeys=error:${it.message}" }
                                val rawBackupJson =
                                    if (deltaSinceParam != null) {
                                        val since = deltaSinceParam.toLongOrNull()
                                        if (since != null) {
                                            Log.d(
                                                DEBUG_TAG,
                                                "[WifiSyncServer] Serving DELTA since=$since $systemKeyStats",
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
                                            "[WifiSyncServer] No deltaSince param, serving FULL export $systemKeyStats",
                                        )
                                        syncRepository.createFullBackupJsonString()
                                    }

                                val backupJson =
                                    gson.toJson(
                                        gson.fromJson(rawBackupJson, FullAppBackup::class.java)
                                            .withLegacyRecurrenceSyncQuarantined(),
                                    )

                                // ========== DEFECT #2 DEBUG: Log attachments in export ==========
                                try {
                                    val backup = gson.fromJson(backupJson, FullAppBackup::class.java)
                                    val attachmentsCount = backup.database?.attachments?.size ?: 0
                                    val crossRefsCount =
                                        backup.database?.contextAttachmentCrossRefs?.size ?: 0
                                    Log.i(
                                        "ForwardSync",
                                        "wifi export bytes=${backupJson.length} dbPlans=${backup.database?.dayPlans?.size ?: 0} " +
                                            "dbFocus=${backup.database?.dayFocusItems?.size ?: 0} " +
                                            "dbTasks=${backup.database?.dayTasks?.size ?: 0} " +
                                            "snapshotPlans=${backup.snapshotBundle?.dayPlans?.size ?: 0} " +
                                            "snapshotFocus=${backup.snapshotBundle?.dayFocusItems?.size ?: 0} " +
                                            "snapshotTasks=${backup.snapshotBundle?.dayTasks?.size ?: 0} " +
                                            "snapshotRuntime=${backup.snapshotBundle?.dayManagementRuntimeState != null}",
                                    )
                                    Log.d(
                                        DEBUG_TAG,
                                        "[WifiSyncServer] /export CONTENT CHECK: attachments=$attachmentsCount, crossRefs=$crossRefsCount",
                                    )
                                    if (attachmentsCount == 0) {
                                        Log.w(
                                            DEBUG_TAG,
                                            "[WifiSyncServer] WARNING: Exporting 0 attachments to desktop!",
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        DEBUG_TAG,
                                        "[WifiSyncServer] Failed to analyze export content",
                                        e,
                                    )
                                }

                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] DEBUG_MARK_EXPORT_READY len=${backupJson.length}",
                                )
                                dumpToFileDebug("export", backupJson)
                                if (BuildConfig.DEBUG) {
                                    Log.d(
                                        DEBUG_TAG,
                                        "[WifiSyncServer] /export dump head=${backupJson.take(400)}",
                                    )
                                }
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] /export COMPLETE, JSON size=${backupJson.length} bytes",
                                )
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
                                Log.d(
                                    DEBUG_TAG,
                                    "[WifiSyncServer] DEBUG_MARK_RECEIVED_IMPORT len=${body.length}",
                                )
                                dumpToFileDebug("import", body)
                                if (BuildConfig.DEBUG) {
                                    Log.d(DEBUG_TAG, "[WifiSyncServer] /import dump head=${body.take(400)}")
                                }
                                val backup =
                                    gson.fromJson(body, FullAppBackup::class.java)
                                        .withLegacyRecurrenceSyncQuarantined()
                                Log.e(DAY_SYNC_IMPORT_TAG, "server received ${backup.describeDayImportPayload(body.length)}")
                                val snapshotBundle = backup.snapshotBundle
                                val db = backup.database
                                Log.i(
                                    "ForwardSync",
                                    "wifi import received bytes=${body.length} hasSnapshot=${snapshotBundle != null} " +
                                        "hasDb=${db != null} dbPlans=${db?.dayPlans?.size ?: 0} " +
                                        "dbFocus=${db?.dayFocusItems?.size ?: 0} dbTasks=${db?.dayTasks?.size ?: 0} " +
                                        "dbRuntime=${db?.dayManagementRuntimeState != null} " +
                                        "dbRuntimePhase=${db?.dayManagementRuntimeState?.currentPhase} " +
                                        "dbRuntimeSleepAt=${db?.dayManagementRuntimeState?.sleepAt} " +
                                        "snapshotPlans=${snapshotBundle?.dayPlans?.size ?: 0} " +
                                        "snapshotFocus=${snapshotBundle?.dayFocusItems?.size ?: 0} " +
                                        "snapshotTasks=${snapshotBundle?.dayTasks?.size ?: 0} " +
                                        "snapshotRuntime=${snapshotBundle?.dayManagementRuntimeState != null} " +
                                        "snapshotRuntimePhase=${snapshotBundle?.dayManagementRuntimeState?.currentPhase} " +
                                        "snapshotRuntimeSleepAt=${snapshotBundle?.dayManagementRuntimeState?.sleepAt}",
                                )
                                if (snapshotBundle == null && db == null) {
                                    return@post call.respond(
                                        HttpStatusCode.Companion.BadRequest,
                                        "Database or snapshotBundle section is missing",
                                    )
                                }

                                withContext(NonCancellable) {
                                    when {
                                        snapshotBundle != null -> syncRepository.applyServerChanges(snapshotBundle).getOrThrow()
                                        db != null -> syncRepository.applyServerChanges(db).getOrThrow()
                                    }
                                    Log.i("ForwardSync", "wifi import applied; legacy recurrence generation quarantined")
                                    backup.settings?.settings?.let { settings ->
                                        try {
                                            settingsRepository.restoreFromMap(settings)
                                        } catch (e: Exception) {
                                            Log.w("ForwardSync", "wifi import settings restore failed: ${e.message}", e)
                                            Log.e(DEBUG_TAG, "[WifiSyncServer] Failed to restore incoming settings", e)
                                        }
                                    }
                                }
                                call.respond(HttpStatusCode.Companion.OK, "Import applied")
                            } catch (e: Exception) {
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

    private fun FullAppBackup.withLegacyRecurrenceSyncQuarantined(): FullAppBackup =
        copy(
            database =
                database?.let { content ->
                    content.copy(
                        recurringTasks = emptyList(),
                        dayTasks =
                            content.dayTasks.filterNot { task ->
                                task.recurringTaskId != null ||
                                    task.nextOccurrenceTime != null ||
                                    task.id.startsWith("recurring-task-instance-") ||
                                    task.id.startsWith("recurrence:TASK:")
                            },
                    )
                },
            snapshotBundle =
                snapshotBundle?.let { bundle ->
                    bundle.copy(
                        recurringTasks = emptyList(),
                        dayTasks =
                            bundle.dayTasks.filterNot { task ->
                                task.recurringTaskId != null ||
                                    task.nextOccurrenceTime != null ||
                                    task.id.startsWith("recurring-task-instance-") ||
                                    (task.id.startsWith("recurrence:TASK:") && task.recurrence == null)
                            },
                    )
                },
        )

    private suspend fun ensureTodayRecurringTasksAfterImport() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        runCatching {
            if (dayManagementRepository.getPlanIdForDate(todayStart) == null) {
                dayManagementRepository.createOrUpdateDayPlan(todayStart)
            }
            dayManagementRepository.generateRecurringTasksForDate(todayStart)
        }.onSuccess {
            Log.i("ForwardSync", "post-import recurring generation triggered date=$todayStart")
        }.onFailure { error ->
            Log.e(DAY_SYNC_IMPORT_TAG, "post-import recurring generation failed date=$todayStart", error)
        }
    }

    private fun startOfLocalDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

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
            return String.Companion.format(
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

    private fun isPortFree(port: Int): Boolean {
        return runCatching {
            ServerSocket().use { socket ->
                socket.bind(InetSocketAddress("0.0.0.0", port))
                true
            }
        }.getOrElse { throwable ->
            Log.w(DEBUG_TAG, "[WifiSyncServer] Port $port busy: ${throwable.message}")
            false
        }
    }
}

private fun FullAppBackup.describeDayImportPayload(bodyLength: Int): String {
    val dbPlans = database?.dayPlans.orEmpty()
    val dbTasks = database?.dayTasks.orEmpty()
    val dbFocus = database?.dayFocusItems.orEmpty()
    val dbRecurring = database?.recurringTasks.orEmpty()
    val snapshotPlans = snapshotBundle?.dayPlans.orEmpty()
    val snapshotTasks = snapshotBundle?.dayTasks.orEmpty()
    val snapshotFocus = snapshotBundle?.dayFocusItems.orEmpty()
    val snapshotRecurring = snapshotBundle?.recurringTasks.orEmpty()
    val dbPlanSample = dbPlans
        .sortedByDescending { it.updatedAt ?: it.createdAt }
        .take(4)
        .joinToString { "${it.id}:${it.date}:v${it.version}:u${it.updatedAt}:s${it.syncedAt}" }
    val dbTaskSample = dbTasks
        .sortedByDescending { it.updatedAt ?: it.createdAt }
        .take(6)
        .joinToString { "${it.id}:${it.dayPlanId}:v${it.version}:u${it.updatedAt}:s${it.syncedAt}:${it.title.take(28)}" }
    val snapshotPlanSample = snapshotPlans
        .sortedByDescending { it.updatedAt }
        .take(4)
        .joinToString { "${it.id}:${it.date}:v${it.version}:u${it.updatedAt}" }
    val snapshotTaskSample = snapshotTasks
        .sortedByDescending { it.updatedAt }
        .take(6)
        .joinToString { "${it.id}:${it.dayPlanId}:v${it.version}:u${it.updatedAt}:${it.title.take(28)}" }

    return listOf(
        "bytes=$bodyLength",
        "dbPlans=${dbPlans.size}",
        "dbTasks=${dbTasks.size}",
        "dbFocus=${dbFocus.size}",
        "dbRecurring=${dbRecurring.size}",
        "snapshotPlans=${snapshotPlans.size}",
        "snapshotTasks=${snapshotTasks.size}",
        "snapshotFocus=${snapshotFocus.size}",
        "snapshotRecurring=${snapshotRecurring.size}",
        "dbPlanSample=$dbPlanSample",
        "dbTaskSample=$dbTaskSample",
        "snapshotPlanSample=$snapshotPlanSample",
        "snapshotTaskSample=$snapshotTaskSample",
    ).joinToString(" ")
}
