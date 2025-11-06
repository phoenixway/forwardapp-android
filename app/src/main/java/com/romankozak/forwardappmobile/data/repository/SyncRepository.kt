package com.romankozak.forwardappmobile.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.data.sync.ReservedGroupAdapter
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectLogLevelValues
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectStatusValues
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.data.database.DatabaseInitializer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.gson.gson
import com.romankozak.forwardappmobile.data.sync.DesktopBackupFile
import com.romankozak.forwardappmobile.data.sync.toGoal
import com.romankozak.forwardappmobile.data.sync.toProject
import com.romankozak.forwardappmobile.features.projects.data.toShared
import com.romankozak.forwardappmobile.features.projects.data.toEntity
import com.romankozak.forwardappmobile.shared.database.RecentItemQueriesQueries
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val FULL_IMPORT_TAG = "FullImportFlow"

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
    private val projectLocalDataSource: ProjectLocalDataSource,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val activityRecordDao: ActivityRecordDao,
    private val inboxRecordDao: InboxRecordDao,
    private val settingsRepository: SettingsRepository,
    private val projectManagementDao: ProjectManagementDao,
    private val legacyNoteDao: LegacyNoteDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val checklistDao: ChecklistDao,
    private val recentItemQueries: RecentItemQueriesQueries,
    private val attachmentRepository: AttachmentRepository,
    private val databaseInitializer: DatabaseInitializer,
) {
    private val TAG = "SyncRepository"
    private val gson = GsonBuilder()
        .registerTypeAdapter(com.romankozak.forwardappmobile.data.database.models.ReservedGroup::class.java, ReservedGroupAdapter())
        .create()
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
                projects = projectLocalDataSource.getAll().map { it.toEntity() },
                listItems = listItemDao.getAll(),
                legacyNotes = legacyNoteDao.getAll(),
                documents = noteDocumentDao.getAllDocuments(),
                documentItems = noteDocumentDao.getAllDocumentItems(),
                checklists = checklistDao.getAllChecklists(),
                checklistItems = checklistDao.getAllChecklistItems(),
                activityRecords = activityRecordDao.getAllRecordsStream().first(),
                linkItemEntities = linkItemDao.getAllEntities(),
                inboxRecords = inboxRecordDao.getAll(),
                projectExecutionLogs = projectManagementDao.getAllLogs(),
                recentProjectEntries = null
            )
        val settingsMap = settingsRepository.getPreferencesSnapshot().asMap().mapKeys { it.key.name }
            .mapValues { it.value.toString() }
        val settingsContent = com.romankozak.forwardappmobile.data.sync.SettingsContent(settings = settingsMap)

        val fullBackup = FullAppBackup(database = databaseContent, settings = settingsContent)
        return gson.toJson(fullBackup)
    }

    suspend fun importFullBackupFromFile(uri: Uri): Result<String> {
        val IMPORT_TAG = "SyncRepository_IMPORT"
        try {
            Log.d(FULL_IMPORT_TAG, "importFullBackupFromFile invoked with uri=$uri on thread ${Thread.currentThread().name}")
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

            Log.d(IMPORT_TAG, "Отриманий JSON. Довжина = ${jsonString.length} символів.")
            Log.d(IMPORT_TAG, "Починаємо розбір JSON в об'єкт FullAppBackup...")
            val parseStartTime = System.currentTimeMillis()
            val backupData =
                try {
                    gson.fromJson(jsonString, FullAppBackup::class.java)
                } catch (parseError: Exception) {
                    Log.e(IMPORT_TAG, "Не вдалося розпарсити JSON бекапу.", parseError)
                    return Result.failure(parseError)
                }
            Log.d(
                IMPORT_TAG,
                "JSON успішно розібрано за ${System.currentTimeMillis() - parseStartTime} мс.",
            )

            val rawBackupVersion = backupData.backupSchemaVersion
            val normalizedBackupVersion = if (rawBackupVersion == 0) 1 else rawBackupVersion
            if (normalizedBackupVersion != 1) {
                val message = "Unsupported backup version: $rawBackupVersion. Expected version 1."
                Log.e(IMPORT_TAG, message)
                return Result.failure(Exception(message))
            }
            Log.d(IMPORT_TAG, "Версія бекапу підтримується: $rawBackupVersion (normalized=$normalizedBackupVersion).")

            val backup = backupData.database
            Log.d(
                IMPORT_TAG,
                "Структура бекапу: projects=${backup.projects.size}, goals=${backup.goals.size}, " +
                    "listItems=${backup.listItems.size}, noteDocs=${backup.documents?.size}, " +
                    "checklists=${backup.checklists?.size}, linkItems=${backup.linkItemEntities?.size}",
            )

            val backupSettingsMap = backupData.settings?.settings ?: emptyMap()

            Log.d(IMPORT_TAG, "Починаємо очищення даних для сумісності...")
            val cleanedProjects =
                backup.projects.map { projectFromBackup ->
                    projectFromBackup.copy(
                        projectType = projectFromBackup.projectType ?: ProjectType.DEFAULT,
                        reservedGroup = com.romankozak.forwardappmobile.data.database.models.ReservedGroup.fromString(projectFromBackup.reservedGroup?.groupName),
                        defaultViewModeName = projectFromBackup.defaultViewModeName ?: ProjectViewMode.BACKLOG.name,
                        isProjectManagementEnabled = projectFromBackup.isProjectManagementEnabled ?: false,
                        projectStatus = projectFromBackup.projectStatus ?: ProjectStatusValues.NO_PLAN,
                        projectStatusText = projectFromBackup.projectStatusText ?: "",
                        projectLogLevel = projectFromBackup.projectLogLevel ?: ProjectLogLevelValues.NORMAL,
                        totalTimeSpentMinutes = projectFromBackup.totalTimeSpentMinutes ?: 0,
                        scoringStatus = projectFromBackup.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
                        valueImportance = projectFromBackup.valueImportance,
                        valueImpact = projectFromBackup.valueImpact,
                        effort = projectFromBackup.effort,
                        cost = projectFromBackup.cost,
                        risk = projectFromBackup.risk,
                        weightEffort = projectFromBackup.weightEffort,
                        weightCost = projectFromBackup.weightCost,
                        weightRisk = projectFromBackup.weightRisk,
                        rawScore = projectFromBackup.rawScore,
                        displayScore = projectFromBackup.displayScore,
                    )
                }.map { it.toShared() }
            Log.d(IMPORT_TAG, "Очищення даних Project завершено.")

            val cleanedListItems =
                backup.listItems.mapNotNull { item ->
                    if (item.id.isBlank() || item.projectId.isBlank() || item.entityId.isBlank()) {
                        Log.w(IMPORT_TAG, "Skipping invalid ListItem due to blank ID(s): $item")
                        null
                    } else {
                        item
                    }
                }
            Log.d(IMPORT_TAG, "Очищення ListItem завершено. Original: ${backup.listItems.size}, Cleaned: ${cleanedListItems.size}")

            val attachmentItemTypes = setOf(
                ListItemTypeValues.NOTE_DOCUMENT,
                ListItemTypeValues.CHECKLIST,
                ListItemTypeValues.LINK_ITEM,
            )
            val (attachmentListItems, backlogListItems) =
                cleanedListItems.partition { it.itemType in attachmentItemTypes }

            val recentItemsToInsert = backup.recentProjectEntries?.mapNotNull { entry ->
                val project = backup.projects.find { it.id == entry.projectId }
                if (project != null) {
                    com.romankozak.forwardappmobile.data.database.models.RecentItem(
                        id = project.id,
                        type = com.romankozak.forwardappmobile.data.database.models.RecentItemType.PROJECT,
                        lastAccessed = entry.timestamp,
                        displayName = project.name,
                        target = project.id
                    )
                } else {
                    null
                }
            }

            val backupChecklists = backup.checklists.orEmpty()
            val backupChecklistItems = backup.checklistItems.orEmpty()

        appDatabase.withTransaction {
            Log.d(FULL_IMPORT_TAG, "Room transaction started")
            Log.d(IMPORT_TAG, "Транзакція: очищення таблиць.")
            Log.d(FULL_IMPORT_TAG, "TX stage: clearing project logs")
            projectManagementDao.deleteAllLogs()
            Log.d(FULL_IMPORT_TAG, "TX stage: project logs cleared")
            Log.d(IMPORT_TAG, "  - logs очищено")
            inboxRecordDao.deleteAll()
            Log.d(FULL_IMPORT_TAG, "TX stage: inbox cleared")
            Log.d(IMPORT_TAG, "  - inbox очищено")
            linkItemDao.deleteAll()
            Log.d(FULL_IMPORT_TAG, "TX stage: link items cleared")
            Log.d(IMPORT_TAG, "  - link_items очищено")
            activityRecordDao.clearAll()
            Log.d(FULL_IMPORT_TAG, "TX stage: activity records cleared")
            Log.d(IMPORT_TAG, "  - activity очищено")
            listItemDao.deleteAll()
            Log.d(FULL_IMPORT_TAG, "TX stage: list items cleared")
            Log.d(IMPORT_TAG, "  - list_items очищено")
            projectLocalDataSource.deleteAllWithinTransaction()
            Log.d(FULL_IMPORT_TAG, "TX stage: projects cleared")
            Log.d(IMPORT_TAG, "  - projects очищено")
            goalDao.deleteAll()
            Log.d(FULL_IMPORT_TAG, "TX stage: goals cleared")
            Log.d(IMPORT_TAG, "  - goals очищено")
            legacyNoteDao.deleteAll()
            Log.d(FULL_IMPORT_TAG, "TX stage: legacy notes cleared")
            Log.d(IMPORT_TAG, "  - legacy_notes очищено")
            noteDocumentDao.deleteAllDocuments()
            Log.d(FULL_IMPORT_TAG, "TX stage: note documents cleared")
            Log.d(IMPORT_TAG, "  - note_documents очищено")
            noteDocumentDao.deleteAllDocumentItems()
            Log.d(FULL_IMPORT_TAG, "TX stage: note document items cleared")
            Log.d(IMPORT_TAG, "  - note_document_items очищено")
            checklistDao.deleteAllChecklistItems()
            Log.d(FULL_IMPORT_TAG, "TX stage: checklist items cleared")
            Log.d(IMPORT_TAG, "  - checklist_items очищено")
            checklistDao.deleteAllChecklists()
            Log.d(FULL_IMPORT_TAG, "TX stage: checklists cleared")
            Log.d(IMPORT_TAG, "  - checklists очищено")
            recentItemQueries.deleteAllRecentItems()
            Log.d(FULL_IMPORT_TAG, "TX stage: recent items cleared")
            Log.d(IMPORT_TAG, "  - recent_items очищено")

                Log.d(IMPORT_TAG, "Транзакція: вставка базових сутностей.")
                Log.d(IMPORT_TAG, "  -> Inserting goals: ${backup.goals.size}")
                goalDao.insertGoals(backup.goals)
                Log.d(IMPORT_TAG, "  - goals вставлено: ${backup.goals.size}")

                Log.d(IMPORT_TAG, "  -> Inserting projects: ${cleanedProjects.size}")
                projectLocalDataSource.upsert(cleanedProjects, useTransaction = true)
                Log.d(IMPORT_TAG, "  - projects вставлено: ${cleanedProjects.size}")

                Log.d(IMPORT_TAG, "  -> Inserting backlogListItems: ${backlogListItems.size}")
                listItemDao.insertItems(backlogListItems)
                Log.d(FULL_IMPORT_TAG, "TX stage: base entities inserted")
                Log.d(IMPORT_TAG, "  - list_items вставлено: ${backlogListItems.size}")

                backup.legacyNotes?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting legacyNotes: ${it.size}")
                    legacyNoteDao.insertAll(it.orEmpty())
                    Log.d(IMPORT_TAG, "  - legacy_notes вставлено: ${it.size}")
                }
                backup.documents?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting documents: ${it.size}")
                    noteDocumentDao.insertAllDocuments(it.orEmpty())
                    Log.d(IMPORT_TAG, "  - documents вставлено: ${it.size}")
                }
                backup.documentItems?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting documentItems: ${it.size}")
                    noteDocumentDao.insertAllDocumentItems(it.orEmpty())
                    Log.d(IMPORT_TAG, "  - document_items вставлено: ${it.size}")
                }
                if (backupChecklists.isNotEmpty()) {
                    Log.d(IMPORT_TAG, "  -> Inserting checklists: ${backupChecklists.size}")
                    checklistDao.insertChecklists(backupChecklists)
                    Log.d(IMPORT_TAG, "  - checklists вставлено: ${backupChecklists.size}")
                }
                if (backupChecklistItems.isNotEmpty()) {
                    Log.d(IMPORT_TAG, "  -> Inserting checklistItems: ${backupChecklistItems.size}")
                    checklistDao.insertItems(backupChecklistItems)
                    Log.d(IMPORT_TAG, "  - checklist_items вставлено: ${backupChecklistItems.size}")
                }

                backup.activityRecords?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting activityRecords: ${it.size}")
                    activityRecordDao.insertAll(it)
                    Log.d(IMPORT_TAG, "  - activity_records вставлено: ${it.size}")
                }
                backup.linkItemEntities?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting linkItemEntities: ${it.size}")
                    linkItemDao.insertAll(it)
                    Log.d(IMPORT_TAG, "  - link_items вставлено: ${it.size}")
                }
                backup.inboxRecords?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting inboxRecords: ${it.size}")
                    inboxRecordDao.insertAll(it)
                    Log.d(IMPORT_TAG, "  - inbox_records вставлено: ${it.size}")
                }
                backup.projectExecutionLogs?.let {
                    Log.d(IMPORT_TAG, "  -> Inserting projectExecutionLogs: ${it.size}")
                    projectManagementDao.insertAllLogs(it)
                    Log.d(IMPORT_TAG, "  - project_logs вставлено: ${it.size}")
                }
                recentItemsToInsert?.let { items ->
                    Log.d(IMPORT_TAG, "  -> Inserting recentItems: ${items.size}")
                    items.forEach { recent ->
                        recentItemQueries.insertRecentItem(
                            id = recent.id,
                            type = recent.type.name,
                            lastAccessed = recent.lastAccessed,
                            displayName = recent.displayName,
                            target = recent.target,
                            isPinned = if (recent.isPinned) 1L else 0L,
                        )
                    }
                    Log.d(FULL_IMPORT_TAG, "TX stage: secondary entities inserted (${items.size})")
                    Log.d(IMPORT_TAG, "  - recent_items вставлено: ${items.size}")
                }

                if (attachmentListItems.isNotEmpty()) {
                    Log.d(IMPORT_TAG, "Транзакція: формування attachments із legacy list_items: ${attachmentListItems.size}")
                    val orderUpdatesByProject = mutableMapOf<String, MutableList<Pair<String, Long>>>()
                    attachmentListItems.forEach { item ->
                        val createdAt = if (item.order < 0) -item.order else System.currentTimeMillis()
                        val attachment =
                            attachmentRepository.ensureAttachmentLinkedToProject(
                                attachmentType = item.itemType,
                                entityId = item.entityId,
                                projectId = item.projectId,
                                ownerProjectId = item.projectId,
                                createdAt = createdAt,
                            )
                        orderUpdatesByProject.getOrPut(item.projectId) { mutableListOf() }
                            .add(attachment.id to item.order)
                    }
                    orderUpdatesByProject.forEach { (projectId, updates) ->
                        attachmentRepository.updateAttachmentOrders(projectId, updates)
                    }
                    Log.d(IMPORT_TAG, "Транзакція: attachments сформовано для ${orderUpdatesByProject.size} проєктів")
                    Log.d(FULL_IMPORT_TAG, "TX stage: attachments synchronized (${orderUpdatesByProject.size})")
                }

                Log.d(IMPORT_TAG, "Транзакція: відновлення settings. Entries=${backupSettingsMap.size}")
                settingsRepository.restoreFromMap(backupSettingsMap)
                Log.d(IMPORT_TAG, "Транзакція: запуск DatabaseInitializer.prePopulate().")
                databaseInitializer.prePopulate()
                Log.d(IMPORT_TAG, "Транзакція: вставка завершена.")
                Log.d(FULL_IMPORT_TAG, "Room transaction committed")
            }

        runPostBackupMigration()

        Log.i(IMPORT_TAG, "Імпорт бекапу успішно завершено.")
        Log.i(FULL_IMPORT_TAG, "Backup import finished successfully")
        return Result.success("Backup imported successfully!")
    } catch (e: Exception) {
        Log.e(IMPORT_TAG, "Під час імпорту сталася критична помилка.", e)
        Log.e(FULL_IMPORT_TAG, "Backup import failed", e)
        return Result.failure(e)
    }
}

    suspend fun fetchBackupFromWifi(address: String): Result<String> =
        try {
            var cleanAddress = address.trim()
            if (!cleanAddress.startsWith("http://") && !cleanAddress.startsWith("https://")) {
                cleanAddress = "http://$cleanAddress"
            }
            val uri = cleanAddress.toUri()
            val hostAndPort = "${uri.host}:${if (uri.port != -1) uri.port else 8080}"
            val fullUrl = "http://$hostAndPort/export"
            Log.e(TAG, "[fetchBackupFromWifi] Fetching from: $fullUrl")
            val response: String = client.get(fullUrl).body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from WiFi", e)
            Result.failure(e)
        }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        val desktopData = gson.fromJson(jsonString, DesktopBackupFile::class.java).data ?: return SyncReport(emptyList())

        val localGoals = goalDao.getAll().associateBy { it.id }
        val localProjects = projectLocalDataSource.getAll().associateBy { it.id }

        val changes = mutableListOf<SyncChange>()

        // Goals
        desktopData.goals?.forEach { (id, desktopGoal) ->
            val localGoal = localGoals[id]
            if (localGoal == null) {
                changes.add(SyncChange(ChangeType.Add, "Ціль", id, "Нова ціль: ${desktopGoal.text}", entity = desktopGoal.toGoal()))
            } else {
                val desktopUpdatedAt = desktopGoal.updatedAt?.let { try { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) { 0L } } ?: 0L
                if (desktopUpdatedAt > (localGoal.updatedAt ?: 0)) {
                    val updates = mutableListOf<String>()
                    if (desktopGoal.text != localGoal.text) updates.add("text changed")
                    if (desktopGoal.completed != localGoal.completed) updates.add("completion status changed")
                    if (desktopGoal.description != localGoal.description) updates.add("description changed")
                    if (desktopGoal.tags?.toSet() != localGoal.tags?.toSet()) updates.add("tags changed")
                    if (desktopGoal.valueImportance != localGoal.valueImportance) updates.add("valueImportance changed")
                    if (desktopGoal.valueImpact != localGoal.valueImpact) updates.add("valueImpact changed")
                    if (desktopGoal.effort != localGoal.effort) updates.add("effort changed")
                    if (desktopGoal.cost != localGoal.cost) updates.add("cost changed")
                    if (desktopGoal.risk != localGoal.risk) updates.add("risk changed")
                    if (desktopGoal.scoringStatus != localGoal.scoringStatus) updates.add("scoringStatus changed")

                    if (updates.isNotEmpty()) {
                        changes.add(
                            SyncChange(
                                ChangeType.Update,
                                "Ціль",
                                id,
                                "Оновлено ціль: ${desktopGoal.text}",
                                longDescription = "Зміни: ${updates.joinToString()}",
                                entity = desktopGoal.toGoal()
                            )
                        )
                    }
                }
            }
        }
        localGoals.keys.minus(desktopData.goals?.keys ?: emptySet()).forEach { id ->
            changes.add(SyncChange(ChangeType.Delete, "Ціль", id, "Видалено ціль: ${localGoals[id]?.text}", entity = localGoals[id]!!))
        }

        // Projects
        desktopData.goalLists?.forEach { (id, desktopProject) ->
            val localProject = localProjects[id]
            if (localProject == null) {
                changes.add(SyncChange(ChangeType.Add, "Список", id, "Новий список: ${desktopProject.name}", entity = desktopProject.toProject()))
            } else {
                val desktopUpdatedAt = desktopProject.updatedAt?.let { try { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) { 0L } } ?: 0L
                if (desktopUpdatedAt > (localProject.updatedAt ?: 0)) {
                    val updates = mutableListOf<String>()
                    if (desktopProject.name != localProject.name) updates.add("name to '${desktopProject.name}'")
                    if (desktopProject.parentId != localProject.parentId) updates.add("parent changed")
                    if (desktopProject.description != localProject.description) updates.add("description changed")
                    if (desktopProject.isExpanded != localProject.isExpanded) updates.add("isExpanded changed to ${desktopProject.isExpanded}")
                    if (desktopProject.order?.toLong() != localProject.order) updates.add("order changed")
                    if (desktopProject.tags?.toSet() != localProject.tags?.toSet()) updates.add("tags changed")
                    if (desktopProject.isCompleted != localProject.isCompleted) updates.add("completion status changed")
                    if (desktopProject.valueImportance != localProject.valueImportance) updates.add("valueImportance changed")
                    if (desktopProject.valueImpact != localProject.valueImpact) updates.add("valueImpact changed")
                    if (desktopProject.effort != localProject.effort) updates.add("effort changed")
                    if (desktopProject.cost != localProject.cost) updates.add("cost changed")
                    if (desktopProject.risk != localProject.risk) updates.add("risk changed")
                    if (desktopProject.scoringStatus != localProject.scoringStatus) updates.add("scoringStatus changed")

                    if (updates.isNotEmpty()) {
                        changes.add(
                            SyncChange(
                                ChangeType.Update,
                                "Список",
                                id,
                                "Оновлено список: ${desktopProject.name}",
                                longDescription = "Зміни: ${updates.joinToString()}",
                                entity = desktopProject.toProject()
                            )
                        )
                    }
                }
            }
        }
        localProjects.keys.minus(desktopData.goalLists?.keys ?: emptySet()).forEach { id ->
            changes.add(SyncChange(ChangeType.Delete, "Список", id, "Видалено список: ${localProjects[id]?.name}", entity = localProjects[id]!!))
        }

        return SyncReport(changes)
    }

    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        val changesByType = approvedChanges.groupBy { it.type }

        changesByType[ChangeType.Delete]?.forEach { change ->
            when (change.entityType) {
                "Привʼязка" -> listItemDao.deleteItemsByIds(listOf(change.id))
                "Список" -> projectLocalDataSource.deleteDefault(change.id)
                "Ціль" -> goalDao.deleteGoalById(change.id)
            }
        }

        changesByType[ChangeType.Update]?.forEach { change ->
            when (change.entityType) {
                "Список" -> projectLocalDataSource.upsert(change.entity as Project)
                "Ціль" -> goalDao.updateGoal(change.entity as Goal)
            }
        }

        val addsAndMoves = (changesByType[ChangeType.Add] ?: emptyList()) + (changesByType[ChangeType.Move] ?: emptyList())
        addsAndMoves.forEach { change ->
            when (change.entityType) {
                "Список" -> projectLocalDataSource.upsert(change.entity as Project)
                "Ціль" -> goalDao.insertGoal(change.entity as Goal)
                "Привʼязка" -> listItemDao.insertItem(change.entity as ListItem)
            }
        }
    }

    suspend fun runPostBackupMigration() {
        Log.d(TAG, "runPostBackupMigration: Starting post-backup migration")
        val db = appDatabase.openHelper.writableDatabase
        com.romankozak.forwardappmobile.data.database.migrateSpecialProjects(db)
        Log.d(TAG, "runPostBackupMigration: Finished post-backup migration")
    }
}
