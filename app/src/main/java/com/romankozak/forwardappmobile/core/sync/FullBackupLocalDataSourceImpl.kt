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

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationSyncStore
import com.romankozak.forwardappmobile.data.orientation.storeCanonicalPayload
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementGroupScopeSyncStore
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementLinkedAppearanceSyncStore
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceDirectionEntrySyncStore
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogSyncStore
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
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
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSyncVersion
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
        private val canonicalWorkspaceProblemSyncStore: CanonicalWorkspaceProblemSyncStore,
        private val canonicalWorkspaceInboxSyncStore: CanonicalWorkspaceInboxSyncStore,
        private val canonicalWorkspaceConnectionSyncStore: CanonicalWorkspaceConnectionSyncStore,
        private val canonicalWorkspaceBacklogSyncStore: CanonicalWorkspaceBacklogSyncStore,
        private val canonicalWorkspaceTagTransportStore: CanonicalWorkspaceTagTransportStore,
        private val systemWorkspaceTagSeed: SystemWorkspaceTagSeed,
        private val focusContextIntervalDao: FocusContextIntervalDao,
        private val userStateIntervalDao: UserStateIntervalDao,
    ) : FullBackupLocalDataSource {
        private val canonicalHierarchyPlacementSyncStore =
            CanonicalHierarchyPlacementSyncStore(db)
        private val canonicalHierarchyPlacementGroupScopeSyncStore =
            CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        private val canonicalHierarchyPlacementLinkedAppearanceSyncStore =
            CanonicalHierarchyPlacementLinkedAppearanceSyncStore(db)

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

        override suspend fun loadUnsyncedCanonicalHierarchyPlacements() =
            canonicalHierarchyPlacementSyncStore.loadUnsynced()

        override suspend fun loadCanonicalHierarchyPlacementsChangedSince(timestamp: Long) =
            canonicalHierarchyPlacementSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalHierarchyPlacementsSynced(
            entries: List<HierarchyPlacementSyncVersion>,
        ) {
            canonicalHierarchyPlacementSyncStore.markSynced(entries)
        }

        override suspend fun loadUnsyncedCanonicalHierarchyPlacementGroupScopes() =
            canonicalHierarchyPlacementGroupScopeSyncStore.loadUnsynced()

        override suspend fun loadCanonicalHierarchyPlacementGroupScopesChangedSince(
            timestamp: Long,
        ) = canonicalHierarchyPlacementGroupScopeSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalHierarchyPlacementGroupScopesSynced(
            entries: List<HierarchyPlacementGroupScopeSyncVersion>,
        ) {
            canonicalHierarchyPlacementGroupScopeSyncStore.markSynced(entries)
        }

        override suspend fun loadUnsyncedCanonicalHierarchyPlacementLinkedAppearances() =
            canonicalHierarchyPlacementLinkedAppearanceSyncStore.loadUnsynced()

        override suspend fun loadCanonicalHierarchyPlacementLinkedAppearancesChangedSince(
            timestamp: Long,
        ) = canonicalHierarchyPlacementLinkedAppearanceSyncStore.loadChangedSince(timestamp)

        override suspend fun markCanonicalHierarchyPlacementLinkedAppearancesSynced(
            entries: List<HierarchyPlacementLinkedAppearanceSyncVersion>,
        ) {
            canonicalHierarchyPlacementLinkedAppearanceSyncStore.markSynced(entries)
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
                hierarchyPlacements = canonicalHierarchyPlacementSyncStore.loadAll(),
                hierarchyPlacementGroupScopes =
                    canonicalHierarchyPlacementGroupScopeSyncStore.loadAll(),
                hierarchyPlacementLinkedAppearances =
                    canonicalHierarchyPlacementLinkedAppearanceSyncStore.loadAll(),
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
