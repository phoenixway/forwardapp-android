// File: FullBackupLocalDataSourceImpl.kt

@file:Suppress("WildcardImport", "MaxLineLength", "UnusedPrivateProperty")

package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncAck
import com.romankozak.forwardappmobile.data.workspace.toCanonicalWorkspaceProblemSyncPayloadOrNull
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceProblemSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceInboxSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceConnectionSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBacklogSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagTransportStore
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed

import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.core.context.normalizeLegacyStructuralContextBacklog

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toWorkspaceOwnedEntityOrNull
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.DayThemeCanonicalBootstrapStateEntity
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationSyncStore
import com.romankozak.forwardappmobile.data.orientation.storeCanonicalPayload
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceDirectionEntrySyncStore
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.data.workspace.capability.InboxSortingLegacyFullBackupAdapter
import com.romankozak.forwardappmobile.data.workspace.capability.LegacyInboxFullBackupAdapter
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogSyncStore
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogMigrationDryRunAdapter
import com.romankozak.forwardappmobile.data.daythemes.planLegacyDayThemeMerge
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestDao
import com.romankozak.forwardappmobile.features.missions.data.*
import com.romankozak.forwardappmobile.sync.SyncMapper
import com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncPayload
import com.romankozak.forwardappmobile.sync.datasource.CanonicalRecurringSeriesSyncVersion
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload
import com.romankozak.forwardappmobile.sync.datasource.CanonicalExecutionLogSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySyncVersion
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullBackupLocalDataSourceImpl
    @Inject
    constructor(
        private val db: AppDatabase,
        val settingsRepository: SettingsRepository,
        private val contextDao: ContextDao,
        private val contextParentLinkDao: ContextParentLinkDao,
        private val goalDao: GoalDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val musicNoteDao: MusicNoteDao,
        private val legacyNoteDao: LegacyNoteDao,
        private val checklistDao: ChecklistDao,
        private val attachmentDao: AttachmentDao,
        private val recentItemDao: RecentItemDao,
        private val dayPlanDao: DayPlanDao,
        private val dayTaskDao: DayTaskDao,
        private val canonicalDayThemeDao: CanonicalDayThemeDao,
        private val canonicalDayThemeBootstrapper: CanonicalDayThemeBootstrapper,
        private val canonicalOrientationBootstrapper: CanonicalOrientationBootstrapper,
        private val canonicalWorkspaceBootstrapper: CanonicalWorkspaceBootstrapper,
        private val contextWorkspaceWriteThrough: ContextWorkspaceWriteThrough,
        private val executionLogWorkspaceOwnershipBridge: ExecutionLogWorkspaceOwnershipBridge,
        private val canonicalExecutionLogSyncStore: CanonicalExecutionLogSyncStore,
        private val canonicalWorkspaceDirectionEntrySyncStore: CanonicalWorkspaceDirectionEntrySyncStore,
        private val canonicalOrientationSyncStore: CanonicalOrientationSyncStore,
        private val dailyMetricDao: DailyMetricDao,
        private val chatDao: ChatDao,
        private val reminderDao: ReminderDao,
        private val tacticalMissionDao: TacticalMissionDao,
        private val tacticalIterationDao: TacticalIterationDao,
        private val missionStreamDao: MissionStreamDao,
        private val tacticalActivitySlotDao: TacticalActivitySlotDao,
        private val arcQuestDao: ArcQuestDao,
        private val aiInsightDao: AiInsightDao,
        private val dayFocusItemDao: DayFocusItemDao,
        private val lifeManagementLevelStatusDao: LifeManagementLevelStatusDao,
        private val mainBeaconDao: MainBeaconDao,
        private val dayManagementRuntimeRepository: DayManagementRuntimeRepository,
        private val databaseInitializer: DatabaseInitializer,
        private val backlogOrderDao: BacklogOrderDao,
        private val backlogItemDao: ListItemDao,
        private val contextLogDao: ContextManagementDao,
        private val scriptDao: ScriptDao,
        private val contextManagementDao: ContextManagementDao,
        private val systemAppDao: SystemAppDao,
        private val activityRecordDao: ActivityRecordDao,
        private val linkItemDao: LinkItemDao,
        private val conversationFolderDao: ConversationFolderDao,
        private val canonicalRecurringSeriesDao: com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao,
        private val aiEventDao: AiEventDao,
        private val lifeSystemStateDao: LifeSystemStateDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
        private val contextStructureDao: ContextStructureDao,
        private val contextInboxSortingDao: ContextInboxSortingDao,
        private val canonicalWorkspaceProblemSyncStore: CanonicalWorkspaceProblemSyncStore,
        private val canonicalWorkspaceInboxSyncStore: CanonicalWorkspaceInboxSyncStore,
        private val canonicalWorkspaceConnectionSyncStore: CanonicalWorkspaceConnectionSyncStore,
        private val canonicalWorkspaceBacklogSyncStore: CanonicalWorkspaceBacklogSyncStore,
        private val canonicalWorkspaceTagTransportStore: CanonicalWorkspaceTagTransportStore,
        private val systemWorkspaceTagSeed: SystemWorkspaceTagSeed,
        private val backlogMigrationDryRunAdapter: BacklogMigrationDryRunAdapter,
        private val legacyInboxFullBackupAdapter: LegacyInboxFullBackupAdapter,
        private val inboxSortingLegacyFullBackupAdapter: InboxSortingLegacyFullBackupAdapter,
        private val focusContextIntervalDao: FocusContextIntervalDao,
        private val userStateIntervalDao: UserStateIntervalDao,
        private val tagAssociationHandler: TagAssociationHandler,
    ) : FullBackupLocalDataSource {
        override suspend fun loadUnsyncedCanonicalOrientations(): CanonicalOrientationSyncPayload =
            canonicalOrientationSyncStore.loadUnsynced()

        override suspend fun loadUnsyncedCanonicalExecutionLogs() =
            canonicalExecutionLogSyncStore.loadUnsynced()

        override suspend fun loadCanonicalExecutionLogsChangedSince(timestamp: Long) =
            canonicalExecutionLogSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalExecutionLogsSynced(
            logs: List<CanonicalExecutionLogSyncVersion>,
        ) {
            canonicalExecutionLogSyncStore.markSynced(logs)
        }

        override suspend fun loadUnsyncedCanonicalWorkspaceDirectionEntries() =
            canonicalWorkspaceDirectionEntrySyncStore.loadUnsynced()

        override suspend fun loadCanonicalWorkspaceDirectionEntriesChangedSince(timestamp: Long) =
            canonicalWorkspaceDirectionEntrySyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalWorkspaceDirectionEntriesSynced(
            entries: List<WorkspaceDirectionEntrySyncVersion>,
        ) {
            canonicalWorkspaceDirectionEntrySyncStore.markSynced(entries)
        }

        override suspend fun loadUnsyncedCanonicalWorkspaceProblems() =
            canonicalWorkspaceProblemSyncStore.loadUnsynced()

        override suspend fun loadCanonicalWorkspaceProblemsChangedSince(timestamp: Long) =
            canonicalWorkspaceProblemSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalWorkspaceProblemsSynced(
            ack: CanonicalWorkspaceProblemSyncAck,
        ) {
            canonicalWorkspaceProblemSyncStore.markSynced(ack)
        }

        override suspend fun loadUnsyncedCanonicalWorkspaceInbox() =
            canonicalWorkspaceInboxSyncStore.loadUnsynced()

        override suspend fun loadCanonicalWorkspaceInboxChangedSince(timestamp: Long) =
            canonicalWorkspaceInboxSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalWorkspaceInboxSynced(
            records: List<WorkspaceInboxRecordSyncVersion>,
        ) {
            canonicalWorkspaceInboxSyncStore.markSynced(records)
        }

        override suspend fun loadUnsyncedCanonicalWorkspaceConnections() =
            canonicalWorkspaceConnectionSyncStore.loadUnsynced()

        override suspend fun loadCanonicalWorkspaceConnectionsChangedSince(timestamp: Long) =
            canonicalWorkspaceConnectionSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalWorkspaceConnectionsSynced(
            connections: List<WorkspaceConnectionSyncVersion>,
        ) {
            canonicalWorkspaceConnectionSyncStore.markSynced(connections)
        }

        override suspend fun loadUnsyncedCanonicalWorkspaceBacklog() =
            canonicalWorkspaceBacklogSyncStore.loadUnsynced()

        override suspend fun loadCanonicalWorkspaceBacklogChangedSince(timestamp: Long) =
            canonicalWorkspaceBacklogSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalWorkspaceBacklogSynced(
            entries: List<WorkspaceBacklogEntrySyncVersion>,
        ) {
            canonicalWorkspaceBacklogSyncStore.markSynced(entries)
        }

        override suspend fun markCanonicalOrientationsSynced(ack: CanonicalOrientationSyncAck) {
            canonicalOrientationSyncStore.markSynced(ack)
        }

        override suspend fun loadCanonicalWorkspaceTagsChangedSince(timestamp: Long) =
            canonicalWorkspaceTagTransportStore.loadChangedSince(timestamp)

        override suspend fun loadUnsyncedCanonicalDayThemes(): CanonicalDayThemeSyncPayload {
            canonicalDayThemeBootstrapper.ensureBootstrapped()
            return db.withTransaction {
                CanonicalDayThemeSyncPayload(
                    themeDefinitions =
                        canonicalDayThemeDao.getUnsyncedThemeDefinitionsForSync().map { it.toCanonicalSnapshot() },
                    dayThemes =
                        canonicalDayThemeDao.getUnsyncedDayThemesForSync().map { it.toCanonicalSnapshot() },
                    assignmentDocuments =
                        canonicalDayThemeDao.getUnsyncedAssignmentDocumentsForSync().map { it.toCanonicalSnapshot() },
                )
            }
        }

        override suspend fun loadCanonicalDayThemesChangedSince(timestamp: Long): CanonicalDayThemeSyncPayload {
            canonicalDayThemeBootstrapper.ensureBootstrapped()
            return db.withTransaction {
                CanonicalDayThemeSyncPayload(
                    themeDefinitions =
                        canonicalDayThemeDao.getThemeDefinitionsChangedSinceForSync(timestamp).map { it.toCanonicalSnapshot() },
                    dayThemes =
                        canonicalDayThemeDao.getDayThemesChangedSinceForSync(timestamp).map { it.toCanonicalSnapshot() },
                    assignmentDocuments =
                        canonicalDayThemeDao.getAssignmentDocumentsChangedSinceForSync(timestamp).map { it.toCanonicalSnapshot() },
                )
            }
        }

        override suspend fun markCanonicalDayThemesSynced(ack: CanonicalDayThemeSyncAck) {
            if (
                ack.themeDefinitions.isEmpty() &&
                ack.dayThemes.isEmpty() &&
                ack.assignmentDocuments.isEmpty()
            ) {
                return
            }

            val syncedAt = System.currentTimeMillis()
            db.withTransaction {
                ack.themeDefinitions.forEach { sent ->
                    canonicalDayThemeDao.markThemeDefinitionSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
                ack.dayThemes.forEach { sent ->
                    canonicalDayThemeDao.markDayThemeSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
                ack.assignmentDocuments.forEach { sent ->
                    canonicalDayThemeDao.markAssignmentDocumentSyncedIfVersionMatches(
                        dayPlanId = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
            }
        }

        override suspend fun loadUnsyncedCanonicalRecurringSeries() =
            canonicalRecurringSeriesDao.getUnsyncedForSync().map { it.toSnapshot() }

        override suspend fun loadCanonicalRecurringSeriesChangedSince(timestamp: Long) =
            canonicalRecurringSeriesDao.getChangedSinceForSync(timestamp).map { it.toSnapshot() }

        override suspend fun markCanonicalRecurringSeriesSynced(
            series: List<CanonicalRecurringSeriesSyncVersion>,
        ) {
            if (series.isEmpty()) return

            val syncedAt = System.currentTimeMillis()
            db.withTransaction {
                series.forEach { sent ->
                    canonicalRecurringSeriesDao.markSyncedIfVersionMatches(
                        seriesId = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
            }
        }

        override suspend fun loadFullSnapshotBundle(): SnapshotBundle {
            canonicalDayThemeBootstrapper.ensureBootstrapped()
            canonicalOrientationBootstrapper.ensureBootstrapped()
            canonicalWorkspaceBootstrapper.ensureBootstrapped()
            executionLogWorkspaceOwnershipBridge.repairUnresolved()

            val (canonicalThemeDefinitions, canonicalDayThemes, canonicalAssignmentDocuments) =
                db.withTransaction {
                    Triple(
                        canonicalDayThemeDao.getAllThemeDefinitionsSync().map { it.toCanonicalSnapshot() },
                        canonicalDayThemeDao.getAllDayThemesSync().map { it.toCanonicalSnapshot() },
                        canonicalDayThemeDao.getAllAssignmentDocumentsSync().map { it.toCanonicalSnapshot() },
                    )
                }

            val canonicalWorkspaceProblems = canonicalWorkspaceProblemSyncStore.loadAll()

            val retiredContextIds = db.canonicalRetiredContextIds()
            val contextsForTransport =
                contextDao
                    .getAllRaw()
                    .filterNot { context ->
                        !context.isDeleted &&
                            (
                                isReservedSystemContextId(context.id) ||
                                    context.id in retiredContextIds
                            )
                    }

            Log.d("SyncV2", "Starting export to SnapshotBundle V2")
            return SnapshotBundle(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                // Core & Structure
                contexts = contextsForTransport.map { it.toSnapshot() },
                contextParentLinks =
                    contextParentLinkDao
                        .getAllRaw()
                        .filterNot { link ->
                            link.parentContextId in retiredContextIds ||
                                link.childContextId in retiredContextIds
                        }
                        .map { it.toSnapshot() },
                goals = goalDao.getAllRaw().map { it.toSnapshot() },
                backlogItems = emptyList(),
                backlogOrders = emptyList(),
                inbox = emptyList(),
                logs = emptyList(),
                canonicalExecutionLogs = canonicalExecutionLogSyncStore.loadAll(),
                workspaceDirectionEntries = canonicalWorkspaceDirectionEntrySyncStore.loadAll(),
                workspaceProblems = canonicalWorkspaceProblems.problems,
                workspaceProblemWorkspaceRefs = canonicalWorkspaceProblems.workspaceRefs,
                workspaceProblemAttachmentRefs = canonicalWorkspaceProblems.attachmentRefs,
                workspaceInboxRecords = canonicalWorkspaceInboxSyncStore.loadAll(),
                workspaceConnections = canonicalWorkspaceConnectionSyncStore.loadAll(),
                workspaceBacklogEntries = canonicalWorkspaceBacklogSyncStore.loadAll(),
                workspaceTagRefs = canonicalWorkspaceTagTransportStore.loadAll(),
                // Knowledge Base
                documents = noteDocumentDao.getAllDocumentsRaw().map { it.toSnapshot() },
                musicNotes = musicNoteDao.getAll().map { it.toSnapshot() },
                notes = legacyNoteDao.getAllRaw().map { it.toSnapshot() },
                checklists = checklistDao.getAllChecklistsRaw().map { it.toSnapshot() },
                checklistItems = checklistDao.getAllChecklistItemsRaw().map { it.toSnapshot() },
                scripts = scriptDao.getAllRaw().map { it.toSnapshot() },
                attachments = attachmentDao.getAllRaw().map { it.toSnapshot() },
                // Activity & RPG
                activityRecords = activityRecordDao.getAllRaw().map { it.toSnapshot() },
                dayPlans = dayPlanDao.getAllPlansSync().map { it.toSnapshot() },
                dayFocusItems =
                    dayFocusItemDao.getAllSync().map { item ->
                        com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                            .dayFocusItemSnapshot(item, item.toSnapshot())
                    },
                dayTasks =
                    dayTaskDao.getAllTasksSync().map { task ->
                        com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                            .dayTaskSnapshot(task, task.toSnapshot())
                    },
                // Canonical Day Themes are authoritative in every new Android snapshot.
                // Legacy documents are intentionally not exported alongside canonical state.
                dayThemeDocuments = emptyList(),
                themeDefinitions = canonicalThemeDefinitions,
                dayThemes = canonicalDayThemes,
                dayThemeAssignmentDocuments = canonicalAssignmentDocuments,
                managedSubjects = db.orientationDao().getAllManagedSubjects(),
                orientations = db.orientationDao().getAllOrientations(),
                aspects = db.orientationDao().getAllAspects(),
                orientationAssessments = db.orientationDao().getAllAssessments(),
                orientationAssessmentRevisions = db.orientationDao().getAllAssessmentRevisions(),
                legacySubjectMappings = db.orientationDao().getAllLegacyMappings(),
                orientationRelations = db.orientationDao().getAllOrientationRelations(),
                aspectOrientationRefs = db.orientationDao().getAllAspectOrientationRefs(),
                workspaces = db.workspaceDao().getAll(),
                workspaceBindings = db.orientationDao().getAllWorkspaceBindings(),
                workspaceCapabilityInstances = db.orientationDao().getAllWorkspaceCapabilities(),
                savedOrientationViews = db.orientationDao().getAllSavedViews(),
                dailyMetrics = dailyMetricDao.getAll().map { it.toSnapshot() },
                recurringTasks = emptyList(),
                recurringSeries = canonicalRecurringSeriesDao.getAllSync().map { it.toSnapshot() },
                // AI Domain
                conversations = chatDao.getAllConversationsSync().map { it.toSnapshot() },
                chatMessages = chatDao.getAllMessagesSync().map { it.toSnapshot() },
                conversationFolders = conversationFolderDao.getAllSync().map { it.toSnapshot() },
                aiInsights = aiInsightDao.getAllSync().map { it.toSnapshot() },
                aiEvents = aiEventDao.getAllSync().map { it.toSnapshot() },
                mainBeacons = mainBeaconDao.getAllBeaconsSync().map { it.toSnapshot() },
                mainBeaconGroups = mainBeaconDao.getAllGroupsSync().map { it.toSnapshot() },
                mainBeaconGroupMembers = mainBeaconDao.getAllGroupMembersSync().map { it.toSnapshot() },
                mainBeaconParentLinks = mainBeaconDao.getAllParentLinksSync().map { it.toSnapshot() },
                mainBeaconContextCrossRefs = mainBeaconDao.getAllContextCrossRefsSync().map { it.toSnapshot() },
                mainBeaconAttachmentCrossRefs = mainBeaconDao.getAllAttachmentCrossRefsSync().map { it.toSnapshot() },
                mainBeaconLevelStatuses = mainBeaconDao.getAllLevelStatusesSync().map { it.toSnapshot() },
                lifeManagementLevelStatuses = lifeManagementLevelStatusDao.getAll().map { it.toSnapshot() },
                // System & Tactical
                tacticalMissions = tacticalMissionDao.getAllMissionsSync().map { it.toSnapshot() },
                tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentCrossRefs().map { it.toSnapshot() },
                tacticalIterations = tacticalIterationDao.getAllSync(),
                missionStreams = missionStreamDao.getAllSync(),
                tacticalActivitySlots = tacticalActivitySlotDao.getAllSync(),
                arcQuests = arcQuestDao.getAllSync(),
                reminders = reminderDao.getAllRemindersSync().map { it.toSnapshot() },
                systemApps = systemAppDao.getAllRaw().map { it.toSnapshot() },
                lifeSystemStates = lifeSystemStateDao.getAllSync().map { it.toSnapshot() },
                dayManagementRuntimeState = dayManagementRuntimeRepository.exportSnapshot(),
                recentProjectEntries = recentItemDao.getAllSync().map { it.toSnapshot() },
                linkItemEntities = linkItemDao.getAllRaw().map { it.toSnapshot() },
                // Configuration
                contextRoleProfiles = structurePresetDao.getAllSync().map { it.toSnapshot() },
                contextRoleProfileItems = structurePresetItemDao.getAllSync().map { it.toSnapshot() },
                // В ContextStructureDao є метод для отримання конфігів
                contextConfigurations =
                    contextStructureDao
                        .getAllSync()
                        .filterNot { configuration -> isReservedSystemContextId(configuration.contextId) }
                        .map { it.toSnapshot() },
                // В ContextStructureDao є метод для отримання айтемів
                projectStructureItems = contextStructureDao.getAllItemsSync().map { it.toSnapshot() },
                contextInboxSortingRules = emptyList(),
                focusContextIntervals = focusContextIntervalDao.getAllRaw().map { it.toSnapshot() },
                userStateIntervals = userStateIntervalDao.getAllRaw().map { it.toSnapshot() },
            )
        }

        private suspend fun insertDayThemePayload(bundle: SnapshotBundle) {
            val canonicalFieldCount =
                listOf(
                    bundle.themeDefinitions,
                    bundle.dayThemes,
                    bundle.dayThemeAssignmentDocuments,
                ).count { it != null }

            require(canonicalFieldCount == 0 || canonicalFieldCount == 3) {
                "Canonical Day Themes must contain either none or all canonical fields."
            }

            if (canonicalFieldCount == 0) {
                // Legacy 000 is accepted only as an input language. It is translated
                // immediately into canonical persistence and is never written back to
                // day_theme_documents.
                val localThemeDefinitions =
                    canonicalDayThemeDao.getAllThemeDefinitionsSync().map { it.toCanonicalSnapshot() }
                val localDayThemes =
                    canonicalDayThemeDao.getAllDayThemesSync().map { it.toCanonicalSnapshot() }
                val localAssignmentDocuments =
                    canonicalDayThemeDao.getAllAssignmentDocumentsSync().map { it.toCanonicalSnapshot() }
                val validPlanIds =
                    dayPlanDao.getAllPlansSync().mapTo(hashSetOf()) { it.id }

                val mergePlan =
                    planLegacyDayThemeMerge(
                        incomingLegacyDocuments = bundle.dayThemeDocuments,
                        incomingPlanIdRemap = emptyMap(),
                        validPlanIds = validPlanIds,
                        localThemeDefinitions = localThemeDefinitions,
                        localDayThemes = localDayThemes,
                        localAssignmentDocuments = localAssignmentDocuments,
                    )

                Log.d(
                    "SyncV2",
                    "Canonicalizing legacy DayThemeDocuments: legacy=${bundle.dayThemeDocuments.size}, " +
                        "definitions=${mergePlan.themeDefinitions.size}, " +
                        "dayThemes=${mergePlan.dayThemes.size}, " +
                        "assignments=${mergePlan.assignmentDocuments.size}",
                )

                if (mergePlan.themeDefinitions.isNotEmpty()) {
                    canonicalDayThemeDao.upsertThemeDefinitions(
                        mergePlan.themeDefinitions.map { it.toCanonicalEntity() },
                    )
                }
                if (mergePlan.dayThemes.isNotEmpty()) {
                    canonicalDayThemeDao.upsertDayThemes(
                        mergePlan.dayThemes.map { it.toCanonicalEntity() },
                    )
                }
                if (mergePlan.assignmentDocuments.isNotEmpty()) {
                    canonicalDayThemeDao.upsertAssignmentDocuments(
                        mergePlan.assignmentDocuments.map { it.toCanonicalEntity() },
                    )
                }

                canonicalDayThemeDao.upsertBootstrapState(
                    DayThemeCanonicalBootstrapStateEntity(
                        version = CanonicalDayThemeBootstrapper.CURRENT_BOOTSTRAP_VERSION,
                        completedAt = System.currentTimeMillis(),
                    ),
                )
                return
            }

            requireValidCanonicalDayThemePayload(bundle)

            val definitions = requireNotNull(bundle.themeDefinitions)
            val dayThemes = requireNotNull(bundle.dayThemes)
            val assignmentDocuments = requireNotNull(bundle.dayThemeAssignmentDocuments)

            Log.d(
                "SyncV2",
                "Inserting canonical Day Themes: definitions=${definitions.size}, " +
                    "dayThemes=${dayThemes.size}, assignments=${assignmentDocuments.size}",
            )

            if (definitions.isNotEmpty()) {
                canonicalDayThemeDao.upsertThemeDefinitions(definitions.map { it.toCanonicalEntity() })
            }
            if (dayThemes.isNotEmpty()) {
                canonicalDayThemeDao.upsertDayThemes(dayThemes.map { it.toCanonicalEntity() })
            }
            if (assignmentDocuments.isNotEmpty()) {
                canonicalDayThemeDao.upsertAssignmentDocuments(assignmentDocuments.map { it.toCanonicalEntity() })
            }

            canonicalDayThemeDao.upsertBootstrapState(
                DayThemeCanonicalBootstrapStateEntity(
                    version = CanonicalDayThemeBootstrapper.CURRENT_BOOTSTRAP_VERSION,
                    completedAt = System.currentTimeMillis(),
                ),
            )
        }

        private suspend fun insertWorkspaceOwnedSystemAppsForFullRestore(bundle: SnapshotBundle) {
            val validWorkspaceIds =
                db.workspaceDao().getAll().asSequence().filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.id }
            val validDocumentIds = bundle.documents.mapTo(hashSetOf()) { it.id }
            val systemAppsToInsert =
                bundle.systemApps.mapNotNull { app ->
                    app.toWorkspaceOwnedEntityOrNull(
                        liveWorkspaceIds = validWorkspaceIds,
                        validDocumentIds = validDocumentIds,
                        allowLegacyContextOwner = true,
                    ) ?: run {
                        Log.w(
                            "SyncV2",
                            "Skipping SystemApp ${app.id}: missing canonical live Workspace owner",
                        )
                        null
                    }
                }
            Log.d("SyncV2", "Inserting SystemApps: ${systemAppsToInsert.size}/${bundle.systemApps.size}")
            systemAppDao.insertAll(systemAppsToInsert)
        }

        private suspend fun insertBundleData(bundle: SnapshotBundle) {
            Log.d("SyncV2", "--- Starting data insertion from SnapshotBundle V2 ---")

            val contextIngress = partitionSystemContextSnapshotIngress(bundle.contexts)
            val retiredContextIds =
                db.canonicalRetiredContextIds(bundle.workspaces.orEmpty())
            val contextSnapshotsForPersistence =
                contextIngress.ordinarySnapshots.withoutLiveRetiredContexts(
                    retiredContextIds = retiredContextIds,
                    id = { it.id },
                    isDeleted = { it.isDeleted },
                )
            val contextConfigurations =
                bundle.contextConfigurations.map { snapshot ->
                    ContextConfiguration(
                        id = snapshot.id,
                        contextId = snapshot.contextId,
                        basePresetCode = snapshot.basePresetCode,
                        experimentalCapabilityIds = snapshot.experimentalCapabilityIds.orEmpty(),
                        applyMode = snapshot.applyMode,
                        enableInbox = snapshot.enableInbox,
                        enableLog = snapshot.enableLog,
                        enableAdvanced = snapshot.enableAdvanced,
                        enableDashboard = snapshot.enableDashboard,
                        enableBacklog = snapshot.enableBacklog,
                        enableAttachments = snapshot.enableAttachments,
                        enableAutoLinkSubprojects = snapshot.enableAutoLinkSubprojects,
                        removeInboxEntryAfterTagAutocopy = snapshot.removeInboxEntryAfterTagAutocopy ?: false,
                        removeBacklogEntryAfterTagAutocopy = snapshot.removeBacklogEntryAfterTagAutocopy ?: false,
                        version = snapshot.version,
                        updatedAt = snapshot.updatedAt,
                        isDeleted = snapshot.isDeleted,
                    )
                }
            val legacySystemConfigurationEvidence =
                contextConfigurations.filter { configuration ->
                    isReservedSystemContextId(configuration.contextId)
                }
            val ordinaryContextConfigurations =
                contextConfigurations.filterNot { configuration ->
                    isReservedSystemContextId(configuration.contextId)
                }

            // Insertion order is critical to avoid foreign key constraint violations.
            // Independent entities are inserted first.

            Log.d("SyncV2", "Inserting StructurePresets: ${bundle.contextRoleProfiles.size}")
            structurePresetDao.insertAll(bundle.contextRoleProfiles.map { it.toEntity() })

            Log.d("SyncV2", "Inserting ConversationFolders: ${bundle.conversationFolders.size}")
            conversationFolderDao.insertAll(bundle.conversationFolders.map { it.toEntity() })

            Log.d(
                "SyncV2",
                "Inserting ordinary Contexts: ${contextSnapshotsForPersistence.size}/" +
                    "${contextIngress.ordinarySnapshots.size}; " +
                    "reserved System evidence: ${contextIngress.systemEvidence.size}",
            )
            contextDao.insertAll(contextSnapshotsForPersistence.map { it.toEntity() })

            val ordinaryContextParentLinks =
                bundle.contextParentLinks.filterNot { link ->
                    isReservedSystemContextId(link.parentContextId) ||
                        isReservedSystemContextId(link.childContextId) ||
                        link.parentContextId in retiredContextIds ||
                        link.childContextId in retiredContextIds
                }
            Log.d(
                "SyncV2",
                "Inserting ContextParentLinks: ${ordinaryContextParentLinks.size}/${bundle.contextParentLinks.size}",
            )
            contextParentLinkDao.insertAll(ordinaryContextParentLinks.map { it.toEntity() })


            tacticalIterationDao.insertAll(bundle.tacticalIterations)
            missionStreamDao.insertAll(bundle.missionStreams)
            tacticalActivitySlotDao.insertAll(
                bundle.tacticalActivitySlots.filterNot { slot ->
                    isReservedSystemContextId(slot.contextId)
                },
            )
            arcQuestDao.insertAll(bundle.arcQuests)

            Log.d("SyncV2", "Inserting DayPlans: ${bundle.dayPlans.size}")
            dayPlanDao.insertPlans(bundle.dayPlans.map { it.toEntity() })
            insertDayThemePayload(bundle)

            Log.d("SyncV2", "Inserting DayFocusItems: ${bundle.dayFocusItems.size}")
            dayFocusItemDao.insertAll(
                bundle.dayFocusItems.map { snapshot ->
                    com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                        .dayFocusItemEntity(snapshot, snapshot.toEntity())
                },
            )

            Log.d("SyncV2", "Inserting Checklists: ${bundle.checklists.size}")
            checklistDao.insertChecklists(bundle.checklists.map { it.toEntity() })

            Log.d("SyncV2", "Inserting NoteDocuments: ${bundle.documents.size}")
            noteDocumentDao.insertAllDocuments(bundle.documents.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MusicNotes: ${bundle.musicNotes.size}")
            musicNoteDao.insertAll(bundle.musicNotes.map { it.toEntity() })

            val ordinaryLegacyNotes =
                bundle.notes.filterNot { note ->
                    isReservedSystemContextId(note.contextId)
                }
            Log.d(
                "SyncV2",
                "Inserting LegacyNotes: ${ordinaryLegacyNotes.size}/${bundle.notes.size}",
            )
            legacyNoteDao.insertAll(ordinaryLegacyNotes.map { it.toEntity() })

            Log.d("SyncV2", "Inserting Scripts: ${bundle.scripts.size}")
            scriptDao.insertAll(bundle.scripts.map { it.toEntity() })

            Log.d("SyncV2", "Inserting Attachments: ${bundle.attachments.size}")
            attachmentDao.insertAttachments(bundle.attachments.map { it.toEntity() })

            Log.d("SyncV2", "Inserting LifeSystemStates: ${bundle.lifeSystemStates.size}")
            lifeSystemStateDao.insertAll(bundle.lifeSystemStates.map { it.toEntity() })

            val legacyBacklogFallback =
                if (bundle.workspaceBacklogEntries == null &&
                    (bundle.backlogItems.isNotEmpty() || bundle.backlogOrders.isNotEmpty())
                ) {
                    normalizeLegacyStructuralContextBacklog(
                        backlogItems =
                            bundle.backlogItems
                                .filterNot { item -> isReservedSystemContextId(item.contextId) }
                                .map { it.toEntity() },
                        backlogOrders =
                            bundle.backlogOrders
                                .filterNot { order -> isReservedSystemContextId(order.listId) }
                                .map { it.toEntity() },
                        parentByContextId = contextDao.getAll().associate { it.id to it.parentId },
                        now = System.currentTimeMillis(),
                    )
                } else {
                    null
                }
            val transientSystemBacklogItems =
                if (bundle.workspaceBacklogEntries == null) {
                    bundle.backlogItems
                        .filter { item -> isReservedSystemContextId(item.contextId) }
                        .map { it.toEntity() }
                } else {
                    emptyList()
                }
            val transientSystemBacklogOrders =
                if (bundle.workspaceBacklogEntries == null) {
                    bundle.backlogOrders
                        .filter { order -> isReservedSystemContextId(order.listId) }
                        .map { it.toEntity() }
                } else {
                    emptyList()
                }
            if (bundle.workspaceBacklogEntries != null &&
                (bundle.backlogItems.isNotEmpty() || bundle.backlogOrders.isNotEmpty())
            ) {
                Log.d("SyncV2", "Ignoring legacy BACKLOG fields because canonical payload is present")
            }
            legacyBacklogFallback?.let { fallback ->
                Log.d("SyncV2", "Staging ${fallback.backlogItems.size} legacy BACKLOG rows for frozen planner")
                backlogItemDao.insertAll(fallback.backlogItems)
            }

            val legacyInboxFallback =
                bundle.inbox.takeIf {
                    bundle.workspaceInboxRecords == null && it.isNotEmpty()
                }
            if (bundle.workspaceInboxRecords != null && bundle.inbox.isNotEmpty()) {
                Log.d("SyncV2", "Ignoring legacy InboxRecords because canonical payload is present")
            }

            Log.d("SyncV2", "Inserting LinkItems: ${bundle.linkItemEntities.size}")
            linkItemDao.insertAll(bundle.linkItemEntities.map { it.toEntity() })

            Log.d("SyncV2", "Inserting AiInsights: ${bundle.aiInsights.size}")
            aiInsightDao.upsertAll(bundle.aiInsights.map { it.toEntity() })

            Log.d("SyncV2", "Inserting AiEvents: ${bundle.aiEvents.size}")
            aiEventDao.insertAll(bundle.aiEvents.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconGroups: ${bundle.mainBeaconGroups.size}")
            mainBeaconDao.insertGroups(bundle.mainBeaconGroups.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeacons: ${bundle.mainBeacons.size}")
            mainBeaconDao.insertBeacons(bundle.mainBeacons.map { it.toEntity() })

            Log.d("SyncV2", "Inserting LifeManagementLevelStatuses: ${bundle.lifeManagementLevelStatuses.size}")
            lifeManagementLevelStatusDao.upsertAll(bundle.lifeManagementLevelStatuses.map { it.toEntity() })

            // Level 2: Dependent entities
            Log.d("SyncV2", "Inserting Goals: ${bundle.goals.size}")
            goalDao.insertAll(bundle.goals.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting StructurePresetItems: ${bundle.contextRoleProfileItems.size}")
            structurePresetItemDao.insertAll(bundle.contextRoleProfileItems.map { it.toEntity() }) // Depends on ContextRoleProfile

            Log.d(
                "SyncV2",
                "Inserting ordinary ContextConfigurations: ${ordinaryContextConfigurations.size}/" +
                    "${contextConfigurations.size}; reserved System evidence: " +
                    legacySystemConfigurationEvidence.size,
            )
            contextStructureDao.insertAll(ordinaryContextConfigurations)

            Log.d("SyncV2", "Inserting ProjectStructureItems: ${bundle.projectStructureItems.size}")
            contextStructureDao.insertAllItems(
                bundle.projectStructureItems
                    .filterNot { item -> item.contextStructureId in legacySystemConfigurationEvidence.map { it.id } }
                    .map { it.toEntity() },
            ) // Depends on ContextConfiguration

            val legacyInboxSortingFallback =
                bundle.workspaceCapabilityInstances == null &&
                    bundle.contextInboxSortingRules.isNotEmpty()
            if (legacyInboxSortingFallback) {
                Log.d(
                    "SyncV2",
                    "Staging ${bundle.contextInboxSortingRules.size} legacy INBOX_SORTING rows for frozen planner",
                )
                contextInboxSortingDao.insertAll(bundle.contextInboxSortingRules.map { it.toEntity() })
            } else if (bundle.contextInboxSortingRules.isNotEmpty()) {
                Log.d(
                    "SyncV2",
                    "Ignoring legacy INBOX_SORTING fields because canonical capability payload is present",
                )
            }

            Log.d("SyncV2", "Inserting FocusContextIntervals: ${bundle.focusContextIntervals.size}")
            focusContextIntervalDao.insertAll(bundle.focusContextIntervals.map { it.toEntity() })

            Log.d("SyncV2", "Inserting UserStateIntervals: ${bundle.userStateIntervals.size}")
            userStateIntervalDao.insertAll(bundle.userStateIntervals.map { it.toEntity() })

            Log.d("SyncV2", "Inserting ActivityRecords: ${bundle.activityRecords.size}")
            activityRecordDao.insertAll(bundle.activityRecords.map { it.toEntity() }) // Depends on Context

            val validDayPlanIds = bundle.dayPlans.map { it.id }.toSet()
            val validGoalIds = bundle.goals.map { it.id }.toSet()
            val validActivityRecordIds = bundle.activityRecords.map { it.id }.toSet()

            val dayTasksToInsert =
                bundle.dayTasks.mapNotNull { taskSnapshot ->
                    // Перевірка наявності батьківського DayPlan
                    if (taskSnapshot.dayPlanId !in validDayPlanIds) {
                        Log.w(
                            "SyncData",
                            "DayTask ${taskSnapshot.id} references non-existent DayPlan ${taskSnapshot.dayPlanId}. Skipping this task.",
                        )
                        return@mapNotNull null
                    }

                    var sanitizedTask = taskSnapshot
                    // Перевірка та очищення Goal ID
                    if (sanitizedTask.goalId != null && sanitizedTask.goalId !in validGoalIds) {
                        Log.w(
                            "SyncData",
                            "DayTask ${sanitizedTask.id} references non-existent Goal ${sanitizedTask.goalId}. Setting goalId to null.",
                        )
                        sanitizedTask = sanitizedTask.copy(goalId = null)
                    }

                    if (sanitizedTask.activityRecordId != null && sanitizedTask.activityRecordId !in validActivityRecordIds) {
                        Log.w(
                            "SyncData",
                            "DayTask ${sanitizedTask.id} references non-existent ActivityRecord ${sanitizedTask.activityRecordId}. Setting activityRecordId to null.",
                        )
                        sanitizedTask = sanitizedTask.copy(activityRecordId = null)
                    }

                    sanitizedTask
                }

            Log.d("SyncV2", "Inserting ChecklistItems: ${bundle.checklistItems.size}")
            checklistDao.insertItems(bundle.checklistItems.map { it.toEntity() }) // Depends on Checklist

            Log.d("SyncV2", "Inserting Conversations: ${bundle.conversations.size}")
            chatDao.insertConversations(bundle.conversations.map { it.toEntity() }) // Depends on ConversationFolder

            Log.d("SyncV2", "Inserting ChatMessages: ${bundle.chatMessages.size}")
            chatDao.insertMessages(bundle.chatMessages.map { it.toEntity() }) // Depends on Conversation

            if (bundle.canonicalExecutionLogs == null) {
                Log.d(
                    "SyncV2",
                    "Importing legacy ContextLogs from pre-cutover backup: ${bundle.logs.size}",
                )
                contextLogDao.insertLogs(
                    bundle.logs
                        .filterNot { log -> isReservedSystemContextId(log.contextId) }
                        .map { it.toEntity() },
                ) // Depends on ordinary Context only
            } else {
                Log.d(
                    "SyncV2",
                    "Ignoring legacy ContextLogs because canonical EXECUTION_LOG authority is present: ${bundle.logs.size}",
                )
            }


            Log.d("SyncV2", "Inserting DailyMetrics: ${bundle.dailyMetrics.size}")
            dailyMetricDao.insertMetrics(bundle.dailyMetrics.map { it.toEntity() }) // Depends on DayPlan

            Log.d("SyncV2", "Inserting MainBeaconGroupMembers: ${bundle.mainBeaconGroupMembers.size}")
            mainBeaconDao.insertGroupMembers(bundle.mainBeaconGroupMembers.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconParentLinks: ${bundle.mainBeaconParentLinks.size}")
            mainBeaconDao.insertParentLinks(bundle.mainBeaconParentLinks.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconAttachmentCrossRefs: ${bundle.mainBeaconAttachmentCrossRefs.size}")
            mainBeaconDao.insertAttachmentCrossRefs(bundle.mainBeaconAttachmentCrossRefs.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconLevelStatuses: ${bundle.mainBeaconLevelStatuses.size}")
            mainBeaconDao.insertLevelStatuses(bundle.mainBeaconLevelStatuses.map { it.toEntity() })

            Log.d("SyncV2", "Inserting Reminders: ${bundle.reminders.size}")
            reminderDao.insertAll(bundle.reminders.map { it.toEntity() }) // Depends on Context

            legacyBacklogFallback?.let { fallback ->
                backlogOrderDao.insertAll(fallback.backlogOrders)
            }

            Log.d("SyncV2", "Inserting RecentProjectEntries: ${bundle.recentProjectEntries.size}")
            recentItemDao.insertAllSync(bundle.recentProjectEntries.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting canonical RecurringSeries: ${bundle.recurringSeries.size}")
            canonicalRecurringSeriesDao.insertAll(bundle.recurringSeries.map { it.toEntity() })

            Log.d("SyncV2", "--- Data insertion finished. Installing canonical payload. ---")
            // Canonical Workspace/capability transport is authoritative for a
            // current snapshot. Materialize it before downstream legacy System
            // Context compatibility convergence so legacy state cannot become
            // the prerequisite owner during restore.
            db.orientationDao().storeCanonicalPayload(bundle, merge = true, workspaceDao = db.workspaceDao())

            val contextsById = contextDao.getAllRaw().associateBy { it.id }
            val workspacesById = db.workspaceDao().getAll().associateBy { it.id }

            fun hasValidNonSystemOperationalProjectOwner(projectId: String): Boolean =
                runCatching {
                    classifyOperationalProjectOwner(
                        logicalProjectId = projectId,
                        context = contextsById[projectId],
                        workspace = workspacesById[projectId],
                    )
                }.isSuccess

            val routedDayTasksToInsert =
                dayTasksToInsert.map { snapshot ->
                    val projectId = snapshot.projectId
                    if (
                        projectId != null &&
                        !SystemContexts.isSystem(ContextId(projectId)) &&
                        !hasValidNonSystemOperationalProjectOwner(projectId)
                    ) {
                        Log.w(
                            "SyncData",
                            "DayTask ${snapshot.id} references an invalid operational owner $projectId. " +
                                "Setting projectId to null.",
                        )
                        snapshot.copy(projectId = null)
                    } else {
                        snapshot
                    }
                }

            Log.d(
                "SyncV2",
                "Inserting DayTasks: ${routedDayTasksToInsert.size} after Workspace routing",
            )
            dayTaskDao.insertTasks(
                routedDayTasksToInsert.map { snapshot ->
                    com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                        .dayTaskEntity(snapshot, snapshot.toEntity())
                },
            )

            // TacticalMission keeps one logical primary-project id with typed
            // Context / Workspace persistence branches. Canonical payload and
            // Context retirement evidence now exist before owner routing.
            val missionsToInsert =
                bundle.tacticalMissions.map { missionSnapshot ->
                    val projectId = missionSnapshot.projectId
                    if (
                        projectId != null &&
                        !SystemContexts.isSystem(ContextId(projectId)) &&
                        !hasValidNonSystemOperationalProjectOwner(projectId)
                    ) {
                        Log.w(
                            "SyncData",
                            "TacticalMission ${missionSnapshot.id} references an invalid operational owner $projectId. " +
                                "Setting projectId to null.",
                        )
                        missionSnapshot.copy(projectId = null)
                    } else {
                        missionSnapshot
                    }
                }.map { it.toEntity() }

            Log.d("SyncV2", "Inserting TacticalMissions: ${missionsToInsert.size}")
            tacticalMissionDao.insertMissions(missionsToInsert)

            Log.d("SyncV2", "Inserting TacticalMissionAttachments: ${bundle.tacticalMissionAttachments.size}")
            tacticalMissionDao.insertMissionAttachments(
                bundle.tacticalMissionAttachments.map { it.toEntity() },
            )

            // Exact reserved Main Beacon owners are now Workspace-backed. The
            // canonical payload must therefore exist before the historical
            // mainBeaconContextCrossRefs wire shape is routed into typed refs.
            Log.d("SyncV2", "Inserting MainBeaconContextCrossRefs: ${bundle.mainBeaconContextCrossRefs.size}")
            mainBeaconDao.insertContextCrossRefs(bundle.mainBeaconContextCrossRefs.map { it.toEntity() })

            canonicalWorkspaceTagTransportStore.mergeIncoming(bundle.workspaceTagRefs)
            if (bundle.workspaceTagRefs != null) {
                systemWorkspaceTagSeed.markImportedCanonicalCollections(
                    workspaceIds =
                        bundle.workspaces.orEmpty().map { it.id } +
                            bundle.workspaceTagRefs.orEmpty().map { it.workspaceId },
                )
            }
            databaseInitializer.ensureCanonicalSystemWorkspaceOwnership(
                legacyContextEvidence = contextIngress.systemEvidence,
            )
            if (bundle.workspaceTagRefs == null) {
                systemWorkspaceTagSeed.ingestLegacySystemTagProjection(
                    legacyTagsByWorkspaceId =
                        bundle.contexts.associate { context ->
                            context.id to context.tags.orEmpty()
                        },
                )
            }
            if (bundle.workspaceCapabilityInstances == null) {
                // Explicit bounded ingress for a pre-canonical backup. At this
                // point System ownership convergence is complete, so only
                // genuinely missing promoted-System instances may be seeded.
                canonicalWorkspaceBootstrapper.ingestLegacySystemCapabilityProjection(
                    legacyContextEvidence = contextIngress.systemEvidence,
                    legacyConfigurationEvidence = legacySystemConfigurationEvidence,
                )
            }
            if (legacyBacklogFallback != null || transientSystemBacklogItems.isNotEmpty()) {
                canonicalOrientationBootstrapper.ensureBootstrapped()
                canonicalWorkspaceBootstrapper.ensureBootstrapped()
                backlogMigrationDryRunAdapter.materializeLegacyFullBackup(
                    transientItems = transientSystemBacklogItems,
                    transientOrders = transientSystemBacklogOrders,
                )
            }
            if (legacyInboxFallback != null) {
                canonicalOrientationBootstrapper.ensureBootstrapped()
                canonicalWorkspaceBootstrapper.ensureBootstrapped()
                legacyInboxFullBackupAdapter.materializeLegacyFullBackup(
                    legacyInboxFallback.map { it.toEntity() },
                )
            }
            if (legacyInboxSortingFallback) {
                canonicalOrientationBootstrapper.ensureBootstrapped()
                canonicalWorkspaceBootstrapper.ensureBootstrapped()
                inboxSortingLegacyFullBackupAdapter.materializeStagedEvidence()
            }
            canonicalWorkspaceDirectionEntrySyncStore.mergeIncoming(bundle.workspaceDirectionEntries)
            bundle.toCanonicalWorkspaceProblemSyncPayloadOrNull()?.let {
                canonicalWorkspaceProblemSyncStore.mergeIncoming(it)
            }
            canonicalExecutionLogSyncStore.mergeIncoming(bundle.canonicalExecutionLogs)
            canonicalWorkspaceInboxSyncStore.mergeIncoming(bundle.workspaceInboxRecords)
            canonicalWorkspaceConnectionSyncStore.mergeIncoming(bundle.workspaceConnections)
            canonicalWorkspaceBacklogSyncStore.mergeIncoming(bundle.workspaceBacklogEntries)
        }

        override suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
            check(bundle.recurringTasks.isEmpty()) {
                "Legacy recurrence-v1 recurringTasks payload is not supported by canonical backup restore"
            }
            check(
                bundle.dayTasks.none { task ->
                    task.recurringTaskId != null ||
                        task.nextOccurrenceTime != null ||
                        task.id.startsWith("recurring-task-instance-") ||
                        (task.id.startsWith("recurrence:TASK:") && task.recurrence == null)
                },
            ) {
                "Legacy recurrence-v1 DayTask payload is not supported by canonical backup restore"
            }

            contextWorkspaceWriteThrough.mutateAndAfterWorkspaceRefresh(
                mutation = {
                    Log.d("SyncV2", "Applying bundle V${bundle.version} in Merge Mode")
                    insertBundleData(bundle)
                },
                afterRefresh = {
                    insertWorkspaceOwnedSystemAppsForFullRestore(bundle)
                },
            )
            executionLogWorkspaceOwnershipBridge.repairUnresolved()
            canonicalOrientationBootstrapper.ensureBootstrapped()
            // Hashtag association links are rebuildable projections, never backup authority.
            tagAssociationHandler.repairAllAssociations()
            bundle.dayManagementRuntimeState?.let { runtimeState ->
                dayManagementRuntimeRepository.importSnapshot(runtimeState)
            }
        }

        override suspend fun clearAllTables() {
            Log.w("Sync", "Clearing all database tables!")
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
