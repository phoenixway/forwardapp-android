// sync/src/main/java/com/romankozak/forwardappmobile/sync/SyncWifiService.kt
package com.romankozak.forwardappmobile.sync

import android.util.Log
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.sync.datasource.SyncSettingsSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

@Singleton
class SyncWifiService @Inject constructor(
    private val syncLocalService: SyncLocalService,
    private val settingsSource: SyncSettingsSource,
    private val logicHelper: SyncLogicHelper, // Використовується в createDeltaBackupJsonString
) {
    private val WIFI_LOG = "FWD_SYNC_WIFI"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {
                registerTypeAdapter(Long::class.java, LongDeserializer())
            }
        }
    }

    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = buildWifiUrl(address, "/export").let { base ->
                if (deltaSince != null) "$base?deltaSince=$deltaSince" else base
            }
            val response: String = client.get(fullUrl).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(WIFI_LOG, "Error fetching from Wi‑Fi", e)
            Result.failure(e)
        }
    }

    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val unsynced = syncLocalService.getUnsyncedChanges()

            if (isEmptyDatabaseContent(unsynced)) {
                Result.success(Unit)
            } else {
                val fullUrl = buildWifiUrl(address, "/import")
                val backupWrapper = FullAppBackup(database = unsynced)

                val response = client.post(fullUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(backupWrapper)
                }

                if (response.status.isSuccess()) {
                    syncLocalService.markSyncedNow(unsynced)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Сервер повернув помилку: ${response.status.value}"))
                }
            }
        } catch (e: Exception) {
            Log.e(WIFI_LOG, "Push error", e)
            Result.failure(e)
        }
    }

    /**
     * Створює JSON-рядок дельта-бекапу (тільки зміни з певного часу)
     */
    suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
        val changes = syncLocalService.getChangesSince(deltaSince)

        val enrichedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = changes.attachments,
            existingCrossRefs = changes.contextAttachmentCrossRefs,
        )

        val deltaBackup = FullAppBackup(
            database = changes.copy(contextAttachmentCrossRefs = enrichedCrossRefs)
        )
        // Використовуємо внутрішній серіалізатор Ktor або окремий Gson за потреби
        return io.ktor.serialization.gson.GsonConverter().let {
            // У вашому проекті краще мати один Gson instance для всіх сервісів
            com.google.gson.GsonBuilder().create().toJson(deltaBackup)
        }
    }

    private suspend fun buildWifiUrl(address: String, path: String): String {
        val cleanAddress = address.trim().let { if (it.startsWith("http")) it else "http://$it" }
        val uri = cleanAddress.toUri()
        val port = if (uri.port != -1) uri.port else settingsSource.wifiSyncPortFlow.first()
        return "http://${uri.host}:$port$path"
    }

    private fun isEmptyDatabaseContent(content: DatabaseContent): Boolean {
        return content.projects.isEmpty() &&
                content.goals.isEmpty() &&
                content.backlogItems.isEmpty() &&
                content.attachments.isEmpty() &&
                content.documents.isEmpty()
    }
}