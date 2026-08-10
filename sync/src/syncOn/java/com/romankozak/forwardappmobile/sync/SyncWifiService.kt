package com.romankozak.forwardappmobile.sync

import android.util.Log
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.SyncLocalDataSource
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

@Singleton
class SyncWifiService @Inject constructor(
    private val localDataSource: SyncLocalDataSource,
    private val settingsSource: SyncSettingsSource,
    private val logicHelper: SyncLogicHelper,
    private val fullBackupLocalDataSource: FullBackupLocalDataSource,
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
            val unsynced = localDataSource.getUnsyncedChanges()
            if (isEmptyDatabaseContent(unsynced)) {
                Result.success(Unit)
            } else {
                val fullUrl = buildWifiUrl(address, "/import")
                val backupWrapper =
                    FullAppBackup(
                        database = unsynced,
                        settings = SettingsContent(fullBackupLocalDataSource.getSettingsSnapshot()),
                    )
                val response = client.post(fullUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(backupWrapper)
                }
                if (response.status.isSuccess()) {
                    localDataSource.markSyncedNow(unsynced)
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

    suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
        val changes = localDataSource.getChangesSince(deltaSince)
        val enrichedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = changes.attachments,
            existingCrossRefs = changes.contextAttachmentCrossRefs,
        )
        val deltaBackup = FullAppBackup(
            database = changes.copy(contextAttachmentCrossRefs = enrichedCrossRefs),
            settings = SettingsContent(fullBackupLocalDataSource.getSettingsSnapshot()),
        )
        // Використовуємо Gson для ручної серіалізації в рядок
        return com.google.gson.GsonBuilder().create().toJson(deltaBackup)
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
                content.backlogOrders.isEmpty() &&
                content.legacyNotes.isEmpty() &&
                content.attachments.isEmpty() &&
                content.documents.isEmpty() &&
                content.musicNotes.isEmpty() &&
                content.checklists.isEmpty() &&
                content.checklistItems.isEmpty() &&
                content.directionItems.isEmpty() &&
                content.inboxRecords.isEmpty() &&
                content.contextLogs.isEmpty() &&
                content.recentProjectEntries.isEmpty() &&
                content.scripts.isEmpty() &&
                content.contextAttachmentCrossRefs.isEmpty() &&
                content.dayPlans.isEmpty() &&
                content.dayFocusItems.isEmpty() &&
                content.dayTasks.isEmpty() &&
                content.dailyMetrics.isEmpty() &&
                content.conversations.isEmpty() &&
                content.chatMessages.isEmpty() &&
                content.conversationFolders.isEmpty() &&
                content.reminders.isEmpty() &&
                content.recurringTasks.isEmpty() &&
                content.systemApps.isEmpty() &&
                content.contextArtifacts.isEmpty() &&
                content.tacticalMissions.isEmpty() &&
                content.tacticalMissionAttachments.isEmpty() &&
                content.aiEvents.isEmpty() &&
                content.aiInsights.isEmpty() &&
                content.lifeSystemStates.isEmpty() &&
                content.contextRoleProfiles.isEmpty() &&
                content.contextRoleProfileItems.isEmpty() &&
                content.contextConfigurations.isEmpty() &&
                content.projectStructureItems.isEmpty() &&
                content.contextInboxSortingRules.isEmpty() &&
                content.contextKeyProblems.isEmpty() &&
                content.focusContextIntervals.isEmpty() &&
                content.userStateIntervals.isEmpty()
    }
}
