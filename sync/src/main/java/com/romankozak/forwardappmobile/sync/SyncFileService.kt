package com.romankozak.forwardappmobile.sync

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

/**
 * Сервіс для обробки операцій резервного копіювання та відновлення на основі файлів.
 * Керує експортом/імпортом повних бекапів бази даних.
 */
@Singleton
class SyncFileService @Inject constructor(
    @param:ApplicationContext private val context: AndroidContext,
    private val localDataSource: FullBackupLocalDataSource
) {
    private val TAG = "SyncFileService"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    /**
     * Експортує повний бекап у папку "Downloads/ForwardApp"
     */
    suspend fun exportFullBackupToFile(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = createFullBackupJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_full_backup_$ts.json"
            saveFileToDownloads(name, json)
            Result.success("Файл бекапу успішно збережено")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting full backup", e)
            Result.failure(e)
        }
    }

    /**
     * Створює JSON-рядок із повним бекапом даних та налаштувань
     */
    suspend fun createFullBackupJsonString(): String {
        val databaseContent = localDataSource.loadFullDatabaseContent()
        val settingsMap = localDataSource.getSettingsSnapshot()

        val fullBackup = FullAppBackup(
            backupSchemaVersion = 1,
            database = databaseContent,
            settings = SettingsContent(settingsMap)
        )

        return gson.toJson(fullBackup)
    }

    /**
     * Імпортує повний бекап із файлу.
     */
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = readTextFromUri(uri) ?: throw Exception("Файл порожній")
            val backupData = gson.fromJson(json, FullAppBackup::class.java)

            localDataSource.restoreDatabaseFromBackup(backupData.database)

            backupData.settings?.settings?.let {
                localDataSource.restoreSettings(it)
            }

            Result.success("Дані успішно відновлено")
        } catch (e: Exception) {
            Log.e(TAG, "Import error", e)
            Result.failure(e)
        }
    }

    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = withContext(Dispatchers.IO) {
        try {
            val jsonString = readTextFromUri(uri)
            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Backup file is empty"))
            }
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)
            Result.success(backupData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse backup file", e)
            Result.failure(e)
        }
    }

    fun readTextFromUri(uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    fun saveFileToDownloads(name: String, json: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = context.contentResolver.insert(collection, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(json.toByteArray())
            }
        }
    }
}