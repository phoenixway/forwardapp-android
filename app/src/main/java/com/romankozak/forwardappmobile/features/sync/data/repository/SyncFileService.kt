package com.romankozak.forwardappmobile.features.sync.data.repository

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncLocalService
import com.romankozak.forwardappmobile.data.repository.SyncMapper
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.LongDeserializer
import com.romankozak.forwardappmobile.data.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.data.sync.SettingsContent
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

/**
 * Service for handling file-based backup and restore operations.
 * Manages export/import of full database backups to/from files.
 */
@Singleton
class SyncFileService @Inject constructor(
    @param:ApplicationContext private val context: AndroidContext,
    private val appDatabase: AppDatabase,
    private val syncLocalService: SyncLocalService,
    private val settingsRepository: SettingsRepository,
    private val contextDao: ContextDao,
    private val goalDao: GoalDao,
    private val listItemDao: ListItemDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val attachmentDao: AttachmentDao,
    private val recentItemDao: RecentItemDao,
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val chatDao: ChatDao,
    private val reminderDao: ReminderDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiInsightDao: AiInsightDao,
) {
    private val TAG = "SyncFileService"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    /**
     * Exports full backup to Downloads folder
     * @return Result with success message or error
     */
    suspend fun exportFullBackupToFile(): Result<String> = try {
        val json = createFullBackupJsonString()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = "forward_full_backup_$ts.json"
        saveFileToDownloads(name, json)
        Result.success("Файл збережено")
    } catch (e: Exception) {
        Log.e(TAG, "Error exporting full backup", e)
        Result.failure(e)
    }

    /**
     * Creates JSON string with complete database backup
     * @return JSON string with all data
     */
    suspend fun createFullBackupJsonString(): String {
        val local = syncLocalService.loadLocalDatabaseContent()

        val recentProjectEntries = recentItemDao.getAll().map {
            RecentProjectEntry(contextId = it.target, timestamp = it.lastAccessed)
        }

        val fullContent = local.copy(
            recentProjectEntries = recentProjectEntries,
            dayPlans = dayPlanDao.getAllPlansSync(),
            dayTasks = dayTaskDao.getAllTasksSync(),
            dailyMetrics = dailyMetricDao.getAllMetricsSync(),
            conversations = chatDao.getAllConversationsSync(),
            chatMessages = chatDao.getAllMessagesSync(),
            reminders = reminderDao.getAllRemindersSync(),
            tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
            aiInsights = aiInsightDao.getAllSync(),
        )

        val settingsMap = settingsRepository.getPreferencesSnapshot().asMap()
            .mapKeys { it.key.name }.mapValues { it.value.toString() }

        return gson.toJson(
            FullAppBackup(
                database = fullContent,
                settings = SettingsContent(settingsMap),
            ),
        )
    }

    /**
     * Imports full backup from file URI
     * Clears all tables and restores data from backup
     * @param uri File URI to import from
     * @return Result with success message or error
     */
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> = try {
        val json = readTextFromUri(uri) ?: throw Exception("Empty file")
        val backupData = gson.fromJson(json, FullAppBackup::class.java)
        val backup = backupData.database

        appDatabase.withTransaction {
            val dbSystemProjects = contextDao.getAll().filter { SystemContexts.isSystem(ContextId(it.id)) }
            val existingSystemByKey = dbSystemProjects.associateBy { it.systemKey!! }
            val contextIdMap = mutableMapOf<String, String>()

            val cleanedProjects = backup.projects.map { incoming ->
                val normalized = SyncMapper.normalizeProject(incoming)
                val existing = normalized.systemKey?.let { existingSystemByKey[it] }

                if (existing != null) {
                    if (normalized.id != existing.id) contextIdMap[normalized.id] = existing.id
                    if ((normalized.updatedAt ?: 0) > (existing.updatedAt ?: 0)) {
                        normalized.copy(id = existing.id)
                    } else {
                        existing
                    }
                } else {
                    normalized
                }
            }

            syncLocalService.clearAllTables()

            goalDao.insertGoals(backup.goals)
            contextDao.insertContexts(cleanedProjects)

            val cleanedListItems = backup.backlogItems.map {
                it.copy(
                    contextId = contextIdMap[it.contextId] ?: it.contextId,
                    entityId = if (it.itemType == BacklogItemTypeValues.SUBLIST) {
                        contextIdMap[it.entityId] ?: it.entityId
                    } else {
                        it.entityId
                    },
                )
            }
            listItemDao.insertItems(cleanedListItems)

            noteDocumentDao.insertAllDocuments(
                backup.documents.map {
                    it.copy(contextId = contextIdMap[it.contextId] ?: it.contextId)
                },
            )
            attachmentDao.insertAttachments(
                backup.attachments.map {
                    it.copy(
                        ownerContextId = it.ownerContextId?.let { cid ->
                            contextIdMap[cid] ?: cid
                        },
                    )
                },
            )

            backupData.settings?.settings?.let { settingsRepository.restoreFromMap(it) }
            runPostBackupMigration()
        }
        Result.success("Success")
    } catch (e: Exception) {
        Log.e(TAG, "Import error", e)
        Result.failure(e)
    }

    /**
     * Parses backup file and returns structured data
     * @param uri File URI to parse
     * @return Result with parsed FullAppBackup or error
     */
    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> = withContext(Dispatchers.IO) {
        try {
            val jsonString = readTextFromUri(uri)
            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Backup file is empty or could not be read."))
            }
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)
            Log.d(TAG, "Parsed backup version: ${backupData.backupSchemaVersion}, projects: ${backupData.database.projects.size}")
            Result.success(backupData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse backup file", e)
            Result.failure(e)
        }
    }

    /**
     * Reads text content from URI
     * @param uri URI to read from
     * @return Text content or null if failed
     */
    fun readTextFromUri(uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    /**
     * Saves file to Downloads/ForwardApp folder
     * @param name File name
     * @param json JSON content to save
     */
    fun saveFileToDownloads(name: String, json: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os -> os.write(json.toByteArray()) }
        }
    }

    /**
     * Runs post-backup migration for special projects
     */
    private suspend fun runPostBackupMigration() {
        Log.d(TAG, "Starting post-backup migration")
        val db = appDatabase.openHelper.writableDatabase
        com.romankozak.forwardappmobile.data.database.migrateSpecialProjects(db)
    }
}