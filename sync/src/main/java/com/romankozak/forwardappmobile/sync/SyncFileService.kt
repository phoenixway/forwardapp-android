package com.romankozak.forwardappmobile.sync

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext
import com.google.gson.JsonObject

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
        Log.d(TAG, "Attempting to export full backup to file.")
        try {
            val json = createFullBackupJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_full_backup_$ts.json"
            saveFileToDownloads(name, json)
            Log.i(TAG, "Full backup successfully exported to file: $name")
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
        Log.d(TAG, "Attempting to import full backup from URI: $uri")
        try {
            val backupResult = parseBackupFile(uri)
            
            if (backupResult.isFailure) {
                // The error is already logged in parseBackupFile, just re-throw
                throw backupResult.exceptionOrNull() ?: Exception("Unknown parsing error")
            }
            
            val backupData = backupResult.getOrThrow()

            localDataSource.restoreDatabaseFromBackup(backupData.database)

            backupData.settings?.settings?.let {
                localDataSource.restoreSettings(it)
            }

            Log.i(TAG, "Full backup successfully imported from URI: $uri")
            Result.success("Дані успішно відновлено")
        } catch (e: Exception) {
            Log.e(TAG, "A critical error occurred during the import process.", e)
            Result.failure(e)
        }
    }

    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Parsing backup file from URI: $uri")
        try {
            val jsonString = readTextFromUri(uri)
            if (jsonString.isNullOrBlank()) {
                Log.w(TAG, "Parse failed: Backup file is empty or blank.")
                return@withContext Result.failure(Exception("Backup file is empty"))
            }

            // Parse to a generic JsonObject to manually clean the data
            val rootObject = gson.fromJson(jsonString, JsonObject::class.java)
            val databaseObject = rootObject.getAsJsonObject("database")
            
            var wasCleaned = false

            if (databaseObject != null && databaseObject.has("backlogItems")) {
                val backlogItemsArray = databaseObject.getAsJsonArray("backlogItems")
                if (backlogItemsArray != null) {
                    val originalSize = backlogItemsArray.size()
                    val cleanedBacklogItems = backlogItemsArray.filter {
                        val item = if (it.isJsonObject) it.asJsonObject else null
                        item != null &&
                            (item.has("itemType") && !item.get("itemType").isJsonNull) &&
                            (item.has("entityId") && !item.get("entityId").isJsonNull)
                    }

                    if (originalSize > cleanedBacklogItems.size) {
                        val ignoredCount = originalSize - cleanedBacklogItems.size
                        Log.w(TAG, "Ignored $ignoredCount corrupt BacklogItems with null itemType or entityId during backup parsing.")
                        
                        // Reconstruct the database object with the cleaned array
                        val cleanedDatabaseObject = databaseObject.deepCopy()
                        cleanedDatabaseObject.remove("backlogItems") // remove old
                        cleanedDatabaseObject.add("backlogItems", gson.toJsonTree(cleanedBacklogItems)) // add new
                        
                        rootObject.remove("database") // remove old
                        rootObject.add("database", cleanedDatabaseObject) // add new
                        wasCleaned = true
                    }
                }
            }
            
            val finalJson = rootObject.toString()
            val backupData = gson.fromJson(finalJson, FullAppBackup::class.java)

            Log.d(TAG, "Successfully parsed backup file. Was data cleaned: $wasCleaned")
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