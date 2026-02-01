package com.romankozak.forwardappmobile.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.*
import com.romankozak.forwardappmobile.core.data.models.sync.ChangeType
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toEntity
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeLocalDataSourceImpl @Inject constructor(
    private val db: AppDatabase,
    private val contextDao: ContextDao,
    private val goalDao: GoalDao,
    private val listItemDao: ListItemDao,
    private val attachmentDao: AttachmentDao,
    private val noteDocumentDao: NoteDocumentDao,
    private val chatDao: ChatDao,
    private val dayPlanDao: DayPlanDao,
    private val dayTaskDao: DayTaskDao,
    private val dailyMetricDao: DailyMetricDao,
    private val reminderDao: ReminderDao,
    private val tacticalMissionDao: TacticalMissionDao,
    private val aiInsightDao: AiInsightDao,
    private val checklistDao: ChecklistDao,
    private val conversationFolderDao: ConversationFolderDao,
    private val recurringTaskDao: RecurringTaskDao,
    private val backlogOrderDao: BacklogOrderDao,
    private val legacyNoteDao: LegacyNoteDao,
    private val contextArtifactDao: ContextArtifactDao,
    private val scriptDao: ScriptDao,
    private val inboxRecordDao: InboxRecordDao,
    private val contextManagementDao: ContextManagementDao,
    private val systemAppDao: SystemAppDao,
    private val activityRecordDao: ActivityRecordDao,
    private val recentItemDao: RecentItemDao,
    private val linkItemDao: LinkItemDao,
    private val aiEventDao: AiEventDao,
    private val lifeSystemStateDao: LifeSystemStateDao,
    private val structurePresetDao: StructurePresetDao,
    private val structurePresetItemDao: StructurePresetItemDao,
    private val contextStructureDao: ContextStructureDao
) : MergeLocalDataSource {

    override suspend fun getContexts(): List<Context> = contextDao.getAll()

    override suspend fun getGoals(): List<Goal> = goalDao.getAll()

    override suspend fun getLocalDatabaseContent(): DatabaseContent {
        return DatabaseContent(
            projects = contextDao.getAll(),
            goals = goalDao.getAll(),
            backlogItems = listItemDao.getAll(),
            documents = noteDocumentDao.getAllDocuments(),
            attachments = attachmentDao.getAll(),
            contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
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

    override suspend fun insertContexts(contexts: List<Context>) = contextDao.insertContexts(contexts)

    override suspend fun insertGoals(goals: List<Goal>) = goalDao.insertGoals(goals)

    override suspend fun insertAttachments(attachments: List<AttachmentEntity>) =
        attachmentDao.insertAttachments(attachments)

    override suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>) =
        attachmentDao.insertContextAttachmentLinks(links)

    override suspend fun insertListItems(items: List<BacklogItem>) = listItemDao.insertItems(items)

    override suspend fun applyChanges(changes: List<SyncChange>) {
        db.withTransaction {
            changes.forEach { change ->
                when (change.type) {
                    ChangeType.Add, ChangeType.Update -> applyUpsert(change)
                    ChangeType.Delete -> applyDelete(change)
                    ChangeType.Move -> {
                        // Логіка переміщення, якщо вона буде потрібна в майбутньому
                        Log.d("MergeDataSource", "Move operation not implemented for ${change.id}")
                    }
                }
            }
        }
    }

    private suspend fun applyUpsert(change: SyncChange) {
        // У SyncChange.entity тип Any, він не може бути null, тому прибираємо Elvis оператор
        when (val entity = change.entity) {
            is Goal -> goalDao.insertGoal(entity)
            is Context -> contextDao.insert(entity)
            is AttachmentEntity -> attachmentDao.insertAttachment(entity)
            is BacklogItem -> listItemDao.insertItem(entity)
        }
    }

    private suspend fun applyDelete(change: SyncChange) {
        // Використовуємо правильні назви полів: entityType та id
        when (change.entityType) {
            "Ціль" -> goalDao.deleteGoalById(change.id)
            "Список" -> contextDao.delete(change.id)
            "Вкладення" -> attachmentDao.deleteAttachment(change.id)
        }
    }

    override suspend fun importSelectedData(
        projects: List<Context>,
        goals: List<Goal>,
        listItems: List<BacklogItem>,
        attachments: List<AttachmentEntity>,
        crossRefs: List<ContextAttachmentCrossRef>
    ) {
        db.withTransaction {
            if (projects.isNotEmpty()) contextDao.insertContexts(projects)
            if (goals.isNotEmpty()) goalDao.insertGoals(goals)
            if (listItems.isNotEmpty()) listItemDao.insertItems(listItems)
            if (attachments.isNotEmpty()) attachmentDao.insertAttachments(attachments)
            if (crossRefs.isNotEmpty()) attachmentDao.insertContextAttachmentLinks(crossRefs)
        }
    }

    override suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
        Log.d("BackupImport", "Applying Snapshot V${bundle.version} in MergeLocalDataSource: notes=${bundle.notes.size}, docs=${bundle.documents.size}, checklists=${bundle.checklists.size}, scripts=${bundle.scripts.size}")

        db.withTransaction {
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
            
            Log.d("BackupImport", "Creating ${newAttachments.size} new AttachmentEntities in MergeLocalDataSource.")
            attachmentDao.insertAttachments(newAttachments)
            Log.d("BackupImport", "Creating ${newCrossRefs.size} new ContextAttachmentCrossRefs in MergeLocalDataSource.")
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
    }
}