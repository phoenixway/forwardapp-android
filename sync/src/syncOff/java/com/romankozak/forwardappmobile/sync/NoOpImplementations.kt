package com.romankozak.forwardappmobile.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.sync.BackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange
import com.romankozak.forwardappmobile.core.data.models.sync.SyncReport
import com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.datasource.CanonicalRecurringSeriesSyncVersion
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.SyncLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.SyncSettingsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

// region NoOp DataSources and Helpers

@Singleton
class NoOpFullBackupLocalDataSource @Inject constructor() : FullBackupLocalDataSource {
    override suspend fun loadFullDatabaseContent(): DatabaseContent = DatabaseContent()
    override suspend fun getSettingsSnapshot(): Map<String, String> = emptyMap()
    override suspend fun loadUnsyncedCanonicalRecurringSeries() = emptyList<com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot>()
    override suspend fun loadCanonicalRecurringSeriesChangedSince(timestamp: Long) = emptyList<com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot>()
    override suspend fun markCanonicalRecurringSeriesSynced(series: List<CanonicalRecurringSeriesSyncVersion>) { /* no-op */ }
    override suspend fun loadUnsyncedCanonicalDayThemes() =
        com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncPayload()
    override suspend fun loadCanonicalDayThemesChangedSince(timestamp: Long) =
        com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncPayload()
    override suspend fun markCanonicalDayThemesSynced(
        ack: com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncAck,
    ) { /* no-op */ }
    override suspend fun loadUnsyncedCanonicalOrientations() =
        com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload()
    override suspend fun markCanonicalOrientationsSynced(
        ack: com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncAck,
    ) { /* no-op */ }
    override suspend fun restoreDatabaseFromBackup(content: DatabaseContent) {
        Log.d("NoOpSync", "NoOpFullBackupLocalDataSource: restoreDatabaseFromBackup called")
    }
    override suspend fun restoreSettings(settings: Map<String, String>) {
        Log.d("NoOpSync", "NoOpFullBackupLocalDataSource: restoreSettings called")
    }
    override suspend fun clearAllTables() {
        Log.d("NoOpSync", "NoOpFullBackupLocalDataSource: clearAllTables called")
    }
}

@Singleton
class NoOpSyncLocalDataSource @Inject constructor() : SyncLocalDataSource {
    override suspend fun getUnsyncedChanges(): DatabaseContent = DatabaseContent()
    override suspend fun getChangesSince(timestamp: Long): DatabaseContent = DatabaseContent()
    override suspend fun markSyncedNow(content: DatabaseContent) {
        Log.d("NoOpSync", "NoOpSyncLocalDataSource: markSyncedNow called")
    }
    override suspend fun loadLocalDatabaseContent(): DatabaseContent = DatabaseContent()
    override suspend fun clearAllTables() {
        Log.d("NoOpSync", "NoOpSyncLocalDataSource: clearAllTables called")
    }
}

@Singleton
class NoOpSyncSettingsSource @Inject constructor() : SyncSettingsSource {
    override val wifiSyncPortFlow: Flow<Int> = flowOf(0)
}

// Needs to be 'open' in main module for this to work
@Singleton
open class NoOpSyncLogicHelper @Inject constructor() : SyncLogicHelper() {
    // All methods have default implementations or can be overridden with no-op logic
}

@Singleton
class NoOpMergeLocalDataSource @Inject constructor() : MergeLocalDataSource {
    override suspend fun getContexts(): List<Context> = emptyList()
    override suspend fun getGoals(): List<Goal> = emptyList()
    override suspend fun getLocalDatabaseContent(): DatabaseContent = DatabaseContent()
    override suspend fun insertContexts(contexts: List<Context>) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: insertContexts called")
    }
    override suspend fun insertGoals(goals: List<Goal>) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: insertGoals called")
    }
    override suspend fun insertAttachments(attachments: List<AttachmentEntity>) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: insertAttachments called")
    }
    override suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: insertContextAttachmentLinks called")
    }
    override suspend fun insertListItems(items: List<BacklogItem>) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: insertListItems called")
    }
    override suspend fun applyChanges(changes: List<SyncChange>) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: applyChanges called")
    }
    override suspend fun importSelectedData(
        projects: List<Context>,
        goals: List<Goal>,
        listItems: List<BacklogItem>,
        attachments: List<AttachmentEntity>,
        crossRefs: List<ContextAttachmentCrossRef>
    ) {
        Log.d("NoOpSync", "NoOpMergeLocalDataSource: importSelectedData called")
    }
}

// endregion

// region NoOp Services and Repositories (implementing main classes/interfaces)

@Singleton
open class NoOpSyncFileService @Inject constructor(
    @ApplicationContext context: Context,
    localDataSource: FullBackupLocalDataSource
) {
    open suspend fun exportFullBackupToFile(): Result<String> = Result.failure(Exception("Disabled"))
    open suspend fun exportFullBackupToFileV2(): Result<String> = Result.failure(Exception("Disabled"))
    open suspend fun createFullBackupJsonString(): String = ""
    open suspend fun importFullBackupFromFile(uri: Uri): Result<String> = Result.failure(Exception("Disabled"))
    open suspend fun importFullBackupFromFileV2(uri: Uri): Result<String> = Result.failure(Exception("Disabled"))
    open suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> =
        Result.failure(Exception("Disabled"))
}

@Singleton
open class NoOpSyncWifiService @Inject constructor(
    localDataSource: SyncLocalDataSource,
    settingsSource: SyncSettingsSource,
    logicHelper: SyncLogicHelper
) {
    open suspend fun fetchBackupFromWifi(address: String, deltaSince: Long?): Result<String> = Result.failure(Exception("Disabled"))
    open suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = Result.failure(Exception("Disabled"))
    open suspend fun createDeltaBackupJsonString(deltaSince: Long): String = ""
}

@Singleton
open class NoOpMergeRepository @Inject constructor(
    localDataSource: MergeLocalDataSource,
    logicHelper: SyncLogicHelper
) {
    open suspend fun createSyncReport(jsonString: String): SyncReport = SyncReport(emptyList())
    open suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> = Result.failure(Exception("Disabled"))
    open suspend fun createBackupDiff(incoming: DatabaseContent): LegacyBackupDiff = LegacyBackupDiff()
    open suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        Log.d("NoOpSync", "NoOpMergeRepository: applyChanges called")
    }
    open suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> = Result.failure(Exception("Disabled"))
}

@Singleton
class NoOpAttachmentsRepository @Inject constructor() : AttachmentsRepository {
    override suspend fun exportAttachmentsToFile(): Result<String> = Result.failure(Exception("Disabled"))
    override suspend fun createAttachmentsBackupJsonString(): String = ""
    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> = Result.failure(Exception("Disabled"))

    override suspend fun ensureAttachmentLinkedToContext(
        attachmentType: String,
        entityId: String,
        contextId: String,
        ownerContextId: String?,
        createdAt: Long,
        roleCode: String?,
        isSystem: Boolean
    ) {
        Log.d("NoOpSync", "NoOpAttachmentsRepository: ensureAttachmentLinkedToContext called")
    }

    override suspend fun findAttachmentByEntity(attachmentType: String, entityId: String): AttachmentEntity? = null
    override suspend fun deleteAttachment(attachmentId: String) {
        Log.d("NoOpSync", "NoOpAttachmentsRepository: deleteAttachment called")
    }

    override fun getAttachmentLibraryItems(): Flow<List<AttachmentLibraryQueryResult>> = flowOf(emptyList())
    override fun getAllAttachmentLinks(): Flow<List<ContextAttachmentCrossRef>> = flowOf(emptyList())
    override suspend fun linkAttachmentToContext(attachmentId: String, contextId: String) {
        Log.d("NoOpSync", "NoOpAttachmentsRepository: linkAttachmentToContext called")
    }

    override fun getAttachmentsForContext(contextId: String): Flow<List<AttachmentWithContext>> = flowOf(emptyList())
    override suspend fun getAttachmentById(id: String): AttachmentEntity? = null
    override suspend fun unlinkAttachmentFromContext(attachmentId: String, contextId: String) {
        Log.d("NoOpSync", "NoOpAttachmentsRepository: unlinkAttachmentFromContext called")
    }
    override suspend fun updateAttachmentOrders(contextId: String, orders: Map<String, Long>) {
        Log.d("NoOpSync", "NoOpAttachmentsRepository: updateAttachmentOrders called")
    }
    override suspend fun createLinkAttachment(
        contextId: String,
        link: RelatedLink,
        roleCode: String?,
        isSystem: Boolean
    ): String = ""

    override suspend fun findAttachmentByRole(contextId: String, roleCode: String): AttachmentEntity? = null
}

@Singleton
open class NoOpSyncApi @Inject constructor(
    private val fileService: NoOpSyncFileService,
    private val wifiSyncService: NoOpSyncWifiService,
    private val mergeRepository: NoOpMergeRepository,
    private val attachmentsRepository: NoOpAttachmentsRepository,
) : SyncApi {
    override suspend fun exportFullBackupToFile(): Result<String> = fileService.exportFullBackupToFile()
    override suspend fun exportFullBackupToFileV2(): Result<String> = fileService.exportFullBackupToFileV2()
    override suspend fun createFullBackupJsonString(): String = fileService.createFullBackupJsonString()
    override suspend fun importFullBackupFromFile(uri: Uri): Result<String> = fileService.importFullBackupFromFile(uri)
    override suspend fun importFullBackupFromFileV2(uri: Uri): Result<String> = fileService.importFullBackupFromFileV2(uri)
    override suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = fileService.parseBackupFile(uri)

    override suspend fun fetchBackupFromWifi(address: String, deltaSince: Long?): Result<String> = wifiSyncService.fetchBackupFromWifi(address, deltaSince)
    override suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = wifiSyncService.pushUnsyncedToWifi(address)
    override suspend fun createDeltaBackupJsonString(deltaSince: Long): String = wifiSyncService.createDeltaBackupJsonString(deltaSince)

    override suspend fun getLastSyncTime(): Long? = null
    override suspend fun createSyncReport(jsonString: String): SyncReport = mergeRepository.createSyncReport(jsonString)
    override suspend fun applyChanges(approvedChanges: List<SyncChange>) = mergeRepository.applyChanges(approvedChanges)
    override suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> = mergeRepository.applyServerChanges(changes)
    override suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff = mergeRepository.createBackupDiff(incoming)
    override suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> = mergeRepository.importSelectedData(selectedData)

    override suspend fun exportAttachmentsToFile(): Result<String> = attachmentsRepository.exportAttachmentsToFile()
    override suspend fun createAttachmentsBackupJsonString(): String = attachmentsRepository.createAttachmentsBackupJsonString()
    override suspend fun importAttachmentsFromFile(uri: Uri): Result<String> = attachmentsRepository.importAttachmentsFromFile(uri)
}

// endregion
