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
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.data.database.models.Project;
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues;
import com.romankozak.forwardappmobile.data.database.models.ProjectLogLevelValues;
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues;
import com.romankozak.forwardappmobile.data.database.models.ProjectType;
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
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
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
    private val WIFI_SYNC_LOG_TAG = "FWD_SYNC_TEST"

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

    suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
        val changes = getChangesSince(deltaSince)
        val fullBackup = FullAppBackup(database = changes)
        return gson.toJson(fullBackup)
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

            // If local system project replaces incoming one (different id), remap children to the kept id
            val projectIdMap = mutableMapOf<String, String>()
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
                        if (normalizedIncoming.id != existingSystemProject.id) {
                            projectIdMap[normalizedIncoming.id] = existingSystemProject.id
                        }
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
            val cleanedProjectsWithParents =
                cleanedProjects.map {
                    val mappedParent = it.parentId?.let { pid -> projectIdMap[pid] ?: pid }
                    val parentId = mappedParent
                    if (parentId != null && cleanedProjects.none { p -> p.id == parentId }) {
                        Log.w(IMPORT_TAG, "Project ${it.id} has missing parent $parentId. Resetting parentId to null.")
                        it.copy(parentId = null)
                    } else {
                        if (mappedParent != it.parentId) it.copy(parentId = mappedParent) else it
                    }
                }
            val orphanProjects = cleanedProjects.zip(cleanedProjectsWithParents)
                .filter { (original, fixed) -> original.parentId != null && fixed.parentId == null }
                .map { it.first }
            if (orphanProjects.isNotEmpty()) {
                val sample = orphanProjects.take(5).joinToString { "${it.name} (${it.id}) parent=${it.parentId}" }
                Log.w(IMPORT_TAG, "Found ${orphanProjects.size} projects with missing parent; parentId reset to null. Sample: $sample")
            } else {
                Log.d(IMPORT_TAG, "No projects with missing parentId detected.")
            }
            Log.d(IMPORT_TAG, "Очищення даних Project завершено.")

            val projectIds = cleanedProjectsWithParents.map { it.id }.toSet()
            val goalIds = backup.goals.map { it.id }.toSet()
            val cleanedListItems =
                backup.listItems.mapNotNull { item ->
                    if (item.id.isBlank() || item.projectId.isBlank() || item.entityId.isBlank()) {
                        Log.w(IMPORT_TAG, "Skipping invalid ListItem due to blank ID(s): $item")
                        null
                    } else {
                        item.copy(
                            projectId = projectIdMap[item.projectId] ?: item.projectId,
                            entityId = if (item.itemType == ListItemTypeValues.SUBLIST) {
                                projectIdMap[item.entityId] ?: item.entityId
                            } else {
                                item.entityId
                            },
                        )
                    }
                }.filter {
                    val projectOk = it.projectId in projectIds
                    val goalOk = it.itemType != ListItemTypeValues.GOAL || it.entityId in goalIds
                    if (!projectOk || !goalOk) {
                        Log.w(IMPORT_TAG, "Skipping ListItem due to missing references. projectOk=$projectOk goalOk=$goalOk item=$it")
                    }
                    projectOk && goalOk
                }
            Log.d(IMPORT_TAG, "Очищення ListItem завершено. Original: ${backup.listItems.size}, Cleaned: ${cleanedListItems.size}")

            val recentItemsToInsert = backup.recentProjectEntries.mapNotNull { entry ->
                val targetId = projectIdMap[entry.projectId] ?: entry.projectId
                val project = cleanedProjectsWithParents.find { it.id == targetId }
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
            projectDao.insertProjects(cleanedProjectsWithParents)
            listItemDao.insertItems(cleanedListItems)
            Log.d(IMPORT_TAG, "  - Вставлено: ${backup.goals.size} goals, ${cleanedProjectsWithParents.size} projects, ${cleanedListItems.size} listItems.")

                legacyNoteDao.insertAll(backup.legacyNotes)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.legacyNotes.size} legacyNotes.")

                val documentProjectIds = cleanedProjectsWithParents.map { it.id }.toSet()
                val validDocuments = backup.documents
                    .map { it.copy(projectId = projectIdMap[it.projectId] ?: it.projectId) }
                    .filter { it.projectId in documentProjectIds }
                val skippedDocuments = backup.documents.size - validDocuments.size
                noteDocumentDao.insertAllDocuments(validDocuments)
                Log.d(
                    IMPORT_TAG,
                    "  - Вставлено: ${validDocuments.size} noteDocuments. Skipped invalid project refs=$skippedDocuments"
                )
                val validDocumentIds = validDocuments.map { it.id }.toSet()
                val validDocumentItems = backup.documentItems.filter { it.listId in validDocumentIds }
                val skippedDocumentItems = backup.documentItems.size - validDocumentItems.size
                noteDocumentDao.insertAllDocumentItems(validDocumentItems)
                Log.d(
                    IMPORT_TAG,
                    "  - Вставлено: ${validDocumentItems.size} noteDocumentItems. Skipped invalid doc refs=$skippedDocumentItems"
                )

                if (backup.checklists.isNotEmpty()) {
                    val mappedChecklists = backup.checklists.map { it.copy(projectId = projectIdMap[it.projectId] ?: it.projectId) }
                    val validChecklists = mappedChecklists.filter { it.projectId in projectIds }
                    val skippedChecklists = mappedChecklists.size - validChecklists.size
                    checklistDao.insertChecklists(validChecklists)
                    Log.d(IMPORT_TAG, "  - Вставлено: ${validChecklists.size} checklists. Skipped invalid project refs=$skippedChecklists")
                }
                if (backup.checklistItems.isNotEmpty()) {
                    val validChecklistIds = backup.checklists.map { it.id }.toSet()
                    val mappedChecklistItems = backup.checklistItems.map {
                        it.copy(checklistId = it.checklistId)
                    }
                    val validChecklistItems = mappedChecklistItems.filter { it.checklistId in validChecklistIds }
                    val skippedChecklistItems = mappedChecklistItems.size - validChecklistItems.size
                    checklistDao.insertItems(validChecklistItems)
                    Log.d(IMPORT_TAG, "  - Вставлено: ${validChecklistItems.size} checklistItems. Skipped invalid checklist refs=$skippedChecklistItems")
                }

                activityRecordDao.insertAll(backup.activityRecords)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.activityRecords.size} activityRecords.")
                linkItemDao.insertAll(backup.linkItemEntities)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.linkItemEntities.size} linkItems.")
                val validInbox = backup.inboxRecords
                    .map { it.copy(projectId = projectIdMap[it.projectId] ?: it.projectId) }
                    .filter { it.projectId in projectIds }
                val skippedInbox = backup.inboxRecords.size - validInbox.size
                inboxRecordDao.insertAll(validInbox)
                Log.d(
                    IMPORT_TAG,
                    "  - Вставлено: ${validInbox.size} inboxRecords. Skipped invalid project refs=$skippedInbox"
                )
                val validLogs = backup.projectExecutionLogs
                    .map { it.copy(projectId = projectIdMap[it.projectId] ?: it.projectId) }
                    .filter { it.projectId in projectIds }
                val skippedLogs = backup.projectExecutionLogs.size - validLogs.size
                projectManagementDao.insertAllLogs(validLogs)
                Log.d(
                    IMPORT_TAG,
                    "  - Вставлено: ${validLogs.size} projectLogs. Skipped invalid project refs=$skippedLogs"
                )
                backup.scripts.forEach { scriptDao.insert(it) }
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.scripts.size} scripts.")
                recentItemDao.insertAll(recentItemsToInsert)
                Log.d(IMPORT_TAG, "  - Вставлено: ${recentItemsToInsert.size} recentItems.")

                val mappedAttachments = backup.attachments.map { at ->
                    at.copy(ownerProjectId = at.ownerProjectId?.let { pid -> projectIdMap[pid] ?: pid })
                }
                attachmentDao.insertAttachments(mappedAttachments)
                Log.d(IMPORT_TAG, "  - Вставлено: ${mappedAttachments.size} attachments.")
                val attachmentIds = mappedAttachments.map { it.id }.toSet()
                val validCrossRefs = backup.projectAttachmentCrossRefs
                    .map { it.copy(projectId = projectIdMap[it.projectId] ?: it.projectId) }
                    .filter { it.projectId in projectIds && it.attachmentId in attachmentIds }
                val skippedCrossRefs = backup.projectAttachmentCrossRefs.size - validCrossRefs.size
                attachmentDao.insertProjectAttachmentLinks(validCrossRefs)
                Log.d(
                    IMPORT_TAG,
                    "  - Вставлено: ${validCrossRefs.size} projectAttachmentCrossRefs. Skipped invalid=$skippedCrossRefs"
                )

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

            Log.i(IMPORT_TAG, "Orphan projects after import (parentId cleared): ${orphanProjects.size}")
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
            val port = if (uri.port != -1) uri.port else settingsRepository.wifiSyncPortFlow.first()
            val hostAndPort = "${uri.host}:$port"
            val fullUrl = "http://$hostAndPort/export"
            Log.d(WIFI_SYNC_LOG_TAG, "[fetchBackupFromWifi] GET $fullUrl")
            val response: String = client.get(fullUrl).body()
            Log.d(WIFI_SYNC_LOG_TAG, "[fetchBackupFromWifi] Success, bytes=${response.length}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(WIFI_SYNC_LOG_TAG, "Error fetching from Wi‑Fi", e)
            Result.failure(e)
        }

    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> =
        try {
            val unsynced = getUnsyncedChanges()
            Log.d(
                WIFI_SYNC_LOG_TAG,
                "[pushUnsyncedToWifi] unsynced goals=${unsynced.goals.size}",
            )
            Log.d(
                WIFI_SYNC_LOG_TAG,
                "[pushUnsyncedToWifi] unsynced listItems=${unsynced.listItems.size}",
            )
            val fullUrl = address.trim().let { raw ->
                val normalized = if (raw.startsWith("http")) raw else "http://$raw"
                val uri = normalized.toUri()
                val port = if (uri.port != -1) uri.port else settingsRepository.wifiSyncPortFlow.first()
                "http://${uri.host}:$port/import"
            }
            Log.d(
                WIFI_SYNC_LOG_TAG,
                "[pushUnsyncedToWifi] POST $fullUrl " +
                    "projects=${unsynced.projects.size} goals=${unsynced.goals.size} " +
                    "listItems=${unsynced.listItems.size} attachments=${unsynced.attachments.size}",
            )
            val payload = gson.toJson(FullAppBackup(database = unsynced))
            client.post(fullUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            markSyncedNow(unsynced)
            Log.d(WIFI_SYNC_LOG_TAG, "[pushUnsyncedToWifi] Success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(WIFI_SYNC_LOG_TAG, "Error pushing unsynced data to Wi‑Fi", e)
            Result.failure(e)
        }

    private suspend fun markSyncedNow(content: DatabaseContent) {
        val ts = System.currentTimeMillis()
        appDatabase.withTransaction {
            projectDao.insertProjects(content.projects.map { it.copy(syncedAt = ts) })
            goalDao.insertGoals(content.goals.map { it.copy(syncedAt = ts) })
            listItemDao.insertItems(content.listItems.map { it.copy(syncedAt = ts) })
            legacyNoteDao.insertAll(content.legacyNotes.map { it.copy(syncedAt = ts) })
            noteDocumentDao.insertAllDocuments(content.documents.map { it.copy(syncedAt = ts) })
            noteDocumentDao.insertAllDocumentItems(content.documentItems.map { it.copy(syncedAt = ts) })
            checklistDao.insertChecklists(content.checklists.map { it.copy(syncedAt = ts) })
            checklistDao.insertItems(content.checklistItems.map { it.copy(syncedAt = ts) })
            activityRecordDao.insertAll(content.activityRecords.map { it.copy(syncedAt = ts) })
            linkItemDao.insertAll(content.linkItemEntities.map { it.copy(syncedAt = ts) })
            inboxRecordDao.insertAll(content.inboxRecords.map { it.copy(syncedAt = ts) })
            projectManagementDao.insertAllLogs(content.projectExecutionLogs.map { it.copy(syncedAt = ts) })
            content.scripts.forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
            attachmentDao.insertAttachments(content.attachments.map { it.copy(syncedAt = ts) })
            attachmentDao.insertProjectAttachmentLinks(content.projectAttachmentCrossRefs.map { it.copy(syncedAt = ts) })
        }
    }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
        val db = backup.database ?: return SyncReport(emptyList())

        val localProjectsAll = projectDao.getAll()
        val localProjects = localProjectsAll.associateBy { it.id }
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
                    // System projects are managed by DatabaseInitializer.prePopulate(), but we need to update them
                    val regularProjects = selectedData.projects
                        .let { keepNewer(it, local.projects.associateBy { p -> p.id }, { it.id }, { it.version }, { it.updatedAt }) }
                    if (regularProjects.isNotEmpty()) {
                        projectDao.insertProjects(regularProjects)
                        Log.d(IMPORT_TAG, "  - Upserted ${regularProjects.size} projects.")
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
                    }.let { keepNewer(it, local.listItems.associateBy { li -> li.id }, { it.id }, { it.version }, { it.updatedAt }) }
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
                    val newerChecklists = keepNewer(selectedData.checklists, local.checklists.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
                    if (newerChecklists.isNotEmpty()) {
                        checklistDao.insertChecklists(newerChecklists)
                    }
                    Log.d(IMPORT_TAG, "  - Upserted ${newerChecklists.size} checklists (filtered from ${selectedData.checklists.size}).")
                }
                if (selectedData.checklistItems.isNotEmpty()) {
                    val newerChecklistItems = keepNewer(selectedData.checklistItems, local.checklistItems.associateBy { it.id }, { it.id }, { it.version }, { it.updatedAt })
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
                            { it.updatedAt }
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

    private fun Project.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun Goal.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun NoteDocumentEntity.updatedTs(): Long = this.updatedAt
    private fun NoteDocumentItemEntity.updatedTs(): Long = this.updatedAt
    private fun LegacyNoteEntity.updatedTs(): Long = this.updatedAt
    private fun ChecklistEntity.updatedTs(): Long = this.updatedAt ?: this.version
    private fun ChecklistItemEntity.updatedTs(): Long = this.updatedAt ?: this.version
    private fun ActivityRecord.updatedTs(): Long = this.updatedAt ?: (this.endTime ?: this.startTime ?: this.createdAt)
    private fun InboxRecord.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun LinkItemEntity.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun ListItem.updatedTs(): Long = this.updatedAt ?: this.version
    private fun ProjectExecutionLog.updatedTs(): Long = this.updatedAt ?: this.timestamp
    private fun ScriptEntity.updatedTs(): Long = this.updatedAt
    private fun AttachmentEntity.updatedTs(): Long = this.updatedAt
    private fun ProjectAttachmentCrossRef.updatedTs(): Long = this.updatedAt ?: this.attachmentOrder.toLong()
    private fun DayPlan.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun DayTask.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun DailyMetric.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun Reminder.updatedTs(): Long = this.updatedAt ?: this.creationTime

    private fun <T> isUnsynced(
        item: T,
        syncedAtSelector: (T) -> Long?,
        updatedSelector: (T) -> Long,
        isDeletedSelector: (T) -> Boolean,
    ): Boolean {
        val syncedAt = syncedAtSelector(item) ?: 0L
        val updated = updatedSelector(item)
        return isDeletedSelector(item) || syncedAt == 0L || updated > syncedAt
    }

    suspend fun getUnsyncedChanges(): DatabaseContent {
        val local = loadLocalDatabaseContent()
        fun <T> logUnsynced(tag: String, items: List<T>, idSel: (T) -> String, updSel: (T) -> Long?, syncSel: (T) -> Long?, delSel: (T) -> Boolean) {
            val sample = items.take(5).joinToString { "${idSel(it)} upd=${updSel(it)} sync=${syncSel(it)} del=${delSel(it)}" }
            Log.d(WIFI_SYNC_LOG_TAG, "[getUnsynced] $tag count=${items.size} sample=$sample")
        }
        return DatabaseContent(
            projects = local.projects.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("projects", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            goals = local.goals.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("goals", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            listItems = local.listItems.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("listItems", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            legacyNotes = local.legacyNotes.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            documents = local.documents.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            documentItems = local.documentItems.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            checklists = local.checklists.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            checklistItems = local.checklistItems.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            activityRecords = local.activityRecords.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            linkItemEntities = local.linkItemEntities.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            inboxRecords = local.inboxRecords.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            projectExecutionLogs = local.projectExecutionLogs.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            scripts = local.scripts.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            attachments = local.attachments.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            projectAttachmentCrossRefs = local.projectAttachmentCrossRefs.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            recentProjectEntries = emptyList(),
        )
    }

    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            Log.d(
                WIFI_SYNC_LOG_TAG,
                "[applyServerChanges] Incoming projects=${changes.projects.size}, goals=${changes.goals.size}, " +
                    "listItems=${changes.listItems.size}, attachments=${changes.attachments.size}",
            )
            appDatabase.withTransaction {
                val normalized = changes.copy(
                    projects = changes.projects.map { normalizeProject(it) },
                    goals = changes.goals.map { normalizeGoal(it) },
                )
                Log.d(
                    WIFI_SYNC_LOG_TAG,
                    "[applyServerChanges] normalized counts projects=${normalized.projects.size} goals=${normalized.goals.size} listItems=${normalized.listItems.size}",
                )
                val local = loadLocalDatabaseContent()

                // 1. Align System Project IDs and create a redirect map
                val localSystemProjects = local.projects.filter { it.systemKey != null }.associateBy { it.systemKey!! }
                val idRedirects = mutableMapOf<String, String>()
                val correctedIncomingProjects = normalized.projects.map { incomingProject ->
                    incomingProject.systemKey?.let { key ->
                        localSystemProjects[key]?.let { localSystemProject ->
                            if (localSystemProject.id != incomingProject.id) {
                                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Aligning system project. Key: $key, Old ID: ${incomingProject.id}, New ID: ${localSystemProject.id}")
                                idRedirects[incomingProject.id] = localSystemProject.id
                                return@map incomingProject.copy(id = localSystemProject.id)
                            }
                        }
                    }
                    incomingProject
                }

                // 2. Apply ID redirects to all related entities
                val correctedChanges = normalized.copy(
                    projects = correctedIncomingProjects.map { proj ->
                        idRedirects[proj.parentId]?.let { proj.copy(parentId = it) } ?: proj
                    },
                    listItems = normalized.listItems.map { li ->
                        val newProjectId = idRedirects[li.projectId]
                        val newEntityId = if (li.itemType == ListItemTypeValues.SUBLIST) idRedirects[li.entityId] else null
                        li.copy(
                            projectId = newProjectId ?: li.projectId,
                            entityId = newEntityId ?: li.entityId
                        )
                    },
                    documents = normalized.documents.map { doc ->
                        idRedirects[doc.projectId]?.let { doc.copy(projectId = it) } ?: doc
                    },
                    checklists = normalized.checklists.map { cl ->
                        idRedirects[cl.projectId]?.let { cl.copy(projectId = it) } ?: cl
                    },
                    projectAttachmentCrossRefs = normalized.projectAttachmentCrossRefs.map { crossRef ->
                        idRedirects[crossRef.projectId]?.let { crossRef.copy(projectId = it) } ?: crossRef
                    },
                    inboxRecords = normalized.inboxRecords.map { r ->
                        idRedirects[r.projectId]?.let { r.copy(projectId = it) } ?: r
                    },
                    projectExecutionLogs = normalized.projectExecutionLogs.map { log ->
                        idRedirects[log.projectId]?.let { log.copy(projectId = it) } ?: log
                    },
                     attachments = normalized.attachments.map { at ->
                        at.copy(ownerProjectId = at.ownerProjectId?.let { pid -> idRedirects[pid] ?: pid })
                    }
                )

                // 3. Merge entities
                val incomingProjects = mergeAndMark(
                    correctedChanges.projects,
                    local.projects.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() },
                    { p, synced -> normalizeProject(p).copy(syncedAt = synced) },
                    { it.isDeleted }
                ).filterNot { it.systemKey != null && it.isDeleted }

                if (incomingProjects.isNotEmpty()) projectDao.insertProjects(incomingProjects)

                val incomingGoals = mergeAndMark(
                    correctedChanges.goals,
                    local.goals.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() },
                    { g, synced -> normalizeGoal(g).copy(syncedAt = synced) }
                )
                if (incomingGoals.isNotEmpty()) goalDao.insertGoals(incomingGoals)

                val projectIds = (local.projects.map { it.id } + incomingProjects.map { it.id }).toSet()
                val goalIds = (local.goals.map { it.id } + incomingGoals.map { it.id }).toSet()

                val incomingListItems = mergeAndMark(
                    correctedChanges.listItems.filter { it.projectId in projectIds && (it.itemType != ListItemTypeValues.GOAL || it.entityId in goalIds) && (it.itemType != ListItemTypeValues.SUBLIST || it.entityId in projectIds) },
                    local.listItems.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() },
                    { li, synced -> li.copy(syncedAt = synced) },
                    { it.isDeleted }
                )
                if (incomingListItems.isNotEmpty()) listItemDao.insertItems(incomingListItems)
                
                val incomingNotes = mergeAndMark(
                    correctedChanges.legacyNotes, local.legacyNotes.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedAt }, { n, synced -> n.copy(syncedAt = synced) }
                )
                if (incomingNotes.isNotEmpty()) legacyNoteDao.insertAll(incomingNotes)

                val incomingDocs = mergeAndMark(
                    correctedChanges.documents, local.documents.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { d, synced -> d.copy(syncedAt = synced) }
                )
                if (incomingDocs.isNotEmpty()) noteDocumentDao.insertAllDocuments(incomingDocs)

                val incomingDocItems = mergeAndMark(
                    correctedChanges.documentItems, local.documentItems.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { di, synced -> di.copy(syncedAt = synced) }
                )
                if (incomingDocItems.isNotEmpty()) noteDocumentDao.insertAllDocumentItems(incomingDocItems)

                val incomingChecklists = mergeAndMark(
                    correctedChanges.checklists, local.checklists.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedAt }, { cl, synced -> cl.copy(syncedAt = synced) }
                )
                if (incomingChecklists.isNotEmpty()) checklistDao.insertChecklists(incomingChecklists)

                val checklistIds = (local.checklists.map { it.id } + incomingChecklists.map { it.id }).toSet()
                val incomingChecklistItems = mergeAndMark(
                    correctedChanges.checklistItems.filter { it.checklistId in checklistIds },
                    local.checklistItems.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedAt }, { cli, synced -> cli.copy(syncedAt = synced) }
                )
                if (incomingChecklistItems.isNotEmpty()) checklistDao.insertItems(incomingChecklistItems)

                val incomingActivities = mergeAndMark(
                    correctedChanges.activityRecords, local.activityRecords.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { ar, synced -> ar.copy(syncedAt = synced) }
                )
                if (incomingActivities.isNotEmpty()) activityRecordDao.insertAll(incomingActivities)

                val incomingLinks = mergeAndMark(
                    correctedChanges.linkItemEntities, local.linkItemEntities.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { li, synced -> li.copy(syncedAt = synced) }
                )
                if (incomingLinks.isNotEmpty()) linkItemDao.insertAll(incomingLinks)

                val incomingInbox = mergeAndMark(
                    correctedChanges.inboxRecords, local.inboxRecords.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { ir, synced -> ir.copy(syncedAt = synced) }
                )
                if (incomingInbox.isNotEmpty()) inboxRecordDao.insertAll(incomingInbox)

                val incomingLogs = mergeAndMark(
                    correctedChanges.projectExecutionLogs, local.projectExecutionLogs.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { log, synced -> log.copy(syncedAt = synced) }
                )
                if (incomingLogs.isNotEmpty()) projectManagementDao.insertAllLogs(incomingLogs)

                val incomingScripts = mergeAndMark(
                    correctedChanges.scripts, local.scripts.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedAt }, { sc, synced -> sc.copy(syncedAt = synced) }
                )
                incomingScripts.forEach { scriptDao.insert(it) }

                val incomingAttachments = mergeAndMark(
                    correctedChanges.attachments.filter { it.ownerProjectId == null || it.ownerProjectId in projectIds },
                    local.attachments.associateBy { it.id }, { it.id }, { it.version }, { it.updatedTs() }, { at, synced -> at.copy(syncedAt = synced) }
                )
                if (incomingAttachments.isNotEmpty()) attachmentDao.insertAttachments(incomingAttachments)

                val attachmentIds = (local.attachments.map { it.id } + incomingAttachments.map { it.id }).toSet()
                val incomingCrossRefs = mergeAndMark(
                    correctedChanges.projectAttachmentCrossRefs.filter { it.projectId in projectIds && it.attachmentId in attachmentIds },
                    local.projectAttachmentCrossRefs.associateBy { "${it.projectId}-${it.attachmentId}" },
                    { "${it.projectId}-${it.attachmentId}" }, { it.version }, { it.updatedTs() }, { cr, synced -> cr.copy(syncedAt = synced) }
                )
                if (incomingCrossRefs.isNotEmpty()) attachmentDao.insertProjectAttachmentLinks(incomingCrossRefs)
            }
            Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Applied at $ts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes", e)
            Result.failure(e)
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

    private suspend fun getChangesSince(since: Long): DatabaseContent {
        val local = loadLocalDatabaseContent()
        fun <T> filter(list: List<T>, updated: (T) -> Long) = list.filter { (updated(it)) > since }
        val changedProjects = filter(local.projects) { it.updatedTs() }
        val projectIds = changedProjects.map { it.id }.toSet()
        val changedGoals = filter(local.goals) { it.updatedTs() }
        val goalIds = changedGoals.map { it.id }.toSet()
        val changedListItems = filter(local.listItems) { it.updatedTs() }
            .filter { it.projectId in projectIds || it.entityId in goalIds || projectIds.isEmpty() && goalIds.isEmpty() }
        val changedLegacyNotes = filter(local.legacyNotes) { it.updatedTs() }
        val changedDocuments = filter(local.documents) { it.updatedTs() }
        val docIds = changedDocuments.map { it.id }.toSet()
        val changedDocumentItems = filter(local.documentItems) { it.updatedTs() }
            .filter { it.listId in docIds || docIds.isEmpty() }
        val changedChecklists = filter(local.checklists) { it.updatedTs() }
        val checklistIds = changedChecklists.map { it.id }.toSet()
        val changedChecklistItems = filter(local.checklistItems) { it.updatedTs() }
            .filter { it.checklistId in checklistIds || checklistIds.isEmpty() }
        val changedActivities = filter(local.activityRecords) { it.updatedTs() }
        val changedLinks = filter(local.linkItemEntities) { it.updatedTs() }
        val changedInbox = filter(local.inboxRecords) { it.updatedTs() }
        val changedLogs = filter(local.projectExecutionLogs) { it.updatedTs() }
        val changedScripts = filter(local.scripts) { it.updatedTs() }
        val changedCrossRefs = filter(local.projectAttachmentCrossRefs) { it.updatedTs() }
        val crossRefAttachmentIds = changedCrossRefs.map { it.attachmentId }.toSet()
        val changedAttachments = filter(local.attachments) { it.updatedTs() }
            .filter { it.id in crossRefAttachmentIds || crossRefAttachmentIds.isEmpty() }

        return DatabaseContent(
            projects = changedProjects,
            goals = changedGoals,
            listItems = changedListItems,
            legacyNotes = changedLegacyNotes,
            activityRecords = changedActivities,
            documents = changedDocuments,
            documentItems = changedDocumentItems,
            checklists = changedChecklists,
            checklistItems = changedChecklistItems,
            linkItemEntities = changedLinks,
            inboxRecords = changedInbox,
            projectExecutionLogs = changedLogs,
            scripts = changedScripts,
            attachments = changedAttachments,
            projectAttachmentCrossRefs = changedCrossRefs,
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
