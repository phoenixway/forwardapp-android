// Файл: AttachmentsRepository.kt
package com.romankozak.forwardappmobile.sync

import android.net.Uri

interface AttachmentsRepository {
    suspend fun exportAttachmentsToFile(): Result<String>
    suspend fun createAttachmentsBackupJsonString(): String
    suspend fun importAttachmentsFromFile(uri: Uri): Result<String>
}