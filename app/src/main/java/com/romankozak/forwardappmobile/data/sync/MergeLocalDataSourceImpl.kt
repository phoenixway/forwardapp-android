package com.romankozak.forwardappmobile.data.sync

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.*
import com.romankozak.forwardappmobile.core.data.models.sync.ChangeType
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toEntity
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
    private val aiInsightDao: AiInsightDao
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

    override suspend fun applySnapshotBundle(bundle: com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle) {
        db.withTransaction {
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
    }
}