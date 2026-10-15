package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalActivitySlot
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.toWorkspaceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedWorkspaceTopology
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalScoringAuthorityActivator
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import com.romankozak.forwardappmobile.data.orientation.MainBeaconOrientationBridge
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementGroupScopeMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementGroupScopeSyncStore
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyTargetDeletedException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyTargetMissingException
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBacklogSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceInboxSyncStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceProblemSyncStore
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
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalActivitySlotDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.sync.SyncLogicHelper
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncPayload
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class ContextTransportAntiResurrectionRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `V2 merge consumes H1 and does not repopulate legacy structural link tables`() =
        runBlocking {
            val database = database()
            try {
                val parentContext = ordinaryContext("legacy-link-parent")
                val childContext = ordinaryContext("legacy-link-child")
                val parentBeacon = MainBeacon(id = "legacy-beacon-parent", title = "Parent")
                val childBeacon =
                    MainBeacon(
                        id = "legacy-beacon-child",
                        title = "Child",
                        order = 9L,
                    )
                database.contextDao().insertAll(listOf(parentContext, childContext))
                // V2 bootstrap must never create Context-backed Workspaces from
                // this compatibility fixture. Seed the pre-existing ownership
                // it may validate while the merge boundary itself is exercised.
                database.workspaceDao().upsert(
                    listOf(
                        contextBackedWorkspace(parentContext.id),
                        contextBackedWorkspace(childContext.id),
                    ),
                )
                SystemWorkspaceMaterializer(
                    database = database,
                    contextDao = database.contextDao(),
                    workspaceDao = database.workspaceDao(),
).materializeAll(now = 5L, seedMissingFactoryCapabilities = false)

                merge(database).applySnapshotBundle(
                    SnapshotBundle(
                        version = 2,
                        contextParentLinks =
                            listOf(
                                ContextParentLinkSnapshot(
                                    parentContextId = parentContext.id,
                                    childContextId = childContext.id,
                                    order = 1L,
                                    createdAt = 10L,
                                    updatedAt = 20L,
                                    syncedAt = null,
                                    isDeleted = false,
                                    version = 1L,
                                ),
                            ),
                        mainBeacons = listOf(parentBeacon.toSnapshot(), childBeacon.toSnapshot()),
                        mainBeaconParentLinks =
                            listOf(
                                MainBeaconParentLinkSnapshot(
                                    parentBeaconId = parentBeacon.id,
                                    childBeaconId = childBeacon.id,
                                    order = 1L,
                                    createdAt = 10L,
                                    updatedAt = 20L,
                                ),
                            ),
                        hierarchyPlacements = emptyList(),
                        hierarchyPlacementGroupScopes = emptyList(),
                        hierarchyPlacementLinkedAppearances = emptyList(),
                    ),
                )

                assertRetiredPhysicalHierarchyAbsent(database)
                val storedChild = requireNotNull(database.mainBeaconDao().getBeaconById(childBeacon.id))
                assertEquals(9L, storedChild.order)
            } finally {
                database.close()
            }
        }

    @Test
    fun `normal merge never persists legacy structural links even in pre cutover fixtures`() =
        runBlocking {
            val database = database()
            try {
                val parentContext = ordinaryContext("pre-cutover-link-parent")
                val childContext = ordinaryContext("pre-cutover-link-child")
                val parentBeacon = MainBeacon(id = "pre-cutover-beacon-parent", title = "Parent")
                val childBeacon = MainBeacon(id = "pre-cutover-beacon-child", title = "Child")
                database.contextDao().insertAll(listOf(parentContext, childContext))
                database.mainBeaconDao().insertBeacons(listOf(parentBeacon, childBeacon))
                val beforeHierarchy =
                    CanonicalHierarchyPlacementRepository(database).getLiveHierarchy()

                merge(database).applySnapshotBundle(
                    SnapshotBundle(
                        version = 2,
                        contextParentLinks =
                            listOf(
                                ContextParentLinkSnapshot(
                                    parentContextId = parentContext.id,
                                    childContextId = childContext.id,
                                    order = 1L,
                                    createdAt = 10L,
                                    updatedAt = 20L,
                                    syncedAt = null,
                                    isDeleted = false,
                                    version = 1L,
                                ),
                            ),
                        mainBeaconParentLinks =
                            listOf(
                                MainBeaconParentLinkSnapshot(
                                    parentBeaconId = parentBeacon.id,
                                    childBeaconId = childBeacon.id,
                                    order = 1L,
                                    createdAt = 10L,
                                    updatedAt = 20L,
                                ),
                            ),
                        hierarchyPlacements = emptyList(),
                        hierarchyPlacementGroupScopes = emptyList(),
                        hierarchyPlacementLinkedAppearances = emptyList(),
                    ),
                )

                assertRetiredPhysicalHierarchyAbsent(database)
                assertEquals(
                    beforeHierarchy,
                    CanonicalHierarchyPlacementRepository(database).getLiveHierarchy(),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `pre canonical restore canonicalizes content before atomic replacement`() =
        runBlocking {
            val destination = database()
            try {
                destination.workspaceDao().upsert(listOf(canonicalOnlyWorkspace("destination-only")))
                val legacy =
                    SnapshotBundle(
                        version = 2,
                        exportedAt = 100L,
                        contexts = listOf(ordinaryContext(RETIRED_ID).toSnapshot()),
                        notes =
                            listOf(
                                LegacyNoteSnapshot(
                                    id = "legacy-note",
                                    contextId = RETIRED_ID,
                                    title = "Note",
                                    content = "Body",
                                    createdAt = 10L,
                                    updatedAt = 20L,
                                    isDeleted = false,
                                    version = 1L,
                                ),
                            ),
                        backlogItems =
                            listOf(
                                BacklogItemSnapshot(
                                    id = "legacy-placement",
                                    contextId = RETIRED_ID,
                                    itemType = "NOTE",
                                    entityId = "legacy-note",
                                    order = 9L,
                                    updatedAt = 20L,
                                    version = 2L,
                                    isDeleted = false,
                                ),
                            ),
                        inbox =
                            listOf(
                                InboxRecordSnapshot(
                                    id = "legacy-inbox",
                                    contextId = RETIRED_ID,
                                    text = "Inbox",
                                    createdAt = 10L,
                                    order = -10L,
                                    updatedAt = 20L,
                                    hideInOwnerInbox = false,
                                    version = 3L,
                                    isDeleted = false,
                                ),
                            ),
                    )
                val canonical = SnapshotRestoreCanonicalizerImpl().canonicalize(legacy)
                val restore =
                    SnapshotRestoreLocalDataSourceImpl(
                        database = destination,
                        writer = CanonicalSnapshotTransactionWriter { merge(destination).applyCanonicalSnapshotBundle(
                            bundle = it,
                            contextPersistenceMode = ContextPersistenceMode.RESTORE_COMPATIBILITY,
                        ) },
                        dayManagementRuntimeRepository = mockk<DayManagementRuntimeRepository>(relaxed = true),
                        dayPlanningAuthorityActivator = mockk(relaxed = true),
                        tacticalPlanningAuthorityActivator = mockk(relaxed = true),
                        strategicPlanningAuthorityActivator = mockk(relaxed = true),
                        canonicalScoringAuthorityActivator = mockk(relaxed = true),
                    )

                restore.replaceWith(canonical)

                assertNull(destination.workspaceDao().getById("destination-only"))
                assertTrue(requireNotNull(destination.contextDao().getContextById(RETIRED_ID)).isDeleted)
                assertNotNull(destination.workspaceDao().getById(RETIRED_ID))
                assertTrue(destination.workspaceBacklogEntryDao().getAll().any { it.id == "legacy-placement" })
                assertTrue(destination.workspaceInboxRecordDao().getAll().any { it.id == "legacy-inbox" })

                val reExported = fullBackup(destination).loadFullSnapshotBundle()
                assertEquals(
                    com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_HIERARCHY_FORMAT_VERSION,
                    reExported.hierarchyFormatVersion,
                )
                assertNotNull(reExported.hierarchyPlacements)
                assertNotNull(reExported.hierarchyPlacementGroupScopes)
                assertNotNull(reExported.hierarchyPlacementLinkedAppearances)
            } finally {
                destination.close()
            }
        }

    @Test
    fun `canonical MainBeacon restore retains one Workspace association across repeat and export`() =
        runBlocking {
            val database = database()
            try {
                val ownerId = "restore-main-beacon-owner"
                val beaconId = "restore-main-beacon"
                val ownerRef =
                    MainBeaconContextCrossRefSnapshot(
                        beaconId = beaconId,
                        contextId = ownerId,
                        order = 23L,
                    )

                val bundle =
                    canonicalBundle(
                        context = ordinaryContext(ownerId).copy(isDeleted = true),
                        workspace = canonicalOnlyWorkspace(ownerId),
                    ).copy(
                        contexts = emptyList(),
                        workspaceBacklogEntries = emptyList(),
                        workspaceInboxRecords = emptyList(),
                        mainBeacons =
                            listOf(
                                MainBeacon(
                                    id = beaconId,
                                    title = "Restored Beacon",
                                ).toSnapshot(),
                            ),
                        mainBeaconContextCrossRefs = listOf(ownerRef),
                    )

                repeat(2) {
                    restore(database, bundle)

                    val physical =
                        database.mainBeaconDao().getAllWorkspaceCrossRefsSync()
                    assertEquals(1, physical.size)
                    assertEquals(beaconId, physical.single().beaconId)
                    assertEquals(ownerId, physical.single().workspaceId)
                    assertEquals(23L, physical.single().order)

                    assertTrue(
                        retiredMainBeaconTableAbsentForTransport(database),
                    )

                    val logical =
                        database.mainBeaconDao().getAllContextCrossRefsSync()
                    assertEquals(1, logical.size)
                    assertEquals(beaconId, logical.single().beaconId)
                    assertEquals(ownerId, logical.single().contextId)
                    assertEquals(23L, logical.single().order)

                    assertNotNull(
                        database.mainBeaconDao().getBeaconById(beaconId),
                    )
                }

                val exported = fullBackup(database).loadFullSnapshotBundle()
                assertEquals(
                    listOf(ownerRef),
                    exported.mainBeaconContextCrossRefs,
                )
                assertTrue(
                    retiredMainBeaconTableAbsentForTransport(database),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `invalid MainBeacon owner rolls back replacement and preceding valid owner`() =
        runBlocking {
            val database = database()
            try {
                val sentinelWorkspaceId = "restore-beacon-sentinel-owner"
                val sentinelBeaconId = "restore-beacon-sentinel"
                database.workspaceDao().upsert(
                    listOf(canonicalOnlyWorkspace(sentinelWorkspaceId)),
                )
                database.mainBeaconDao().insertBeacons(
                    listOf(
                        MainBeacon(
                            id = sentinelBeaconId,
                            title = "Existing Beacon",
                        ),
                    ),
                )

                val incomingWorkspaceId = "restore-beacon-incoming-owner"
                val incomingBeaconId = "restore-beacon-incoming"
                val missingWorkspaceId = "restore-beacon-missing-owner"

                val bundle =
                    canonicalBundle(
                        context = ordinaryContext(incomingWorkspaceId)
                            .copy(isDeleted = true),
                        workspace = canonicalOnlyWorkspace(incomingWorkspaceId),
                    ).copy(
                        contexts = emptyList(),
                        workspaceBacklogEntries = emptyList(),
                        workspaceInboxRecords = emptyList(),
                        mainBeacons =
                            listOf(
                                MainBeacon(
                                    id = incomingBeaconId,
                                    title = "Incoming Beacon",
                                ).toSnapshot(),
                            ),
                        mainBeaconContextCrossRefs =
                            listOf(
                                MainBeaconContextCrossRefSnapshot(
                                    beaconId = incomingBeaconId,
                                    contextId = incomingWorkspaceId,
                                    order = 0L,
                                ),
                                MainBeaconContextCrossRefSnapshot(
                                    beaconId = incomingBeaconId,
                                    contextId = missingWorkspaceId,
                                    order = 1L,
                                ),
                            ),
                    )

                val failure =
                    runCatching {
                        restore(database, bundle)
                    }.exceptionOrNull()

                assertTrue(
                    "Expected owner policy failure, got " +
                        "${failure?.javaClass?.name}: ${failure?.message}",
                    failure is IllegalArgumentException &&
                        failure.message.orEmpty()
                            .contains("Main Beacon Workspace owner"),
                )

                // The preceding table clear must have rolled back.
                assertNotNull(
                    database.workspaceDao().getById(sentinelWorkspaceId),
                )
                assertNotNull(
                    database.mainBeaconDao().getBeaconById(sentinelBeaconId),
                )

                // Neither the incoming rows nor the first valid association
                // may survive the failed outer Room transaction.
                assertNull(
                    database.workspaceDao().getById(incomingWorkspaceId),
                )
                assertNull(
                    database.mainBeaconDao().getBeaconById(incomingBeaconId),
                )
                assertTrue(
                    database.mainBeaconDao()
                        .getAllWorkspaceCrossRefsSync()
                        .isEmpty(),
                )
                assertTrue(
                    retiredMainBeaconTableAbsentForTransport(database),
                )

                database.openHelper.writableDatabase
                    .query("PRAGMA foreign_key_check")
                    .use { cursor ->
                        assertEquals(0, cursor.count)
                    }
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 merge accepts empty H1 and discards embedded Workspace parent and order`() =
        runBlocking {
            val database = database()
            try {
                // The P2 prerequisite owns reserved System targets before normal V2 ingress.
                SystemWorkspaceMaterializer(
                    database = database,
                    contextDao = database.contextDao(),
                    workspaceDao = database.workspaceDao(),
).materializeAll(now = 5L, seedMissingFactoryCapabilities = false)

                val ownerId = "v2-merge-owner"
                val legacyParentId = "v2-legacy-parent"
                val owner = canonicalOnlyWorkspace(ownerId)
                val parent = canonicalOnlyWorkspace(legacyParentId)
                database.workspaceDao().upsert(listOf(owner, parent))
                val placements = CanonicalHierarchyPlacementRepository(database)
                placements.createPrimaryAppearance(
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, ownerId),
                    now = 11L,
                )
                placements.createPrimaryAppearance(
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, legacyParentId),
                    now = 12L,
                )
                val established = placements.getLiveHierarchy()
                val incoming =
                    canonicalBundle(
                        context = ordinaryContext(ownerId).copy(isDeleted = true),
                        workspace =
                            owner.copy(
                                version = owner.version + 1L,
                                updatedAt = owner.updatedAt + 10L,
                            ),
                    ).copy(
                        contexts = emptyList(),
                    ).let { bundle ->
                        val ownerSnapshot =
                            requireNotNull(bundle.workspaces).single().copy(
                                parentWorkspaceId = legacyParentId,
                                workspaceOrder = 99L,
                            )
                        bundle.copy(
                            workspaces =
                                listOf(ownerSnapshot) +
                                    parent
                                        .toWorkspaceSnapshot()
                                        .withoutEmbeddedWorkspaceTopology(),
                            hierarchyPlacements = emptyList(),
                            hierarchyPlacementGroupScopes = emptyList(),
                            hierarchyPlacementLinkedAppearances = emptyList(),
                        )
                    }

                merge(database).applySnapshotBundle(incoming)

                assertEquals(established, placements.getLiveHierarchy())
                assertRetiredPhysicalHierarchyAbsent(database)
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 merge rolls back Workspace winner when incoming H1 target is missing`() =
        runBlocking {
            val database = database()
            try {
                SystemWorkspaceMaterializer(
                    database = database,
                    contextDao = database.contextDao(),
                    workspaceDao = database.workspaceDao(),
).materializeAll(now = 5L, seedMissingFactoryCapabilities = false)

                val ownerId = "v2-rollback-owner"
                val parentId = "v2-rollback-legacy-parent"
                val owner = canonicalOnlyWorkspace(ownerId)
                val parent = canonicalOnlyWorkspace(parentId)
                database.workspaceDao().upsert(listOf(owner, parent))

                val placements = CanonicalHierarchyPlacementRepository(database)
                placements.createPrimaryAppearance(
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, ownerId),
                    now = 11L,
                )
                val beforeWorkspaces = database.workspaceDao().getAll()
                val beforeHierarchy = placements.getLiveHierarchy()

                val incoming =
                    canonicalBundle(
                        context = ordinaryContext(ownerId).copy(isDeleted = true),
                        workspace =
                            owner.copy(
                                version = owner.version + 1L,
                                updatedAt = owner.updatedAt + 10L,
                            ),
                    ).copy(
                        contexts = emptyList(),
                    ).let { bundle ->
                        val ownerSnapshot =
                            requireNotNull(bundle.workspaces).single().copy(
                                parentWorkspaceId = parentId,
                                workspaceOrder = 99L,
                            )
                        bundle.copy(
                            workspaces =
                                listOf(ownerSnapshot) +
                                    parent
                                        .toWorkspaceSnapshot()
                                        .withoutEmbeddedWorkspaceTopology(),
                            hierarchyPlacements =
                                listOf(
                                    HierarchyPlacementSnapshot(
                                        id = "missing-target-placement",
                                        hierarchyId = "GENERAL",
                                        targetType = "WORKSPACE",
                                        targetId = "missing-workspace",
                                        parentPlacementId = null,
                                        placementKind = "PRIMARY",
                                        siblingOrder = 0L,
                                        createdAt = 1L,
                                        updatedAt = 1L,
                                        syncedAt = null,
                                        isDeleted = false,
                                        version = 1L,
                                    ),
                                ),
                            hierarchyPlacementGroupScopes = emptyList(),
                            hierarchyPlacementLinkedAppearances = emptyList(),
                        )
                    }

                val failure =
                    runCatching {
                        merge(database).applySnapshotBundle(incoming)
                    }.exceptionOrNull()

                assertTrue(
                    "Expected missing H1 target, got ${failure?.javaClass?.name}: ${failure?.message}",
                    failure is HierarchyTargetMissingException,
                )
                assertEquals(beforeWorkspaces, database.workspaceDao().getAll())
                assertEquals(beforeHierarchy, placements.getLiveHierarchy())
                assertNull(database.hierarchyPlacementDao().getById("missing-target-placement"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 merge rejects Workspace tombstone with empty H1 while local occurrence remains live`() =
        runBlocking {
            val database = database()
            try {
                SystemWorkspaceMaterializer(
                    database = database,
                    contextDao = database.contextDao(),
                    workspaceDao = database.workspaceDao(),
).materializeAll(now = 5L, seedMissingFactoryCapabilities = false)

                val ownerId = "v2-tombstone-owner"
                val owner = canonicalOnlyWorkspace(ownerId)
                database.workspaceDao().upsert(listOf(owner))
                val placements = CanonicalHierarchyPlacementRepository(database)
                placements.createPrimaryAppearance(
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, ownerId),
                    now = 11L,
                )
                val beforeWorkspaces = database.workspaceDao().getAll()
                val beforeHierarchy = placements.getLiveHierarchy()

                val incoming =
                    canonicalBundle(
                        context = ordinaryContext(ownerId).copy(isDeleted = true),
                        workspace =
                            owner.copy(
                                isDeleted = true,
                                version = owner.version + 1L,
                                updatedAt = owner.updatedAt + 10L,
                            ),
                    ).copy(
                        contexts = emptyList(),
                        hierarchyPlacements = emptyList(),
                        hierarchyPlacementGroupScopes = emptyList(),
                        hierarchyPlacementLinkedAppearances = emptyList(),
                    )

                val failure =
                    runCatching {
                        merge(database).applySnapshotBundle(incoming)
                    }.exceptionOrNull()

                assertTrue(
                    "Expected deleted H1 target, got ${failure?.javaClass?.name}: ${failure?.message}",
                    failure is HierarchyTargetDeletedException,
                )
                assertEquals(beforeWorkspaces, database.workspaceDao().getAll())
                assertEquals(beforeHierarchy, placements.getLiveHierarchy())
            } finally {
                database.close()
            }
        }

    @Test
    fun `normal merge rejects live ordinary Context even with incoming canonical Workspace`() =
        runBlocking {
            val destination = database()
            try {
                val failure =
                    runCatching {
                        merge(destination).applySnapshotBundle(
                            canonicalBundle(
                                context = ordinaryContext(RETIRED_ID),
                                workspace = canonicalOnlyWorkspace(RETIRED_ID),
                            ),
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(
                    failure!!.message.orEmpty().contains(
                        "Canonical merge ingress refuses ordinary Context persistence",
                    ),
                )
                assertNull(destination.contextDao().getContextById(RETIRED_ID))
                assertNull(destination.workspaceDao().getById(RETIRED_ID))
            } finally {
                destination.close()
            }
        }

    @Test
    fun `normal merge rejects live ordinary Context with local canonical Workspace`() =
        runBlocking {
            val database = database()
            try {
                val existingWorkspace = canonicalOnlyWorkspace(RETIRED_ID)
                database.workspaceDao().upsert(listOf(existingWorkspace))

                val failure =
                    runCatching {
                        merge(database).applySnapshotBundle(
                            SnapshotBundle(
                                version = 1,
                                contexts = listOf(ordinaryContext(RETIRED_ID).toSnapshot()),
                                hierarchyPlacements = emptyList(),
                                hierarchyPlacementGroupScopes = emptyList(),
                                hierarchyPlacementLinkedAppearances = emptyList(),
                            ),
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertNull(database.contextDao().getContextById(RETIRED_ID))
                assertEquals(existingWorkspace, database.workspaceDao().getById(RETIRED_ID))
            } finally {
                database.close()
            }
        }

    @Test
    fun `normal merge rejects ordinary Context tombstone transport evidence`() =
        runBlocking {
            val destination = database()
            try {
                val tombstone =
                    ordinaryContext(RETIRED_ID).copy(
                        isDeleted = true,
                        version = 7L,
                    )

                val failure =
                    runCatching {
                        merge(destination).applySnapshotBundle(
                            canonicalBundle(
                                context = tombstone,
                                workspace = canonicalOnlyWorkspace(RETIRED_ID),
                            ),
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertNull(destination.contextDao().getContextById(RETIRED_ID))
                assertNull(destination.workspaceDao().getById(RETIRED_ID))
            } finally {
                destination.close()
            }
        }

    @Test
    fun `full export excludes retired Context and legacy structural links`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(
                        ordinaryContext(RETIRED_ID),
                        ordinaryContext(CONTEXT_BACKED_ID),
                        ordinaryContext("context-backed-child"),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnlyWorkspace(RETIRED_ID),
                        contextBackedWorkspace(CONTEXT_BACKED_ID),
                        contextBackedWorkspace("context-backed-child"),
                    ),
                )
                database.mainBeaconDao().insertBeacons(
                    listOf(
                        MainBeacon(id = "export-beacon-parent", title = "Parent"),
                        MainBeacon(
                            id = "export-beacon-child",
                            title = "Child",
                            order = 7L,
                        ),
                    ),
                )
                assertRetiredPhysicalHierarchyAbsent(database)

                val exported = fullBackup(database).loadFullSnapshotBundle()

                assertEquals(
                    com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_HIERARCHY_FORMAT_VERSION,
                    exported.hierarchyFormatVersion,
                )
                assertFalse(
                    exported.contexts.any {
                        it.id == RETIRED_ID && !it.isDeleted
                    },
                )
                assertTrue(
                    exported.contexts.any {
                        it.id == CONTEXT_BACKED_ID && !it.isDeleted
                    },
                )
                assertTrue(exported.contextParentLinks.isEmpty())
                assertTrue(exported.mainBeaconParentLinks.isEmpty())
                val exportedChild =
                    exported.mainBeacons.single { it.id == "export-beacon-child" }
                assertNull(exportedChild.parentBeaconId)
                assertEquals(7L, exportedChild.order)
                assertNotNull(exported.hierarchyPlacements)
                assertNotNull(exported.hierarchyPlacementGroupScopes)
                assertNotNull(exported.hierarchyPlacementLinkedAppearances)
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync selection and delta do not emit Context persistence`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(
                        ordinaryContext(RETIRED_ID),
                        ordinaryContext(CONTEXT_BACKED_ID),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnlyWorkspace(RETIRED_ID),
                        contextBackedWorkspace(CONTEXT_BACKED_ID),
                    ),
                )
                val sync = sync(database)

                val selection = sync.getUnsyncedSelection()
                assertTrue(selection.isEmpty())

                val delta = sync.getChangesSince(0L)
                assertTrue(delta.contexts.isEmpty())
                assertTrue(delta.contextParentLinks.isEmpty())
                assertTrue(delta.mainBeaconParentLinks.isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync pipeline ACK preserves canonical DayTask Workspace owner and rejects stale version`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(ordinaryContext(CONTEXT_BACKED_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(contextBackedWorkspace(CONTEXT_BACKED_ID)),
                )
                val plan =
                    DayPlan(
                        id = "ack-canonical-plan",
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = 500L,
                        version = 1L,
                    )
                database.dayPlanDao().insert(plan)

                val dao = database.dayTaskDao()
                dao.insert(
                    DayTask(
                        id = "ack-canonical-task",
                        dayPlanId = plan.id,
                        title = "Canonical task",
                        projectId = CONTEXT_BACKED_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val before = dao.getAllTasksSync().single()
                assertNull(before.projectId)
                assertEquals(CONTEXT_BACKED_ID, before.projectWorkspaceId)

                val sync = sync(database)
                val selection = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(before.id, before.version)),
                    selection.dayTasks,
                )

                sync.acknowledge(selection)

                val acknowledged = dao.getAllTasksSync().single()
                assertNotNull(acknowledged.syncedAt)
                assertEquals(
                    before.copy(syncedAt = acknowledged.syncedAt),
                    acknowledged,
                )

                val newer =
                    acknowledged.copy(
                        title = "Newer canonical task",
                        version = acknowledged.version + 1L,
                        updatedAt = acknowledged.updatedAt?.plus(1L),
                        syncedAt = null,
                    )
                dao.update(newer)

                sync.acknowledge(selection)
                assertEquals(newer, dao.getAllTasksSync().single())
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync pipeline TacticalMission ACK preserves Workspace owner and rejects stale versions`() =
        runBlocking {
            val database = database()
            try {
                val ownerId = "ack-tactical-workspace"
                val missionId = 98001L

                database.workspaceDao().upsert(
                    listOf(canonicalOnlyWorkspace(ownerId)),
                )

                val dao = database.tacticalMissionDao()
                dao.insertMission(
                    TacticalMission(
                        id = missionId,
                        title = "Canonical tactical mission",
                        description = "Preserve mission payload",
                        deadline = com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE,
                        projectId = ownerId,
                        linkedProjectIds = listOf("related-workspace"),
                        linkedAttachmentIds = listOf("related-attachment"),
                        missionStreamId = "mission-stream",
                        orderInSlot = 9L,
                        createdAt = 1L,
                        updatedAt = 2L,
                        version = 1L,
                    ),
                )

                val before = requireNotNull(dao.getMissionById(missionId))
                assertNull(before.projectId)
                assertEquals(ownerId, before.projectWorkspaceId)

                val sync = sync(database)
                val selection = sync.getUnsyncedSelection()

                assertEquals(
                    listOf(LocalSyncVersion(missionId.toString(), before.version)),
                    selection.tacticalMissions,
                )

                sync.acknowledge(selection)

                val acknowledged = requireNotNull(dao.getMissionById(missionId))
                assertNotNull(acknowledged.syncedAt)
                assertEquals(
                    before.copy(syncedAt = acknowledged.syncedAt),
                    acknowledged,
                )

                val newer =
                    acknowledged.copy(
                        title = "Newer tactical mission",
                        version = acknowledged.version + 1L,
                        updatedAt = acknowledged.updatedAt?.plus(1L),
                        syncedAt = null,
                    )

                dao.updateMission(newer)

                sync.acknowledge(selection)
                assertEquals(newer, dao.getMissionById(missionId))

                val freshSelection = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(missionId.toString(), newer.version)),
                    freshSelection.tacticalMissions,
                )

                sync.acknowledge(freshSelection)

                val after = requireNotNull(dao.getMissionById(missionId))
                assertNotNull(after.syncedAt)
                assertEquals(
                    newer.copy(syncedAt = after.syncedAt),
                    after,
                )
                assertNull(after.projectId)
                assertEquals(ownerId, after.projectWorkspaceId)
                assertEquals("mission-stream", after.missionStreamId)
                assertEquals(9L, after.orderInSlot)
                assertEquals(
                    listOf("related-attachment"),
                    after.linkedAttachmentIds,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync pipeline ACK preserves migrated Workspace-only DayTask across stale and fresh selections`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(ordinaryContext(CONTEXT_BACKED_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(contextBackedWorkspace(CONTEXT_BACKED_ID)),
                )
                val plan =
                    DayPlan(
                        id = "ack-historical-plan",
                        date = 1L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = 500L,
                        version = 1L,
                    )
                database.dayPlanDao().insert(plan)

                val dao = database.dayTaskDao()

                // Room 196 has no physical day_tasks.projectId.
                // Migration 195 -> 196 is covered by its dedicated Room test.
                // This raw fixture represents already-migrated storage.
                // ACK must never rewrite ownership or newer versions.
                dao.insertRaw(
                    DayTask(
                        id = "ack-historical-task",
                        dayPlanId = plan.id,
                        title = "Historical task",
                        projectWorkspaceId = CONTEXT_BACKED_ID,
                        createdAt = 1L,
                        updatedAt = 1L,
                        version = 1L,
                    ),
                )

                val before = dao.getAllTasksSync().single()
                assertNull(before.projectId)
                assertEquals(CONTEXT_BACKED_ID, before.projectWorkspaceId)

                val sync = sync(database)
                val staleSelection = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(before.id, before.version)),
                    staleSelection.dayTasks,
                )

                val newer =
                    before.copy(
                        title = "Updated historical task",
                        version = before.version + 1L,
                        updatedAt = before.updatedAt?.plus(1L),
                    )
                dao.updateRaw(newer)

                sync.acknowledge(staleSelection)
                assertEquals(newer, dao.getAllTasksSync().single())

                val freshSelection = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(newer.id, newer.version)),
                    freshSelection.dayTasks,
                )

                sync.acknowledge(freshSelection)

                val acknowledged = dao.getAllTasksSync().single()
                assertNotNull(acknowledged.syncedAt)
                assertEquals(
                    newer.copy(syncedAt = acknowledged.syncedAt),
                    acknowledged,
                )
                assertNull(acknowledged.projectId)
                assertEquals(CONTEXT_BACKED_ID, acknowledged.projectWorkspaceId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync pipeline TacticalActivitySlot ACK is metadata only and version guarded`() =
        runBlocking {
            val database = database()
            try {
                val contextId = "ack-tactical-slot-context"
                database.contextDao().insertContexts(
                    listOf(ordinaryContext(contextId)),
                )

                val dao = database.tacticalActivitySlotDao()
                val before =
                    TacticalActivitySlot(
                        id = "ack-tactical-slot",
                        contextId = contextId,
                        slotOrder = 7L,
                        createdAt = 1L,
                        updatedAt = 2L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 3L,
                    )
                dao.insertSlot(before)

                val sync = sync(database)
                val selection = sync.getUnsyncedSelection()

                assertEquals(
                    listOf(LocalSyncVersion(before.id, before.version)),
                    selection.tacticalActivitySlots,
                )

                sync.acknowledge(selection)

                val acknowledged =
                    requireNotNull(dao.getSlotForContext(contextId))

                assertNotNull(acknowledged.syncedAt)
                assertEquals(
                    before.copy(syncedAt = acknowledged.syncedAt),
                    acknowledged,
                )

                val newer =
                    acknowledged.copy(
                        slotOrder = 19L,
                        updatedAt = 4L,
                        syncedAt = null,
                        isDeleted = true,
                        version = acknowledged.version + 1L,
                    )
                dao.updateSlot(newer)

                sync.acknowledge(selection)

                assertEquals(
                    newer,
                    dao.getSlotForContext(contextId),
                )

                val freshSelection = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(newer.id, newer.version)),
                    freshSelection.tacticalActivitySlots,
                )

                sync.acknowledge(freshSelection)

                val after =
                    requireNotNull(dao.getSlotForContext(contextId))

                assertNotNull(after.syncedAt)
                assertEquals(
                    newer.copy(syncedAt = after.syncedAt),
                    after,
                )
                assertEquals(contextId, after.contextId)
                assertEquals(19L, after.slotOrder)
                assertEquals(4L, after.updatedAt)
                assertTrue(after.isDeleted)
                assertEquals(4L, after.version)
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync acknowledge does not rewrite Context persistence`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(ordinaryContext(RETIRED_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(canonicalOnlyWorkspace(RETIRED_ID)),
                )

                val sync = sync(database)
                val before = requireNotNull(database.contextDao().getContextById(RETIRED_ID))
                assertNull(before.syncedAt)

                val baseline = sync.getUnsyncedSelection()
                sync.acknowledge(baseline)

                val after = requireNotNull(database.contextDao().getContextById(RETIRED_ID))
                assertNull(after.syncedAt)
                assertFalse(after.isDeleted)
            } finally {
                database.close()
            }
        }



    @Test
    fun `V2 merge cannot reinsert retired Beacon from stale canonical and legacy delta`() = runBlocking {
        val database = database()
        try {
            SystemWorkspaceMaterializer(
                database = database,
                contextDao = database.contextDao(),
                workspaceDao = database.workspaceDao(),
).materializeAll(now = 5L, seedMissingFactoryCapabilities = false)
            val bridge = MainBeaconOrientationBridge(
                orientationDao = database.orientationDao(),
                mainBeaconDao = database.mainBeaconDao(),
                bootstrapper = mockk<CanonicalOrientationBootstrapper>(relaxed = true),
                hierarchyPlacementLifecycleCoordinator = HierarchyPlacementLifecycleCoordinator(database),
            )
            val repository = MainBeaconRepository(
                appDatabase = database,
                mainBeaconDao = database.mainBeaconDao(),
                orientationBridge = bridge,
                hierarchyPlacementRepository = CanonicalHierarchyPlacementRepository(database),
                groupScopeCoordinator = HierarchyPlacementGroupScopeMutationCoordinator(database),
            )

            val group = MainBeaconGroup(id = "stale-group", title = "Group")
            database.mainBeaconDao().insertGroup(group)
            bridge.writeCommon(group)
            val retired = MainBeacon(id = "stale-deleted-beacon", title = "Retired")
            val independent = MainBeacon(id = "stale-independent-beacon", title = "Independent")
            repository.createBeacon(retired, emptySet(), emptySet(), setOf(group.id), emptyList())
            repository.createBeacon(independent, emptySet(), emptySet(), emptySet(), emptyList())
            val orientationDao = database.orientationDao()
            val mapping = requireNotNull(
                orientationDao.getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, retired.id),
            )
            val oldSubject = requireNotNull(orientationDao.getManagedSubject(mapping.subjectId))
            val oldPlacementIds = database.hierarchyPlacementDao().getAll()
                .filter { it.targetId == mapping.subjectId && !it.isDeleted }
                .map { it.id }
                .toSet()
            assertTrue(oldPlacementIds.isNotEmpty())

            // A delayed peer sends its old Beacon metadata and compatibility
            // associations, but no new H1 changes. Canonical freshness must
            // retain our deletion and legacy cleanup must prevent resurrection.
            val staleDelta = SnapshotBundle(
                version = 2,
                mainBeacons = database.mainBeaconDao().getAllBeaconsSync().map { it.toSnapshot() },
                mainBeaconGroups = database.mainBeaconDao().getAllGroupsSync().map { it.toSnapshot() },
                mainBeaconGroupMembers = database.mainBeaconDao().getAllGroupMembersSync().map { it.toSnapshot() },
                mainBeaconParentLinks =
                    listOf(
                        MainBeaconParentLinkSnapshot(
                            parentBeaconId = independent.id,
                            childBeaconId = retired.id,
                            order = 1L,
                            updatedAt = 20L,
                            createdAt = 10L,
                        ),
                    ),
                managedSubjects = orientationDao.getAllManagedSubjects(),
                orientations = orientationDao.getAllOrientations(),
                aspects = orientationDao.getAllAspects(),
                orientationAssessments = orientationDao.getAllAssessments(),
                orientationAssessmentRevisions = orientationDao.getAllAssessmentRevisions(),
                legacySubjectMappings = orientationDao.getAllLegacyMappings(),
                orientationRelations = orientationDao.getAllOrientationRelations(),
                aspectOrientationRefs = orientationDao.getAllAspectOrientationRefs(),
                workspaces =
                    database.workspaceDao().getAll().map {
                        it.toWorkspaceSnapshot().withoutEmbeddedWorkspaceTopology()
                    },
                workspaceBindings = orientationDao.getAllWorkspaceBindings(),
                workspaceCapabilityInstances = orientationDao.getAllWorkspaceCapabilities(),
                savedOrientationViews = orientationDao.getAllSavedViews(),
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )
            assertTrue(staleDelta.mainBeaconParentLinks.any { it.childBeaconId == retired.id })
            repository.deleteBeacon(retired.id)
            assertNull(database.mainBeaconDao().getBeaconById(retired.id))
            assertTrue(requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)).isDeleted)

            // Normal V2 ingress requires a complete GroupScope stream. Carry
            // the current receiver-side scopes while replaying stale Beacon
            // and canonical Orientation rows from the delayed peer.
            val incoming = staleDelta.copy(
                hierarchyPlacementGroupScopes =
                    CanonicalHierarchyPlacementGroupScopeSyncStore(database).loadAll(),
            )
            merge(database).applySnapshotBundle(incoming)

            assertNull(database.mainBeaconDao().getBeaconById(retired.id))
            assertTrue(database.mainBeaconDao().getBeaconById(independent.id) != null)
            val after = requireNotNull(orientationDao.getManagedSubject(mapping.subjectId))
            assertTrue(after.isDeleted)
            assertTrue(after.version > oldSubject.version)
            assertTrue(
                database.hierarchyPlacementDao().getAll()
                    .filter { it.id in oldPlacementIds }
                    .all { it.isDeleted },
            )
            assertTrue(database.mainBeaconDao().getAllGroupMembersSync().none { it.beaconId == retired.id })
            assertRetiredPhysicalHierarchyAbsent(database)
        } finally {
            database.close()
        }
    }

    @Test
    fun `V2 merge cannot reinsert retired Group or membership from stale payload`() = runBlocking {
        val database = database()
        try {
            SystemWorkspaceMaterializer(
                database = database,
                contextDao = database.contextDao(),
                workspaceDao = database.workspaceDao(),
).materializeAll(now = 5L, seedMissingFactoryCapabilities = false)
            val bridge = MainBeaconOrientationBridge(
                orientationDao = database.orientationDao(),
                mainBeaconDao = database.mainBeaconDao(),
                bootstrapper = mockk<CanonicalOrientationBootstrapper>(relaxed = true),
                hierarchyPlacementLifecycleCoordinator = HierarchyPlacementLifecycleCoordinator(database),
            )
            val repository = MainBeaconRepository(
                appDatabase = database,
                mainBeaconDao = database.mainBeaconDao(),
                orientationBridge = bridge,
                hierarchyPlacementRepository = CanonicalHierarchyPlacementRepository(database),
                groupScopeCoordinator = HierarchyPlacementGroupScopeMutationCoordinator(database),
            )

            val group = MainBeaconGroup(id = "stale-retired-group", title = "Retired Group")
            val beacon = MainBeacon(id = "stale-group-member", title = "Preserved Beacon")
            database.mainBeaconDao().insertGroup(group)
            bridge.writeCommon(group)
            repository.createBeacon(beacon, emptySet(), emptySet(), setOf(group.id), emptyList())
            val orientationDao = database.orientationDao()
            val groupMapping = requireNotNull(
                orientationDao.getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON_GROUP.name, group.id),
            )
            val beaconMapping = requireNotNull(
                orientationDao.getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, beacon.id),
            )
            val oldGroupSubject = requireNotNull(orientationDao.getManagedSubject(groupMapping.subjectId))
            val appearance = database.hierarchyPlacementDao().getAll().single {
                !it.isDeleted && it.targetId == beaconMapping.subjectId
            }

            val staleDelta = SnapshotBundle(
                version = 2,
                mainBeacons = database.mainBeaconDao().getAllBeaconsSync().map { it.toSnapshot() },
                mainBeaconGroups = database.mainBeaconDao().getAllGroupsSync().map { it.toSnapshot() },
                mainBeaconGroupMembers = database.mainBeaconDao().getAllGroupMembersSync().map { it.toSnapshot() },
                managedSubjects = orientationDao.getAllManagedSubjects(),
                orientations = orientationDao.getAllOrientations(),
                aspects = orientationDao.getAllAspects(),
                orientationAssessments = orientationDao.getAllAssessments(),
                orientationAssessmentRevisions = orientationDao.getAllAssessmentRevisions(),
                legacySubjectMappings = orientationDao.getAllLegacyMappings(),
                orientationRelations = orientationDao.getAllOrientationRelations(),
                aspectOrientationRefs = orientationDao.getAllAspectOrientationRefs(),
                workspaces =
                    database.workspaceDao().getAll().map {
                        it.toWorkspaceSnapshot().withoutEmbeddedWorkspaceTopology()
                    },
                workspaceBindings = orientationDao.getAllWorkspaceBindings(),
                workspaceCapabilityInstances = orientationDao.getAllWorkspaceCapabilities(),
                savedOrientationViews = orientationDao.getAllSavedViews(),
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )
            assertTrue(staleDelta.mainBeaconGroupMembers.any { it.groupId == group.id })

            repository.deleteGroup(group.id)
            assertTrue(requireNotNull(orientationDao.getManagedSubject(groupMapping.subjectId)).isDeleted)
            assertNull(database.mainBeaconDao().getAllGroupsSync().firstOrNull { it.id == group.id })
            assertEquals(
                null,
                database.hierarchyPlacementGroupScopeDao().getByPlacementId(appearance.id)?.groupSubjectId,
            )

            val incoming = staleDelta.copy(
                hierarchyPlacementGroupScopes =
                    CanonicalHierarchyPlacementGroupScopeSyncStore(database).loadAll(),
            )
            merge(database).applySnapshotBundle(incoming)

            assertNull(database.mainBeaconDao().getAllGroupsSync().firstOrNull { it.id == group.id })
            assertTrue(database.mainBeaconDao().getAllGroupMembersSync().none { it.groupId == group.id })
            val retired = requireNotNull(orientationDao.getManagedSubject(groupMapping.subjectId))
            assertTrue(retired.isDeleted)
            assertTrue(retired.version > oldGroupSubject.version)
            assertTrue(
                orientationDao.getAllOrientationRelations()
                    .filter {
                        it.fromOrientationId == beaconMapping.subjectId &&
                            it.toOrientationId == groupMapping.subjectId
                    }.all { it.isDeleted },
            )
            assertTrue(database.mainBeaconDao().getBeaconById(beacon.id) != null)
            assertFalse(requireNotNull(orientationDao.getManagedSubject(beaconMapping.subjectId)).isDeleted)
            assertFalse(requireNotNull(database.hierarchyPlacementDao().getById(appearance.id)).isDeleted)
            assertEquals(
                null,
                database.hierarchyPlacementGroupScopeDao().getByPlacementId(appearance.id)?.groupSubjectId,
            )
            HierarchyPlacementGroupScopeMutationCoordinator(database).validateAuthoritativeState()
        } finally {
            database.close()
        }
    }

    @Test
    fun `Legacy Note ACK updates metadata only and rejects stale selection`() =
        runBlocking {
            val database = database()
            try {
                val ownerId = "ack-legacy-note-context"
                database.contextDao().insertAll(listOf(ordinaryContext(ownerId)))

                val dao = database.legacyNoteDao()
                val original =
                    com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity(
                        id = "ack-legacy-note",
                        contextId = ownerId,
                        title = "Original title",
                        content = "Original body",
                        version = 1L,
                    )
                dao.insert(original)

                val sync = sync(database)
                val selected = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(original.id, original.version)),
                    selected.notes,
                )

                sync.acknowledge(selected)

                val acknowledged = requireNotNull(dao.getNoteById(original.id))
                assertNotNull(acknowledged.syncedAt)
                assertEquals(
                    original.copy(syncedAt = acknowledged.syncedAt),
                    acknowledged,
                )

                val newer =
                    acknowledged.copy(
                        title = "Newer title",
                        content = "Newer body",
                        updatedAt = acknowledged.updatedAt + 1L,
                        version = acknowledged.version + 1L,
                        syncedAt = null,
                    )
                dao.update(newer)

                sync.acknowledge(selected)
                assertEquals(newer, dao.getNoteById(newer.id))

                val fresh = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(newer.id, newer.version)),
                    fresh.notes,
                )

                sync.acknowledge(fresh)

                val finalNote = requireNotNull(dao.getNoteById(newer.id))
                assertNotNull(finalNote.syncedAt)
                assertEquals(
                    newer.copy(syncedAt = finalNote.syncedAt),
                    finalNote,
                )
                assertEquals(ownerId, finalNote.contextId)
            } finally {
                database.close()
            }
        }

    private fun assertRetiredPhysicalHierarchyAbsent(database: AppDatabase) {
        val sql = database.openHelper.writableDatabase

        for (table in listOf("context_parent_links", "main_beacon_parent_links")) {
            val count =
                sql.query(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                    arrayOf(table),
                ).use { cursor ->
                    check(cursor.moveToFirst())
                    cursor.getLong(0)
                }
            assertEquals("Retired table must not exist: $table", 0L, count)
        }

        fun columns(table: String): Set<String> =
            buildSet {
                sql.query("PRAGMA table_info(`$table`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        add(cursor.getString(nameIndex))
                    }
                }
            }

        val workspaceColumns = columns("workspaces")
        assertFalse(workspaceColumns.contains("parentWorkspaceId"))
        assertFalse(workspaceColumns.contains("workspaceOrder"))

        val beaconColumns = columns("main_beacons")
        assertFalse(beaconColumns.contains("parent_beacon_id"))
        assertTrue(beaconColumns.contains("beacon_order"))
    }

    @Test
    fun `R2A3P real scoring Merge rollback rejects tombstone atomically`() = runBlocking {
        val source = database()
        val destination = database()
        try {
            val goal =
                Goal(
                    id = "r2a3p-goal",
                    text = "Scoring integration fixture",
                    completed = false,
                    createdAt = 100L,
                    updatedAt = 100L,
                    valueImportance = 2.5f,
                    valueImpact = 11f,
                    effort = 13f,
                    cost = 4f,
                    risk = 8f,
                    weightEffort = 1.7f,
                    weightCost = 0.6f,
                    weightRisk = 1.2f,
                    rawScore = -0.573368f,
                    displayScore = 21,
                    scoringStatus = ScoringStatusValues.ASSESSED,
                )
            val subjectId = "r2a3p-orientation"
            val assessment =
                emptyApplicableAssessment().copy(
                    importance = AxisAssessment(
                        ImportanceValue.LOW.name,
                        ValueOrigin.EXPLICIT,
                    ),
                    impact = AxisAssessment(
                        ImpactValue.TRANSFORMATIVE.name,
                        ValueOrigin.EXPLICIT,
                    ),
                )

            source.goalDao().insertGoal(goal)
            destination.goalDao().insertGoal(goal)

            CanonicalOrientationRepository(source, source.orientationDao()).saveOrientation(
                ManagedSubject(
                    subjectId, 100L, 100L, null, false, 1L,
                    ManagedSubjectType.ORIENTATION, "Goal", null,
                ),
                OrientationNode(
                    subjectId, OrientationKind.GOAL, null,
                    ValueOrigin.UNSET, assessment,
                ),
                OrientationAssessmentRevision(
                    "r2a3p-revision", 100L, 100L, null, false, 1L,
                    subjectId, 100L, 100L,
                    AssessmentRevisionSource.MIGRATION,
                    "integration fixture",
                    assessment,
                ),
            )

            source.orientationDao().upsertLegacyMappings(
                listOf(
                    LegacySubjectMappingEntity(
                        id = "r2a3p-mapping",
                        sourceType = LegacyOrientationSourceType.GOAL.name,
                        sourceId = goal.id,
                        subjectId = subjectId,
                        migrationVersion = 1,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = 100L,
                        updatedAt = 100L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )

            fun scoring(db: AppDatabase) =
                CanonicalScoringAuthorityActivator(
                    db,
                    db.orientationDao(),
                    db.goalDao(),
                    db.contextDao(),
                    db.canonicalScoringAuthorityStateDao(),
                )

            scoring(source).ensureActivated(now = 500L)
            val destScoring = scoring(destination)
            destScoring.ensureActivated(now = 550L)
            assertNotNull(destination.canonicalScoringAuthorityStateDao().get())

            val orientationDao = source.orientationDao()
            val incoming =
                canonicalBundle(
                    ordinaryContext("r2a3p-unused-context").copy(isDeleted = true),
                    canonicalOnlyWorkspace("r2a3p-valid-workspace"),
                ).copy(
                    contexts = emptyList(),
                    managedSubjects = orientationDao.getAllManagedSubjects(),
                    orientations = orientationDao.getAllOrientations(),
                    aspects = orientationDao.getAllAspects(),
                    orientationAssessments = orientationDao.getAllAssessments(),
                    orientationAssessmentRevisions =
                        orientationDao.getAllAssessmentRevisions(),
                    orientationScoringOverrides =
                        orientationDao.getAllScoringOverrides(),
                    legacySubjectMappings = orientationDao.getAllLegacyMappings(),
                    orientationRelations = orientationDao.getAllOrientationRelations(),
                    aspectOrientationRefs = orientationDao.getAllAspectOrientationRefs(),
                )

            val destOrientation =
                CanonicalOrientationBootstrapper(
                    destination,
                    destination.orientationDao(),
                    destination.goalDao(),
                    destination.mainBeaconDao(),
                    destination.arcQuestDao(),
                    destination.canonicalDayThemeDao(),
                    CanonicalDayThemeBootstrapper(
                        destination,
                        destination.dayThemeDocumentDao(),
                        destination.canonicalDayThemeDao(),
                    ),
                )

            val subject =
                merge(
                    database = destination,
                    scoringActivator = destScoring,
                    orientationBootstrapper = destOrientation,
                )

            // Real Merge with the marker already present.
            subject.applySnapshotBundle(incoming)
            assertNotNull(destination.orientationDao().getManagedSubject(subjectId))

            val admittedOverride =
                requireNotNull(destination.orientationDao().getScoringOverride(subjectId))
            assertFalse(admittedOverride.isDeleted)

            val sentinelId = "r2a3p-rollback-workspace"
            val sentinel =
                canonicalOnlyWorkspace(sentinelId)
                    .toWorkspaceSnapshot()
                    .withoutEmbeddedWorkspaceTopology()

            val tombstone =
                admittedOverride.copy(
                    isDeleted = true,
                    version = admittedOverride.version + 1L,
                    updatedAt = admittedOverride.updatedAt + 100L,
                )

            val rejected =
                incoming.copy(
                    workspaces = incoming.workspaces.orEmpty() + sentinel,
                    orientationScoringOverrides = listOf(tombstone),
                )

            val failure =
                runCatching {
                    subject.applySnapshotBundle(rejected)
                }.exceptionOrNull()

            assertTrue(
                "Expected scoring admission rejection, got " +
                    "${failure?.javaClass?.name}: ${failure?.message}",
                failure is IllegalArgumentException,
            )
            assertTrue(
                "Expected scoring activator stack, got ${failure?.stackTraceToString()}",
                failure?.stackTrace?.any {
                    it.className.contains("CanonicalScoringAuthorityActivator")
                } == true,
            )

            // The sentinel Workspace entered the Merge mutation before admission.
            // Its absence proves rollback across mutation and afterRefresh.
            assertNull(destination.workspaceDao().getAll().firstOrNull {
                it.id == sentinelId
            })
            assertEquals(
                admittedOverride,
                destination.orientationDao().getScoringOverride(subjectId),
            )
            assertNotNull(destination.canonicalScoringAuthorityStateDao().get())
        } finally {
            source.close()
            destination.close()
        }
    }

    private fun canonicalBundle(
        context: ContextEntity,
        workspace: WorkspaceEntity,
    ) = SnapshotBundle(
        version = 2,
        contexts = listOf(context.toSnapshot()),
        managedSubjects = emptyList(),
        orientations = emptyList(),
        aspects = emptyList(),
        orientationAssessments = emptyList(),
        orientationAssessmentRevisions = emptyList(),
        legacySubjectMappings = emptyList(),
        orientationRelations = emptyList(),
        aspectOrientationRefs = emptyList(),
        workspaces =
            listOf(
                workspace
                    .toWorkspaceSnapshot()
                    .withoutEmbeddedWorkspaceTopology(),
            ),
        workspaceBindings = emptyList(),
        workspaceCapabilityInstances = emptyList(),
        savedOrientationViews = emptyList(),
        hierarchyPlacements = emptyList(),
        hierarchyPlacementGroupScopes = emptyList(),
        hierarchyPlacementLinkedAppearances = emptyList(),
    )

    private fun ordinaryContext(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 10L,
            updatedAt = 20L,
        )

    private fun canonicalOnlyWorkspace(id: String) =
        workspace(
            id = id,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun contextBackedWorkspace(id: String) =
        workspace(
            id = id,
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    private fun workspace(
        id: String,
        provenance: String,
        sourceContextId: String?,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        roleCode = null,
        createdAt = 10L,
        updatedAt = 20L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = provenance,
        sourceContextId = sourceContextId,
    )

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
                    LegacyNoteDao::class.java to database.legacyNoteDao(),
                    MainBeaconDao::class.java to database.mainBeaconDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    CanonicalWorkspaceBootstrapper::class.java to bootstrapper,
                    ContextWorkspaceWriteThrough::class.java to ContextWorkspaceWriteThrough(bootstrapper),
                    DatabaseInitializer::class.java to initializer(database),
                    CanonicalWorkspaceProblemSyncStore::class.java to problemStore,
                    CanonicalWorkspaceBacklogSyncStore::class.java to
                        CanonicalWorkspaceBacklogSyncStore(
                            database = database,
                            entryDao = database.workspaceBacklogEntryDao(),
                            workspaceDao = database.workspaceDao(),
                            orientationDao = database.orientationDao(),
                            targetValidator = CanonicalBacklogTargetValidator(database),
                        ),
                    CanonicalWorkspaceTagTransportStore::class.java to
                        CanonicalWorkspaceTagTransportStore(database),
                    SystemWorkspaceTagSeed::class.java to
                        SystemWorkspaceTagSeed(database, database.contextDao()),
                ),
        )
    }

    // FK.10B.3a: standalone Note authority through real Restore/Merge ingress.

    @Test
    fun `FK10B3a Restore admits standalone Notes and tombstones without Context`() =
        runBlocking {
            val database = database()
            try {
                val standalone = fk10b3aNoteSnapshot("standalone", null)
                val historical = fk10b3aNoteSnapshot("historical", "missing-historical-context")
                val tombstone =
                    fk10b3aNoteSnapshot(
                        "deleted-note",
                        "deleted-historical-context",
                        deleted = true,
                    )

                restore(database, fk10b3aBundle(standalone, historical, tombstone))

                val dao = database.legacyNoteDao()
                assertEquals(standalone.id, requireNotNull(dao.getNoteById(standalone.id)).id)
                assertNull(requireNotNull(dao.getNoteById(standalone.id)).contextId)

                val historicalStored = requireNotNull(dao.getNoteById(historical.id))
                assertEquals(historical.contextId, historicalStored.contextId)
                assertEquals(historical.version, historicalStored.version)
                assertEquals(historical.createdAt, historicalStored.createdAt)
                assertEquals(historical.updatedAt, historicalStored.updatedAt)
                assertFalse(historicalStored.isDeleted)

                val deletedStored = requireNotNull(dao.getNoteById(tombstone.id))
                assertTrue(deletedStored.isDeleted)
                assertEquals(tombstone.contextId, deletedStored.contextId)
                assertEquals(tombstone.version, deletedStored.version)

                assertNull(database.contextDao().getContextById("missing-historical-context"))
                assertNull(database.contextDao().getContextById("deleted-historical-context"))
                assertTrue(database.workspaceBacklogEntryDao().getAll().none {
                    it.targetKind == "LEGACY_NOTE"
                })
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3a Normal Merge follows Note version and preserves local locator on replay`() =
        runBlocking {
            val database = database()
            try {
                val local = fk10b3aNoteSnapshot("versioned", "historical-local", version = 4L)
                merge(database).applySnapshotBundle(fk10b3aBundle(local))

                val dao = database.legacyNoteDao()
                val first = requireNotNull(dao.getNoteById(local.id))
                assertEquals(local.contextId, first.contextId)
                assertNull(database.contextDao().getContextById("historical-local"))

                val sameContentDifferentLocator = local.copy(contextId = "historical-remote")
                merge(database).applySnapshotBundle(fk10b3aBundle(sameContentDifferentLocator))
                assertEquals(first, dao.getNoteById(local.id))

                val older = local.copy(
                    title = "Outdated",
                    content = "Outdated content",
                    version = 3L,
                )
                merge(database).applySnapshotBundle(fk10b3aBundle(older))
                assertEquals(first, dao.getNoteById(local.id))

                val newer = local.copy(
                    title = "Incoming winner",
                    content = "New canonical body",
                    contextId = "missing-newer-context",
                    updatedAt = 35L,
                    version = 5L,
                )
                merge(database).applySnapshotBundle(fk10b3aBundle(newer))

                val final = requireNotNull(dao.getNoteById(local.id))
                assertEquals(newer.title, final.title)
                assertEquals(newer.content, final.content)
                assertEquals(newer.version, final.version)
                assertEquals(newer.contextId, final.contextId)
                assertNull(database.contextDao().getContextById("missing-newer-context"))
                assertTrue(database.workspaceBacklogEntryDao().getAll().none {
                    it.targetKind == "LEGACY_NOTE"
                })
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3a Normal Merge divergent same-version content rolls back earlier Note`() =
        runBlocking {
            val database = database()
            try {
                val existing = fk10b3aNoteSnapshot("conflict", null, version = 7L)
                merge(database).applySnapshotBundle(fk10b3aBundle(existing))

                val earlierValid = fk10b3aNoteSnapshot("must-rollback", null)
                val conflicting = existing.copy(content = "Divergent authoritative content")

                val outcome = runCatching {
                    merge(database).applySnapshotBundle(
                        fk10b3aBundle(earlierValid, conflicting),
                    )
                }

                assertTrue(outcome.isFailure)
                assertNull(database.legacyNoteDao().getNoteById(earlierValid.id))

                val preserved = requireNotNull(database.legacyNoteDao().getNoteById(existing.id))
                assertEquals(existing.title, preserved.title)
                assertEquals(existing.content, preserved.content)
                assertEquals(existing.version, preserved.version)
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3a Restore duplicate authoritative Note IDs rolls back replacement`() =
        runBlocking {
            val database = database()
            try {
                val preserved = fk10b3aNoteSnapshot("before-invalid-restore", null)
                database.legacyNoteDao().insert(
                    com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity(
                        id = preserved.id,
                        contextId = preserved.contextId,
                        title = preserved.title,
                        content = preserved.content,
                        createdAt = preserved.createdAt,
                        updatedAt = preserved.updatedAt,
                        isDeleted = preserved.isDeleted,
                        version = preserved.version,
                    ),
                )

                val duplicate = fk10b3aNoteSnapshot("duplicate", null)
                val outcome = runCatching {
                    restore(database, fk10b3aBundle(duplicate, duplicate))
                }

                assertTrue(outcome.isFailure)
                assertNotNull(database.legacyNoteDao().getNoteById(preserved.id))
                assertNull(database.legacyNoteDao().getNoteById(duplicate.id))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3a Merge admits two Workspace placements for one Note`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("multi-note", "missing-multi-context")
                val first = fk10b3aPlacement("placement-a", "fk10b3a-workspace", note.id)
                val second = fk10b3aPlacement("placement-b", "fk10b3a-workspace-b", note.id)

                merge(database).applySnapshotBundle(
                    fk10b3aPlacedBundle(note, listOf(first, second)),
                )

                val stored = requireNotNull(database.legacyNoteDao().getNoteById(note.id))
                assertEquals(note.content, stored.content)
                assertEquals(note.contextId, stored.contextId)

                val placements =
                    database.workspaceBacklogEntryDao().getAll()
                        .filter { !it.isDeleted && it.targetKind == "LEGACY_NOTE" }

                assertEquals(2, placements.size)
                assertEquals(setOf(note.id), placements.map { it.targetId }.toSet())
                assertEquals(
                    setOf("fk10b3a-workspace", "fk10b3a-workspace-b"),
                    placements.map { it.workspaceId }.toSet(),
                )
                assertEquals(1, database.legacyNoteDao().getAllRaw().count { it.id == note.id })
                assertNull(database.contextDao().getContextById("missing-multi-context"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3a Merge orphan placement rolls back Note and valid placement`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("rollback-placement-note", null)
                val valid =
                    fk10b3aPlacement(
                        "valid-placement",
                        "fk10b3a-workspace",
                        note.id,
                    )
                val orphan =
                    fk10b3aPlacement(
                        "orphan-placement",
                        "fk10b3a-workspace-b",
                        "missing-target-note",
                    )

                val outcome = runCatching {
                    merge(database).applySnapshotBundle(
                        fk10b3aPlacedBundle(note, listOf(valid, orphan)),
                    )
                }

                assertTrue(outcome.isFailure)
                assertNull(database.legacyNoteDao().getNoteById(note.id))
                assertNull(database.workspaceBacklogEntryDao().getById(valid.id))
                assertNull(database.workspaceBacklogEntryDao().getById(orphan.id))
                assertNull(database.contextDao().getContextById("missing-target-note"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3a Restore retains two placements and rolls back orphan replacement`() =
        runBlocking {
            val source = database()
            val destination = database()
            try {
                fk10b3aSeedNotePlacementOwners(source)
                val note = fk10b3aNoteSnapshot("restorable-multi-note", null)
                val first =
                    fk10b3aPlacement("restore-placement-a", "fk10b3a-workspace", note.id)
                val second =
                    fk10b3aPlacement("restore-placement-b", "fk10b3a-workspace-b", note.id)

                merge(source).applySnapshotBundle(
                    fk10b3aPlacedBundle(note, listOf(first, second)),
                )

                val exported = fullBackup(source).loadFullSnapshotBundle()
                assertEquals(
                    1,
                    exported.notes.count { it.id == note.id },
                )
                assertEquals(
                    2,
                    exported.workspaceBacklogEntries.orEmpty().count {
                        it.targetKind == "LEGACY_NOTE" && it.targetId == note.id
                    },
                )

                restore(destination, exported)

                val stored = requireNotNull(destination.legacyNoteDao().getNoteById(note.id))
                assertEquals(note.title, stored.title)
                assertEquals(note.content, stored.content)
                assertNull(stored.contextId)

                val restoredPlacements =
                    destination.workspaceBacklogEntryDao().getAll()
                        .filter { !it.isDeleted && it.targetId == note.id }
                assertEquals(2, restoredPlacements.size)
                assertEquals(
                    setOf(first.id, second.id),
                    restoredPlacements.map { it.id }.toSet(),
                )

                // An invalid replacement must not clear a previously valid graph.
                val invalid = exported.copy(
                    notes = exported.notes.filterNot { it.id == note.id },
                )
                val outcome = runCatching { restore(destination, invalid) }
                assertTrue(outcome.isFailure)

                assertEquals(note.content, requireNotNull(
                    destination.legacyNoteDao().getNoteById(note.id),
                ).content)
                assertEquals(
                    setOf(first.id, second.id),
                    destination.workspaceBacklogEntryDao().getAll()
                        .filter { !it.isDeleted && it.targetId == note.id }
                        .map { it.id }
                        .toSet(),
                )
            } finally {
                source.close()
                destination.close()
            }
        }

    @Test
    fun `FK10B3a Restore live Note survives deleted historical Context`() =
        runBlocking {
            val database = database()
            try {
                val contextId = "fk10b3a-deleted-historical-context"
                val note = fk10b3aNoteSnapshot("deleted-owner-note", contextId)

                restore(
                    database,
                    fk10b3aBundle(note).copy(
                        contexts =
                            listOf(
                                ordinaryContext(contextId)
                                    .copy(isDeleted = true)
                                    .toSnapshot(),
                            ),
                    ),
                )

                val restored = requireNotNull(database.legacyNoteDao().getNoteById(note.id))
                assertFalse(restored.isDeleted)
                assertEquals(contextId, restored.contextId)
                assertEquals(note.version, restored.version)
                assertTrue(requireNotNull(
                    database.contextDao().getContextById(contextId),
                ).isDeleted)
                assertTrue(database.workspaceBacklogEntryDao().getAll()
                    .none { !it.isDeleted && it.targetId == note.id })
            } finally {
                database.close()
            }
        }

    // FK.10B.3b: Selective Import uses the canonical Note admission and rollback boundary.

    @Test
    fun `FK10B3b Selective Note merge respects versions and historical locator`() =
        runBlocking {
            val database = database()
            try {
                val original =
                    fk10b3aNoteSnapshot("selective-versioned", "missing-original-context", version = 4L)
                merge(database).applySnapshotBundle(fk10b3aBundle(original))

                val selective = merge(database)
                val dao = database.legacyNoteDao()

                selective.applySelectiveSnapshotBundle(
                    fk10b3aBundle(original.copy(contextId = "missing-replay-context")),
                )
                assertEquals(original.contextId, requireNotNull(dao.getNoteById(original.id)).contextId)

                selective.applySelectiveSnapshotBundle(
                    fk10b3aBundle(
                        original.copy(
                            title = "Outdated title",
                            content = "Outdated body",
                            version = 3L,
                        ),
                    ),
                )
                assertEquals(original.title, requireNotNull(dao.getNoteById(original.id)).title)

                val newer =
                    original.copy(
                        title = "Selected newer title",
                        content = "Selected newer body",
                        contextId = "missing-newer-context",
                        updatedAt = 35L,
                        version = 5L,
                    )
                selective.applySelectiveSnapshotBundle(fk10b3aBundle(newer))

                val stored = requireNotNull(dao.getNoteById(original.id))
                assertEquals(newer.title, stored.title)
                assertEquals(newer.content, stored.content)
                assertEquals(newer.contextId, stored.contextId)
                assertEquals(newer.version, stored.version)
                assertNull(database.contextDao().getContextById("missing-newer-context"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3b Selective divergent Note conflict rolls back earlier Note`() =
        runBlocking {
            val database = database()
            try {
                val existing = fk10b3aNoteSnapshot("selective-conflict", null, version = 7L)
                merge(database).applySnapshotBundle(fk10b3aBundle(existing))

                val earlier = fk10b3aNoteSnapshot("selective-must-rollback", null)
                val divergent = existing.copy(content = "Divergent selective content")
                val outcome =
                    runCatching {
                        merge(database).applySelectiveSnapshotBundle(
                            fk10b3aBundle(earlier, divergent),
                        )
                    }

                assertTrue(outcome.isFailure)
                assertNull(database.legacyNoteDao().getNoteById(earlier.id))

                val preserved = requireNotNull(database.legacyNoteDao().getNoteById(existing.id))
                assertEquals(existing.content, preserved.content)
                assertEquals(existing.version, preserved.version)
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3b Selective admits two Workspace placements for one historical Note`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("selective-shared-note", "missing-legacy-context")
                val first = fk10b3aPlacement("selective-placement-a", "fk10b3a-workspace", note.id)
                val second = fk10b3aPlacement("selective-placement-b", "fk10b3a-workspace-b", note.id)

                merge(database).applySelectiveSnapshotBundle(
                    fk10b3aPlacedBundle(note, listOf(first, second)),
                )

                val stored = requireNotNull(database.legacyNoteDao().getNoteById(note.id))
                assertEquals(note.content, stored.content)
                assertEquals(note.contextId, stored.contextId)

                val placements =
                    database.workspaceBacklogEntryDao().getAll()
                        .filter { !it.isDeleted && it.targetKind == "LEGACY_NOTE" }

                assertEquals(setOf(first.id, second.id), placements.map { it.id }.toSet())
                assertEquals(setOf(note.id), placements.map { it.targetId }.toSet())
                assertEquals(1, database.legacyNoteDao().getAllRaw().count { it.id == note.id })
                assertNull(database.contextDao().getContextById("missing-legacy-context"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3b Selective orphan placement rolls back Note and valid placement`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("selective-rollback-note", null)
                val valid =
                    fk10b3aPlacement(
                        "selective-valid-placement",
                        "fk10b3a-workspace",
                        note.id,
                    )
                val orphan =
                    fk10b3aPlacement(
                        "selective-orphan-placement",
                        "fk10b3a-workspace-b",
                        "missing-selective-target",
                    )

                val outcome =
                    runCatching {
                        merge(database).applySelectiveSnapshotBundle(
                            fk10b3aPlacedBundle(note, listOf(valid, orphan)),
                        )
                    }

                assertTrue(outcome.isFailure)
                assertNull(database.legacyNoteDao().getNoteById(note.id))
                assertNull(database.workspaceBacklogEntryDao().getById(valid.id))
                assertNull(database.workspaceBacklogEntryDao().getById(orphan.id))
                assertNull(database.contextDao().getContextById("missing-selective-target"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3b Selective Note tombstone cannot orphan existing live placement`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("selective-live-note", null)
                val placement =
                    fk10b3aPlacement(
                        "selective-existing-placement",
                        "fk10b3a-workspace",
                        note.id,
                    )

                merge(database).applySnapshotBundle(
                    fk10b3aPlacedBundle(note, listOf(placement)),
                )

                val tombstone =
                    note.copy(
                        isDeleted = true,
                        updatedAt = 30L,
                        version = note.version + 1L,
                    )

                val outcome =
                    runCatching {
                        merge(database).applySelectiveSnapshotBundle(
                            fk10b3aBundle(tombstone),
                        )
                    }

                assertTrue(outcome.isFailure)

                val preserved = requireNotNull(database.legacyNoteDao().getNoteById(note.id))
                assertEquals(note.version, preserved.version)
                assertTrue(!preserved.isDeleted)

                val preservedPlacement =
                    requireNotNull(database.workspaceBacklogEntryDao().getById(placement.id))
                assertTrue(!preservedPlacement.isDeleted)
                assertEquals(note.id, preservedPlacement.targetId)
            } finally {
                database.close()
            }
        }

    // FK.10B.3c: Wi-Fi Note delta and ACK use independent Note authority.


    @Test
    fun `FK10B3c acknowledged tombstone is not selected again`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.legacyNoteDao()
                val tombstone =
                    com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity(
                        id = "wifi-ack-no-repeat-tombstone",
                        contextId = null,
                        title = "Deleted Note",
                        content = "Preserved tombstone content",
                        createdAt = 10L,
                        updatedAt = 20L,
                        isDeleted = true,
                        version = 4L,
                    )
                val live =
                    tombstone.copy(
                        id = "wifi-ack-no-repeat-live",
                        title = "Live Note",
                        isDeleted = false,
                        version = 1L,
                    )
                dao.insertAll(listOf(tombstone, live))

                val sync = sync(database)
                val selected = sync.getUnsyncedSelection()

                assertEquals(
                    setOf(
                        LocalSyncVersion(tombstone.id, tombstone.version),
                        LocalSyncVersion(live.id, live.version),
                    ),
                    selected.notes.toSet(),
                )

                sync.acknowledge(selected)

                val acknowledged =
                    requireNotNull(dao.getNoteById(tombstone.id))
                val acknowledgedLive =
                    requireNotNull(dao.getNoteById(live.id))

                assertNotNull(acknowledged.syncedAt)
                assertNotNull(acknowledgedLive.syncedAt)
                assertTrue(acknowledged.isDeleted)

                // Neither unchanged live content nor an ACKed tombstone
                // should be selected for another push.
                assertTrue(sync.getUnsyncedSelection().notes.isEmpty())

                // A subsequent Note version must still be admitted.
                val newer =
                    acknowledged.copy(
                        version = acknowledged.version + 1L,
                        updatedAt = acknowledged.updatedAt + 1L,
                        syncedAt = null,
                    )
                dao.update(newer)

                assertEquals(
                    listOf(LocalSyncVersion(newer.id, newer.version)),
                    sync.getUnsyncedSelection().notes,
                )

                // Replay only the stale tombstone V4 ACK. The unchanged
                // live Note V1 remains outside this replay.
                sync.acknowledge(
                    selected.copy(
                        notes = listOf(
                            LocalSyncVersion(tombstone.id, tombstone.version),
                        ),
                    ),
                )
                assertEquals(newer, dao.getNoteById(newer.id))
                assertEquals(
                    acknowledgedLive,
                    dao.getNoteById(live.id),
                )
            } finally {
                database.close()
            }
        }


    @Test
    fun `FK10B3c timestamp delta includes standalone Note and tombstone only after cutoff`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.legacyNoteDao()
                val changed =
                    com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity(
                        id = "wifi-delta-standalone",
                        contextId = "missing-wifi-historical-context",
                        title = "Changed standalone Note",
                        content = "Changed after cutoff",
                        createdAt = 10L,
                        updatedAt = 150L,
                        version = 3L,
                    )
                val deleted =
                    changed.copy(
                        id = "wifi-delta-tombstone",
                        contextId = null,
                        title = "Deleted standalone Note",
                        isDeleted = true,
                        updatedAt = 160L,
                        version = 4L,
                    )
                val unchanged =
                    changed.copy(
                        id = "wifi-delta-unchanged",
                        contextId = null,
                        updatedAt = 90L,
                    )
                val boundary =
                    changed.copy(
                        id = "wifi-delta-boundary",
                        contextId = null,
                        updatedAt = 100L,
                    )
                dao.insertAll(listOf(changed, deleted, unchanged, boundary))

                val delta = sync(database).getChangesSince(100L)

                assertEquals(
                    setOf(changed.id, deleted.id),
                    delta.notes.map { it.id }.toSet(),
                )
                assertEquals(2, delta.notes.size)
                assertEquals(changed.version, delta.notes.single { it.id == changed.id }.version)
                assertTrue(delta.notes.single { it.id == deleted.id }.isDeleted)
                assertTrue(delta.contexts.isEmpty())
                assertNull(database.contextDao().getContextById(changed.contextId!!))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3c BACKLOG placement update does not fabricate Note content delta`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("wifi-delta-shared-note", null)
                val first =
                    fk10b3aPlacement(
                        "wifi-delta-placement-a",
                        "fk10b3a-workspace",
                        note.id,
                    )
                val second =
                    fk10b3aPlacement(
                        "wifi-delta-placement-b",
                        "fk10b3a-workspace-b",
                        note.id,
                    )
                val merge = merge(database)
                merge.applySnapshotBundle(fk10b3aPlacedBundle(note, listOf(first, second)))

                val updatedPlacement =
                    first.copy(
                        order = 5L,
                        updatedAt = 200L,
                        version = first.version + 1L,
                    )
                merge.applySnapshotBundle(
                    fk10b3aPlacedBundle(note, listOf(updatedPlacement)),
                )

                val delta = sync(database).getChangesSince(note.updatedAt)

                assertTrue(delta.notes.isEmpty())
                assertEquals(
                    note.version,
                    requireNotNull(database.legacyNoteDao().getNoteById(note.id)).version,
                )
                assertEquals(
                    updatedPlacement.version,
                    requireNotNull(
                        database.workspaceBacklogEntryDao().getById(first.id),
                    ).version,
                )
                assertNotNull(database.workspaceBacklogEntryDao().getById(second.id))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3c standalone tombstone ACK rejects stale version and preserves payload`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.legacyNoteDao()
                val original =
                    com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity(
                        id = "wifi-ack-standalone",
                        contextId = "missing-wifi-ack-context",
                        title = "Before ACK race",
                        content = "Version 3",
                        createdAt = 10L,
                        updatedAt = 100L,
                        version = 3L,
                    )
                dao.insert(original)
                val sync = sync(database)
                val staleSelection = sync.getUnsyncedSelection()

                assertEquals(
                    listOf(LocalSyncVersion(original.id, original.version)),
                    staleSelection.notes,
                )

                val tombstone =
                    original.copy(
                        title = "Deleted after send",
                        content = "Version 4 tombstone",
                        updatedAt = 200L,
                        version = 4L,
                        isDeleted = true,
                        syncedAt = null,
                    )
                dao.update(tombstone)

                sync.acknowledge(staleSelection)
                assertEquals(tombstone, dao.getNoteById(tombstone.id))

                val freshSelection = sync.getUnsyncedSelection()
                assertEquals(
                    listOf(LocalSyncVersion(tombstone.id, tombstone.version)),
                    freshSelection.notes,
                )

                sync.acknowledge(freshSelection)
                val acknowledged = requireNotNull(dao.getNoteById(tombstone.id))
                assertNotNull(acknowledged.syncedAt)
                assertEquals(
                    tombstone.copy(syncedAt = acknowledged.syncedAt),
                    acknowledged,
                )
                assertTrue(acknowledged.isDeleted)

                sync.acknowledge(
                    freshSelection.copy(
                        notes = listOf(LocalSyncVersion("missing-wifi-ack-note", 4L)),
                    ),
                )
                assertEquals(acknowledged, dao.getNoteById(tombstone.id))
                assertNull(database.contextDao().getContextById(original.contextId!!))
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3c Note ACK leaves independent BACKLOG placement unchanged`() =
        runBlocking {
            val database = database()
            try {
                fk10b3aSeedNotePlacementOwners(database)

                val note = fk10b3aNoteSnapshot("wifi-ack-placed-note", null)
                val placement =
                    fk10b3aPlacement(
                        "wifi-ack-independent-placement",
                        "fk10b3a-workspace",
                        note.id,
                    )

                merge(database).applySnapshotBundle(
                    fk10b3aPlacedBundle(note, listOf(placement)),
                )

                val before =
                    requireNotNull(database.workspaceBacklogEntryDao().getById(placement.id))
                val sync = sync(database)
                val selected = sync.getUnsyncedSelection()

                assertEquals(
                    listOf(LocalSyncVersion(note.id, note.version)),
                    selected.notes,
                )
                sync.acknowledge(selected)

                val stored = requireNotNull(database.legacyNoteDao().getNoteById(note.id))
                assertNotNull(stored.syncedAt)
                assertEquals(
                    before,
                    database.workspaceBacklogEntryDao().getById(placement.id),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `FK10B3c WiFi canonical ingress follows version policy and rolls back conflict`() =
        runBlocking {
            val database = database()
            try {
                val incoming =
                    fk10b3aNoteSnapshot(
                        "wifi-ingress-standalone",
                        "missing-wifi-ingress-context",
                        version = 3L,
                    )
                val merge = merge(database)
                val dao = database.legacyNoteDao()

                merge.applySnapshotBundle(fk10b3aBundle(incoming))
                assertEquals(incoming.content, requireNotNull(dao.getNoteById(incoming.id)).content)
                assertNull(
                    database.contextDao().getContextById("missing-wifi-ingress-context"),
                )

                val newer =
                    incoming.copy(
                        title = "Newer canonical Note",
                        content = "Wi-Fi version 4",
                        updatedAt = 40L,
                        version = 4L,
                    )
                merge.applySnapshotBundle(fk10b3aBundle(newer))
                merge.applySnapshotBundle(fk10b3aBundle(incoming))
                assertEquals(newer.content, requireNotNull(dao.getNoteById(newer.id)).content)

                merge.applySnapshotBundle(
                    fk10b3aBundle(newer.copy(contextId = "another-missing-context")),
                )
                assertEquals(
                    newer.contextId,
                    requireNotNull(dao.getNoteById(newer.id)).contextId,
                )

                val earlier = fk10b3aNoteSnapshot("wifi-conflict-rollback", null)
                val divergent = newer.copy(content = "Conflicting equal-version Wi-Fi body")
                val conflict =
                    runCatching {
                        merge.applySnapshotBundle(fk10b3aBundle(earlier, divergent))
                    }

                assertTrue(conflict.isFailure)
                assertNull(dao.getNoteById(earlier.id))
                assertEquals(newer.content, requireNotNull(dao.getNoteById(newer.id)).content)

                val tombstone =
                    newer.copy(
                        isDeleted = true,
                        updatedAt = 50L,
                        version = 5L,
                    )
                merge.applySnapshotBundle(fk10b3aBundle(tombstone))
                merge.applySnapshotBundle(fk10b3aBundle(newer))

                val stored = requireNotNull(dao.getNoteById(tombstone.id))
                assertTrue(stored.isDeleted)
                assertEquals(tombstone.version, stored.version)

                val resurrection =
                    runCatching {
                        merge.applySnapshotBundle(
                            fk10b3aBundle(tombstone.copy(isDeleted = false)),
                        )
                    }
                assertTrue(resurrection.isFailure)
                assertEquals(stored, dao.getNoteById(stored.id))
                assertNull(database.contextDao().getContextById("another-missing-context"))
            } finally {
                database.close()
            }
        }

    private suspend fun fk10b3aSeedNotePlacementOwners(database: AppDatabase) {
        val workspaceIds = listOf("fk10b3a-workspace", "fk10b3a-workspace-b")
        database.workspaceDao().upsert(workspaceIds.map(::canonicalOnlyWorkspace))
        database.orientationDao().upsertWorkspaceCapabilities(
            workspaceIds.map { id ->
                com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity(
                    id = "backlog-$id",
                    workspaceId = id,
                    capabilityType = "BACKLOG",
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = "ACTIVE",
                    configurationVersion = 1,
                    configuration = "{}",
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                )
            },
        )
    }

    private fun fk10b3aPlacement(
        id: String,
        workspaceId: String,
        noteId: String,
    ) = com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = "backlog-$workspaceId",
        targetKind = "LEGACY_NOTE",
        targetId = noteId,
        order = 0L,
        createdAt = 1L,
        updatedAt = 20L,
        version = 1L,
        isDeleted = false,
    )

    private fun fk10b3aPlacedBundle(
        note: LegacyNoteSnapshot,
        placements: List<com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot>,
    ): SnapshotBundle =
        fk10b3aBundle(note).copy(
            workspaces =
                listOf("fk10b3a-workspace", "fk10b3a-workspace-b")
                    .map { id ->
                        canonicalOnlyWorkspace(id)
                            .toWorkspaceSnapshot()
                            .withoutEmbeddedWorkspaceTopology()
                    },
            workspaceBacklogEntries = placements,
        )

    private fun fk10b3aNoteSnapshot(
        id: String,
        contextId: String?,
        deleted: Boolean = false,
        version: Long = 2L,
    ) = LegacyNoteSnapshot(
        id = id,
        contextId = contextId,
        title = "Title-$id",
        content = "Body-$id",
        createdAt = 10L,
        updatedAt = 20L,
        isDeleted = deleted,
        version = version,
    )

    private fun fk10b3aBundle(
        vararg notes: LegacyNoteSnapshot,
    ): SnapshotBundle =
        canonicalBundle(
            context = ordinaryContext("fk10b3a-fixture").copy(isDeleted = true),
            workspace = canonicalOnlyWorkspace("fk10b3a-workspace"),
        ).copy(
            contexts = emptyList(),
            notes = notes.toList(),
            workspaceBacklogEntries = emptyList(),
            workspaceInboxRecords = emptyList(),
        )

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

    private fun merge(
        database: AppDatabase,
        scoringActivator: CanonicalScoringAuthorityActivator? = null,
        orientationBootstrapper: CanonicalOrientationBootstrapper? = null,
    ): MergeLocalDataSourceImpl {
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

        val integrationDependencies = mutableMapOf<Class<*>, Any>()
        scoringActivator?.let {
            integrationDependencies[CanonicalScoringAuthorityActivator::class.java] = it
        }
        orientationBootstrapper?.let {
            integrationDependencies[CanonicalOrientationBootstrapper::class.java] = it
        }


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
                ) + integrationDependencies,
        )
    }

    private fun sync(database: AppDatabase): SyncLocalDataSourceImpl =
        construct(
            type = SyncLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    ContextDao::class.java to database.contextDao(),
                    LegacyNoteDao::class.java to database.legacyNoteDao(),
                    ActivityRecordDao::class.java to database.activityRecordDao(),
                    DayPlanDao::class.java to database.dayPlanDao(),
                    DayTaskDao::class.java to database.dayTaskDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    TacticalActivitySlotDao::class.java to database.tacticalActivitySlotDao(),
                    SyncLogicHelper::class.java to SyncLogicHelper(),
                ),
        )

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
        // Kotlin default arguments generate a synthetic constructor with
        // a DefaultConstructorMarker. Use the real declared constructor.
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

    private companion object {
        const val RETIRED_ID = "transport-retired-context"
        const val CONTEXT_BACKED_ID = "transport-context-backed"
    }
}

private fun retiredMainBeaconTableAbsentForTransport(database: com.romankozak.forwardappmobile.database.AppDatabase): Boolean =
    database.openHelper.writableDatabase.query(
        "SELECT COUNT(*) FROM sqlite_master " +
            "WHERE type = 'table' AND name = 'main_beacon_context_cross_ref'",
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0) == 0L
    }
