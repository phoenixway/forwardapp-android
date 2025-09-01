// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/sync/SyncRepository.kt ---
package com.romankozak.forwardappmobile.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.sync.DesktopBackupData
import com.romankozak.forwardappmobile.data.sync.DesktopBackupFile
import com.romankozak.forwardappmobile.data.sync.DesktopGoal
import com.romankozak.forwardappmobile.data.sync.DesktopGoalInstance
import com.romankozak.forwardappmobile.data.sync.DesktopGoalList
import com.romankozak.forwardappmobile.data.sync.DesktopNote
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.gson.gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import com.romankozak.forwardappmobile.data.dao.* // Імпортуємо всі DAO
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.SettingsContent
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction // Додайте цей імпорт

enum class ChangeType {
    Add, Update, Delete, Move
}

data class SyncChange(
    val type: ChangeType,
    val entityType: String, // "Список", "Ціль", "Нотатка", "Привʼязка"
    val id: String,
    val description: String,
    val longDescription: String? = null,
    val entity: Any
)

data class SyncReport(
    val changes: List<SyncChange>
)

private data class LocalSyncState(
    val goals: Map<String, Goal>,
    val goalLists: Map<String, GoalList>,
    val notes: Map<String, Note>,
    val listItems: Map<String, ListItem>
)

@Singleton
class SyncRepository @Inject constructor(
    private val goalRepository: GoalRepository, // Можна залишити, якщо зручно
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context,
    // ✨ ДОДАНО: Прямі залежності для повноти
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    private val noteDao: NoteDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val activityRecordDao: ActivityRecordDao,
    private val recentListDao: RecentListDao,
    private val settingsRepository: SettingsRepository

) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { gson() }
    }

    suspend fun fetchBackupFromWifi(address: String): Result<String> {
        return try {
            var cleanAddress = address.trim()
            if (!cleanAddress.startsWith("http://") && !cleanAddress.startsWith("https://")) {
                cleanAddress = "http://$cleanAddress"
            }
            val uri = Uri.parse(cleanAddress)
            val hostAndPort = "${uri.host}:${if (uri.port != -1) uri.port else 8080}"
            val fullUrl = "http://$hostAndPort/export"
            Log.d("SyncRepository", "Fetching from: $fullUrl")
            val response: String = client.get(fullUrl).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error fetching from WiFi", e)
            Result.failure(e)
        }
    }

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
        val newGoals = (data.goals ?: emptyMap()).mapValues { (_, goal) ->
            Goal(
                id = goal.id, text = goal.text, completed = goal.completed,
                createdAt = dateStringToLong(goal.createdAt) ?: 0L,
                updatedAt = dateStringToLong(goal.updatedAt),
                description = goal.description, tags = goal.tags,
                relatedLinks = goal.associatedListIds?.map { listId ->
                    RelatedLink(type = LinkType.GOAL_LIST, target = listId, displayName = data.goalLists?.get(listId)?.name)
                },
                valueImportance = goal.valueImportance, valueImpact = goal.valueImpact,
                effort = goal.effort, cost = goal.cost, risk = goal.risk,
                weightEffort = goal.weightEffort, weightCost = goal.weightCost,
                weightRisk = goal.weightRisk, rawScore = goal.rawScore,
                displayScore = goal.displayScore, scoringStatus = goal.scoringStatus
            )
        }

        val newGoalLists = (data.goalLists ?: emptyMap()).mapValues { (_, list) ->
            GoalList(
                id = list.id, name = list.name, parentId = list.parentId,
                description = list.description, createdAt = dateStringToLong(list.createdAt) ?: 0L,
                updatedAt = dateStringToLong(list.updatedAt), isExpanded = list.isExpanded ?: true,
                order = list.order ?: 0L, tags = list.tags
            )
        }

        val newNotes = (data.notes ?: emptyMap()).mapValues { (_, note) ->
            Note(
                id = note.id, title = note.title, content = note.content,
                createdAt = dateStringToLong(note.createdAt) ?: 0L,
                updatedAt = dateStringToLong(note.updatedAt)
            )
        }

        val newListItems = mutableMapOf<String, ListItem>()
        (data.goalLists ?: emptyMap()).values.forEach { list ->
            list.itemInstanceIds.forEachIndexed { index, instanceId ->
                data.goalInstances?.get(instanceId)?.let { originalInstance ->
                    newListItems[instanceId] = ListItem(
                        id = originalInstance.id,
                        listId = list.id,
                        itemType = ListItemType.GOAL,
                        entityId = originalInstance.goalId,
                        order = index.toLong()
                    )
                }
            }
        }

        return LocalSyncState(goals = newGoals, goalLists = newGoalLists, notes = newNotes, listItems = newListItems)
    }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        try {
            val backupFile = Gson().fromJson(jsonString, DesktopBackupFile::class.java)
            val remoteData = backupFile.data ?: throw IllegalArgumentException("Backup data is missing.")

            val remoteState = transformImportedData(remoteData)
            val localLists = goalRepository.getAllGoalLists().associateBy { it.id }
            val localGoals = goalRepository.getAllGoals().associateBy { it.id }
            val localNotes = goalRepository.getAllNotes().associateBy { it.id }
            val localItems = goalRepository.getAllListItems().associateBy { it.id }

            val changes = mutableListOf<SyncChange>()

            // Порівняння списків
            (localLists.keys + remoteState.goalLists.keys).distinct().forEach { id ->
                val local = localLists[id]
                val remote = remoteState.goalLists[id]
                when {
                    remote != null && local == null -> changes.add(SyncChange(ChangeType.Add, "Список", id, remote.name, entity = remote))
                    remote == null && local != null -> changes.add(SyncChange(ChangeType.Delete, "Список", id, local.name, entity = local))
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) -> changes.add(SyncChange(ChangeType.Update, "Список", id, remote.name, entity = remote))
                }
            }

            // Порівняння цілей
            (localGoals.keys + remoteState.goals.keys).distinct().forEach { id ->
                val local = localGoals[id]
                val remote = remoteState.goals[id]
                when {
                    remote != null && local == null -> changes.add(SyncChange(ChangeType.Add, "Ціль", id, remote.text, entity = remote))
                    remote == null && local != null -> changes.add(SyncChange(ChangeType.Delete, "Ціль", id, local.text, entity = local))
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) -> changes.add(SyncChange(ChangeType.Update, "Ціль", id, remote.text, entity = remote))
                }
            }

            // Порівняння нотаток
            (localNotes.keys + remoteState.notes.keys).distinct().forEach { id ->
                val local = localNotes[id]
                val remote = remoteState.notes[id]
                when {
                    remote != null && local == null -> changes.add(SyncChange(ChangeType.Add, "Нотатка", id, remote.content.take(50), entity = remote))
                    remote == null && local != null -> changes.add(SyncChange(ChangeType.Delete, "Нотатка", id, local.content.take(50), entity = local))
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) -> changes.add(SyncChange(ChangeType.Update, "Нотатка", id, remote.content.take(50), entity = remote))
                }
            }

            // Порівняння прив'язок (ListItem)
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
            Log.e("SyncRepository", "Error creating sync report", e)
            throw IllegalStateException("Error parsing data: ${e.message}", e)
        }
    }

    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        val changesByType = approvedChanges.groupBy { it.type }

        val goalDao = appDatabase.goalDao()
        val goalListDao = appDatabase.goalListDao()
        val noteDao = appDatabase.noteDao()
        val listItemDao = appDatabase.listItemDao()

        // 1. Видалення
        changesByType[ChangeType.Delete]?.forEach { change ->
            when (change.entityType) {
                "Привʼязка" -> listItemDao.deleteItemsByIds(listOf(change.id))
                "Нотатка" -> noteDao.deleteNoteById(change.id)
                "Список" -> goalListDao.deleteListById(change.id)
                "Ціль" -> goalDao.deleteGoalById(change.id)
            }
        }

        // 2. Оновлення
        changesByType[ChangeType.Update]?.forEach { change ->
            when (change.entityType) {
                "Список" -> goalListDao.update(change.entity as GoalList)
                "Ціль" -> goalDao.updateGoal(change.entity as Goal)
                "Нотатка" -> noteDao.updateNote(change.entity as Note)
            }
        }

        // 3. Додавання та Переміщення
        val addsAndMoves = (changesByType[ChangeType.Add] ?: emptyList()) + (changesByType[ChangeType.Move] ?: emptyList())
        addsAndMoves.forEach { change ->
            when (change.entityType) {
                "Список" -> goalListDao.insert(change.entity as GoalList)
                "Ціль" -> goalDao.insertGoal(change.entity as Goal)
                "Нотатка" -> noteDao.insertNote(change.entity as Note)
                "Привʼязка" -> listItemDao.insertItem(change.entity as ListItem)
            }
        }
    }

    suspend fun exportDatabaseToFile(): Result<String> {
        return try {
            val backupJson = createBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_app_backup_$timestamp.json"

            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
                }
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { it.write(backupJson.toByteArray()) }
                Result.success("Експорт успішно завершено до Downloads/ForwardApp.")
            } else {
                Result.failure(Exception("Не вдалося створити файл."))
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Помилка експорту", e)
            Result.failure(e)
        }
    }

    suspend fun importDatabaseFromFile(uri: Uri): Result<String> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (jsonString.isNullOrBlank()) {
                return Result.failure(Exception("Файл порожній або пошкоджений."))
            }
            val report = createSyncReport(jsonString)
            applyChanges(report.changes)
            Result.success("Імпорт та синхронізацію завершено!")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Помилка імпорту", e)
            Result.failure(e)
        }
    }

    suspend fun createBackupJsonString(): String {
        val lists = goalRepository.getAllGoalLists()
        val goals = goalRepository.getAllGoals()
        val notes = goalRepository.getAllNotes()
        val listItems = goalRepository.getAllListItems()

        val desktopGoals = goals.associate {
            it.id to DesktopGoal(
                id = it.id, text = it.text, completed = it.completed,
                createdAt = longToDateString(it.createdAt)!!,
                updatedAt = longToDateString(it.updatedAt),
                associatedListIds = it.relatedLinks?.filter { l -> l.type == LinkType.GOAL_LIST }
                    ?.map { l -> l.target },
                description = it.description, tags = it.tags,
                valueImportance = it.valueImportance, valueImpact = it.valueImpact,
                effort = it.effort, cost = it.cost, risk = it.risk,
                weightEffort = it.weightEffort, weightCost = it.weightCost,
                weightRisk = it.weightRisk, rawScore = it.rawScore,
                displayScore = it.displayScore, scoringStatus = it.scoringStatus
            )
        }

        val goalListItems = listItems.filter { it.itemType == ListItemType.GOAL }
        val desktopInstances = goalListItems.associate {
            it.id to DesktopGoalInstance(id = it.id, goalId = it.entityId)
        }

        val desktopNotes = notes.associate {
            it.id to DesktopNote(
                id = it.id, title = it.title, content = it.content,
                createdAt = longToDateString(it.createdAt)!!,
                updatedAt = longToDateString(it.updatedAt)
            )
        }

        val desktopLists = lists.associate { list ->
            val listInstances = goalListItems.filter { it.listId == list.id }.sortedBy { it.order }
            list.id to DesktopGoalList(
                id = list.id, name = list.name, parentId = list.parentId,
                description = list.description,
                createdAt = longToDateString(list.createdAt)!!,
                updatedAt = longToDateString(list.updatedAt),
                itemInstanceIds = listInstances.map { it.id },
                isExpanded = list.isExpanded, order = list.order, tags = list.tags
            )
        }

        val desktopBackupData = DesktopBackupData(
            goals = desktopGoals,
            goalLists = desktopLists,
            goalInstances = desktopInstances,
            notes = desktopNotes
        )

        val desktopBackupFile = DesktopBackupFile(
            version = 4,
            exportedAt = longToDateString(System.currentTimeMillis())!!,
            data = desktopBackupData
        )

        return Gson().toJson(desktopBackupFile)
    }

    // ... всередині класу SyncRepository

    /**
     * Створює JSON-рядок, що містить повну резервну копію всіх даних додатку.
     */
    suspend fun createFullBackupJsonString(): String {
        // 1. Отримуємо всі дані з бази даних
        val databaseContent = DatabaseContent(
            goals = goalDao.getAll(),
            goalLists = goalListDao.getAll(),
            notes = noteDao.getAll(),
            listItems = listItemDao.getAll(),
            activityRecords = activityRecordDao.getAllRecordsStream().first(), // Отримуємо поточний список
            recentListEntries = recentListDao.getAllEntries(), // Потрібно додати цей метод в DAO
            linkItemEntities = linkItemDao.getAllEntities(), // Потрібно додати цей метод в DAO
        )

        // 2. Отримуємо всі налаштування з DataStore
        val settingsSnapshot: Preferences = settingsRepository.getPreferencesSnapshot() // Потрібно додати цей метод
        val settingsMap = settingsSnapshot.asMap().mapKeys { entry ->
            entry.key.name
        }.mapValues { entry ->
            entry.value.toString()
        }
        val settingsContent = SettingsContent(settings = settingsMap)

        // 3. Компонуємо все в один об'єкт
        val fullBackup = FullAppBackup(
            database = databaseContent,
            settings = settingsContent
        )

        // 4. Серіалізуємо в JSON
        return Gson().toJson(fullBackup)
    }
    // ... всередині класу SyncRepository

    /**
     * Виконує повний експорт даних додатку у файл JSON, обраний користувачем.
     */
    suspend fun exportFullBackupToFile(): Result<String> {
        return try {
            val backupJson = createFullBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_app_full_backup_$timestamp.json"

            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
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
            Log.e("SyncRepository", "Помилка повного експорту", e)
            Result.failure(e)
        }
    }
    suspend fun importFullBackupFromFile(uri: Uri): Result<String> {
        return try {
            // 1. Читаємо та десеріалізуємо JSON
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (jsonString.isNullOrBlank()) {
                return Result.failure(Exception("Файл бекапу порожній або пошкоджений."))
            }
            val backupData = Gson().fromJson(jsonString, FullAppBackup::class.java)

            // Перевірка версії, якщо потрібно
            if (backupData.backupSchemaVersion != 1) {
                return Result.failure(Exception("Несумісна версія файлу бекапу."))
            }

            // 2. Відновлюємо базу даних в одній транзакції
            appDatabase.withTransaction {
                // КРОК 2.1: ВИДАЛЯЄМО ВСІ СТАРІ ДАНІ
                // Порядок важливий через зовнішні ключі! Спочатку залежні таблиці.
                listItemDao.deleteAll() // Потрібно додати метод в DAO
                recentListDao.deleteAll() // Потрібно додати метод в DAO
                activityRecordDao.clearAll() // Існуючий метод
                goalDao.deleteAll() // Потрібно додати метод в DAO
                goalListDao.deleteAll() // Потрібно додати метод в DAO
                noteDao.deleteAll() // Потрібно додати метод в DAO
                linkItemDao.deleteAll() // Потрібно додати метод в DAO

                // КРОК 2.2: ВСТАВЛЯЄМО НОВІ ДАНІ
                // Порядок важливий! Спочатку незалежні таблиці.
                val dbContent = backupData.database
                goalListDao.insertLists(dbContent.goalLists)
                goalDao.insertGoals(dbContent.goals)
                noteDao.insertNotes(dbContent.notes)
                linkItemDao.insertAll(dbContent.linkItemEntities) // Потрібно додати метод в DAO
                activityRecordDao.insertAll(dbContent.activityRecords) // Потрібно додати метод в DAO
                recentListDao.insertAll(dbContent.recentListEntries) // Потрібно додати метод в DAO
                listItemDao.insertItems(dbContent.listItems)
            }

            // 3. Відновлюємо налаштування
            settingsRepository.restoreFromMap(backupData.settings.settings)

            Result.success("Дані успішно відновлено з резервної копії!")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Помилка повного імпорту", e)
            Result.failure(e)
        }
    }


}