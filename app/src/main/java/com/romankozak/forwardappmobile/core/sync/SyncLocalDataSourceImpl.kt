@file:Suppress("WildcardImport", "MaxLineLength")

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
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestDao
import com.romankozak.forwardappmobile.features.missions.data.*
import com.romankozak.forwardappmobile.sync.SyncLogicHelper
import com.romankozak.forwardappmobile.sync.SyncMapper.updatedTs
import com.romankozak.forwardappmobile.sync.datasource.SyncLocalDataSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalDataSourceImpl
    @Inject
    @Suppress("LongParameterList")
    constructor(
        private val db: AppDatabase,
        private val logicHelper: SyncLogicHelper,
        private val goalDao: GoalDao,
        private val contextDao: ContextDao,
        private val contextParentLinkDao: ContextParentLinkDao,
        private val listItemDao: ListItemDao,
        private val linkItemDao: LinkItemDao,
        private val directionDao: DirectionDao,
        private val activityRecordDao: ActivityRecordDao,
        private val inboxRecordDao: InboxRecordDao,
        private val contextManagementDao: ContextManagementDao,
        private val legacyNoteDao: LegacyNoteDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val musicNoteDao: MusicNoteDao,
        private val checklistDao: ChecklistDao,
        private val recentItemDao: RecentItemDao,
        private val backlogOrderDao: BacklogOrderDao,
        private val scriptDao: ScriptDao,
        private val attachmentDao: AttachmentDao,
        private val systemAppDao: SystemAppDao,
        private val dayPlanDao: DayPlanDao,
        private val dayFocusItemDao: DayFocusItemDao,
        private val dayTaskDao: DayTaskDao,
        private val dailyMetricDao: DailyMetricDao,
        private val chatDao: ChatDao,
        private val conversationFolderDao: ConversationFolderDao,
        private val reminderDao: ReminderDao,
        private val contextArtifactDao: ContextArtifactDao,
        private val tacticalMissionDao: TacticalMissionDao,
        private val tacticalIterationDao: TacticalIterationDao,
        private val missionStreamDao: MissionStreamDao,
        private val tacticalActivitySlotDao: TacticalActivitySlotDao,
        private val arcQuestDao: ArcQuestDao,
        private val aiEventDao: AiEventDao,
        private val lifeSystemStateDao: LifeSystemStateDao,
        private val aiInsightDao: AiInsightDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
        private val contextStructureDao: ContextStructureDao,
        private val contextInboxSortingDao: ContextInboxSortingDao,
        private val contextKeyProblemsDao: ContextKeyProblemsDao,
        private val focusContextIntervalDao: FocusContextIntervalDao,
        private val userStateIntervalDao: UserStateIntervalDao,
        private val mainBeaconDao: MainBeaconDao,
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
                contextParentLinks = contextParentLinkDao.getAllRaw(),
                backlogItems = listItems,
                backlogOrders = backlogOrders,
                legacyNotes = legacyNoteDao.getAll(),
                documents = noteDocumentDao.getAllDocuments(),
                musicNotes = musicNoteDao.getAll(),
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
                dayPlans = dayPlanDao.getAllPlansSync(),
                dayFocusItems = dayFocusItemDao.getAllSync(),
                dayTasks = dayTaskDao.getAllTasksSync(),
                dailyMetrics = dailyMetricDao.getAll(),
                conversations = chatDao.getAllConversationsSync(),
                chatMessages = chatDao.getAllMessagesSync(),
                conversationFolders = conversationFolderDao.getAllSync(),
                reminders = reminderDao.getAllRemindersSync(),
                recurringTasks = emptyList(),
                contextArtifacts = contextArtifactDao.getAllRaw(),
                tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
                tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentCrossRefs(),
                tacticalIterations = tacticalIterationDao.getAllSync(),
                missionStreams = missionStreamDao.getAllSync(),
                tacticalActivitySlots = tacticalActivitySlotDao.getAllSync(),
                arcQuests = arcQuestDao.getAllSync(),
                systemApps = systemAppDao.getAllRaw(),
                aiEvents = aiEventDao.getAllSync(),
                aiInsights = aiInsightDao.getAllSync(),
                mainBeacons = mainBeaconDao.getAllBeaconsSync(),
                mainBeaconGroups = mainBeaconDao.getAllGroupsSync(),
                mainBeaconGroupMembers = mainBeaconDao.getAllGroupMembersSync(),
                mainBeaconParentLinks = mainBeaconDao.getAllParentLinksSync(),
                mainBeaconContextCrossRefs = mainBeaconDao.getAllContextCrossRefsSync(),
                mainBeaconAttachmentCrossRefs = mainBeaconDao.getAllAttachmentCrossRefsSync(),
                mainBeaconLevelStatuses = mainBeaconDao.getAllLevelStatusesSync(),
                lifeSystemStates = lifeSystemStateDao.getAllSync(),
                contextRoleProfiles = structurePresetDao.getAllSync(),
                contextRoleProfileItems = structurePresetItemDao.getAllSync(),
                contextConfigurations = contextStructureDao.getAllSync(),
                projectStructureItems = contextStructureDao.getAllItemsSync(),
                contextInboxSortingRules = contextInboxSortingDao.getAllRaw(),
                contextKeyProblems = contextKeyProblemsDao.getAllRaw(),
                focusContextIntervals = focusContextIntervalDao.getAllRaw(),
                userStateIntervals = userStateIntervalDao.getAllRaw(),
            )
        }

        override suspend fun getUnsyncedChanges(): DatabaseContent {
            val local = loadLocalDatabaseContent()

            return local.copy(
                projects = local.projects.filterUnsynced({ it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                goals = local.goals.filterUnsynced({ it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                backlogItems =
                    local.backlogItems.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                backlogOrders =
                    local.backlogOrders.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                legacyNotes =
                    local.legacyNotes.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                documents =
                    local.documents.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                musicNotes =
                    local.musicNotes.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                checklists =
                    local.checklists.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                checklistItems =
                    local.checklistItems.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                activityRecords =
                    local.activityRecords.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                linkItemEntities =
                    local.linkItemEntities.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                directionItems =
                    local.directionItems.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                inboxRecords =
                    local.inboxRecords.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                contextLogs =
                    local.contextLogs.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                scripts =
                    local.scripts.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                attachments =
                    local.attachments.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                contextAttachmentCrossRefs =
                    local.contextAttachmentCrossRefs.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedTs() },
                        isDeleted = { it.isDeleted },
                    ),
                dayPlans =
                    local.dayPlans.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                dayFocusItems =
                    local.dayFocusItems.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                dayTasks =
                    local.dayTasks.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                tacticalMissions =
                    local.tacticalMissions.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                tacticalIterations =
                    local.tacticalIterations.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                missionStreams =
                    local.missionStreams.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                tacticalActivitySlots =
                    local.tacticalActivitySlots.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
                arcQuests =
                    local.arcQuests.filterUnsynced(
                        syncedAt = { it.syncedAt },
                        updatedAt = { it.updatedAt ?: it.createdAt },
                        isDeleted = { it.isDeleted },
                    ),
            )
        }

        override suspend fun getChangesSince(timestamp: Long): DatabaseContent {
            val local = loadLocalDatabaseContent()
            return DatabaseContent(
                projects = local.projects.filter { it.updatedTs() > timestamp },
                contextParentLinks = local.contextParentLinks.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                goals = local.goals.filter { it.updatedTs() > timestamp },
                backlogItems = local.backlogItems.filter { it.updatedTs() > timestamp },
                documents = local.documents.filter { it.updatedTs() > timestamp },
                musicNotes = local.musicNotes.filter { it.updatedTs() > timestamp },
                attachments = local.attachments.filter { it.updatedTs() > timestamp },
                contextAttachmentCrossRefs = local.contextAttachmentCrossRefs.filter { it.updatedTs() > timestamp },
                directionItems = local.directionItems.filter { it.updatedTs() > timestamp },
                scripts = local.scripts.filter { it.updatedTs() > timestamp },
                contextInboxSortingRules = local.contextInboxSortingRules.filter { it.updatedAt > timestamp },
                contextKeyProblems = local.contextKeyProblems.filter { it.updatedAt > timestamp },
                focusContextIntervals = local.focusContextIntervals.filter { it.startedAt > timestamp || (it.endedAt ?: 0L) > timestamp },
                userStateIntervals = local.userStateIntervals.filter { it.startedAt > timestamp || (it.endedAt ?: 0L) > timestamp },
                dayPlans = local.dayPlans.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                dayFocusItems = local.dayFocusItems.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                dayTasks = local.dayTasks.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                dailyMetrics = local.dailyMetrics.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                conversations = local.conversations.filter { it.creationTimestamp > timestamp },
                chatMessages = local.chatMessages.filter { it.timestamp > timestamp },
                conversationFolders = local.conversationFolders,
                reminders = local.reminders.filter { (it.updatedAt ?: it.creationTime) > timestamp },
                recurringTasks = emptyList(),
                contextArtifacts = local.contextArtifacts.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                tacticalMissions = local.tacticalMissions.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                tacticalMissionAttachments = local.tacticalMissionAttachments,
                tacticalIterations = local.tacticalIterations.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                missionStreams = local.missionStreams.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                tacticalActivitySlots = local.tacticalActivitySlots.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                arcQuests = local.arcQuests.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                systemApps = local.systemApps.filter { (it.updatedAt ?: it.createdAt) > timestamp },
                aiEvents = local.aiEvents.filter { it.timestamp > timestamp },
                aiInsights = local.aiInsights.filter { it.timestamp > timestamp },
                mainBeacons = local.mainBeacons.filter { it.updatedAt > timestamp },
                mainBeaconGroups = local.mainBeaconGroups.filter { it.updatedAt > timestamp },
                mainBeaconGroupMembers = local.mainBeaconGroupMembers,
                mainBeaconParentLinks = local.mainBeaconParentLinks.filter { it.updatedAt > timestamp },
                mainBeaconContextCrossRefs = local.mainBeaconContextCrossRefs,
                mainBeaconAttachmentCrossRefs = local.mainBeaconAttachmentCrossRefs,
                mainBeaconLevelStatuses = local.mainBeaconLevelStatuses.filter { it.updatedAt > timestamp },
                lifeSystemStates = local.lifeSystemStates.filter { it.updatedAt > timestamp },
                contextRoleProfiles = local.contextRoleProfiles.filter { it.updatedAt > timestamp },
                contextRoleProfileItems = local.contextRoleProfileItems.filter { it.updatedAt > timestamp },
                contextConfigurations = local.contextConfigurations.filter { it.updatedAt > timestamp },
                projectStructureItems = local.projectStructureItems.filter { it.updatedAt > timestamp },
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
                musicNoteDao.insertAll(content.musicNotes.map { it.copy(syncedAt = ts) })

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
                dayPlanDao.insertPlans(content.dayPlans.map { it.copy(syncedAt = ts) })
                dayFocusItemDao.insertAll(content.dayFocusItems.map { it.copy(syncedAt = ts) })
                dayTaskDao.insertTasks(content.dayTasks.map { it.copy(syncedAt = ts) })
                tacticalMissionDao.insertMissions(content.tacticalMissions.map { it.copy(syncedAt = ts) })
                tacticalIterationDao.insertAll(content.tacticalIterations.map { it.copy(syncedAt = ts) })
                missionStreamDao.insertAll(content.missionStreams.map { it.copy(syncedAt = ts) })
                tacticalActivitySlotDao.insertAll(content.tacticalActivitySlots.map { it.copy(syncedAt = ts) })
                arcQuestDao.insertAll(content.arcQuests.map { it.copy(syncedAt = ts) })
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
                musicNoteDao.deleteAll()
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
                contextArtifactDao.deleteAll()
                tacticalMissionDao.deleteAllMissionAttachmentCrossRefs()
                tacticalMissionDao.deleteAllMissions()
                tacticalActivitySlotDao.deleteAll()
                tacticalIterationDao.deleteAll()
                missionStreamDao.deleteAll()
                arcQuestDao.deleteAll()
                aiEventDao.deleteAll()
                lifeSystemStateDao.deleteAll()
                aiInsightDao.clearAll()
                structurePresetItemDao.deleteAllItems()
                structurePresetDao.deleteAll()
                contextStructureDao.deleteAllItems()
                contextStructureDao.deleteAllStructures()
                contextInboxSortingDao.deleteAll()
                contextKeyProblemsDao.deleteAll()
                focusContextIntervalDao.deleteAll()
                userStateIntervalDao.deleteAll()
                systemAppDao.deleteAll()
                recentItemDao.deleteAll()
                scriptDao.deleteAll()
                directionDao.deleteAll()
                contextDao.deleteAll()
                goalDao.deleteAll()
            }
        }

        private fun <T> List<T>.filterUnsynced(
            syncedAt: (T) -> Long?,
            updatedAt: (T) -> Long,
            isDeleted: (T) -> Boolean,
        ): List<T> =
            filter {
                logicHelper.isUnsynced(
                    item = it,
                    syncedAtSelector = syncedAt,
                    updatedSelector = updatedAt,
                    isDeletedSelector = isDeleted,
                )
            }
    }
