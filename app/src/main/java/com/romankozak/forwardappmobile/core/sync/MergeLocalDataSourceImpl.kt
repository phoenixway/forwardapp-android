@file:Suppress("WildcardImport", "MaxLineLength")

package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.data.logic.InboxAssociationCache
import com.romankozak.forwardappmobile.core.context.normalizeLegacyStructuralContextBacklog

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.sync.ChangeType
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalOrientationPayload
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.daythemes.planCanonicalDayThemeMerge
import com.romankozak.forwardappmobile.data.daythemes.planLegacyDayThemeMerge
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.storeCanonicalPayload
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceDirectionEntrySyncStore
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDirectionEntryShadowMaterializer
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogSyncStore
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestDao
import com.romankozak.forwardappmobile.features.missions.data.*
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeLocalDataSourceImpl
    @Inject
    constructor(
        private val db: AppDatabase,
        private val contextDao: ContextDao,
        private val contextParentLinkDao: ContextParentLinkDao,
        private val goalDao: GoalDao,
        private val listItemDao: ListItemDao,
        private val attachmentDao: AttachmentDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val musicNoteDao: MusicNoteDao,
        private val chatDao: ChatDao,
        private val dayPlanDao: DayPlanDao,
        private val dayTaskDao: DayTaskDao,
        private val canonicalDayThemeDao: CanonicalDayThemeDao,
        private val canonicalDayThemeBootstrapper: CanonicalDayThemeBootstrapper,
        private val canonicalOrientationBootstrapper: CanonicalOrientationBootstrapper,
        private val contextWorkspaceWriteThrough: ContextWorkspaceWriteThrough,
        private val executionLogWorkspaceOwnershipBridge: ExecutionLogWorkspaceOwnershipBridge,
        private val canonicalExecutionLogSyncStore: CanonicalExecutionLogSyncStore,
        private val canonicalWorkspaceDirectionEntrySyncStore: CanonicalWorkspaceDirectionEntrySyncStore,
        private val workspaceDirectionEntryShadowMaterializer: WorkspaceDirectionEntryShadowMaterializer,
        private val dailyMetricDao: DailyMetricDao,
        private val reminderDao: ReminderDao,
        private val tacticalMissionDao: TacticalMissionDao,
        private val tacticalIterationDao: TacticalIterationDao,
        private val missionStreamDao: MissionStreamDao,
        private val tacticalActivitySlotDao: TacticalActivitySlotDao,
        private val arcQuestDao: ArcQuestDao,
        private val aiInsightDao: AiInsightDao,
        private val dayFocusItemDao: DayFocusItemDao,
        private val checklistDao: ChecklistDao,
        private val conversationFolderDao: ConversationFolderDao,
        private val canonicalRecurringSeriesDao: CanonicalRecurringSeriesDao,
        private val backlogOrderDao: BacklogOrderDao,
        private val legacyNoteDao: LegacyNoteDao,
        private val contextArtifactDao: ContextArtifactDao,
        private val scriptDao: ScriptDao,
        private val inboxRecordDao: InboxRecordDao,
        private val contextManagementDao: ContextManagementDao,
        private val systemAppDao: SystemAppDao,
        private val activityRecordDao: ActivityRecordDao,
        private val recentItemDao: RecentItemDao,
        private val linkItemDao: LinkItemDao,
        private val aiEventDao: AiEventDao,
        private val lifeManagementLevelStatusDao: LifeManagementLevelStatusDao,
        private val lifeSystemStateDao: LifeSystemStateDao,
        private val mainBeaconDao: MainBeaconDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
        private val contextStructureDao: ContextStructureDao,
        private val directionDao: DirectionDao,
        private val contextInboxSortingDao: ContextInboxSortingDao,
        private val contextKeyProblemsDao: ContextKeyProblemsDao,
        private val focusContextIntervalDao: FocusContextIntervalDao,
        private val userStateIntervalDao: UserStateIntervalDao,
        private val dayManagementRuntimeRepository: DayManagementRuntimeRepository,
        private val inboxAssociationCache: InboxAssociationCache,
    ) : MergeLocalDataSource {
        override suspend fun getContexts(): List<Context> = contextDao.getAll()

        override suspend fun getGoals(): List<Goal> = goalDao.getAll()

        override suspend fun insertContexts(contexts: List<Context>) =
            contextWorkspaceWriteThrough.mutate { contextDao.insertContexts(contexts) }

        override suspend fun insertGoals(goals: List<Goal>) = goalDao.insertGoals(goals)

        override suspend fun insertAttachments(attachments: List<AttachmentEntity>) = attachmentDao.insertAttachments(attachments)

        override suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>) =
            attachmentDao.insertContextAttachmentLinks(links)

        override suspend fun insertListItems(items: List<BacklogItem>) {
            val normalized =
                normalizeLegacyStructuralContextBacklog(
                    backlogItems = items,
                    backlogOrders = emptyList(),
                    parentByContextId =
                        contextDao.getAll().associate { it.id to it.parentId },
                    now = System.currentTimeMillis(),
                )
            listItemDao.insertItems(normalized.backlogItems)
        }

        override suspend fun applyChanges(changes: List<SyncChange>) {
            contextWorkspaceWriteThrough.mutate {
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
                "Список" -> {
                    val context = contextDao.getContextById(change.id)
                    if (context == null) contextDao.delete(change.id) else contextDao.insert(context.softDelete())
                }
                "Вкладення" -> attachmentDao.deleteAttachment(change.id)
            }
        }

        override suspend fun importSelectedData(
            projects: List<Context>,
            goals: List<Goal>,
            listItems: List<BacklogItem>,
            attachments: List<AttachmentEntity>,
            crossRefs: List<ContextAttachmentCrossRef>,
        ) {
            contextWorkspaceWriteThrough.mutate {
                if (projects.isNotEmpty()) contextDao.insertContexts(projects)
                if (goals.isNotEmpty()) goalDao.insertGoals(goals)
                if (listItems.isNotEmpty()) listItemDao.insertItems(listItems)
                if (attachments.isNotEmpty()) attachmentDao.insertAttachments(attachments)
                if (crossRefs.isNotEmpty()) attachmentDao.insertContextAttachmentLinks(crossRefs)
            }
        }

        override suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
            requireValidCanonicalDayThemePayload(bundle)
            requireValidCanonicalOrientationPayload(bundle)

            val hasCanonicalDayThemePayload =
                bundle.themeDefinitions != null &&
                    bundle.dayThemes != null &&
                    bundle.dayThemeAssignmentDocuments != null

            // Both canonical 111 and legacy 000 merge into canonical persistence.
            // Bootstrap first so local canonical authority is complete before conflict resolution.
            canonicalDayThemeBootstrapper.ensureBootstrapped()

            Log.d(
                "BackupImport",
                "Applying Snapshot V${bundle.version} in MergeLocalDataSource: notes=${bundle.notes.size}, docs=${bundle.documents.size}, checklists=${bundle.checklists.size}, scripts=${bundle.scripts.size}",
            )
            val incomingPlanIdRemap =
                bundle.dayPlans
                    .mapNotNull { incomingPlan ->
                        val existingPlan = findExistingPlanForIncomingDate(incomingPlan.date)
                        if (existingPlan != null && existingPlan.id != incomingPlan.id) {
                            incomingPlan.id to existingPlan.id
                        } else {
                            null
                        }
                    }.toMap()
            val dayPlansToInsert =
                bundle.dayPlans.filterNot { incomingPlan ->
                    incomingPlanIdRemap.containsKey(incomingPlan.id)
                }
            val remappedDayFocusItems =
                bundle.dayFocusItems.map { item ->
                    incomingPlanIdRemap[item.dayPlanId]?.let { existingPlanId ->
                        item.copy(dayPlanId = existingPlanId)
                    } ?: item
                }
            val remappedDayTasks =
                bundle.dayTasks.map { task ->
                    incomingPlanIdRemap[task.dayPlanId]?.let { existingPlanId ->
                        task.copy(dayPlanId = existingPlanId)
                    } ?: task
                }
            val validPlanIds =
                (dayPlanDao.getAllPlansSync().map { plan -> plan.id } + dayPlansToInsert.map { plan -> plan.id })
                    .toSet()

            val localCanonicalThemeDefinitions =
                canonicalDayThemeDao.getAllThemeDefinitionsSync().map { it.toCanonicalSnapshot() }
            val localCanonicalDayThemes =
                canonicalDayThemeDao.getAllDayThemesSync().map { it.toCanonicalSnapshot() }
            val localCanonicalAssignmentDocuments =
                canonicalDayThemeDao.getAllAssignmentDocumentsSync().map { it.toCanonicalSnapshot() }

            val canonicalDayThemeMergePlan =
                if (hasCanonicalDayThemePayload) {
                    planCanonicalDayThemeMerge(
                        incomingThemeDefinitions = requireNotNull(bundle.themeDefinitions),
                        incomingDayThemes = requireNotNull(bundle.dayThemes),
                        incomingAssignmentDocuments = requireNotNull(bundle.dayThemeAssignmentDocuments),
                        incomingPlanIdRemap = incomingPlanIdRemap,
                        validPlanIds = validPlanIds,
                        localThemeDefinitions = localCanonicalThemeDefinitions,
                        localDayThemes = localCanonicalDayThemes,
                        localAssignmentDocuments = localCanonicalAssignmentDocuments,
                    )
                } else {
                    planLegacyDayThemeMerge(
                        incomingLegacyDocuments = bundle.dayThemeDocuments,
                        incomingPlanIdRemap = incomingPlanIdRemap,
                        validPlanIds = validPlanIds,
                        localThemeDefinitions = localCanonicalThemeDefinitions,
                        localDayThemes = localCanonicalDayThemes,
                        localAssignmentDocuments = localCanonicalAssignmentDocuments,
                    )
                }

            // A DayPlan remap can make an incoming focus/responsibility item
            // share the same logical recurring occurrence with an existing local
            // physical row while retaining a different incoming id. Contain that
            // alias instead of silently creating a second row. Live/tombstone
            // state is deliberately not conflict-resolved here.
            val localFocusOccurrenceIdsByLogicalKey =
                dayFocusItemDao
                    .getAllSync()
                    .mapNotNull { item ->
                        item.recurringKey?.let { recurringKey ->
                            (item.dayPlanId to recurringKey) to item.id
                        }
                    }.groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )
            val localCanonicalFocusOccurrenceIdsByLogicalKey =
                dayFocusItemDao
                    .getAllSync()
                    .mapNotNull { item ->
                        val seriesId = item.recurrenceSeriesId ?: return@mapNotNull null
                        val dayKey = item.recurrenceOccurrenceDayKey ?: return@mapNotNull null
                        (seriesId to dayKey) to item.id
                    }.groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )
            val remappedIncomingFocusItemIds =
                bundle.dayFocusItems
                    .asSequence()
                    .filter { item -> incomingPlanIdRemap.containsKey(item.dayPlanId) }
                    .mapTo(hashSetOf()) { item -> item.id }

            val localFocusItemsById = dayFocusItemDao.getAllSync().associateBy { item -> item.id }
            val dayFocusItemsToInsert =
                remappedDayFocusItems
                    .filter { item -> item.dayPlanId in validPlanIds }
                    .filterNot { item ->
                        val canonicalRecurrence = item.recurrence
                        if (canonicalRecurrence != null) {
                            localCanonicalFocusOccurrenceIdsByLogicalKey[
                                canonicalRecurrence.seriesId to canonicalRecurrence.occurrenceDayKey
                            ]?.any { localPhysicalId -> localPhysicalId != item.id } == true
                        } else if (item.id !in remappedIncomingFocusItemIds) {
                            false
                        } else {
                            val recurringKey = item.recurringKey ?: return@filterNot false
                            localFocusOccurrenceIdsByLogicalKey[item.dayPlanId to recurringKey]
                                ?.any { localPhysicalId -> localPhysicalId != item.id } == true
                        }
                    }
                    .filter { incoming ->
                        val local = localFocusItemsById[incoming.id] ?: return@filter true
                        val incomingUpdatedAt = incoming.updatedAt
                        val localUpdatedAt = local.updatedAt ?: Long.MIN_VALUE
                        when {
                            incoming.version != local.version -> incoming.version > local.version
                            incomingUpdatedAt != localUpdatedAt -> incomingUpdatedAt > localUpdatedAt
                            incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                            else -> false
                        }
                    }
            check(bundle.recurringTasks.isEmpty()) {
                "Legacy recurrence-v1 recurringTasks payload is not supported by canonical sync"
            }
            check(
                remappedDayTasks.none { task ->
                    task.recurringTaskId != null ||
                        task.nextOccurrenceTime != null ||
                        task.id.startsWith("recurring-task-instance-") ||
                        (task.id.startsWith("recurrence:TASK:") && task.recurrence == null)
                },
            ) {
                "Legacy recurrence-v1 DayTask payload is not supported by canonical sync"
            }

            val validGoalIds =
                (goalDao.getAllRaw().map { goal -> goal.id } + bundle.goals.map { goal -> goal.id })
                    .toSet()
            val validContextIdsForDayTasks =
                (contextDao.getAllRaw().map { context -> context.id } + bundle.contexts.map { context -> context.id })
                    .toSet()
            val validActivityRecordIds =
                (activityRecordDao.getAllRaw().map { record -> record.id } + bundle.activityRecords.map { record -> record.id })
                    .toSet()

            // Preserve one physical row for each canonical logical occurrence.
            // Live and tombstone rows share the same (seriesId, occurrenceDayKey)
            // identity, so this also preserves anti-resurrection semantics.
            val localCanonicalTaskOccurrenceIdsByLogicalKey =
                dayTaskDao
                    .getAllTasksSync()
                    .mapNotNull { task ->
                        val seriesId = task.recurrenceSeriesId ?: return@mapNotNull null
                        val dayKey = task.recurrenceOccurrenceDayKey ?: return@mapNotNull null
                        (seriesId to dayKey) to task.id
                    }.groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )
            val dayTasksToInsert =
                remappedDayTasks
                    .filter { task -> task.dayPlanId in validPlanIds }
                    .filterNot { task ->
                        val canonicalRecurrence = task.recurrence ?: return@filterNot false
                        localCanonicalTaskOccurrenceIdsByLogicalKey[
                            canonicalRecurrence.seriesId to canonicalRecurrence.occurrenceDayKey
                        ]?.any { localPhysicalId -> localPhysicalId != task.id } == true
                    }.map { task ->
                        task.copy(
                            goalId = task.goalId?.takeIf { id -> id in validGoalIds },
                            projectId = task.projectId?.takeIf { id -> id in validContextIdsForDayTasks },
                            activityRecordId = task.activityRecordId?.takeIf { id -> id in validActivityRecordIds },
                        )
                    }
            val skippedDayTaskCount = remappedDayTasks.size - dayTasksToInsert.size
            val skippedDayFocusCount = remappedDayFocusItems.size - dayFocusItemsToInsert.size
            val clearedTaskGoalCount = remappedDayTasks.count { task -> task.goalId != null && task.goalId !in validGoalIds }
            val clearedTaskContextCount =
                remappedDayTasks.count { task -> task.projectId != null && task.projectId !in validContextIdsForDayTasks }
            val clearedTaskActivityCount =
                remappedDayTasks.count { task -> task.activityRecordId != null && task.activityRecordId !in validActivityRecordIds }
            Log.i(
                "ForwardSync",
                    "merge snapshot version=${bundle.version} incomingPlans=${bundle.dayPlans.size} " +
                    "insertPlans=${dayPlansToInsert.size} remapPlans=${incomingPlanIdRemap.size} " +
                    "incomingFocus=${bundle.dayFocusItems.size} insertFocus=${dayFocusItemsToInsert.size} " +
                    "skippedFocus=$skippedDayFocusCount " +
                    "incomingTasks=${bundle.dayTasks.size} insertTasks=${dayTasksToInsert.size} skippedTasks=$skippedDayTaskCount " +
                    "clearedTaskFks=goal:$clearedTaskGoalCount,context:$clearedTaskContextCount," +
                    "activity:$clearedTaskActivityCount " +
                    "runtime=${bundle.dayManagementRuntimeState != null} " +
                    "incomingPlanDates=${bundle.dayPlans.map { plan -> "${plan.id}:${plan.date}" }} " +
                    "remap=$incomingPlanIdRemap",
            )

            contextWorkspaceWriteThrough.mutate {
                contextDao.insertAll(bundle.contexts.map { it.toEntity() })
                val validContextIds = bundle.contexts.map { it.id }.toSet()
                contextParentLinkDao.insertAll(bundle.contextParentLinks.map { it.toEntity() })
                directionDao.insertAll(bundle.directionItems.map { it.toEntity() })
                goalDao.insertAll(bundle.goals.map { it.toEntity() })
                noteDocumentDao.insertAllDocuments(bundle.documents.map { it.toEntity() })
                val validDocumentIds = bundle.documents.map { it.id }.toSet()
                musicNoteDao.insertAll(bundle.musicNotes.map { it.toEntity() })
                legacyNoteDao.insertAll(bundle.notes.map { it.toEntity() })
                checklistDao.insertChecklists(bundle.checklists.map { it.toEntity() })

                // --- Consolidate and auto-link attachments and cross-refs ---
                val finalAttachments = bundle.attachments.map { it.toEntity() }.toMutableList()
                val finalCrossRefs = bundle.crossRefs.map { it.toEntity() }.toMutableList()

                Log.d("BackupImport", "Total attachments to insert: ${finalAttachments.size}")
                attachmentDao.insertAttachments(finalAttachments)
                Log.d("BackupImport", "Total cross-refs to insert: ${finalCrossRefs.size}")
                attachmentDao.insertContextAttachmentCrossRefs(finalCrossRefs)

                conversationFolderDao.insertAll(bundle.conversationFolders.map { it.toEntity() })
                dayPlanDao.insertPlans(dayPlansToInsert.map { it.toEntity() })

                if (canonicalDayThemeMergePlan.themeDefinitions.isNotEmpty()) {
                    canonicalDayThemeDao.upsertThemeDefinitions(
                        canonicalDayThemeMergePlan.themeDefinitions.map { it.toCanonicalEntity() },
                    )
                }
                if (canonicalDayThemeMergePlan.dayThemes.isNotEmpty()) {
                    canonicalDayThemeDao.upsertDayThemes(
                        canonicalDayThemeMergePlan.dayThemes.map { it.toCanonicalEntity() },
                    )
                }
                if (canonicalDayThemeMergePlan.assignmentDocuments.isNotEmpty()) {
                    canonicalDayThemeDao.upsertAssignmentDocuments(
                        canonicalDayThemeMergePlan.assignmentDocuments.map { it.toCanonicalEntity() },
                    )
                }

                dayFocusItemDao.insertAll(
                    dayFocusItemsToInsert.map { snapshot ->
                        com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                            .dayFocusItemEntity(snapshot, snapshot.toEntity())
                    },
                )
                val localCanonicalSeriesById =
                    canonicalRecurringSeriesDao.getAllSync().associateBy { series -> series.id }
                canonicalRecurringSeriesDao.insertAll(
                    bundle.recurringSeries
                        .filter { incoming ->
                            val local = localCanonicalSeriesById[incoming.id] ?: return@filter true
                            val incomingUpdatedAt = incoming.updatedAt ?: Long.MIN_VALUE
                            when {
                                incoming.version != local.version -> incoming.version > local.version
                                incomingUpdatedAt != local.updatedAt -> incomingUpdatedAt > local.updatedAt
                                incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                                else -> false
                            }
                        }
                        .map { it.toEntity() },
                )

                val missions = bundle.tacticalMissions.map { it.toEntity() }
                if (missions.isNotEmpty()) {
                    tacticalMissionDao.insertMissions(missions)
                    Log.d("MergeImport", "Tactical Missions: ${missions.size} records processed during Import.")
                }
                tacticalIterationDao.insertAll(bundle.tacticalIterations)
                missionStreamDao.insertAll(bundle.missionStreams)
                tacticalActivitySlotDao.insertAll(bundle.tacticalActivitySlots)
                arcQuestDao.insertAll(bundle.arcQuests)

                val normalizedContextBacklog =
                    normalizeLegacyStructuralContextBacklog(
                        backlogItems = bundle.backlogItems.map { it.toEntity() },
                        backlogOrders = bundle.backlogOrders.map { it.toEntity() },
                        parentByContextId =
                            contextDao.getAll().associate { it.id to it.parentId },
                        now = System.currentTimeMillis(),
                    )
                listItemDao.insertItems(normalizedContextBacklog.backlogItems)
                backlogOrderDao.insertAll(normalizedContextBacklog.backlogOrders)
                checklistDao.insertItems(bundle.checklistItems.map { it.toEntity() })
                contextArtifactDao.insertAll(bundle.artifacts.map { it.toEntity() })
                scriptDao.insertAll(bundle.scripts.map { it.toEntity() })
                inboxRecordDao.insertAll(bundle.inbox.map { it.toEntity() })

                val localContextLogsById =
                    contextManagementDao.getAllLogs().associateBy { log -> log.id }
                val canonicalIncomingIds =
                    bundle.canonicalExecutionLogs.orEmpty().mapTo(hashSetOf()) { it.id }

                require(bundle.logs.none { it.id in canonicalIncomingIds }) {
                    "EXECUTION_LOG payload contains the same id in legacy and canonical streams"
                }

                contextManagementDao.insertLogs(
                    bundle.logs
                        .filter { incoming ->
                            val local = localContextLogsById[incoming.id] ?: return@filter true
                            require(local.contextId != null) {
                                "EXECUTION_LOG id collision between legacy Context and canonical Workspace streams: ${incoming.id}"
                            }
                            val localUpdatedAt = local.updatedAt ?: Long.MIN_VALUE
                            when {
                                incoming.version != local.version -> incoming.version > local.version
                                incoming.updatedAt != localUpdatedAt -> incoming.updatedAt > localUpdatedAt
                                incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                                else -> false
                            }
                        }
                        .map { it.toEntity() },
                )

                systemAppDao.insertAll(
                    bundle.systemApps.mapNotNull { app ->
                        when {
                            app.contextId !in validContextIds -> null
                            app.noteDocumentId != null && app.noteDocumentId !in validDocumentIds ->
                                app.copy(noteDocumentId = null)
                            else -> app
                        }
                    }.map { it.toEntity() },
                )
                activityRecordDao.insertAll(bundle.activityRecords.map { it.toEntity() })
                recentItemDao.insertAllSync(bundle.recentProjectEntries.map { it.toEntity() })
                linkItemDao.insertAll(bundle.linkItemEntities.map { it.toEntity() })
                dayTaskDao.insertTasks(
                    dayTasksToInsert.map { snapshot ->
                        com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
                            .dayTaskEntity(snapshot, snapshot.toEntity())
                    },
                )
                dailyMetricDao.insertMetrics(bundle.dailyMetrics.map { it.toEntity() })
                chatDao.insertConversations(bundle.conversations.map { it.toEntity() })
                chatDao.insertMessages(bundle.chatMessages.map { it.toEntity() })
                reminderDao.insertAll(bundle.reminders.map { it.toEntity() })
                tacticalMissionDao.insertMissionAttachments(bundle.tacticalMissionAttachments.map { it.toEntity() })
                aiEventDao.insertAll(bundle.aiEvents.map { it.toEntity() })
                aiInsightDao.upsertAll(bundle.aiInsights.map { it.toEntity() })
                mainBeaconDao.insertGroups(bundle.mainBeaconGroups.map { it.toEntity() })
                mainBeaconDao.insertBeacons(bundle.mainBeacons.map { it.toEntity() })
                lifeManagementLevelStatusDao.upsertAll(bundle.lifeManagementLevelStatuses.map { it.toEntity() })
                lifeSystemStateDao.insertAll(bundle.lifeSystemStates.map { it.toEntity() })
                structurePresetDao.insertAll(bundle.contextRoleProfiles.map { it.toEntity() })
                structurePresetItemDao.insertAll(bundle.contextRoleProfileItems.map { it.toEntity() })
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
                )
                contextStructureDao.insertAllItems(bundle.projectStructureItems.map { it.toEntity() })
                contextInboxSortingDao.insertAll(bundle.contextInboxSortingRules.map { it.toEntity() })
                contextKeyProblemsDao.insertAll(bundle.contextKeyProblems.map { it.toEntity() })
                focusContextIntervalDao.insertAll(bundle.focusContextIntervals.map { it.toEntity() })
                userStateIntervalDao.insertAll(bundle.userStateIntervals.map { it.toEntity() })
                mainBeaconDao.insertGroupMembers(bundle.mainBeaconGroupMembers.map { it.toEntity() })
                mainBeaconDao.insertParentLinks(bundle.mainBeaconParentLinks.map { it.toEntity() })
                mainBeaconDao.insertContextCrossRefs(bundle.mainBeaconContextCrossRefs.map { it.toEntity() })
                mainBeaconDao.insertAttachmentCrossRefs(bundle.mainBeaconAttachmentCrossRefs.map { it.toEntity() })
                mainBeaconDao.insertLevelStatuses(bundle.mainBeaconLevelStatuses.map { it.toEntity() })
                db.orientationDao().storeCanonicalPayload(bundle, merge = true, workspaceDao = db.workspaceDao())
                canonicalWorkspaceDirectionEntrySyncStore.mergeIncoming(bundle.workspaceDirectionEntries)
                canonicalExecutionLogSyncStore.mergeIncoming(bundle.canonicalExecutionLogs)
            }
            executionLogWorkspaceOwnershipBridge.repairUnresolved()
            // InboxRecordLink is a local materialized cache only.
            // Rebuild it from canonical InboxRecord + Context.tags after import.
            inboxAssociationCache.rebuild()
            canonicalOrientationBootstrapper.ensureBootstrapped()
            workspaceDirectionEntryShadowMaterializer.ensureMaterialized()

            bundle.dayManagementRuntimeState?.let { runtimeState ->
                Log.i(
                    "ForwardSync",
                    "merge runtime import phase=${runtimeState.currentPhase} sleepAt=${runtimeState.sleepAt} " +
                        "wokeAt=${runtimeState.wokeAt} updatedAt=${runtimeState.updatedAt}",
                )
                dayManagementRuntimeRepository.mergeSnapshot(runtimeState)
            }
            val affectedPlanIds =
                (dayPlansToInsert.map { plan -> plan.id } + dayTasksToInsert.map { task -> task.dayPlanId })
                    .distinct()
            affectedPlanIds.forEach { planId ->
                Log.i(
                    "ForwardSync",
                    "merge affected plan dayPlanId=$planId taskCount=${dayTaskDao.getTaskCountForDay(planId)}",
                )
            }
        }

        private suspend fun findExistingPlanForIncomingDate(incomingDate: Long) =
            dayPlanDao.getPlanForDateSync(localDayStart(incomingDate))
                ?: dayPlanDao.getPlanForDateWindowSync(
                    startInclusiveMillis = localDayStart(incomingDate),
                    endExclusiveMillis = localDayStart(incomingDate) + DAY_IN_MILLIS,
                    anchorMillis = incomingDate,
                )

        private fun localDayStart(timestamp: Long): Long =
            Calendar
                .getInstance()
                .apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

        private companion object {
            const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        }
    }
