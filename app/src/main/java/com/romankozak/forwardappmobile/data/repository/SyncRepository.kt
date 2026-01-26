package com.romankozak.forwardappmobile.data.repository

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.SyncMapper.updatedTs
import com.romankozak.forwardappmobile.data.sync.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.*
import com.romankozak.forwardappmobile.features.attachments.data.*
import com.romankozak.forwardappmobile.features.attachments.data.models.*
import com.romankozak.forwardappmobile.features.contexts.data.*
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.contexts.data.models.*
import com.romankozak.forwardappmobile.features.daymanagement.data.models.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.recent.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

@Singleton
class SyncRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    @param:ApplicationContext private val context: AndroidContext,
    private val syncLocalService: SyncLocalService,
    private val logicHelper: SyncLogicHelper,
    private val goalDao: GoalDao,
    private val contextDao: ContextDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val settingsRepository: SettingsRepository,
    private val noteDocumentDao: NoteDocumentDao,
    private val checklistDao: ChecklistDao,
    private val recentItemDao: RecentItemDao,
    private val attachmentDao: AttachmentDao,
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val chatDao: ChatDao,
    private val reminderDao: ReminderDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiInsightDao: AiInsightDao,
) {
    private val TAG = "SyncRepository"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    suspend fun exportFullBackupToFile(): Result<String> = try {
        val json = createFullBackupJsonString()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = "forward_full_backup_$ts.json"
        saveFileToDownloads(name, json)
        Result.success("Файл збережено")
    } catch (e: Exception) {
        Result.failure(e)
    }

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

    suspend fun getLastSyncTime(): Long? {
        val local = syncLocalService.loadLocalDatabaseContent()
        val allSyncedTimes = listOfNotNull(
            local.projects.mapNotNull { it.syncedAt }.minOrNull(),
            local.goals.mapNotNull { it.syncedAt }.minOrNull(),
            local.documents.mapNotNull { it.syncedAt }.minOrNull(),
            local.attachments.mapNotNull { it.syncedAt }.minOrNull(),
            local.contextAttachmentCrossRefs.mapNotNull { it.syncedAt }.minOrNull(),
            local.backlogOrders.mapNotNull { it.syncedAt }.minOrNull(),
        )
        return allSyncedTimes.minOrNull()
    }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
        val incomingDb = backup.database ?: return SyncReport(emptyList())

        val localProjects = contextDao.getAll().associateBy { it.id }
        val localGoals = goalDao.getAll().associateBy { it.id }
        val changes = mutableListOf<SyncChange>()

        incomingDb.goals.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeGoal(incomingRaw)
            val local = localGoals[incoming.id]?.let { SyncMapper.normalizeGoal(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Ціль", incoming.id, "Нова ціль: ${incoming.text}", entity = incoming))
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(SyncChange(ChangeType.Update, "Ціль", incoming.id, "Оновлено ціль: ${incoming.text}", entity = incoming))
            }
        }

        incomingDb.projects.filter { !SystemContexts.isSystem(ContextId(it.id)) }.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeProject(incomingRaw)
            val local = localProjects[incoming.id]?.let { SyncMapper.normalizeProject(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Список", incoming.id, "Новий список: ${incoming.name}", entity = incoming))
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(SyncChange(ChangeType.Update, "Список", incoming.id, "Оновлено список: ${incoming.name}", entity = incoming))
            }
        }

        val incomingGoalIds = incomingDb.goals.map { it.id }.toSet()
        localGoals.keys.minus(incomingGoalIds).forEach { id ->
            localGoals[id]?.let {
                changes.add(SyncChange(ChangeType.Delete, "Ціль", id, "Видалено ціль: ${it.text}", entity = it))
            }
        }

        return SyncReport(changes)
    }

    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        appDatabase.withTransaction {
            approvedChanges.forEach { change ->
                when (change.type) {
                    ChangeType.Delete -> {
                        when (change.entityType) {
                            "Список" -> contextDao.deleteContextById(change.id)
                            "Ціль" -> goalDao.deleteGoalById(change.id)
                            "Привʼязка" -> listItemDao.deleteItemsByIds(listOf(change.id))
                        }
                    }
                    ChangeType.Update, ChangeType.Add, ChangeType.Move -> {
                        when (change.entity) {
                            is Context -> contextDao.insert(change.entity)
                            is Goal -> goalDao.insertGoal(change.entity)
                            is BacklogItem -> listItemDao.insertItem(change.entity)
                        }
                    }
                }
            }
        }
    }

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

    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            Log.d(TAG, "[applyServerChanges] Incoming items: projects=${changes.projects.size}, attachments=${changes.attachments.size}")

            appDatabase.withTransaction {
                val local = syncLocalService.loadLocalDatabaseContent()
                val allProjectIds = local.projects.map { it.id }.toSet()

                val idRedirects = mutableMapOf<String, String>()
                val localSystemProjects = local.projects.filter { it.systemKey != null }.associateBy { it.systemKey!! }

                val correctedIncomingProjects = changes.projects.map { incoming ->
                    val key = incoming.systemKey
                    if (key != null) {
                        localSystemProjects[key]?.let { localSys ->
                            if (localSys.id != incoming.id) {
                                idRedirects[incoming.id] = localSys.id
                                return@map incoming.copy(id = localSys.id)
                            }
                        }
                    }
                    incoming
                }

                val mergedContexts = logicHelper.mergeAndMark(
                    incoming = correctedIncomingProjects.map { SyncMapper.normalizeProject(it) },
                    localMap = local.projects.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { p, s -> p.copy(syncedAt = s) },
                    syncedAt = ts,
                    isDeletedSelector = { it.isDeleted },
                )
                if (mergedContexts.isNotEmpty()) contextDao.insertContexts(mergedContexts)

                val mergedGoals = logicHelper.mergeAndMark(
                    incoming = changes.goals.map { SyncMapper.normalizeGoal(it) },
                    localMap = local.goals.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { g, s -> g.copy(syncedAt = s) },
                    syncedAt = ts,
                    isDeletedSelector = { it.isDeleted },
                )
                if (mergedGoals.isNotEmpty()) goalDao.insertGoals(mergedGoals)

                val contextIds = (allProjectIds + mergedContexts.map { it.id }).toSet()

                val processedAttachments = changes.attachments.map { att ->
                    val newOwnerId = att.ownerContextId?.let { idRedirects[it] ?: it }
                    att.copy(ownerContextId = newOwnerId)
                }.filter { it.ownerContextId == null || it.ownerContextId in contextIds }

                val incomingAttachments = logicHelper.mergeAndMark(
                    incoming = processedAttachments,
                    localMap = local.attachments.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { at, s -> at.copy(syncedAt = s) },
                    syncedAt = ts,
                )

                val alreadySyncedIds = incomingAttachments.map { it.id }.toSet()
                val matchedExisting = processedAttachments
                    .filter { it.id !in alreadySyncedIds }
                    .mapNotNull { inc -> local.attachments.find { it.id == inc.id } }
                    .map { it.copy(syncedAt = ts) }

                attachmentDao.insertAttachments(incomingAttachments + matchedExisting)

                val synthesizedRefs = logicHelper.synthesizeMissingCrossRefs(processedAttachments, changes.contextAttachmentCrossRefs)
                val finalRefs = synthesizedRefs.map { ref ->
                    val newCtxId = idRedirects[ref.contextId] ?: ref.contextId
                    ref.copy(contextId = newCtxId, syncedAt = ts)
                }.filter { it.contextId in contextIds }

                attachmentDao.insertContextAttachmentLinks(finalRefs)

                val cleanedListItems = changes.backlogItems.map {
                    it.copy(
                        contextId = idRedirects[it.contextId] ?: it.contextId,
                        entityId = if (it.itemType == BacklogItemTypeValues.SUBLIST) idRedirects[it.entityId] ?: it.entityId else it.entityId,
                    )
                }
                listItemDao.insertItems(logicHelper.dedupListItems(cleanedListItems))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes", e)
            Result.failure(e)
        }
    }

    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff {
        val local = syncLocalService.loadLocalDatabaseContent()

        return BackupDiff(
            projects = logicHelper.diffEntities(
                incomingList = incoming.projects.map { SyncMapper.normalizeProject(it) },
                localList = local.projects,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            goals = logicHelper.diffEntities(
                incomingList = incoming.goals.map { SyncMapper.normalizeGoal(it) },
                localList = local.goals,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            backlogItems = logicHelper.diffEntities(
                incomingList = incoming.backlogItems,
                localList = local.backlogItems,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            documents = logicHelper.diffEntities(
                incomingList = incoming.documents,
                localList = local.documents,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            attachments = logicHelper.diffEntities(
                incomingList = incoming.attachments,
                localList = local.attachments,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            contextAttachmentCrossRefs = logicHelper.diffEntities(
                incomingList = incoming.contextAttachmentCrossRefs,
                localList = local.contextAttachmentCrossRefs,
                idSelector = { "${it.contextId}-${it.attachmentId}" },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
        )
    }

    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        val IMPORT_TAG = "SyncRepo_Selective"
        return try {
            val local = syncLocalService.loadLocalDatabaseContent()
            val ts = System.currentTimeMillis()

            fun <T> filterNewer(
                incoming: List<T>,
                localMap: Map<String, T>,
                idSelector: (T) -> String,
                versionSelector: (T) -> Long,
                updatedAtSelector: (T) -> Long,
            ): List<T> = incoming.filter { inc ->
                val loc = localMap[idSelector(inc)]
                if (loc == null) return@filter true
                val incVer = versionSelector(inc)
                val locVer = versionSelector(loc)
                if (incVer > locVer) return@filter true
                if (incVer < locVer) return@filter false
                updatedAtSelector(inc) > updatedAtSelector(loc)
            }

            appDatabase.withTransaction {
                Log.d(IMPORT_TAG, "Starting selective transaction...")

                val regularProjects = selectedData.projects.filter { !SystemContexts.isSystem(ContextId(it.id)) }
                val newerProjects = filterNewer(
                    regularProjects,
                    local.projects.associateBy { it.id },
                    { it.id },
                    { it.version },
                    { it.updatedTs() },
                )
                if (newerProjects.isNotEmpty()) {
                    contextDao.insertContexts(newerProjects.map { it.copy(syncedAt = ts) })
                }

                val newerGoals = filterNewer(
                    selectedData.goals,
                    local.goals.associateBy { it.id },
                    { it.id },
                    { it.version },
                    { it.updatedTs() },
                )
                if (newerGoals.isNotEmpty()) {
                    goalDao.insertGoals(newerGoals.map { it.copy(syncedAt = ts) })
                }

                val currentContextIds = (local.projects.map { it.id } + newerProjects.map { it.id }).toSet()
                val currentGoalIds = (local.goals.map { it.id } + newerGoals.map { it.id }).toSet()

                val validListItems = selectedData.backlogItems.filter {
                    it.contextId in currentContextIds || it.entityId in currentGoalIds
                }
                listItemDao.insertItems(validListItems.map { it.copy(syncedAt = ts) })

                if (selectedData.attachments.isNotEmpty()) {
                    val newerAttachments = filterNewer(
                        selectedData.attachments,
                        local.attachments.associateBy { it.id },
                        { it.id },
                        { it.version },
                        { it.updatedTs() },
                    )
                    attachmentDao.insertAttachments(newerAttachments.map { it.copy(syncedAt = ts) })
                }

                if (selectedData.contextAttachmentCrossRefs.isNotEmpty()) {
                    val validCrossRefs = selectedData.contextAttachmentCrossRefs.filter {
                        it.contextId in currentContextIds
                    }
                    attachmentDao.insertContextAttachmentLinks(validCrossRefs.map { it.copy(syncedAt = ts) })
                }
            }

            Result.success("Вибрані дані успішно імпортовано.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Critical error during selective import", e)
            Result.failure(e)
        }
    }

    suspend fun exportAttachmentsToFile(): Result<String> = try {
        val backupJson = createAttachmentsBackupJsonString()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "forward_attachments_$timestamp.json"
        saveFileToDownloads(fileName, backupJson)
        Result.success("Вкладення успішно збережено до Downloads/ForwardApp")
    } catch (e: Exception) {
        Log.e(TAG, "Error exporting attachments", e)
        Result.failure(e)
    }

    suspend fun createAttachmentsBackupJsonString(): String {
        Log.d(TAG, "=== СТАРТ ЕКСПОРТУ ВКЛАДЕНЬ ===")
        val attachments = attachmentDao.getAll()
        val crossRefs = attachmentDao.getAllContextAttachmentCrossRefs()

        val synthesizedCrossRefs = logicHelper.synthesizeMissingCrossRefs(
            attachments = attachments,
            existingCrossRefs = crossRefs,
        )

        val attachmentsBackup = AttachmentsBackup(
            documents = noteDocumentDao.getAllDocuments(),
            documentItems = noteDocumentDao.getAllDocumentItems(),
            checklists = checklistDao.getAllChecklists(),
            checklistItems = checklistDao.getAllChecklistItems(),
            linkItemEntities = linkItemDao.getAllEntities(),
            attachments = attachments,
            contextAttachmentCrossRefs = synthesizedCrossRefs,
        )

        return gson.toJson(attachmentsBackup)
    }

    suspend fun importAttachmentsFromFile(uri: Uri): Result<String> {
        val IMPORT_TAG = "SyncRepo_AttImport"
        try {
            val jsonString = readTextFromUri(uri) ?: throw Exception("File is empty")
            val backupData = gson.fromJson(jsonString, AttachmentsBackup::class.java)
            val existingContextIds = contextDao.getAll().map { it.id }.toSet()

            appDatabase.withTransaction {
                val validDocs = backupData.documents.filter { it.contextId in existingContextIds }
                noteDocumentDao.insertAllDocuments(validDocs)

                val validDocIds = validDocs.map { it.id }.toSet()
                noteDocumentDao.insertAllDocumentItems(backupData.documentItems.filter { it.listId in validDocIds })

                val validChecklists = backupData.checklists.filter { it.contextId in existingContextIds }
                checklistDao.insertChecklists(validChecklists)

                val validChecklistIds = validChecklists.map { it.id }.toSet()
                checklistDao.insertItems(backupData.checklistItems.filter { it.checklistId in validChecklistIds })

                linkItemDao.insertAll(backupData.linkItemEntities)

                val processedAttachments = backupData.attachments.map { att ->
                    if (att.ownerContextId != null && att.ownerContextId !in existingContextIds) {
                        att.copy(ownerContextId = null)
                    } else {
                        att
                    }
                }
                attachmentDao.insertAttachments(processedAttachments)

                val attachmentIds = processedAttachments.map { it.id }.toSet()
                val validCrossRefs = backupData.contextAttachmentCrossRefs.filter {
                    it.contextId in existingContextIds && it.attachmentId in attachmentIds
                }
                attachmentDao.insertContextAttachmentLinks(validCrossRefs)
            }

            val orphanCount = backupData.attachments.size - backupData.attachments.count { it.ownerContextId in existingContextIds }
            return Result.success("Імпорт завершено. Знайдено $orphanCount вкладень без прив'язки до проектів.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Critical error during attachments import", e)
            return Result.failure(e)
        }
    }

    private fun readTextFromUri(uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    private fun saveFileToDownloads(name: String, json: String) {
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

    private suspend fun runPostBackupMigration() {
        Log.d(TAG, "Starting post-backup migration")
        val db = appDatabase.openHelper.writableDatabase
        com.romankozak.forwardappmobile.data.database.migrateSpecialProjects(db)
    }
}