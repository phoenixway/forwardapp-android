package com.romankozak.forwardappmobile.data.sync

import android.util.Log
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.gson.gson
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// --- Sealed class без змін ---
sealed class SyncChange(
    val id: String,
    val type: String,
    val entityType: String,
    val description: String
) {
    class Add(id: String, entityType: String, description: String, val entity: Any) : SyncChange(id, "Додавання", entityType, description)
    class Update(id: String, entityType: String, description: String, val oldEntity: Any, val newEntity: Any) : SyncChange(id, "Оновлення", entityType, description)
    class Remove(id: String, entityType: String, description: String, val entity: Any) : SyncChange(id, "Видалення", entityType, description)
}

// --- ВИПРАВЛЕНО: Додаємо анотації для Hilt і змінюємо конструктор ---
@Singleton
class SyncRepository @Inject constructor(
    private val goalRepository: GoalRepository
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
        return try { format.parse(dateString)?.time } catch (e: Exception) { System.currentTimeMillis() }
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
            if (backupFile?.data == null) throw IllegalArgumentException("Отримано некоректний формат даних (поле 'data' відсутнє).")

            val changes = mutableListOf<SyncChange>()

            // --- ВИПРАВЛЕНО: Використовуємо goalRepository ---
            val localLists = goalRepository.getAllGoalLists().associateBy { it.id }
            val localGoals = goalRepository.getAllGoals().associateBy { it.id }

            val importedGoals = (backupFile.data.goals ?: emptyMap()).values.map {
                Goal(
                    id = it.id, text = it.text, completed = it.completed,
                    createdAt = dateStringToLong(it.createdAt) ?: 0L,
                    updatedAt = dateStringToLong(it.updatedAt), description = "",
                    tags = null,
                    associatedListIds = it.associatedListIds
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
                    changes.add(SyncChange.Add(importedList.id, "Список", importedList.name, importedList))
                } else if ((importedList.updatedAt ?: 0L) > (localList.updatedAt ?: 0L)) {
                    changes.add(SyncChange.Update(importedList.id, "Список", importedList.name, localList, importedList))
                }
            }

            importedGoals.forEach { importedGoal ->
                val localGoal = localGoals[importedGoal.id]
                if (localGoal == null) {
                    changes.add(SyncChange.Add(importedGoal.id, "Ціль", importedGoal.text, importedGoal))
                } else if ((importedGoal.updatedAt ?: 0L) > (localGoal.updatedAt ?: 0L)) {
                    changes.add(SyncChange.Update(importedGoal.id, "Ціль", importedGoal.text, localGoal, importedGoal))
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

        // --- ВИПРАВЛЕНО: Використовуємо goalRepository ---
        if (listsToApply.isNotEmpty()) goalRepository.insertGoalLists(listsToApply)
        if (goalsToApply.isNotEmpty()) goalRepository.insertGoals(goalsToApply)

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

            // --- ВИПРАВЛЕНО: Використовуємо goalRepository ---
            goalRepository.deleteInstancesForLists(affectedListIds)

            val newInstances = affectedLists.flatMap { list ->
                list.itemInstanceIds.mapIndexedNotNull { index, instanceId ->
                    allDesktopInstances[instanceId]?.let { desktopInstance ->
                        GoalInstance(
                            instanceId = desktopInstance.id,
                            goalId = desktopInstance.goalId,
                            listId = list.id,
                            order = index.toLong()
                        )
                    }
                }
            }

            // --- ВИПРАВЛЕНО: Використовуємо goalRepository ---
            if (newInstances.isNotEmpty()) goalRepository.insertGoalInstances(newInstances)
        }
    }

    suspend fun createBackupJsonString(): String {
        // --- ВИПРАВЛЕНО: Використовуємо goalRepository ---
        val lists = goalRepository.getAllGoalLists()
        val goals = goalRepository.getAllGoals()
        val instances = goalRepository.getAllGoalInstances()

        val childMap = lists.filter { it.parentId != null }.groupBy { it.parentId!! }

        val desktopGoals = goals.associate {
            it.id to DesktopGoal(
                id = it.id, text = it.text, completed = it.completed,
                createdAt = longToDateString(it.createdAt)!!,
                updatedAt = longToDateString(it.updatedAt),
                associatedListIds = it.associatedListIds
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