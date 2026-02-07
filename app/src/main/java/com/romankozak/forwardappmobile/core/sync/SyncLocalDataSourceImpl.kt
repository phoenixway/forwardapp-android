package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.sync.SyncLogicHelper
import com.romankozak.forwardappmobile.sync.SyncMapper.updatedTs
import com.romankozak.forwardappmobile.sync.datasource.SyncLocalDataSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalDataSourceImpl
    @Inject
    constructor(
        private val db: AppDatabase,
        private val logicHelper: SyncLogicHelper,
        private val goalDao: GoalDao,
        private val contextDao: ContextDao,
        private val listItemDao: ListItemDao,
        private val linkItemDao: LinkItemDao,
        private val directionDao: DirectionDao,
        private val activityRecordDao: ActivityRecordDao,
        private val inboxRecordDao: InboxRecordDao,
        private val contextManagementDao: ContextManagementDao,
        private val legacyNoteDao: LegacyNoteDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val checklistDao: ChecklistDao,
        private val recentItemDao: RecentItemDao,
        private val backlogOrderDao: BacklogOrderDao,
        private val scriptDao: ScriptDao,
        private val attachmentDao: AttachmentDao,
        private val systemAppDao: SystemAppDao,
        private val dayPlanDao: DayPlanDao,
        private val dayTaskDao: DayTaskDao,
        private val dailyMetricDao: DailyMetricDao,
        private val chatDao: ChatDao,
        private val conversationFolderDao: ConversationFolderDao,
        private val reminderDao: ReminderDao,
        private val recurringTaskDao: RecurringTaskDao,
        private val contextArtifactDao: ContextArtifactDao,
        private val tacticalMissionDao: TacticalMissionDao,
        private val aiEventDao: AiEventDao,
        private val lifeSystemStateDao: LifeSystemStateDao,
        private val aiInsightDao: AiInsightDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
        private val contextStructureDao: ContextStructureDao,
    ) : SyncLocalDataSource {
        override suspend fun loadLocalDatabaseContent(): DatabaseContent {
            val recentProjectEntries =
                recentItemDao.getAll().map {
                    RecentProjectEntry(contextId = it.target, timestamp = it.lastAccessed)
                }
            val scripts = scriptDao.getAll()
            val listItems = listItemDao.getAll()
            val backlogOrders = logicHelper.dedupBacklogOrders(backlogOrderDao.getAll())

            return DatabaseContent(
                goals = goalDao.getAll(),
                projects = contextDao.getAll(),
                backlogItems = listItems,
                backlogOrders = backlogOrders,
                legacyNotes = legacyNoteDao.getAll(),
                documents = noteDocumentDao.getAllDocuments(),
                checklists = checklistDao.getAllChecklists(),
                checklistItems = checklistDao.getAllChecklistItems(),
                activityRecords = activityRecordDao.getAllRecordsStream().first(),
                linkItemEntities = linkItemDao.getAllEntities(),
                directionItems = directionDao.getAllRaw(),
                inboxRecords = inboxRecordDao.getAll(),
                contextLogs = contextManagementDao.getAllLogs(),
                recentProjectEntries = recentProjectEntries,
                scripts = scripts,
                attachments = attachmentDao.getAll(),
                contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs(),
            )
        }

        override suspend fun getUnsyncedChanges(): DatabaseContent {
            val local = loadLocalDatabaseContent()

            return DatabaseContent(
                projects = local.projects.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
                goals = local.goals.filter { logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }) },
                backlogItems =
                    local.backlogItems.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                backlogOrders =
                    local.backlogOrders.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                legacyNotes =
                    local.legacyNotes.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                documents =
                    local.documents.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                checklists =
                    local.checklists.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                checklistItems =
                    local.checklistItems.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                activityRecords =
                    local.activityRecords.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                linkItemEntities =
                    local.linkItemEntities.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                directionItems =
                    local.directionItems.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                inboxRecords =
                    local.inboxRecords.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                contextLogs =
                    local.contextLogs.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                scripts =
                    local.scripts.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                attachments =
                    local.attachments.filter {
                        logicHelper.isUnsynced(
                            it,
                            { it.syncedAt },
                            { it.updatedTs() },
                            { it.isDeleted },
                        )
                    },
                contextAttachmentCrossRefs =
                    local.contextAttachmentCrossRefs.filter {
                        logicHelper.isUnsynced(it, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted })
                    },
            )
        }

        override suspend fun getChangesSince(since: Long): DatabaseContent {
            val local = loadLocalDatabaseContent()
            return DatabaseContent(
                projects = local.projects.filter { it.updatedTs() > since },
                goals = local.goals.filter { it.updatedTs() > since },
                backlogItems = local.backlogItems.filter { it.updatedTs() > since },
                documents = local.documents.filter { it.updatedTs() > since },
                attachments = local.attachments.filter { it.updatedTs() > since },
                contextAttachmentCrossRefs = local.contextAttachmentCrossRefs.filter { it.updatedTs() > since },
                directionItems = local.directionItems.filter { it.updatedTs() > since },
                scripts = local.scripts.filter { it.updatedTs() > since },
            )
        }

        override suspend fun markSyncedNow(content: DatabaseContent) {
            val ts = System.currentTimeMillis()
            db.withTransaction {
                contextDao.insertContexts(content.projects.map { it.copy(syncedAt = ts) })
                goalDao.insertGoals(content.goals.map { it.copy(syncedAt = ts) })
                listItemDao.insertItems(content.backlogItems.map { it.copy(syncedAt = ts) })

                if (content.backlogOrders.isNotEmpty()) {
                    backlogOrderDao.insertOrders(content.backlogOrders.map { it.copy(syncedAt = ts) })
                }

                legacyNoteDao.insertAll(content.legacyNotes.map { it.copy(syncedAt = ts) })
                noteDocumentDao.insertAllDocuments(content.documents.map { it.copy(syncedAt = ts) })

                checklistDao.insertChecklists(content.checklists.map { it.copy(syncedAt = ts) })
                checklistDao.insertItems(content.checklistItems.map { it.copy(syncedAt = ts) })

                activityRecordDao.insertAll(content.activityRecords.map { it.copy(syncedAt = ts) })
                linkItemDao.insertAll(content.linkItemEntities.map { it.copy(syncedAt = ts) })
                directionDao.updateAll(content.directionItems.map { it.copy(syncedAt = ts) })
                inboxRecordDao.insertAll(content.inboxRecords.map { it.copy(syncedAt = ts) })
                contextManagementDao.insertAllLogs(content.contextLogs.map { it.copy(syncedAt = ts) })

                content.scripts.forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
                attachmentDao.insertAttachments(content.attachments.map { it.copy(syncedAt = ts) })
                attachmentDao.insertContextAttachmentLinks(content.contextAttachmentCrossRefs.map { it.copy(syncedAt = ts) })
            }
        }

        override suspend fun clearAllTables() {
            db.withTransaction {
                contextManagementDao.deleteAllLogs()
                inboxRecordDao.deleteAll()
                linkItemDao.deleteAll()
                activityRecordDao.clearAll()
                listItemDao.deleteAll()
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
                directionDao.deleteAll()
                contextDao.deleteAll()
                goalDao.deleteAll()
            }
        }
    }
