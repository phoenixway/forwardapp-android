package com.romankozak.forwardappmobile.sync

import android.util.Log
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalOrientationPayload
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncPayload
import com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncVersion
import com.romankozak.forwardappmobile.sync.datasource.CanonicalExecutionLogSyncVersion
import com.romankozak.forwardappmobile.sync.datasource.CanonicalRecurringSeriesSyncVersion
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySyncVersion
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
    val dayThemesAck: CanonicalDayThemeSyncAck,
    val orientationsAck: CanonicalOrientationSyncAck,
    val executionLogsAck: List<CanonicalExecutionLogSyncVersion>,
    val directionEntriesAck: List<WorkspaceDirectionEntrySyncVersion>,
)

internal fun CanonicalDayThemeSyncPayload.hasChanges(): Boolean =
    themeDefinitions.isNotEmpty() || dayThemes.isNotEmpty() || assignmentDocuments.isNotEmpty()

internal fun CanonicalOrientationSyncPayload.hasChanges(): Boolean =
    managedSubjects.isNotEmpty() ||
        orientations.isNotEmpty() ||
        aspects.isNotEmpty() ||
        assessments.isNotEmpty() ||
        assessmentRevisions.isNotEmpty() ||
        legacyMappings.isNotEmpty() ||
        relations.isNotEmpty() ||
        aspectRefs.isNotEmpty() ||
        workspaces.isNotEmpty() ||
        workspaceBindings.isNotEmpty() ||
        workspaceCapabilities.isNotEmpty() ||
        savedViews.isNotEmpty()

private fun CanonicalOrientationSyncPayload.toAck() =
    CanonicalOrientationSyncAck(
        managedSubjects = managedSubjects.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        assessments = assessments.map { CanonicalOrientationSyncVersion(it.orientationId, it.version) },
        assessmentRevisions = assessmentRevisions.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        legacyMappings = legacyMappings.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        relations = relations.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        aspectRefs = aspectRefs.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        workspaces = workspaces.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        workspaceBindings = workspaceBindings.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        workspaceCapabilities = workspaceCapabilities.map { CanonicalOrientationSyncVersion(it.id, it.version) },
        savedViews = savedViews.map { CanonicalOrientationSyncVersion(it.id, it.version) },
    )

internal fun shouldPushCanonicalWifi(
    databaseIsEmpty: Boolean,
    dirtyCanonicalSeries: List<CanonicalRecurringSeriesSnapshot>,
    dirtyCanonicalDayThemes: CanonicalDayThemeSyncPayload = CanonicalDayThemeSyncPayload(),
    dirtyCanonicalOrientations: CanonicalOrientationSyncPayload = CanonicalOrientationSyncPayload(),
    dirtyCanonicalExecutionLogs: List<CanonicalExecutionLogSnapshot> = emptyList(),
    dirtyCanonicalDirectionEntries: List<WorkspaceDirectionEntrySnapshot> = emptyList(),
): Boolean =
    !databaseIsEmpty ||
        dirtyCanonicalSeries.isNotEmpty() ||
        dirtyCanonicalDayThemes.hasChanges() ||
        dirtyCanonicalOrientations.hasChanges() ||
        dirtyCanonicalExecutionLogs.isNotEmpty() ||
        dirtyCanonicalDirectionEntries.isNotEmpty()

internal fun buildCanonicalWifiPushPlan(
    selection: LocalSyncSelection,
    fullSnapshot: SnapshotBundle,
    dirtyCanonicalSeries: List<CanonicalRecurringSeriesSnapshot>,
    dirtyCanonicalDayThemes: CanonicalDayThemeSyncPayload = CanonicalDayThemeSyncPayload(),
    dirtyCanonicalOrientations: CanonicalOrientationSyncPayload = CanonicalOrientationSyncPayload(),
    dirtyCanonicalExecutionLogs: List<CanonicalExecutionLogSnapshot> = emptyList(),
    dirtyCanonicalDirectionEntries: List<WorkspaceDirectionEntrySnapshot> = emptyList(),
): CanonicalWifiPushPlan =
    CanonicalWifiPushPlan(
        snapshotDelta =
            buildCanonicalSnapshotDelta(
                baseDelta = buildSnapshotSelectionDelta(selection, fullSnapshot),
                fullSnapshot = fullSnapshot,
                explicitCanonicalSeriesIds =
                    dirtyCanonicalSeries.mapTo(hashSetOf()) { it.id },
                explicitCanonicalDayThemes = dirtyCanonicalDayThemes,
                explicitCanonicalOrientations = dirtyCanonicalOrientations,
                explicitCanonicalExecutionLogs = dirtyCanonicalExecutionLogs,
                explicitCanonicalDirectionEntries = dirtyCanonicalDirectionEntries,
            ),
        recurringSeriesAck =
            dirtyCanonicalSeries.map { series ->
                CanonicalRecurringSeriesSyncVersion(
                    id = series.id,
                    version = series.version,
                )
            },
        dayThemesAck =
            CanonicalDayThemeSyncAck(
                themeDefinitions =
                    dirtyCanonicalDayThemes.themeDefinitions.map { item ->
                        CanonicalDayThemeSyncVersion(item.id, item.version)
                    },
                dayThemes =
                    dirtyCanonicalDayThemes.dayThemes.map { item ->
                        CanonicalDayThemeSyncVersion(item.id, item.version)
                    },
                assignmentDocuments =
                    dirtyCanonicalDayThemes.assignmentDocuments.map { item ->
                        CanonicalDayThemeSyncVersion(item.dayPlanId, item.version)
                    },
            ),
        orientationsAck = dirtyCanonicalOrientations.toAck(),
        executionLogsAck =
            dirtyCanonicalExecutionLogs.map { log ->
                CanonicalExecutionLogSyncVersion(log.id, log.version)
            },
        directionEntriesAck =
            dirtyCanonicalDirectionEntries.map { entry ->
                WorkspaceDirectionEntrySyncVersion(entry.id, entry.version)
            },
    )


private fun buildSnapshotSelectionDelta(
    selection: LocalSyncSelection,
    full: SnapshotBundle,
): SnapshotBundle {
    fun ids(items: List<com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncVersion>) =
        items.mapTo(hashSetOf()) { it.id }

    val contexts = ids(selection.contexts)
    val goals = ids(selection.goals)
    val backlogItems = ids(selection.backlogItems)
    val backlogOrders = ids(selection.backlogOrders)
    val notes = ids(selection.notes)
    val documents = ids(selection.documents)
    val musicNotes = ids(selection.musicNotes)
    val checklists = ids(selection.checklists)
    val checklistItems = ids(selection.checklistItems)
    val activity = ids(selection.activityRecords)
    val links = ids(selection.linkItemEntities)
    val directions = ids(selection.directionItems)
    val inbox = ids(selection.inbox)
    val logs = ids(selection.logs)
    val scripts = ids(selection.scripts)
    val attachments = ids(selection.attachments)
    val plans = ids(selection.dayPlans)
    val focus = ids(selection.dayFocusItems)
    val tasks = ids(selection.dayTasks)
    val missions = ids(selection.tacticalMissions)
    val iterations = ids(selection.tacticalIterations)
    val streams = ids(selection.missionStreams)
    val slots = ids(selection.tacticalActivitySlots)
    val quests = ids(selection.arcQuests)
    val crossRefs = selection.crossRefs
        .mapTo(hashSetOf()) { "${it.contextId}\u0000${it.attachmentId}" }

    return full.copy(
        contexts = full.contexts.filter { it.id in contexts },
        goals = full.goals.filter { it.id in goals },
        backlogItems = full.backlogItems.filter { it.id in backlogItems },
        backlogOrders = full.backlogOrders.filter { it.id in backlogOrders },
        directionItems = full.directionItems.filter { it.id in directions },
        notes = full.notes.filter { it.id in notes },
        documents = full.documents.filter { it.id in documents },
        musicNotes = full.musicNotes.filter { it.id in musicNotes },
        checklists = full.checklists.filter { it.id in checklists },
        checklistItems = full.checklistItems.filter { it.id in checklistItems },
        activityRecords = full.activityRecords.filter { it.id in activity },
        linkItemEntities = full.linkItemEntities.filter { it.id in links },
        inbox = full.inbox.filter { it.id in inbox },
        logs = full.logs.filter { it.id in logs },
        scripts = full.scripts.filter { it.id in scripts },
        attachments = full.attachments.filter { it.id in attachments },
        crossRefs = full.crossRefs.filter {
            "${it.contextId}\u0000${it.attachmentId}" in crossRefs
        },
        dayPlans = full.dayPlans.filter { it.id in plans },
        dayFocusItems = full.dayFocusItems.filter { it.id in focus },
        dayTasks = full.dayTasks.filter { it.id in tasks },
        tacticalMissions = full.tacticalMissions.filter { it.id.toString() in missions },
        tacticalIterations = full.tacticalIterations.filter { it.id in iterations },
        missionStreams = full.missionStreams.filter { it.id in streams },
        tacticalActivitySlots = full.tacticalActivitySlots.filter { it.id in slots },
        arcQuests = full.arcQuests.filter { it.id in quests },

        // Canonical streams are injected below by buildCanonicalSnapshotDelta().
        canonicalExecutionLogs = null,
        workspaceDirectionEntries = null,
        themeDefinitions = null,
        dayThemes = null,
        dayThemeAssignmentDocuments = null,
        managedSubjects = null,
        orientations = null,
        aspects = null,
        orientationAssessments = null,
        orientationAssessmentRevisions = null,
        legacySubjectMappings = null,
        orientationRelations = null,
        aspectOrientationRefs = null,
        workspaces = null,
        workspaceBindings = null,
        workspaceCapabilityInstances = null,
        savedOrientationViews = null,
        recurringSeries = emptyList(),
        recurringTasks = emptyList(),
        dayThemeDocuments = emptyList(),

        // The legacy local delta did not represent this collection.
        lifeManagementLevelStatuses = emptyList(),
    )
}

internal fun buildCanonicalSnapshotDelta(
    baseDelta: SnapshotBundle,
    fullSnapshot: SnapshotBundle,
    explicitCanonicalSeriesIds: Set<String> = emptySet(),
    explicitCanonicalDayThemes: CanonicalDayThemeSyncPayload = CanonicalDayThemeSyncPayload(),
    explicitCanonicalOrientations: CanonicalOrientationSyncPayload = CanonicalOrientationSyncPayload(),
    explicitCanonicalExecutionLogs: List<CanonicalExecutionLogSnapshot> = emptyList(),
    explicitCanonicalDirectionEntries: List<WorkspaceDirectionEntrySnapshot> = emptyList(),
): SnapshotBundle {
    val dayPlanIds = baseDelta.dayPlans.mapTo(hashSetOf()) { it.id }
    val dayFocusItemIds = baseDelta.dayFocusItems.mapTo(hashSetOf()) { it.id }
    val dayTaskIds = baseDelta.dayTasks.mapTo(hashSetOf()) { it.id }
    val requiredCanonicalSeriesIds = explicitCanonicalSeriesIds.toMutableSet()

    baseDelta.dayTasks.mapNotNullTo(requiredCanonicalSeriesIds) { it.recurrence?.seriesId }
    baseDelta.dayFocusItems.mapNotNullTo(requiredCanonicalSeriesIds) { it.recurrence?.seriesId }

    val includeCanonicalDayThemes = explicitCanonicalDayThemes.hasChanges()
    val includeCanonicalExecutionLogs = explicitCanonicalExecutionLogs.isNotEmpty()
    val includeCanonicalDirectionEntries = explicitCanonicalDirectionEntries.isNotEmpty()
    val includeCanonicalOrientations =
        explicitCanonicalOrientations.hasChanges() ||
            includeCanonicalExecutionLogs ||
            includeCanonicalDirectionEntries

    fun <T> canonicalOrientationDependency(
        full: List<T>?,
        dirty: List<T>,
        fieldName: String,
    ): List<T>? =
        when {
            !includeCanonicalOrientations -> null
            includeCanonicalExecutionLogs || includeCanonicalDirectionEntries ->
                requireNotNull(full) {
                    "Local full snapshot must expose $fieldName before building a canonical capability-content delta."
                }
            else -> dirty
        }

    val fullThemeDefinitions =
        if (includeCanonicalDayThemes) {
            requireNotNull(fullSnapshot.themeDefinitions) {
                "Local full snapshot must expose canonical ThemeDefinitions before building a Day Theme delta."
            }
        } else {
            emptyList()
        }
    val fullDayThemes =
        if (includeCanonicalDayThemes) {
            requireNotNull(fullSnapshot.dayThemes) {
                "Local full snapshot must expose canonical DayThemes before building a Day Theme delta."
            }
        } else {
            emptyList()
        }
    val fullAssignmentDocuments =
        if (includeCanonicalDayThemes) {
            requireNotNull(fullSnapshot.dayThemeAssignmentDocuments) {
                "Local full snapshot must expose canonical Day Theme assignments before building a Day Theme delta."
            }
        } else {
            emptyList()
        }

    val dirtyAssignmentDayPlanIds =
        explicitCanonicalDayThemes.assignmentDocuments.mapTo(hashSetOf()) { it.dayPlanId }
    val selectedAssignmentDocuments =
        fullAssignmentDocuments.filter { it.dayPlanId in dirtyAssignmentDayPlanIds }

    val requiredDayThemeIds =
        explicitCanonicalDayThemes.dayThemes.mapTo(hashSetOf()) { it.id }
    selectedAssignmentDocuments.forEach { document ->
        document.assignments.forEach { assignment ->
            requiredDayThemeIds.addAll(assignment.dayThemeIds)
        }
    }

    val selectedDayThemes =
        fullDayThemes.filter { it.id in requiredDayThemeIds }

    val requiredDefinitionIds =
        explicitCanonicalDayThemes.themeDefinitions.mapTo(hashSetOf()) { it.id }
    selectedDayThemes.mapTo(requiredDefinitionIds) { it.themeId }

    val selectedThemeDefinitions =
        fullThemeDefinitions.filter { it.id in requiredDefinitionIds }

    selectedDayThemes.mapTo(dayPlanIds) { it.dayPlanId }
    selectedAssignmentDocuments.mapTo(dayPlanIds) { it.dayPlanId }

    val result =
        baseDelta.copy(
            dayPlans = fullSnapshot.dayPlans.filter { it.id in dayPlanIds },
            dayFocusItems = fullSnapshot.dayFocusItems.filter { it.id in dayFocusItemIds },
            dayTasks = fullSnapshot.dayTasks.filter { it.id in dayTaskIds },
            dayThemeDocuments = emptyList(),
            themeDefinitions = if (includeCanonicalDayThemes) selectedThemeDefinitions else null,
            dayThemes = if (includeCanonicalDayThemes) selectedDayThemes else null,
            dayThemeAssignmentDocuments =
                if (includeCanonicalDayThemes) selectedAssignmentDocuments else null,
            recurringTasks = emptyList(),
            recurringSeries =
                fullSnapshot.recurringSeries.filter { series ->
                    series.id in requiredCanonicalSeriesIds
                },
            dayManagementRuntimeState = fullSnapshot.dayManagementRuntimeState,
            managedSubjects =
                canonicalOrientationDependency(
                    fullSnapshot.managedSubjects,
                    explicitCanonicalOrientations.managedSubjects,
                    "managedSubjects",
                ),
            orientations =
                canonicalOrientationDependency(
                    fullSnapshot.orientations,
                    explicitCanonicalOrientations.orientations,
                    "orientations",
                ),
            aspects =
                canonicalOrientationDependency(
                    fullSnapshot.aspects,
                    explicitCanonicalOrientations.aspects,
                    "aspects",
                ),
            orientationAssessments =
                canonicalOrientationDependency(
                    fullSnapshot.orientationAssessments,
                    explicitCanonicalOrientations.assessments,
                    "orientationAssessments",
                ),
            orientationAssessmentRevisions =
                canonicalOrientationDependency(
                    fullSnapshot.orientationAssessmentRevisions,
                    explicitCanonicalOrientations.assessmentRevisions,
                    "orientationAssessmentRevisions",
                ),
            legacySubjectMappings =
                canonicalOrientationDependency(
                    fullSnapshot.legacySubjectMappings,
                    explicitCanonicalOrientations.legacyMappings,
                    "legacySubjectMappings",
                ),
            orientationRelations =
                canonicalOrientationDependency(
                    fullSnapshot.orientationRelations,
                    explicitCanonicalOrientations.relations,
                    "orientationRelations",
                ),
            aspectOrientationRefs =
                canonicalOrientationDependency(
                    fullSnapshot.aspectOrientationRefs,
                    explicitCanonicalOrientations.aspectRefs,
                    "aspectOrientationRefs",
                ),
            workspaces =
                canonicalOrientationDependency(
                    fullSnapshot.workspaces,
                    explicitCanonicalOrientations.workspaces,
                    "workspaces",
                ),
            workspaceBindings =
                canonicalOrientationDependency(
                    fullSnapshot.workspaceBindings,
                    explicitCanonicalOrientations.workspaceBindings,
                    "workspaceBindings",
                ),
            workspaceCapabilityInstances =
                canonicalOrientationDependency(
                    fullSnapshot.workspaceCapabilityInstances,
                    explicitCanonicalOrientations.workspaceCapabilities,
                    "workspaceCapabilityInstances",
                ),
            savedOrientationViews =
                canonicalOrientationDependency(
                    fullSnapshot.savedOrientationViews,
                    explicitCanonicalOrientations.savedViews,
                    "savedOrientationViews",
                ),
            canonicalExecutionLogs =
                explicitCanonicalExecutionLogs.takeIf { includeCanonicalExecutionLogs },
            workspaceDirectionEntries =
                explicitCanonicalDirectionEntries.takeIf { includeCanonicalDirectionEntries },
        )

    if (includeCanonicalDayThemes) {
        requireValidCanonicalDayThemePayload(result)
    }
    if (includeCanonicalOrientations) {
        requireValidCanonicalOrientationPayload(result)
    }

    return result
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
            val selection = localDataSource.getUnsyncedSelection()
            val dirtyCanonicalSeries = fullBackupLocalDataSource.loadUnsyncedCanonicalRecurringSeries()
            val dirtyCanonicalDayThemes = fullBackupLocalDataSource.loadUnsyncedCanonicalDayThemes()
            val dirtyCanonicalOrientations = fullBackupLocalDataSource.loadUnsyncedCanonicalOrientations()
            val dirtyCanonicalExecutionLogs =
                fullBackupLocalDataSource.loadUnsyncedCanonicalExecutionLogs()
            val dirtyCanonicalDirectionEntries =
                fullBackupLocalDataSource.loadUnsyncedCanonicalWorkspaceDirectionEntries()
            val databaseIsEmpty = selection.isEmpty()

            if (
                !shouldPushCanonicalWifi(
                    databaseIsEmpty = databaseIsEmpty,
                    dirtyCanonicalSeries = dirtyCanonicalSeries,
                    dirtyCanonicalDayThemes = dirtyCanonicalDayThemes,
                    dirtyCanonicalOrientations = dirtyCanonicalOrientations,
                    dirtyCanonicalExecutionLogs = dirtyCanonicalExecutionLogs,
                    dirtyCanonicalDirectionEntries = dirtyCanonicalDirectionEntries,
                )
            ) {
                Result.success(Unit)
            } else {
                val fullSnapshot = fullBackupLocalDataSource.loadFullSnapshotBundle()
                val pushPlan =
                    buildCanonicalWifiPushPlan(
                        selection = selection,
                        fullSnapshot = fullSnapshot,
                        dirtyCanonicalSeries = dirtyCanonicalSeries,
                        dirtyCanonicalDayThemes = dirtyCanonicalDayThemes,
                        dirtyCanonicalOrientations = dirtyCanonicalOrientations,
                        dirtyCanonicalExecutionLogs = dirtyCanonicalExecutionLogs,
                        dirtyCanonicalDirectionEntries = dirtyCanonicalDirectionEntries,
                    )
                val fullUrl = buildWifiUrl(address, "/import")
                val backupWrapper =
                    FullAppBackup(
                        backupSchemaVersion = 2,
                        settings = SettingsContent(fullBackupLocalDataSource.getSettingsSnapshot()),
                        snapshotBundle = pushPlan.snapshotDelta,
                    )
                val response = client.post(fullUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(backupWrapper)
                }
                if (response.status.isSuccess()) {
                    localDataSource.acknowledge(selection)
                    fullBackupLocalDataSource.markCanonicalRecurringSeriesSynced(
                        pushPlan.recurringSeriesAck,
                    )
                    fullBackupLocalDataSource.markCanonicalDayThemesSynced(
                        pushPlan.dayThemesAck,
                    )
                    fullBackupLocalDataSource.markCanonicalOrientationsSynced(
                        pushPlan.orientationsAck,
                    )
                    fullBackupLocalDataSource.markCanonicalExecutionLogsSynced(
                        pushPlan.executionLogsAck,
                    )
                    fullBackupLocalDataSource.markCanonicalWorkspaceDirectionEntriesSynced(
                        pushPlan.directionEntriesAck,
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

    private suspend fun buildWifiUrl(address: String, path: String): String {
        val cleanAddress =
            address.trim().let {
                if (it.startsWith("http")) it else "http://$it"
            }
        val uri = cleanAddress.toUri()
        val port =
            if (uri.port != -1) {
                uri.port
            } else {
                settingsSource.wifiSyncPortFlow.first()
            }
        return "http://${uri.host}:$port$path"
    }

    suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
        val changes = localDataSource.getChangesSince(deltaSince)
        val fullSnapshot = fullBackupLocalDataSource.loadFullSnapshotBundle()
        val changedCanonicalSeries =
            fullBackupLocalDataSource.loadCanonicalRecurringSeriesChangedSince(deltaSince)
        val changedCanonicalDayThemes =
            fullBackupLocalDataSource.loadCanonicalDayThemesChangedSince(deltaSince)
        val changedCanonicalExecutionLogs =
            fullBackupLocalDataSource.loadCanonicalExecutionLogsChangedSince(deltaSince)
        val changedCanonicalDirectionEntries =
            fullBackupLocalDataSource.loadCanonicalWorkspaceDirectionEntriesChangedSince(deltaSince)
        val snapshotDelta =
            buildCanonicalSnapshotDelta(
                baseDelta = changes,
                fullSnapshot = fullSnapshot,
                explicitCanonicalSeriesIds =
                    changedCanonicalSeries.mapTo(hashSetOf()) { it.id },
                explicitCanonicalDayThemes = changedCanonicalDayThemes,
                explicitCanonicalExecutionLogs = changedCanonicalExecutionLogs,
                explicitCanonicalDirectionEntries = changedCanonicalDirectionEntries,
            )
        val deltaBackup = FullAppBackup(
            backupSchemaVersion = 2,
            settings = SettingsContent(fullBackupLocalDataSource.getSettingsSnapshot()),
            snapshotBundle = snapshotDelta,
        )
        // Використовуємо Gson для ручної серіалізації в рядок
        return com.google.gson.GsonBuilder().create().toJson(deltaBackup)
    }

}
