package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BackupRepository @Inject constructor() {
    open suspend fun exportFullBackupToFile(): Result<String> = Result.success("")
    open suspend fun createFullBackupJsonString(): String = "{}"
    open suspend fun importFullBackupFromFile(uri: Uri): Result<String> = Result.success("")
    open suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = Result.success(FullAppBackup(settings = com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent(emptyMap())))
    open suspend fun getLastSyncTime(): Long? = null
}
