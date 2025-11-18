package com.romankozak.forwardappmobile.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.Project;
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues;
import com.romankozak.forwardappmobile.data.database.models.ProjectLogLevelValues;
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues;
import com.romankozak.forwardappmobile.data.database.models.ProjectType;
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.DesktopBackupFile
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.AttachmentsBackup
import com.romankozak.forwardappmobile.data.sync.ReservedGroupAdapter
import com.romankozak.forwardappmobile.data.sync.toGoal
import com.romankozak.forwardappmobile.data.sync.toProject
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
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
    private val inboxRecordDao: InboxRecordDao,
    private val settingsRepository: SettingsRepository,
    private val projectManagementDao: ProjectManagementDao,
    private val legacyNoteDao: LegacyNoteDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val checklistDao: ChecklistDao,
    private val recentItemDao: RecentItemDao,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentDao: AttachmentDao,
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
                projects = projectDao.getAll(),
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
                recentProjectEntries = emptyList(),
                attachments = attachmentDao.getAll(),
                projectAttachmentCrossRefs = attachmentDao.getAllProjectAttachmentCrossRefs(),
            )
        val settingsMap = settingsRepository.getPreferencesSnapshot().asMap().mapKeys { it.key.name }
            .mapValues { it.value.toString() }
        val settingsContent = com.romankozak.forwardappmobile.data.sync.SettingsContent(settings = settingsMap)

        val fullBackup = FullAppBackup(database = databaseContent, settings = settingsContent)
        return gson.toJson(fullBackup)
    }

    suspend fun exportAttachmentsToFile(): Result<String> =
        try {
            val backupJson = createAttachmentsBackupJsonString()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "forward_app_attachments_backup_$timestamp.json"

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
                Result.success("Attachments backup saved to Downloads/ForwardApp.")
            } else {
                Result.failure(Exception("Failed to create file for attachments backup."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting attachments", e)
            Result.failure(e)
        }

    suspend fun createAttachmentsBackupJsonString(): String {
        val attachmentsBackup =
            AttachmentsBackup(
                documents = noteDocumentDao.getAllDocuments(),
                documentItems = noteDocumentDao.getAllDocumentItems(),
                checklists = checklistDao.getAllChecklists(),
                checklistItems = checklistDao.getAllChecklistItems(),
                linkItemEntities = linkItemDao.getAllEntities(),
                attachments = attachmentDao.getAll(),
                projectAttachmentCrossRefs = attachmentDao.getAllProjectAttachmentCrossRefs(),
            )
        return gson.toJson(attachmentsBackup)
    }

    suspend fun importAttachmentsFromFile(uri: Uri): Result<String> {
        val IMPORT_TAG = "SyncRepo_AttachmentsImport"
        try {
            Log.d(IMPORT_TAG, "Starting attachments import from URI: $uri")
            val jsonString =
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    .use { it?.readText() }

            if (jsonString.isNullOrBlank()) {
                Log.e(IMPORT_TAG, "Error: Attachments backup file is empty or could not be read.")
                return Result.failure(Exception("Attachments backup file is empty or could not be read."))
            }
            Log.d(IMPORT_TAG, "File read successfully. JSON size: ${jsonString.length} chars.")

            val backupData =
                try {
                    gson.fromJson(jsonString, com.romankozak.forwardappmobile.data.sync.AttachmentsBackup::class.java)
                } catch (parseError: Exception) {
                    Log.e(IMPORT_TAG, "Failed to parse attachments backup JSON.", parseError)
                    return Result.failure(parseError)
                }
            Log.d(IMPORT_TAG, "JSON parsed successfully.")

            val existingProjectIds = projectDao.getAll().map { it.id }.toSet()
            Log.d(IMPORT_TAG, "Found ${existingProjectIds.size} existing projects in the database.")

            val validCrossRefs = backupData.projectAttachmentCrossRefs.filter { it.projectId in existingProjectIds }
            val orphanedCrossRefsCount = backupData.projectAttachmentCrossRefs.size - validCrossRefs.size
            Log.d(IMPORT_TAG, "Found ${validCrossRefs.size} valid attachment links. $orphanedCrossRefsCount orphaned links will be skipped.")

            val validDocuments = backupData.documents.filter { it.projectId in existingProjectIds }
            val orphanedDocumentsCount = backupData.documents.size - validDocuments.size
            if (orphanedDocumentsCount > 0) {
                Log.d(IMPORT_TAG, "Found ${validDocuments.size} valid documents. $orphanedDocumentsCount orphaned documents will be skipped.")
            }

            val validDocumentIds = validDocuments.map { it.id }.toSet()
            val validDocumentItems = backupData.documentItems.filter { it.listId in validDocumentIds }
            val orphanedDocumentItemsCount = backupData.documentItems.size - validDocumentItems.size
            if (orphanedDocumentItemsCount > 0) {
                Log.d(IMPORT_TAG, "Found ${validDocumentItems.size} valid document items. $orphanedDocumentItemsCount orphaned items will be skipped.")
            }

            val validChecklists = backupData.checklists.filter { it.projectId in existingProjectIds }
            val orphanedChecklistsCount = backupData.checklists.size - validChecklists.size
            if (orphanedChecklistsCount > 0) {
                Log.d(IMPORT_TAG, "Found ${validChecklists.size} valid checklists. $orphanedChecklistsCount orphaned checklists will be skipped.")
            }

            val validChecklistIds = validChecklists.map { it.id }.toSet()
            val validChecklistItems = backupData.checklistItems.filter { it.checklistId in validChecklistIds }
            val orphanedChecklistItemsCount = backupData.checklistItems.size - validChecklistItems.size
            if (orphanedChecklistItemsCount > 0) {
                Log.d(IMPORT_TAG, "Found ${validChecklistItems.size} valid checklist items. $orphanedChecklistItemsCount orphaned items will be skipped.")
            }

            appDatabase.withTransaction {
                Log.d(IMPORT_TAG, "Transaction: Inserting attachments data.")
                // Insert content entities first
                noteDocumentDao.insertAllDocuments(validDocuments)
                Log.d(IMPORT_TAG, "  - Inserted ${validDocuments.size} note documents.")
                noteDocumentDao.insertAllDocumentItems(validDocumentItems)
                Log.d(IMPORT_TAG, "  - Inserted ${validDocumentItems.size} note document items.")
                checklistDao.insertChecklists(validChecklists)
                Log.d(IMPORT_TAG, "  - Inserted ${validChecklists.size} checklists.")
                checklistDao.insertItems(validChecklistItems)
                Log.d(IMPORT_TAG, "  - Inserted ${validChecklistItems.size} checklist items.")
                linkItemDao.insertAll(backupData.linkItemEntities)
                Log.d(IMPORT_TAG, "  - Inserted ${backupData.linkItemEntities.size} link items.")

                // Insert attachments themselves
                attachmentDao.insertAttachments(backupData.attachments)
                Log.d(IMPORT_TAG, "  - Inserted ${backupData.attachments.size} attachments.")

                // Insert only the valid cross-references
                attachmentDao.insertProjectAttachmentLinks(validCrossRefs)
                Log.d(IMPORT_TAG, "  - Inserted ${validCrossRefs.size} valid attachment cross-refs.")
            }

            Log.i(IMPORT_TAG, "Attachments import completed successfully. $orphanedCrossRefsCount attachments were imported as orphans.")
            return Result.success("Attachments imported successfully! $orphanedCrossRefsCount attachments were imported without a parent project and can be found in the attachments library.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "A critical error occurred during attachments import.", e)
            return Result.failure(e)
        }
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
            Log.d(IMPORT_TAG, "Файл успішно прочитано. Розмір: ${jsonString.length} символів.")

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
                "Дані з бекапу: \n" +
                    "  - Projects: ${backup.projects.size}\n" +
                    "  - Goals: ${backup.goals.size}\n" +
                    "  - ListItems: ${backup.listItems.size}\n" +
                    "  - NoteDocuments: ${backup.documents.size}\n" +
                    "  - NoteDocumentItems: ${backup.documentItems.size}\n" +
                    "  - Checklists: ${backup.checklists.size}\n" +
                    "  - ChecklistItems: ${backup.checklistItems.size}\n" +
                    "  - Attachments: ${backup.attachments.size}\n" +
                    "  - Attachment CrossRefs: ${backup.projectAttachmentCrossRefs.size}\n" +
                    "  - LinkItems: ${backup.linkItemEntities.size}\n" +
                    "  - InboxRecords: ${backup.inboxRecords.size}\n" +
                    "  - ActivityRecords: ${backup.activityRecords.size}\n" +
                    "  - ProjectLogs: ${backup.projectExecutionLogs.size}\n" +
                    "  - RecentEntries: ${backup.recentProjectEntries.size}"
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
                }
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

            val recentItemsToInsert = backup.recentProjectEntries.mapNotNull { entry ->
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

            Log.d(IMPORT_TAG, "Перед транзакцією. projectDao=${projectDao.hashCode()}")
            appDatabase.withTransaction {
                Log.d(IMPORT_TAG, "Транзакція: очищення таблиць.")
                projectManagementDao.deleteAllLogs()
                inboxRecordDao.deleteAll()
                linkItemDao.deleteAll()
                activityRecordDao.clearAll()
                listItemDao.deleteAll()
                projectDao.deleteAll()
                goalDao.deleteAll()
                legacyNoteDao.deleteAll()
                noteDocumentDao.deleteAllDocumentItems()
                noteDocumentDao.deleteAllDocuments()
                checklistDao.deleteAllChecklistItems()
                checklistDao.deleteAllChecklists()
                recentItemDao.deleteAll()
                attachmentDao.deleteAllProjectAttachmentLinks()
                attachmentDao.deleteAll()
                Log.d(IMPORT_TAG, "Всі таблиці очищено.")

                Log.d(IMPORT_TAG, "Транзакція: вставка базових сутностей.")
                goalDao.insertGoals(backup.goals)
                projectDao.insertProjects(cleanedProjects)
                listItemDao.insertItems(cleanedListItems)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.goals.size} goals, ${cleanedProjects.size} projects, ${cleanedListItems.size} listItems.")

                legacyNoteDao.insertAll(backup.legacyNotes)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.legacyNotes.size} legacyNotes.")

                noteDocumentDao.insertAllDocuments(backup.documents)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.documents.size} noteDocuments.")
                noteDocumentDao.insertAllDocumentItems(backup.documentItems)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.documentItems.size} noteDocumentItems.")

                if (backup.checklists.isNotEmpty()) {
                    checklistDao.insertChecklists(backup.checklists)
                    Log.d(IMPORT_TAG, "  - Вставлено: ${backup.checklists.size} checklists.")
                }
                if (backup.checklistItems.isNotEmpty()) {
                    checklistDao.insertItems(backup.checklistItems)
                    Log.d(IMPORT_TAG, "  - Вставлено: ${backup.checklistItems.size} checklistItems.")
                }

                activityRecordDao.insertAll(backup.activityRecords)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.activityRecords.size} activityRecords.")
                linkItemDao.insertAll(backup.linkItemEntities)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.linkItemEntities.size} linkItems.")
                inboxRecordDao.insertAll(backup.inboxRecords)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.inboxRecords.size} inboxRecords.")
                projectManagementDao.insertAllLogs(backup.projectExecutionLogs)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.projectExecutionLogs.size} projectLogs.")
                recentItemDao.insertAll(recentItemsToInsert)
                Log.d(IMPORT_TAG, "  - Вставлено: ${recentItemsToInsert.size} recentItems.")

                attachmentDao.insertAttachments(backup.attachments)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.attachments.size} attachments.")
                attachmentDao.insertProjectAttachmentLinks(backup.projectAttachmentCrossRefs)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.projectAttachmentCrossRefs.size} projectAttachmentCrossRefs.")

                Log.d(IMPORT_TAG, "Транзакція: відновлення settings. Entries=${backupSettingsMap.size}")
                settingsRepository.restoreFromMap(backupSettingsMap)
                Log.d(IMPORT_TAG, "Транзакція: запуск DatabaseInitializer.prePopulate().")
                com.romankozak.forwardappmobile.data.database.DatabaseInitializer(projectDao, context).prePopulate()

                if (backup.attachments.isEmpty()) {
                    Log.d(IMPORT_TAG, "Спроба створити відсутні записи вкладень...")
                    backup.documents.forEach {
                        attachmentRepository.ensureAttachmentLinkedToProject(
                            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
                            entityId = it.id,
                            projectId = it.projectId,
                            ownerProjectId = it.projectId,
                            createdAt = it.createdAt,
                        )
                    }
                    backup.checklists.forEach {
                        attachmentRepository.ensureAttachmentLinkedToProject(
                            attachmentType = ListItemTypeValues.CHECKLIST,
                            entityId = it.id,
                            projectId = it.projectId,
                            ownerProjectId = it.projectId,
                            createdAt = System.currentTimeMillis(),
                        )
                    }
                    Log.d(IMPORT_TAG, "Створення відсутніх записів вкладень завершено.")
                }

                Log.d(IMPORT_TAG, "Транзакція: вставка завершена.")
            }

            runPostBackupMigration()

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
        val localProjects = projectDao.getAll().associateBy { it.id }

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
                "Список" -> projectDao.deleteProjectById(change.id)
                "Ціль" -> goalDao.deleteGoalById(change.id)
            }
        }

        changesByType[ChangeType.Update]?.forEach { change ->
            when (change.entityType) {
                "Список" -> projectDao.update(change.entity as Project)
                "Ціль" -> goalDao.updateGoal(change.entity as Goal)
            }
        }

        val addsAndMoves = (changesByType[ChangeType.Add] ?: emptyList()) + (changesByType[ChangeType.Move] ?: emptyList())
        addsAndMoves.forEach { change ->
            when (change.entityType) {
                "Список" -> projectDao.insert(change.entity as Project)
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
