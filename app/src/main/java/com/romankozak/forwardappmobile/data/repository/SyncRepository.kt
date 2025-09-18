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
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentProjectDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.DesktopBackupData
import com.romankozak.forwardappmobile.data.sync.DesktopBackupFile
import com.romankozak.forwardappmobile.data.sync.DesktopGoal
import com.romankozak.forwardappmobile.data.sync.DesktopGoalInstance
import com.romankozak.forwardappmobile.data.sync.DesktopGoalList
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
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
    val goalLists: Map<String, GoalList>,
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
        }}

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

    suspend fun createBackupJsonString(): String {
        val lists = projectDao.getAll()
        val goals = goalDao.getAll()
        val listItems = listItemDao.getAll()

        val desktopGoals =
            goals.associate {
                it.id to
                        DesktopGoal(
                            id = it.id,
                            text = it.text,
                            completed = it.completed,
                            createdAt = longToDateString(it.createdAt)!!,
                            updatedAt = longToDateString(it.updatedAt),
                            associatedListIds =
                                it.relatedLinks
                                    ?.filter { l -> l.type == LinkType.GOAL_LIST }
                                    ?.map { l -> l.target },
                            description = it.description,
                            tags = it.tags,
                            valueImportance = it.valueImportance,
                            valueImpact = it.valueImpact,
                            effort = it.effort,
                            cost = it.cost,
                            risk = it.risk,
                            weightEffort = it.weightEffort,
                            weightCost = it.weightCost,
                            weightRisk = it.weightRisk,
                            rawScore = it.rawScore,
                            displayScore = it.displayScore,
                            scoringStatus = it.scoringStatus,
                        )
            }

        val goalListItems = listItems.filter { it.itemType == ListItemType.GOAL }
        val desktopInstances =
            goalListItems.associate {
                it.id to DesktopGoalInstance(id = it.id, goalId = it.entityId)
            }

        val desktopLists =
            lists.associate { list ->
                val listInstances = goalListItems.filter { it.listId == list.id }.sortedBy { it.order }
                list.id to
                        DesktopGoalList(
                            id = list.id,
                            name = list.name,
                            parentId = list.parentId,
                            description = list.description,
                            createdAt = longToDateString(list.createdAt)!!,
                            updatedAt = longToDateString(list.updatedAt),
                            itemInstanceIds = listInstances.map { it.id },
                            isExpanded = list.isExpanded,
                            order = list.order,
                            tags = list.tags,
                        )
            }

        val desktopBackupData =
            DesktopBackupData(
                goals = desktopGoals,
                goalLists = desktopLists,
                goalInstances = desktopInstances,
                notes = emptyMap(),
            )

        val desktopBackupFile =
            DesktopBackupFile(
                version = 4,
                exportedAt = longToDateString(System.currentTimeMillis())!!,
                data = desktopBackupData,
            )

        return gson.toJson(desktopBackupFile)
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

    private suspend fun createFullBackupJsonString(): String {
        val databaseContent =
            DatabaseContent(
                goals = goalDao.getAll(),
                goalLists = projectDao.getAll(),
                listItems = listItemDao.getAll(),
                activityRecords = activityRecordDao.getAllRecordsStream().first(),
                recentListEntries = recentProjectDao.getAllEntries(),
                linkItemEntities = linkItemDao.getAllEntities(),
                inboxRecords = inboxRecordDao.getAll(),
                projectExecutionLogs = projectManagementDao.getAllLogs(),
            )

        val settingsSnapshot: Preferences = settingsRepository.getPreferencesSnapshot()
        val settingsMap = settingsSnapshot.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
        val settingsContent = SettingsContent(settings = settingsMap)

        val fullBackup =
            FullAppBackup(
                database = databaseContent,
                settings = settingsContent,
            )
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
            val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
            Log.d(IMPORT_TAG, "JSON успішно розібрано.")

            Log.d(IMPORT_TAG, "Починаємо очищення даних для сумісності...")
            val cleanedGoalLists =
                backup.database.goalLists.map { listFromBackup ->
                    listFromBackup.copy(
                        // Існуючі перевірки для сумісності
                        defaultViewModeName = listFromBackup.defaultViewModeName ?: ProjectViewMode.BACKLOG.name,
                        isProjectManagementEnabled = listFromBackup.isProjectManagementEnabled ?: false,
                        projectStatus = listFromBackup.projectStatus ?: ProjectStatus.NO_PLAN,
                        projectStatusText = listFromBackup.projectStatusText ?: "",
                        projectLogLevel = listFromBackup.projectLogLevel ?: ProjectLogLevel.NORMAL,
                        totalTimeSpentMinutes = listFromBackup.totalTimeSpentMinutes ?: 0,

                        // ВИПРАВЛЕННЯ: Додано перевірки для нових полів, щоб уникнути NPE
                        scoringStatus = listFromBackup.scoringStatus ?: ScoringStatus.NOT_ASSESSED,
                        // Примітивні типи (Float, Int) автоматично отримають 0, якщо їх немає в JSON,
                        // але для Enum (ScoringStatus) Gson поверне null, що спричиняло помилку.
                        // Явно задаємо значення за замовчуванням.
                        valueImportance = listFromBackup.valueImportance,
                        valueImpact = listFromBackup.valueImpact,
                        effort = listFromBackup.effort,
                        cost = listFromBackup.cost,
                        risk = listFromBackup.risk,
                        weightEffort = listFromBackup.weightEffort,
                        weightCost = listFromBackup.weightCost,
                        weightRisk = listFromBackup.weightRisk,
                        rawScore = listFromBackup.rawScore,
                        displayScore = listFromBackup.displayScore
                    )
                }
            Log.d(IMPORT_TAG, "Очищення даних завершено.")

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
                goalDao.insertGoals(backup.database.goals)
                projectDao.insertLists(cleanedGoalLists)
                listItemDao.insertItems(backup.database.listItems)

                backup.database.activityRecords?.let { activityRecordDao.insertAll(it) }
                backup.database.recentListEntries?.let { recentProjectDao.insertAll(it) }
                backup.database.linkItemEntities?.let { linkItemDao.insertAll(it) }
                backup.database.inboxRecords?.let { inboxRecordDao.insertAll(it) }
                backup.database.projectExecutionLogs?.let { projectManagementDao.insertAllLogs(it) }

                Log.d(IMPORT_TAG, "Вставка даних завершена.")
            }

            Log.d(IMPORT_TAG, "Відновлення налаштувань...")
            settingsRepository.restoreFromMap(backup.settings.settings)
            Log.d(IMPORT_TAG, "Налаштування відновлено.")

            Log.i(IMPORT_TAG, "Імпорт бекапу успішно завершено.")
            return Result.success("Backup imported successfully!")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Під час імпорту сталася критична помилка.", e)
            return Result.failure(e)
        }
    }

    // ... (решта файлу без змін)
    private fun dateStringToLong(dateString: String?): Long? {
        if (dateString == null) return null
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            format.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun longToDateString(time: Long?): String? {
        if (time == null) return null
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(time))
    }

    private fun transformImportedData(data: DesktopBackupData): LocalSyncState {
        val newGoals =
            (data.goals ?: emptyMap()).mapValues { (_, goal) ->
                Goal(
                    id = goal.id,
                    text = goal.text,
                    completed = goal.completed,
                    createdAt = dateStringToLong(goal.createdAt) ?: 0L,
                    updatedAt = dateStringToLong(goal.updatedAt),
                    description = goal.description,
                    tags = goal.tags,
                    relatedLinks =
                        goal.associatedListIds?.map { listId ->
                            RelatedLink(type = LinkType.GOAL_LIST, target = listId, displayName = data.goalLists?.get(listId)?.name)
                        },
                    valueImportance = goal.valueImportance,
                    valueImpact = goal.valueImpact,
                    effort = goal.effort,
                    cost = goal.cost,
                    risk = goal.risk,
                    weightEffort = goal.weightEffort,
                    weightCost = goal.weightCost,
                    weightRisk = goal.weightRisk,
                    rawScore = goal.rawScore,
                    displayScore = goal.displayScore,
                    scoringStatus = goal.scoringStatus,
                )
            }

        val newGoalLists =
            (data.goalLists ?: emptyMap()).mapValues { (_, list) ->
                GoalList(
                    id = list.id,
                    name = list.name,
                    parentId = list.parentId,
                    description = list.description,
                    createdAt = dateStringToLong(list.createdAt) ?: 0L,
                    updatedAt = dateStringToLong(list.updatedAt),
                    isExpanded = list.isExpanded ?: true,
                    order = list.order ?: 0L,
                    tags = list.tags,
                )
            }

        val newListItems = mutableMapOf<String, ListItem>()
        (data.goalLists ?: emptyMap()).values.forEach { list ->
            list.itemInstanceIds.forEachIndexed { index, instanceId ->
                data.goalInstances?.get(instanceId)?.let { originalInstance ->
                    newListItems[instanceId] =
                        ListItem(
                            id = originalInstance.id,
                            listId = list.id,
                            itemType = ListItemType.GOAL,
                            entityId = originalInstance.goalId,
                            order = index.toLong(),
                        )
                }
            }
        }

        return LocalSyncState(goals = newGoals, goalLists = newGoalLists, listItems = newListItems)
    }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        try {
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
        }
    }

    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        val changesByType = approvedChanges.groupBy { it.type }

        changesByType[ChangeType.Delete]?.forEach { change ->
            when (change.entityType) {
                "Привʼязка" -> listItemDao.deleteItemsByIds(listOf(change.id))
                "Список" -> projectDao.deleteListById(change.id)
                "Ціль" -> goalDao.deleteGoalById(change.id)
            }
        }

        changesByType[ChangeType.Update]?.forEach { change ->
            when (change.entityType) {
                "Список" -> projectDao.update(change.entity as GoalList)
                "Ціль" -> goalDao.updateGoal(change.entity as Goal)
            }
        }

        val addsAndMoves = (changesByType[ChangeType.Add] ?: emptyList()) + (changesByType[ChangeType.Move] ?: emptyList())
        addsAndMoves.forEach { change ->
            when (change.entityType) {
                "Список" -> projectDao.insert(change.entity as GoalList)
                "Ціль" -> goalDao.insertGoal(change.entity as Goal)
                "Привʼязка" -> listItemDao.insertItem(change.entity as ListItem)
            }
        }
    }
}