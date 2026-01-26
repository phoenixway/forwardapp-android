package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.LongDeserializer
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWifiService @Inject constructor(
    private val syncLocalService: SyncLocalService,
    private val settingsRepository: SettingsRepository,
    private val logicHelper: SyncLogicHelper,
) {
    private val TAG = "SyncWifiService"
    private val WIFI_SYNC_TAG = "WIFI_SYNC"
    private val WIFI_LOG = "FWD_SYNC_WIFI"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { gson() }
        }
    }

    /**
     * Fetches backup data from Wi-Fi server
     * @param address Server address (can be with or without http://)
     * @param deltaSince Optional timestamp to fetch only changes since that time
     * @return Result with JSON string or error
     */
    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> = try {
        Log.d(WIFI_LOG, "Fetching from $address, deltaSince=$deltaSince")
        val fullUrl = buildWifiUrl(address, "/export").let { base ->
            if (deltaSince != null) "$base?deltaSince=$deltaSince" else base
        }
        val response: String = client.get(fullUrl).body()
        Log.d(WIFI_LOG, "Successfully fetched ${response.length} bytes")
        Result.success(response)
    } catch (e: Exception) {
        Log.e(WIFI_LOG, "Error fetching from Wi‑Fi", e)
        Result.failure(e)
    }

    /**
     * Pushes unsynced local changes to Wi-Fi server
     * @param address Server address
     * @return Result with Unit on success or error
     */
    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = try {
        Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Початок процесу. Адреса: $address")

        val unsynced = syncLocalService.getUnsyncedChanges()

        Log.d(
            WIFI_SYNC_TAG,
            "[pushUnsyncedToWifi] Знайдено змін: projects=${unsynced.projects.size}, " +
                    "goals=${unsynced.goals.size}, listItems=${unsynced.backlogItems.size}, " +
                    "attachments=${unsynced.attachments.size}",
        )

        if (isEmptyDatabaseContent(unsynced)) {
            Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Несинхронізованих даних не знайдено. Пропускаємо.")
            Result.success(Unit)
        } else {
            val fullUrl = buildWifiUrl(address, "/import")
            val backupWrapper = FullAppBackup(database = unsynced)
            val payload = gson.toJson(backupWrapper)

            Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] POST запит на $fullUrl. Розмір payload: ${payload.length} байт")

            val response = client.post(fullUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }

            if (response.status.isSuccess()) {
                Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Дані успішно прийнято сервером. Оновлюємо локальний статус.")
                syncLocalService.markSyncedNow(unsynced)
                Result.success(Unit)
            } else {
                val errorMsg = "Сервер повернув помилку: ${response.status.value}"
                Log.e(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        }
    } catch (e: Exception) {
        Log.e(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Критична помилка під час синхронізації", e)
        Result.failure(e)
    }

    /**
     * Creates delta backup JSON string with changes since specified timestamp
     * @param deltaSince Timestamp to get changes since
     * @return JSON string with delta backup
     */
    suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
        val changes = syncLocalService.getChangesSince(deltaSince)

        val enrichedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = changes.attachments,
            existingCrossRefs = changes.contextAttachmentCrossRefs,
        )

        val deltaBackup = FullAppBackup(database = changes.copy(contextAttachmentCrossRefs = enrichedCrossRefs))
        return gson.toJson(deltaBackup)
    }

    /**
     * Builds full Wi-Fi URL with proper port handling
     * @param address Server address (can be with or without http://)
     * @param path API path (e.g., "/export", "/import")
     * @return Formatted URL string
     */
    private suspend fun buildWifiUrl(address: String, path: String): String {
        val cleanAddress = address.trim().let { if (it.startsWith("http")) it else "http://$it" }
        val uri = cleanAddress.toUri()
        val port = if (uri.port != -1) uri.port else settingsRepository.wifiSyncPortFlow.first()
        return "http://${uri.host}:$port$path"
    }

    /**
     * Checks if DatabaseContent is empty (has no changes)
     */
    private fun isEmptyDatabaseContent(content: DatabaseContent): Boolean {
        return content.projects.isEmpty() &&
                content.goals.isEmpty() &&
                content.backlogItems.isEmpty() &&
                content.attachments.isEmpty() &&
                content.documents.isEmpty()
    }
}