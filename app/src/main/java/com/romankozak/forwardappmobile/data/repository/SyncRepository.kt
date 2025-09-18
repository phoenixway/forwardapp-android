package com.romankozak.forwardappmobile.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentProjectDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.DatabaseContent
import com.romankozak.forwardappmobile.data.database.models.FullAppBackup
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectLogLevel
import com.romankozak.forwardappmobile.data.database.models.ProjectStatus
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.data.sync.DesktopBackupData
import com.romankozak.forwardappmobile.data.sync.DesktopBackupFile
import com.romankozak.forwardappmobile.data.sync.SettingsContent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton


enum class ChangeType {
    Add,
    Update,
    Delete,
    Move,
}

data class SyncChange(
    val type: ChangeType,
    val entityType: String,
    val id: String,
    val description: String,
    val longDescription: String? = null,
    val entity: Any,
)

data class SyncReport(
    val changes: List<SyncChange>,
)

private data class LocalSyncState(
    val goals: Map<String, Goal>,
    val goalLists: Map<String, Project>,
    val listItems: Map<String, ListItem>,
)

@Singleton
class SyncRepository
@Inject
constructor(
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context,
    private val goalDao: GoalDao,
    private val projectDao: ProjectDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val activityRecordDao: ActivityRecordDao,
    private val recentProjectDao: RecentProjectDao,
    private val inboxRecordDao: InboxRecordDao,
    private val settingsRepository: SettingsRepository,
    private val projectManagementDao: ProjectManagementDao,
) {
    private val TAG = "SyncRepository"
    private val gson = Gson()
    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { gson() }
        }
    }

    suspend fun exportFullBackupToFile(): Result<String> =
        try {
            val backupJson = createFullBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_app_full_backup_$timestamp.json"

            val contentResolver = context.contentResolver
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
                    }
                }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { it.write(backupJson.toByteArray()) }
                Result.success("Повний бекап успішно збережено до Downloads/ForwardApp.")
            } else {
                Result.failure(Exception("Не вдалося створити файл для бекапу."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка повного експорту", e)
            Result.failure(e)
        }

    suspend fun createFullBackupJsonString(): String {
        val databaseContent =
            DatabaseContent(
                goals = goalDao.getAll(),
                projects = projectDao.getAll(),
                listItems = listItemDao.getAll(),
                activityRecords = activityRecordDao.getAllRecordsStream().first(),
                recentProjectEntries = recentProjectDao.getAllEntries(),
                linkItemEntities = linkItemDao.getAllEntities(),
                inboxRecords = inboxRecordDao.getAll(),
                projectExecutionLogs = projectManagementDao.getAllLogs(),
            )
        val fullBackup = FullAppBackup(database = databaseContent)
        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFile(uri: Uri): Result<String> {
        val IMPORT_TAG = "SyncRepository_IMPORT"
        try {
            Log.d(IMPORT_TAG, "Починаємо імпорт з URI: $uri")
            val jsonString =
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    .use { it?.readText() }

            if (jsonString.isNullOrBlank()) {
                Log.e(IMPORT_TAG, "Помилка: файл бекапу порожній або не вдалося прочитати.")
                return Result.failure(Exception("Backup file is empty or could not be read."))
            }
            Log.d(IMPORT_TAG, "Файл успішно прочитано.")

            Log.d(IMPORT_TAG, "Починаємо розбір JSON в об'єкт FullAppBackup...")
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)
            val backup = backupData.database
            Log.d(IMPORT_TAG, "JSON успішно розібрано.")

            Log.d(IMPORT_TAG, "Починаємо очищення даних для сумісності...")
            val cleanedProjects =
                backup.projects.map { projectFromBackup ->
                    projectFromBackup.copy(
                        defaultViewModeName = projectFromBackup.defaultViewModeName ?: ProjectViewMode.BACKLOG.name,
                        isProjectManagementEnabled = projectFromBackup.isProjectManagementEnabled ?: false,
                        projectStatus = projectFromBackup.projectStatus ?: ProjectStatus.NO_PLAN,
                        projectStatusText = projectFromBackup.projectStatusText ?: "",
                        projectLogLevel = projectFromBackup.projectLogLevel ?: ProjectLogLevel.NORMAL,
                        totalTimeSpentMinutes = projectFromBackup.totalTimeSpentMinutes ?: 0,
                        scoringStatus = projectFromBackup.scoringStatus ?: ScoringStatus.NOT_ASSESSED,
                        valueImportance = projectFromBackup.valueImportance,
                        valueImpact = projectFromBackup.valueImpact,
                        effort = projectFromBackup.effort,
                        cost = projectFromBackup.cost,
                        risk = projectFromBackup.risk,
                        weightEffort = projectFromBackup.weightEffort,
                        weightCost = projectFromBackup.weightCost,
                        weightRisk = projectFromBackup.weightRisk,
                        rawScore = projectFromBackup.rawScore,
                        displayScore = projectFromBackup.displayScore
                    )
                }
            Log.d(IMPORT_TAG, "Очищення даних Project завершено.")

            val cleanedListItems = backup.listItems.mapNotNull { item ->
                if (item.id.isBlank() || item.projectId.isBlank() || item.entityId.isBlank()) {
                    Log.w(IMPORT_TAG, "Skipping invalid ListItem due to blank ID(s): $item")
                    null
                } else {
                    item
                }
            }
            Log.d(IMPORT_TAG, "Очищення ListItem завершено. Original: ${backup.listItems.size}, Cleaned: ${cleanedListItems.size}")

            appDatabase.withTransaction {
                Log.d(IMPORT_TAG, "Початок транзакції в БД. Очищення старих даних...")
                projectManagementDao.deleteAllLogs()
                inboxRecordDao.deleteAll()
                linkItemDao.deleteAll()
                recentProjectDao.deleteAll()
                activityRecordDao.clearAll()
                listItemDao.deleteAll()
                projectDao.deleteAll()
                goalDao.deleteAll()
                Log.d(IMPORT_TAG, "Всі таблиці очищено.")

                Log.d(IMPORT_TAG, "Вставка нових даних...")
                goalDao.insertGoals(backup.goals)
                projectDao.insertProjects(cleanedProjects)
                listItemDao.insertItems(cleanedListItems)

                backup.activityRecords?.let { activityRecordDao.insertAll(it) }
                backup.recentProjectEntries?.let { recentProjectDao.insertAll(it) }
                backup.linkItemEntities?.let { linkItemDao.insertAll(it) }
                backup.inboxRecords?.let { inboxRecordDao.insertAll(it) }
                backup.projectExecutionLogs?.let { projectManagementDao.insertAllLogs(it) }

                Log.d(IMPORT_TAG, "Вставка даних завершена.")
            }

            Log.i(IMPORT_TAG, "Імпорт бекапу успішно завершено.")
            return Result.success("Backup imported successfully!")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Під час імпорту сталася критична помилка.", e)
            return Result.failure(e)
        }
    }

    suspend fun fetchBackupFromWifi(address: String): Result<String> =
        try {
            var cleanAddress = address.trim()
            if (!cleanAddress.startsWith("http://") && !cleanAddress.startsWith("https://")) {
                cleanAddress = "http://$cleanAddress"
            }
            val uri = Uri.parse(cleanAddress)
            val hostAndPort = "${uri.host}:${if (uri.port != -1) uri.port else 8080}"
            val fullUrl = "http://$hostAndPort/export"
            Log.d(TAG, "Fetching from: $fullUrl")
            val response: String = client.get(fullUrl).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from WiFi", e)
            Result.failure(e)
        }

    suspend fun createSyncReport(jsonString: String): SyncReport {
/*        try {
            val backupFile = gson.fromJson(jsonString, DesktopBackupFile::class.java)
            val remoteData = backupFile.data ?: throw IllegalArgumentException("Backup data is missing.")

            val remoteState = transformImportedData(remoteData)
            val localLists = projectDao.getAll().associateBy { it.id }
            val localGoals = goalDao.getAll().associateBy { it.id }
            val localItems = listItemDao.getAll().associateBy { it.id }

            val changes = mutableListOf<SyncChange>()

            (localLists.keys + remoteState.goalLists.keys).distinct().forEach { id ->
                val local = localLists[id]
                val remote = remoteState.goalLists[id]
                when {
                    remote != null && local == null ->
                        changes.add(
                            SyncChange(ChangeType.Add, "Список", id, remote.name, entity = remote),
                        )
                    remote == null && local != null ->
                        changes.add(
                            SyncChange(ChangeType.Delete, "Список", id, local.name, entity = local),
                        )
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) ->
                        changes.add(
                            SyncChange(ChangeType.Update, "Список", id, remote.name, entity = remote),
                        )
                }
            }

            (localGoals.keys + remoteState.goals.keys).distinct().forEach { id ->
                val local = localGoals[id]
                val remote = remoteState.goals[id]
                when {
                    remote != null && local == null -> changes.add(SyncChange(ChangeType.Add, "Ціль", id, remote.text, entity = remote))
                    remote == null && local != null ->
                        changes.add(
                            SyncChange(ChangeType.Delete, "Ціль", id, local.text, entity = local),
                        )
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) ->
                        changes.add(
                            SyncChange(ChangeType.Update, "Ціль", id, remote.text, entity = remote),
                        )
                }
            }

            (localItems.keys + remoteState.listItems.keys).distinct().forEach { id ->
                val local = localItems[id]
                val remote = remoteState.listItems[id]
                when {
                    remote != null && local == null -> {
                        val desc = "Прив'язка до списку \"${remoteState.goalLists[remote.listId]?.name ?: "?"}\""
                        changes.add(SyncChange(ChangeType.Add, "Привʼязка", id, desc, entity = remote))
                    }
                    remote == null && local != null -> {
                        val desc = "Прив'язка зі списку \"${localLists[local.listId]?.name ?: "?"}\""
                        changes.add(SyncChange(ChangeType.Delete, "Привʼязка", id, desc, entity = local))
                    }
                    remote != null && local != null && (remote.order != local.order || remote.listId != local.listId) -> {
                        val goalText = remoteState.goals[remote.entityId]?.text ?: "?"
                        val fromList = localLists[local.listId]?.name ?: "?"
                        val toList = remoteState.goalLists[remote.listId]?.name ?: "?"
                        val desc = "Переміщення прив'язки \"$goalText\""
                        val longDesc = "Прив'язку \"$goalText\" переміщено з \"$fromList\" (поз. ${local.order}) у \"$toList\" (поз. ${remote.order})."
                        changes.add(SyncChange(ChangeType.Move, "Привʼязка", id, desc, longDesc, entity = remote))
                    }
                }
            }

            return SyncReport(changes)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sync report", e)
            throw IllegalStateException("Error parsing data: ${e.message}", e)
        }*/
        val changes = mutableListOf<SyncChange>()
        return SyncReport(changes)
    }

}