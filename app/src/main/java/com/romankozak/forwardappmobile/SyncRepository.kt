package com.romankozak.forwardappmobile

import android.util.Log
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.gson.gson
import java.text.SimpleDateFormat
import java.util.*

// --- ФІНАЛЬНА, ПРАВИЛЬНА РЕАЛІЗАЦІЯ SEALED CLASS ---
sealed class SyncChange(
    val id: String,
    val type: String,
    val entityType: String,
    val description: String
) {
    // Класи-спадкоємці тепер визначені всередині тіла sealed class, що є правильною практикою
    class Add(
        id: String,
        entityType: String,
        description: String,
        val entity: Any
    ) : SyncChange(id, "Додавання", entityType, description)

    class Update(
        id: String,
        entityType: String,
        description: String,
        val oldEntity: Any,
        val newEntity: Any
    ) : SyncChange(id, "Оновлення", entityType, description)

    class Remove(
        id: String,
        entityType: String,
        description: String,
        val entity: Any
    ) : SyncChange(id, "Видалення", entityType, description)
}


class SyncRepository(
    private val goalListDao: GoalListDao,
    private val goalDao: GoalDao
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
    }

    suspend fun fetchBackupFromWifi(address: String): Result<String> {
        return try {
            var cleanAddress = address.trim()
            if (cleanAddress.startsWith("http://")) {
                cleanAddress = cleanAddress.substring("http://".length)
            } else if (cleanAddress.startsWith("https://")) {
                cleanAddress = cleanAddress.substring("https://".length)
            }
            cleanAddress = cleanAddress.split("/").first()

            val fullUrl = "http://$cleanAddress/export"
            Log.d("SyncRepository", "Attempting to fetch from sanitized URL: $fullUrl")

            val response: String = client.get(fullUrl).body()
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
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
            System.currentTimeMillis()
        }
    }

    private fun longToDateString(time: Long?): String? {
        if (time == null) return null
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(time))
    }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        Log.d("SyncRepository", "JSON to parse: $jsonString")

        try {
            val backupFile = Gson().fromJson(jsonString, DesktopBackupFile::class.java)

            if (backupFile?.data == null) {
                throw IllegalArgumentException("Отримано некоректний формат даних (поле 'data' відсутнє).")
            }

            val changes = mutableListOf<SyncChange>()
            val localLists = goalListDao.getAll().associateBy { it.id }
            val localGoals = goalDao.getAll().associateBy { it.id }

            val importedGoals = (backupFile.data.goals ?: emptyMap()).values.map {
                Goal(
                    id = it.id, text = it.text, completed = it.completed,
                    createdAt = dateStringToLong(it.createdAt) ?: 0L,
                    updatedAt = dateStringToLong(it.updatedAt), description = "",
                    tags = null
                )
            }
            val importedLists = (backupFile.data.goalLists ?: emptyMap()).values.map {
                GoalList(
                    id = it.id, name = it.name, parentId = it.parentId,
                    description = it.description, createdAt = dateStringToLong(it.createdAt) ?: 0L,
                    updatedAt = dateStringToLong(it.updatedAt)
                )
            }

            importedLists.forEach { importedList ->
                val localList = localLists[importedList.id]
                if (localList == null) {
                    changes.add(SyncChange.Add(id = importedList.id, entityType = "Список", description = importedList.name, entity = importedList))
                } else if ((importedList.updatedAt ?: 0L) > (localList.updatedAt ?: 0L)) {
                    changes.add(SyncChange.Update(id = importedList.id, entityType = "Список", description = importedList.name, oldEntity = localList, newEntity = importedList))
                }
            }

            importedGoals.forEach { importedGoal ->
                val localGoal = localGoals[importedGoal.id]
                if (localGoal == null) {
                    changes.add(SyncChange.Add(id = importedGoal.id, entityType = "Ціль", description = importedGoal.text, entity = importedGoal))
                } else if ((importedGoal.updatedAt ?: 0L) > (localGoal.updatedAt ?: 0L)) {
                    changes.add(SyncChange.Update(id = importedGoal.id, entityType = "Ціль", description = importedGoal.text, oldEntity = localGoal, newEntity = importedGoal))
                }
            }

            return SyncReport(changes)
        } catch (e: Exception) {
            throw IllegalStateException("Помилка розбору даних: ${e.message}", e)
        }
    }

    suspend fun applyChanges(approvedChanges: List<SyncChange>, jsonString: String) {
        val backupFile = Gson().fromJson(jsonString, DesktopBackupFile::class.java)

        if (backupFile?.data == null) return

        val listsToApply = approvedChanges.mapNotNull { (it as? SyncChange.Add)?.entity as? GoalList } +
                approvedChanges.mapNotNull { (it as? SyncChange.Update)?.newEntity as? GoalList }

        val goalsToApply = approvedChanges.mapNotNull { (it as? SyncChange.Add)?.entity as? Goal } +
                approvedChanges.mapNotNull { (it as? SyncChange.Update)?.newEntity as? Goal }

        if (listsToApply.isNotEmpty()) {
            goalListDao.insertLists(listsToApply)
        }
        if (goalsToApply.isNotEmpty()) {
            goalDao.insertGoals(goalsToApply)
        }

        val approvedListIds = listsToApply.map { it.id }.toSet()
        val approvedGoalIds = goalsToApply.map { it.id }.toSet()

        val allDesktopLists = backupFile.data.goalLists ?: emptyMap()
        val allDesktopInstances = backupFile.data.goalInstances ?: emptyMap()

        val affectedLists = allDesktopLists.values.filter { list ->
            list.id in approvedListIds || list.itemInstanceIds.any { instId ->
                allDesktopInstances[instId]?.goalId in approvedGoalIds
            }
        }

        if (affectedLists.isNotEmpty()) {
            val affectedListIds = affectedLists.map { it.id }

            goalDao.deleteInstancesForLists(affectedListIds)

            val newInstances = affectedLists.flatMap { list ->
                list.itemInstanceIds.mapIndexedNotNull { index, instanceId ->
                    allDesktopInstances[instanceId]?.let { desktopInstance ->
                        GoalInstance(
                            id = desktopInstance.id,
                            goalId = desktopInstance.goalId,
                            listId = list.id,
                            orderIndex = index
                        )
                    }
                }
            }

            if (newInstances.isNotEmpty()) {
                goalDao.insertGoalInstances(newInstances)
            }
        }
    }

    suspend fun createBackupJsonString(): String {
        val lists = goalListDao.getAll()
        val goals = goalDao.getAll()
        val instances = goalDao.getAllInstances()
        val childMap = lists.filter { it.parentId != null }.groupBy { it.parentId!! }

        val desktopGoals = goals.associate {
            it.id to DesktopGoal(
                id = it.id, text = it.text, completed = it.completed,
                createdAt = longToDateString(it.createdAt)!!,
                updatedAt = longToDateString(it.updatedAt)
            )
        }

        val desktopInstances = instances.associate {
            it.id to DesktopGoalInstance(id = it.id, goalId = it.goalId)
        }

        val desktopLists = lists.associate { list ->
            val listInstances = instances.filter { it.listId == list.id }.sortedBy { it.orderIndex }
            list.id to DesktopGoalList(
                id = list.id, name = list.name, parentId = list.parentId,
                description = list.description,
                createdAt = longToDateString(list.createdAt)!!,
                updatedAt = longToDateString(list.updatedAt),
                itemInstanceIds = listInstances.map { it.id },
                childListIds = childMap[list.id]?.map { it.id } ?: emptyList()
            )
        }

        val rootListIds = lists.filter { it.parentId == null }.map { it.id }

        val desktopBackupData = DesktopBackupData(
            goals = desktopGoals,
            goalLists = desktopLists,
            goalInstances = desktopInstances,
            rootListIds = rootListIds
        )

        val desktopBackupFile = DesktopBackupFile(
            version = 2,
            exportedAt = longToDateString(System.currentTimeMillis())!!,
            data = desktopBackupData
        )

        return Gson().toJson(desktopBackupFile)
    }
}