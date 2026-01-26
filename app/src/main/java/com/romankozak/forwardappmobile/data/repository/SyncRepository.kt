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
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

@Singleton
class SyncRepository
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        @param:ApplicationContext private val context: AndroidContext,
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
        private val contextStructureDao: ContextStructureDao,
    ) {
        private val TAG = "SyncRepository"
        private val WIFI_SYNC_TAG = "WIFI_SYNC"
        private val WIFI_LOG = "FWD_SYNC_WIFI"

        private val gson =
            GsonBuilder()
                .registerTypeAdapter(Long::class.java, LongDeserializer())
                .registerTypeAdapter(ReservedGroup::class.java, ReservedGroupAdapter())
                .create()

        private val client by lazy {
            HttpClient(CIO) {
                install(ContentNegotiation) { gson() }
            }
        }

        suspend fun exportFullBackupToFile(): Result<String> =
            try {
                val json = createFullBackupJsonString()
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val name = "forward_full_backup_$ts.json"

                saveFileToDownloads(name, json)
                Result.success("Файл збережено")
            } catch (e: Exception) {
                Result.failure(e)
            }

        suspend fun createFullBackupJsonString(): String {
            val local = loadLocalDatabaseContent()

            // Додаємо специфічні для повного бекапу дані (Recent Items)
            val recentProjectEntries =
                recentItemDao.getAll().map {
                    RecentProjectEntry(contextId = it.target, timestamp = it.lastAccessed)
                }

            val fullContent =
                local.copy(
                    recentProjectEntries = recentProjectEntries,
                    // Додаємо розширені сутності
                    dayPlans = dayPlanDao.getAllPlansSync(),
                    dayTasks = dayTaskDao.getAllTasksSync(),
                    dailyMetrics = dailyMetricDao.getAllMetricsSync(),
                    conversations = chatDao.getAllConversationsSync(),
                    chatMessages = chatDao.getAllMessagesSync(),
                    reminders = reminderDao.getAllRemindersSync(),
                    tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
                    aiInsights = aiInsightDao.getAllSync(),
                )

            val settingsMap =
                settingsRepository.getPreferencesSnapshot().asMap()
                    .mapKeys { it.key.name }.mapValues { it.value.toString() }

            return gson.toJson(
                FullAppBackup(
                    database = fullContent,
                    settings = SettingsContent(settingsMap),
                ),
            )
        }

        suspend fun importFullBackupFromFile(uri: Uri): Result<String> =
            try {
                val json = readTextFromUri(uri) ?: throw Exception("Empty file")
                val backupData = gson.fromJson(json, FullAppBackup::class.java)
                val backup = backupData.database

                appDatabase.withTransaction {
                    // Крок 1-3: Системні проекти та ID Mapping
                    val dbSystemProjects = contextDao.getAll().filter { it.systemKey != null }
                    val existingSystemByKey = dbSystemProjects.associateBy { it.systemKey!! }
                    val contextIdMap = mutableMapOf<String, String>()

                    val cleanedProjects =
                        backup.projects.map { incoming ->
                            val normalized = SyncMapper.normalizeProject(incoming)
                            val existing = normalized.systemKey?.let { existingSystemByKey[it] }

                            if (existing != null) {
                                if (normalized.id != existing.id) contextIdMap[normalized.id] = existing.id
                                // LWW: беремо новіше, але ID завжди лишаємо локальним
                                if ((normalized.updatedAt ?: 0) > (existing.updatedAt ?: 0)) {
                                    normalized.copy(id = existing.id)
                                } else {
                                    existing
                                }
                            } else {
                                normalized
                            }
                        }

                    // Крок 4-6: Очищення таблиць та переіндексація
                    clearAllTables()

                    // Крок 7: Вставка базових сутностей
                    goalDao.insertGoals(backup.goals)
                    contextDao.insertContexts(cleanedProjects)

                    // Вставка ListItems з урахуванням мапінгу
                    val cleanedListItems =
                        backup.backlogItems.map {
                            it.copy(
                                contextId = contextIdMap[it.contextId] ?: it.contextId,
                                entityId =
                                    if (it.itemType == BacklogItemTypeValues.SUBLIST) {
                                        contextIdMap[it.entityId] ?: it.entityId
                                    } else {
                                        it.entityId
                                    },
                            )
                        }
                    listItemDao.insertItems(cleanedListItems)

                    // Вставка всього іншого (розширені сутності)
                    noteDocumentDao.insertAllDocuments(
                        backup.documents.map {
                            it.copy(
                                contextId = contextIdMap[it.contextId] ?: it.contextId,
                            )
                        },
                    )
                    attachmentDao.insertAttachments(
                        backup.attachments.map {
                            it.copy(
                                ownerContextId =
                                    it.ownerContextId?.let { cid ->
                                        contextIdMap[cid] ?: cid
                                    },
                            )
                        },
                    )

                    // Відновлення налаштувань
                    backupData.settings?.settings?.let { settingsRepository.restoreFromMap(it) }

                    // Post-migration
                    runPostBackupMigration()
                }
                Result.success("Success")
            } catch (e: Exception) {
                Log.e(TAG, "Import error", e)
                Result.failure(e)
            }

        // --- Повне завантаження локального контенту ---

        private suspend fun loadLocalDatabaseContent(): DatabaseContent {
            val recentProjectEntries =
                recentItemDao.getAll().map { recentItem ->
                    com.romankozak.forwardappmobile.data.sync.RecentProjectEntry(
                        contextId = recentItem.target,
                        timestamp = recentItem.lastAccessed,
                    )
                }
            val scripts = scriptDao.getAll().first()
            val listItems = listItemDao.getAll()
            val backlogOrders = logicHelper.dedupBacklogOrders(backlogOrderDao.getAll())

            return DatabaseContent(
                goals = goalDao.getAll(),
                projects = contextDao.getAll(),
                backlogItems = listItems,
                backlogOrders = backlogOrders,
                legacyNotes = legacyNoteDao.getAll(),
                documents = noteDocumentDao.getAllDocuments(),
                documentItems = noteDocumentDao.getAllDocumentItems(),
                checklists = checklistDao.getAllChecklists(),
                checklistItems = checklistDao.getAllChecklistItems(),
                activityRecords = activityRecordDao.getAllRecordsStream().first(),
                linkItemEntities = linkItemDao.getAllEntities(),
                inboxRecords = inboxRecordDao.getAll(),
                contextLogs = contextManagementDao.getAllLogs(),
                recentProjectEntries = recentProjectEntries,
                scripts = scripts,
                attachments = attachmentDao.getAll(),
                contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
            )
        }

        // --- Повне очищення бази даних (враховуючи ієрархію FK) ---

        private suspend fun clearAllTables() {
            // Спочатку видаляємо зв'язки та дочірні елементи
            contextManagementDao.deleteAllLogs()
            inboxRecordDao.deleteAll()
            linkItemDao.deleteAll()
            activityRecordDao.clearAll()
            listItemDao.deleteAll()

            noteDocumentDao.deleteAllDocumentItems()
            noteDocumentDao.deleteAllDocuments()
            checklistDao.deleteAllChecklistItems()
            checklistDao.deleteAllChecklists()

            attachmentDao.deleteAllContextAttachmentLinks()
            attachmentDao.deleteAll()

            dayTaskDao.deleteAllTasks()
            dayPlanDao.deleteAllPlans()
            dailyMetricDao.deleteAllMetrics()

            chatDao.deleteAllMessages()
            chatDao.deleteAllConversations()
            conversationFolderDao.deleteAllFolders()

            reminderDao.deleteAll()
            recurringTaskDao.deleteAll()

            contextArtifactDao.deleteAll()
            tacticalMissionDao.deleteAllMissionAttachmentCrossRefs()
            tacticalMissionDao.deleteAllMissions()

            aiEventDao.deleteAll()
            lifeSystemStateDao.deleteAll()
            aiInsightDao.clearAll()

            structurePresetItemDao.deleteAllItems()
            structurePresetDao.deleteAll()
            contextStructureDao.deleteAllItems()
            contextStructureDao.deleteAllStructures()

            systemAppDao.deleteAll()
            recentItemDao.deleteAll()
            scriptDao.deleteAll()

            // В останню чергу — головні сутності
            contextDao.deleteAll()
            goalDao.deleteAll()
        }

        // --- Маркування всіх сутностей як синхронізованих ---

        private suspend fun markSyncedNow(content: DatabaseContent) {
            val ts = System.currentTimeMillis()
            appDatabase.withTransaction {
                contextDao.insertContexts(content.projects.map { it.copy(syncedAt = ts) })
                goalDao.insertGoals(content.goals.map { it.copy(syncedAt = ts) })
                listItemDao.insertItems(content.backlogItems.map { it.copy(syncedAt = ts) })

                if (content.backlogOrders.isNotEmpty()) {
                    backlogOrderDao.insertOrders(content.backlogOrders.map { it.copy(syncedAt = ts) })
                }

                legacyNoteDao.insertAll(content.legacyNotes.map { it.copy(syncedAt = ts) })
                noteDocumentDao.insertAllDocuments(content.documents.map { it.copy(syncedAt = ts) })
                noteDocumentDao.insertAllDocumentItems(content.documentItems.map { it.copy(syncedAt = ts) })

                checklistDao.insertChecklists(content.checklists.map { it.copy(syncedAt = ts) })
                checklistDao.insertItems(content.checklistItems.map { it.copy(syncedAt = ts) })

                activityRecordDao.insertAll(content.activityRecords.map { it.copy(syncedAt = ts) })
                linkItemDao.insertAll(content.linkItemEntities.map { it.copy(syncedAt = ts) })
                inboxRecordDao.insertAll(content.inboxRecords.map { it.copy(syncedAt = ts) })
                contextManagementDao.insertAllLogs(content.contextLogs.map { it.copy(syncedAt = ts) })

                content.scripts.forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
                attachmentDao.insertAttachments(content.attachments.map { it.copy(syncedAt = ts) })
                attachmentDao.insertContextAttachmentLinks(
                    content.contextAttachmentCrossRefs.map {
                        it.copy(
                            syncedAt = ts,
                        )
                    },
                )
            }
        }

        // --- Отримання несинхронізованих змін ---

        suspend fun getUnsyncedChanges(): DatabaseContent {
            val local = loadLocalDatabaseContent()

            return DatabaseContent(
                projects =
                    local.projects.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                goals =
                    local.goals.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                backlogItems =
                    local.backlogItems.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                backlogOrders =
                    local.backlogOrders.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                legacyNotes =
                    local.legacyNotes.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                documents =
                    local.documents.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                documentItems =
                    local.documentItems.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                checklists =
                    local.checklists.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                checklistItems =
                    local.checklistItems.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                activityRecords =
                    local.activityRecords.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                linkItemEntities =
                    local.linkItemEntities.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                inboxRecords =
                    local.inboxRecords.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                contextLogs =
                    local.contextLogs.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                scripts =
                    local.scripts.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                attachments =
                    local.attachments.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                contextAttachmentCrossRefs =
                    local.contextAttachmentCrossRefs.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                recentProjectEntries = emptyList(),
            )
        }

        // --- Допоміжні методи інфраструктури ---

        private suspend fun buildWifiUrl(
            address: String,
            path: String,
        ): String {
            val cleanAddress = address.trim().let { if (it.startsWith("http")) it else "http://$it" }
            val uri = cleanAddress.toUri()
            val port = if (uri.port != -1) uri.port else settingsRepository.wifiSyncPortFlow.first()
            return "http://${uri.host}:$port$path"
        }

        private fun readTextFromUri(uri: Uri): String? =
            context.contentResolver.openInputStream(
                uri,
            )?.bufferedReader()?.use { it.readText() }

        private fun saveFileToDownloads(
            name: String,
            json: String,
        ) {
            val values =
                ContentValues().apply {
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

        // --- Експорт вкладень у файл ---

        suspend fun exportAttachmentsToFile(): Result<String> =
            try {
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

            // Використовуємо логіку синтезу для відновлення крос-лінковки, якщо вона пошкоджена
            val synthesizedCrossRefs =
                logicHelper.synthesizeMissingCrossRefs(
                    attachments = attachments,
                    existingCrossRefs = crossRefs,
                )

            val attachmentsBackup =
                AttachmentsBackup(
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

        // --- Імпорт вкладень з файлу ---

        suspend fun importAttachmentsFromFile(uri: Uri): Result<String> {
            val IMPORT_TAG = "SyncRepo_AttImport"
            try {
                val jsonString = readTextFromUri(uri) ?: throw Exception("File is empty")
                val backupData = gson.fromJson(jsonString, AttachmentsBackup::class.java)

                // Перевіряємо, які контексти (проекти) вже є в базі, щоб не створити "сиріт"
                val existingContextIds = contextDao.getAll().map { it.id }.toSet()

                appDatabase.withTransaction {
                    // 1. Імпортуємо документи нотаток (тільки для існуючих проектів)
                    val validDocs = backupData.documents.filter { it.contextId in existingContextIds }
                    noteDocumentDao.insertAllDocuments(validDocs)

                    val validDocIds = validDocs.map { it.id }.toSet()
                    noteDocumentDao.insertAllDocumentItems(backupData.documentItems.filter { it.listId in validDocIds })

                    // 2. Імпортуємо чеклисти
                    val validChecklists = backupData.checklists.filter { it.contextId in existingContextIds }
                    checklistDao.insertChecklists(validChecklists)

                    val validChecklistIds = validChecklists.map { it.id }.toSet()
                    checklistDao.insertItems(backupData.checklistItems.filter { it.checklistId in validChecklistIds })

                    // 3. Імпортуємо лінки
                    linkItemDao.insertAll(backupData.linkItemEntities)

                    // 4. Імпортуємо самі вкладення (Attachments)
                    // Якщо власник не знайдений, додаємо як "сироту" (ownerContextId = null)
                    val processedAttachments =
                        backupData.attachments.map { att ->
                            if (att.ownerContextId != null && att.ownerContextId !in existingContextIds) {
                                att.copy(ownerContextId = null)
                            } else {
                                att
                            }
                        }
                    attachmentDao.insertAttachments(processedAttachments)

                    // 5. Крос-посилання (тільки валідні пари)
                    val attachmentIds = processedAttachments.map { it.id }.toSet()
                    val validCrossRefs =
                        backupData.contextAttachmentCrossRefs.filter {
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

        // --- Отримання часу останньої успішної синхронізації ---

        suspend fun getLastSyncTime(): Long? {
            val local = loadLocalDatabaseContent()
            // Знаходимо найменший час синхронізації серед усіх ключових сутностей
            val allSyncedTimes =
                listOfNotNull(
                    local.projects.mapNotNull { it.syncedAt }.minOrNull(),
                    local.goals.mapNotNull { it.syncedAt }.minOrNull(),
                    local.documents.mapNotNull { it.syncedAt }.minOrNull(),
                    local.attachments.mapNotNull { it.syncedAt }.minOrNull(),
                    local.contextAttachmentCrossRefs.mapNotNull { it.syncedAt }.minOrNull(),
                    local.backlogOrders.mapNotNull { it.syncedAt }.minOrNull(),
                )
            return allSyncedTimes.minOrNull()
        }

        // --- Завантаження бекапу через Wi-Fi ---

        suspend fun fetchBackupFromWifi(
            address: String,
            deltaSince: Long? = null,
        ): Result<String> =
            try {
                Log.d(WIFI_LOG, "Fetching from $address, deltaSince=$deltaSince")

                val fullUrl =
                    buildWifiUrl(address, "/export").let { base ->
                        if (deltaSince != null) "$base?deltaSince=$deltaSince" else base
                    }

                val response: String = client.get(fullUrl).body()
                Log.d(WIFI_LOG, "Successfully fetched ${response.length} bytes")

                Result.success(response)
            } catch (e: Exception) {
                Log.e(WIFI_LOG, "Error fetching from Wi‑Fi", e)
                Result.failure(e)
            }

        // --- Створення звіту про зміни (Sync Report) ---

        suspend fun createSyncReport(jsonString: String): SyncReport {
            val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
            val incomingDb = backup.database ?: return SyncReport(emptyList())

            val localProjects = contextDao.getAll().associateBy { it.id }
            val localGoals = goalDao.getAll().associateBy { it.id }
            val localListItems = listItemDao.getAll().associateBy { it.id }

            val changes = mutableListOf<SyncChange>()

            // 1. Диференціація Цілей
            incomingDb.goals.forEach { incomingRaw ->
                val incoming = SyncMapper.normalizeGoal(incomingRaw)
                val local = localGoals[incoming.id]?.let { SyncMapper.normalizeGoal(it) }

                if (local == null) {
                    changes.add(SyncChange(ChangeType.Add, "Ціль", incoming.id, "Нова ціль: ${incoming.text}", entity = incoming))
                } else if (incoming.updatedTs() > local.updatedTs()) {
                    changes.add(SyncChange(ChangeType.Update, "Ціль", incoming.id, "Оновлено ціль: ${incoming.text}", entity = incoming))
                }
            }

            // 2. Диференціація Списків (Проектів)
            incomingDb.projects.filter { it.systemKey == null }.forEach { incomingRaw ->
                val incoming = SyncMapper.normalizeProject(incomingRaw)
                val local = localProjects[incoming.id]?.let { SyncMapper.normalizeProject(it) }

                if (local == null) {
                    changes.add(SyncChange(ChangeType.Add, "Список", incoming.id, "Новий список: ${incoming.name}", entity = incoming))
                } else if (incoming.updatedTs() > local.updatedTs()) {
                    changes.add(
                        SyncChange(ChangeType.Update, "Список", incoming.id, "Оновлено список: ${incoming.name}", entity = incoming),
                    )
                }
            }

            // 3. Обробка видалень (локальні, яких немає в бекапі)
            val incomingGoalIds = incomingDb.goals.map { it.id }.toSet()
            localGoals.keys.minus(incomingGoalIds).forEach { id ->
                localGoals[id]?.let {
                    changes.add(SyncChange(ChangeType.Delete, "Ціль", id, "Видалено ціль: ${it.text}", entity = it))
                }
            }

            return SyncReport(changes)
        }

        // --- Застосування вибраних змін зі звіту ---

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
                                is com.romankozak.forwardappmobile.features.contexts.data.models.Context ->
                                    contextDao.insert(change.entity)
                                is Goal ->
                                    goalDao.insertGoal(change.entity)
                                is BacklogItem ->
                                    listItemDao.insertItem(change.entity)
                            }
                        }
                    }
                }
            }
        }

        // --- Парсинг файлу бекапу ---

        suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> =
            withContext(Dispatchers.IO) {
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

        // --- Створення дельта-бекапу (тільки зміни з певного часу) ---

        suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
            val changes = getChangesSince(deltaSince)

            // Використовуємо логіку синтезу для вкладень, щоб уникнути дефектів при дельта-синхронізації
            val enrichedCrossRefs =
                logicHelper.synthesizeMissingCrossRefs(
                    attachments = changes.attachments,
                    existingCrossRefs = changes.contextAttachmentCrossRefs,
                )

            // Оновлюємо базу, щоб Android знав, що ці лінки тепер існують локально
            runCatching { attachmentDao.insertContextAttachmentLinks(enrichedCrossRefs) }

            val deltaBackup = FullAppBackup(database = changes.copy(contextAttachmentCrossRefs = enrichedCrossRefs))
            return gson.toJson(deltaBackup)
        }

        // --- Глибоке застосування змін (applyServerChanges) з виправленням дефектів ---

        suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
            val ts = System.currentTimeMillis()
            return try {
                Log.d(
                    TAG,
                    "[applyServerChanges] Incoming items: projects=${changes.projects.size}, attachments=${changes.attachments.size}",
                )

                appDatabase.withTransaction {
                    val local = loadLocalDatabaseContent()
                    val allProjectIds = local.projects.map { it.id }.toSet()

                    // 1. Аліасинг системних ID (Inbox, Archive тощо)
                    val idRedirects = mutableMapOf<String, String>()
                    val localSystemProjects = local.projects.filter { it.systemKey != null }.associateBy { it.systemKey!! }

                    val correctedIncomingProjects =
                        changes.projects.map { incoming ->
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

                    // 2. Злиття Списків та Цілей (LWW через LogicHelper)
                    val mergedContexts =
                        logicHelper.mergeAndMark(
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

                    val mergedGoals =
                        logicHelper.mergeAndMark(
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

                    // 3. Переіндексація дитячих сутностей через idRedirects
                    val contextIds = (allProjectIds + mergedContexts.map { it.id }).toSet()

                    // 4. Обробка Вкладень (Fix Defects #1, #3, #4, #5)
                    val processedAttachments =
                        changes.attachments.map { att ->
                            val newOwnerId = att.ownerContextId?.let { idRedirects[it] ?: it }
                            att.copy(ownerContextId = newOwnerId)
                        }.filter { it.ownerContextId == null || it.ownerContextId in contextIds }

                    val incomingAttachments =
                        logicHelper.mergeAndMark(
                            incoming = processedAttachments,
                            localMap = local.attachments.associateBy { it.id },
                            idSelector = { it.id },
                            versionSelector = { it.version },
                            updatedSelector = { it.updatedTs() },
                            markSynced = { at, s -> at.copy(syncedAt = s) },
                            syncedAt = ts,
                        )

                    // Defect #4 & #5: Перевіряємо локальні вкладення, які вже є, але потребують відмітки syncedAt
                    val alreadySyncedIds = incomingAttachments.map { it.id }.toSet()
                    val matchedExisting =
                        processedAttachments
                            .filter { it.id !in alreadySyncedIds }
                            .mapNotNull { inc -> local.attachments.find { it.id == inc.id } }
                            .map { it.copy(syncedAt = ts) }

                    attachmentDao.insertAttachments(incomingAttachments + matchedExisting)

                    // 5. Крос-посилання (CrossRefs)
                    val synthesizedRefs = logicHelper.synthesizeMissingCrossRefs(processedAttachments, changes.contextAttachmentCrossRefs)
                    val finalRefs =
                        synthesizedRefs.map { ref ->
                            val newCtxId = idRedirects[ref.contextId] ?: ref.contextId
                            ref.copy(contextId = newCtxId, syncedAt = ts)
                        }.filter { it.contextId in contextIds }

                    attachmentDao.insertContextAttachmentLinks(finalRefs)

                    // 6. Оновлення ListItems та BacklogOrders (дедуплікація)
                    val cleanedListItems =
                        changes.backlogItems.map {
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

        // --- Отримання змін для дельта-бекапу ---

        suspend fun getChangesSince(since: Long): DatabaseContent {
            val local = loadLocalDatabaseContent()

            return DatabaseContent(
                projects = local.projects.filter { (it.updatedTs()) > since },
                goals = local.goals.filter { (it.updatedTs()) > since },
                backlogItems = local.backlogItems.filter { (it.updatedTs()) > since },
                documents = local.documents.filter { (it.updatedTs()) > since },
                attachments = local.attachments.filter { (it.updatedTs()) > since },
                contextAttachmentCrossRefs = local.contextAttachmentCrossRefs.filter { (it.updatedTs()) > since },
                scripts = local.scripts.filter { (it.updatedTs()) > since },
                // Для дельти не передаємо повну історію
                recentProjectEntries = emptyList(),
            )
        }

        // --- Створення детальної різниці (Diff) між бекапом та локальною базою ---

        suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff {
            val local = loadLocalDatabaseContent()

            // Спрощуємо виклик через делегування до logicHelper.diffEntities
            return BackupDiff(
                projects =
                    logicHelper.diffEntities(
                        incomingList = incoming.projects.map { SyncMapper.normalizeProject(it) },
                        localList = local.projects,
                        idSelector = { it.id },
                        versionSelector = { it.version },
                        updatedSelector = { it.updatedTs() },
                        isDeletedSelector = { it.isDeleted },
                    ),
                goals =
                    logicHelper.diffEntities(
                        incomingList = incoming.goals.map { SyncMapper.normalizeGoal(it) },
                        localList = local.goals,
                        idSelector = { it.id },
                        versionSelector = { it.version },
                        updatedSelector = { it.updatedTs() },
                        isDeletedSelector = { it.isDeleted },
                    ),
                backlogItems =
                    logicHelper.diffEntities(
                        incomingList = incoming.backlogItems,
                        localList = local.backlogItems,
                        idSelector = { it.id },
                        versionSelector = { it.version },
                        updatedSelector = { it.updatedTs() },
                        isDeletedSelector = { it.isDeleted },
                    ),
                documents =
                    logicHelper.diffEntities(
                        incomingList = incoming.documents,
                        localList = local.documents,
                        idSelector = { it.id },
                        versionSelector = { it.version },
                        updatedSelector = { it.updatedTs() },
                        isDeletedSelector = { it.isDeleted },
                    ),
                attachments =
                    logicHelper.diffEntities(
                        incomingList = incoming.attachments,
                        localList = local.attachments,
                        idSelector = { it.id },
                        versionSelector = { it.version },
                        updatedSelector = { it.updatedTs() },
                        isDeletedSelector = { it.isDeleted },
                    ),
                contextAttachmentCrossRefs =
                    logicHelper.diffEntities(
                        incomingList = incoming.contextAttachmentCrossRefs,
                        localList = local.contextAttachmentCrossRefs,
                        idSelector = { "${it.contextId}-${it.attachmentId}" },
                        versionSelector = { it.version },
                        updatedSelector = { it.updatedTs() },
                        isDeletedSelector = { it.isDeleted },
                    ),
                // Додай інші сутності (scripts, notes) за аналогією
            )
        }

        // --- Імпорт вибраних даних (Selective Import) ---

        suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
            val IMPORT_TAG = "SyncRepo_Selective"
            return try {
                val local = loadLocalDatabaseContent()
                val ts = System.currentTimeMillis()

                // Допоміжна функція для LWW фільтрації
                fun <T> filterNewer(
                    incoming: List<T>,
                    localMap: Map<String, T>,
                    idSelector: (T) -> String,
                    versionSelector: (T) -> Long,
                    updatedAtSelector: (T) -> Long,
                ): List<T> =
                    incoming.filter { inc ->
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

                    // 1. Проекти (пропускаємо системні, вони керуються локально)
                    val regularProjects = selectedData.projects.filter { it.systemKey == null }
                    val newerProjects =
                        filterNewer(
                            regularProjects,
                            local.projects.associateBy { it.id },
                            { it.id },
                            { it.version },
                            { it.updatedTs() },
                        )
                    if (newerProjects.isNotEmpty()) {
                        contextDao.insertContexts(newerProjects.map { it.copy(syncedAt = ts) })
                    }

                    // 2. Цілі
                    val newerGoals =
                        filterNewer(
                            selectedData.goals,
                            local.goals.associateBy { it.id },
                            { it.id },
                            { it.version },
                            { it.updatedTs() },
                        )
                    if (newerGoals.isNotEmpty()) {
                        goalDao.insertGoals(newerGoals.map { it.copy(syncedAt = ts) })
                    }

                    // 3. Елементи беклогу (тільки ті, що мають валідні батьківські проекти/цілі)
                    val currentContextIds = (local.projects.map { it.id } + newerProjects.map { it.id }).toSet()
                    val currentGoalIds = (local.goals.map { it.id } + newerGoals.map { it.id }).toSet()

                    val validListItems =
                        selectedData.backlogItems.filter {
                            it.contextId in currentContextIds || it.entityId in currentGoalIds
                        }
                    listItemDao.insertItems(validListItems.map { it.copy(syncedAt = ts) })

                    // 4. Вкладення та Крос-посилання
                    if (selectedData.attachments.isNotEmpty()) {
                        val newerAttachments =
                            filterNewer(
                                selectedData.attachments,
                                local.attachments.associateBy { it.id },
                                { it.id },
                                { it.version },
                                { it.updatedTs() },
                            )
                        attachmentDao.insertAttachments(newerAttachments.map { it.copy(syncedAt = ts) })
                    }

                    if (selectedData.contextAttachmentCrossRefs.isNotEmpty()) {
                        val validCrossRefs =
                            selectedData.contextAttachmentCrossRefs.filter {
                                it.contextId in currentContextIds
                            }
                        attachmentDao.insertContextAttachmentLinks(validCrossRefs.map { it.copy(syncedAt = ts) })
                    }

                    // 5. Скрипти
                    if (selectedData.scripts.isNotEmpty()) {
                        selectedData.scripts.forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
                    }
                }

                Result.success("Вибрані дані успішно імпортовано.")
            } catch (e: Exception) {
                Log.e(IMPORT_TAG, "Critical error during selective import", e)
                Result.failure(e)
            }
        }

        /**
         * Відправляє всі локальні зміни (unsynced) на віддалений пристрій (наприклад, Desktop) через Wi-Fi.
         * Після успішної відправки маркує ці дані як синхронізовані (syncedAt = currentTs).
         */
        suspend fun pushUnsyncedToWifi(address: String): Result<Unit> =
            try {
                Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Початок процесу. Адреса: $address")

                // 1. Отримуємо всі локальні зміни, що потребують синхронізації
                val unsynced = getUnsyncedChanges()

                Log.d(
                    WIFI_SYNC_TAG,
                    "[pushUnsyncedToWifi] Знайдено змін: " +
                        "projects=${unsynced.projects.size}, " +
                        "goals=${unsynced.goals.size}, " +
                        "listItems=${unsynced.backlogItems.size}, " +
                        "attachments=${unsynced.attachments.size}",
                )

                // Якщо змін немає, завершуємо роботу раніше, щоб не навантажувати мережу
                if (unsynced.projects.isEmpty() &&
                    unsynced.goals.isEmpty() &&
                    unsynced.backlogItems.isEmpty() &&
                    unsynced.attachments.isEmpty() &&
                    unsynced.documents.isEmpty()
                ) {
                    Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Несинхронізованих даних не знайдено. Пропускаємо.")
                    Result.success(Unit)
                } else {
                    // 2. Формуємо URL та payload
                    val fullUrl = buildWifiUrl(address, "/import")
                    val backupWrapper = FullAppBackup(database = unsynced)
                    val payload = gson.toJson(backupWrapper)

                    // Записуємо дамп для дебагу (опціонально)
                    // writeDebugDump("push_export", payload)

                    Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] POST запит на $fullUrl. Розмір payload: ${payload.length} байт")

                    // 3. Відправляємо дані на сервер
                    val response =
                        client.post(fullUrl) {
                            contentType(ContentType.Application.Json)
                            setBody(payload)
                        }

                    if (response.status.isSuccess()) {
                        Log.d(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Дані успішно прийнято сервером. Оновлюємо локальний статус.")

                        // 4. Оновлюємо syncedAt у локальній базі, щоб не відправляти це знову наступного разу
                        markSyncedNow(unsynced)

                        Result.success(Unit)
                    } else {
                        val errorMsg = "Сервер повернув помилку: ${response.status.value}"
                        Log.e(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] $errorMsg")
                        Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Log.e(WIFI_SYNC_TAG, "[pushUnsyncedToWifi] Критична помилка під час синхронізації", e)
                Result.failure(e)
            }
    }
