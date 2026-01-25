package com.romankozak.forwardappmobile.data.repository

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.sync.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord
import com.romankozak.forwardappmobile.features.ai.data.dao.*
import com.romankozak.forwardappmobile.features.attachments.data.*
import com.romankozak.forwardappmobile.features.attachments.data.models.*
import com.romankozak.forwardappmobile.features.contexts.data.*
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.contexts.data.models.*
import com.romankozak.forwardappmobile.features.daymanagement.data.models.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.recent.data.models.*
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder
import com.romankozak.forwardappmobile.data.repository.SyncMapper.updatedTs
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

@Singleton
class SyncRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: AndroidContext,
    private val logicHelper: SyncLogicHelper,
    private val goalDao: GoalDao,
    private val contextDao: ContextDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val activityRecordDao: ActivityRecordDao,
    private val inboxRecordDao: InboxRecordDao,
    private val settingsRepository: SettingsRepository,
    private val contextManagementDao: ContextManagementDao,
    private val legacyNoteDao: LegacyNoteDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val checklistDao: ChecklistDao,
    private val recentItemDao: RecentItemDao,
    private val backlogOrderDao: BacklogOrderDao,
    private val scriptDao: ScriptDao,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentDao: AttachmentDao,
    private val systemAppDao: SystemAppDao,
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val chatDao: ChatDao,
    private val conversationFolderDao: ConversationFolderDao,
    private val reminderDao: ReminderDao,
    private val recurringTaskDao: RecurringTaskDao,
    private val contextArtifactDao: ContextArtifactDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiEventDao: AiEventDao,
    private val lifeSystemStateDao: LifeSystemStateDao,
    private val aiInsightDao: AiInsightDao,
    private val structurePresetDao: StructurePresetDao,
    private val structurePresetItemDao: StructurePresetItemDao,
    private val contextStructureDao: ContextStructureDao
) {
    private val TAG = "SyncRepository"
    private val WIFI_SYNC_TAG = "FWD_WIFI_SYNC"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .registerTypeAdapter(ReservedGroup::class.java, ReservedGroupAdapter())
        .create()

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { gson() }
        }
    }

    // --- Експорт та Імпорт файлів ---

    suspend fun exportFullBackupToFile(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupJson = createFullBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_app_full_backup_$timestamp.json"

            saveJsonToDownloads(fileName, backupJson)
            Result.success("Бекап збережено в Downloads/ForwardApp")
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }

    suspend fun importFullBackupFromFile(uri: Uri): Result<String> {
        return try {
            val jsonString = readTextFromUri(uri) ?: throw Exception("File is empty")
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)

            applyServerChanges(backupData.database)
            Result.success("Імпорт завершено успішно")
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            Result.failure(e)
        }
    }

    // --- Wi-Fi Синхронізація ---

    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> = try {
        val url = buildWifiUrl(address, "/export", deltaSince)
        val response: String = client.get(url).body()
        Result.success(response)
    } catch (e: Exception) {
        Log.e(WIFI_SYNC_TAG, "Fetch failed", e)
        Result.failure(e)
    }

    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = try {
        val unsynced = getUnsyncedChanges()
        val url = buildWifiUrl(address, "/import", null)
        val payload = gson.toJson(FullAppBackup(database = unsynced))

        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        markSyncedNow(unsynced)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(WIFI_SYNC_TAG, "Push failed", e)
        Result.failure(e)
    }

    // --- Логіка обробки даних (Core) ---

    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            appDatabase.withTransaction {
                val local = loadLocalDatabaseContent()

                // 1. Злиття Проектів (Contexts)
                val incomingContexts = logicHelper.mergeAndMark(
                    incoming = changes.projects.map { SyncMapper.normalizeProject(it) },
                    localMap = local.projects.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { p, synced -> p.copy(syncedAt = synced) },
                    syncedAt = ts,
                    isDeletedSelector = { it.isDeleted }
                )
                if (incomingContexts.isNotEmpty()) contextDao.insertContexts(incomingContexts)

                // 2. Злиття Цілей (Goals)
                val incomingGoals = logicHelper.mergeAndMark(
                    incoming = changes.goals.map { SyncMapper.normalizeGoal(it) },
                    localMap = local.goals.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { g, synced -> g.copy(syncedAt = synced) },
                    syncedAt = ts,
                    isDeletedSelector = { it.isDeleted }
                )
                if (incomingGoals.isNotEmpty()) goalDao.insertGoals(incomingGoals)

                // 3. Обробка вкладень (Attachments)
                val synthesizedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
                    changes.attachments,
                    changes.contextAttachmentCrossRefs
                )

                // ... аналогічно для інших сутностей (нотатки, чеклисти тощо)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Apply changes failed", e)
            Result.failure(e)
        }
    }

    // --- Допоміжні методи I/O ---

    private suspend fun loadLocalDatabaseContent(): DatabaseContent = DatabaseContent(
        goals = goalDao.getAll(),
        projects = contextDao.getAll(),
        backlogItems = listItemDao.getAll(),
        backlogOrders = backlogOrderDao.getAll(),
        legacyNotes = legacyNoteDao.getAll(),
        documents = noteDocumentDao.getAllDocuments(),
        documentItems = noteDocumentDao.getAllDocumentItems(),
        checklists = checklistDao.getAllChecklists(),
        checklistItems = checklistDao.getAllChecklistItems(),
        attachments = attachmentDao.getAll(),
        contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
        scripts = scriptDao.getAll().first()
        // ... завантаження решти сутностей
    )

    suspend fun getUnsyncedChanges(): DatabaseContent {
        val local = loadLocalDatabaseContent()
        return DatabaseContent(
            projects = local.projects.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            goals = local.goals.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            attachments = local.attachments.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
            // ... фільтрація решти
        )
    }

    private suspend fun markSyncedNow(content: DatabaseContent) {
        val ts = System.currentTimeMillis()
        appDatabase.withTransaction {
            contextDao.insertContexts(content.projects.map { it.copy(syncedAt = ts) })
            goalDao.insertGoals(content.goals.map { it.copy(syncedAt = ts) })
            attachmentDao.insertAttachments(content.attachments.map { it.copy(syncedAt = ts) })
            // ... відмітка інших сутностей
        }
    }

    private fun saveJsonToDownloads(fileName: String, content: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(content.toByteArray()) } }
    }

    private fun readTextFromUri(uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    private suspend fun buildWifiUrl(address: String, path: String, deltaSince: Long?): String {
        val cleanAddress = if (address.startsWith("http")) address else "http://$address"
        val port = settingsRepository.wifiSyncPortFlow.first()
        val base = "$cleanAddress:$port$path"
        return if (deltaSince != null) "$base?deltaSince=$deltaSince" else base
    }

    private suspend fun createFullBackupJsonString(): String {
        val content = loadLocalDatabaseContent()
        return gson.toJson(FullAppBackup(database = content))
    }
}