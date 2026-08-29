package com.romankozak.forwardappmobile.sync

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportDescriptor
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportSourceMode
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.shared.domain.contexts.WorkspaceSnapshotResolver
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

data class ResolvedImportBundle(
    val snapshotBundle: SnapshotBundle,
    val descriptor: WorkspaceImportDescriptor,
)

@Singleton
class SyncFileService @Inject constructor(
    private val contentProvider: IContentProvider,
    private val localDataSource: FullBackupLocalDataSource,
    private val mergeRepository: MergeRepository,
) {
    private val tag = "SyncFileService"
    private val workspaceSnapshotResolver = WorkspaceSnapshotResolver(Json { ignoreUnknownKeys = true })
    private val desktopWorkspaceSnapshotSyncAdapter = DesktopWorkspaceSnapshotSyncAdapter()

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    // === Legacy Methods (V1) ===

    suspend fun exportFullBackupToFile(): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to export full backup to file.")
        try {
            val json = createFullBackupJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_full_backup_$ts.json"

            contentProvider.saveFile(name, json).fold(
                onSuccess = {
                    Timber.tag(tag).i("Full backup successfully exported to file: $name")
                    Result.success("Файл бекапу успішно збережено")
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Timber.tag(tag).e( "Error exporting full backup", e)
            Result.failure(e)
        }
    }

    suspend fun createFullBackupJsonString(): String {
                val snapshotBundle = localDataSource.loadFullSnapshotBundle()
        val settingsMap = localDataSource.getSettingsSnapshot()

        val fullBackup = FullAppBackup(
            backupSchemaVersion = 2,
            settings = SettingsContent(settingsMap),
            snapshotBundle = snapshotBundle,
        )
        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFile(uriString: String): Result<String> =
        importFullBackupFromFileV2(uriString)

    suspend fun parseBackupFile(uriString: String): Result<FullAppBackup> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d("Parsing backup file from URI: $uriString")
        try {
            val jsonResult = contentProvider.readText(uriString)
            val jsonString = jsonResult.getOrThrow()
            val normalizedJson = sanitizeIncomingBackupJson(jsonString)
            Log.e("FullJsonImport", "parseBackupFile chars=${normalizedJson.length} head=${normalizedJson.take(120)}")

            if (normalizedJson.isBlank()) {
                Timber.tag(tag).w( "Parse failed: Backup file is empty or blank.")
                return@withContext Result.failure(Exception("Backup file is empty"))
            }
            val rawRoot = JsonParser.parseString(normalizedJson).asJsonObject
            val backupData = gson.fromJson(normalizedJson, FullAppBackup::class.java)

            backupData.snapshotBundle?.let { snapshotBundle ->
                val rawSnapshotBundle =
                    rawRoot
                        .get("snapshotBundle")
                        ?.takeIf { it.isJsonObject }
                        ?.asJsonObject
                        ?: throw IllegalArgumentException("FullAppBackup snapshotBundle must be a JSON object.")

                CanonicalDayThemeImportGate.requireFullRestoreImportable(
                    rawSnapshotBundle = rawSnapshotBundle,
                    decodedBundle = snapshotBundle,
                )
                requireValidCanonicalDayThemePayload(snapshotBundle)
            }

            Timber.tag(tag).d( "Successfully parsed and validated backup file object.")
            Result.success(backupData)
        } catch (e: Exception) {
            Timber.tag(tag).e( "Failed to parse backup file", e)
            Result.failure(e)
        }
    }

    suspend fun resolveSnapshotBundleForImport(uriString: String): Result<ResolvedImportBundle> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d("Resolving snapshot bundle for import from URI: $uriString")
        try {
            val jsonResult = contentProvider.readText(uriString)
            val jsonString = jsonResult.getOrThrow()
            val normalizedJson = sanitizeIncomingBackupJson(jsonString)
            Result.success(resolveIncomingImportBundle(normalizedJson))
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to resolve snapshot bundle for import")
            Result.failure(e)
        }
    }

    // === New Snapshot-based Methods (V2) ===

    suspend fun exportFullBackupToFileV2(): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to export snapshot backup to file.")
        try {
            val json = createFullSnapshotJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_snapshot_backup_$ts.json"

            contentProvider.saveFile(name, json).fold(
                onSuccess = {
                    Timber.tag(tag).i("Full backup successfully exported to file: $name")
                    Result.success("Файл бекапу (V2) успішно збережено")
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Timber.tag(tag).e( "Error exporting snapshot backup", e)
            Result.failure(e)
        }
    }

    suspend fun createFullSnapshotJsonString(): String {
        val snapshotBundle = localDataSource.loadFullSnapshotBundle()
        val settingsMap = localDataSource.getSettingsSnapshot()

        val fullBackup = FullAppBackup(
            backupSchemaVersion = 2,
            settings = SettingsContent(settingsMap),
            snapshotBundle = snapshotBundle,
        )
        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFileV2(uriString: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to import smart backup (V2) from URI: $uriString")
        try {
            val jsonResult = contentProvider.readText(uriString)
            val jsonString = jsonResult.getOrThrow()
            val normalizedJson = sanitizeIncomingBackupJson(jsonString)
            val backupData = gson.fromJson(normalizedJson, FullAppBackup::class.java)
            val resolvedSnapshotBundle = resolveIncomingImportBundle(normalizedJson).snapshotBundle
            val snapshotBundleToApply =
                resolvedSnapshotBundle.copy(
                    dayTasks =
                        resolvedSnapshotBundle.dayTasks.map { task ->
                            if (task.executionStrictness == null) {
                                task.copy(executionStrictness = "NORMAL")
                            } else {
                                task
                            }
                        },
                )

            if (isEffectivelyEmpty(snapshotBundleToApply)) {
                throw IllegalArgumentException("Backup payload is empty. Nothing to import.")
            }

            mergeRepository.applyServerChanges(snapshotBundleToApply).getOrThrow()

            backupData.settings?.settings?.let {
                localDataSource.restoreSettings(it)
            }

            Timber.tag(tag).i("Smart backup successfully imported and merged from URI: $uriString")
            Result.success("Дані імпортовано: ${snapshotBundleToApply.importItemCount()} items")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "A critical error occurred during the smart import process.")
            Result.failure(e)
        }
    }

    suspend fun importBackupJsonString(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val normalizedJson = sanitizeIncomingBackupJson(jsonString)
            val backupData = runCatching { gson.fromJson(normalizedJson, FullAppBackup::class.java) }.getOrNull()
            val snapshotBundleToApply = resolveIncomingImportBundle(normalizedJson).snapshotBundle
            Log.e(
                "DaySyncImport",
                "json import resolved chars=${normalizedJson.length} ${snapshotBundleToApply.describeDayPayload()}",
            )

            if (isEffectivelyEmpty(snapshotBundleToApply)) {
                throw IllegalArgumentException("Backup payload is empty. Nothing to import.")
            }

            mergeRepository.applyServerChanges(snapshotBundleToApply).getOrThrow()
            backupData?.settings?.settings?.let { localDataSource.restoreSettings(it) }

            Result.success(snapshotBundleToApply.importItemCount())
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "A critical error occurred during JSON import.")
            Result.failure(e)
        }
    }

    private fun resolveIncomingImportBundle(normalizedJson: String): ResolvedImportBundle {
        val jsonObject = JsonParser.parseString(normalizedJson).asJsonObject
        val backup = gson.fromJson(normalizedJson, FullAppBackup::class.java)
        val snapshot = backup.snapshotBundle
        val desktop = workspaceSnapshotResolver.resolve(normalizedJson)

        return when {
            snapshot != null -> {
                val raw =
                    jsonObject.get("snapshotBundle")
                        ?.takeIf { it.isJsonObject }
                        ?.asJsonObject
                        ?: error("FullAppBackup snapshotBundle must be a JSON object.")

                ResolvedImportBundle(
                    snapshotBundle =
                        CanonicalDayThemeImportGate.requireImportable(
                            rawSnapshotBundle = raw,
                            decodedBundle = snapshot,
                        ),
                    descriptor =
                        WorkspaceImportDescriptor(
                            format = WorkspaceSnapshotFormat.AndroidSnapshotBundleV2,
                            sourceMode = WorkspaceImportSourceMode.SnapshotBundle,
                        ),
                )
            }

            desktop?.format == WorkspaceSnapshotFormat.Desktop ->
                ResolvedImportBundle(
                    snapshotBundle =
                        desktopWorkspaceSnapshotSyncAdapter.toSnapshotBundle(desktop.snapshot),
                    descriptor =
                        WorkspaceImportDescriptor(
                            format = WorkspaceSnapshotFormat.Desktop,
                            sourceMode = WorkspaceImportSourceMode.DesktopSnapshot,
                        ),
                )

            hasSnapshotBundleKeys(jsonObject) -> {
                val decoded = gson.fromJson(normalizedJson, SnapshotBundle::class.java)
                ResolvedImportBundle(
                    snapshotBundle =
                        CanonicalDayThemeImportGate.requireImportable(
                            rawSnapshotBundle = jsonObject,
                            decodedBundle = decoded,
                        ),
                    descriptor =
                        WorkspaceImportDescriptor(
                            format = WorkspaceSnapshotFormat.AndroidSnapshotBundleV2,
                            sourceMode = WorkspaceImportSourceMode.SnapshotBundle,
                        ),
                )
            }

            else -> error("Unsupported backup format: canonical SnapshotBundle is required.")
        }
    }

    private fun hasSnapshotBundleKeys(jsonObject: com.google.gson.JsonObject): Boolean {
        return jsonObject.has("contexts") || jsonObject.has("snapshotVersion")
    }

    private fun isEffectivelyEmpty(bundle: SnapshotBundle): Boolean {
        return bundle.importItemCount() == 0
    }

    private fun sanitizeIncomingBackupJson(rawJson: String): String {
        requireNoLegacyTaskRecurrenceV1(rawJson)
        return rawJson.replace(
            Regex("\"experimentalCapabilityIds\"\\s*:\\s*null"),
            "\"experimentalCapabilityIds\":[]",
        )
    }
}

private fun SnapshotBundle.describeDayPayload(): String {
    val planSample = dayPlans
        .sortedByDescending { it.updatedAt }
        .take(4)
        .joinToString { "${it.id}:${it.date}:v${it.version}:u${it.updatedAt}" }
    val taskSample = dayTasks
        .sortedByDescending { it.updatedAt }
        .take(6)
        .joinToString { "${it.id}:${it.dayPlanId}:v${it.version}:u${it.updatedAt}:${it.title.take(28)}" }
    val focusSample = dayFocusItems
        .sortedByDescending { it.updatedAt }
        .take(4)
        .joinToString { "${it.id}:${it.dayPlanId}:${it.type}:v${it.version}:u${it.updatedAt}" }

    return listOf(
        "plans=${dayPlans.size}",
        "tasks=${dayTasks.size}",
        "focus=${dayFocusItems.size}",
        "runtime=${dayManagementRuntimeState != null}",
        "planSample=$planSample",
        "taskSample=$taskSample",
        "focusSample=$focusSample",
    ).joinToString(" ")
}
