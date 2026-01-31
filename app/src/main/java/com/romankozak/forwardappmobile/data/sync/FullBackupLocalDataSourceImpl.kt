package com.romankozak.forwardappmobile.data.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toSnapshot
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
    private val checklistDao: ChecklistDao,
    private val attachmentDao: AttachmentDao,
    private val recentItemDao: RecentItemDao,
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val chatDao: ChatDao,
    private val reminderDao: ReminderDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiInsightDao: AiInsightDao,
    private val systemContextEnsurer: SystemContextEnsurer
) : FullBackupLocalDataSource {

    override suspend fun loadFullDatabaseContent(): DatabaseContent {
        // Step 1: Load primary entities and create sets of their valid IDs
        val allProjects = contextDao.getAll()
        val allGoals = goalDao.getAll()
        val allDocuments = noteDocumentDao.getAllDocuments()
        val allAttachments = attachmentDao.getAll()
        
        val validProjectIds = allProjects.map { it.id }.toSet()
        val validGoalIds = allGoals.map { it.id }.toSet()
        val validDocumentIds = allDocuments.map { it.id }.toSet()
        val validAttachmentIds = allAttachments.map { it.id }.toSet()

        // Step 2: Load and filter dependent entities
        val allBacklogItems = listItemDao.getAll()
        val validBacklogItems = allBacklogItems.filter {
            validProjectIds.contains(it.contextId) &&
            when(it.itemType) {
                "GOAL" -> it.entityId != null && validGoalIds.contains(it.entityId)
                "SUBLIST" -> it.entityId != null && validProjectIds.contains(it.entityId)
                "NOTE_DOCUMENT" -> it.entityId != null && validDocumentIds.contains(it.entityId)
                 else -> true // Allow other types that don't have FK constraints to entity tables
            }
        }
        
        val allCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs()
        val validCrossRefs = allCrossRefs.filter {
            validProjectIds.contains(it.contextId) && validAttachmentIds.contains(it.attachmentId)
        }

        // Step 3: Return a DatabaseContent object with only the valid, filtered data
        return DatabaseContent(
            projects = allProjects,
            goals = allGoals,
            documents = allDocuments,
            attachments = allAttachments,
            backlogItems = validBacklogItems,
            contextAttachmentCrossRefs = validCrossRefs,
            // Keep other entities as they are, assuming they have fewer complex dependencies
            // or that their dependencies are handled by Room's CASCADE deletes.
            // This can be expanded if other orphaned data is found.
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
                if (!isValid) {
                     android.util.Log.w("FullBackupImport", "DIAGNOSTIC: NoteDocument ignored: id=${it.id}, name=${it.name}, contextId=${it.contextId} (context exists: ${validContextIds.contains(it.contextId)})")
                }
                isValid
            }
            val validDocumentIds = validDocuments.map { it.id }.toSet()
            if (content.documents.size > validDocuments.size) android.util.Log.w("FullBackupImport", "NoteDocuments: Ignored ${content.documents.size - validDocuments.size} of ${content.documents.size}.")
            noteDocumentDao.insertAllDocuments(validDocuments)

            val validChecklists = content.checklists.filter {
                !it.id.isNullOrBlank() && !it.name.isNullOrBlank() && validContextIds.contains(it.contextId)
            }
            val validChecklistIds = validChecklists.map { it.id }.toSet()
            checklistDao.insertChecklists(validChecklists)


            // Step 2: Filter dependent entities against the sets of valid parent IDs.

            val validBacklogItems = content.backlogItems.filter {
                val entityId = it.entityId
                val itemType = it.itemType
                val contextId = it.contextId
                val id = it.id

                val contextExists = validContextIds.contains(contextId)
                val entityIdValidForType = when (itemType) {
                    "GOAL" -> entityId != null && validGoalIds.contains(entityId)
                    "SUBLIST" -> entityId != null && validChecklistIds.contains(entityId)
                    "NOTE_DOCUMENT" -> entityId != null && validDocumentIds.contains(entityId)
                    else -> false
                }
                
                val isValid = !id.isNullOrBlank() &&
                        !contextId.isNullOrBlank() &&
                        contextExists &&
                        entityIdValidForType
                if (!isValid) {
                    android.util.Log.w("FullBackupImport", "DIAGNOSTIC: BacklogItem ignored: id=${id}, itemType=${itemType}, contextId=${contextId} (context exists: $contextExists), entityId=${entityId} (entity valid: $entityIdValidForType)")
                }
                isValid
            }
            if (content.backlogItems.size > validBacklogItems.size) android.util.Log.w("FullBackupImport", "BacklogItems: Ignored ${content.backlogItems.size - validBacklogItems.size} of ${content.backlogItems.size}.")
            listItemDao.insertItems(validBacklogItems)

            val validChecklistItems = content.checklistItems.filter {
                !it.id.isNullOrBlank() && validChecklistIds.contains(it.checklistId)
            }
            checklistDao.insertItems(validChecklistItems)


            val validCrossRefs = content.contextAttachmentCrossRefs.filter {
                val contextExists = validContextIds.contains(it.contextId)
                val attachmentExists = validAttachmentIds.contains(it.attachmentId)
                val isValid = !it.contextId.isNullOrBlank() && !it.attachmentId.isNullOrBlank() && contextExists && attachmentExists
                if (!isValid) {
                     android.util.Log.w("FullBackupImport", "DIAGNOSTIC: CrossRef ignored: contextId=${it.contextId} (context exists: $contextExists), attachmentId=${it.attachmentId} (attachment exists: $attachmentExists)")
                }
                isValid
            }

            systemContextEnsurer.ensureAllSystemContextsExist()

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

    // === New Snapshot-based Methods Implementation ===

    override suspend fun loadFullSnapshotBundle(): com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle {
        return com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            contexts = contextDao.getAllRaw().map { it.toSnapshot() },
            goals = db.goalDao().getAllRaw().map { it.toSnapshot() },
            backlogItems = db.listItemDao().getAllRaw().map { it.toSnapshot() },
            backlogOrders = db.backlogOrderDao().getAllRaw().map { it.toSnapshot() },
            notes = db.legacyNoteDao().getAllRaw().map { it.toSnapshot() },
            documents = db.noteDocumentDao().getAllDocumentsRaw().map { it.toSnapshot() },
            documentItems = db.noteDocumentDao().getAllDocumentItemsRaw().map { it.toSnapshot() },
            checklists = db.checklistDao().getAllChecklistsRaw().map { it.toSnapshot() },
            checklistItems = db.checklistDao().getAllChecklistItemsRaw().map { it.toSnapshot() },
            artifacts = db.contextArtifactDao().getAllRaw().map { it.toSnapshot() },
            scripts = db.scriptDao().getAllRaw().map { it.toSnapshot() },
            attachments = db.attachmentDao().getAllRaw().map { it.toSnapshot() },
            crossRefs = db.attachmentDao().getAllContextAttachmentCrossRefsRaw().map { it.toSnapshot() },
            inbox = db.inboxRecordDao().getAllRaw().map { it.toSnapshot() },
            logs = db.contextManagementDao().getAllLogsRaw().map { it.toSnapshot() },
            systemApps = db.systemAppDao().getAllRaw().map { it.toSnapshot() },
            activityRecords = db.activityRecordDao().getAllRaw().map { it.toSnapshot() },
            recentProjectEntries = db.recentItemDao().getAllSync().map { it.toSnapshot() },
            linkItemEntities = db.linkItemDao().getAllRaw().map { it.toSnapshot() },
            dayPlans = db.dayPlanDao().getAllPlansSync().map { it.toSnapshot() },
            dayTasks = db.dayTaskDao().getAllTasksSync().map { it.toSnapshot() },
            dailyMetrics = db.dailyMetricDao().getAllMetricsSync().map { it.toSnapshot() },
            conversations = db.chatDao().getAllConversationsSync().map { it.toSnapshot() },
            chatMessages = db.chatDao().getAllMessagesSync().map { it.toSnapshot() },
            conversationFolders = db.conversationFolderDao().getAllSync().map { it.toSnapshot() },
            reminders = db.reminderDao().getAllRemindersSync().map { it.toSnapshot() },
            recurringTasks = db.recurringTaskDao().getAllSync().map { it.toSnapshot() },
            tacticalMissions = db.tacticalMissionDao().getAllMissionsSync().map { it.toSnapshot() },
            tacticalMissionAttachments = db.tacticalMissionDao().getAllMissionAttachmentsSync().map { it.toSnapshot() },
            aiEvents = db.aiEventDao().getAllSync().map { it.toSnapshot() },
            aiInsights = db.aiInsightDao().getAllSync().map { it.toSnapshot() },
            lifeSystemStates = db.lifeSystemStateDao().getAllSync().map { it.toSnapshot() },
            contextRoleProfiles = db.structurePresetDao().getAllSync().map { it.toSnapshot() },
            contextRoleProfileItems = db.structurePresetItemDao().getAllSync().map { it.toSnapshot() },
            contextConfigurations = db.contextStructureDao().getAllSync().map { it.toSnapshot() },
            projectStructureItems = db.contextStructureDao().getAllItemsSync().map { it.toSnapshot() }
        )
    }

    override suspend fun applySnapshotBundle(bundle: com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle) {
        db.withTransaction {
            // This is a non-destructive operation.
            // The actual merge logic (insert vs update) will be handled by the DAOs' insert methods with OnConflictStrategy.REPLACE
            db.contextDao().insertAll(bundle.contexts.map { it.toEntity() })
            db.goalDao().insertAll(bundle.goals.map { it.toEntity() })
            db.noteDocumentDao().insertAllDocuments(bundle.documents.map { it.toEntity() })
            db.checklistDao().insertChecklists(bundle.checklists.map { it.toEntity() })
            db.conversationFolderDao().insertAll(bundle.conversationFolders.map { it.toEntity() })
            db.dayPlanDao().insertPlans(bundle.dayPlans.map { it.toEntity() })
            db.recurringTaskDao().insertAll(bundle.recurringTasks.map { it.toEntity() })
            db.tacticalMissionDao().insertMissions(bundle.tacticalMissions.map { it.toEntity() })

            db.listItemDao().insertItems(bundle.backlogItems.map { it.toEntity() })
            db.backlogOrderDao().insertAll(bundle.backlogOrders.map { it.toEntity() })
            db.legacyNoteDao().insertAll(bundle.notes.map { it.toEntity() })
            db.noteDocumentDao().insertAllItems(bundle.documentItems.map { it.toEntity() })
            db.checklistDao().insertItems(bundle.checklistItems.map { it.toEntity() })
            db.contextArtifactDao().insertAll(bundle.artifacts.map { it.toEntity() })
            db.scriptDao().insertAll(bundle.scripts.map { it.toEntity() })
            db.attachmentDao().insertAttachments(bundle.attachments.map { it.toEntity() })
            db.attachmentDao().insertContextAttachmentCrossRefs(bundle.crossRefs.map { it.toEntity() })
            db.inboxRecordDao().insertAll(bundle.inbox.map { it.toEntity() })
            db.contextManagementDao().insertLogs(bundle.logs.map { it.toEntity() })
            db.systemAppDao().insertAll(bundle.systemApps.map { it.toEntity() })
            db.activityRecordDao().insertAll(bundle.activityRecords.map { it.toEntity() })
            db.recentItemDao().insertAllSync(bundle.recentProjectEntries.map { it.toEntity() })
            db.linkItemDao().insertAll(bundle.linkItemEntities.map { it.toEntity() })
            db.dayTaskDao().insertTasks(bundle.dayTasks.map { it.toEntity() })
            db.dailyMetricDao().insertMetrics(bundle.dailyMetrics.map { it.toEntity() })
            db.chatDao().insertConversations(bundle.conversations.map { it.toEntity() })
            db.chatDao().insertMessages(bundle.chatMessages.map { it.toEntity() })
            db.reminderDao().insertAll(bundle.reminders.map { it.toEntity() })
            db.tacticalMissionDao().insertMissionAttachments(bundle.tacticalMissionAttachments.map { it.toEntity() })
            db.aiEventDao().insertAll(bundle.aiEvents.map { it.toEntity() })
            db.aiInsightDao().upsertAll(bundle.aiInsights.map { it.toEntity() })
            db.lifeSystemStateDao().insertAll(bundle.lifeSystemStates.map { it.toEntity() })
            db.structurePresetDao().insertAll(bundle.contextRoleProfiles.map { it.toEntity() })
            db.structurePresetItemDao().insertAll(bundle.contextRoleProfileItems.map { it.toEntity() })
            db.contextStructureDao().insertAll(bundle.contextConfigurations.map { it.toEntity() })
            db.contextStructureDao().insertAllItems(bundle.projectStructureItems.map { it.toEntity() })
        }
        systemContextEnsurer.ensureAllSystemContextsExist()
    }
}
