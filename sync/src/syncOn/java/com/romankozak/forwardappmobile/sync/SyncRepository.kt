package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer // Added import
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val fileService: SyncFileService,
    private val wifiSyncService: SyncWifiService,
    private val mergeRepository: MergeRepository,
    private val attachmentsRepository: AttachmentsRepository,
    private val systemContextEnsurer: SystemContextEnsurer, // Injected interface
) : SyncApi {

    // FILE OPERATIONS
    override suspend fun exportFullBackupToFile() = fileService.exportFullBackupToFile()
    override suspend fun createFullBackupJsonString() = fileService.createFullBackupJsonString()
    override suspend fun importFullBackupFromFile(uri: Uri) = fileService.importFullBackupFromFile(uri)

    // Тут важливо вказати тип явно, щоб не було помилок subtype
    override suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = fileService.parseBackupFile(uri)

    // WI-FI OPERATIONS
    override suspend fun fetchBackupFromWifi(address: String, deltaSince: Long?) =
        wifiSyncService.fetchBackupFromWifi(address, deltaSince)

    override suspend fun pushUnsyncedToWifi(address: String) =
        wifiSyncService.pushUnsyncedToWifi(address)

    override suspend fun createDeltaBackupJsonString(deltaSince: Long) =
        wifiSyncService.createDeltaBackupJsonString(deltaSince)

    // MERGE & REPORTING
    override suspend fun getLastSyncTime(): Long? = null

    override suspend fun createSyncReport(jsonString: String) = mergeRepository.createSyncReport(jsonString)

    // Тепер ці методи знайдуться в mergeRepository
    override suspend fun applyChanges(approvedChanges: List<SyncChange>) =
        mergeRepository.applyChanges(approvedChanges)

    override suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val result = mergeRepository.applyServerChanges(changes)
        systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    override suspend fun createBackupDiff(incoming: DatabaseContent) =
        mergeRepository.createBackupDiff(incoming)

    override suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        val result = mergeRepository.importSelectedData(selectedData)
        systemContextEnsurer.ensureAllSystemContextsExist()
        return result
    }

    // ATTACHMENTS
    override suspend fun exportAttachmentsToFile() = attachmentsRepository.exportAttachmentsToFile()
    override suspend fun createAttachmentsBackupJsonString() = attachmentsRepository.createAttachmentsBackupJsonString()
    override suspend fun importAttachmentsFromFile(uri: Uri) = attachmentsRepository.importAttachmentsFromFile(uri)
}