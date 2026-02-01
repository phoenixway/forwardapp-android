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
    private val localDataSource: FullBackupLocalDataSource,
    private val legacyMigrationMapper: LegacyMigrationMapper,
    private val mergeRepository: MergeRepository
) {
    private val TAG = "SyncFileService"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    // === Legacy Methods ===

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
                throw backupResult.exceptionOrNull() ?: Exception("Unknown parsing error")
            }
            val backupData = backupResult.getOrThrow()

            backupData.database?.let { localDataSource.restoreDatabaseFromBackup(it) }

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
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)
            Log.d(TAG, "Successfully parsed backup file object.")
            Result.success(backupData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse backup file", e)
            Result.failure(e)
        }
    }

    // === New Snapshot-based Methods ===

    suspend fun exportFullBackupToFileV2(): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting to export snapshot backup to file.")
        try {
            val json = createFullSnapshotJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_snapshot_backup_$ts.json"
            saveFileToDownloads(name, json)
            Log.i(TAG, "Full backup successfully exported to file: $name")
            Result.success("Файл бекапу (V2) успішно збережено")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting snapshot backup", e)
            Result.failure(e)
        }
    }

    suspend fun createFullSnapshotJsonString(): String {
        val snapshotBundle = localDataSource.loadFullSnapshotBundle()
        val settingsMap = localDataSource.getSettingsSnapshot()

        val fullBackup = FullAppBackup(
            backupSchemaVersion = 2, // New version
            database = null, // Old field is null
            settings = SettingsContent(settingsMap),
            snapshotBundle = snapshotBundle // New field with all the data
        )

        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFileV2(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting to import smart backup (V2) from URI: $uri")
        try {
            val jsonString = readTextFromUri(uri) ?: throw IOException("Failed to read file from URI.")

            // Try parsing as new format first
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)

            val snapshotBundleToApply = if (backupData.snapshotBundle != null) {
                Log.d(TAG, "Successfully parsed as new SnapshotBundle format.")
                backupData.snapshotBundle!!
            } else if (backupData.database != null) {
                // It's the old format, migrate it
                Log.d(TAG, "Parsed as legacy FullAppBackup format. Migrating to SnapshotBundle...")
                legacyMigrationMapper.toSnapshotBundle(backupData.database!!)
            } else {
                // Could be an even older format, just a raw DatabaseContent
                Log.d(TAG, "Could not parse as FullAppBackup, trying as raw DatabaseContent.")
                val databaseContent = gson.fromJson(jsonString, com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent::class.java)
                legacyMigrationMapper.toSnapshotBundle(databaseContent)
            }

            // At this point, we have a SnapshotBundle, one way or another.
            // Now, we use the non-destructive merge logic.
            mergeRepository.applyServerChanges(snapshotBundleToApply)

            // Restore settings separately
            backupData.settings?.settings?.let {
                localDataSource.restoreSettings(it)
            }

            Log.i(TAG, "Smart backup successfully imported and merged from URI: $uri")
            Result.success("Дані успішно імпортовано та об'єднано (V2)")
        } catch (e: Exception) {
            Log.e(TAG, "A critical error occurred during the smart import process.", e)
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