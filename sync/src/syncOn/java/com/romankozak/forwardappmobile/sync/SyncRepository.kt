package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val fileService: SyncFileService,
    private val wifiSyncService: SyncWifiService,
    private val mergeRepository: MergeRepository,
    private val attachmentsRepository: AttachmentsRepository,
    private val systemContextEnsurer: SystemContextEnsurer,
    private val fullBackupLocalDataSource: FullBackupLocalDataSource,
) : SyncApi {
    private val selectiveImportFilter = SnapshotBundleSelectiveImportFilter()
    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()
    private var pendingSettingsFromLastSyncReport: Map<String, String>? = null

    // === FILE OPERATIONS ===

    override suspend fun exportFullBackupToFile(): Result<String> =
        fileService.exportFullBackupToFile()

    override suspend fun createFullBackupJsonString(): String =
        fileService.createFullBackupJsonString()

    override suspend fun importFullBackupFromFile(uri: Uri): Result<String> =
        fileService.importFullBackupFromFile(uri.toString())

    override suspend fun exportFullBackupToFileV2(): Result<String> =
        fileService.exportFullBackupToFileV2()

    override suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> =
        fileService.parseBackupFile(uri.toString())

    suspend fun resolveSnapshotBundleForImport(uri: Uri): Result<ResolvedImportBundle> =
        fileService.resolveSnapshotBundleForImport(uri.toString())

    /**
     *
     * Новий метод для Snapshot-бекапів (V2).
     * Якщо його ще немає в SyncApi — додайте його туди.
     */
    override suspend fun importFullBackupFromFileV2(uri: Uri): Result<String> =
        fileService.importFullBackupFromFileV2(uri.toString())

    // === WI-FI OPERATIONS ===

    // Виправлено: SyncApi очікує Result<String>
    override suspend fun fetchBackupFromWifi(address: String, deltaSince: Long?): Result<String> =
        wifiSyncService.fetchBackupFromWifi(address, deltaSince)

    // Виправлено: SyncApi очікує Result<Unit>
    override suspend fun pushUnsyncedToWifi(address: String): Result<Unit> =
        wifiSyncService.pushUnsyncedToWifi(address)

    override suspend fun createDeltaBackupJsonString(deltaSince: Long): String =
        wifiSyncService.createDeltaBackupJsonString(deltaSince)

    // === MERGE & REPORTING ===

    override suspend fun getLastSyncTime(): Long? = null

    // Виправлено: SyncApi очікує SyncReport (а не List<SyncChange>)
    override suspend fun createSyncReport(jsonString: String): SyncReport {
        try {
            val backup = gson.fromJson(sanitizeIncomingBackupJson(jsonString), FullAppBackup::class.java)
            pendingSettingsFromLastSyncReport = backup.settings?.settings
        } catch (_: Exception) {
            pendingSettingsFromLastSyncReport = null
        }
        return mergeRepository.createSyncReport(jsonString)
    }

    // Виправлено: SyncApi очікує Unit (або Result<Unit>, перевірте SyncApi)
    // Якщо SyncApi вимагає Unit, ми просто викликаємо метод
    override suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        mergeRepository.applyChanges(approvedChanges)
        pendingSettingsFromLastSyncReport?.let { settings ->
            try {
                fullBackupLocalDataSource.restoreSettings(settings)
            } catch (_: Exception) {
            }
        }
        pendingSettingsFromLastSyncReport = null
    }

    suspend fun applyServerChanges(changes: SnapshotBundle): Result<Unit> {
        val result = mergeRepository.applyServerChanges(changes)
        systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    suspend fun importBackupJsonString(jsonString: String): Result<Int> {
        val result = fileService.importBackupJsonString(jsonString)
        if (result.isSuccess) systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    suspend fun createBackupDiff(incoming: SnapshotBundle): BackupDiff =
        mergeRepository.createBackupDiff(incoming)

    private fun sanitizeIncomingBackupJson(rawJson: String): String {
        requireNoLegacyTaskRecurrenceV1(rawJson)
        return rawJson.replace(
            Regex("\"experimentalCapabilityIds\"\\s*:\\s*null"),
            "\"experimentalCapabilityIds\":[]",
        )
    }

    suspend fun loadSelectiveImportPreview(
        uri: Uri,
    ): Result<SelectiveImportPreviewBundle> =
        resolveSnapshotBundleForImport(uri).map { resolved ->
            SelectiveImportPreviewBundle(
                descriptor = resolved.descriptor,
                sourceSnapshotBundle = resolved.snapshotBundle,
                snapshotDiff = createBackupDiff(resolved.snapshotBundle),
            )
        }

    suspend fun importSelectedSnapshotBundle(bundle: SnapshotBundle): Result<String> {
        val result = mergeRepository.importSelectedSnapshotBundle(bundle)
        systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    fun filterSnapshotBundleForSelectiveImport(
        bundle: SnapshotBundle,
        selection: WorkspaceSelectiveImportSelection,
    ): SnapshotBundle = selectiveImportFilter.filter(bundle, selection)


    // === ATTACHMENTS ===

    override suspend fun exportAttachmentsToFile(): Result<String> =
        attachmentsRepository.exportAttachmentsToFile()

    override suspend fun createAttachmentsBackupJsonString(): String =
        attachmentsRepository.createAttachmentsBackupJsonString()

    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> =
        attachmentsRepository.importAttachmentsFromFile(uri)
}
