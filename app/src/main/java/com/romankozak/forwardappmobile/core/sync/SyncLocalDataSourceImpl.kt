@file:Suppress("WildcardImport", "MaxLineLength")

package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncCrossRefVersion
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.*
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
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
        private val contextWorkspaceWriteThrough: ContextWorkspaceWriteThrough,
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
        private val dayThemeDocumentDao: DayThemeDocumentDao,
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
        override suspend fun getUnsyncedSelection(): LocalSyncSelection {
            val projects = contextDao.getAll()
            val goals = goalDao.getAll()
            val backlogItems = listItemDao.getAll()
            val backlogOrders = logicHelper.dedupBacklogOrders(backlogOrderDao.getAll())
            val legacyNotes = legacyNoteDao.getAll()
            val documents = noteDocumentDao.getAllDocuments()
            val musicNotes = musicNoteDao.getAll()
            val checklists = checklistDao.getAllChecklists()
            val checklistItems = checklistDao.getAllChecklistItems()
            val activityRecords = activityRecordDao.getAllRecordsStream().first()
            val linkItemEntities = linkItemDao.getAllEntities()
            val directionItems = directionDao.getAllRaw()
            val inboxRecords = inboxRecordDao.getAll()
            val contextLogs = contextManagementDao.getLegacyContextLogs()
            val scripts = scriptDao.getAll()
            val attachments = attachmentDao.getAll()
            val contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs()
            val dayPlans = dayPlanDao.getAllPlansSync()
            val dayFocusItems = dayFocusItemDao.getAllSync()
            val dayTasks = dayTaskDao.getAllTasksSync()
            val tacticalMissions = tacticalMissionDao.getAllMissionsSync()
            val tacticalIterations = tacticalIterationDao.getAllSync()
            val missionStreams = missionStreamDao.getAllSync()
            val tacticalActivitySlots = tacticalActivitySlotDao.getAllSync()
            val arcQuests = arcQuestDao.getAllSync()

            fun <T> versions(
                items: List<T>,
                id: (T) -> String,
                version: (T) -> Long,
                syncedAt: (T) -> Long?,
                updatedAt: (T) -> Long,
                isDeleted: (T) -> Boolean,
            ): List<LocalSyncVersion> =
                items
                    .filterUnsynced(syncedAt, updatedAt, isDeleted)
                    .map { LocalSyncVersion(id(it), version(it)) }

            return LocalSyncSelection(
                contexts = versions(projects, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                goals = versions(goals, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                backlogItems = versions(backlogItems, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                backlogOrders = versions(backlogOrders, { it.id }, { it.orderVersion }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                notes = versions(legacyNotes, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                documents = versions(documents, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                musicNotes = versions(musicNotes, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                checklists = versions(checklists, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                checklistItems = versions(checklistItems, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                activityRecords = versions(activityRecords, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                linkItemEntities = versions(linkItemEntities, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                directionItems = versions(directionItems, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                inbox = versions(inboxRecords, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                logs = versions(contextLogs, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                scripts = versions(scripts, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                attachments = versions(attachments, { it.id }, { it.version }, { it.syncedAt }, { it.updatedTs() }, { it.isDeleted }),
                crossRefs =
                    contextAttachmentCrossRefs
                        .filterUnsynced({ it.syncedAt }, { it.updatedTs() }, { it.isDeleted })
                        .map {
                            LocalSyncCrossRefVersion(
                                contextId = it.contextId,
                                attachmentId = it.attachmentId,
                                version = it.version,
                            )
                        },
                dayPlans = versions(dayPlans, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                dayFocusItems = versions(dayFocusItems, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                dayTasks = versions(dayTasks, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                tacticalMissions = versions(tacticalMissions, { it.id.toString() }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                tacticalIterations = versions(tacticalIterations, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                missionStreams = versions(missionStreams, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                tacticalActivitySlots = versions(tacticalActivitySlots, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
                arcQuests = versions(arcQuests, { it.id }, { it.version }, { it.syncedAt }, { it.updatedAt ?: it.createdAt }, { it.isDeleted }),
            )
        }

        override suspend fun getChangesSince(timestamp: Long): SnapshotBundle {
            val projects = contextDao.getAll()
            val contextParentLinks = contextParentLinkDao.getAllRaw()
            val goals = goalDao.getAll()
            val backlogItems = listItemDao.getAll()
            val documents = noteDocumentDao.getAllDocuments()
            val musicNotes = musicNoteDao.getAll()
            val attachments = attachmentDao.getAll()
            val contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs()
            val directionItems = directionDao.getAllRaw()
            val inboxRecords = inboxRecordDao.getAll()
            val scripts = scriptDao.getAll()
            val contextInboxSortingRules = contextInboxSortingDao.getAllRaw()
            val contextKeyProblems = contextKeyProblemsDao.getAllRaw()
            val focusContextIntervals = focusContextIntervalDao.getAllRaw()
            val userStateIntervals = userStateIntervalDao.getAllRaw()
            val dayPlans = dayPlanDao.getAllPlansSync()
            val dayFocusItems = dayFocusItemDao.getAllSync()
            val dayTasks = dayTaskDao.getAllTasksSync()
            val dailyMetrics = dailyMetricDao.getAll()
            val conversations = chatDao.getAllConversationsSync()
            val chatMessages = chatDao.getAllMessagesSync()
            val conversationFolders = conversationFolderDao.getAllSync()
            val reminders = reminderDao.getAllRemindersSync()
            val contextArtifacts = contextArtifactDao.getAllRaw()
            val tacticalMissions = tacticalMissionDao.getAllMissionsSync()
            val tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentCrossRefs()
            val tacticalIterations = tacticalIterationDao.getAllSync()
            val missionStreams = missionStreamDao.getAllSync()
            val tacticalActivitySlots = tacticalActivitySlotDao.getAllSync()
            val arcQuests = arcQuestDao.getAllSync()
            val systemApps = systemAppDao.getAllRaw()
            val aiEvents = aiEventDao.getAllSync()
            val aiInsights = aiInsightDao.getAllSync()
            val mainBeacons = mainBeaconDao.getAllBeaconsSync()
            val mainBeaconGroups = mainBeaconDao.getAllGroupsSync()
            val mainBeaconGroupMembers = mainBeaconDao.getAllGroupMembersSync()
            val mainBeaconParentLinks = mainBeaconDao.getAllParentLinksSync()
            val mainBeaconContextCrossRefs = mainBeaconDao.getAllContextCrossRefsSync()
            val mainBeaconAttachmentCrossRefs = mainBeaconDao.getAllAttachmentCrossRefsSync()
            val mainBeaconLevelStatuses = mainBeaconDao.getAllLevelStatusesSync()
            val lifeSystemStates = lifeSystemStateDao.getAllSync()
            val contextRoleProfiles = structurePresetDao.getAllSync()
            val contextRoleProfileItems = structurePresetItemDao.getAllSync()
            val contextConfigurations = contextStructureDao.getAllSync()
            val projectStructureItems = contextStructureDao.getAllItemsSync()

            val changedAttachments =
                attachments.filter { it.updatedTs() > timestamp }
            val changedAttachmentIds =
                changedAttachments.mapTo(hashSetOf()) { it.id }

            val selectedCrossRefs =
                (
                    contextAttachmentCrossRefs.filter { it.updatedTs() > timestamp } +
                        contextAttachmentCrossRefs.filter { it.attachmentId in changedAttachmentIds }
                ).distinctBy { "${it.contextId}\u0000${it.attachmentId}" }

            return SnapshotBundle(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                contexts = projects.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                contextParentLinks =
                    contextParentLinks
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { it.toSnapshot() },
                goals = goals.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                backlogItems =
                    backlogItems.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                documents =
                    documents.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                musicNotes =
                    musicNotes.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                attachments = changedAttachments.map { it.toSnapshot() },
                crossRefs = selectedCrossRefs.map { it.toSnapshot() },
                directionItems =
                    directionItems.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                inbox =
                    inboxRecords.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                scripts =
                    scripts.filter { it.updatedTs() > timestamp }.map { it.toSnapshot() },
                contextInboxSortingRules =
                    contextInboxSortingRules
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                contextKeyProblems =
                    contextKeyProblems
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                focusContextIntervals =
                    focusContextIntervals
                        .filter {
                            it.startedAt > timestamp ||
                                (it.endedAt ?: 0L) > timestamp
                        }
                        .map { it.toSnapshot() },
                userStateIntervals =
                    userStateIntervals
                        .filter {
                            it.startedAt > timestamp ||
                                (it.endedAt ?: 0L) > timestamp
                        }
                        .map { it.toSnapshot() },
                dayPlans =
                    dayPlans
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { it.toSnapshot() },
                dayFocusItems =
                    dayFocusItems
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { item ->
                            com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                                .dayFocusItemSnapshot(item, item.toSnapshot())
                        },
                dayTasks =
                    dayTasks
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { task ->
                            com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                                .dayTaskSnapshot(task, task.toSnapshot())
                        },
                dayThemeDocuments = emptyList(),
                dailyMetrics =
                    dailyMetrics
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { it.toSnapshot() },
                conversations =
                    conversations
                        .filter { it.creationTimestamp > timestamp }
                        .map { it.toSnapshot() },
                chatMessages =
                    chatMessages
                        .filter { it.timestamp > timestamp }
                        .map { it.toSnapshot() },
                conversationFolders =
                    conversationFolders.map { it.toSnapshot() },
                reminders =
                    reminders
                        .filter { (it.updatedAt ?: it.creationTime) > timestamp }
                        .map { it.toSnapshot() },
                recurringTasks = emptyList(),
                artifacts =
                    contextArtifacts
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { it.toSnapshot() },
                tacticalMissions =
                    tacticalMissions
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { it.toSnapshot() },
                tacticalMissionAttachments =
                    tacticalMissionAttachments.map { it.toSnapshot() },
                tacticalIterations =
                    tacticalIterations.filter {
                        (it.updatedAt ?: it.createdAt) > timestamp
                    },
                missionStreams =
                    missionStreams.filter {
                        (it.updatedAt ?: it.createdAt) > timestamp
                    },
                tacticalActivitySlots =
                    tacticalActivitySlots.filter {
                        (it.updatedAt ?: it.createdAt) > timestamp
                    },
                arcQuests =
                    arcQuests.filter {
                        (it.updatedAt ?: it.createdAt) > timestamp
                    },
                systemApps =
                    systemApps
                        .filter { (it.updatedAt ?: it.createdAt) > timestamp }
                        .map { it.toSnapshot() },
                aiEvents =
                    aiEvents.filter { it.timestamp > timestamp }.map { it.toSnapshot() },
                aiInsights =
                    aiInsights.filter { it.timestamp > timestamp }.map { it.toSnapshot() },
                mainBeacons =
                    mainBeacons.filter { it.updatedAt > timestamp }.map { it.toSnapshot() },
                mainBeaconGroups =
                    mainBeaconGroups.filter { it.updatedAt > timestamp }.map { it.toSnapshot() },
                mainBeaconGroupMembers =
                    mainBeaconGroupMembers.map { it.toSnapshot() },
                mainBeaconParentLinks =
                    mainBeaconParentLinks
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                mainBeaconContextCrossRefs =
                    mainBeaconContextCrossRefs.map { it.toSnapshot() },
                mainBeaconAttachmentCrossRefs =
                    mainBeaconAttachmentCrossRefs.map { it.toSnapshot() },
                mainBeaconLevelStatuses =
                    mainBeaconLevelStatuses
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                lifeSystemStates =
                    lifeSystemStates
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                contextRoleProfiles =
                    contextRoleProfiles
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                contextRoleProfileItems =
                    contextRoleProfileItems
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                contextConfigurations =
                    contextConfigurations
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
                projectStructureItems =
                    projectStructureItems
                        .filter { it.updatedAt > timestamp }
                        .map { it.toSnapshot() },
            )
        }

        override suspend fun acknowledge(selection: LocalSyncSelection) {
            val ts = System.currentTimeMillis()

            db.withTransaction {
                val projects = contextDao.getAll()
                val goals = goalDao.getAll()
                val backlogItems = listItemDao.getAll()
                val backlogOrders = logicHelper.dedupBacklogOrders(backlogOrderDao.getAll())
                val legacyNotes = legacyNoteDao.getAll()
                val documents = noteDocumentDao.getAllDocuments()
                val musicNotes = musicNoteDao.getAll()
                val checklists = checklistDao.getAllChecklists()
                val checklistItems = checklistDao.getAllChecklistItems()
                val activityRecords = activityRecordDao.getAllRecordsStream().first()
                val linkItemEntities = linkItemDao.getAllEntities()
                val directionItems = directionDao.getAllRaw()
                val inboxRecords = inboxRecordDao.getAll()
                val contextLogs = contextManagementDao.getLegacyContextLogs()
                val scripts = scriptDao.getAll()
                val attachments = attachmentDao.getAll()
                val contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefs()
                val dayPlans = dayPlanDao.getAllPlansSync()
                val dayFocusItems = dayFocusItemDao.getAllSync()
                val dayTasks = dayTaskDao.getAllTasksSync()
                val tacticalMissions = tacticalMissionDao.getAllMissionsSync()
                val tacticalIterations = tacticalIterationDao.getAllSync()
                val missionStreams = missionStreamDao.getAllSync()
                val tacticalActivitySlots = tacticalActivitySlotDao.getAllSync()
                val arcQuests = arcQuestDao.getAllSync()

                fun versions(items: List<LocalSyncVersion>): Map<String, Long> =
                    items.associate { it.id to it.version }

                val contextVersions = versions(selection.contexts)
                val goalVersions = versions(selection.goals)
                val backlogItemVersions = versions(selection.backlogItems)
                val backlogOrderVersions = versions(selection.backlogOrders)
                val noteVersions = versions(selection.notes)
                val documentVersions = versions(selection.documents)
                val musicNoteVersions = versions(selection.musicNotes)
                val checklistVersions = versions(selection.checklists)
                val checklistItemVersions = versions(selection.checklistItems)
                val activityRecordVersions = versions(selection.activityRecords)
                val linkVersions = versions(selection.linkItemEntities)
                val directionVersions = versions(selection.directionItems)
                val inboxVersions = versions(selection.inbox)
                val logVersions = versions(selection.logs)
                val scriptVersions = versions(selection.scripts)
                val attachmentVersions = versions(selection.attachments)
                val planVersions = versions(selection.dayPlans)
                val focusItemVersions = versions(selection.dayFocusItems)
                val taskVersions = versions(selection.dayTasks)
                val missionVersions = versions(selection.tacticalMissions)
                val iterationVersions = versions(selection.tacticalIterations)
                val streamVersions = versions(selection.missionStreams)
                val slotVersions = versions(selection.tacticalActivitySlots)
                val questVersions = versions(selection.arcQuests)
                val crossRefVersions =
                    selection.crossRefs.associate {
                        "${it.contextId}\u0000${it.attachmentId}" to it.version
                    }

                contextDao.insertContexts(
                    projects.filter { contextVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                goalDao.insertGoals(
                    goals.filter { goalVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                listItemDao.insertItems(
                    backlogItems.filter { backlogItemVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                backlogOrderDao.insertOrders(
                    backlogOrders
                        .filter { backlogOrderVersions[it.id] == it.orderVersion }
                        .map { it.copy(syncedAt = ts) },
                )
                legacyNoteDao.insertAll(
                    legacyNotes.filter { noteVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                noteDocumentDao.insertAllDocuments(
                    documents.filter { documentVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                musicNoteDao.insertAll(
                    musicNotes.filter { musicNoteVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                checklistDao.insertChecklists(
                    checklists.filter { checklistVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                checklistDao.insertItems(
                    checklistItems.filter { checklistItemVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                activityRecordDao.insertAll(
                    activityRecords.filter { activityRecordVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                linkItemDao.insertAll(
                    linkItemEntities.filter { linkVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                directionDao.updateAll(
                    directionItems.filter { directionVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                inboxRecordDao.insertAll(
                    inboxRecords.filter { inboxVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                contextManagementDao.insertAllLogs(
                    contextLogs.filter { logVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                scripts
                    .filter { scriptVersions[it.id] == it.version }
                    .forEach { scriptDao.insert(it.copy(syncedAt = ts)) }
                attachmentDao.insertAttachments(
                    attachments.filter { attachmentVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                attachmentDao.insertContextAttachmentLinks(
                    contextAttachmentCrossRefs
                        .filter {
                            crossRefVersions["${it.contextId}\u0000${it.attachmentId}"] == it.version
                        }
                        .map { it.copy(syncedAt = ts) },
                )
                dayPlanDao.insertPlans(
                    dayPlans.filter { planVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                dayFocusItemDao.insertAll(
                    dayFocusItems.filter { focusItemVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                dayTaskDao.insertTasks(
                    dayTasks.filter { taskVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                tacticalMissionDao.insertMissions(
                    tacticalMissions
                        .filter { missionVersions[it.id.toString()] == it.version }
                        .map { it.copy(syncedAt = ts) },
                )
                tacticalIterationDao.insertAll(
                    tacticalIterations.filter { iterationVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                missionStreamDao.insertAll(
                    missionStreams.filter { streamVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                tacticalActivitySlotDao.insertAll(
                    tacticalActivitySlots.filter { slotVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
                arcQuestDao.insertAll(
                    arcQuests.filter { questVersions[it.id] == it.version }.map { it.copy(syncedAt = ts) },
                )
            }
        }

        override suspend fun clearAllTables() {
            contextWorkspaceWriteThrough.mutate {
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
                dayThemeDocumentDao.deleteAll()
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
