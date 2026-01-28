package com.romankozak.forwardappmobile.sync

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentsRepository @Inject constructor() {
    suspend fun exportAttachmentsToFile(): Result<String> = Result.success("Sync disabled")
    suspend fun createAttachmentsBackupJsonString(): String = "Sync disabled"
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String> = Result.failure(UnsupportedOperationException("Sync disabled"))
}
