package com.romankozak.forwardappmobile.core.sync

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
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceProblemSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagTransportStore
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemContextShellRetirer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
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

                fullBackup(destination).applySnapshotBundle(bundle)
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

                fullBackup(destination).applySnapshotBundle(bundle)

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

                fullBackup(destination).applySnapshotBundle(fullBackup(source).loadFullSnapshotBundle())

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

                fullBackup(destination).applySnapshotBundle(bundle)

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

                fullBackup(destination).applySnapshotBundle(bundle)

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
    fun `pre canonical tag payload seeds missing System membership once but cannot reclaim it`() =
        runBlocking {
            val database = database()
            try {
                initializer(database).ensureCanonicalSystemWorkspaceOwnership()
                val sourceContext = historicalSystemContext()
                val legacy =
                    SnapshotBundle(
                        version = 1,
                        contexts = listOf(sourceContext.copy(tags = listOf("#Legacy")).toSnapshot()),
                    )

                fullBackup(database).applySnapshotBundle(legacy)
                assertReservedSystemShellsAbsent(database)

                assertEquals(listOf("legacy"), CanonicalWorkspaceTagRepository(database).getTags(SYSTEM_ID))
                CanonicalWorkspaceTagRepository(database).replaceTags(
                    workspaceId = SYSTEM_ID,
                    tags = listOf("canonical"),
                    now = 300L,
                )

                fullBackup(database).applySnapshotBundle(
                    legacy.copy(
                        contexts = listOf(sourceContext.copy(tags = listOf("stale")).toSnapshot()),
                    ),
                )

                assertEquals(listOf("canonical"), CanonicalWorkspaceTagRepository(database).getTags(SYSTEM_ID))
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

                fullBackup(database).applySnapshotBundle(legacyBundle)

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

                fullBackup(database).applySnapshotBundle(legacyBundle)

                assertEquals(disabled, inbox(database))
                assertNull(database.contextStructureDao().getStructureByContext(SYSTEM_ID))
            } finally {
                database.close()
            }
        }

    @Test
    fun `historical nonreserved sys configuration remains ordinary transport input`() = runBlocking {
        val database = database()
        try {
            val contextId = "sys_custom"
            val context = historicalSystemContext().copy(id = contextId, name = "Historical ordinary context")
            val configuration =
                contradictoryLegacyConfiguration().copy(
                    id = "transport-config-$contextId",
                    contextId = contextId,
                )

            fullBackup(database).applySnapshotBundle(
                SnapshotBundle(
                    version = 1,
                    contexts = listOf(context.toSnapshot()),
                    contextConfigurations = listOf(configuration.toSnapshot()),
                ),
            )

            assertNotNull(database.contextDao().getContextById(contextId))
            val transportedConfiguration =
                requireNotNull(database.contextStructureDao().getStructureByContext(contextId))
            // The legacy transport DTO is non-nullable here: entity null normalizes to false on the wire.
            assertEquals(configuration.copy(enableAdvanced = false), transportedConfiguration)
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
            workspaces = database.workspaceDao().getAll(),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = capabilities,
            savedOrientationViews = emptyList(),
        )

    private suspend fun legacyBundle(
        database: AppDatabase,
        legacyConfiguration: ContextConfiguration,
    ) = SnapshotBundle(
        version = 1,
        contexts = listOf(historicalSystemContext().toSnapshot()),
        contextConfigurations = listOf(legacyConfiguration.toSnapshot()),
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

    private fun merge(database: AppDatabase): MergeLocalDataSourceImpl {
        val bootstrapper = workspaceBootstrapper(database)
        return construct(
            type = MergeLocalDataSourceImpl::class.java,
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
                    ContextWorkspaceWriteThrough::class.java to ContextWorkspaceWriteThrough(bootstrapper),
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
        val constructor = type.declaredConstructors.single()
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

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        val SYSTEM_ID: String = SystemContexts.INBOX.raw
        val TACTICAL_SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val TACTICAL_MISSION_ID = 9_001L
        val DAY_TASK_SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val DAY_TASK_PLAN_ID = "system-transport-day-plan"
        const val DAY_TASK_ID = "system-transport-day-task"
        val TARGET_TYPES: List<WorkspaceCapabilityType> =
            orientationCapabilityRegistry
                .filter { it.availability == WorkspaceCapabilityAvailability.TARGET }
                .map { it.type }
    }
}
