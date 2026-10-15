package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningSyncStore
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.DayPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.TacticalPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.StrategicPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.core.data.models.sync.toWorkspaceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedWorkspaceTopology
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.DayTaskOwnerRejection
import com.romankozak.forwardappmobile.data.dao.DayTaskWorkspaceOwnerRejected
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBacklogSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceInboxSyncStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceProblemSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagTransportStore
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemContextShellRetirer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.MusicNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.KeyProblemsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityAvailability
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncVersion
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncPayload
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkClass
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.logicalProjectId
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao

@RunWith(RobolectricTestRunner::class)
class SystemCapabilityTransportRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `H1 full export carries live and tombstoned placements including syncedAt`() = runBlocking {
        val database = database()
        try {
            val workspace =
                canonicalWorkspace("h1-export-workspace")
            database.workspaceDao().upsert(listOf(workspace))
            database.hierarchyPlacementDao().upsertAll(
                listOf(
                    HierarchyPlacementEntity(
                        id = "h1-live",
                        hierarchyId = "GENERAL",
                        targetType = "WORKSPACE",
                        targetId = workspace.id,
                        parentPlacementId = null,
                        placementKind = "PRIMARY",
                        siblingOrder = 0L,
                        createdAt = 10L,
                        updatedAt = 20L,
                        syncedAt = 15L,
                        isDeleted = false,
                        version = 2L,
                    ),
                    HierarchyPlacementEntity(
                        id = "h1-deleted",
                        hierarchyId = "GENERAL",
                        targetType = "WORKSPACE",
                        targetId = workspace.id,
                        parentPlacementId = null,
                        placementKind = "LINK",
                        siblingOrder = 1L,
                        createdAt = 11L,
                        updatedAt = 30L,
                        syncedAt = 25L,
                        isDeleted = true,
                        version = 3L,
                    ),
                ),
            )

            val bundle = fullBackup(database).loadFullSnapshotBundle()
            val exported =
                requireNotNull(bundle.hierarchyPlacements)
                    .associateBy { it.id }

            assertEquals(setOf("h1-live", "h1-deleted"), exported.keys)
            assertFalse(exported.getValue("h1-live").isDeleted)
            assertTrue(exported.getValue("h1-deleted").isDeleted)
            assertEquals(15L, exported.getValue("h1-live").syncedAt)
            assertEquals(25L, exported.getValue("h1-deleted").syncedAt)
            val exportedWorkspace =
                requireNotNull(bundle.workspaces)
                    .single { it.id == workspace.id }
            assertNull(exportedWorkspace.parentWorkspaceId)
            assertEquals(0L, exportedWorkspace.workspaceOrder)
        } finally {
            database.close()
        }
    }

    @Test
    fun `full export carries canonical Day scope extension without legacy DayPlan synthesis`() = runBlocking {
        val database = database()
        try {
            val planningStore = CanonicalPlanningSyncStore(database)
            val decodedPlanning =
                planningStore.decodeAndValidateForRestore(
                    scopes = listOf(planningScope(kind = "DAY")),
                    commitments = emptyList(),
                    liveOrientationIds = emptySet(),
                )
            planningStore.restoreExactDecoded(decodedPlanning)

            val extensionStore = CanonicalDayPlanningScopeExtensionSyncStore(database)
            extensionStore.restoreExactDecoded(
                extensionStore.decodeAndValidateForRestore(
                    snapshots = listOf(dayPlanningExtension()),
                    scopes = decodedPlanning.scopes,
                ),
            )

            val bundle = fullBackup(database).loadFullSnapshotBundle()

            assertEquals(CURRENT_PLANNING_FORMAT_VERSION, bundle.planningFormatVersion)
            assertEquals(listOf(planningScope(kind = "DAY")), bundle.planningScopes)
            assertEquals(emptyList<Any>(), bundle.planningCommitments)
            assertEquals(listOf(dayPlanningExtension()), bundle.dayPlanningScopeExtensions)
            assertEquals(emptyList<Any>(), bundle.tacticalPlanningScopeExtensions)
            assertEquals(emptyList<Any>(), bundle.strategicPlanningScopeExtensions)
            assertTrue(database.dayPlanDao().getAllPlansSync().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `full export carries canonical Strategic extension without ArcQuest synthesis`() = runBlocking {
        val database = database()
        try {
            val planningStore = CanonicalPlanningSyncStore(database)
            val decodedPlanning =
                planningStore.decodeAndValidateForRestore(
                    scopes = listOf(planningScope(kind = "STRATEGIC_ARC")),
                    commitments = emptyList(),
                    liveOrientationIds = emptySet(),
                )
            planningStore.restoreExactDecoded(decodedPlanning)

            val extensionStore = CanonicalStrategicPlanningScopeExtensionSyncStore(database)
            extensionStore.restoreExactDecoded(
                extensionStore.decodeAndValidateForRestore(
                    snapshots = listOf(strategicPlanningExtension()),
                    scopes = decodedPlanning.scopes,
                ),
            )

            val bundle = fullBackup(database).loadFullSnapshotBundle()

            assertEquals(CURRENT_PLANNING_FORMAT_VERSION, bundle.planningFormatVersion)
            assertEquals(listOf(strategicPlanningExtension()), bundle.strategicPlanningScopeExtensions)
            assertTrue(bundle.arcQuests.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `full export carries all eight System capability instances losslessly`() = runBlocking {
        val database = database()
        try {
            initializer(database).ensureCanonicalSystemWorkspaceOwnership()
            val expected = seedCanonicalCapabilities(database)
            database.contextStructureDao().insertStructure(contradictoryLegacyConfiguration())

            val bundle = fullBackup(database).loadFullSnapshotBundle()

            val workspace = bundle.workspaces.orEmpty().single { it.id == SYSTEM_ID }
            assertEquals("CANONICAL_ONLY", workspace.provenance)
            assertNull(workspace.sourceContextId)
            val exported =
                bundle.workspaceCapabilityInstances.orEmpty()
                    .filter { it.workspaceId == SYSTEM_ID }
                    .associateBy { it.capabilityType }
            assertEquals(TARGET_TYPES.map { it.name }.toSet(), exported.keys)
            expected.forEach { (type, instance) ->
                assertEquals(instance, exported.getValue(type.name))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `full restore keeps canonical System lifecycle and typed configuration over legacy`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()
                val expected = seedCanonicalCapabilities(source)
                source.contextStructureDao().insertStructure(contradictoryLegacyConfiguration())
                val bundle = fullBackup(source).loadFullSnapshotBundle()

                restore(destination, bundle)
                assertReservedSystemShellsAbsent(destination)

                val restored =
                    destination.orientationDao().getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == SYSTEM_ID }
                        .associateBy { it.capabilityType }
                assertEquals(TARGET_TYPES.map { it.name }.toSet(), restored.keys)
                expected.forEach { (type, instance) ->
                    assertEquals(instance, restored.getValue(type.name))
                }
                assertEquals(
                    InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED),
                    InboxCapabilityConfigurationCodec.decode(
                        restored.getValue(WorkspaceCapabilityType.INBOX.name).configurationVersion,
                        restored.getValue(WorkspaceCapabilityType.INBOX.name).configuration,
                    ),
                )
                assertEquals(
                    DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false),
                    DirectionCapabilityConfigurationCodec.decode(
                        restored.getValue(WorkspaceCapabilityType.DIRECTION.name).configurationVersion,
                        restored.getValue(WorkspaceCapabilityType.DIRECTION.name).configuration,
                    ),
                )
                assertEquals(
                    BacklogCapabilityConfigurationV2(removeEntryAfterTagAutocopy = true),
                    BacklogCapabilityConfigurationCodec.decode(
                        restored.getValue(WorkspaceCapabilityType.BACKLOG.name).configurationVersion,
                        restored.getValue(WorkspaceCapabilityType.BACKLOG.name).configuration,
                    ),
                )
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full snapshot round trips canonical System Workspace tags before compatibility shells converge`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()
                CanonicalWorkspaceTagRepository(source).replaceTags(
                    workspaceId = SYSTEM_ID,
                    tags = listOf("#Focus", "focus", "Strategy"),
                    now = 200L,
                )
                val expected = source.workspaceTagRefDao().getAllForWorkspace(SYSTEM_ID)

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(expected, bundle.workspaceTagRefs.orEmpty().filter { it.workspaceId == SYSTEM_ID })

                restore(destination, bundle)

                assertEquals(expected, destination.workspaceTagRefDao().getAllForWorkspace(SYSTEM_ID))
                assertEquals(20, destination.systemWorkspaceTagSeedStateDao().getAll().size)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full restore installs System Workspace before routing Main Beacon operational owner`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()
                source.mainBeaconDao().insertBeacon(
                    MainBeacon(id = "transport-beacon", title = "Beacon", createdAt = 1L, updatedAt = 1L),
                )
                source.mainBeaconDao().insertContextCrossRefs(
                    listOf(MainBeaconContextCrossRef("transport-beacon", SYSTEM_ID, order = 7L)),
                )

                restore(destination, fullBackup(source).loadFullSnapshotBundle())

                assertEquals(
                    listOf(SYSTEM_ID),
                    destination.mainBeaconDao().getAllWorkspaceCrossRefsSync().map { it.workspaceId },
                )
                assertEquals(
                    7L,
                    destination.mainBeaconDao().getAllWorkspaceCrossRefsSync().single().order,
                )
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `merge installs System Workspace before routing Main Beacon operational owner`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()
                source.mainBeaconDao().insertBeacon(
                    MainBeacon(id = "merge-beacon", title = "Beacon", createdAt = 1L, updatedAt = 1L),
                )
                source.mainBeaconDao().insertContextCrossRefs(
                    listOf(MainBeaconContextCrossRef("merge-beacon", SYSTEM_ID, order = 9L)),
                )

                merge(destination).applySnapshotBundle(fullBackup(source).loadFullSnapshotBundle())
                assertReservedSystemShellsAbsent(destination)

                assertEquals(
                    listOf(SYSTEM_ID),
                    destination.mainBeaconDao().getAllWorkspaceCrossRefsSync().map { it.workspaceId },
                )
                assertEquals(
                    9L,
                    destination.mainBeaconDao().getAllWorkspaceCrossRefsSync().single().order,
                )
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full restore round trips reserved DayTask project through Workspace branch`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                source.dayPlanDao().insert(
                    DayPlan(
                        id = DAY_TASK_PLAN_ID,
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )
                source.dayTaskDao().insert(
                    DayTask(
                        id = DAY_TASK_ID,
                        dayPlanId = DAY_TASK_PLAN_ID,
                        title = "System-owned day task",
                        projectId = DAY_TASK_SYSTEM_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val sourceStored = requireNotNull(source.dayTaskDao().getTaskById(DAY_TASK_ID))
                assertNull(sourceStored.projectId)
                assertEquals(DAY_TASK_SYSTEM_ID, sourceStored.projectWorkspaceId)
                assertEquals(DAY_TASK_SYSTEM_ID, sourceStored.logicalProjectId)

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                val wireTask = bundle.dayTasks.single { it.id == DAY_TASK_ID }
                assertEquals(DAY_TASK_SYSTEM_ID, wireTask.projectId)

                restore(destination, bundle)

                val restored =
                    requireNotNull(destination.dayTaskDao().getTaskById(DAY_TASK_ID))
                assertNull(restored.projectId)
                assertEquals(DAY_TASK_SYSTEM_ID, restored.projectWorkspaceId)
                assertEquals(DAY_TASK_SYSTEM_ID, restored.logicalProjectId)

                val exportedAgain =
                    fullBackup(destination)
                        .loadFullSnapshotBundle()
                        .dayTasks
                        .single { it.id == DAY_TASK_ID }

                assertEquals(DAY_TASK_SYSTEM_ID, exportedAgain.projectId)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `merge installs System Workspace before routing reserved DayTask project`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                source.dayPlanDao().insert(
                    DayPlan(
                        id = DAY_TASK_PLAN_ID,
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )
                source.dayTaskDao().insert(
                    DayTask(
                        id = DAY_TASK_ID,
                        dayPlanId = DAY_TASK_PLAN_ID,
                        title = "Merged system-owned day task",
                        projectId = DAY_TASK_SYSTEM_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(
                    DAY_TASK_SYSTEM_ID,
                    bundle.dayTasks.single { it.id == DAY_TASK_ID }.projectId,
                )

                merge(destination).applySnapshotBundle(bundle)

                val merged =
                    requireNotNull(destination.dayTaskDao().getTaskById(DAY_TASK_ID))
                assertNull(merged.projectId)
                assertEquals(DAY_TASK_SYSTEM_ID, merged.projectWorkspaceId)
                assertEquals(DAY_TASK_SYSTEM_ID, merged.logicalProjectId)

                val exportedAgain =
                    fullBackup(destination)
                        .loadFullSnapshotBundle()
                        .dayTasks
                        .single { it.id == DAY_TASK_ID }

                assertEquals(DAY_TASK_SYSTEM_ID, exportedAgain.projectId)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full restore round trips reserved TacticalMission project through Workspace branch`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                source.tacticalMissionDao().insertMission(
                    TacticalMission(
                        id = TACTICAL_MISSION_ID,
                        title = "System-owned tactical mission",
                        description = null,
                        deadline = NO_DEADLINE,
                        projectId = TACTICAL_SYSTEM_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                val wireMission =
                    bundle.tacticalMissions.single { it.id == TACTICAL_MISSION_ID }

                assertEquals(TACTICAL_SYSTEM_ID, wireMission.projectId)

                restore(destination, bundle)

                val restored =
                    requireNotNull(
                        destination.tacticalMissionDao().getMissionById(TACTICAL_MISSION_ID),
                    )
                assertNull(restored.projectId)
                assertEquals(TACTICAL_SYSTEM_ID, restored.projectWorkspaceId)
                assertEquals(TACTICAL_SYSTEM_ID, restored.logicalProjectId)

                val exportedAgain =
                    fullBackup(destination)
                        .loadFullSnapshotBundle()
                        .tacticalMissions
                        .single { it.id == TACTICAL_MISSION_ID }

                assertEquals(TACTICAL_SYSTEM_ID, exportedAgain.projectId)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full restore round trips standalone TacticalMission project through Workspace branch`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                source.workspaceDao().upsert(listOf(standaloneWorkspace(TACTICAL_STANDALONE_ID)))
                source.tacticalMissionDao().insertMission(
                    TacticalMission(
                        id = TACTICAL_MISSION_ID,
                        title = "Standalone-owned tactical mission",
                        description = null,
                        deadline = NO_DEADLINE,
                        projectId = TACTICAL_STANDALONE_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(
                    TACTICAL_STANDALONE_ID,
                    bundle.tacticalMissions.single { it.id == TACTICAL_MISSION_ID }.projectId,
                )

                restore(destination, bundle)

                val restored =
                    requireNotNull(
                        destination.tacticalMissionDao().getMissionById(TACTICAL_MISSION_ID),
                    )
                assertNull(restored.projectId)
                assertEquals(TACTICAL_STANDALONE_ID, restored.projectWorkspaceId)
                assertEquals(TACTICAL_STANDALONE_ID, restored.logicalProjectId)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full restore preserves standalone and retired ordinary operational owners`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                source.contextDao().insertContexts(listOf(retiredContext(RETIRED_OWNER_ID)))
                source.workspaceDao().upsert(
                    listOf(
                        standaloneWorkspace(DAY_TASK_STANDALONE_ID),
                        canonicalWorkspace(RETIRED_OWNER_ID),
                    ),
                )
                source.dayPlanDao().insert(
                    DayPlan(
                        id = DAY_TASK_PLAN_ID,
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )
                source.dayTaskDao().insertTasks(
                    listOf(
                        DayTask(
                            id = DAY_TASK_STANDALONE_ID,
                            dayPlanId = DAY_TASK_PLAN_ID,
                            title = "Standalone-owned task",
                            projectId = DAY_TASK_STANDALONE_ID,
                            createdAt = 1L,
                            updatedAt = 1L,
                            version = 1L,
                        ),
                        DayTask(
                            id = DAY_TASK_RETIRED_ID,
                            dayPlanId = DAY_TASK_PLAN_ID,
                            title = "Retired-owner task",
                            projectId = RETIRED_OWNER_ID,
                            createdAt = 1L,
                            updatedAt = 1L,
                            version = 1L,
                        ),
                    ),
                )
                source.tacticalMissionDao().insertMission(
                    TacticalMission(
                        id = RETIRED_TACTICAL_MISSION_ID,
                        title = "Retired-owner tactical mission",
                        description = null,
                        deadline = NO_DEADLINE,
                        projectId = RETIRED_OWNER_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(
                    DAY_TASK_STANDALONE_ID,
                    bundle.dayTasks.single { it.id == DAY_TASK_STANDALONE_ID }.projectId,
                )
                assertEquals(
                    RETIRED_OWNER_ID,
                    bundle.dayTasks.single { it.id == DAY_TASK_RETIRED_ID }.projectId,
                )
                assertEquals(
                    RETIRED_OWNER_ID,
                    bundle.tacticalMissions.single { it.id == RETIRED_TACTICAL_MISSION_ID }.projectId,
                )

                restore(destination, bundle)

                val standaloneTask =
                    requireNotNull(destination.dayTaskDao().getTaskById(DAY_TASK_STANDALONE_ID))
                assertNull(standaloneTask.projectId)
                assertEquals(DAY_TASK_STANDALONE_ID, standaloneTask.projectWorkspaceId)
                assertEquals(DAY_TASK_STANDALONE_ID, standaloneTask.logicalProjectId)

                val retiredTask =
                    requireNotNull(destination.dayTaskDao().getTaskById(DAY_TASK_RETIRED_ID))
                assertNull(retiredTask.projectId)
                assertEquals(RETIRED_OWNER_ID, retiredTask.projectWorkspaceId)
                assertEquals(RETIRED_OWNER_ID, retiredTask.logicalProjectId)

                val mission =
                    requireNotNull(destination.tacticalMissionDao().getMissionById(RETIRED_TACTICAL_MISSION_ID))
                assertNull(mission.projectId)
                assertEquals(RETIRED_OWNER_ID, mission.projectWorkspaceId)
                assertEquals(RETIRED_OWNER_ID, mission.logicalProjectId)

                val exportedAgain = fullBackup(destination).loadFullSnapshotBundle()
                assertEquals(
                    DAY_TASK_STANDALONE_ID,
                    exportedAgain.dayTasks.single { it.id == DAY_TASK_STANDALONE_ID }.projectId,
                )
                assertEquals(
                    RETIRED_OWNER_ID,
                    exportedAgain.dayTasks.single { it.id == DAY_TASK_RETIRED_ID }.projectId,
                )
                assertEquals(
                    RETIRED_OWNER_ID,
                    exportedAgain.tacticalMissions.single { it.id == RETIRED_TACTICAL_MISSION_ID }.projectId,
                )
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `full restore does not legalize arbitrary canonical-only project owners`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                source.workspaceDao().upsert(listOf(canonicalWorkspace(ARBITRARY_CANONICAL_ONLY_ID)))
                source.dayPlanDao().insert(
                    DayPlan(
                        id = DAY_TASK_PLAN_ID,
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )
                source.dayTaskDao().insertRaw(
                    DayTask(
                        id = ARBITRARY_CANONICAL_ONLY_ID,
                        dayPlanId = DAY_TASK_PLAN_ID,
                        title = "Invalid canonical owner",
                        projectId = null,
                        projectWorkspaceId = ARBITRARY_CANONICAL_ONLY_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                restore(destination, fullBackup(source).loadFullSnapshotBundle())

                val restored =
                    requireNotNull(destination.dayTaskDao().getTaskById(ARBITRARY_CANONICAL_ONLY_ID))
                assertNull(restored.projectId)
                assertNull(restored.projectWorkspaceId)
                assertNull(restored.logicalProjectId)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `normal merge rejects unproven DayTask Workspace owners atomically`() =
        runBlocking {
            val source = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                source.dayPlanDao().insert(
                    DayPlan(
                        id = DAY_TASK_PLAN_ID,
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val validId = "merge-rollback-valid-daytask"
                val rejectedId = "merge-rollback-invalid-daytask"

                source.dayTaskDao().insertTasks(
                    listOf(
                        DayTask(
                            id = validId,
                            dayPlanId = DAY_TASK_PLAN_ID,
                            title = "Valid task preceding rejected owner",
                            createdAt = 1L,
                            updatedAt = 1L,
                            version = 1L,
                        ),
                        DayTask(
                            id = rejectedId,
                            dayPlanId = DAY_TASK_PLAN_ID,
                            title = "Task with rejected owner",
                            createdAt = 1L,
                            updatedAt = 1L,
                            version = 1L,
                        ),
                    ),
                )

                val baseline = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(2, baseline.dayTasks.size)
                assertTrue(baseline.dayTasks.all { it.projectId == null })

                val cases =
                    listOf(
                        Triple(
                            "missing",
                            "merge-missing-daytask-owner",
                            DayTaskOwnerRejection.MISSING_WORKSPACE,
                        ),
                        Triple(
                            "deleted",
                            "merge-deleted-daytask-owner",
                            DayTaskOwnerRejection.DELETED_WORKSPACE,
                        ),
                        Triple(
                            "unproven-live",
                            "merge-unproven-daytask-owner",
                            DayTaskOwnerRejection.UNPROVEN_LIVE_CONTEXT_MAPPING,
                        ),
                    )

                cases.forEach { (caseName, ownerId, expectedReason) ->
                    val destination = database()
                    try {
                        initializer(destination).ensureCanonicalSystemWorkspaceOwnership()

                        when (caseName) {
                            "missing" -> Unit
                            "deleted" -> {
                                destination.workspaceDao().upsert(
                                    listOf(
                                        canonicalWorkspace(ownerId).copy(
                                            isDeleted = true,
                                        ),
                                    ),
                                )
                            }
                            "unproven-live" -> {
                                destination.contextDao().insertContexts(
                                    listOf(
                                        retiredContext(ownerId).copy(
                                            isDeleted = false,
                                        ),
                                    ),
                                )
                                destination.workspaceDao().upsert(
                                    listOf(canonicalWorkspace(ownerId)),
                                )
                            }
                            else -> error("Unexpected test case: $caseName")
                        }

                        destination.dayPlanDao().insert(
                            DayPlan(
                                id = "merge-local-survivor-plan",
                                date = 2L,
                                createdAt = 1L,
                                updatedAt = 1L,
                                version = 1L,
                            ),
                        )
                        destination.dayTaskDao().insert(
                            DayTask(
                                id = "merge-local-survivor-task",
                                dayPlanId = "merge-local-survivor-plan",
                                title = "Existing local task",
                                createdAt = 1L,
                                updatedAt = 1L,
                                version = 1L,
                            ),
                        )

                        val plansBefore =
                            destination.dayPlanDao().getAllPlansSync()
                        val tasksBefore =
                            destination.dayTaskDao().getAllTasksSync()

                        val incoming =
                            baseline.copy(
                                dayTasks =
                                    baseline.dayTasks.map { task ->
                                        if (task.id == rejectedId) {
                                            task.copy(projectId = ownerId)
                                        } else {
                                            task
                                        }
                                    },
                            )

                        val failure =
                            runCatching {
                                merge(destination).applySnapshotBundle(incoming)
                            }.exceptionOrNull()

                        assertTrue(
                            "$caseName expected DayTask owner rejection, " +
                                "got ${failure?.javaClass?.name}: ${failure?.message}",
                            failure is DayTaskWorkspaceOwnerRejected,
                        )
                        assertEquals(
                            "$caseName rejection reason",
                            expectedReason,
                            (failure as DayTaskWorkspaceOwnerRejected).reason,
                        )

                        assertEquals(
                            "$caseName must roll back incoming DayPlan",
                            plansBefore,
                            destination.dayPlanDao().getAllPlansSync(),
                        )
                        assertEquals(
                            "$caseName must preserve existing tasks and " +
                                "roll back both incoming tasks",
                            tasksBefore,
                            destination.dayTaskDao().getAllTasksSync(),
                        )
                        assertNull(
                            destination.dayTaskDao().getTaskById(validId),
                        )
                        assertNull(
                            destination.dayTaskDao().getTaskById(rejectedId),
                        )
                    } finally {
                        destination.close()
                    }
                }
            } finally {
                source.close()
            }
        }

    @Test
    fun `full restore neutralizes missing DayTask Workspace owner without dropping task`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                val taskId = "restore-missing-owner-daytask"
                val missingOwner = "restore-missing-workspace"

                source.dayPlanDao().insert(
                    DayPlan(
                        id = DAY_TASK_PLAN_ID,
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )
                source.dayTaskDao().insert(
                    DayTask(
                        id = taskId,
                        dayPlanId = DAY_TASK_PLAN_ID,
                        title = "Task survives invalid owner",
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val exported = fullBackup(source).loadFullSnapshotBundle()
                val incoming =
                    exported.copy(
                        dayTasks =
                            exported.dayTasks.map { task ->
                                if (task.id == taskId) {
                                    task.copy(projectId = missingOwner)
                                } else {
                                    task
                                }
                            },
                    )

                assertEquals(
                    missingOwner,
                    incoming.dayTasks.single { it.id == taskId }.projectId,
                )
                assertTrue(
                    incoming.workspaces.orEmpty().none {
                        it.id == missingOwner
                    },
                )

                restore(destination, incoming)

                val restored =
                    requireNotNull(
                        destination.dayTaskDao().getTaskById(taskId),
                    )
                assertEquals(
                    "Task survives invalid owner",
                    restored.title,
                )
                assertNull(restored.projectId)
                assertNull(restored.projectWorkspaceId)
                assertNull(restored.logicalProjectId)

                val exportedAgain =
                    fullBackup(destination)
                        .loadFullSnapshotBundle()
                        .dayTasks
                        .single { it.id == taskId }

                assertNull(exportedAgain.projectId)
                assertEquals(restored.title, exportedAgain.title)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `normal merge rejects invalid TacticalMission Workspace owners atomically`() =
        runBlocking {
            val source = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                val validId = 97001L
                val rejectedId = 97002L
                val survivorId = 97003L

                source.dayPlanDao().insert(
                    DayPlan(
                        id = "merge-tactical-incoming-plan",
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                source.tacticalMissionDao().insertMissions(
                    listOf(
                        TacticalMission(
                            id = validId,
                            title = "Valid incoming mission",
                            description = null,
                            deadline = NO_DEADLINE,
                            projectId = null,
                            createdAt = 1L,
                            updatedAt = 1L,
                            version = 1L,
                        ),
                        TacticalMission(
                            id = rejectedId,
                            title = "Mission with rejected owner",
                            description = null,
                            deadline = NO_DEADLINE,
                            projectId = null,
                            createdAt = 1L,
                            updatedAt = 1L,
                            version = 1L,
                        ),
                    ),
                )

                val baseline = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(
                    setOf(validId, rejectedId),
                    baseline.tacticalMissions.map { it.id }.toSet(),
                )

                val cases =
                    listOf(
                        Triple(
                            "missing",
                            "merge-missing-tactical-owner",
                            DayTaskOwnerRejection.MISSING_WORKSPACE,
                        ),
                        Triple(
                            "deleted",
                            "merge-deleted-tactical-owner",
                            DayTaskOwnerRejection.DELETED_WORKSPACE,
                        ),
                        Triple(
                            "unproven-live",
                            "merge-unproven-tactical-owner",
                            DayTaskOwnerRejection.UNPROVEN_LIVE_CONTEXT_MAPPING,
                        ),
                    )

                cases.forEach { (caseName, ownerId, expectedReason) ->
                    val destination = database()
                    try {
                        initializer(destination)
                            .ensureCanonicalSystemWorkspaceOwnership()

                        when (caseName) {
                            "missing" -> Unit

                            "deleted" -> {
                                destination.workspaceDao().upsert(
                                    listOf(
                                        canonicalWorkspace(ownerId)
                                            .copy(isDeleted = true),
                                    ),
                                )
                            }

                            "unproven-live" -> {
                                destination.contextDao().insertContexts(
                                    listOf(
                                        retiredContext(ownerId)
                                            .copy(isDeleted = false),
                                    ),
                                )
                                destination.workspaceDao().upsert(
                                    listOf(canonicalWorkspace(ownerId)),
                                )
                            }

                            else -> error("Unexpected test case: $caseName")
                        }

                        destination.dayPlanDao().insert(
                            DayPlan(
                                id = "merge-tactical-local-plan",
                                date = 2L,
                                createdAt = 1L,
                                updatedAt = 1L,
                                version = 1L,
                            ),
                        )

                        destination.tacticalMissionDao().insertMission(
                            TacticalMission(
                                id = survivorId,
                                title = "Existing local mission",
                                description = null,
                                deadline = NO_DEADLINE,
                                projectId = null,
                                createdAt = 1L,
                                updatedAt = 1L,
                                version = 1L,
                            ),
                        )

                        val plansBefore =
                            destination.dayPlanDao().getAllPlansSync()
                        val missionsBefore =
                            destination.tacticalMissionDao()
                                .getAllMissionsSync()

                        val incoming =
                            baseline.copy(
                                tacticalMissions =
                                    baseline.tacticalMissions.map { mission ->
                                        if (mission.id == rejectedId) {
                                            mission.copy(projectId = ownerId)
                                        } else {
                                            mission
                                        }
                                    },
                            )

                        val failure =
                            runCatching {
                                merge(destination).applySnapshotBundle(incoming)
                            }.exceptionOrNull()

                        assertTrue(
                            "$caseName expected TacticalMission owner rejection, " +
                                "got ${failure?.javaClass?.name}: ${failure?.message}",
                            failure is DayTaskWorkspaceOwnerRejected,
                        )

                        assertEquals(
                            "$caseName rejection reason",
                            expectedReason,
                            (failure as DayTaskWorkspaceOwnerRejected).reason,
                        )

                        assertEquals(
                            "$caseName must roll back incoming DayPlan",
                            plansBefore,
                            destination.dayPlanDao().getAllPlansSync(),
                        )

                        assertEquals(
                            "$caseName must preserve local mission and roll back incoming missions",
                            missionsBefore,
                            destination.tacticalMissionDao()
                                .getAllMissionsSync(),
                        )

                        assertNull(
                            destination.tacticalMissionDao()
                                .getMissionById(validId),
                        )
                        assertNull(
                            destination.tacticalMissionDao()
                                .getMissionById(rejectedId),
                        )
                    } finally {
                        destination.close()
                    }
                }
            } finally {
                source.close()
            }
        }

    @Test
    fun `merge installs System Workspace before routing reserved TacticalMission project`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                initializer(source).ensureCanonicalSystemWorkspaceOwnership()

                source.tacticalMissionDao().insertMission(
                    TacticalMission(
                        id = TACTICAL_MISSION_ID,
                        title = "Merged system-owned tactical mission",
                        description = null,
                        deadline = NO_DEADLINE,
                        projectId = TACTICAL_SYSTEM_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val bundle = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(
                    TACTICAL_SYSTEM_ID,
                    bundle.tacticalMissions
                        .single { it.id == TACTICAL_MISSION_ID }
                        .projectId,
                )

                merge(destination).applySnapshotBundle(bundle)

                val merged =
                    requireNotNull(
                        destination.tacticalMissionDao().getMissionById(TACTICAL_MISSION_ID),
                    )
                assertNull(merged.projectId)
                assertEquals(TACTICAL_SYSTEM_ID, merged.projectWorkspaceId)
                assertEquals(TACTICAL_SYSTEM_ID, merged.logicalProjectId)

                val exportedAgain =
                    fullBackup(destination)
                        .loadFullSnapshotBundle()
                        .tacticalMissions
                        .single { it.id == TACTICAL_MISSION_ID }

                assertEquals(TACTICAL_SYSTEM_ID, exportedAgain.projectId)
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `restore canonicalizes pre canonical System tags into Workspace membership`() =
        runBlocking {
            val database = database()
            try {
                val sourceContext = historicalSystemContext()
                val legacy =
                    SnapshotBundle(
                        version = 1,
                        contexts = listOf(sourceContext.copy(tags = listOf("#Legacy")).toSnapshot()),
                    )

                restore(database, legacy)
                assertReservedSystemShellsAbsent(database)

                assertEquals(
                    listOf("legacy"),
                    CanonicalWorkspaceTagRepository(database).getTags(SYSTEM_ID),
                )
                assertEquals(
                    listOf("legacy"),
                    database.workspaceTagRefDao()
                        .getAllForWorkspace(SYSTEM_ID)
                        .filterNot { it.isDeleted }
                        .map { it.normalizedTag },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `merge freshness wins before legacy compatibility refresh and remains canonical`() =
        runBlocking {
            val database = database()
            try {
                initializer(database).ensureCanonicalSystemWorkspaceOwnership()
                val local = seedCanonicalCapabilities(database)
                val localInbox = local.getValue(WorkspaceCapabilityType.INBOX)
                    .copy(state = WorkspaceCapabilityState.DISABLED.name, version = 5L, updatedAt = 500L)
                val localDirection = local.getValue(WorkspaceCapabilityType.DIRECTION)
                    .copy(state = WorkspaceCapabilityState.ACTIVE.name, version = 5L, updatedAt = 500L)
                val localBacklog = local.getValue(WorkspaceCapabilityType.BACKLOG)
                    .copy(state = WorkspaceCapabilityState.DISABLED.name, version = 6L, updatedAt = 100L)
                val localConnections = local.getValue(WorkspaceCapabilityType.CONNECTIONS)
                    .copy(state = WorkspaceCapabilityState.ACTIVE.name, version = 5L, updatedAt = 900L)
                database.orientationDao().upsertWorkspaceCapabilities(
                    listOf(localInbox, localDirection, localBacklog, localConnections),
                )

                val incomingInbox =
                    localInbox.copy(
                        state = WorkspaceCapabilityState.ACTIVE.name,
                        updatedAt = 400L,
                    )
                val incomingDirection =
                    localDirection.copy(
                        state = WorkspaceCapabilityState.DISABLED.name,
                        configuration =
                            DirectionCapabilityConfigurationCodec.encode(
                                DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false),
                            ),
                        updatedAt = 600L,
                    )
                val incomingBacklog =
                    localBacklog.copy(
                        state = WorkspaceCapabilityState.ACTIVE.name,
                        version = 5L,
                        updatedAt = 1_000L,
                    )
                val incomingConnections =
                    localConnections.copy(
                        state = WorkspaceCapabilityState.DISABLED.name,
                        version = 6L,
                        updatedAt = 100L,
                    )
                val bundle =
                    canonicalBundle(
                        database = database,
                        capabilities =
                            listOf(
                                incomingInbox,
                                incomingDirection,
                                incomingBacklog,
                                incomingConnections,
                            ),
                        legacyConfiguration = contradictoryLegacyConfiguration(),
                    )

                merge(database).applySnapshotBundle(bundle)

                val persisted =
                    database.orientationDao().getAllWorkspaceCapabilities().associateBy { it.id }
                assertEquals(localInbox, persisted.getValue(localInbox.id))
                assertEquals(incomingDirection, persisted.getValue(incomingDirection.id))
                assertEquals(localBacklog, persisted.getValue(localBacklog.id))
                assertEquals(incomingConnections, persisted.getValue(incomingConnections.id))
                assertNull(database.contextStructureDao().getStructureByContext(SYSTEM_ID))
            } finally {
                database.close()
            }
        }

    @Test
    fun `pre-canonical backup seeds missing System capability but cannot overwrite established state`() =
        runBlocking {
            val database = database()
            try {
                initializer(database).ensureCanonicalSystemWorkspaceOwnership()
                val existing =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .singleOrNull {
                            it.workspaceId == SYSTEM_ID &&
                                it.capabilityType == WorkspaceCapabilityType.INBOX.name
                        }
                assertNull(existing)
                val legacyBundle = legacyBundle(database, contradictoryLegacyConfiguration())

                merge(database).applySnapshotBundle(legacyBundle)

                val seeded = inbox(database)
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, seeded.state)
                assertNull(database.contextStructureDao().getStructureByContext(SYSTEM_ID))
                val disabled =
                    seeded.copy(
                        state = WorkspaceCapabilityState.DISABLED.name,
                        version = seeded.version + 1L,
                        updatedAt = seeded.updatedAt + 1L,
                        syncedAt = null,
                    )
                database.orientationDao().upsertWorkspaceCapabilities(listOf(disabled))

                merge(database).applySnapshotBundle(legacyBundle)

                assertEquals(disabled, inbox(database))
                assertNull(database.contextStructureDao().getStructureByContext(SYSTEM_ID))
            } finally {
                database.close()
            }
        }

    @Test
    fun `normal merge rejects historical nonreserved sys Context transport`() = runBlocking {
        val database = database()
        try {
            val contextId = "sys_custom"
            val context = historicalSystemContext().copy(id = contextId, name = "Historical ordinary context")
            val configuration =
                contradictoryLegacyConfiguration().copy(
                    id = "transport-config-$contextId",
                    contextId = contextId,
                )

            val failure =
                runCatching {
                    merge(database).applySnapshotBundle(
                        SnapshotBundle(
                            version = 1,
                            contexts = listOf(context.toSnapshot()),
                            contextConfigurations = listOf(configuration.toSnapshot()),
                            hierarchyPlacements = emptyList(),
                            hierarchyPlacementGroupScopes = emptyList(),
                            hierarchyPlacementLinkedAppearances = emptyList(),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertNull(database.contextDao().getContextById(contextId))
            assertNull(database.contextStructureDao().getStructureByContext(contextId))
        } finally {
            database.close()
        }
    }

    @Test
    fun `normal merge rejects ordinary Context configuration without Context row`() = runBlocking {
        val database = database()
        try {
            val contextId = "ordinary-config-only"
            val configuration =
                contradictoryLegacyConfiguration().copy(
                    id = "transport-config-$contextId",
                    contextId = contextId,
                )

            val failure =
                runCatching {
                    merge(database).applySnapshotBundle(
                        SnapshotBundle(
                            version = 1,
                            contextConfigurations = listOf(configuration.toSnapshot()),
                            hierarchyPlacements = emptyList(),
                            hierarchyPlacementGroupScopes = emptyList(),
                            hierarchyPlacementLinkedAppearances = emptyList(),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(
                failure!!.message.orEmpty().contains(
                    "Canonical merge ingress refuses ordinary Context configuration persistence",
                ),
            )
            assertNull(database.contextDao().getContextById(contextId))
            assertNull(database.contextStructureDao().getStructureByContext(contextId))
        } finally {
            database.close()
        }
    }

    @Test
    fun `normal merge rejects ordinary Context structure item without configuration`() = runBlocking {
        val database = database()
        try {
            val structureId = "ordinary-structure"
            val itemId = "ordinary-structure-item"

            val failure =
                runCatching {
                    merge(database).applySnapshotBundle(
                        SnapshotBundle(
                            version = 1,
                            projectStructureItems =
                                listOf(
                                    com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextStructureItemSnapshot(
                                        id = itemId,
                                        contextStructureId = structureId,
                                        entityType = "GOAL",
                                        roleCode = "GOAL",
                                        containerType = null,
                                        title = "Legacy structure item",
                                        mandatory = false,
                                        isEnabled = true,
                                        version = 1L,
                                        updatedAt = 1L,
                                        isDeleted = false,
                                    ),
                                ),
                            hierarchyPlacements = emptyList(),
                            hierarchyPlacementGroupScopes = emptyList(),
                            hierarchyPlacementLinkedAppearances = emptyList(),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(
                failure!!.message.orEmpty().contains(
                    "Canonical merge ingress refuses ordinary Context structure persistence",
                ),
            )
            assertNull(database.contextStructureDao().getStructureById(structureId))
            assertTrue(database.contextStructureDao().getItems(structureId).isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `planning restore null preserves dormant canonical state`() = runBlocking {
        val database = database()
        try {
            seedPlanningOrientation(database)
            val store = CanonicalPlanningSyncStore(database)
            store.restoreExactDecoded(
                store.decodeAndValidateForRestore(
                    scopes = listOf(planningScope(syncedAt = 51L)),
                    commitments = listOf(planningCommitment(syncedAt = 61L)),
                    liveOrientationIds = setOf(PLANNING_ORIENTATION_ID),
                ),
            )

            restore(database, planningBundle())

            assertEquals(51L, store.loadAllScopes().single().syncedAt)
            assertEquals(61L, store.loadAllCommitments().single().syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `planning restore explicit empty authoritatively clears dormant state`() = runBlocking {
        val database = database()
        try {
            seedPlanningOrientation(database)
            val store = CanonicalPlanningSyncStore(database)
            store.restoreExactDecoded(
                store.decodeAndValidateForRestore(
                    scopes = listOf(planningScope()),
                    commitments = listOf(planningCommitment()),
                    liveOrientationIds = setOf(PLANNING_ORIENTATION_ID),
                ),
            )

            restore(
                database,
                planningBundle(
                    scopes = emptyList(),
                    commitments = emptyList(),
                ),
            )

            assertTrue(store.loadAllScopes().isEmpty())
            assertTrue(store.loadAllCommitments().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `Strategic extension restore explicit empty is authoritative while scope remains`() = runBlocking {
        val database = database()
        try {
            val planningStore = CanonicalPlanningSyncStore(database)
            val decoded =
                planningStore.decodeAndValidateForRestore(
                    scopes = listOf(planningScope(kind = "STRATEGIC_ARC")),
                    commitments = emptyList(),
                    liveOrientationIds = emptySet(),
                )
            planningStore.restoreExactDecoded(decoded)
            val strategicStore = CanonicalStrategicPlanningScopeExtensionSyncStore(database)
            strategicStore.restoreExactDecoded(
                strategicStore.decodeAndValidateForRestore(
                    snapshots = listOf(strategicPlanningExtension()),
                    scopes = decoded.scopes,
                ),
            )

            restore(
                database,
                planningBundle(
                    scopes = listOf(planningScope(kind = "STRATEGIC_ARC")),
                    commitments = emptyList(),
                    strategicExtensions = emptyList(),
                ),
            )

            assertEquals(1, planningStore.loadAllScopes().size)
            assertTrue(strategicStore.loadAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `planning v3 restore preserves unsupported local Strategic extension`() = runBlocking {
        val database = database()
        try {
            val planningStore = CanonicalPlanningSyncStore(database)
            val decoded =
                planningStore.decodeAndValidateForRestore(
                    scopes = listOf(planningScope(kind = "STRATEGIC_ARC")),
                    commitments = emptyList(),
                    liveOrientationIds = emptySet(),
                )
            planningStore.restoreExactDecoded(decoded)
            val strategicStore = CanonicalStrategicPlanningScopeExtensionSyncStore(database)
            strategicStore.restoreExactDecoded(
                strategicStore.decodeAndValidateForRestore(
                    snapshots = listOf(strategicPlanningExtension()),
                    scopes = decoded.scopes,
                ),
            )

            val v3Bundle =
                planningBundle(
                    scopes = listOf(planningScope(kind = "STRATEGIC_ARC")),
                    commitments = emptyList(),
                ).copy(
                    planningFormatVersion = TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION,
                    strategicPlanningScopeExtensions = null,
                )
            restore(database, v3Bundle)

            assertEquals(listOf(strategicPlanningExtension()), strategicStore.loadAll())
        } finally {
            database.close()
        }
    }

    @Test
    fun `planning populated restore preserves exact version tombstone and syncedAt`() = runBlocking {
        val database = database()
        try {
            restore(
                database,
                planningBundle(
                    scopes = listOf(planningScope(version = 7L, syncedAt = 71L)),
                    commitments =
                        listOf(
                            planningCommitment(
                                version = 8L,
                                syncedAt = 81L,
                                deleted = true,
                            ),
                        ),
                ),
            )

            val store = CanonicalPlanningSyncStore(database)
            val scope = store.loadAllScopes().single()
            val commitment = store.loadAllCommitments().single()
            assertEquals(7L, scope.version)
            assertEquals(71L, scope.syncedAt)
            assertEquals(8L, commitment.version)
            assertEquals(81L, commitment.syncedAt)
            assertTrue(commitment.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `day planning extension restore preserves typed state and exact metadata`() = runBlocking {
        val database = database()
        try {
            restore(
                database,
                planningBundle(
                    scopes = listOf(planningScope(kind = "DAY")),
                    commitments = emptyList(),
                    dayExtensions = listOf(dayPlanningExtension()),
                ),
            )

            val extension = CanonicalDayPlanningScopeExtensionSyncStore(database).loadAll().single()
            assertEquals(PLANNING_SCOPE_ID, extension.scopeId)
            assertEquals(1_735_689_600_000L, extension.calendarDayStartMillis)
            assertEquals("MISSED", extension.outcome)
            assertEquals(420L, extension.plannedBudgetMinutes)
            assertEquals(9L, extension.version)
            assertEquals(91L, extension.syncedAt)
            assertTrue(extension.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `day planning extension restore rejects missing scope dependency`() = runBlocking {
        val database = database()
        try {
            val failure =
                runCatching {
                    restore(
                        database,
                        planningBundle(
                            scopes = emptyList(),
                            commitments = emptyList(),
                            dayExtensions = listOf(dayPlanningExtension()),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(CanonicalDayPlanningScopeExtensionSyncStore(database).loadAll().isEmpty())
            assertTrue(CanonicalPlanningSyncStore(database).loadAllScopes().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `Tactical planning extension restore preserves typed state and exact metadata`() = runBlocking {
        val database = database()
        try {
            restore(
                database,
                planningBundle(
                    scopes = listOf(planningScope()),
                    commitments = emptyList(),
                    tacticalExtensions = listOf(tacticalPlanningExtension()),
                ),
            )

            val extension = CanonicalTacticalPlanningScopeExtensionSyncStore(database).loadAll().single()
            assertEquals(PLANNING_SCOPE_ID, extension.scopeId)
            assertEquals("TIMEBOXED", extension.iterationType)
            assertEquals("2026-W41", extension.weekKey)
            assertEquals(300L, extension.closedAt)
            assertEquals(10L, extension.version)
            assertEquals(101L, extension.syncedAt)
            assertTrue(extension.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `planning normal merge equal version divergence rolls back`() = runBlocking {
        val database = database()
        try {
            seedPlanningOrientation(database)
            val store = CanonicalPlanningSyncStore(database)
            store.mergeIncoming(
                scopes = listOf(planningScope(version = 4L, title = "local")),
                commitments = listOf(planningCommitment(version = 4L)),
            )

            val failure =
                runCatching {
                    merge(database).applySnapshotBundle(
                        planningBundle(
                            scopes = listOf(planningScope(version = 4L, title = "remote")),
                            commitments = listOf(planningCommitment(version = 4L)),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals("local", store.loadAllScopes().single().title)
            assertEquals(4L, store.loadAllScopes().single().version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `planning normal merge missing orientation dependency rolls back`() = runBlocking {
        val database = database()
        try {
            val failure =
                runCatching {
                    merge(database).applySnapshotBundle(
                        planningBundle(
                            includeOrientation = false,
                            scopes = listOf(planningScope()),
                            commitments =
                                listOf(
                                    planningCommitment(
                                        orientationId = "missing-planning-orientation",
                                    ),
                                ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(CanonicalPlanningSyncStore(database).loadAllScopes().isEmpty())
            assertTrue(CanonicalPlanningSyncStore(database).loadAllCommitments().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical sync includes and acknowledges System capability versions without legacy config`() =
        runBlocking {
            val database = database()
            try {
                initializer(database).ensureCanonicalSystemWorkspaceOwnership()
                val expected = seedCanonicalCapabilities(database).values.toList()
                database.contextStructureDao().deleteAllStructures()
                val orientationBootstrapper = mockk<CanonicalOrientationBootstrapper>(relaxed = true)
                val workspaceBootstrapper = mockk<CanonicalWorkspaceBootstrapper>(relaxed = true)
                val syncStore =
                    CanonicalOrientationSyncStore(
                        database = database,
                        dao = database.orientationDao(),
                        bootstrapper = orientationBootstrapper,
                        workspaceDao = database.workspaceDao(),
                        workspaceTagRefDao = database.workspaceTagRefDao(),
                        workspaceBootstrapper = workspaceBootstrapper,
                    )

                val payload = syncStore.loadUnsynced()

                assertEquals(
                    expected.associateBy { it.id },
                    payload.workspaceCapabilities.filter { it.workspaceId == SYSTEM_ID }.associateBy { it.id },
                )
                syncStore.markSynced(
                    CanonicalOrientationSyncAck(
                        workspaceCapabilities =
                            expected.map { CanonicalOrientationSyncVersion(it.id, it.version) },
                    ),
                )
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == SYSTEM_ID }
                    .forEach { assertNotNull(it.syncedAt) }
            } finally {
                database.close()
            }
        }

    private suspend fun seedPlanningOrientation(database: AppDatabase) {
        database.orientationDao().upsertManagedSubjects(listOf(planningManagedSubject()))
        database.orientationDao().upsertOrientations(listOf(planningOrientation()))
    }

    private fun planningBundle(
        includeOrientation: Boolean = true,
        scopes: List<PlanningScopeSnapshot>? = null,
        commitments: List<PlanningCommitmentSnapshot>? = null,
        dayExtensions: List<DayPlanningScopeExtensionSnapshot>? = null,
        tacticalExtensions: List<TacticalPlanningScopeExtensionSnapshot>? = null,
        strategicExtensions: List<StrategicPlanningScopeExtensionSnapshot>? = null,
    ): SnapshotBundle {
        val planningPresent =
            scopes != null || commitments != null || dayExtensions != null || tacticalExtensions != null ||
                strategicExtensions != null
        return SnapshotBundle(
            version = 2,
            contexts = emptyList(),
            managedSubjects = if (includeOrientation) listOf(planningManagedSubject()) else emptyList(),
            orientations = if (includeOrientation) listOf(planningOrientation()) else emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces = emptyList(),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
            workspaceBacklogEntries = emptyList(),
            workspaceInboxRecords = emptyList(),
            hierarchyPlacements = emptyList(),
            hierarchyPlacementGroupScopes = emptyList(),
            hierarchyPlacementLinkedAppearances = emptyList(),
            planningFormatVersion = if (planningPresent) CURRENT_PLANNING_FORMAT_VERSION else null,
            planningScopes = scopes,
            planningCommitments = commitments,
            dayPlanningScopeExtensions = if (planningPresent) dayExtensions.orEmpty() else null,
            tacticalPlanningScopeExtensions = if (planningPresent) tacticalExtensions.orEmpty() else null,
            strategicPlanningScopeExtensions = if (planningPresent) strategicExtensions.orEmpty() else null,
        )
    }

    private fun planningManagedSubject() =
        ManagedSubjectEntity(
            id = PLANNING_ORIENTATION_ID,
            subjectType = "ORIENTATION",
            title = PLANNING_ORIENTATION_ID,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun planningOrientation() =
        OrientationEntity(
            subjectId = PLANNING_ORIENTATION_ID,
            kind = "GOAL",
            lifecycle = null,
            lifecycleOrigin = "UNSET",
        )

    private fun planningScope(
        version: Long = 1L,
        title: String? = null,
        syncedAt: Long? = 50L,
        kind: String = "TACTICAL_CYCLE",
    ) = PlanningScopeSnapshot(
        id = PLANNING_SCOPE_ID,
        kind = kind,
        lifecycle = "ACTIVE",
        title = title,
        startsAt = 100L,
        endsAt = 200L,
        createdAt = 10L,
        updatedAt = 20L + version,
        syncedAt = syncedAt,
        version = version,
        isDeleted = false,
    )

    private fun planningCommitment(
        orientationId: String = PLANNING_ORIENTATION_ID,
        version: Long = 1L,
        syncedAt: Long? = 60L,
        deleted: Boolean = false,
    ) = PlanningCommitmentSnapshot(
        id = PLANNING_COMMITMENT_ID,
        scopeId = PLANNING_SCOPE_ID,
        orientationId = orientationId,
        role = "DAY.THEME",
        order = 0L,
        priorityLevel = null,
        status = "PLANNED",
        provenanceKind = "IMPORTED",
        provenanceSourceType = "transport-test",
        provenanceSourceId = "source-1",
        createdAt = 11L,
        updatedAt = 30L + version,
        syncedAt = syncedAt,
        version = version,
        isDeleted = deleted,
    )

    private fun dayPlanningExtension() =
        DayPlanningScopeExtensionSnapshot(
            scopeId = PLANNING_SCOPE_ID,
            calendarDayStartMillis = 1_735_689_600_000L,
            outcome = "MISSED",
            plannedBudgetMinutes = 420L,
            createdAt = 12L,
            updatedAt = 92L,
            syncedAt = 91L,
            version = 9L,
            isDeleted = true,
        )

    private fun tacticalPlanningExtension() =
        TacticalPlanningScopeExtensionSnapshot(
            scopeId = PLANNING_SCOPE_ID,
            iterationType = "TIMEBOXED",
            weekKey = "2026-W41",
            closedAt = 300L,
            createdAt = 12L,
            updatedAt = 102L,
            syncedAt = 101L,
            version = 10L,
            isDeleted = true,
        )

    private fun strategicPlanningExtension() =
        StrategicPlanningScopeExtensionSnapshot(
            scopeId = PLANNING_SCOPE_ID,
            legacyArcKey = " Exact/Arc Key ",
            createdAt = 12L,
            updatedAt = 112L,
            syncedAt = 111L,
            version = 11L,
            isDeleted = false,
        )

    private suspend fun assertReservedSystemShellsAbsent(database: AppDatabase) {
        val definitions = SystemOperationalDefinitions.all
        assertEquals(20, definitions.size)

        definitions.forEach { definition ->
            val context = database.contextDao().getContextById(definition.id)
            assertTrue(
                "Active reserved System Context shell survived convergence: ${definition.id}",
                context == null || context.isDeleted,
            )

            val workspace =
                requireNotNull(database.workspaceDao().getById(definition.id))
            assertFalse(
                "Reserved System Workspace deleted after shell extinction: ${definition.id}",
                workspace.isDeleted,
            )
            assertEquals("CANONICAL_ONLY", workspace.provenance)
            assertNull(workspace.sourceContextId)
        }
    }

    private suspend fun seedCanonicalCapabilities(
        database: AppDatabase,
    ): Map<WorkspaceCapabilityType, WorkspaceCapabilityInstanceEntity> {
        val existing =
            database.orientationDao().getAllWorkspaceCapabilities()
                .filter { it.workspaceId == SYSTEM_ID }
                .associateBy { it.capabilityType }
        val instances =
            TARGET_TYPES.mapIndexed { index, type ->
                val prior = existing[type.name]
                val (configurationVersion, configuration) = configuration(type)
                WorkspaceCapabilityInstanceEntity(
                    id = prior?.id ?: "system-transport-${type.name.lowercase()}",
                    workspaceId = SYSTEM_ID,
                    capabilityType = type.name,
                    instanceKey = "default",
                    capabilityOrder = index.toLong(),
                    state = WorkspaceCapabilityState.DISABLED.name,
                    configurationVersion = configurationVersion,
                    configuration = configuration,
                    createdAt = 10L + index,
                    updatedAt = 100L + index,
                    syncedAt = if (type == WorkspaceCapabilityType.KEY_PROBLEMS) 90L else null,
                    isDeleted = type == WorkspaceCapabilityType.KEY_PROBLEMS,
                    version = 3L + index,
                )
            }
        database.orientationDao().upsertWorkspaceCapabilities(instances)
        return instances.associateBy { it.capabilityType }.mapKeys { WorkspaceCapabilityType.valueOf(it.key) }
    }

    private fun configuration(type: WorkspaceCapabilityType): Pair<Int, String> =
        when (type) {
            WorkspaceCapabilityType.BACKLOG ->
                BacklogCapabilityConfigurationCodec.CURRENT_VERSION to
                    BacklogCapabilityConfigurationCodec.encode(
                        BacklogCapabilityConfigurationV2(removeEntryAfterTagAutocopy = true),
                    )
            WorkspaceCapabilityType.INBOX ->
                InboxCapabilityConfigurationCodec.CURRENT_VERSION to
                    InboxCapabilityConfigurationCodec.encode(
                        InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED),
                    )
            WorkspaceCapabilityType.INBOX_SORTING ->
                InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION to
                    InboxSortingCapabilityConfigurationCodec.encodeDefault()
            WorkspaceCapabilityType.KEY_PROBLEMS ->
                KeyProblemsCapabilityConfigurationCodec.CURRENT_VERSION to
                    KeyProblemsCapabilityConfigurationCodec.encodeDefault()
            WorkspaceCapabilityType.DIRECTION ->
                DirectionCapabilityConfigurationCodec.CURRENT_VERSION to
                    DirectionCapabilityConfigurationCodec.encode(
                        DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false),
                    )
            WorkspaceCapabilityType.DASHBOARD ->
                DashboardCapabilityConfigurationCodec.CURRENT_VERSION to
                    DashboardCapabilityConfigurationCodec.encodeDefault()
            WorkspaceCapabilityType.EXECUTION_LOG ->
                ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION to
                    ExecutionLogCapabilityConfigurationCodec.encodeDefault()
            WorkspaceCapabilityType.CONNECTIONS ->
                ConnectionsCapabilityConfigurationCodec.CURRENT_VERSION to
                    ConnectionsCapabilityConfigurationCodec.encodeDefault()
            else -> error("Reserved capability is outside System transport scope: $type")
        }

    private fun contradictoryLegacyConfiguration() =
        ContextConfiguration(
            id = "transport-config-$SYSTEM_ID",
            contextId = SYSTEM_ID,
            basePresetCode = "default",
            experimentalCapabilityIds =
                listOf(
                    CapabilityId("direction"),
                    CapabilityId("inbox_sorting"),
                    CapabilityId("key_problems"),
                ),
            enableInbox = true,
            enableLog = true,
            enableDashboard = true,
            enableBacklog = true,
            enableAttachments = true,
            enableAutoLinkSubprojects = true,
            removeInboxEntryAfterTagAutocopy = false,
            removeBacklogEntryAfterTagAutocopy = false,
            updatedAt = 1_000L,
            version = 20L,
        )

    private fun historicalSystemContext(
        tags: List<String>? = null,
    ) = ContextEntity(
        id = SYSTEM_ID,
        name = "Historical System Inbox",
        description = null,
        parentId = null,
        createdAt = 1L,
        updatedAt = 1L,
        tags = tags,
    )

    private suspend fun canonicalBundle(
        database: AppDatabase,
        capabilities: List<WorkspaceCapabilityInstanceEntity>,
        legacyConfiguration: ContextConfiguration,
    ): SnapshotBundle =
        SnapshotBundle(
            version = 2,
            contexts = listOf(historicalSystemContext().toSnapshot()),
            contextConfigurations = listOf(legacyConfiguration.toSnapshot()),
            managedSubjects = emptyList(),
            orientations = emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces =
                database.workspaceDao().getAll().map {
                    it.toWorkspaceSnapshot().withoutEmbeddedWorkspaceTopology()
                },
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = capabilities,
            savedOrientationViews = emptyList(),
            hierarchyPlacements = emptyList(),
            hierarchyPlacementGroupScopes = emptyList(),
            hierarchyPlacementLinkedAppearances = emptyList(),
        )

    private suspend fun legacyBundle(
        database: AppDatabase,
        legacyConfiguration: ContextConfiguration,
    ) = SnapshotBundle(
        version = 1,
        contexts = listOf(historicalSystemContext().toSnapshot()),
        contextConfigurations = listOf(legacyConfiguration.toSnapshot()),
        hierarchyPlacements = emptyList(),
        hierarchyPlacementGroupScopes = emptyList(),
        hierarchyPlacementLinkedAppearances = emptyList(),
    )

    private suspend fun inbox(database: AppDatabase): WorkspaceCapabilityInstanceEntity =
        database.orientationDao().getAllWorkspaceCapabilities().single {
            it.workspaceId == SYSTEM_ID && it.capabilityType == WorkspaceCapabilityType.INBOX.name
        }

    private fun initializer(database: AppDatabase): DatabaseInitializer =
        DatabaseInitializer(
            systemWorkspaceMaterializer =
                SystemWorkspaceMaterializer(
                    database,
                    database.contextDao(),
                    database.workspaceDao(),
                ),
            systemWorkspaceTagSeed =
                SystemWorkspaceTagSeed(
                    database,
                    database.contextDao(),
                ),
            systemContextShellRetirer =
                SystemContextShellRetirer(
                    database = database,
                    contextDao = database.contextDao(),
                ),
        )

    private fun fullBackup(database: AppDatabase): FullBackupLocalDataSourceImpl {
        val bootstrapper = workspaceBootstrapper(database)
        val writeThrough = ContextWorkspaceWriteThrough(bootstrapper)
        val initializer =
            DatabaseInitializer(
                systemWorkspaceMaterializer =
                    SystemWorkspaceMaterializer(
                        database,
                        database.contextDao(),
                        database.workspaceDao(),
                    ),
                systemWorkspaceTagSeed =
                    SystemWorkspaceTagSeed(
                        database,
                        database.contextDao(),
                    ),
                systemContextShellRetirer =
                    SystemContextShellRetirer(
                        database = database,
                        contextDao = database.contextDao(),
                    ),
            )
        val problemStore = mockk<CanonicalWorkspaceProblemSyncStore>(relaxed = true)
        coEvery { problemStore.loadAll() } returns CanonicalWorkspaceProblemSyncPayload()
        return construct(
            type = FullBackupLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    DayPlanDao::class.java to database.dayPlanDao(),
                    DayTaskDao::class.java to database.dayTaskDao(),
                    ContextDao::class.java to database.contextDao(),
                    ContextStructureDao::class.java to database.contextStructureDao(),
                    MainBeaconDao::class.java to database.mainBeaconDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    CanonicalWorkspaceBootstrapper::class.java to bootstrapper,
                    ContextWorkspaceWriteThrough::class.java to writeThrough,
                    DatabaseInitializer::class.java to initializer,
                    CanonicalWorkspaceProblemSyncStore::class.java to problemStore,
                    CanonicalWorkspaceTagTransportStore::class.java to CanonicalWorkspaceTagTransportStore(database),
                    SystemWorkspaceTagSeed::class.java to SystemWorkspaceTagSeed(database, database.contextDao()),
                ),
        )
    }

    private suspend fun restore(
        database: AppDatabase,
        bundle: SnapshotBundle,
    ) {
        val canonical = SnapshotRestoreCanonicalizerImpl().canonicalize(bundle)
        SnapshotRestoreLocalDataSourceImpl(
            database = database,
            writer =
                CanonicalSnapshotTransactionWriter {
                    merge(database).applyCanonicalSnapshotBundle(
                        bundle = it,
                        contextPersistenceMode = ContextPersistenceMode.RESTORE_COMPATIBILITY,
                    )
                },
            dayManagementRuntimeRepository =
                mockk<DayManagementRuntimeRepository>(relaxed = true),
            dayPlanningAuthorityActivator = mockk(relaxed = true),
            tacticalPlanningAuthorityActivator = mockk(relaxed = true),
            strategicPlanningAuthorityActivator = mockk(relaxed = true),
            canonicalScoringAuthorityActivator = mockk(relaxed = true),
        ).replaceWith(canonical)
    }

    private fun merge(database: AppDatabase): MergeLocalDataSourceImpl {
        val bootstrapper = workspaceBootstrapper(database)
        val systemWorkspaceMaterializer =
            SystemWorkspaceMaterializer(
                database = database,
                contextDao = database.contextDao(),
                workspaceDao = database.workspaceDao(),
            )
        val systemWorkspaceTagSeed =
            SystemWorkspaceTagSeed(
                database = database,
                contextDao = database.contextDao(),
            )
        val databaseInitializer =
            DatabaseInitializer(
                systemWorkspaceMaterializer = systemWorkspaceMaterializer,
                systemWorkspaceTagSeed = systemWorkspaceTagSeed,
                systemContextShellRetirer =
                    SystemContextShellRetirer(
                        database = database,
                        contextDao = database.contextDao(),
                    ),
            )
        val canonicalWorkspaceBacklogSyncStore =
            CanonicalWorkspaceBacklogSyncStore(
                database = database,
                entryDao = database.workspaceBacklogEntryDao(),
                workspaceDao = database.workspaceDao(),
                orientationDao = database.orientationDao(),
                targetValidator = CanonicalBacklogTargetValidator(database),
            )
        val canonicalWorkspaceInboxSyncStore =
            CanonicalWorkspaceInboxSyncStore(
                database = database,
                recordDao = database.workspaceInboxRecordDao(),
                workspaceDao = database.workspaceDao(),
                orientationDao = database.orientationDao(),
            )
        val canonicalWorkspaceTagTransportStore =
            CanonicalWorkspaceTagTransportStore(database)

        return construct(
            type = MergeLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    DayPlanDao::class.java to database.dayPlanDao(),
                    DayTaskDao::class.java to database.dayTaskDao(),
                    LegacyNoteDao::class.java to database.legacyNoteDao(),
                    NoteDocumentDao::class.java to database.noteDocumentDao(),
                    ChecklistDao::class.java to database.checklistDao(),
                    MusicNoteDao::class.java to database.musicNoteDao(),
                    LinkItemDao::class.java to database.linkItemDao(),
                    ContextDao::class.java to database.contextDao(),
                    ContextStructureDao::class.java to database.contextStructureDao(),
                    MainBeaconDao::class.java to database.mainBeaconDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    CanonicalWorkspaceBootstrapper::class.java to bootstrapper,
                    ContextWorkspaceWriteThrough::class.java to ContextWorkspaceWriteThrough(bootstrapper),
                    SystemWorkspaceMaterializer::class.java to systemWorkspaceMaterializer,
                    SystemWorkspaceTagSeed::class.java to systemWorkspaceTagSeed,
                    DatabaseInitializer::class.java to databaseInitializer,
                    CanonicalWorkspaceTagTransportStore::class.java to canonicalWorkspaceTagTransportStore,
                    CanonicalWorkspaceBacklogSyncStore::class.java to canonicalWorkspaceBacklogSyncStore,
                    CanonicalWorkspaceInboxSyncStore::class.java to canonicalWorkspaceInboxSyncStore,
                ),
        )
    }

    private fun workspaceBootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun <T : Any> construct(
        type: Class<T>,
        overrides: Map<Class<*>, Any>,
    ): T {
        // Ignore Kotlin synthetic/default-argument constructors.
        // Select the actual injectable constructor, as the Context
        // transport acceptance harness already does.
        val constructor =
            type.declaredConstructors.single { candidate ->
                !candidate.isSynthetic &&
                    candidate.parameterTypes.none {
                        it.name == "kotlin.jvm.internal.DefaultConstructorMarker"
                    }
            }
        constructor.isAccessible = true
        val arguments =
            constructor.parameterTypes.map { parameterType ->
                overrides[parameterType] ?: relaxedMock(parameterType)
            }.toTypedArray()
        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(*arguments) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun relaxedMock(type: Class<*>): Any =
        mockkClass(type.kotlin as KClass<Any>, relaxed = true)

    private fun database(): AppDatabase {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        database.openHelper.writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO hierarchy_establishment_origin(hierarchyId, origin)
            VALUES('${HierarchyId.GENERAL.value}', '${HierarchyEstablishmentOrigin.FRESH_NATIVE.name}')
            """.trimIndent(),
        )
        return database
    }

    private fun standaloneWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            provenance = WorkspaceProvenance.STANDALONE.name,
        )

    private fun canonicalWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun retiredContext(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = true,
        )

    private companion object {
        const val PLANNING_SCOPE_ID = "transport-planning-scope"
        const val PLANNING_COMMITMENT_ID = "transport-planning-commitment"
        const val PLANNING_ORIENTATION_ID = "transport-planning-orientation"
        val SYSTEM_ID: String = SystemContexts.INBOX.raw
        val TACTICAL_SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val TACTICAL_MISSION_ID = 9_001L
        const val TACTICAL_STANDALONE_ID = "standalone-tactical-owner"
        val DAY_TASK_SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val DAY_TASK_PLAN_ID = "system-transport-day-plan"
        const val DAY_TASK_ID = "system-transport-day-task"
        const val DAY_TASK_STANDALONE_ID = "standalone-day-task-owner"
        const val DAY_TASK_RETIRED_ID = "retired-day-task"
        const val RETIRED_OWNER_ID = "retired-operational-owner"
        const val RETIRED_TACTICAL_MISSION_ID = 9_002L
        const val ARBITRARY_CANONICAL_ONLY_ID = "arbitrary-canonical-only-owner"
        val TARGET_TYPES: List<WorkspaceCapabilityType> =
            orientationCapabilityRegistry
                .filter { it.availability == WorkspaceCapabilityAvailability.TARGET }
                .map { it.type }
    }
}
