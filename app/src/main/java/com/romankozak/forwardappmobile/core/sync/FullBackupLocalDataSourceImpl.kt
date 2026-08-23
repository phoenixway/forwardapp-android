// File: FullBackupLocalDataSourceImpl.kt

@file:Suppress("WildcardImport", "MaxLineLength", "UnusedPrivateProperty")

package com.romankozak.forwardappmobile.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.DayThemeCanonicalBootstrapStateEntity
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.daythemes.planLegacyDayThemeMerge
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
        private val listItemDao: ListItemDao,
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
        private val systemContextEnsurer: SystemContextEnsurer,
        private val backlogOrderDao: BacklogOrderDao,
        private val backlogItemDao: ListItemDao,
        private val contextArtifactDao: ContextArtifactDao,
        private val contextLogDao: ContextManagementDao,
        private val scriptDao: ScriptDao,
        private val inboxRecordDao: InboxRecordDao,
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
        private val directionDao: DirectionDao,
        private val contextInboxSortingDao: ContextInboxSortingDao,
        private val contextKeyProblemsDao: ContextKeyProblemsDao,
        private val focusContextIntervalDao: FocusContextIntervalDao,
        private val userStateIntervalDao: UserStateIntervalDao,
    ) : FullBackupLocalDataSource {
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

            val (canonicalThemeDefinitions, canonicalDayThemes, canonicalAssignmentDocuments) =
                db.withTransaction {
                    Triple(
                        canonicalDayThemeDao.getAllThemeDefinitionsSync().map { it.toCanonicalSnapshot() },
                        canonicalDayThemeDao.getAllDayThemesSync().map { it.toCanonicalSnapshot() },
                        canonicalDayThemeDao.getAllAssignmentDocumentsSync().map { it.toCanonicalSnapshot() },
                    )
                }

            Log.d("SyncV2", "Starting export to SnapshotBundle V2")
            return SnapshotBundle(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                // Core & Structure
                contexts = contextDao.getAllRaw().map { it.toSnapshot() },
                contextParentLinks = contextParentLinkDao.getAllRaw().map { it.toSnapshot() },
                goals = goalDao.getAllRaw().map { it.toSnapshot() },
                backlogItems = backlogItemDao.getAllRaw().map { it.toSnapshot() },
                backlogOrders = backlogOrderDao.getAllRaw().map { it.toSnapshot() },
                directionItems = directionDao.getAllRaw().map { it.toSnapshot() },
                inbox = inboxRecordDao.getAllRaw().map { it.toSnapshot() },
                logs = contextLogDao.getAllLogs().map { it.toSnapshot() },
                artifacts = contextArtifactDao.getAllRaw().map { it.toSnapshot() },
                // Knowledge Base
                documents = noteDocumentDao.getAllDocumentsRaw().map { it.toSnapshot() },
                musicNotes = musicNoteDao.getAll().map { it.toSnapshot() },
                notes = legacyNoteDao.getAllRaw().map { it.toSnapshot() },
                checklists = checklistDao.getAllChecklistsRaw().map { it.toSnapshot() },
                checklistItems = checklistDao.getAllChecklistItemsRaw().map { it.toSnapshot() },
                scripts = scriptDao.getAllRaw().map { it.toSnapshot() },
                attachments = attachmentDao.getAllRaw().map { it.toSnapshot() },
                crossRefs = attachmentDao.getAllContextAttachmentCrossRefsRaw().map { it.toSnapshot() },
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
                contextConfigurations = contextStructureDao.getAllSync().map { it.toSnapshot() },
                // В ContextStructureDao є метод для отримання айтемів
                projectStructureItems = contextStructureDao.getAllItemsSync().map { it.toSnapshot() },
                contextInboxSortingRules = contextInboxSortingDao.getAllRaw().map { it.toSnapshot() },
                contextKeyProblems = contextKeyProblemsDao.getAllRaw().map { it.toSnapshot() },
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

        private suspend fun insertBundleData(bundle: SnapshotBundle) {
            Log.d("SyncV2", "--- Starting data insertion from SnapshotBundle V2 ---")

            // Insertion order is critical to avoid foreign key constraint violations.
            // Independent entities are inserted first.

            Log.d("SyncV2", "Inserting StructurePresets: ${bundle.contextRoleProfiles.size}")
            structurePresetDao.insertAll(bundle.contextRoleProfiles.map { it.toEntity() })

            Log.d("SyncV2", "Inserting ConversationFolders: ${bundle.conversationFolders.size}")
            conversationFolderDao.insertAll(bundle.conversationFolders.map { it.toEntity() })

            Log.d("SyncV2", "Inserting Contexts: ${bundle.contexts.size}")
            contextDao.insertAll(bundle.contexts.map { it.toEntity() })

            Log.d("SyncV2", "Inserting ContextParentLinks: ${bundle.contextParentLinks.size}")
            contextParentLinkDao.insertAll(bundle.contextParentLinks.map { it.toEntity() })

            Log.d("SyncV2", "Inserting DirectionItems: ${bundle.directionItems.size}")
            directionDao.insertAll(bundle.directionItems.map { it.toEntity() })

            val validContextIds = bundle.contexts.map { it.id }.toSet()
            val missionsToInsert =
                bundle.tacticalMissions.map { missionSnapshot ->
                    if (missionSnapshot.projectId != null && missionSnapshot.projectId !in validContextIds) {
                        Log.w(
                            "SyncData",
                            "TacticalMission ${missionSnapshot.id} references non-existent Context ${missionSnapshot.projectId}. Setting projectId to null.",
                        )
                        missionSnapshot.copy(projectId = null)
                    } else {
                        missionSnapshot
                    }
                }.map { it.toEntity() }
            Log.d("SyncV2", "Inserting TacticalMissions: ${missionsToInsert.size}")
            tacticalMissionDao.insertMissions(missionsToInsert)
            tacticalIterationDao.insertAll(bundle.tacticalIterations)
            missionStreamDao.insertAll(bundle.missionStreams)
            tacticalActivitySlotDao.insertAll(bundle.tacticalActivitySlots)
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

            val validDocumentIds = bundle.documents.map { it.id }.toSet()
            val systemAppsToInsert =
                bundle.systemApps.mapNotNull { app ->
                    when {
                        app.contextId !in validContextIds -> {
                            Log.w(
                                "SyncV2",
                                "Skipping SystemApp ${app.id}: missing context ${app.contextId}",
                            )
                            null
                        }
                        app.noteDocumentId != null && app.noteDocumentId !in validDocumentIds -> {
                            Log.w(
                                "SyncV2",
                                "SystemApp ${app.id} references missing document ${app.noteDocumentId}. Clearing noteDocumentId.",
                            )
                            app.copy(noteDocumentId = null)
                        }
                        else -> app
                    }
                }
            Log.d("SyncV2", "Inserting SystemApps: ${systemAppsToInsert.size}/${bundle.systemApps.size}")
            systemAppDao.insertAll(systemAppsToInsert.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MusicNotes: ${bundle.musicNotes.size}")
            musicNoteDao.insertAll(bundle.musicNotes.map { it.toEntity() })

            Log.d("SyncV2", "Inserting LegacyNotes: ${bundle.notes.size}")
            legacyNoteDao.insertAll(bundle.notes.map { it.toEntity() })

            Log.d("SyncV2", "Inserting Scripts: ${bundle.scripts.size}")
            scriptDao.insertAll(bundle.scripts.map { it.toEntity() })

            Log.d("SyncV2", "Inserting Attachments: ${bundle.attachments.size}")
            attachmentDao.insertAttachments(bundle.attachments.map { it.toEntity() })

            Log.d("SyncV2", "Inserting LifeSystemStates: ${bundle.lifeSystemStates.size}")
            lifeSystemStateDao.insertAll(bundle.lifeSystemStates.map { it.toEntity() })

            Log.d("SyncV2", "Inserting BacklogItems: ${bundle.backlogItems.size}")
            backlogItemDao.insertAll(bundle.backlogItems.map { it.toEntity() })

            Log.d("SyncV2", "Inserting InboxRecords: ${bundle.inbox.size}")
            inboxRecordDao.insertAll(bundle.inbox.map { it.toEntity() })

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

            Log.d("SyncV2", "Inserting ContextConfigurations: ${bundle.contextConfigurations.size}")
            contextStructureDao.insertAll(
                bundle.contextConfigurations.map { snapshot ->
                    ContextConfiguration(
                        id = snapshot.id,
                        contextId = snapshot.contextId,
                        basePresetCode = snapshot.basePresetCode,
                        experimentalCapabilityIds = snapshot.experimentalCapabilityIds.orEmpty(),
                        applyMode = snapshot.applyMode,
                        enableInbox = snapshot.enableInbox,
                        enableLog = snapshot.enableLog,
                        enableArtifact = snapshot.enableArtifact,
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
                },
            ) // Depends on Context

            Log.d("SyncV2", "Inserting ProjectStructureItems: ${bundle.projectStructureItems.size}")
            contextStructureDao.insertAllItems(bundle.projectStructureItems.map { it.toEntity() }) // Depends on ContextConfiguration

            Log.d("SyncV2", "Inserting ContextInboxSorting: ${bundle.contextInboxSortingRules.size}")
            contextInboxSortingDao.insertAll(bundle.contextInboxSortingRules.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting ContextKeyProblems: ${bundle.contextKeyProblems.size}")
            contextKeyProblemsDao.insertAll(bundle.contextKeyProblems.map { it.toEntity() }) // Depends on Context

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

                    // Перевірка та очищення Project ID
                    if (sanitizedTask.projectId != null && sanitizedTask.projectId !in validContextIds) {
                        Log.w(
                            "SyncData",
                            "DayTask ${sanitizedTask.id} references non-existent Context ${sanitizedTask.projectId}. Setting projectId to null.",
                        )
                        sanitizedTask = sanitizedTask.copy(projectId = null)
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

            Log.d("SyncV2", "Inserting DayTasks: ${dayTasksToInsert.size} (after filtering)")
            dayTaskDao.insertTasks(
                dayTasksToInsert.map { snapshot ->
                    com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                        .dayTaskEntity(snapshot, snapshot.toEntity())
                },
            ) // Depends on DayPlan

            Log.d("SyncV2", "Inserting ChecklistItems: ${bundle.checklistItems.size}")
            checklistDao.insertItems(bundle.checklistItems.map { it.toEntity() }) // Depends on Checklist

            Log.d("SyncV2", "Inserting Conversations: ${bundle.conversations.size}")
            chatDao.insertConversations(bundle.conversations.map { it.toEntity() }) // Depends on ConversationFolder

            Log.d("SyncV2", "Inserting ChatMessages: ${bundle.chatMessages.size}")
            chatDao.insertMessages(bundle.chatMessages.map { it.toEntity() }) // Depends on Conversation

            Log.d("SyncV2", "Inserting ContextLogs: ${bundle.logs.size}")
            contextLogDao.insertLogs(bundle.logs.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting ContextArtifacts: ${bundle.artifacts.size}")
            contextArtifactDao.insertAll(bundle.artifacts.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting DailyMetrics: ${bundle.dailyMetrics.size}")
            dailyMetricDao.insertMetrics(bundle.dailyMetrics.map { it.toEntity() }) // Depends on DayPlan

            Log.d("SyncV2", "Inserting ContextAttachmentCrossRefs: ${bundle.crossRefs.size}")
            attachmentDao.insertContextAttachmentCrossRefs(bundle.crossRefs.map { it.toEntity() }) // Depends on Context and Attachment

            Log.d("SyncV2", "Inserting MainBeaconContextCrossRefs: ${bundle.mainBeaconContextCrossRefs.size}")
            mainBeaconDao.insertContextCrossRefs(bundle.mainBeaconContextCrossRefs.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconGroupMembers: ${bundle.mainBeaconGroupMembers.size}")
            mainBeaconDao.insertGroupMembers(bundle.mainBeaconGroupMembers.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconParentLinks: ${bundle.mainBeaconParentLinks.size}")
            mainBeaconDao.insertParentLinks(bundle.mainBeaconParentLinks.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconAttachmentCrossRefs: ${bundle.mainBeaconAttachmentCrossRefs.size}")
            mainBeaconDao.insertAttachmentCrossRefs(bundle.mainBeaconAttachmentCrossRefs.map { it.toEntity() })

            Log.d("SyncV2", "Inserting MainBeaconLevelStatuses: ${bundle.mainBeaconLevelStatuses.size}")
            mainBeaconDao.insertLevelStatuses(bundle.mainBeaconLevelStatuses.map { it.toEntity() })

            Log.d("SyncV2", "Inserting TacticalMissionAttachments: ${bundle.tacticalMissionAttachments.size}")
            tacticalMissionDao.insertMissionAttachments(
                bundle.tacticalMissionAttachments.map {
                    it.toEntity()
                },
            ) // Depends on TacticalMission and Attachment

            Log.d("SyncV2", "Inserting Reminders: ${bundle.reminders.size}")
            reminderDao.insertAll(bundle.reminders.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting BacklogOrders: ${bundle.backlogOrders.size}")
            backlogOrderDao.insertAll(bundle.backlogOrders.map { it.toEntity() }) // Depends on BacklogItem

            Log.d("SyncV2", "Inserting RecentProjectEntries: ${bundle.recentProjectEntries.size}")
            recentItemDao.insertAllSync(bundle.recentProjectEntries.map { it.toEntity() }) // Depends on Context

            Log.d("SyncV2", "Inserting canonical RecurringSeries: ${bundle.recurringSeries.size}")
            canonicalRecurringSeriesDao.insertAll(bundle.recurringSeries.map { it.toEntity() })

            Log.d("SyncV2", "--- Data insertion finished. Ensuring system contexts. ---")
            // Ensure system contexts exist after all other data is inserted.
            systemContextEnsurer.ensureAllSystemContextsExist()
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

            db.withTransaction {
                Log.d("SyncV2", "Applying bundle V${bundle.version} in Merge Mode")
                insertBundleData(bundle)
            }
            bundle.dayManagementRuntimeState?.let { runtimeState ->
                dayManagementRuntimeRepository.importSnapshot(runtimeState)
            }
        }

        override suspend fun loadFullDatabaseContent(): DatabaseContent {
            return DatabaseContent(
                projects = contextDao.getAll(),
                contextParentLinks = contextParentLinkDao.getAllRaw(),
                goals = goalDao.getAll(),
                backlogItems = backlogItemDao.getAllRaw(),
                backlogOrders = backlogOrderDao.getAllRaw(),
                legacyNotes = legacyNoteDao.getAllRaw(),
                documents = noteDocumentDao.getAllDocuments(),
                musicNotes = musicNoteDao.getAll(),
                checklists = checklistDao.getAllChecklistsRaw(),
                checklistItems = checklistDao.getAllChecklistItemsRaw(),
                scripts = scriptDao.getAllRaw(),
                attachments = attachmentDao.getAllRaw(),
                contextAttachmentCrossRefs = attachmentDao.getAllContextAttachmentCrossRefsRaw(),
                directionItems = directionDao.getAllRaw(),
                activityRecords = activityRecordDao.getAllRaw(),
                inboxRecords = inboxRecordDao.getAllRaw(),
                contextLogs = contextLogDao.getAllLogs(),
                recentProjectEntries =
                    recentItemDao.getAllSync().map {
                        com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry(
                            contextId = it.target,
                            timestamp = it.lastAccessed,
                        )
                    },
                linkItemEntities = linkItemDao.getAllRaw(),
                dayPlans = dayPlanDao.getAllPlansSync(),
                dayFocusItems = dayFocusItemDao.getAllSync(),
                dayTasks = dayTaskDao.getAllTasksSync(),
                // DatabaseContent is a legacy carrier and cannot represent canonical
                // Day Themes. Never export stale quarantine rows as current state.
                dayThemeDocuments = emptyList(),
                dailyMetrics = dailyMetricDao.getAll(),
                conversations = chatDao.getAllConversationsSync(),
                chatMessages = chatDao.getAllMessagesSync(),
                conversationFolders = conversationFolderDao.getAllSync(),
                reminders = reminderDao.getAllRemindersSync(),
                recurringTasks = emptyList(),
                systemApps = systemAppDao.getAllRaw(),
                contextArtifacts = contextArtifactDao.getAllRaw(),
                tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
                tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentCrossRefs(),
                tacticalIterations = tacticalIterationDao.getAllSync(),
                missionStreams = missionStreamDao.getAllSync(),
                tacticalActivitySlots = tacticalActivitySlotDao.getAllSync(),
                arcQuests = arcQuestDao.getAllSync(),
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

        override suspend fun restoreDatabaseFromBackup(content: DatabaseContent) {
            val snapshotBundle = SyncMapper.migrateV1ToV2(content)
            // Clear tables outside of transaction to prevent nesting issues
            clearAllTables()
            db.withTransaction {
                Log.d("SyncV1", "Migrating Legacy V1 to Snapshot V2")
                insertBundleData(snapshotBundle)
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
