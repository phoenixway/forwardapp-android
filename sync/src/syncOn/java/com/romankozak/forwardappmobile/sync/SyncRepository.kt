package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val fileService: SyncFileService,
    private val wifiSyncService: SyncWifiService,
    private val mergeRepository: MergeRepository,
    private val attachmentsRepository: AttachmentsRepository,
    private val systemContextEnsurer: SystemContextEnsurer,
) : SyncApi {

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
    override suspend fun createSyncReport(jsonString: String): SyncReport =
        mergeRepository.createSyncReport(jsonString)

    // Виправлено: SyncApi очікує Unit (або Result<Unit>, перевірте SyncApi)
    // Якщо SyncApi вимагає Unit, ми просто викликаємо метод
    override suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        mergeRepository.applyChanges(approvedChanges)
    }

    override suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val result = mergeRepository.applyServerChanges(changes)
        systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    override suspend fun createBackupDiff(incoming: DatabaseContent): LegacyBackupDiff =
        mergeRepository.createBackupDiff(incoming)

    override suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        val result = mergeRepository.importSelectedData(selectedData)
        systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    // === ATTACHMENTS ===

    override suspend fun exportAttachmentsToFile(): Result<String> =
        attachmentsRepository.exportAttachmentsToFile()

    override suspend fun createAttachmentsBackupJsonString(): String =
        attachmentsRepository.createAttachmentsBackupJsonString()

    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> =
        attachmentsRepository.importAttachmentsFromFile(uri)
}