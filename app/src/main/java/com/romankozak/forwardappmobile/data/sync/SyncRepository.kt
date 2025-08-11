package com.romankozak.forwardappmobile.data.sync

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.repository.GoalRepository
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

// --- ОНОВЛЕНІ МОДЕЛІ ДЛЯ ЗВІТУ ТА ЗМІН (аналогічно до десктопу) ---
enum class ChangeType {
    Add, Update, Delete, Move
}

data class SyncChange(
    val type: ChangeType,
    val entityType: String, // "Список", "Ціль", "Привʼязка"
    val id: String,
    val description: String,
    val longDescription: String? = null,
    val entity: Any // Повна сутність для Add, Update, Move
)

data class SyncReport(
    val changes: List<SyncChange>
)

// Допоміжний клас для зберігання стану, аналогічний ListsState в Redux
private data class ListsState(
    val goals: Map<String, Goal>,
    val goalLists: Map<String, GoalList>,
    val goalInstances: Map<String, GoalInstance>
)

@Singleton
class SyncRepository @Inject constructor(
    private val goalRepository: GoalRepository,
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
    }

    suspend fun fetchBackupFromWifi(address: String): Result<String> {
        return try {
            var cleanAddress = address.trim()
            if (!cleanAddress.startsWith("http://") && !cleanAddress.startsWith("https://")) {
                cleanAddress = "http://$cleanAddress"
            }
            // Видаляємо можливий шлях, залишаючи тільки хост і порт
            val uri = Uri.parse(cleanAddress)
            val hostAndPort = "${uri.host}:${if (uri.port != -1) uri.port else 8080}"

            val fullUrl = "http://$hostAndPort/export"
            Log.d("SyncRepository", "Attempting to fetch from sanitized URL: $fullUrl")

            val response: String = client.get(fullUrl).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error fetching from WiFi", e)
            Result.failure(e)
        }
    }

    private fun dateStringToLong(dateString: String?): Long? {
        if (dateString == null) return null
        // Формат, який використовує new Date().toISOString() в JavaScript
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return try { format.parse(dateString)?.time } catch (e: Exception) { null }
    }

    private fun longToDateString(time: Long?): String? {
        if (time == null) return null
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(time))
    }

    /**
     * Трансформує дані з десктопного формату в локальні моделі Room.
     * Аналог `transformImportedData` з syncLogic.ts.
     */
    private fun transformImportedData(data: DesktopBackupData): ListsState {
        val newGoals = (data.goals ?: emptyMap()).mapValues { (_, goal) ->
            Goal(
                id = goal.id,
                text = goal.text,
                completed = goal.completed,
                createdAt = dateStringToLong(goal.createdAt) ?: 0L,
                updatedAt = dateStringToLong(goal.updatedAt),
                description = goal.description,
                tags = goal.tags,
                associatedListIds = goal.associatedListIds,
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
                scoringStatus = goal.scoringStatus
            )
        }

        val newGoalLists = (data.goalLists ?: emptyMap()).mapValues { (_, list) ->
            GoalList(
                id = list.id,
                name = list.name,
                parentId = list.parentId,
                description = list.description,
                createdAt = dateStringToLong(list.createdAt) ?: 0L,
                updatedAt = dateStringToLong(list.updatedAt),
                isExpanded = list.isExpanded ?: true,
                order = list.order ?: 0L,
                tags = list.tags
            )
        }

        val newGoalInstances = mutableMapOf<String, GoalInstance>()
        (data.goalLists ?: emptyMap()).values.forEach { list ->
            list.itemInstanceIds.forEachIndexed { index, instanceId ->
                data.goalInstances?.get(instanceId)?.let { originalInstance ->
                    newGoalInstances[instanceId] = GoalInstance(
                        instanceId = originalInstance.id,
                        goalId = originalInstance.goalId,
                        listId = list.id,
                        order = index.toLong()
                    )
                }
            }
        }

        return ListsState(goals = newGoals, goalLists = newGoalLists, goalInstances = newGoalInstances)
    }

    /**
     * ПОВНІСТЮ ОНОВЛЕНИЙ МЕТОД
     * Створює детальний звіт про зміни, порівнюючи локальний та віддалений стан.
     * Повністю відтворює логіку `syncComparator` з десктопу.
     */
    suspend fun createSyncReport(jsonString: String): SyncReport {
        return try {
            val backupFile = Gson().fromJson(jsonString, DesktopBackupFile::class.java)
            val remoteData = backupFile.data ?: throw IllegalArgumentException("Поле 'data' відсутнє у файлі бекапу.")

            val remoteState = transformImportedData(remoteData)
            val localLists = goalRepository.getAllGoalLists().associateBy { it.id }
            val localGoals = goalRepository.getAllGoals().associateBy { it.id }
            val localInstances = goalRepository.getAllGoalInstances().associateBy { it.instanceId }

            val changes = mutableListOf<SyncChange>()
            val deletedListIds = mutableSetOf<String>()
            val deletedGoalIds = mutableSetOf<String>()

            // Крок 1: Порівняння списків
            val allListIds = localLists.keys + remoteState.goalLists.keys
            allListIds.forEach { id ->
                val local = localLists[id]
                val remote = remoteState.goalLists[id]
                when {
                    remote != null && local == null ->
                        changes.add(SyncChange(ChangeType.Add, "Список", id, remote.name, entity = remote))
                    remote == null && local != null -> {
                        changes.add(SyncChange(ChangeType.Delete, "Список", id, local.name, entity = local))
                        deletedListIds.add(id)
                    }
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) ->
                        changes.add(SyncChange(ChangeType.Update, "Список", id, remote.name, entity = remote))
                }
            }

            // Крок 2: Порівняння цілей
            val allGoalIds = localGoals.keys + remoteState.goals.keys
            allGoalIds.forEach { id ->
                val local = localGoals[id]
                val remote = remoteState.goals[id]
                when {
                    remote != null && local == null ->
                        changes.add(SyncChange(ChangeType.Add, "Ціль", id, remote.text, entity = remote))
                    remote == null && local != null -> {
                        changes.add(SyncChange(ChangeType.Delete, "Ціль", id, local.text, entity = local))
                        deletedGoalIds.add(id)
                    }
                    remote != null && local != null && (remote.updatedAt ?: 0) > (local.updatedAt ?: 0) ->
                        changes.add(SyncChange(ChangeType.Update, "Ціль", id, remote.text, entity = remote))
                }
            }

            // Крок 3: Порівняння прив'язок (GoalInstance)
            val allInstanceIds = localInstances.keys + remoteState.goalInstances.keys
            allInstanceIds.forEach { id ->
                val local = localInstances[id]
                val remote = remoteState.goalInstances[id]
                when {
                    remote != null && local == null -> {
                        val desc = "Ціль \"${remoteState.goals[remote.goalId]?.text ?: "?"}\" до списку \"${remoteState.goalLists[remote.listId]?.name ?: "?"}\""
                        changes.add(SyncChange(ChangeType.Add, "Привʼязка", id, desc, entity = remote))
                    }
                    remote == null && local != null -> {
                        if (!deletedListIds.contains(local.listId) && !deletedGoalIds.contains(local.goalId)) { //
                            val desc = "Ціль \"${localGoals[local.goalId]?.text ?: "?"}\" зі списку \"${localLists[local.listId]?.name ?: "?"}\""
                            changes.add(SyncChange(ChangeType.Delete, "Привʼязка", id, desc, entity = local))
                        }
                    }
                    remote != null && local != null && (remote.order != local.order || remote.listId != local.listId) -> { //
                        val goalText = localGoals[local.goalId]?.text ?: remoteState.goals[remote.goalId]?.text ?: "?"
                        val fromList = localLists[local.listId]?.name ?: "?"
                        val toList = remoteState.goalLists[remote.listId]?.name ?: "?"
                        val desc = "Переміщення цілі \"$goalText\""
                        val longDesc = "Ціль \"$goalText\" переміщено з \"$fromList\" (поз. ${local.order}) у \"$toList\" (поз. ${remote.order})."
                        changes.add(SyncChange(ChangeType.Move, "Привʼязка", id, desc, longDesc, entity = remote))
                    }
                }
            }

            SyncReport(changes)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Помилка розбору JSON або створення звіту", e)
            throw IllegalStateException("Помилка розбору даних: ${e.message}", e)
        }
    }

    /**
     * ПОВНІСТЮ ОНОВЛЕНИЙ МЕТОД
     * Застосовує схвалені зміни до локальної БД.
     * Повністю відтворює логіку `applyChanges` з десктопу.
     */
    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        val changesByType = approvedChanges.groupBy { it.type }

        val goalDao = appDatabase.goalDao()
        val goalListDao = appDatabase.goalListDao()

        // 1. Видалення (у зворотному порядку залежностей)
        changesByType[ChangeType.Delete]?.forEach { change ->
            when (change.entityType) {
                "Привʼязка" -> goalDao.deleteInstanceById(change.id)
                "Список" -> {
                    goalDao.deleteInstancesForLists(listOf(change.id))
                    goalListDao.deleteListById(change.id)
                }
                "Ціль" -> {
                    // Знайти та видалити всі екземпляри цієї цілі
                    val instancesToDelete = goalRepository.getAllGoalInstances().filter { it.goalId == change.id }.map { it.instanceId }
                    goalDao.deleteInstancesByIds(instancesToDelete)
                    goalDao.deleteGoalById(change.id)
                }
            }
        }

        // 2. Оновлення
        changesByType[ChangeType.Update]?.forEach { change ->
            when (change.entityType) {
                "Список" -> goalListDao.update(change.entity as GoalList)
                "Ціль" -> goalDao.updateGoal(change.entity as Goal)
            }
        }

        // 3. Додавання та Переміщення (обробляються однаково - як вставка/заміна)
        val addsAndMoves = (changesByType[ChangeType.Add] ?: emptyList()) + (changesByType[ChangeType.Move] ?: emptyList())
        addsAndMoves.forEach { change ->
            when (change.entityType) {
                "Список" -> goalListDao.insert(change.entity as GoalList)
                "Ціль" -> goalDao.insertGoal(change.entity as Goal)
                "Привʼязка" -> goalDao.insertInstance(change.entity as GoalInstance)
            }
        }
    }

    // --- Методи експорту/імпорту (залишаються без змін, але тепер використовують нову логіку) ---
    suspend fun exportDatabaseToFile(): Result<String> {
        return try {
            val backupJson = createBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_app_backup_$timestamp.json"

            val contentResolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
                }
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupJson.toByteArray())
                }
                Result.success("Експорт успішно завершено! Файл збережено в Downloads/ForwardApp.")
            } else {
                Result.failure(Exception("Не вдалося створити файл для збереження."))
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Помилка експорту в файл", e)
            Result.failure(e)
        }
    }

    /**
     * Імпорт з файлу тепер також використовує нову логіку синхронізації,
     * а не просто перезаписує базу даних.
     */
    suspend fun importDatabaseFromFile(uri: Uri): Result<String> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (jsonString.isNullOrBlank()) {
                return Result.failure(Exception("Обраний файл порожній або пошкоджений."))
            }

            // Замість appDatabase.clearAllTables() ми створюємо звіт і застосовуємо зміни
            val report = createSyncReport(jsonString)
            // При імпорті з файлу зазвичай приймають усі зміни
            applyChanges(report.changes)

            Result.success("Імпорт та синхронізацію успішно завершено!")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Помилка імпорту з файлу", e)
            Result.failure(e)
        }
    }

    /**
     * Створює JSON-рядок для експорту, сумісний з десктопним додатком.
     * Залишається без змін.
     */
    suspend fun createBackupJsonString(): String {
        val lists = goalRepository.getAllGoalLists()
        val goals = goalRepository.getAllGoals()
        val instances = goalRepository.getAllGoalInstances()

        val desktopGoals = goals.associate {
            it.id to DesktopGoal(
                id = it.id, text = it.text, completed = it.completed,
                createdAt = longToDateString(it.createdAt)!!,
                updatedAt = longToDateString(it.updatedAt),
                associatedListIds = it.associatedListIds,
                description = it.description, tags = it.tags,
                valueImportance = it.valueImportance, valueImpact = it.valueImpact,
                effort = it.effort, cost = it.cost, risk = it.risk,
                weightEffort = it.weightEffort, weightCost = it.weightCost,
                weightRisk = it.weightRisk, rawScore = it.rawScore,
                displayScore = it.displayScore, scoringStatus = it.scoringStatus
            )
        }

        val desktopInstances = instances.associate {
            it.instanceId to DesktopGoalInstance(id = it.instanceId, goalId = it.goalId)
        }

        val desktopLists = lists.associate { list ->
            val listInstances = instances.filter { it.listId == list.id }.sortedBy { it.order }
            list.id to DesktopGoalList(
                id = list.id, name = list.name, parentId = list.parentId,
                description = list.description,
                createdAt = longToDateString(list.createdAt)!!,
                updatedAt = longToDateString(list.updatedAt),
                itemInstanceIds = listInstances.map { it.instanceId },
                isExpanded = list.isExpanded, order = list.order, tags = list.tags
            )
        }

        val desktopBackupData = DesktopBackupData(
            goals = desktopGoals,
            goalLists = desktopLists,
            goalInstances = desktopInstances
        )

        val desktopBackupFile = DesktopBackupFile(
            version = 4,
            exportedAt = longToDateString(System.currentTimeMillis())!!,
            data = desktopBackupData
        )

        return Gson().toJson(desktopBackupFile)
    }
}