package com.romankozak.forwardappmobile.sync

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

@Singleton
class SyncFileService @Inject constructor(@param:dagger.hilt.android.qualifiers.ApplicationContext private val context: AndroidContext) {
    suspend fun exportFullBackupToFile(): Result<String> = Result.success("Sync disabled")
    suspend fun createFullBackupJsonString(): String = "Sync disabled"
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = Result.failure(UnsupportedOperationException("Sync disabled"))
    fun readTextFromUri(uri: Uri): String? = null
    fun saveFileToDownloads(name: String, json: String) { /* no-op */ }
}
