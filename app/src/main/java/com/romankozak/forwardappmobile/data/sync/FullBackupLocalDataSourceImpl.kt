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
            clearAllTables()

            // Вставляємо дані, використовуючи точні назви методів з ваших DAO
            goalDao.insertGoals(content.goals) //
            contextDao.insertContexts(content.projects) //
            listItemDao.insertItems(content.backlogItems) //

            noteDocumentDao.insertAllDocuments(content.documents)
            attachmentDao.insertAttachments(content.attachments)
            attachmentDao.insertContextAttachmentLinks(content.contextAttachmentCrossRefs)

            dayPlanDao.insertAll(content.dayPlans)
            dayTaskDao.insertAll(content.dayTasks)
            dailyMetricDao.insertAll(content.dailyMetrics)
            chatDao.insertConversations(content.conversations)
            chatDao.insertMessages(content.chatMessages)
            reminderDao.insertAll(content.reminders)
            tacticalMissionDao.insertMissions(content.tacticalMissions)
            aiInsightDao.upsertAll(content.aiInsights)
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