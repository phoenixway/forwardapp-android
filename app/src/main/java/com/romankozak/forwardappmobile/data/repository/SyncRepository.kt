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
import com.romankozak.forwardappmobile.data.dao.AiEventDao
import com.romankozak.forwardappmobile.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.LifeSystemStateDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ProjectArtifactDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.ProjectStructureDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.BacklogOrder
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
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.data.database.models.ReservedProjectKeys
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.AttachmentsBackup
import com.romankozak.forwardappmobile.data.sync.BacklogOrderUtils
import com.romankozak.forwardappmobile.data.sync.NormalizedBacklogOrderResult
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
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
    private val backlogOrderDao: BacklogOrderDao,
    private val scriptDao: ScriptDao,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentDao: AttachmentDao,
    private val systemAppDao: SystemAppDao,
    // --- New DAOs for Full Backup ---
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val chatDao: ChatDao,
    private val conversationFolderDao: ConversationFolderDao,
    private val reminderDao: ReminderDao,
    private val recurringTaskDao: RecurringTaskDao,
    private val projectArtifactDao: ProjectArtifactDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiEventDao: AiEventDao,
    private val lifeSystemStateDao: LifeSystemStateDao,
    private val aiInsightDao: AiInsightDao,
    private val structurePresetDao: StructurePresetDao,
    private val structurePresetItemDao: StructurePresetItemDao,
    private val projectStructureDao: ProjectStructureDao,
) {
    private val TAG = "SyncRepository"
    private val WIFI_SYNC_LOG_TAG = "FWD_SYNC_TEST"
    private val debugDumpDir: File? = context.getExternalFilesDir("forwardapp-backup-dump")
    private val dumpDateFormat = SimpleDateFormat("yyyy-MM-dd---HH-mm-ss", Locale.US)
    private val maxDumpBytes = 20 * 1024 * 1024L

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

        val allAttachments = attachmentDao.getAll()
        val allCrossRefs = attachmentDao.getAllProjectAttachmentCrossRefs()
        val listItems = listItemDao.getAll()
        val backlogOrders = ensureBacklogOrdersSeeded(listItems)

        val synthesizedCrossRefs = synthesizeMissingCrossRefs(
            attachments = allAttachments,
            existingCrossRefs = allCrossRefs,
            logPrefix = "[createFullBackupJsonString]",
        )
        val databaseContent =
            DatabaseContent(
                goals = goalDao.getAll(),
                projects = projectDao.getAll(),
                listItems = listItems,
                backlogOrders = backlogOrders,
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
                attachments = allAttachments,
                projectAttachmentCrossRefs = synthesizedCrossRefs,
                // --- Extended Entities ---
                dayPlans = dayPlanDao.getAllPlansSync(),
                dayTasks = dayTaskDao.getAllTasksSync(),
                dailyMetrics = dailyMetricDao.getAllMetricsSync(),
                conversations = chatDao.getAllConversationsSync(),
                chatMessages = chatDao.getAllMessagesSync(),
                conversationFolders = conversationFolderDao.getAllFoldersSync(),
                reminders = reminderDao.getAllRemindersSync(),
                recurringTasks = recurringTaskDao.getAll(),
                systemApps = systemAppDao.getAll(),
                projectArtifacts = projectArtifactDao.getAll(),
                tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
                tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentCrossRefs(),
                aiEvents = aiEventDao.getAll(),
                aiInsights = aiInsightDao.getAllSync(),
                lifeSystemStates = lifeSystemStateDao.getAll(),
                structurePresets = structurePresetDao.getAllSync(),
                structurePresetItems = structurePresetItemDao.getAllItems(),
                projectStructures = projectStructureDao.getAllStructures(),
                projectStructureItems = projectStructureDao.getAllItems(),
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
        val EXPORT_TAG = "SyncRepo_AttachmentsExport"
        Log.d(EXPORT_TAG, "=== ATTACHMENTS EXPORT START ===")
        
        val documents = noteDocumentDao.getAllDocuments()
        Log.d(EXPORT_TAG, "Exporting ${documents.size} note documents")
        
        val documentItems = noteDocumentDao.getAllDocumentItems()
        Log.d(EXPORT_TAG, "Exporting ${documentItems.size} note document items")
        
        val checklists = checklistDao.getAllChecklists()
        Log.d(EXPORT_TAG, "Exporting ${checklists.size} checklists")
        
        val checklistItems = checklistDao.getAllChecklistItems()
        Log.d(EXPORT_TAG, "Exporting ${checklistItems.size} checklist items")
        
        val linkItems = linkItemDao.getAllEntities()
        Log.d(EXPORT_TAG, "Exporting ${linkItems.size} link items")
        val linkItemsWithDetails = linkItems.groupBy { it.syncedAt }.let {
            val synced = it[null]?.size ?: 0
            val notSynced = it.filterKeys { k -> k != null }.values.sumOf { it.size }
            "total=$synced synced, $notSynced not synced"
        }
        Log.d(EXPORT_TAG, "  LinkItems details: $linkItemsWithDetails")
        
        val attachments = attachmentDao.getAll()
        Log.d(EXPORT_TAG, "Exporting ${attachments.size} attachments")
        val attachmentsByStatus = attachments.groupBy { it.syncedAt }
        Log.d(EXPORT_TAG, "  Attachments status: unsynced=${(attachmentsByStatus[null]?.size ?: 0)}, synced=${attachmentsByStatus.filterKeys { it != null }.values.sumOf { it.size }}")
        
        val crossRefs = attachmentDao.getAllProjectAttachmentCrossRefs()
        Log.d(EXPORT_TAG, "Exporting ${crossRefs.size} attachment cross-refs")
        
        val synthesizedCrossRefs = synthesizeMissingCrossRefs(
            attachments = attachments,
            existingCrossRefs = crossRefs,
            logPrefix = "[createAttachmentsBackupJsonString]",
            persistToDb = true,
        )

        val attachmentsBackup =
            AttachmentsBackup(
                documents = documents,
                documentItems = documentItems,
                checklists = checklists,
                checklistItems = checklistItems,
                linkItemEntities = linkItems,
                attachments = attachments,
                projectAttachmentCrossRefs = synthesizedCrossRefs,
            )
        
        Log.d(EXPORT_TAG, "=== ATTACHMENTS EXPORT DONE ===")
        return gson.toJson(attachmentsBackup)
    }

    suspend fun createDeltaBackupJsonString(deltaSince: Long): String {
         val changes = getChangesSince(deltaSince)
         val enrichedCrossRefs = synthesizeMissingCrossRefs(
             attachments = changes.attachments,
             existingCrossRefs = changes.projectAttachmentCrossRefs,
             logPrefix = "[createDeltaBackupJsonString]",
             persistToDb = true,
         )
         val changesWithCrossRefs = changes.copy(projectAttachmentCrossRefs = enrichedCrossRefs)
         Log.d(WIFI_SYNC_LOG_TAG, "[createDeltaBackupJsonString] deltaSince=$deltaSince, changes: projects=${changesWithCrossRefs.projects.size}, goals=${changesWithCrossRefs.goals.size}, attachments=${changesWithCrossRefs.attachments.size}, crossRefs=${changesWithCrossRefs.projectAttachmentCrossRefs.size}")
         
         // DEFECT #2 CHECK: If attachments is 0 but there are local attachments, that's a problem
         val allLocalAttachments = attachmentDao.getAll()
         if (changesWithCrossRefs.attachments.isEmpty() && allLocalAttachments.isNotEmpty()) {
             Log.w(WIFI_SYNC_LOG_TAG, "[createDeltaBackupJsonString] DEFECT #2 DETECTED: Exporting 0 attachments but ${allLocalAttachments.size} exist locally. unsynced=${allLocalAttachments.count { it.syncedAt == null }}, synced=${allLocalAttachments.count { it.syncedAt != null }}")
         }
         
         val fullBackup = FullAppBackup(database = changesWithCrossRefs)
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
                Log.d(TAG, "parseBackupFile: JSON size=${jsonString.length}, first 500 chars: ${jsonString.take(500)}")
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
                Log.d(IMPORT_TAG, "=== ATTACHMENTS IMPORT TRANSACTION START ===")
                // Insert content entities first
                Log.d(IMPORT_TAG, "STEP1: Inserting ${validDocuments.size} note documents...")
                noteDocumentDao.insertAllDocuments(validDocuments)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${validDocuments.size} note documents.")
                
                Log.d(IMPORT_TAG, "STEP2: Inserting ${validDocumentItems.size} note document items...")
                noteDocumentDao.insertAllDocumentItems(validDocumentItems)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${validDocumentItems.size} note document items.")
                
                Log.d(IMPORT_TAG, "STEP3: Inserting ${validChecklists.size} checklists...")
                checklistDao.insertChecklists(validChecklists)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${validChecklists.size} checklists.")
                
                Log.d(IMPORT_TAG, "STEP4: Inserting ${validChecklistItems.size} checklist items...")
                checklistDao.insertItems(validChecklistItems)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${validChecklistItems.size} checklist items.")
                
                Log.d(IMPORT_TAG, "STEP5: Inserting ${backupData.linkItemEntities.size} link items...")
                linkItemDao.insertAll(backupData.linkItemEntities)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${backupData.linkItemEntities.size} link items.")

                // Insert attachments themselves
                // Filter attachments: include only those with valid ownerProjectId or orphans (ownerProjectId == null)
                Log.d(IMPORT_TAG, "STEP6: Processing ${backupData.attachments.size} attachments...")
                val validAttachments = backupData.attachments.filter { att ->
                    val isValid = att.ownerProjectId == null || att.ownerProjectId in existingProjectIds
                    if (!isValid) Log.w(IMPORT_TAG, "  ! Skipping orphaned attachment: id=${att.id}, ownerProject=${att.ownerProjectId}")
                    isValid
                }
                val orphanedAttachments = backupData.attachments.size - validAttachments.size
                Log.d(IMPORT_TAG, "  - Valid attachments: ${validAttachments.size}, orphaned: $orphanedAttachments")
                attachmentDao.insertAttachments(validAttachments)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${validAttachments.size} attachments.")

                // Insert only the valid cross-references
                Log.d(IMPORT_TAG, "STEP7: Processing ${validCrossRefs.size} attachment cross-refs...")
                val crossRefsByAttachment = validCrossRefs.groupBy { it.attachmentId }
                Log.d(IMPORT_TAG, "  - Cross-refs distribution: ${crossRefsByAttachment.size} unique attachments")
                crossRefsByAttachment.forEach { (attId, refs) ->
                    Log.d(IMPORT_TAG, "    - Attachment $attId: ${refs.size} projects")
                }
                attachmentDao.insertProjectAttachmentLinks(validCrossRefs)
                Log.d(IMPORT_TAG, "  ✓ Inserted ${validCrossRefs.size} attachment cross-refs.")
                Log.d(IMPORT_TAG, "=== ATTACHMENTS IMPORT TRANSACTION END ===")
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
            Log.d(IMPORT_TAG, "========== STARTING FULL BACKUP IMPORT ==========")
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

            // ============================================================================
            // КРОК 1: Перевірка цілісності БД ДО ІМПОРТУ
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 1: Перевірка цілісності БД до імпорту ===")
            val dbSystemProjects = projectDao.getAll().filter { it.systemKey != null }
            val dbDuplicatesByKey = dbSystemProjects.groupBy { it.systemKey }
            val dbDuplicateKeys = dbDuplicatesByKey.filter { it.value.size > 1 }.keys
            
            if (dbDuplicateKeys.isNotEmpty()) {
                val message = "CRITICAL: Database already has duplicate system keys: $dbDuplicateKeys. " +
                    "This violates system project invariants. Please reset database and reimport a clean backup."
                Log.e(IMPORT_TAG, message)
                return Result.failure(Exception(message))
            }
            Log.d(IMPORT_TAG, "✅ DB системні проекти унікальні (${dbSystemProjects.size} проектів)")

            // ============================================================================
            // КРОК 2: Будування маппінгу системних проектів
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 2: Будування маппінгу системних проектів ===")
            
            val existingSystemProjectsByKey = dbSystemProjects.associateBy { it.systemKey!! }
            Log.d(IMPORT_TAG, "Знайдено ${existingSystemProjectsByKey.size} системних проектів у БД")
            
            // Перевірити дублі в бекапі
            val backupSystemProjects = backup.projects.filter { it.systemKey != null }
            val backupDuplicatesByKey = backupSystemProjects.groupBy { it.systemKey }
            val backupDuplicateKeys = backupDuplicatesByKey.filter { it.value.size > 1 }.keys
            
            // Сніг-лист для очищення дублів у бекапі
            var projectsToImport = backup.projects
            
            if (backupDuplicateKeys.isNotEmpty()) {
                Log.w(IMPORT_TAG, "WARNING: Backup has duplicate system keys: $backupDuplicateKeys (обробляємо)")
                // Вибираємо "правильну" версію для кожного дублювального ключа
                val cleanedBackupSystem = mutableMapOf<String?, Project>()
                backupDuplicateKeys.forEach { key ->
                    val duplicates = backupDuplicatesByKey[key]!!
                    val chosen = duplicates.maxByOrNull { it.updatedAt ?: 0 } ?: duplicates.first()
                    cleanedBackupSystem[key] = chosen
                    Log.d(IMPORT_TAG, "  System проект '$key': Вибрано ${chosen.name} (${chosen.id}), видалено ${duplicates.size - 1} дублів")
                }
                // Замінити дублі в бекапі на вибрані версії
                projectsToImport = backup.projects.filter { proj ->
                    if (proj.systemKey != null && proj.systemKey in backupDuplicateKeys) {
                        proj.id == cleanedBackupSystem[proj.systemKey]?.id
                    } else {
                        true
                    }
                }
            }
            Log.d(IMPORT_TAG, "✅ Дублі в бекапі очищені")

            // ============================================================================
            // КРОК 3: Розрахунок ID-маппінгу для системних проектів
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 3: Розрахунок ID-маппінгу системних проектів ===")
            
            // projectIdMap: backupId -> actualId
            val projectIdMap = mutableMapOf<String, String>()
            
            val cleanedProjects = projectsToImport.map { projectFromBackup ->
                val normalizedIncoming = projectFromBackup.copy(
                    projectType = projectFromBackup.projectType ?: ProjectType.DEFAULT,
                    reservedGroup = com.romankozak.forwardappmobile.data.database.models.ReservedGroup.fromString(projectFromBackup.reservedGroup?.groupName),
                    // Do NOT force BACKLOG; keep incoming view mode
                    defaultViewModeName = projectFromBackup.defaultViewModeName,
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
                        // Система проект: порівнюємо версії
                        val incomingUpdated = normalizedIncoming.updatedAt ?: 0
                        val existingUpdated = existingSystemProject.updatedAt ?: 0
                        val incomingView = normalizedIncoming.defaultViewModeName ?: existingSystemProject.defaultViewModeName
                    
                        // Записуємо маппінг якщо ID різні
                        if (normalizedIncoming.id != existingSystemProject.id) {
                            projectIdMap[normalizedIncoming.id] = existingSystemProject.id
                            Log.d(IMPORT_TAG, "  System '$systemKey': маппінг ${normalizedIncoming.id} -> ${existingSystemProject.id}")
                    }
                    
                    // LWW (Last-Write-Wins) логіка
                        val candidate = normalizedIncoming.copy(id = existingSystemProject.id, defaultViewModeName = incomingView)
                        if (incomingUpdated > existingUpdated) {
                            Log.d(IMPORT_TAG, "  System '$systemKey': оновлюємо (incoming=$incomingUpdated > existing=$existingUpdated)")
                            candidate  // ← ВАЖЛИВО: зберігаємо існуючий ID!
                        } else {
                            Log.d(IMPORT_TAG, "  System '$systemKey': залишаємо локальну (existing=$existingUpdated >= incoming=$incomingUpdated)")
                            existingSystemProject
                        }
                } else {
                    // Новий проект (системний або звичайний)
                    normalizedIncoming
                }
            }
            Log.d(IMPORT_TAG, "✅ ID-маппінг розраховано: ${projectIdMap.size} переіндексацій")

            // ============================================================================
            // КРОК 4: Валідація та очищення parentId
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 4: Валідація та очищення parentId ===")
            
            val projectIdsSet = cleanedProjects.map { it.id }.toSet()
            Log.d(IMPORT_TAG, "Всього проектів після нормалізації: ${cleanedProjects.size}")
            
            // Будуємо маппу systemKey -> actualId для швидкого пошуку
            val systemKeyToActualId = mutableMapOf<String, String>()
            cleanedProjects.forEach { proj ->
                if (proj.systemKey != null) {
                    systemKeyToActualId[proj.systemKey!!] = proj.id
                }
            }
            
            // Функція для правильної переіндексації батьків системних проектів
            val remapParentId: (String?) -> String? = { parentId ->
                parentId?.let { pid ->
                    when {
                        // 1. Спочатку перевірити прямий маппінг ID
                        pid in projectIdMap -> {
                            val mappedId = projectIdMap[pid]!!
                            // 2. Якщо помапнений проект є системним, отримати його актуальний ID
                            val mappedProj = cleanedProjects.find { it.id == mappedId }
                            if (mappedProj?.systemKey != null) {
                                systemKeyToActualId[mappedProj.systemKey!!] ?: mappedId
                            } else {
                                mappedId
                            }
                        }
                        // 3. Якщо ID є в наборі — залишити як є
                        pid in projectIdsSet -> pid
                        // 4. Інакше — null (батько не існує)
                        else -> null
                    }
                }
            }
            
            var projectsCleaned = 0
            val cleanedProjectsWithParents = cleanedProjects.map { proj ->
                val mappedParent = remapParentId(proj.parentId)
                
                if (mappedParent == null && proj.parentId != null) {
                    projectsCleaned++
                    Log.w(IMPORT_TAG, "  Очищення: ${proj.id} (${proj.name}) батько ${proj.parentId} не існує")
                    proj.copy(parentId = null)
                } else if (mappedParent != proj.parentId) {
                    Log.d(IMPORT_TAG, "  Переіндексація: ${proj.id} батько ${proj.parentId} -> $mappedParent")
                    proj.copy(parentId = mappedParent)
                } else {
                    proj
                }
            }
            
            Log.d(IMPORT_TAG, "✅ Очищено проектів: $projectsCleaned, всього батьків: ${cleanedProjectsWithParents.count { it.parentId != null }}")

            // ============================================================================
            // КРОК 5: ВАЛІДАЦІЯ ПЕРЕД ВСТАВКОЮ (КРИТИЧНЕ)
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 5: Валідація цілісності перед вставкою ===")
            
            val finalProjectIds = cleanedProjectsWithParents.map { it.id }.toSet()
            
            // 5A. Перевірити що немає orphan projects
            val orphans = cleanedProjectsWithParents.filter { 
                it.parentId != null && it.parentId !in finalProjectIds 
            }
            if (orphans.isNotEmpty()) {
                val message = "ABORT: Found ${orphans.size} projects with invalid parents after remapping: " +
                    orphans.take(5).joinToString { "${it.name}(${it.id})->${it.parentId}" }
                Log.e(IMPORT_TAG, message)
                return Result.failure(Exception(message))
            }
            Log.d(IMPORT_TAG, "✅ Батьки: всі валідні (${cleanedProjectsWithParents.count { it.parentId != null }} з дітьми)")
            
            // 5B. Перевірити що системні проекти унікальні
            val finalSystemProjects = cleanedProjectsWithParents.filter { it.systemKey != null }
            val duplicateSystemKeys = finalSystemProjects.groupBy { it.systemKey }
                .filter { it.value.size > 1 }
                .keys
            if (duplicateSystemKeys.isNotEmpty()) {
                val message = "ABORT: Found duplicate systemKeys after processing: $duplicateSystemKeys"
                Log.e(IMPORT_TAG, message)
                return Result.failure(Exception(message))
            }
            Log.d(IMPORT_TAG, "✅ Системні проекти: унікальні (${finalSystemProjects.size} системних ключів)")

            // ============================================================================
            // КРОК 6: Переіндексація сутностей з projectId
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 6: Переіндексація сутностей з projectId ===")
            
            val projectIds = cleanedProjectsWithParents.map { it.id }.toSet()
            val goalIds = backup.goals.map { it.id }.toSet()
            
            // ListItems - переіндексація projectId
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
            Log.d(IMPORT_TAG, "  ListItems: ${backup.listItems.size} -> ${cleanedListItems.size}")
            
            // NoteDocuments - переіндексація projectId
            val cleanedDocuments = backup.documents.map { doc ->
                doc.copy(projectId = projectIdMap[doc.projectId] ?: doc.projectId)
            }.filter { it.projectId in projectIds }.also {
                val skipped = backup.documents.size - it.size
                if (skipped > 0) Log.w(IMPORT_TAG, "  NoteDocuments: пропущено $skipped з невалідними посиланнями")
            }
            Log.d(IMPORT_TAG, "  NoteDocuments: ${backup.documents.size} -> ${cleanedDocuments.size}")
            
            // Checklists - переіндексація projectId
            val cleanedChecklists = backup.checklists.map { cl ->
                cl.copy(projectId = projectIdMap[cl.projectId] ?: cl.projectId)
            }.filter { it.projectId in projectIds }.also {
                val skipped = backup.checklists.size - it.size
                if (skipped > 0) Log.w(IMPORT_TAG, "  Checklists: пропущено $skipped з невалідними посиланнями")
            }
            Log.d(IMPORT_TAG, "  Checklists: ${backup.checklists.size} -> ${cleanedChecklists.size}")
            
            // InboxRecords - переіндексація projectId
            val cleanedInboxRecords = backup.inboxRecords.map { rec ->
                rec.copy(projectId = projectIdMap[rec.projectId] ?: rec.projectId)
            }.filter { it.projectId in projectIds }.also {
                val skipped = backup.inboxRecords.size - it.size
                if (skipped > 0) Log.w(IMPORT_TAG, "  InboxRecords: пропущено $skipped з невалідними посиланнями")
            }
            Log.d(IMPORT_TAG, "  InboxRecords: ${backup.inboxRecords.size} -> ${cleanedInboxRecords.size}")
            
            // ProjectLogs - переіндексація projectId
            val cleanedProjectLogs = backup.projectExecutionLogs.map { log ->
                log.copy(projectId = projectIdMap[log.projectId] ?: log.projectId)
            }.filter { it.projectId in projectIds }.also {
                val skipped = backup.projectExecutionLogs.size - it.size
                if (skipped > 0) Log.w(IMPORT_TAG, "  ProjectLogs: пропущено $skipped з невалідними посиланнями")
            }
            Log.d(IMPORT_TAG, "  ProjectLogs: ${backup.projectExecutionLogs.size} -> ${cleanedProjectLogs.size}")
            
            // Attachments - переіндексація ownerProjectId
            val cleanedAttachments = backup.attachments.map { att ->
                att.copy(ownerProjectId = att.ownerProjectId?.let { projectIdMap[it] ?: it })
            }.filter { it.ownerProjectId == null || it.ownerProjectId in projectIds }.also {
                val skipped = backup.attachments.size - it.size
                if (skipped > 0) Log.w(IMPORT_TAG, "  Attachments: пропущено $skipped з невалідними посиланнями")
            }
            Log.d(IMPORT_TAG, "  Attachments: ${backup.attachments.size} -> ${cleanedAttachments.size}")
            
            // ProjectAttachmentCrossRefs - переіндексація projectId
            val cleanedCrossRefs = backup.projectAttachmentCrossRefs.map { cr ->
                cr.copy(projectId = projectIdMap[cr.projectId] ?: cr.projectId)
            }.filter { it.projectId in projectIds }.also {
                val skipped = backup.projectAttachmentCrossRefs.size - it.size
                if (skipped > 0) Log.w(IMPORT_TAG, "  CrossRefs: пропущено $skipped з невалідними посиланнями")
            }
            Log.d(IMPORT_TAG, "  CrossRefs: ${backup.projectAttachmentCrossRefs.size} -> ${cleanedCrossRefs.size}")
            
            Log.d(IMPORT_TAG, "✅ Крок 6 завершен: всі сутності переіндексовані")

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

                // --- Extended Cleanup ---
                dayTaskDao.deleteAllTasks()
                dayPlanDao.deleteAllPlans()
                dailyMetricDao.deleteAllMetrics()

                chatDao.deleteAllMessages()
                chatDao.deleteAllConversations()
                conversationFolderDao.deleteAllFolders()

                reminderDao.deleteAll()
                recurringTaskDao.deleteAll()

                projectArtifactDao.deleteAll()

                tacticalMissionDao.deleteAllMissionAttachmentCrossRefs()
                tacticalMissionDao.deleteAllMissions()

                aiEventDao.deleteAll()
                lifeSystemStateDao.deleteAll()
                aiInsightDao.clearAll()

                structurePresetItemDao.deleteAllItems()
                structurePresetDao.deleteAll()

                projectStructureDao.deleteAllItems()
                projectStructureDao.deleteAllStructures()

                systemAppDao.deleteAll()

                Log.d(IMPORT_TAG, "Всі таблиці очищено.")

            // ============================================================================
            // КРОК 7: Вставка в базу даних (з использованием cleaned даних)
            // ============================================================================
            Log.d(IMPORT_TAG, "=== КРОК 7: Вставка в базу даних ===")
            
            Log.d(IMPORT_TAG, "Транзакція: вставка базових сутностей.")
            goalDao.insertGoals(backup.goals)
            projectDao.insertProjects(cleanedProjectsWithParents)
            listItemDao.insertItems(cleanedListItems)
            Log.d(IMPORT_TAG, "  - Вставлено: ${backup.goals.size} goals, ${cleanedProjectsWithParents.size} projects, ${cleanedListItems.size} listItems.")

            legacyNoteDao.insertAll(backup.legacyNotes)
            Log.d(IMPORT_TAG, "  - Вставлено: ${backup.legacyNotes.size} legacyNotes.")

            // Використовуємо cleaned документи замість переобробки
            val validDocumentIds = cleanedDocuments.map { it.id }.toSet()
            noteDocumentDao.insertAllDocuments(cleanedDocuments)
            Log.d(
                IMPORT_TAG,
                "  - Вставлено: ${cleanedDocuments.size} noteDocuments."
            )
            
            val validDocumentItems = backup.documentItems.filter { it.listId in validDocumentIds }
            val skippedDocumentItems = backup.documentItems.size - validDocumentItems.size
            noteDocumentDao.insertAllDocumentItems(validDocumentItems)
            Log.d(
                IMPORT_TAG,
                "  - Вставлено: ${validDocumentItems.size} noteDocumentItems. Skipped invalid doc refs=$skippedDocumentItems"
            )

            // Використовуємо cleaned checklists
            if (cleanedChecklists.isNotEmpty()) {
                checklistDao.insertChecklists(cleanedChecklists)
                Log.d(IMPORT_TAG, "  - Вставлено: ${cleanedChecklists.size} checklists.")
            }
            
            if (backup.checklistItems.isNotEmpty()) {
                val validChecklistIds = cleanedChecklists.map { it.id }.toSet()
                val validChecklistItems = backup.checklistItems.filter { it.checklistId in validChecklistIds }
                val skippedChecklistItems = backup.checklistItems.size - validChecklistItems.size
                checklistDao.insertItems(validChecklistItems)
                Log.d(IMPORT_TAG, "  - Вставлено: ${validChecklistItems.size} checklistItems. Skipped invalid checklist refs=$skippedChecklistItems")
            }

            activityRecordDao.insertAll(backup.activityRecords)
            Log.d(IMPORT_TAG, "  - Вставлено: ${backup.activityRecords.size} activityRecords.")
            linkItemDao.insertAll(backup.linkItemEntities)
            Log.d(IMPORT_TAG, "  - Вставлено: ${backup.linkItemEntities.size} linkItems.")
            
            // Використовуємо cleaned inbox records
            inboxRecordDao.insertAll(cleanedInboxRecords)
            Log.d(
                IMPORT_TAG,
                "  - Вставлено: ${cleanedInboxRecords.size} inboxRecords."
            )
            
            // Використовуємо cleaned project logs
            projectManagementDao.insertAllLogs(cleanedProjectLogs)
            Log.d(
                IMPORT_TAG,
                "  - Вставлено: ${cleanedProjectLogs.size} projectLogs."
            )
            
            backup.scripts.forEach { scriptDao.insert(it) }
            Log.d(IMPORT_TAG, "  - Вставлено: ${backup.scripts.size} scripts.")
            recentItemDao.insertAll(recentItemsToInsert)
            Log.d(IMPORT_TAG, "  - Вставлено: ${recentItemsToInsert.size} recentItems.")

            // Використовуємо cleaned attachments
            attachmentDao.insertAttachments(cleanedAttachments)
            Log.d(IMPORT_TAG, "  - Вставлено: ${cleanedAttachments.size} attachments.")
            
            // Verify actual count in DB after insert
            val actualAttachmentCount = attachmentDao.getAll().size
            Log.d(IMPORT_TAG, "  [VERIFY] Actual attachments in DB after insert: $actualAttachmentCount")
            
            // Використовуємо cleaned crossrefs
            val attachmentIds = cleanedAttachments.map { it.id }.toSet()
            val validCrossRefs = cleanedCrossRefs.filter { it.attachmentId in attachmentIds && it.projectId in projectIds }
            attachmentDao.insertProjectAttachmentLinks(validCrossRefs)
            Log.d(
                IMPORT_TAG,
                "  - Вставлено: ${validCrossRefs.size} projectAttachmentCrossRefs."
            )
            
            // Verify actual count in DB after insert
            val actualCrossRefCount = attachmentDao.getAllProjectAttachmentCrossRefs().size
            Log.d(IMPORT_TAG, "  [VERIFY] Actual crossRefs in DB after insert: $actualCrossRefCount")

            // --- Extended Insert ---
            if (backup.dayPlans.isNotEmpty()) {
                dayPlanDao.insertAll(backup.dayPlans)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.dayPlans.size} dayPlans.")
            }
            if (backup.dayTasks.isNotEmpty()) {
                dayTaskDao.insertAll(backup.dayTasks)
                Log.d(IMPORT_TAG, "  - Вставлено: ${backup.dayTasks.size} dayTasks.")
            }
            backup.dailyMetrics.forEach { dailyMetricDao.insert(it) }
            
            backup.conversationFolders.forEach { conversationFolderDao.insertFolder(it) }
            backup.conversations.forEach { chatDao.insertConversation(it) }
            backup.chatMessages.forEach { chatDao.insertMessage(it) }
            
            backup.reminders.forEach { reminderDao.insert(it) }
            backup.recurringTasks.forEach { recurringTaskDao.insert(it) }
            
            backup.systemApps.forEach { systemAppDao.upsert(it) }
            backup.projectArtifacts.forEach { projectArtifactDao.insert(it) }
            
            backup.tacticalMissions.forEach { tacticalMissionDao.insertMission(it) }
            backup.tacticalMissionAttachments.forEach { tacticalMissionDao.insertMissionAttachmentCrossRef(it) }
            
            backup.aiEvents.forEach { aiEventDao.insert(it) }
            if (backup.aiInsights.isNotEmpty()) aiInsightDao.upsertAll(backup.aiInsights)
            backup.lifeSystemStates.forEach { lifeSystemStateDao.upsert(it) }
            
            backup.structurePresets.forEach { structurePresetDao.insertPreset(it) }
            if (backup.structurePresetItems.isNotEmpty()) structurePresetItemDao.insertItems(backup.structurePresetItems)
            
            backup.projectStructures.forEach { projectStructureDao.insertStructure(it) }
            if (backup.projectStructureItems.isNotEmpty()) projectStructureDao.insertItems(backup.projectStructureItems)

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
                cleanedDocuments.forEach {
                        attachmentRepository.ensureAttachmentLinkedToProject(
                            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
                            entityId = it.id,
                            projectId = it.projectId,
                            ownerProjectId = it.projectId,
                            createdAt = it.createdAt,
                        )
                    }
                    cleanedChecklists.forEach {
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
                    Log.d(IMPORT_TAG, "Attachments уже присутні в бекапі, пропускаємо автоматичне створення. attachments=${cleanedAttachments.size}, crossRefs=${validCrossRefs.size}")
                    }

                    Log.d(IMPORT_TAG, "✅ Крок 7 завершен: вставка в базу даних закончена.")
                    }

                    Log.i(IMPORT_TAG, "Orphan projects after import (parentId cleared): 0")
            runPostBackupMigration()

            Log.i(IMPORT_TAG, "Імпорт бекапу успішно завершено.")
            return Result.success("Backup imported successfully!")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Під час імпорту сталася критична помилка. Повідомлення: ${e.message}", e)
            return Result.failure(e)
        }
    }

    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> = 
        try {
            Log.d(WIFI_SYNC_LOG_TAG, "[fetchBackupFromWifi] DEBUG_ENTER address=$address deltaSince=$deltaSince")
            var cleanAddress = address.trim()
            if (!cleanAddress.startsWith("http://") && !cleanAddress.startsWith("https://")) {
                cleanAddress = "http://$cleanAddress"
            }
            val uri = cleanAddress.toUri()
            val port = if (uri.port != -1) uri.port else settingsRepository.wifiSyncPortFlow.first()
            val hostAndPort = "${uri.host}:$port"
            val fullUrl = if (deltaSince != null) {
                "http://$hostAndPort/export?deltaSince=$deltaSince"
            } else {
                "http://$hostAndPort/export"
            }
            Log.d(WIFI_SYNC_LOG_TAG, "[fetchBackupFromWifi] GET $fullUrl (deltaSince=$deltaSince)")
            val response: String = client.get(fullUrl).body()
            Log.d(WIFI_SYNC_LOG_TAG, "[fetchBackupFromWifi] Success, bytes=${response.length}")
            Log.d(WIFI_SYNC_LOG_TAG, "[fetchBackupFromWifi] DEBUG_MARK: reached after HTTP GET")
            writeDebugDump("import", response)
            Result.success(response)
        } catch (e: Exception) {
            Log.e(WIFI_SYNC_LOG_TAG, "Error fetching from Wi‑Fi", e)
            Result.failure(e)
        }

    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> = 
        try {
            Log.d(WIFI_SYNC_LOG_TAG, "[pushUnsyncedToWifi] DEBUG_ENTER address=$address")
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
            writeDebugDump("export", payload)
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
        Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] START: ts=$ts, projects=${content.projects.size}, docs=${content.documents.size}, attachs=${content.attachments.size}, crossRefs=${content.projectAttachmentCrossRefs.size}")
        appDatabase.withTransaction {
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.projects.size} projects synced")
            projectDao.insertProjects(content.projects.map { it.copy(syncedAt = ts) })
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.goals.size} goals synced")
            goalDao.insertGoals(content.goals.map { it.copy(syncedAt = ts) })
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.listItems.size} listItems synced")
            listItemDao.insertItems(content.listItems.map { it.copy(syncedAt = ts) })
            
            if (content.backlogOrders.isNotEmpty()) {
                Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.backlogOrders.size} backlog orders synced")
                backlogOrderDao.insertOrders(content.backlogOrders.map { it.copy(syncedAt = ts) })
            }
            
            legacyNoteDao.insertAll(content.legacyNotes.map { it.copy(syncedAt = ts) })
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.documents.size} note documents synced")
            noteDocumentDao.insertAllDocuments(content.documents.map { it.copy(syncedAt = ts) })
            
            noteDocumentDao.insertAllDocumentItems(content.documentItems.map { it.copy(syncedAt = ts) })
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.checklists.size} checklists synced")
            checklistDao.insertChecklists(content.checklists.map { it.copy(syncedAt = ts) })
            checklistDao.insertItems(content.checklistItems.map { it.copy(syncedAt = ts) })
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.activityRecords.size} activity records synced")
            activityRecordDao.insertAll(content.activityRecords.map { it.copy(syncedAt = ts) })
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.linkItemEntities.size} link items synced")
            linkItemDao.insertAll(content.linkItemEntities.map { it.copy(syncedAt = ts) })
            
            inboxRecordDao.insertAll(content.inboxRecords.map { it.copy(syncedAt = ts) })
            projectManagementDao.insertAllLogs(content.projectExecutionLogs.map { it.copy(syncedAt = ts) })
            content.scripts.forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.attachments.size} attachments synced")
            val markedAttachments = content.attachments.map { it.copy(syncedAt = ts) }
            if (markedAttachments.isNotEmpty()) {
                markedAttachments.take(3).forEach {
                    Log.d(WIFI_SYNC_LOG_TAG, "  [MARK-SYNCED] Attachment: id=${it.id}, type=${it.attachmentType}, entity=${it.entityId}, owner=${it.ownerProjectId}, version=${it.version}, syncedAt=${it.syncedAt}")
                }
            }
            attachmentDao.insertAttachments(markedAttachments)
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] Marking ${content.projectAttachmentCrossRefs.size} attachment cross-refs synced")
            val markedCrossRefs = content.projectAttachmentCrossRefs.map { it.copy(syncedAt = ts) }
            if (markedCrossRefs.isNotEmpty()) {
                markedCrossRefs.take(3).forEach {
                    Log.d(WIFI_SYNC_LOG_TAG, "  [MARK-SYNCED-XREF] CrossRef: project=${it.projectId}, attachment=${it.attachmentId}, version=${it.version}, syncedAt=${it.syncedAt}")
                }
            }
            attachmentDao.insertProjectAttachmentLinks(markedCrossRefs)
            
            Log.d(WIFI_SYNC_LOG_TAG, "[markSyncedNow] DONE")
        }
    }

    suspend fun getLastSyncTime(): Long? {
        val local = loadLocalDatabaseContent()
        // Find the earliest syncedAt time (oldest sync) to use as deltaSince for next sync
        val allSyncedTimes = listOfNotNull(
            local.projects.mapNotNull { it.syncedAt }.minOrNull(),
            local.goals.mapNotNull { it.syncedAt }.minOrNull(),
            local.documents.mapNotNull { it.syncedAt }.minOrNull(),
            local.attachments.mapNotNull { it.syncedAt }.minOrNull(),
            local.projectAttachmentCrossRefs.mapNotNull { it.syncedAt }.minOrNull(),
            local.backlogOrders.mapNotNull { it.syncedAt }.minOrNull(),
        )
        return allSyncedTimes.minOrNull()
    }

    suspend fun createSyncReport(jsonString: String): SyncReport {
        val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
        val db = backup.database ?: return SyncReport(emptyList())

        val localProjectsAll = projectDao.getAll()
        val localProjects = localProjectsAll.associateBy { it.id }
        val localGoals = goalDao.getAll().associateBy { it.id }
        val localListItems = listItemDao.getAll()
            .filter { it.projectId in localProjects.keys }
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
            .filter { it.projectId in incomingProjectIds }
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

    private suspend fun loadLocalDatabaseContent(): DatabaseContent {
        val recentProjectEntries = recentItemDao.getAll().map { recentItem ->
            com.romankozak.forwardappmobile.data.sync.RecentProjectEntry(
                projectId = recentItem.target,
                timestamp = recentItem.lastAccessed,
            )
        }
        val scripts = scriptDao.getAll().first()
        val listItems = listItemDao.getAll()
        val backlogOrders = ensureBacklogOrdersSeeded(listItems)

        return DatabaseContent(
            goals = goalDao.getAll(),
            projects = projectDao.getAll(),
            listItems = listItems,
            backlogOrders = backlogOrders,
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
    }

    suspend fun getChangesSince(since: Long): DatabaseContent {
         val local = loadLocalDatabaseContent()
         fun <T> filterByUpdated(items: List<T>, updatedSelector: (T) -> Long?): List<T> =
             items.filter { (updatedSelector(it) ?: 0L) > since }

         fun <T> unsyncedAndUpdated(
             items: List<T>,
             syncedSelector: (T) -> Long?,
             updatedSelector: (T) -> Long?,
         ): Pair<List<T>, List<T>> {
             val unsynced = items.filter { syncedSelector(it) == null }
             val updated = items.filter { (updatedSelector(it) ?: 0L) > since && syncedSelector(it) != null }
             return unsynced to updated
         }

        val (projectsUnsync, projectsUpdated) = unsyncedAndUpdated(local.projects, { it.syncedAt }, { it.updatedTs() })
        val projectsResult = projectsUnsync + projectsUpdated

        val (goalsUnsync, goalsUpdated) = unsyncedAndUpdated(local.goals, { it.syncedAt }, { it.updatedTs() })
        val goalsResult = goalsUnsync + goalsUpdated

        val projectIds = local.projects.map { it.id }.toSet()
        val goalIds = local.goals.map { it.id }.toSet()
         val existingDocIds = local.documents.map { it.id }.toSet()
         val checklistIds = local.checklists.map { it.id }.toSet()

         // ========== DEFECT #3 FIX: Include new local attachments in export ==========
         // Attachments should be exported if:
         // 1. Unsynced (syncedAt=null)
         // 2. Updated after 'since' (modification export)
         // 3. Any attachment that's newly created should be in unsynced list
         
         // For attachments: export if unsync'd (syncedAt=null) OR updated after 'since'
         val attachmentsUnsync = local.attachments.filter { it.syncedAt == null }
         val attachmentsUpdated = local.attachments.filter { (it.updatedTs() ?: 0L) > since && it.syncedAt != null }
         
         // ========== POTENTIAL DEFECT #3 FIX: Validate ownerProjectId ==========
         // Attachments with non-existent ownerProjectId will be rejected by desktop
         // We should warn about this or preserve them as orphans
         val attachmentsWithInvalidOwner = (attachmentsUnsync + attachmentsUpdated).filter { 
             it.ownerProjectId != null && it.ownerProjectId !in projectIds 
         }
         if (attachmentsWithInvalidOwner.isNotEmpty()) {
             Log.w(WIFI_SYNC_LOG_TAG, "[getChangesSince] WARNING: ${attachmentsWithInvalidOwner.size} attachments have invalid ownerProjectId (will be rejected by desktop)")
             attachmentsWithInvalidOwner.take(3).forEach {
                 Log.d(WIFI_SYNC_LOG_TAG, "  ! Invalid owner: id=${it.id}, owner=${it.ownerProjectId} (not in projects)")
             }
         }
         
         val attachmentsResult = attachmentsUnsync + attachmentsUpdated
         
         // For crossRefs: export if unsync'd (syncedAt=null) OR updated after 'since'
         val crossRefsUnsync = local.projectAttachmentCrossRefs.filter { it.syncedAt == null }
         val crossRefsUpdated = local.projectAttachmentCrossRefs.filter { (it.updatedTs() ?: 0L) > since && it.syncedAt != null }
         val crossRefsResult = crossRefsUnsync + crossRefsUpdated

         // Documents: export unsynced OR updated (avoid sending attachments without documents)
         val (documentsUnsync, documentsUpdated) = unsyncedAndUpdated(local.documents, { it.syncedAt }, { it.updatedTs() })
         val documentsResult = (documentsUnsync + documentsUpdated).filter { it.projectId in projectIds }
         val docIds = documentsResult.map { it.id }.toSet().ifEmpty { existingDocIds }

         // Document items: export unsynced OR updated
         val (documentItemsUnsync, documentItemsUpdated) = unsyncedAndUpdated(local.documentItems, { it.syncedAt }, { it.updatedTs() })
         // Include items for exported documents even if items themselves were not updated after 'since'
         val rawDocumentItems = documentItemsUnsync + documentItemsUpdated +
             local.documentItems.filter { it.listId in docIds }
         val documentItemsResult = rawDocumentItems.filter { it.listId in docIds }

         // Checklists: export unsynced OR updated
         val (checklistsUnsync, checklistsUpdated) = unsyncedAndUpdated(local.checklists, { it.syncedAt }, { it.updatedTs() })
         val rawChecklistsResult = checklistsUnsync + checklistsUpdated
         val checklistsResult = rawChecklistsResult.filter { it.projectId in projectIds }
         val checklistIdsForExport = checklistsResult.map { it.id }.toSet()
         val skippedChecklists = rawChecklistsResult.size - checklistsResult.size

         // Checklist items: export unsynced OR updated
         val (checklistItemsUnsync, checklistItemsUpdated) = unsyncedAndUpdated(local.checklistItems, { it.syncedAt }, { it.updatedTs() })
         // Include items for exported checklists even if items themselves were not updated after 'since'
         val rawChecklistItemsResult = checklistItemsUnsync + checklistItemsUpdated +
             local.checklistItems.filter { it.checklistId in checklistIdsForExport }
         val checklistItemsResult = rawChecklistItemsResult.filter { it.checklistId in checklistIds }
         val skippedChecklistItems = rawChecklistItemsResult.size - checklistItemsResult.size

         // List items: export unsynced OR updated
         val dedupedLocalListItems = dedupListItems(local.listItems)
         val (listItemsUnsync, listItemsUpdated) = unsyncedAndUpdated(dedupedLocalListItems, { it.syncedAt }, { it.updatedTs() })
         val rawListItemsResult = listItemsUnsync + listItemsUpdated
         val listItemsResult = rawListItemsResult
             .filter { it.projectId in projectIds || it.entityId in goalIds }

         val dedupedLocalBacklogOrders = dedupBacklogOrders(local.backlogOrders)
         val (backlogOrdersUnsync, backlogOrdersUpdated) = unsyncedAndUpdated(dedupedLocalBacklogOrders, { it.syncedAt }, { it.updatedTs() })
         val rawBacklogOrders = backlogOrdersUnsync + backlogOrdersUpdated
         val backlogOrdersResult = rawBacklogOrders
             .filter { it.listId in projectIds && it.itemId in (projectIds + goalIds) }

         // Links: export unsynced OR updated
         val (linkItemsUnsync, linkItemsUpdated) = unsyncedAndUpdated(local.linkItemEntities, { it.syncedAt }, { it.updatedTs() })
         val linkItemsResult = linkItemsUnsync + linkItemsUpdated
         
         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] since=$since (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(since))})")
         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] Attachments: total=${local.attachments.size}, unsync'd=${attachmentsUnsync.size}, updated=${attachmentsUpdated.size}, result=${attachmentsResult.size}")
         
         // DEFECT #3 LOGGING: Track if new attachments are properly exported
         if (attachmentsUnsync.isNotEmpty()) {
             Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] DEFECT #3 INFO: Found ${attachmentsUnsync.size} unsynced attachments that WILL be exported")
             attachmentsUnsync.take(5).forEach {
                 Log.d(WIFI_SYNC_LOG_TAG, "  [EXPORT-UNSYNC] Attachment: id=${it.id}, type=${it.attachmentType}, entity=${it.entityId}, owner=${it.ownerProjectId}, createdAt=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(it.createdAt))}, version=${it.version}")
             }
         }
         
         if (attachmentsUpdated.isNotEmpty()) {
             Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] Found ${attachmentsUpdated.size} updated attachments that WILL be exported")
         }
         
         if (attachmentsResult.isNotEmpty()) {
             attachmentsResult.drop(attachmentsUnsync.size).take(5).forEach {
                 val isUnsync = it.syncedAt == null
                 val isUpdated = (it.updatedTs() ?: 0L) > since
                 Log.d(WIFI_SYNC_LOG_TAG, "  [EXPORT] Attachment: id=${it.id}, type=${it.attachmentType}, entity=${it.entityId}, owner=${it.ownerProjectId}, unsync'd=$isUnsync, updated=$isUpdated, updatedAt=${it.updatedAt}, syncedAt=${it.syncedAt}, version=${it.version}")
             }
         }
         
         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] CrossRefs: total=${local.projectAttachmentCrossRefs.size}, unsync'd=${crossRefsUnsync.size}, updated=${crossRefsUpdated.size}, result=${crossRefsResult.size}")
         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] Documents: total=${local.documents.size}, unsync'd=${documentsUnsync.size}, updated=${documentsUpdated.size}, result=${documentsResult.size} (skipped_invalid_project=${documentsUnsync.size + documentsUpdated.size - documentsResult.size})")
         if (documentsResult.size < documentsUnsync.size + documentsUpdated.size) {
             (documentsUnsync + documentsUpdated)
                 .filter { it.projectId !in projectIds }
                 .take(3)
                 .forEach { Log.w(WIFI_SYNC_LOG_TAG, "[getChangesSince] Skipping document with invalid projectId=${it.projectId}, id=${it.id}") }
         }
         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] DocumentItems: total=${local.documentItems.size}, unsync'd=${documentItemsUnsync.size}, updated=${documentItemsUpdated.size}, result=${documentItemsResult.size} (skipped_invalid_doc=${rawDocumentItems.size - documentItemsResult.size})")
         if (documentItemsResult.size < rawDocumentItems.size) {
             rawDocumentItems
                 .filter { it.listId !in docIds }
                 .take(3)
                 .forEach { Log.w(WIFI_SYNC_LOG_TAG, "[getChangesSince] Skipping documentItem with invalid docId=${it.listId}, id=${it.id}") }
         }
         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] Checklists: total=${local.checklists.size}, unsync'd=${checklistsUnsync.size}, updated=${checklistsUpdated.size}, result=${checklistsResult.size} (skipped_invalid_project=$skippedChecklists)")
         if (skippedChecklists > 0) {
             rawChecklistsResult.filter { it.projectId !in projectIds }.take(3).forEach {
                 Log.w(WIFI_SYNC_LOG_TAG, "[getChangesSince] Skipping checklist with invalid projectId=${it.projectId}, id=${it.id}")
             }
         }

         Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] ChecklistItems: total=${local.checklistItems.size}, unsync'd=${checklistItemsUnsync.size}, updated=${checklistItemsUpdated.size}, result=${checklistItemsResult.size} (skipped_invalid_checklist=$skippedChecklistItems)")
         if (skippedChecklistItems > 0) {
             rawChecklistItemsResult.filter { it.checklistId !in checklistIds }.take(3).forEach {
                 Log.w(WIFI_SYNC_LOG_TAG, "[getChangesSince] Skipping checklistItem with invalid checklistId=${it.checklistId}, id=${it.id}")
             }
         }
        Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] LinkItems: total=${local.linkItemEntities.size}, unsync'd=${linkItemsUnsync.size}, updated=${linkItemsUpdated.size}, result=${linkItemsResult.size}")

        return DatabaseContent(
            projects = projectsResult,
            goals = goalsResult,
            listItems = listItemsResult,
            backlogOrders = backlogOrdersResult,
            legacyNotes = filterByUpdated(local.legacyNotes) { it.updatedTs() },
            documents = documentsResult,
            documentItems = documentItemsResult,
            checklists = checklistsResult,
            checklistItems = checklistItemsResult,
            activityRecords = filterByUpdated(local.activityRecords) { it.updatedTs() },
            linkItemEntities = linkItemsResult,
            inboxRecords = filterByUpdated(local.inboxRecords) { it.updatedTs() },
            projectExecutionLogs = filterByUpdated(local.projectExecutionLogs) { it.updatedTs() },
            scripts = filterByUpdated(local.scripts) { it.updatedTs() },
            attachments = attachmentsResult,
            projectAttachmentCrossRefs = crossRefsResult,
            recentProjectEntries = emptyList(),
        )
    }

    private suspend fun synthesizeMissingCrossRefs(
        attachments: List<AttachmentEntity>,
        existingCrossRefs: List<ProjectAttachmentCrossRef>,
        logPrefix: String,
        persistToDb: Boolean = true,
    ): List<ProjectAttachmentCrossRef> {
        val existingKeys = existingCrossRefs.associateBy { "${it.projectId}-${it.attachmentId}" }
        val synthesized = attachments.mapNotNull { attachment ->
            val owner = attachment.ownerProjectId ?: return@mapNotNull null
            val key = "$owner-${attachment.id}"
            if (existingKeys.containsKey(key)) return@mapNotNull null

            ProjectAttachmentCrossRef(
                projectId = owner,
                attachmentId = attachment.id,
                attachmentOrder = -attachment.updatedAt, // keep ordering roughly by recency
                updatedAt = attachment.updatedAt,
                syncedAt = attachment.syncedAt,
                isDeleted = attachment.isDeleted,
                version = attachment.version,
            )
        }

        val combined = (existingCrossRefs + synthesized).distinctBy { "${it.projectId}-${it.attachmentId}" }
        if (synthesized.isNotEmpty()) {
            Log.w(
                WIFI_SYNC_LOG_TAG,
                "$logPrefix Synthesized ${synthesized.size} missing crossRefs from attachment.ownerProjectId. dbCrossRefs=${existingCrossRefs.size}, total=${combined.size}",
            )
            if (persistToDb) {
                // Heal the database so attachments don't disappear from the Android UI after sync
                runCatching { attachmentDao.insertProjectAttachmentLinks(synthesized) }
                    .onSuccess {
                        Log.d(
                            WIFI_SYNC_LOG_TAG,
                            "$logPrefix Persisted synthesized crossRefs to DB: inserted=${synthesized.size}",
                        )
                    }
                    .onFailure { error ->
                        Log.e(
                            WIFI_SYNC_LOG_TAG,
                            "$logPrefix Failed to persist synthesized crossRefs: ${error.message}",
                            error,
                        )
                    }
            }
        } else {
            Log.d(
                WIFI_SYNC_LOG_TAG,
                "$logPrefix CrossRefs intact: dbCrossRefs=${existingCrossRefs.size}, synthesized=0",
            )
        }
        return combined
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
                    // Skip systemKey projects; they are prepopulated locally
                    val incomingRegular = selectedData.projects.filter { it.systemKey == null }
                    val regularProjects = incomingRegular
                        .let { keepNewer(it, local.projects.associateBy { p -> p.id }, { it.id }, { it.version }, { it.updatedAt }) }
                    if (regularProjects.isNotEmpty()) {
                        projectDao.insertProjects(regularProjects)
                        Log.d(IMPORT_TAG, "  - Upserted ${regularProjects.size} projects (systemKey skipped=${selectedData.projects.size - incomingRegular.size}).")
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
                if (selectedData.backlogOrders.isNotEmpty()) {
                    val importedProjectIds = selectedData.projects.map { it.id }.toSet()
                    val importedGoalIds = selectedData.goals.map { it.id }.toSet()
                    val validBacklogOrders = selectedData.backlogOrders.filter {
                        it.listId in importedProjectIds && it.itemId in (importedGoalIds + importedProjectIds)
                    }.let {
                        keepNewer(
                            it,
                            local.backlogOrders.associateBy { bo -> bo.id },
                            { it.id },
                            { it.orderVersion },
                            { it.updatedAt ?: it.orderVersion }
                        )
                    }
                    if (validBacklogOrders.isNotEmpty()) {
                        backlogOrderDao.insertOrders(validBacklogOrders)
                        Log.d(IMPORT_TAG, "  - Upserted ${validBacklogOrders.size} backlog orders (filtered from ${selectedData.backlogOrders.size}).")
                    }
                    if (validBacklogOrders.size < selectedData.backlogOrders.size) {
                        Log.w(IMPORT_TAG, "  - Skipped ${selectedData.backlogOrders.size - validBacklogOrders.size} backlog orders with missing references.")
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

    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff {
        val local = loadLocalDatabaseContent()

        fun <T> diffEntities(
            incomingList: List<T>,
            localList: List<T>,
            idSelector: (T) -> String,
            versionSelector: (T) -> Long,
            updatedSelector: (T) -> Long,
            isDeletedSelector: (T) -> Boolean = { false },
        ): DiffResult<T> {
            val localMap = localList.associateBy(idSelector)
            val incomingMap = incomingList.associateBy(idSelector)

            val added = incomingList.filter { idSelector(it) !in localMap && !isDeletedSelector(it) }

            val deleted = localList.mapNotNull { loc ->
                val incomingMatch = incomingMap[idSelector(loc)]
                if (incomingMatch != null && isDeletedSelector(incomingMatch)) loc else null
            }

            val updated = incomingList.mapNotNull { inc ->
                if (isDeletedSelector(inc)) return@mapNotNull null
                val localItem = localMap[idSelector(inc)] ?: return@mapNotNull null
                val incVer = versionSelector(inc)
                val locVer = versionSelector(localItem)
                val incUpdated = updatedSelector(inc)
                val locUpdated = updatedSelector(localItem)
                val changed = incVer > locVer || (incVer == locVer && incUpdated > locUpdated) || inc != localItem
                if (changed) UpdatedItem(local = localItem, incoming = inc) else null
            }

            return DiffResult(added = added, updated = updated, deleted = deleted)
        }

        val normalizedProjects = incoming.projects.map { normalizeProject(it) }
        val normalizedGoals = incoming.goals.map { normalizeGoal(it) }

        return BackupDiff(
            projects = diffEntities(normalizedProjects, local.projects, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            goals = diffEntities(normalizedGoals, local.goals, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            listItems = diffEntities(incoming.listItems, local.listItems, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            backlogOrders = diffEntities(incoming.backlogOrders, local.backlogOrders, { it.id }, { it.orderVersion }, { it.updatedTs() }, { it.isDeleted }),
            legacyNotes = diffEntities(incoming.legacyNotes, local.legacyNotes, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            documents = diffEntities(incoming.documents, local.documents, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            documentItems = diffEntities(incoming.documentItems, local.documentItems, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            checklists = diffEntities(incoming.checklists, local.checklists, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            checklistItems = diffEntities(incoming.checklistItems, local.checklistItems, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            activityRecords = diffEntities(incoming.activityRecords, local.activityRecords, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            linkItems = diffEntities(incoming.linkItemEntities, local.linkItemEntities, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            inboxRecords = diffEntities(incoming.inboxRecords, local.inboxRecords, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            projectExecutionLogs = diffEntities(incoming.projectExecutionLogs, local.projectExecutionLogs, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            scripts = diffEntities(incoming.scripts, local.scripts, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            attachments = diffEntities(incoming.attachments, local.attachments, { it.id }, { it.version }, { it.updatedTs() }, { it.isDeleted }),
            projectAttachmentCrossRefs = diffEntities(
                incoming.projectAttachmentCrossRefs,
                local.projectAttachmentCrossRefs,
                { "${it.projectId}-${it.attachmentId}" },
                { it.version },
                { it.updatedTs() },
                { it.isDeleted },
            ),
        )
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
    private fun BacklogOrder.updatedTs(): Long = this.updatedAt ?: this.orderVersion
    private fun ProjectExecutionLog.updatedTs(): Long = this.updatedAt ?: this.timestamp
    private fun ScriptEntity.updatedTs(): Long = this.updatedAt
    private fun AttachmentEntity.updatedTs(): Long = this.updatedAt
    private fun ProjectAttachmentCrossRef.updatedTs(): Long = this.updatedAt ?: this.attachmentOrder.toLong()
    private fun DayPlan.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun DayTask.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun DailyMetric.updatedTs(): Long = this.updatedAt ?: this.createdAt
    private fun Reminder.updatedTs(): Long = this.updatedAt ?: this.creationTime

    private fun dedupListItems(items: List<ListItem>): List<ListItem> =
        items.groupBy { Triple(it.projectId, it.entityId, it.itemType) }
            .mapNotNull { (_, candidates) ->
                candidates.maxWithOrNull(
                    compareBy<ListItem> { it.version }
                        .thenBy { it.updatedTs() }
                        .thenBy { if (it.isDeleted) 1 else 0 },
                )
            }

    private fun dedupBacklogOrders(items: List<BacklogOrder>): List<BacklogOrder> =
        BacklogOrderUtils.dedupBacklogOrders(items)

    private suspend fun ensureBacklogOrdersSeeded(listItems: List<ListItem>): List<BacklogOrder> {
        val existing = backlogOrderDao.getAll()
        val existingIds = existing.associateBy { it.id }
        val missing = listItems.filter { it.id !in existingIds }
            .map { BacklogOrderUtils.listItemToBacklogOrder(it) }
        if (missing.isNotEmpty()) {
            Log.w(WIFI_SYNC_LOG_TAG, "[SeedBacklogOrder] Seeding ${missing.size} missing orders from listItems")
            backlogOrderDao.insertOrders(missing)
        }
        return dedupBacklogOrders(existing + missing)
    }

    private suspend fun cleanupListItemDuplicates(
        projectIds: Set<String>,
        goalIds: Set<String>,
        backlogValidIds: Set<String>,
    ) {
        val all = listItemDao.getAll()
        val valid = dedupListItems(
            all.filter { it.projectId in projectIds && (it.itemType != ListItemTypeValues.GOAL || it.entityId in goalIds) && (it.itemType != ListItemTypeValues.SUBLIST || it.entityId in projectIds) },
        )
        val keepIds = valid.map { it.id }.toSet()
        val toDelete = all.map { it.id }.filterNot { it in keepIds }
        if (toDelete.isNotEmpty()) {
            Log.w(WIFI_SYNC_LOG_TAG, "[applyServerChanges][DedupListItems] Removing ${toDelete.size} duplicate listItems (keep=${keepIds.size})")
            listItemDao.deleteItemsByIds(toDelete)
            backlogOrderDao.deleteOrders(toDelete)
        }

        val allOrders = backlogOrderDao.getAll()
        val validOrders = dedupBacklogOrders(allOrders.filter { it.listId in projectIds && it.itemId in backlogValidIds })
        val keepOrderIds = validOrders.map { it.id }.toSet()
        val orderDeletes = allOrders.map { it.id }.filterNot { it in keepOrderIds }
        if (orderDeletes.isNotEmpty()) {
            Log.w(WIFI_SYNC_LOG_TAG, "[applyServerChanges][DedupBacklogOrders] Removing ${orderDeletes.size} duplicate backlogOrders (keep=${keepOrderIds.size})")
            backlogOrderDao.deleteOrders(orderDeletes)
        }
    }

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
        val unsynced = DatabaseContent(
            projects = local.projects.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("projects", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            goals = local.goals.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("goals", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            listItems = dedupListItems(local.listItems).filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("listItems", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            backlogOrders = dedupBacklogOrders(local.backlogOrders).filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("backlogOrders", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            legacyNotes = local.legacyNotes.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            documents = local.documents.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("documents", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            documentItems = local.documentItems.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            checklists = local.checklists.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            checklistItems = local.checklistItems.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            activityRecords = local.activityRecords.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            linkItemEntities = local.linkItemEntities.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("linkItems", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            inboxRecords = local.inboxRecords.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            projectExecutionLogs = local.projectExecutionLogs.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            scripts = local.scripts.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            attachments = local.attachments.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("attachments", it, { it.id }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            projectAttachmentCrossRefs = local.projectAttachmentCrossRefs.filter { isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) }
                .also { logUnsynced("crossRefs", it, { "${it.projectId}:${it.attachmentId}" }, { it.updatedTs() }, { it.syncedAt }, { it.isDeleted }) },
            recentProjectEntries = emptyList(),
        )
        Log.d(WIFI_SYNC_LOG_TAG, "[getUnsyncedChanges] SUMMARY: docs=${unsynced.documents.size} docItems=${unsynced.documentItems.size} attachs=${unsynced.attachments.size} crossRefs=${unsynced.projectAttachmentCrossRefs.size}")
        return unsynced
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
                val attachmentsBeforeRedirect = normalized.attachments
                val attachmentsAfterRedirect = normalized.attachments.mapNotNull {
                    if (it.attachmentType == null) {
                        Log.w(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Skipping attachment with null type id=${it.id} entity=${it.entityId} owner=${it.ownerProjectId}")
                        return@mapNotNull null
                    }
                    val newOwnerId = it.ownerProjectId?.let { pid -> idRedirects[pid] ?: pid }
                    if (newOwnerId != it.ownerProjectId) {
                        Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Redirecting attachment ownerProjectId: id=${it.id}, old=${it.ownerProjectId}, new=$newOwnerId")
                    }
                    it.copy(ownerProjectId = newOwnerId)
                }
                
                val correctedChanges = normalized.copy(
                    projects = correctedIncomingProjects.map { proj ->
                        idRedirects[proj.parentId]?.let { proj.copy(parentId = it) } ?: proj
                    },
                    listItems = normalized.listItems.map { li ->
                        val newProjectId = idRedirects[li.projectId] ?: li.projectId
                        val newEntityId = if (li.itemType == ListItemTypeValues.SUBLIST) idRedirects[li.entityId] ?: li.entityId else li.entityId
                        li.copy(projectId = newProjectId, entityId = newEntityId)
                    },
                    backlogOrders = normalized.backlogOrders.map { bo ->
                        val newListId = idRedirects[bo.listId] ?: bo.listId
                        val newItemId = idRedirects[bo.itemId] ?: bo.itemId
                        bo.copy(listId = newListId, itemId = newItemId)
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
                     attachments = attachmentsAfterRedirect
                )

                // Preserve local view mode if incoming does not specify one
                val incomingProjectsWithViewMode = correctedChanges.projects.map { inc ->
                    val localProj = local.projects.find { it.id == inc.id }
                    val viewMode = inc.defaultViewModeName ?: localProj?.defaultViewModeName
                    if (inc.defaultViewModeName == null && viewMode != null) {
                        Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Keeping local view mode for project ${inc.id}: $viewMode")
                    }
                    inc.copy(defaultViewModeName = viewMode)
                }

                // 3. Merge entities
                val incomingProjects = mergeAndMark(
                    incomingProjectsWithViewMode,
                    local.projects.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() },
                    { p, synced -> normalizeProject(p).copy(syncedAt = synced) },
                    ts, { it.isDeleted } 
                ).filterNot { it.systemKey != null && it.isDeleted }

                if (incomingProjects.isNotEmpty()) projectDao.insertProjects(incomingProjects)

                val incomingGoals = mergeAndMark(
                    correctedChanges.goals,
                    local.goals.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() },
                    { g, synced -> normalizeGoal(g).copy(syncedAt = synced) },
                    ts, { it.isDeleted }
                )
                if (incomingGoals.isNotEmpty()) goalDao.insertGoals(incomingGoals)

                val projectIds = (local.projects.map { it.id } + incomingProjects.map { it.id }).toSet()
                val goalIds = (local.goals.map { it.id } + incomingGoals.map { it.id }).toSet()
                // Use every known project (local + incoming) for FK validation of docs/attachments/crossRefs
                val allProjectIds = projectIds
                val backlogValidIds = goalIds + allProjectIds
                
                val incomingOrderKeys = correctedChanges.backlogOrders
                    .filter { it.listId in projectIds && it.itemId in backlogValidIds }
                    .map { it.listId to it.itemId }
                    .toSet()
                val localOrderByKey = local.backlogOrders.associateBy { it.listId to it.itemId }

                val incomingListItemsPrepared = correctedChanges.listItems
                    .filter { it.projectId in projectIds && (it.itemType != ListItemTypeValues.GOAL || it.entityId in goalIds) && (it.itemType != ListItemTypeValues.SUBLIST || it.entityId in projectIds) }
                    .map { li ->
                        val key = li.projectId to li.entityId
                        val localOrder = localOrderByKey[key]
                        val hasIncomingOrder = key in incomingOrderKeys
                        if (localOrder != null && !hasIncomingOrder) {
                            val updatedAt = maxOf(li.updatedAt ?: 0L, localOrder.updatedAt ?: 0L, localOrder.orderVersion)
                            val version = maxOf(li.version, localOrder.orderVersion, updatedAt)
                            li.copy(
                                order = localOrder.order,
                                version = version,
                                updatedAt = updatedAt,
                            )
                        } else {
                            li
                        }
                    }

                val incomingOrdersNormalized: NormalizedBacklogOrderResult = BacklogOrderUtils.normalizeBacklogOrderSets(
                    incomingListItemsPrepared,
                    correctedChanges.backlogOrders.filter { it.listId in projectIds && it.itemId in backlogValidIds },
                )
                val orderOverrideMap = dedupBacklogOrders(local.backlogOrders + incomingOrdersNormalized.backlogOrders)
                    .associateBy { it.listId to it.itemId }

                val dedupedIncomingListItems = dedupListItems(
                    incomingOrdersNormalized.listItems
                        .map { li ->
                            val override = orderOverrideMap[li.projectId to li.entityId]
                            if (override != null && !override.isDeleted) {
                                li.copy(
                                    order = override.order,
                                    version = maxOf(li.version, override.orderVersion),
                                    updatedAt = maxOf(li.updatedAt ?: 0L, override.updatedAt ?: 0L, override.orderVersion),
                                )
                            } else {
                                li
                            }
                        },
                )
                val incomingListItems = mergeAndMark(
                    dedupedIncomingListItems,
                    local.listItems.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() },
                    { li, synced -> li.copy(syncedAt = synced) },
                    ts, { it.isDeleted }
                )
                if (incomingListItems.isNotEmpty()) listItemDao.insertItems(incomingListItems)

                val incomingBacklogOrders = mergeAndMark(
                    dedupBacklogOrders(incomingOrdersNormalized.backlogOrders + dedupedIncomingListItems.map { BacklogOrderUtils.listItemToBacklogOrder(it) }),
                    local.backlogOrders.associateBy { it.id },
                    { it.id }, { it.orderVersion }, { it.updatedTs() },
                    { bo, synced -> bo.copy(syncedAt = synced) },
                    ts, { it.isDeleted }
                )
                if (incomingBacklogOrders.isNotEmpty()) backlogOrderDao.insertOrders(incomingBacklogOrders)
                cleanupListItemDuplicates(projectIds, goalIds, backlogValidIds)
                ensureBacklogOrdersSeeded(listItemDao.getAll())
                
                val incomingNotes = mergeAndMark(
                    correctedChanges.legacyNotes, local.legacyNotes.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { n, synced -> n.copy(syncedAt = synced) },
                    ts
                )
                if (incomingNotes.isNotEmpty()) legacyNoteDao.insertAll(incomingNotes)

                val incomingDocs = mergeAndMark(
                    correctedChanges.documents.filter { it.projectId in allProjectIds }, local.documents.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { d, synced -> d.copy(syncedAt = synced) },
                    ts,
                    logConsumer = { local, incoming ->
                        Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges][DocumentMergeCheck] Local: $local, Incoming: $incoming")
                    }
                )
                if (incomingDocs.isNotEmpty()) noteDocumentDao.insertAllDocuments(incomingDocs)

                val docIds = (local.documents.map { it.id } + incomingDocs.map { it.id }).toSet()

                val incomingDocItems = mergeAndMark(
                    correctedChanges.documentItems.filter { it.listId in docIds }, local.documentItems.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { di, synced -> di.copy(syncedAt = synced) },
                    ts
                )
                if (incomingDocItems.isNotEmpty()) noteDocumentDao.insertAllDocumentItems(incomingDocItems)

                val incomingChecklists = mergeAndMark(
                    correctedChanges.checklists.filter { it.projectId in allProjectIds }, local.checklists.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { cl, synced -> cl.copy(syncedAt = synced) },
                    ts
                )
                if (incomingChecklists.isNotEmpty()) checklistDao.insertChecklists(incomingChecklists)

                val checklistIds = (local.checklists.map { it.id } + incomingChecklists.map { it.id }).toSet()
                val incomingChecklistItems = mergeAndMark(
                    correctedChanges.checklistItems.filter { it.checklistId in checklistIds },
                    local.checklistItems.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { cli, synced -> cli.copy(syncedAt = synced) },
                    ts
                )
                if (incomingChecklistItems.isNotEmpty()) checklistDao.insertItems(incomingChecklistItems)

                val incomingActivities = mergeAndMark(
                    correctedChanges.activityRecords, local.activityRecords.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { ar, synced -> ar.copy(syncedAt = synced) },
                    ts
                )
                if (incomingActivities.isNotEmpty()) activityRecordDao.insertAll(incomingActivities)

                val incomingLinks = mergeAndMark(
                    correctedChanges.linkItemEntities, local.linkItemEntities.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { li, synced -> li.copy(syncedAt = synced) },
                    ts
                )
                if (incomingLinks.isNotEmpty()) linkItemDao.insertAll(incomingLinks)

                val incomingInbox = mergeAndMark(
                    correctedChanges.inboxRecords, local.inboxRecords.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { ir, synced -> ir.copy(syncedAt = synced) },
                    ts
                )
                if (incomingInbox.isNotEmpty()) inboxRecordDao.insertAll(incomingInbox)

                val incomingLogs = mergeAndMark(
                    correctedChanges.projectExecutionLogs, local.projectExecutionLogs.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { log, synced -> log.copy(syncedAt = synced) },
                    ts
                )
                if (incomingLogs.isNotEmpty()) projectManagementDao.insertAllLogs(incomingLogs)

                val incomingScripts = mergeAndMark(
                    correctedChanges.scripts, local.scripts.associateBy { it.id },
                    { it.id }, { it.version }, { it.updatedTs() }, { sc, synced -> sc.copy(syncedAt = synced) },
                    ts
                )
                incomingScripts.forEach { scriptDao.insert(it) }

                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Processing attachments. Total incoming: ${correctedChanges.attachments.size}, delta projects: ${projectIds.size}, all local projects: ${allProjectIds.size}")
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] IMPORTANT: Incoming attachments count: ${correctedChanges.attachments.size}. If 0, this indicates desktop didn't export them.")
                
                // ========== DEFECT #3 FIX: Use ALL local project IDs for filtering, not just delta projects ==========
                // This prevents massive data loss when attachments belong to projects not in the current delta
                // BEFORE (buggy): using projectIds only → 106 → 12 loss
                // AFTER (fixed): using allLocalProjectIds → all attachments preserved
                
                val attachmentsWithoutOwner = correctedChanges.attachments.count { it.ownerProjectId == null }
                val attachmentsWithInvalidOwner = correctedChanges.attachments.count { it.ownerProjectId != null && it.ownerProjectId !in allProjectIds }
                val attachmentsWithValidOwner = correctedChanges.attachments.count { it.ownerProjectId != null && it.ownerProjectId in allProjectIds }
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Attachments breakdown: orphans=${attachmentsWithoutOwner}, truly_invalid=${attachmentsWithInvalidOwner}, valid=${attachmentsWithValidOwner}")
                
                // Log local attachments state BEFORE processing
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Local attachments BEFORE: total=${local.attachments.size}, synced=${local.attachments.count { it.syncedAt != null }}, unsynced=${local.attachments.count { it.syncedAt == null }}")
                local.attachments.filter { it.syncedAt == null }.take(3).forEach {
                    Log.d(WIFI_SYNC_LOG_TAG, "  Local unsynced: id=${it.id}, type=${it.attachmentType}, entity=${it.entityId}, owner=${it.ownerProjectId}, version=${it.version}")
                }
                
                if (attachmentsWithInvalidOwner > 0) {
                    correctedChanges.attachments.filter { it.ownerProjectId != null && it.ownerProjectId !in allProjectIds }.take(5).forEach {
                        Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Filtered out attachment (truly orphaned): id=${it.id}, type=${it.attachmentType}, entity=${it.entityId}, ownerProjectId=${it.ownerProjectId}")
                    }
                }
                
                // ========== DEFECT #4 FIX: Handle attachments that are already synced ==========
                // When Desktop re-sends attachments that were already synced, mergeAndMark() skips them
                // because version hasn't changed. We need to ensure synced attachments stay marked as synced.
                // Track which attachments are already synced locally
                val alreadySyncedLocalAttachments = local.attachments.filter { it.syncedAt != null }.associateBy { it.id }
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] DEFECT #4: Found ${alreadySyncedLocalAttachments.size} already-synced local attachments")
                
                // Process incoming attachments using ALL local projects (DEFECT #3 FIX)
                val incomingAttachments = mergeAndMark(
                    correctedChanges.attachments.filter { it.ownerProjectId == null || it.ownerProjectId in allProjectIds },
                    local.attachments.associateBy { it.id }, { it.id }, { it.version }, { it.updatedTs() }, { at, synced -> at.copy(syncedAt = synced) },
                    ts
                )
                // DEFECT #5: If incoming attachments match local by id/version but are not newer, they won't be returned by mergeAndMark.
                // We still need to mark local copies as synced to avoid them staying unsynced and "disappearing" from UI.
                val matchedExistingAttachments = correctedChanges.attachments
                    .filter { it.ownerProjectId == null || it.ownerProjectId in allProjectIds }
                    .mapNotNull { inc -> local.attachments.find { it.id == inc.id } }
                    .filter { it.syncedAt == null } // only those still unsynced locally
                    .map { it.copy(syncedAt = ts) }
                if (matchedExistingAttachments.isNotEmpty()) {
                    Log.d(
                        WIFI_SYNC_LOG_TAG,
                        "[applyServerChanges] DEFECT #5: Marking ${matchedExistingAttachments.size} locally existing attachments as synced (matched incoming ids)",
                    )
                    matchedExistingAttachments.take(3).forEach {
                        Log.d(WIFI_SYNC_LOG_TAG, "  [DEFECT #5] Mark-synced existing: id=${it.id}, owner=${it.ownerProjectId}")
                    }
                }
                
                // DEFECT #4 FIX: Re-include attachments that were already synced but filtered by mergeAndMark
                // This prevents losing sync state when Desktop re-sends the same attachments
                val validIncomingIds = correctedChanges.attachments
                    .filter { it.ownerProjectId == null || it.ownerProjectId in allProjectIds }
                    .map { it.id }
                    .toSet()
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] DEFECT #4: Incoming valid attachment IDs: ${validIncomingIds.size}")
                
                val alreadySyncedReincluded = correctedChanges.attachments
                    .filter { it.id in validIncomingIds && it.id in alreadySyncedLocalAttachments }
                    .map { it.copy(syncedAt = ts) }
                
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] DEFECT #4: Re-including ${alreadySyncedReincluded.size} already-synced attachments to maintain sync state")
                if (alreadySyncedReincluded.isNotEmpty()) {
                    alreadySyncedReincluded.take(3).forEach {
                        Log.d(WIFI_SYNC_LOG_TAG, "  [DEFECT #4] Re-included: id=${it.id}, syncedAt=${it.syncedAt}")
                    }
                }
                
                val allIncomingAttachments = incomingAttachments + alreadySyncedReincluded
                val uniqueIncomingAttachments = allIncomingAttachments.distinctBy { it.id }
                
                // DEFECT #1 FIX: When desktop sends 0 attachments, mark unsynced local attachments as synced
                // This prevents them from being stuck in "unsynced" state forever
                val unsyncedLocalAttachments = if (correctedChanges.attachments.isEmpty()) {
                    local.attachments
                        .filter { it.syncedAt == null && (it.ownerProjectId == null || it.ownerProjectId in allProjectIds) }
                        .map { it.copy(syncedAt = ts) }
                } else {
                    emptyList()
                }
                
                if (unsyncedLocalAttachments.isNotEmpty()) {
                    Log.w(WIFI_SYNC_LOG_TAG, "[applyServerChanges] DEFECT #1 HANDLED: Desktop sent 0 attachments but marking ${unsyncedLocalAttachments.size} local unsynced attachments as synced to prevent limbo state")
                }
                
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] After merge: ${incomingAttachments.size} from mergeAndMark, ${alreadySyncedReincluded.size} re-included, ${unsyncedLocalAttachments.size} from DEFECT #1")
                
                // Combine all attachment sources: incoming (new/updated), already-synced (re-included), and DEFECT #1 (local unsynced)
                val allAttachmentsToInsert = uniqueIncomingAttachments + unsyncedLocalAttachments + matchedExistingAttachments
                if (allAttachmentsToInsert.isNotEmpty()) {
                    allAttachmentsToInsert.take(3).forEach {
                        Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Attachment to insert: id=${it.id}, type=${it.attachmentType}, entity=${it.entityId}, syncedAt=${it.syncedAt}, ownerProjectId=${it.ownerProjectId}, version=${it.version}")
                    }
                    attachmentDao.insertAttachments(allAttachmentsToInsert)
                }

                // Use all attachment ids after merge (including re-included and DEFECT #1 path) to validate crossRefs
                val attachmentIds = (local.attachments.map { it.id } + uniqueIncomingAttachments.map { it.id } + unsyncedLocalAttachments.map { it.id } + matchedExistingAttachments.map { it.id }).toSet()
                val synthesizedCrossRefs = synthesizeMissingCrossRefs(
                    attachments = correctedChanges.attachments,
                    existingCrossRefs = correctedChanges.projectAttachmentCrossRefs,
                    logPrefix = "[applyServerChanges]",
                    persistToDb = false, // defer actual insert to mergeAndMark below to respect FK order
                )
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Processing crossRefs. Total incoming: ${synthesizedCrossRefs.size}, attachments in scope: ${attachmentIds.size}, delta projects: ${projectIds.size}, all local projects: ${allProjectIds.size}")
                
                // Log crossRef filtering details (also use allLocalProjectIds for crossRefs - DEFECT #3 FIX)
                val validCrossRefs = synthesizedCrossRefs.filter { it.projectId in allProjectIds && it.attachmentId in attachmentIds }
                val invalidProjectCrossRefs = synthesizedCrossRefs.count { it.projectId !in allProjectIds }
                val invalidAttachmentCrossRefs = synthesizedCrossRefs.count { it.projectId in allProjectIds && it.attachmentId !in attachmentIds }
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] CrossRefs breakdown: valid=${validCrossRefs.size}, invalid_project=${invalidProjectCrossRefs}, invalid_attachment=${invalidAttachmentCrossRefs}")
                
                if (invalidProjectCrossRefs > 0 || invalidAttachmentCrossRefs > 0) {
                    synthesizedCrossRefs.filter { it.projectId !in allProjectIds || it.attachmentId !in attachmentIds }.take(5).forEach {
                        Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Filtered out crossRef: projectId=${it.projectId} (valid=${it.projectId in allProjectIds}), attachmentId=${it.attachmentId} (valid=${it.attachmentId in attachmentIds})")
                    }
                }
                
                val incomingCrossRefs = mergeAndMark(
                    validCrossRefs,
                    local.projectAttachmentCrossRefs.associateBy { "${it.projectId}-${it.attachmentId}" },
                    { "${it.projectId}-${it.attachmentId}" }, { it.version }, { it.updatedTs() }, { cr, synced -> cr.copy(syncedAt = synced) },
                    ts
                )
                Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] After merge: ${incomingCrossRefs.size} crossRefs to insert")
                if (incomingCrossRefs.isNotEmpty()) attachmentDao.insertProjectAttachmentLinks(incomingCrossRefs)

                // Safety net: ensure every validCrossRef exists even if mergeAndMark skipped (same version/timestamp)
                val existingCrossRefKeys = (local.projectAttachmentCrossRefs + incomingCrossRefs).map { "${it.projectId}-${it.attachmentId}" }.toSet()
                val missingCrossRefs = validCrossRefs
                    .filter { "${it.projectId}-${it.attachmentId}" !in existingCrossRefKeys }
                    .map { it.copy(syncedAt = ts, updatedAt = maxOf(it.updatedAt ?: 0L, ts)) }
                if (missingCrossRefs.isNotEmpty()) {
                    Log.w(WIFI_SYNC_LOG_TAG, "[applyServerChanges] DEFECT #4 CROSSREF SAFETY: Inserting ${missingCrossRefs.size} missing crossRefs that were filtered out by merge")
                    attachmentDao.insertProjectAttachmentLinks(missingCrossRefs)
                }
            }
            Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Applied at $ts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes", e)
            Result.failure(e)
        }
    }

    private inline fun <T> mergeAndMark(
        incoming: List<T>,
        localMap: Map<String, T>,
        crossinline idSelector: (T) -> String,
        crossinline versionSelector: (T) -> Long,
        crossinline updatedSelector: (T) -> Long,
        crossinline markSynced: (T, Long) -> T,
        syncedAt: Long,
        crossinline isDeletedSelector: (T) -> Boolean = { false },
        noinline logConsumer: ((T, T) -> Unit)? = null,
    ): List<T> {
        // Deduplicate incoming by id with LWW priority: version → updatedAt → tombstone flag
        val bestIncomingById = incoming
            .groupBy { idSelector(it) }
            .mapValues { entry ->
                entry.value.maxWithOrNull(
                    compareBy<T> { versionSelector(it) }
                        .thenBy { updatedSelector(it) }
                        .thenBy { if (isDeletedSelector(it)) 1 else 0 },
                )!!
            }

        return bestIncomingById.values.mapNotNull { inc ->
            val id = idSelector(inc)
            val local = localMap[id]
            if (local != null) logConsumer?.invoke(local, inc)

            val incVersion = versionSelector(inc)
            val localVersion = local?.let { versionSelector(it) } ?: Long.MIN_VALUE
            val incUpdated = updatedSelector(inc)
            val localUpdated = local?.let { updatedSelector(it) } ?: Long.MIN_VALUE

            // LWW: higher version, then newer updatedAt, then remote wins on tie
            val shouldTakeIncoming = when {
                local == null -> true
                incVersion > localVersion -> true
                incVersion < localVersion -> false
                incUpdated > localUpdated -> true
                incUpdated < localUpdated -> false
                else -> true
            }

            if (shouldTakeIncoming) markSynced(inc, syncedAt) else null
        }
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
        // Keep incoming view mode as-is, do not force BACKLOG on sync
        defaultViewModeName = project.defaultViewModeName,
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

private fun mergeSystemProjects(
    localSystem: List<Project>,
    incomingSystem: List<Project>,
    syncedAt: Long
): List<Project> {
    val localMap = localSystem.associateBy { it.systemKey!! }
    val incomingMap = incomingSystem.associateBy { it.systemKey!! }

    val allKeys = localMap.keys + incomingMap.keys

    return allKeys.mapNotNull { key ->
        val local = localMap[key]
        val incoming = incomingMap[key]

        when {
            // New system project from incoming, should ideally not happen if prepopulate is correct
            local == null && incoming != null -> incoming.copy(syncedAt = syncedAt)
            // Local system project exists, but not in incoming. Keep local.
            local != null && incoming == null -> null
            // Both exist, merge them
            local != null && incoming != null -> {
                val localVer = local.version
                val incomingVer = incoming.version
                val localUpdated = local.updatedAt ?: local.createdAt
                val incomingUpdated = incoming.updatedAt ?: incoming.createdAt

                // LWW logic
                val winner = if (incomingVer > localVer || (incomingVer == localVer && incomingUpdated > localUpdated)) {
                    incoming
                } else {
                    local
                }
                // Preserve local ID, take data from winner, set syncedAt
                winner.copy(id = local.id, syncedAt = syncedAt)
            }
            else -> null // Should not happen
        }
    }
}

    private suspend fun writeDebugDump(kind: String, payload: String) {
        withContext(Dispatchers.IO) {
            try {
                val dir = debugDumpDir
                if (dir == null) {
                    Log.w(WIFI_SYNC_LOG_TAG, "[debugDump] Dump dir is null, skip")
                    return@withContext
                }
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    Log.d(WIFI_SYNC_LOG_TAG, "[debugDump] mkdirs ${dir.absolutePath} created=$created")
                }
                val stamp = dumpDateFormat.format(Date())
                val file = File(dir, "$kind---$stamp.json")
                file.writeText(payload)
                trimDebugDumpsIfNeeded()
                Log.d(WIFI_SYNC_LOG_TAG, "[debugDump] wrote ${file.name} bytes=${payload.length} at ${file.absolutePath}")
            } catch (e: Exception) {
                Log.w(WIFI_SYNC_LOG_TAG, "[debugDump] Failed to write dump", e)
            }
        }
    }

    private fun trimDebugDumpsIfNeeded() {
        val dir = debugDumpDir ?: run {
            Log.w(WIFI_SYNC_LOG_TAG, "[debugDump] trim skipped: dir is null")
            return
        }
        val files = dir.listFiles()?.filter { it.extension == "json" }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxDumpBytes) return
        for (f in files) {
            if (total <= maxDumpBytes) break
            val len = f.length()
            if (f.delete()) {
                total -= len
            }
        }
    }

}
