package com.romankozak.forwardappmobile.sync

import Timber.log.Timber
import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.timber

@Singleton
class SyncFileService @Inject constructor(
    private val contentProvider: IContentProvider,
    private val localDataSource: FullBackupLocalDataSource,
    private val legacyMigrationMapper: LegacyMigrationMapper,
    private val mergeRepository: MergeRepository,
) {
    private val tag = "SyncFileService"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    // === Legacy Methods (V1) ===

    suspend fun exportFullBackupToFile(): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to export full backup to file.")
        try {
            val json = createFullBackupJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_full_backup_$ts.json"

            contentProvider.saveFile(name, json).fold(
                onSuccess = {
                    Timber.tag(tag).i("Full backup successfully exported to file: $name")
                    Result.success("Файл бекапу успішно збережено")
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Timber.tag(tag).e( "Error exporting full backup", e)
            Result.failure(e)
        }
    }

    suspend fun createFullBackupJsonString(): String {
        val databaseContent = localDataSource.loadFullDatabaseContent()
        val settingsMap = localDataSource.getSettingsSnapshot()

        val fullBackup = FullAppBackup(
            backupSchemaVersion = 1,
            database = databaseContent,
            settings = SettingsContent(settingsMap),
            snapshotBundle = null,
        )
        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFile(uriString: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to import full backup from URI: $uriString")
        try {
            val backupResult = parseBackupFile(uriString)
            if (backupResult.isFailure) {
                throw backupResult.exceptionOrNull() ?: Exception("Unknown parsing error")
            }
            val backupData = backupResult.getOrThrow()

            backupData.database?.let { localDataSource.restoreDatabaseFromBackup(it) }
            backupData.settings?.settings?.let { localDataSource.restoreSettings(it) }

            Timber.tag(tag).i("Full backup successfully imported from URI: $uriString")
            Result.success("Дані успішно відновлено")
        } catch (e: Exception) {
            Timber.tag(tag).e("A critical error occurred during the import process.", e)
            Result.failure(e)
        }
    }

    suspend fun parseBackupFile(uriString: String): Result<FullAppBackup> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d("Parsing backup file from URI: $uriString")
        try {
            val jsonResult = contentProvider.readText(uriString)
            val jsonString = jsonResult.getOrThrow()

            if (jsonString.isBlank()) {
                Timber.tag(tag).w( "Parse failed: Backup file is empty or blank.")
                return@withContext Result.failure(Exception("Backup file is empty"))
            }
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)
            Timber.tag(tag).d( "Successfully parsed backup file object.")
            Result.success(backupData)
        } catch (e: Exception) {
            Timber.tag(tag).e( "Failed to parse backup file", e)
            Result.failure(e)
        }
    }

    // === New Snapshot-based Methods (V2) ===

    suspend fun exportFullBackupToFileV2(): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to export snapshot backup to file.")
        try {
            val json = createFullSnapshotJsonString()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "forward_snapshot_backup_$ts.json"

            contentProvider.saveFile(name, json).fold(
                onSuccess = {
                    Timber.tag(tag).i("Full backup successfully exported to file: $name")
                    Result.success("Файл бекапу (V2) успішно збережено")
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Timber.tag(tag).e( "Error exporting snapshot backup", e)
            Result.failure(e)
        }
    }

    suspend fun createFullSnapshotJsonString(): String {
        val snapshotBundle = localDataSource.loadFullSnapshotBundle()
        val settingsMap = localDataSource.getSettingsSnapshot()

        val fullBackup = FullAppBackup(
            backupSchemaVersion = 2,
            database = null,
            settings = SettingsContent(settingsMap),
            snapshotBundle = snapshotBundle,
        )
        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFileV2(uriString: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(tag).d( "Attempting to import smart backup (V2) from URI: $uriString")
        try {
            val jsonResult = contentProvider.readText(uriString)
            val jsonString = jsonResult.getOrThrow()

            // Спроба розпарсити як новий формат (FullAppBackup)
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)

            val snapshotBundleToApply = if (backupData.snapshotBundle != null) {
                Timber.tag(tag).d("Successfully parsed as new SnapshotBundle format.")
                backupData.snapshotBundle!!
            } else if (backupData.database != null) {
                Timber.tag(tag).d("Parsed as legacy FullAppBackup format. Migrating to SnapshotBundle...")
                legacyMigrationMapper.toSnapshotBundle(backupData.database!!)
            } else {
                Timber.tag(tag).d("Could not parse as FullAppBackup, trying as raw DatabaseContent.")
                val databaseContent = gson.fromJson(jsonString, DatabaseContent::class.java)
                legacyMigrationMapper.toSnapshotBundle(databaseContent)
            }

            // Використання логіки об'єднання
            mergeRepository.applyServerChanges(snapshotBundleToApply)

            // Відновлення налаштувань
            backupData.settings?.settings?.let {
                localDataSource.restoreSettings(it)
            }

            Timber.tag(tag).i("Smart backup successfully imported and merged from URI: $uriString")
            Result.success("Дані успішно імпортовано та об'єднано (V2)")
        } catch (e: Exception) {
            Timber.tag(tag).e( "A critical error occurred during the smart import process.", e)
            Result.failure(e)
        }
    }
}