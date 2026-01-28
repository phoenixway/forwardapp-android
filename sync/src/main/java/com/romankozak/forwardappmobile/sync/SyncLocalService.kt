package com.romankozak.forwardappmobile.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.sync.SyncMapper.updatedTs
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalService @Inject constructor(
    private val appDatabase: com.romankozak.forwardappmobile.database.AppDatabase,
    private val logicHelper: SyncLogicHelper,
    private val goalDao: com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao,
    private val contextDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao,
    private val listItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao,
    private val linkItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao,
    private val activityRecordDao: com.romankozak.forwardappmobile.data.dao.ActivityRecordDao,
    private val inboxRecordDao: com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao,
    private val contextManagementDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao,
    private val legacyNoteDao: com.romankozak.forwardappmobile.data.dao.LegacyNoteDao,
    private val noteDocumentDao: com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao,
    private val checklistDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao,
    private val recentItemDao: com.romankozak.forwardappmobile.data.dao.RecentItemDao,
    private val backlogOrderDao: com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogOrderDao,
    private val scriptDao: com.romankozak.forwardappmobile.data.dao.ScriptDao,
    private val attachmentDao: com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao,
    private val systemAppDao: com.romankozak.forwardappmobile.data.dao.SystemAppDao,
    private val dayPlanDao: com.romankozak.forwardappmobile.data.dao.DayPlanDao,
    private val dayTaskDao: com.romankozak.forwardappmobile.data.dao.DayTaskDao,
    private val dailyMetricDao: com.romankozak.forwardappmobile.data.dao.DailyMetricDao,
    private val chatDao: com.romankozak.forwardappmobile.data.dao.ChatDao,
    private val conversationFolderDao: com.romankozak.forwardappmobile.data.dao.ConversationFolderDao,
    private val reminderDao: com.romankozak.forwardappmobile.data.dao.ReminderDao,
    private val recurringTaskDao: com.romankozak.forwardappmobile.data.dao.RecurringTaskDao,
    private val contextArtifactDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ContextArtifactDao,
    private val tacticalMissionDao: com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao,
    private val aiEventDao: com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao,
    private val lifeSystemStateDao: com.romankozak.forwardappmobile.data.dao.LifeSystemStateDao,
    private val aiInsightDao: com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao,
    private val structurePresetDao: com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao,
    private val structurePresetItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao,
    private val contextStructureDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
) {

    /**
     * Збирає повний зріз даних локальної бази для бекапу або синхронізації.
     */
    suspend fun loadLocalDatabaseContent(): DatabaseContent {
        val recentProjectEntries = recentItemDao.getAll().map { recentItem ->
            RecentProjectEntry(
                contextId = recentItem.target,
                timestamp = recentItem.lastAccessed
            )
        }
        val scripts = scriptDao.getAll().first()
        val listItems = listItemDao.getAll()
        val backlogOrders = logicHelper.dedupBacklogOrders(backlogOrderDao.getAll())

        return DatabaseContent(
            goals = goalDao.getAll(),
            projects = contextDao.getAll(),
            backlogItems = listItems,
            backlogOrders = backlogOrders,
            legacyNotes = legacyNoteDao.getAll(),
            documents = noteDocumentDao.getAllDocuments(),
            documentItems = noteDocumentDao.getAllDocumentItems(),
            checklists = checklistDao.getAllChecklists(),
            checklistItems = checklistDao.getAllChecklistItems(),
            activityRecords = activityRecordDao.getAllRecordsStream().first(),
            linkItemEntities = linkItemDao.getAllEntities(),
            inboxRecords = inboxRecordDao.getAll(),
            contextLogs = contextManagementDao.getAllLogs(),
            recentProjectEntries = recentProjectEntries,
            scripts = scripts,
            attachments = attachmentDao.getAll(),
            contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs()
        )
    }

    /**
     * Повертає лише ті об'єкти, які мають зміни, що ще не були синхронізовані.
     */
    suspend fun getUnsyncedChanges(): DatabaseContent {
        val local = loadLocalDatabaseContent()

        return DatabaseContent(
            projects = local.projects.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            goals = local.goals.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            backlogItems = local.backlogItems.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            backlogOrders = local.backlogOrders.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            legacyNotes = local.legacyNotes.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            documents = local.documents.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            documentItems = local.documentItems.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            checklists = local.checklists.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            checklistItems = local.checklistItems.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            activityRecords = local.activityRecords.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            linkItemEntities = local.linkItemEntities.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            inboxRecords = local.inboxRecords.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            contextLogs = local.contextLogs.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            scripts = local.scripts.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            attachments = local.attachments.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            contextAttachmentCrossRefs = local.contextAttachmentCrossRefs.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
            recentProjectEntries = emptyList()
        )
    }

    /**
     * Отримання змін для дельта-бекапу (тільки те, що оновлено після [since]).
     */
    suspend fun getChangesSince(since: Long): DatabaseContent {
        val local = loadLocalDatabaseContent()
        return DatabaseContent(
            projects = local.projects.filter { it.updatedTs() > since },
            goals = local.goals.filter { it.updatedTs() > since },
            backlogItems = local.backlogItems.filter { it.updatedTs() > since },
            documents = local.documents.filter { it.updatedTs() > since },
            attachments = local.attachments.filter { it.updatedTs() > since },
            contextAttachmentCrossRefs = local.contextAttachmentCrossRefs.filter { it.updatedTs() > since },
            scripts = local.scripts.filter { it.updatedTs() > since },
            recentProjectEntries = emptyList()
        )
    }

    /**
     * Повне очищення бази даних з урахуванням ієрархії Foreign Keys.
     */
    suspend fun clearAllTables() {
        appDatabase.withTransaction {
            contextManagementDao.deleteAllLogs()
            inboxRecordDao.deleteAll()
            linkItemDao.deleteAll()
            activityRecordDao.clearAll()
            listItemDao.deleteAll()

            noteDocumentDao.deleteAllDocumentItems()
            noteDocumentDao.deleteAllDocuments()
            checklistDao.deleteAllChecklistItems()
            checklistDao.deleteAllChecklists()

            attachmentDao.deleteAllContextAttachmentLinks()
            attachmentDao.deleteAll()

            dayTaskDao.deleteAllTasks()
            dayPlanDao.deleteAllPlans()
            dailyMetricDao.deleteAllMetrics()

            chatDao.deleteAllMessages()
            chatDao.deleteAllConversations()
            conversationFolderDao.deleteAllFolders()

            reminderDao.deleteAll()
            recurringTaskDao.deleteAll()

            contextArtifactDao.deleteAll()
            tacticalMissionDao.deleteAllMissionAttachmentCrossRefs()
            tacticalMissionDao.deleteAllMissions()

            aiEventDao.deleteAll()
            lifeSystemStateDao.deleteAll()
            aiInsightDao.clearAll()

            structurePresetItemDao.deleteAllItems()
            structurePresetDao.deleteAll()
            contextStructureDao.deleteAllItems()
            contextStructureDao.deleteAllStructures()

            systemAppDao.deleteAll()
            recentItemDao.deleteAll()
            scriptDao.deleteAll()

            contextDao.deleteAll()
            goalDao.deleteAll()
        }
    }

    /**
     * Оновлює [syncedAt] для всіх переданих об'єктів поточним часом.
     */
    suspend fun markSyncedNow(content: DatabaseContent) {
        val ts = System.currentTimeMillis()
        appDatabase.withTransaction {
            contextDao.insertContexts(content.projects.map { it.copy(syncedAt = ts) })
            goalDao.insertGoals(content.goals.map { it.copy(syncedAt = ts) })
            listItemDao.insertItems(content.backlogItems.map { it.copy(syncedAt = ts) })

            if (content.backlogOrders.isNotEmpty()) {
                backlogOrderDao.insertOrders(content.backlogOrders.map { it.copy(syncedAt = ts) })
            }

            legacyNoteDao.insertAll(content.legacyNotes.map { it.copy(syncedAt = ts) })
            noteDocumentDao.insertAllDocuments(content.documents.map { it.copy(syncedAt = ts) })
            noteDocumentDao.insertAllDocumentItems(content.documentItems.map { it.copy(syncedAt = ts) })

            checklistDao.insertChecklists(content.checklists.map { it.copy(syncedAt = ts) })
            checklistDao.insertItems(content.checklistItems.map { it.copy(syncedAt = ts) })

            activityRecordDao.insertAll(content.activityRecords.map { it.copy(syncedAt = ts) })
            linkItemDao.insertAll(content.linkItemEntities.map { it.copy(syncedAt = ts) })
            inboxRecordDao.insertAll(content.inboxRecords.map { it.copy(syncedAt = ts) })
            contextManagementDao.insertAllLogs(content.contextLogs.map { it.copy(syncedAt = ts) })

            content.scripts.forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
            attachmentDao.insertAttachments(content.attachments.map { it.copy(syncedAt = ts) })
            attachmentDao.insertContextAttachmentLinks(
                content.contextAttachmentCrossRefs.map { it.copy(syncedAt = ts) }
            )
        }
    }
}