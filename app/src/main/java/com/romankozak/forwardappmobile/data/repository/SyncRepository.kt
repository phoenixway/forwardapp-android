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
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
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
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.AttachmentsBackup
import com.romankozak.forwardappmobile.data.sync.ReservedGroupAdapter
import com.romankozak.forwardappmobile.data.sync.toGoal
import com.romankozak.forwardappmobile.data.sync.toProject
import com.romankozak.forwardappmobile.data.sync.LongDeserializer
import com.romankozak.forwardappmobile.data.sync.BackupDiff
import com.romankozak.forwardappmobile.data.sync.DiffResult
import com.romankozak.forwardappmobile.data.sync.UpdatedItem
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
    @param:ApplicationContext private val context: Context,
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
    private val scriptDao: ScriptDao,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentDao: AttachmentDao,
    private val systemAppDao: SystemAppDao,
) {
    private val TAG = "SyncRepository"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
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
        val recentProjectEntries = recentItemDao.getAll().map { recentItem ->
            com.romankozak.forwardappmobile.data.sync.RecentProjectEntry(
                projectId = recentItem.target,
                timestamp = recentItem.lastAccessed
            )
        }
        val scripts = scriptDao.getAll().first()

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
                recentProjectEntries = recentProjectEntries,
                scripts = scripts,
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

    suspend fun parseBackupFile(uri: Uri): Result<FullAppBackup> {
        return try {
            val jsonString =
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    .use { it?.readText() }

            if (jsonString.isNullOrBlank()) {
                Result.failure(Exception("Backup file is empty or could not be read."))
            } else {
                Log.d(TAG, "parseBackupFile: JSON size=${jsonString.length}, first 500 chars: ${jsonString.take(500)}")
                parseFullAppBackup(jsonString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse backup file.", e)
            Result.failure(e)
        }
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
                    val parsed = gson.fromJson(jsonString, FullAppBackup::class.java)
                    Log.d(IMPORT_TAG, "Parsed FullAppBackup: database=${parsed.database}, backupVersion=${parsed.backupSchemaVersion}")
                    Log.d(IMPORT_TAG, "Database projects: ${parsed.database.projects.size}, goals: ${parsed.database.goals.size}")
                    parsed
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
            if (normalizedBackupVersion !in setOf(1, 2)) {
                val message = "Unsupported backup version: $rawBackupVersion. Expected version 1 or 2."
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
            val existingSystemProjectsByKey = projectDao.getAll()
                .filter { it.systemKey != null }
                .associateBy { it.systemKey!! }

            val cleanedProjects =
                backup.projects.map { projectFromBackup ->
                    val normalizedIncoming = projectFromBackup.copy(
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

                    val systemKey = normalizedIncoming.systemKey
                    val existingSystemProject = systemKey?.let { existingSystemProjectsByKey[it] }
                    if (existingSystemProject != null) {
                        val incomingUpdated = normalizedIncoming.updatedAt ?: 0
                        val existingUpdated = existingSystemProject.updatedAt ?: 0
                        if (incomingUpdated > existingUpdated) {
                            Log.d(IMPORT_TAG, "System project ${systemKey} will be updated from backup (incoming newer: $incomingUpdated > $existingUpdated)")
                            normalizedIncoming
                        } else {
                            Log.d(IMPORT_TAG, "System project ${systemKey} kept from local DB (local newer or same: $existingUpdated >= $incomingUpdated)")
                            existingSystemProject
                        }
                    } else {
                        normalizedIncoming
                    }
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
                scriptDao.deleteAll()
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
                backup.scripts.forEach { scriptDao.insert(it) }
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.scripts.size} scripts.")
                recentItemDao.insertAll(recentItemsToInsert)
                Log.d(IMPORT_TAG, "  - Вставлено: ${recentItemsToInsert.size} recentItems.")

                attachmentDao.insertAttachments(backup.attachments)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.attachments.size} attachments.")
                attachmentDao.insertProjectAttachmentLinks(backup.projectAttachmentCrossRefs)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.projectAttachmentCrossRefs.size} projectAttachmentCrossRefs.")

                Log.d(IMPORT_TAG, "Транзакція: відновлення settings. Entries=${backupSettingsMap.size}")
                settingsRepository.restoreFromMap(backupSettingsMap)
                Log.d(IMPORT_TAG, "Транзакція: запуск DatabaseInitializer.prePopulate().")
                val systemAppRepository = com.romankozak.forwardappmobile.data.repository.SystemAppRepository(
                    systemAppDao = systemAppDao,
                    projectDao = projectDao,
                    noteDocumentDao = noteDocumentDao,
                    attachmentRepository = attachmentRepository,
                )
                com.romankozak.forwardappmobile.data.database.DatabaseInitializer(projectDao, systemAppRepository).prePopulate()

                // Create attachment records for documents and checklists if they don't have attachments yet
                // This ensures backward compatibility with older backup files
                if (backup.attachments.isEmpty() && backup.projectAttachmentCrossRefs.isEmpty()) {
                    Log.d(IMPORT_TAG, "Спроба створити відсутні записи вкладень для старих бекапів...")
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
                } else {
                    Log.d(IMPORT_TAG, "Attachments уже присутні в бекапі, пропускаємо автоматичне створення. attachments=${backup.attachments.size}, crossRefs=${backup.projectAttachmentCrossRefs.size}")
                }

                Log.d(IMPORT_TAG, "Транзакція: вставка завершена.")
            }

            runPostBackupMigration()

            Log.i(IMPORT_TAG, "Імпорт бекапу успішно завершено.")
            return Result.success("Backup imported successfully!")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Під час імпорту сталася критична помилка. Повідомлення: ${e.message}", e)
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
        val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
        val db = backup.database ?: return SyncReport(emptyList())

        val localProjectsAll = projectDao.getAll()
        val localProjects = localProjectsAll.filter { it.systemKey == null }.associateBy { it.id }
        val localGoals = goalDao.getAll().associateBy { it.id }
        val localListItems = listItemDao.getAll()
            .filter { it.projectId == null || it.projectId in localProjects.keys }
            .associateBy { it.id }

        val changes = mutableListOf<SyncChange>()

        // Goals
        val incomingGoals = db.goals.map { normalizeGoal(it) }
        incomingGoals.forEach { incoming ->
            val local = localGoals[incoming.id]?.let { normalizeGoal(it) }
            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Ціль", incoming.id, "Нова ціль: ${incoming.text}", entity = incoming))
            } else {
                if ((incoming.updatedAt ?: 0) > (local.updatedAt ?: 0)) {
                    val updates = mutableListOf<String>()
                    if (incoming.text != local.text) updates.add("text changed")
                    if (incoming.completed != local.completed) updates.add("completion status changed")
                    if (incoming.description != local.description) updates.add("description changed")
                    if (incoming.tags?.toSet() != local.tags?.toSet()) updates.add("tags changed")
                    if (incoming.valueImportance != local.valueImportance) updates.add("valueImportance changed")
                    if (incoming.valueImpact != local.valueImpact) updates.add("valueImpact changed")
                    if (incoming.effort != local.effort) updates.add("effort changed")
                    if (incoming.cost != local.cost) updates.add("cost changed")
                    if (incoming.risk != local.risk) updates.add("risk changed")
                    if (incoming.scoringStatus != local.scoringStatus) updates.add("scoringStatus changed")

                    if (updates.isNotEmpty()) {
                        changes.add(
                            SyncChange(
                                ChangeType.Update,
                                "Ціль",
                                incoming.id,
                                "Оновлено ціль: ${incoming.text}",
                                longDescription = "Зміни: ${updates.joinToString()}",
                                entity = incoming
                            )
                        )
                    }
                }
            }
        }
        localGoals.keys.minus(db.goals.map { it.id }.toSet()).forEach { id ->
            changes.add(SyncChange(ChangeType.Delete, "Ціль", id, "Видалено ціль: ${localGoals[id]?.text}", entity = localGoals[id]!!))
        }

        // Projects
        val incomingProjects = db.projects.filter { it.systemKey == null }.map { normalizeProject(it) }
        val incomingProjectIds = incomingProjects.map { it.id }.toSet()
        incomingProjects.forEach { incoming ->
            val local = localProjects[incoming.id]?.let { normalizeProject(it) }
            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Список", incoming.id, "Новий список: ${incoming.name}", entity = incoming))
            } else {
                if ((incoming.updatedAt ?: 0) > (local.updatedAt ?: 0)) {
                    val updates = mutableListOf<String>()
                    if (incoming.name != local.name) updates.add("name to '${incoming.name}'")
                    if (incoming.parentId != local.parentId) updates.add("parent changed")
                    if (incoming.description != local.description) updates.add("description changed")
                    if (incoming.isExpanded != local.isExpanded) updates.add("isExpanded changed to ${incoming.isExpanded}")
                    if (incoming.order != local.order) updates.add("order changed")
                    if (incoming.tags?.toSet() != local.tags?.toSet()) updates.add("tags changed")
                    if (incoming.isCompleted != local.isCompleted) updates.add("completion status changed")
                    if (incoming.valueImportance != local.valueImportance) updates.add("valueImportance changed")
                    if (incoming.valueImpact != local.valueImpact) updates.add("valueImpact changed")
                    if (incoming.effort != local.effort) updates.add("effort changed")
                    if (incoming.cost != local.cost) updates.add("cost changed")
                    if (incoming.risk != local.risk) updates.add("risk changed")
                    if (incoming.scoringStatus != local.scoringStatus) updates.add("scoringStatus changed")

                    if (updates.isNotEmpty()) {
                        changes.add(
                            SyncChange(
                                ChangeType.Update,
                                "Список",
                                incoming.id,
                                "Оновлено список: ${incoming.name}",
                                longDescription = "Зміни: ${updates.joinToString()}",
                                entity = incoming
                            )
                        )
                    }
                }
            }
        }
        localProjects.keys.minus(incomingProjectIds).forEach { id ->
            changes.add(SyncChange(ChangeType.Delete, "Список", id, "Видалено список: ${localProjects[id]?.name}", entity = localProjects[id]!!))
        }

        // List items
        val incomingListItems = db.listItems
            .filter { it.projectId == null || it.projectId in incomingProjectIds }
            .associateBy { it.id }
        incomingListItems.forEach { (id, incoming) ->
            val local = localListItems[id]
            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Привʼязка", id, "Нова привʼязка", entity = incoming))
            } else if (incoming != local) {
                changes.add(SyncChange(ChangeType.Move, "Привʼязка", id, "Оновлено привʼязку", entity = incoming))
            }
        }
        localListItems.keys.minus(incomingListItems.keys).forEach { id ->
            changes.add(SyncChange(ChangeType.Delete, "Привʼязка", id, "Видалено привʼязку", entity = localListItems[id]!!))
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

    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        val IMPORT_TAG = "SyncRepo_SelectiveImport"
        return try {
            val local = loadLocalDatabaseContent()

            fun <T> keepNewer(
                incoming: List<T>,
                localMap: Map<String, T>,
                idSelector: (T) -> String,
                versionSelector: (T) -> Long,
                updatedAtSelector: (T) -> Long?
            ): List<T> {
                return incoming.filter { inc ->
                    val localItem = localMap[idSelector(inc)]
                    if (localItem == null) return@filter true
                    val incVer = versionSelector(inc)
                    val locVer = versionSelector(localItem)
                    if (incVer > locVer) return@filter true
                    if (incVer < locVer) return@filter false
                    val incUpdated = updatedAtSelector(inc) ?: 0L
                    val locUpdated = updatedAtSelector(localItem) ?: 0L
                    incUpdated > locUpdated
                }
            }

            appDatabase.withTransaction {
                Log.d(IMPORT_TAG, "Transaction: Inserting selected data (LWW).")

                if (selectedData.projects.isNotEmpty()) {
                    // Filter out system projects (those with systemKey) to prevent duplication
                    // System projects are managed by DatabaseInitializer.prePopulate()
                    val regularProjects = selectedData.projects
                        .filter { it.systemKey == null }
                        .let { keepNewer(it, local.projects.associateBy { p -> p.id }, { it.id }, { it.version }, { it.updatedAt }) }
                    if (regularProjects.isNotEmpty()) {
                        projectDao.insertProjects(regularProjects)
                        Log.d(IMPORT_TAG, "  - Upserted ${regularProjects.size} regular projects. Skipped ${selectedData.projects.size - regularProjects.size} system projects.")
                    } else {
                        Log.d(IMPORT_TAG, "  - All ${selectedData.projects.size} projects are system projects, skipped.")
                    }
                }
                if (selectedData.goals.isNotEmpty()) {
                    val newerGoals = keepNewer(selectedData.goals, local.goals.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    if (newerGoals.isNotEmpty()) {
                        goalDao.insertGoals(newerGoals)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerGoals.size} goals (filtered from ${selectedData.goals.size}).")
                }
                if (selectedData.listItems.isNotEmpty()) {
                    // Filter out list items that reference non-existent projects/goals
                    val importedProjectIds = selectedData.projects.map { it.id }.toSet()
                    val importedGoalIds = selectedData.goals.map { it.id }.toSet()
                    val validListItems = selectedData.listItems.filter { 
                        it.projectId in importedProjectIds || it.entityId in importedGoalIds
                    }.let { keepNewer(it, local.listItems.associateBy { li -> li.id }, { it.id }, { it.version }, { null }) }
                    if (validListItems.isNotEmpty()) {
                        listItemDao.insertItems(validListItems)
                        Log.d(IMPORT_TAG, "  - Upserted ${validListItems.size} list items (filtered from ${selectedData.listItems.size}).")
                    }
                    if (validListItems.size < selectedData.listItems.size) {
                        Log.w(IMPORT_TAG, "  - Skipped ${selectedData.listItems.size - validListItems.size} list items with missing references.")
                    }
                 }
                if (selectedData.legacyNotes.isNotEmpty()) {
                    val newerNotes = keepNewer(selectedData.legacyNotes, local.legacyNotes.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    if (newerNotes.isNotEmpty()) {
                        legacyNoteDao.insertAll(newerNotes)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerNotes.size} legacy notes (filtered from ${selectedData.legacyNotes.size}).")
                }
                if (selectedData.documents.isNotEmpty()) {
                    val newerDocs = keepNewer(selectedData.documents, local.documents.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    if (newerDocs.isNotEmpty()) {
                        noteDocumentDao.insertAllDocuments(newerDocs)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerDocs.size} documents (filtered from ${selectedData.documents.size}).")
                }
                if (selectedData.documentItems.isNotEmpty()) {
                    val newerDocItems = keepNewer(selectedData.documentItems, local.documentItems.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    if (newerDocItems.isNotEmpty()) {
                        noteDocumentDao.insertAllDocumentItems(newerDocItems)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerDocItems.size} document items (filtered from ${selectedData.documentItems.size}).")
                }
                if (selectedData.checklists.isNotEmpty()) {
                    val newerChecklists = keepNewer(selectedData.checklists, local.checklists.associateBy { it.id }, { it.id }, { it.version }, { null })
                    if (newerChecklists.isNotEmpty()) {
                        checklistDao.insertChecklists(newerChecklists)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerChecklists.size} checklists (filtered from ${selectedData.checklists.size}).")
                }
                if (selectedData.checklistItems.isNotEmpty()) {
                    val newerChecklistItems = keepNewer(selectedData.checklistItems, local.checklistItems.associateBy { it.id }, { it.id }, { it.version }, { null })
                    if (newerChecklistItems.isNotEmpty()) {
                        checklistDao.insertItems(newerChecklistItems)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerChecklistItems.size} checklist items (filtered from ${selectedData.checklistItems.size}).")
                }
                if (selectedData.projectAttachmentCrossRefs.isNotEmpty()) {
                    // Get IDs of projects that were selected for import (from selectedData, not DB)
                    // This ensures we don't import attachments linked to system projects that weren't selected
                    val selectedProjectIds = selectedData.projects.map { it.id }.toSet()
                    val validCrossRefs = selectedData.projectAttachmentCrossRefs.filter { it.projectId in selectedProjectIds }
                    val validAttachmentIds = validCrossRefs.map { it.attachmentId }.toSet()
                    
                    // Only import attachments that have valid cross-refs to selected projects
                    val validAttachments = selectedData.attachments
                        .filter { it.id in validAttachmentIds }
                        .let { keepNewer(it, local.attachments.associateBy { at -> at.id }, { it.id }, { it.version }, { it.updatedAt }) }
                    
                    if (validAttachments.isNotEmpty()) {
                        attachmentDao.insertAttachments(validAttachments)
                        Log.d(IMPORT_TAG, "  - Upserted ${validAttachments.size} attachments (filtered from ${selectedData.attachments.size}).")
                    }
                    if (validCrossRefs.isNotEmpty()) {
                        val newerCrossRefs = keepNewer(
                            validCrossRefs,
                            local.projectAttachmentCrossRefs.associateBy { "${it.projectId}-${it.attachmentId}" },
                            { "${it.projectId}-${it.attachmentId}" },
                            { it.version },
                            { it.syncedAt }
                        )
                        if (newerCrossRefs.isNotEmpty()) {
                            attachmentDao.insertProjectAttachmentLinks(newerCrossRefs)
                        }
                        Log.d(IMPORT_TAG, "  - Upserted ${newerCrossRefs.size} project attachment cross-refs (filtered from ${selectedData.projectAttachmentCrossRefs.size}).")
                    }
                    if (validCrossRefs.size < selectedData.projectAttachmentCrossRefs.size) {
                        Log.w(IMPORT_TAG, "  - Skipped ${selectedData.projectAttachmentCrossRefs.size - validCrossRefs.size} cross-refs pointing to non-existent or unselected projects.")
                    }
                } else if (selectedData.attachments.isNotEmpty()) {
                    // If there are no cross-refs but attachments exist, just import the attachments
                    val newerAttachments = keepNewer(selectedData.attachments, local.attachments.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    if (newerAttachments.isNotEmpty()) {
                        attachmentDao.insertAttachments(newerAttachments)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerAttachments.size} attachments (no cross-refs).")
                }
                if (selectedData.scripts.isNotEmpty()) {
                    val newerScripts = keepNewer(selectedData.scripts, local.scripts.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    newerScripts.forEach { scriptDao.insert(it) }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerScripts.size} scripts.")
                }
                // Add other entities as needed
            }
            Result.success("Selected items imported successfully!")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "A critical error occurred during selective import.", e)
            Result.failure(e)
        }
    }

    private fun parseFullAppBackup(jsonString: String): Result<FullAppBackup> {
        return try {
            val backupData = gson.fromJson(jsonString, FullAppBackup::class.java)
            Log.d(TAG, "parseFullAppBackup: Parsed FullAppBackup - database=${backupData.database}, backupVersion=${backupData.backupSchemaVersion}")
            Log.d(TAG, "parseFullAppBackup: Database projects=${backupData.database.projects.size}, goals=${backupData.database.goals.size}")
            Result.success(backupData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse FullAppBackup JSON.", e)
            Result.failure(e)
        }
    }

    private fun parseIsoOrNull(value: String): Long? {
        return try {
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse ISO timestamp: $value, using null", e)
            null
        }
    }

    private fun DatabaseContent.normalizeForDiff(): DatabaseContent {
        val nonSystemProjects = projects.filter { it.systemKey == null }.map { normalizeProject(it) }
        val nonSystemProjectIds = nonSystemProjects.map { it.id }.toSet()

        val safeDocuments = documents
            .filter { it.projectId in nonSystemProjectIds }
            .map { it.copy(content = it.content ?: "", name = it.name, projectId = it.projectId, lastCursorPosition = it.lastCursorPosition) }
        val safeDocumentIds = safeDocuments.map { it.id }.toSet()

        val safeChecklists = checklists.filter { it.projectId in nonSystemProjectIds }
        val safeChecklistIds = safeChecklists.map { it.id }.toSet()

        val safeListItems = listItems.filter { it.projectId in nonSystemProjectIds }
        val safeInboxRecords = inboxRecords.filter { it.projectId in nonSystemProjectIds }
        val safeProjectLogs = projectExecutionLogs.filter { it.projectId in nonSystemProjectIds }

        val safeProjectAttachmentCrossRefs = projectAttachmentCrossRefs.filter { it.projectId in nonSystemProjectIds }
        val safeAttachmentIds = safeProjectAttachmentCrossRefs.map { it.attachmentId }.toSet()
        val safeAttachments = attachments.filter { it.id in safeAttachmentIds || safeProjectAttachmentCrossRefs.isEmpty() }

        val safeDocumentItems = documentItems.filter { it.listId in safeDocumentIds }
        val safeChecklistItems = checklistItems.filter { it.checklistId in safeChecklistIds }

        return copy(
            projects = nonSystemProjects,
            goals = goals.map { normalizeGoal(it) },
            listItems = safeListItems,
            legacyNotes = legacyNotes,
            activityRecords = activityRecords,
            documents = safeDocuments,
            documentItems = safeDocumentItems,
            checklists = safeChecklists,
            checklistItems = safeChecklistItems,
            linkItemEntities = linkItemEntities,
            inboxRecords = safeInboxRecords,
            projectExecutionLogs = safeProjectLogs,
            scripts = scripts,
            attachments = safeAttachments,
            projectAttachmentCrossRefs = safeProjectAttachmentCrossRefs,
        )
    }

    suspend fun createBackupDiff(backupContent: DatabaseContent): BackupDiff {
        val local = loadLocalDatabaseContent().normalizeForDiff()
        val incoming = backupContent.normalizeForDiff()
        return BackupDiff(
            projects = buildDiff(local.projects, incoming.projects) { it.id },
            goals = buildDiff(local.goals, incoming.goals) { it.id },
            listItems = buildDiff(local.listItems, incoming.listItems) { it.id },
            legacyNotes = buildDiff(local.legacyNotes, incoming.legacyNotes) { it.id },
            activityRecords = buildDiff(local.activityRecords, incoming.activityRecords) { it.id },
            documents = buildDiff(local.documents, incoming.documents) { it.id },
            documentItems = buildDiff(local.documentItems, incoming.documentItems) { it.id },
            checklists = buildDiff(local.checklists, incoming.checklists) { it.id },
            checklistItems = buildDiff(local.checklistItems, incoming.checklistItems) { it.id },
            linkItems = buildDiff(local.linkItemEntities, incoming.linkItemEntities) { it.id },
            inboxRecords = buildDiff(local.inboxRecords, incoming.inboxRecords) { it.id },
            projectExecutionLogs = buildDiff(local.projectExecutionLogs, incoming.projectExecutionLogs) { it.id },
            scripts = buildDiff(local.scripts, incoming.scripts) { it.id },
            attachments = buildDiff(local.attachments, incoming.attachments) { it.id },
            projectAttachmentCrossRefs = buildDiff(local.projectAttachmentCrossRefs, incoming.projectAttachmentCrossRefs) { "${it.projectId}-${it.attachmentId}" },
        )
    }

    private fun <T> buildDiff(local: List<T>, incoming: List<T>, idSelector: (T) -> String): DiffResult<T> {
        val localMap = local.associateBy(idSelector)
        val incomingMap = incoming.associateBy(idSelector)

        val added = incomingMap.filterKeys { it !in localMap }.values.toList()
        val deleted = localMap.filterKeys { it !in incomingMap }.values.toList()

        val updated = incomingMap.mapNotNull { (id, incomingItem) ->
            val localItem = localMap[id]
            if (localItem != null && localItem != incomingItem) {
                UpdatedItem(local = localItem, incoming = incomingItem)
            } else {
                null
            }
        }

        return DiffResult(
            added = added,
            updated = updated,
            deleted = deleted,
        )
    }

    private suspend fun loadLocalDatabaseContent(): DatabaseContent {
        val systemApps = systemAppDao.getAll()
        val systemNoteIds = systemApps.mapNotNull { it.noteDocumentId }.toSet()

        val projects = projectDao.getAll()
        val systemProjectIds = projects.filter { it.systemKey != null }.map { it.id }.toSet()
        val goals = goalDao.getAll()
        val listItems = listItemDao.getAll().filterNot { it.projectId in systemProjectIds }
        val legacyNotes = legacyNoteDao.getAll()
        val documents = noteDocumentDao.getAllDocuments()
            .filterNot { it.id in systemNoteIds }
            .filterNot { it.projectId in systemProjectIds }
        val documentItems = noteDocumentDao.getAllDocumentItems().filterNot { it.listId in systemNoteIds }
        val checklists = checklistDao.getAllChecklists().filterNot { it.projectId in systemProjectIds }
        val checklistItems = checklistDao.getAllChecklistItems()
        val linkItemEntities = linkItemDao.getAllEntities()
        val inboxRecords = inboxRecordDao.getAll().filterNot { it.projectId in systemProjectIds }
        val projectExecutionLogs = projectManagementDao.getAllLogs().filterNot { it.projectId in systemProjectIds }
        val scripts = scriptDao.getAll().first()
        val activityRecords = activityRecordDao.getAllRecordsStream().first()
        val projectAttachmentCrossRefs = attachmentDao.getAllProjectAttachmentCrossRefs().filterNot { it.projectId in systemProjectIds }
        val attachmentIds = projectAttachmentCrossRefs.map { it.attachmentId }.toSet()
        val attachments = attachmentDao.getAll().filter { attachmentIds.isEmpty() || it.id in attachmentIds }

        return DatabaseContent(
            projects = projects,
            goals = goals,
            listItems = listItems,
            legacyNotes = legacyNotes,
            activityRecords = activityRecords,
            documents = documents,
            documentItems = documentItems,
            checklists = checklists,
            checklistItems = checklistItems,
            linkItemEntities = linkItemEntities,
            inboxRecords = inboxRecords,
            projectExecutionLogs = projectExecutionLogs,
            scripts = scripts,
            attachments = attachments,
            projectAttachmentCrossRefs = projectAttachmentCrossRefs,
            recentProjectEntries = emptyList(),
        )
    }

    private fun normalizeGoal(goal: Goal): Goal {
        return goal.copy(
            tags = goal.tags ?: emptyList(),
            relatedLinks = goal.relatedLinks ?: emptyList(),
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
            scoringStatus = goal.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
        )
    }

    private fun normalizeProject(project: Project): Project {
        return project.copy(
            tags = project.tags ?: emptyList(),
            relatedLinks = project.relatedLinks ?: emptyList(),
            isExpanded = project.isExpanded,
            order = project.order,
            isAttachmentsExpanded = project.isAttachmentsExpanded,
            // Align with full-import cleaning to avoid false "updated" after re-importing same file
            defaultViewModeName = project.defaultViewModeName ?: ProjectViewMode.BACKLOG.name,
            isCompleted = project.isCompleted,
            isProjectManagementEnabled = project.isProjectManagementEnabled ?: false,
            projectStatus = project.projectStatus ?: ProjectStatusValues.NO_PLAN,
            projectStatusText = project.projectStatusText ?: "",
            projectLogLevel = project.projectLogLevel ?: ProjectLogLevelValues.NORMAL,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes ?: 0,
            valueImportance = project.valueImportance,
            valueImpact = project.valueImpact,
            effort = project.effort,
            cost = project.cost,
            risk = project.risk,
            weightEffort = project.weightEffort,
            weightCost = project.weightCost,
            weightRisk = project.weightRisk,
            rawScore = project.rawScore,
            displayScore = project.displayScore,
            scoringStatus = project.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
            showCheckboxes = project.showCheckboxes,
            projectType = project.projectType ?: ProjectType.DEFAULT,
            reservedGroup = project.reservedGroup,
        )
    }
}
