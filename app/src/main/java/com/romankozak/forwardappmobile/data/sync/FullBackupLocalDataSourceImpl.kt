package com.romankozak.forwardappmobile.data.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullBackupLocalDataSourceImpl @Inject constructor(
    private val db: AppDatabase,
    val settingsRepository: SettingsRepository,
    private val contextDao: ContextDao,
    private val goalDao: GoalDao,
    private val listItemDao: ListItemDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val attachmentDao: AttachmentDao,
    private val recentItemDao: RecentItemDao,
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val chatDao: ChatDao,
    private val reminderDao: ReminderDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiInsightDao: AiInsightDao
) : FullBackupLocalDataSource {

    override suspend fun loadFullDatabaseContent(): DatabaseContent {
        return DatabaseContent(
            // Використовуємо методи getAll(), які повертають List замість Flow
            projects = contextDao.getAll(), //
            goals = goalDao.getAll(), //
            backlogItems = listItemDao.getAll(), //
            documents = noteDocumentDao.getAllDocuments(),
            attachments = attachmentDao.getAll(),
            contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
            recentProjectEntries = recentItemDao.getAll().map {
                RecentProjectEntry(contextId = it.target, timestamp = it.lastAccessed)
            },
            dayPlans = dayPlanDao.getAllPlansSync(),
            dayTasks = dayTaskDao.getAllTasksSync(),
            dailyMetrics = dailyMetricDao.getAllMetricsSync(),
            conversations = chatDao.getAllConversationsSync(),
            chatMessages = chatDao.getAllMessagesSync(),
            reminders = reminderDao.getAllRemindersSync(),
            tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
            aiInsights = aiInsightDao.getAllSync()
        )
    }

    override suspend fun restoreDatabaseFromBackup(content: DatabaseContent) {
        db.withTransaction {
            android.util.Log.d("FullBackupImport", "--- STARTING DATABASE RESTORE ---")
            clearAllTables()

            // Step 1: Filter and insert independent "parent" entities and collect their valid IDs.
            val validGoals = content.goals.filter { !it.id.isNullOrBlank() && !it.text.isNullOrBlank() }
            val validGoalIds = validGoals.map { it.id }.toSet()
            if (content.goals.size > validGoals.size) android.util.Log.w("FullBackupImport", "Goals: Ignored ${content.goals.size - validGoals.size} of ${content.goals.size}.")
            goalDao.insertGoals(validGoals)

            val validContexts = content.projects.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
            val validContextIds = validContexts.map { it.id }.toSet()
            if (content.projects.size > validContexts.size) android.util.Log.w("FullBackupImport", "Contexts: Ignored ${content.projects.size - validContexts.size} of ${content.projects.size}.")
            contextDao.insertContexts(validContexts)
            android.util.Log.d("FullBackupImport", "DIAGNOSTIC: First 5 valid context IDs: [${validContextIds.take(5).joinToString()}]")


            val validAttachments = content.attachments.filter { !it.id.isNullOrBlank() && !it.attachmentType.isNullOrBlank() && !it.entityId.isNullOrBlank() }
            val validAttachmentIds = validAttachments.map { it.id }.toSet()
            if (content.attachments.size > validAttachments.size) android.util.Log.w("FullBackupImport", "Attachments: Ignored ${content.attachments.size - validAttachments.size} of ${content.attachments.size}.")
            attachmentDao.insertAttachments(validAttachments)
            
            val validDocuments = content.documents.filter {
                val isValid = !it.id.isNullOrBlank() && !it.name.isNullOrBlank() && validContextIds.contains(it.contextId)
                if (!isValid && !it.id.isNullOrBlank()) {
                     android.util.Log.w("FullBackupImport", "DIAGNOSTIC: NoteDocument with id ${it.id} is being ignored due to unknown contextId: ${it.contextId}")
                }
                isValid
            }
            val validDocumentIds = validDocuments.map { it.id }.toSet()
            if (content.documents.size > validDocuments.size) android.util.Log.w("FullBackupImport", "NoteDocuments: Ignored ${content.documents.size - validDocuments.size} of ${content.documents.size}.")
            noteDocumentDao.insertAllDocuments(validDocuments)

            // Step 2: Filter dependent entities against the sets of valid parent IDs.

            val validBacklogItems = content.backlogItems.filter {
                val entityId = it.entityId
                val itemType = it.itemType
                val contextId = it.contextId
                val id = it.id

                val isValid = !id.isNullOrBlank() &&
                        !contextId.isNullOrBlank() &&
                        validContextIds.contains(contextId) &&
                        entityId != null &&
                        itemType != null &&
                        when (itemType) {
                            "GOAL" -> validGoalIds.contains(entityId)
                            "SUBLIST" -> validContextIds.contains(entityId)
                            "NOTE_DOCUMENT" -> validDocumentIds.contains(entityId)
                            else -> false
                        }
                if (!isValid) {
                    android.util.Log.w("FullBackupImport", "DIAGNOSTIC: BacklogItem ignored: id=${id}, itemType=${itemType}, contextId=${contextId}, entityId=${entityId}")
                }
                isValid
            }
            if (content.backlogItems.size > validBacklogItems.size) android.util.Log.w("FullBackupImport", "BacklogItems: Ignored ${content.backlogItems.size - validBacklogItems.size} of ${content.backlogItems.size}.")
            listItemDao.insertItems(validBacklogItems)


            val validCrossRefs = content.contextAttachmentCrossRefs.filter {
                val contextOk = validContextIds.contains(it.contextId)
                val attachmentOk = validAttachmentIds.contains(it.attachmentId)
                val isValid = !it.contextId.isNullOrBlank() && !it.attachmentId.isNullOrBlank() && contextOk && attachmentOk
                if (!isValid) {
                     android.util.Log.w("FullBackupImport", "DIAGNOSTIC: CrossRef ignored: contextId=${it.contextId} (exists: $contextOk), attachmentId=${it.attachmentId} (exists: $attachmentOk)")
                }
                isValid
            }
            if (content.contextAttachmentCrossRefs.size > validCrossRefs.size) android.util.Log.w("FullBackupImport", "CrossRefs: Ignored ${content.contextAttachmentCrossRefs.size - validCrossRefs.size} of ${content.contextAttachmentCrossRefs.size}.")
            attachmentDao.insertContextAttachmentLinks(validCrossRefs)

            android.util.Log.d("FullBackupImport", "--- DATABASE RESTORE FINISHED ---")
        }
    }

    override suspend fun clearAllTables() {
        db.clearAllTables()
    }

    override suspend fun getSettingsSnapshot(): Map<String, String> {
        return settingsRepository.getPreferencesSnapshot().asMap()
            .mapKeys { it.key.name }
            .mapValues { it.value.toString() }
    }

    override suspend fun restoreSettings(settings: Map<String, String>) {
        settingsRepository.restoreFromMap(settings)
    }
}