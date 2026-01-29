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


}