package com.romankozak.forwardappmobile.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
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
    private val systemContextEnsurer: SystemContextEnsurer,
    // Newly Injected DAOs
    private val legacyNoteDao: LegacyNoteDao,
    private val backlogOrderDao: BacklogOrderDao,
    private val contextArtifactDao: ContextArtifactDao,
    private val scriptDao: ScriptDao,
    private val inboxRecordDao: InboxRecordDao,
    private val contextManagementDao: ContextManagementDao,
    private val systemAppDao: SystemAppDao,
    private val activityRecordDao: ActivityRecordDao,
    private val linkItemDao: LinkItemDao,
    private val conversationFolderDao: ConversationFolderDao,
    private val recurringTaskDao: RecurringTaskDao,
    private val aiEventDao: AiEventDao,
    private val lifeSystemStateDao: LifeSystemStateDao,
    private val structurePresetDao: StructurePresetDao,
    private val structurePresetItemDao: StructurePresetItemDao,
    private val contextStructureDao: ContextStructureDao
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

        val tacticalMissions = tacticalMissionDao.getAllMissionsSync()
        Log.d("FullBackupExport", "Tactical Missions: [${tacticalMissions.size}] records processed during [Export].")

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
            tacticalMissions = tacticalMissions,
            aiInsights = aiInsightDao.getAllSync()
        )
    }

    override suspend fun restoreDatabaseFromBackup(content: DatabaseContent) {
        db.withTransaction {
            Log.d("FullBackupImport", "--- STARTING DATABASE RESTORE ---")
            clearAllTables()

            // Step 1: Filter and insert independent "parent" entities and collect their valid IDs.
            val validGoals = content.goals.filter { !it.id.isNullOrBlank() && !it.text.isNullOrBlank() }
            val validGoalIds = validGoals.map { it.id }.toSet()
            if (content.goals.size > validGoals.size) Log.w("FullBackupImport", "Goals: Ignored ${content.goals.size - validGoals.size} of ${content.goals.size}.")
            goalDao.insertGoals(validGoals)

            val validContexts = content.projects.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
            val validContextIds = validContexts.map { it.id }.toSet()
            if (content.projects.size > validContexts.size) Log.w("FullBackupImport", "Contexts: Ignored ${content.projects.size - validContexts.size} of ${content.projects.size}.")
            contextDao.insertContexts(validContexts)
            Log.d("FullBackupImport", "DIAGNOSTIC: First 5 valid context IDs: [${validContextIds.take(5).joinToString()}]")

            // Documents
            Log.d("FullBackupImport", "Documents in backup: ${content.documents.size}")
            val validDocuments = content.documents.filter {
                val isValid = !it.id.isNullOrBlank() && !it.name.isNullOrBlank() && validContextIds.contains(it.contextId)
                if (!isValid) {
                     Log.w("FullBackupImport", "DIAGNOSTIC: NoteDocument ignored: id=${it.id}, name=${it.name}, contextId=${it.contextId} (context exists: ${validContextIds.contains(it.contextId)})")
                }
                isValid
            }
            val validDocumentIds = validDocuments.map { it.id }.toSet()
            Log.d("FullBackupImport", "Valid documents after filtering: ${validDocuments.size}")
            noteDocumentDao.insertAllDocuments(validDocuments)

            // Checklists
            Log.d("FullBackupImport", "Checklists in backup: ${content.checklists.size}")
            val validChecklists = content.checklists.filter {
                !it.id.isNullOrBlank() && !it.name.isNullOrBlank() && validContextIds.contains(it.contextId)
            }
            Log.d("FullBackupImport", "Valid checklists after filtering: ${validChecklists.size}")
            val validChecklistIds = validChecklists.map { it.id }.toSet()
            checklistDao.insertChecklists(validChecklists)

            // Activity Records
            Log.d("FullBackupImport", "Activity records in backup: ${content.activityRecords.size}")
            val validActivityRecords = content.activityRecords.filter {
                !it.id.isNullOrBlank() && !it.text.isNullOrBlank()
            }
            Log.d("FullBackupImport", "Valid activity records after filtering: ${validActivityRecords.size}")
            activityRecordDao.insertAll(validActivityRecords)
            Log.d("FullBackupImport", "Imported ${validActivityRecords.size} activity records from V1 backup.")

            // Inbox Records
            Log.d("FullBackupImport", "First inbox record contextId: ${content.inboxRecords.firstOrNull()?.contextId}")
            Log.d("FullBackupImport", "Inbox records in backup: ${content.inboxRecords.size}")
            val validInboxRecords = content.inboxRecords.filter {
                val isValid = !it.id.isNullOrBlank() && !it.text.isNullOrBlank() && validContextIds.contains(it.contextId)
                if (!isValid) {
                     Log.w("FullBackupImport", "DIAGNOSTIC: InboxRecord ignored/partially imported: id=${it.id}, text=${it.text}, contextId=${it.contextId} (context exists: ${validContextIds.contains(it.contextId)})")
                }
                isValid
            }
            Log.d("FullBackupImport", "Valid inbox records after filtering: ${validInboxRecords.size}")
            inboxRecordDao.insertAll(validInboxRecords)
            Log.d("FullBackupImport", "Legacy Import: ${validInboxRecords.size} inbox records processed.")

            // --- Consolidate and auto-link attachments and cross-refs ---

            val finalAttachments = content.attachments.filter { 
                !it.id.isNullOrBlank() && !it.attachmentType.isNullOrBlank() && !it.entityId.isNullOrBlank() 
            }.toMutableList()
            val finalCrossRefs = content.contextAttachmentCrossRefs.toMutableList()
            
            val existingAttachmentEntityIds = finalAttachments.mapNotNull { it.entityId }.toSet()

            validDocuments.forEach { doc ->
                if (doc.id !in existingAttachmentEntityIds) {
                    val attachment = AttachmentEntity(
                        entityId = doc.id,
                        attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                        ownerContextId = doc.contextId,
                        createdAt = doc.createdAt,
                        updatedAt = doc.updatedAt
                    )
                    finalAttachments.add(attachment)
                    val crossRef = ContextAttachmentCrossRef(
                        contextId = doc.contextId,
                        attachmentId = attachment.id
                    )
                    finalCrossRefs.add(crossRef)
                }
            }

            validChecklists.forEach { checklist ->
                 if (checklist.id !in existingAttachmentEntityIds) {
                    val attachment = AttachmentEntity(
                        entityId = checklist.id,
                        attachmentType = BacklogItemTypeValues.CHECKLIST,
                        ownerContextId = checklist.contextId,
                        createdAt = checklist.createdAt,
                        updatedAt = checklist.updatedAt
                    )
                    finalAttachments.add(attachment)
                    val crossRef = ContextAttachmentCrossRef(
                        contextId = checklist.contextId,
                        attachmentId = attachment.id
                    )
                    finalCrossRefs.add(crossRef)
                }
            }

            Log.d("FullBackupImport", "Total attachments to insert: ${finalAttachments.size}")
            attachmentDao.insertAttachments(finalAttachments)
            
            val validAttachmentIds = finalAttachments.map { it.id }.toSet()
            val validFinalCrossRefs = finalCrossRefs.filter { it.contextId in validContextIds && it.attachmentId in validAttachmentIds }
            Log.d("FullBackupImport", "Total cross-refs to insert: ${validFinalCrossRefs.size}")
            attachmentDao.insertContextAttachmentCrossRefs(validFinalCrossRefs)


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
                    Log.w("FullBackupImport", "DIAGNOSTIC: BacklogItem ignored: id=${id}, itemType=${itemType}, contextId=${contextId} (context exists: $contextExists), entityId=${entityId} (entity valid: $entityIdValidForType)")
                }
                isValid
            }
            if (content.backlogItems.size > validBacklogItems.size) Log.w("FullBackupImport", "BacklogItems: Ignored ${content.backlogItems.size - validBacklogItems.size} of ${content.backlogItems.size}.")
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
                     Log.w("FullBackupImport", "DIAGNOSTIC: CrossRef ignored: contextId=${it.contextId} (context exists: $contextExists), attachmentId=${it.attachmentId} (attachment exists: $attachmentExists)")
                }
                isValid
            }

            // Tactical Missions
            val missions = content.tacticalMissions
            if (missions.isNotEmpty()) {
                tacticalMissionDao.insertMissions(missions)
                Log.d("FullBackupImport", "Tactical Missions: ${missions.size} records processed during Import.")
            }
            val missionAttachments = content.tacticalMissionAttachments
            if (missionAttachments.isNotEmpty()) {
                tacticalMissionDao.insertMissionAttachments(missionAttachments)
            }

            systemContextEnsurer.ensureAllSystemContextsExist()

            Log.d("FullBackupImport", "--- DATABASE RESTORE FINISHED ---")
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

    override suspend fun loadFullSnapshotBundle(): SnapshotBundle {
        val notes = legacyNoteDao.getAllRaw().map { it.toSnapshot() }
        val documents = noteDocumentDao.getAllDocumentsRaw().map { it.toSnapshot() }
        val checklists = checklistDao.getAllChecklistsRaw().map { it.toSnapshot() }
        val scripts = scriptDao.getAllRaw().map { it.toSnapshot() }
        val activityRecords = activityRecordDao.getAllRaw().map { it.toSnapshot() }
        val inbox = inboxRecordDao.getAllRaw().map { it.toSnapshot() }

        Log.d("BackupExport", "Exporting Snapshot: notes=${notes.size}, docs=${documents.size}, checklists=${checklists.size}, scripts=${scripts.size}, activityRecords=${activityRecords.size}, inbox=${inbox.size}")

        val tacticalMissions = tacticalMissionDao.getAllMissionsSync().map { it.toSnapshot() }
        Log.d("BackupExport", "Tactical Missions: [${tacticalMissions.size}] records processed during [Export].")

        return SnapshotBundle(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            contexts = contextDao.getAllRaw().map { it.toSnapshot() },
            goals = goalDao.getAllRaw().map { it.toSnapshot() },
            backlogItems = listItemDao.getAllRaw().map { it.toSnapshot() },
            backlogOrders = backlogOrderDao.getAllRaw().map { it.toSnapshot() },
            notes = notes,
            documents = documents,
            checklists = checklists,
            checklistItems = checklistDao.getAllChecklistItemsRaw().map { it.toSnapshot() },
            artifacts = contextArtifactDao.getAllRaw().map { it.toSnapshot() },
            scripts = scripts,
            attachments = attachmentDao.getAllRaw().map { it.toSnapshot() },
            crossRefs = attachmentDao.getAllContextAttachmentCrossRefsRaw().map { it.toSnapshot() },
            inbox = inbox,
            logs = contextManagementDao.getAllLogsRaw().map { it.toSnapshot() },
            systemApps = systemAppDao.getAllRaw().map { it.toSnapshot() },
            activityRecords = activityRecords,
            recentProjectEntries = recentItemDao.getAllSync().map { it.toSnapshot() },
            linkItemEntities = linkItemDao.getAllRaw().map { it.toSnapshot() },
            dayPlans = dayPlanDao.getAllPlansSync().map { it.toSnapshot() },
            dayTasks = dayTaskDao.getAllTasksSync().map { it.toSnapshot() },
            dailyMetrics = dailyMetricDao.getAllMetricsSync().map { it.toSnapshot() },
            conversations = chatDao.getAllConversationsSync().map { it.toSnapshot() },
            chatMessages = chatDao.getAllMessagesSync().map { it.toSnapshot() },
            conversationFolders = conversationFolderDao.getAllSync().map { it.toSnapshot() },
            reminders = reminderDao.getAllRemindersSync().map { it.toSnapshot() },
            recurringTasks = recurringTaskDao.getAllSync().map { it.toSnapshot() },
            tacticalMissions = tacticalMissions,
            tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentsSync().map { it.toSnapshot() },
            aiEvents = aiEventDao.getAllSync().map { it.toSnapshot() },
            aiInsights = aiInsightDao.getAllSync().map { it.toSnapshot() },
            lifeSystemStates = lifeSystemStateDao.getAllSync().map { it.toSnapshot() },
            contextRoleProfiles = structurePresetDao.getAllSync().map { it.toSnapshot() },
            contextRoleProfileItems = structurePresetItemDao.getAllSync().map { it.toSnapshot() },
            contextConfigurations = contextStructureDao.getAllSync().map { it.toSnapshot() },
            projectStructureItems = contextStructureDao.getAllItemsSync().map { it.toSnapshot() }
        )
    }

    override suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
        Log.d("BackupImport", "Applying Snapshot V${bundle.version}: notes=${bundle.notes.size}, docs=${bundle.documents.size}, checklists=${bundle.checklists.size}, scripts=${bundle.scripts.size}")

        db.withTransaction {
            // This is a non-destructive operation.
            // The actual merge logic (insert vs update) will be handled by the DAOs' insert methods with OnConflictStrategy.REPLACE
            contextDao.insertAll(bundle.contexts.map { it.toEntity() })
            goalDao.insertAll(bundle.goals.map { it.toEntity() })
            noteDocumentDao.insertAllDocuments(bundle.documents.map { it.toEntity() })
            checklistDao.insertChecklists(bundle.checklists.map { it.toEntity() })

            val newAttachments = mutableListOf<AttachmentEntity>()
            val newCrossRefs = mutableListOf<ContextAttachmentCrossRef>()

            bundle.documents.forEach { doc ->
                doc.contextId?.let { contextId ->
                    val attachment = AttachmentEntity(
                        entityId = doc.id,
                        attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                        ownerContextId = contextId,
                        createdAt = doc.createdAt,
                        updatedAt = doc.updatedAt
                    )
                    newAttachments.add(attachment)
                    val crossRef = ContextAttachmentCrossRef(
                        contextId = contextId,
                        attachmentId = attachment.id
                    )
                    newCrossRefs.add(crossRef)
                    Log.d("BackupImport", "Linking Attachment [${attachment.id}] to Context [${crossRef.contextId}] for NoteDocument [${doc.id}]")
                }
            }

            bundle.checklists.forEach { checklist ->
                checklist.contextId?.let { contextId ->
                    val attachment = AttachmentEntity(
                        entityId = checklist.id,
                        attachmentType = BacklogItemTypeValues.CHECKLIST,
                        ownerContextId = contextId,
                        createdAt = checklist.createdAt,
                        updatedAt = checklist.updatedAt
                    )
                    newAttachments.add(attachment)
                    val crossRef = ContextAttachmentCrossRef(
                        contextId = contextId,
                        attachmentId = attachment.id
                    )
                    newCrossRefs.add(crossRef)
                    Log.d("BackupImport", "Linking Attachment [${attachment.id}] to Context [${crossRef.contextId}] for Checklist [${checklist.id}]")
                }
            }

            Log.d("BackupImport", "Creating ${newAttachments.size} new AttachmentEntities.")
            attachmentDao.insertAttachments(newAttachments)
            Log.d("BackupImport", "Creating ${newCrossRefs.size} new ContextAttachmentCrossRefs.")
            attachmentDao.insertContextAttachmentCrossRefs(newCrossRefs)
            
            conversationFolderDao.insertAll(bundle.conversationFolders.map { it.toEntity() })
            dayPlanDao.insertPlans(bundle.dayPlans.map { it.toEntity() })
            recurringTaskDao.insertAll(bundle.recurringTasks.map { it.toEntity() })
            tacticalMissionDao.insertMissions(bundle.tacticalMissions.map { it.toEntity() })

            listItemDao.insertItems(bundle.backlogItems.map { it.toEntity() })
            backlogOrderDao.insertAll(bundle.backlogOrders.map { it.toEntity() })
            legacyNoteDao.insertAll(bundle.notes.map { it.toEntity() })
            checklistDao.insertItems(bundle.checklistItems.map { it.toEntity() })
            contextArtifactDao.insertAll(bundle.artifacts.map { it.toEntity() })
            scriptDao.insertAll(bundle.scripts.map { it.toEntity() })
            attachmentDao.insertAttachments(bundle.attachments.map { it.toEntity() })
            attachmentDao.insertContextAttachmentCrossRefs(bundle.crossRefs.map { it.toEntity() })
            inboxRecordDao.insertAll(bundle.inbox.map { it.toEntity() })
            contextManagementDao.insertLogs(bundle.logs.map { it.toEntity() })
            systemAppDao.insertAll(bundle.systemApps.map { it.toEntity() })
            activityRecordDao.insertAll(bundle.activityRecords.map { it.toEntity() })
            recentItemDao.insertAllSync(bundle.recentProjectEntries.map { it.toEntity() })
            linkItemDao.insertAll(bundle.linkItemEntities.map { it.toEntity() })
            dayTaskDao.insertTasks(bundle.dayTasks.map { it.toEntity() })
            dailyMetricDao.insertMetrics(bundle.dailyMetrics.map { it.toEntity() })
            chatDao.insertConversations(bundle.conversations.map { it.toEntity() })
            chatDao.insertMessages(bundle.chatMessages.map { it.toEntity() })
            reminderDao.insertAll(bundle.reminders.map { it.toEntity() })
            tacticalMissionDao.insertMissionAttachments(bundle.tacticalMissionAttachments.map { it.toEntity() })
            aiEventDao.insertAll(bundle.aiEvents.map { it.toEntity() })
            aiInsightDao.upsertAll(bundle.aiInsights.map { it.toEntity() })
            lifeSystemStateDao.insertAll(bundle.lifeSystemStates.map { it.toEntity() })
            structurePresetDao.insertAll(bundle.contextRoleProfiles.map { it.toEntity() })
            structurePresetItemDao.insertAll(bundle.contextRoleProfileItems.map { it.toEntity() })
            contextStructureDao.insertAll(bundle.contextConfigurations.map { it.toEntity() })
            contextStructureDao.insertAllItems(bundle.projectStructureItems.map { it.toEntity() })
        }
        systemContextEnsurer.ensureAllSystemContextsExist()
    }
}
