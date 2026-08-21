package com.romankozak.forwardappmobile.sync

import android.util.Log
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.sync.datasource.CanonicalRecurringSeriesSyncVersion
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

internal data class CanonicalWifiPushPlan(
    val snapshotDelta: SnapshotBundle,
    val recurringSeriesAck: List<CanonicalRecurringSeriesSyncVersion>,
)

internal fun shouldPushCanonicalWifi(
    databaseIsEmpty: Boolean,
    dirtyCanonicalSeries: List<CanonicalRecurringSeriesSnapshot>,
): Boolean = !databaseIsEmpty || dirtyCanonicalSeries.isNotEmpty()

internal fun buildCanonicalWifiPushPlan(
    source: DatabaseContent,
    fullSnapshot: SnapshotBundle,
    dirtyCanonicalSeries: List<CanonicalRecurringSeriesSnapshot>,
): CanonicalWifiPushPlan =
    CanonicalWifiPushPlan(
        snapshotDelta =
            buildCanonicalSnapshotDelta(
                source = source,
                fullSnapshot = fullSnapshot,
                explicitCanonicalSeriesIds =
                    dirtyCanonicalSeries.mapTo(hashSetOf()) { it.id },
            ),
        recurringSeriesAck =
            dirtyCanonicalSeries.map { series ->
                CanonicalRecurringSeriesSyncVersion(
                    id = series.id,
                    version = series.version,
                )
            },
    )

internal fun buildCanonicalSnapshotDelta(
    source: DatabaseContent,
    fullSnapshot: SnapshotBundle,
    explicitCanonicalSeriesIds: Set<String> = emptySet(),
): SnapshotBundle {
    val dayPlanIds = source.dayPlans.mapTo(hashSetOf()) { it.id }
    val dayFocusItemIds = source.dayFocusItems.mapTo(hashSetOf()) { it.id }
    val dayTaskIds = source.dayTasks.mapTo(hashSetOf()) { it.id }
    val requiredCanonicalSeriesIds = explicitCanonicalSeriesIds.toMutableSet()

    source.dayTasks.mapNotNullTo(requiredCanonicalSeriesIds) { it.recurrenceSeriesId }
    source.dayFocusItems.mapNotNullTo(requiredCanonicalSeriesIds) { it.recurrenceSeriesId }

    return SyncMapper.migrateV1ToV2(source).copy(
        dayPlans = fullSnapshot.dayPlans.filter { it.id in dayPlanIds },
        dayFocusItems = fullSnapshot.dayFocusItems.filter { it.id in dayFocusItemIds },
        dayTasks = fullSnapshot.dayTasks.filter { it.id in dayTaskIds },
        recurringTasks = emptyList(),
        recurringSeries =
            fullSnapshot.recurringSeries.filter { series ->
                series.id in requiredCanonicalSeriesIds
            },
        dayManagementRuntimeState = fullSnapshot.dayManagementRuntimeState,
    )
}

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
            val dirtyCanonicalSeries = fullBackupLocalDataSource.loadUnsyncedCanonicalRecurringSeries()
            val databaseIsEmpty = isEmptyDatabaseContent(unsynced)

            if (!shouldPushCanonicalWifi(databaseIsEmpty, dirtyCanonicalSeries)) {
                Result.success(Unit)
            } else {
                val fullSnapshot = fullBackupLocalDataSource.loadFullSnapshotBundle()
                val pushPlan =
                    buildCanonicalWifiPushPlan(
                        source = unsynced,
                        fullSnapshot = fullSnapshot,
                        dirtyCanonicalSeries = dirtyCanonicalSeries,
                    )
                val fullUrl = buildWifiUrl(address, "/import")
                val backupWrapper =
                    FullAppBackup(
                        backupSchemaVersion = 2,
                        database = unsynced,
                        settings = SettingsContent(fullBackupLocalDataSource.getSettingsSnapshot()),
                        snapshotBundle = pushPlan.snapshotDelta,
                    )
                val response = client.post(fullUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(backupWrapper)
                }
                if (response.status.isSuccess()) {
                    localDataSource.markSyncedNow(unsynced)
                    fullBackupLocalDataSource.markCanonicalRecurringSeriesSynced(
                        pushPlan.recurringSeriesAck,
                    )
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
        val enrichedChanges = changes.copy(contextAttachmentCrossRefs = enrichedCrossRefs)
        val fullSnapshot = fullBackupLocalDataSource.loadFullSnapshotBundle()
        val changedCanonicalSeries =
            fullBackupLocalDataSource.loadCanonicalRecurringSeriesChangedSince(deltaSince)
        val snapshotDelta =
            canonicalSnapshotDelta(
                source = enrichedChanges,
                fullSnapshot = fullSnapshot,
                explicitCanonicalSeriesIds =
                    changedCanonicalSeries.mapTo(hashSetOf()) { it.id },
            )
        val deltaBackup = FullAppBackup(
            backupSchemaVersion = 2,
            database = enrichedChanges,
            settings = SettingsContent(fullBackupLocalDataSource.getSettingsSnapshot()),
            snapshotBundle = snapshotDelta,
        )
        // Використовуємо Gson для ручної серіалізації в рядок
        return com.google.gson.GsonBuilder().create().toJson(deltaBackup)
    }

    private fun canonicalSnapshotDelta(
        source: DatabaseContent,
        fullSnapshot: SnapshotBundle,
        explicitCanonicalSeriesIds: Set<String> = emptySet(),
    ): SnapshotBundle =
        buildCanonicalSnapshotDelta(
            source = source,
            fullSnapshot = fullSnapshot,
            explicitCanonicalSeriesIds = explicitCanonicalSeriesIds,
        )

    private suspend fun buildWifiUrl(address: String, path: String): String {
        val cleanAddress = address.trim().let { if (it.startsWith("http")) it else "http://$it" }
        val uri = cleanAddress.toUri()
        val port = if (uri.port != -1) uri.port else settingsSource.wifiSyncPortFlow.first()
        return "http://${uri.host}:$port$path"
    }

    private fun isEmptyDatabaseContent(content: DatabaseContent): Boolean {
        return content.projects.isEmpty() &&
                content.contextParentLinks.isEmpty() &&
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
                content.dayThemeDocuments.isEmpty() &&
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
                content.tacticalIterations.isEmpty() &&
                content.missionStreams.isEmpty() &&
                content.tacticalActivitySlots.isEmpty() &&
                content.arcQuests.isEmpty() &&
                content.aiEvents.isEmpty() &&
                content.aiInsights.isEmpty() &&
                content.mainBeacons.isEmpty() &&
                content.mainBeaconGroups.isEmpty() &&
                content.mainBeaconGroupMembers.isEmpty() &&
                content.mainBeaconParentLinks.isEmpty() &&
                content.mainBeaconContextCrossRefs.isEmpty() &&
                content.mainBeaconAttachmentCrossRefs.isEmpty() &&
                content.mainBeaconLevelStatuses.isEmpty() &&
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
