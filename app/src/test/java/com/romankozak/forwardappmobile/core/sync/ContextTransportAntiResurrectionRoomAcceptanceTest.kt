package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
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
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
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
                        writer = CanonicalSnapshotTransactionWriter { merge(destination).applyCanonicalSnapshotBundle(it) },
                        dayManagementRuntimeRepository = mockk<DayManagementRuntimeRepository>(relaxed = true),
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
    fun `merge cannot resurrect Context retired by incoming canonical Workspace`() =
        runBlocking {
            val destination = database()
            try {
                val liveLegacy = ordinaryContext(RETIRED_ID)

                merge(destination).applySnapshotBundle(
                    canonicalBundle(
                        context = liveLegacy,
                        workspace = canonicalOnlyWorkspace(RETIRED_ID),
                    ),
                )

                val persisted = destination.contextDao().getContextById(RETIRED_ID)
                assertTrue(persisted == null || persisted.isDeleted)

                val workspace = destination.workspaceDao().getById(RETIRED_ID)
                assertTrue(workspace != null)
                assertTrue(workspace!!.provenance == WorkspaceProvenance.CANONICAL_ONLY.name)
                assertNull(workspace.sourceContextId)
            } finally {
                destination.close()
            }
        }

    @Test
    fun `merge cannot resurrect Context retired by local canonical Workspace`() =
        runBlocking {
            val database = database()
            try {
                database.workspaceDao().upsert(
                    listOf(canonicalOnlyWorkspace(RETIRED_ID)),
                )

                merge(database).applySnapshotBundle(
                    SnapshotBundle(
                        version = 1,
                        contexts = listOf(ordinaryContext(RETIRED_ID).toSnapshot()),
                        hierarchyPlacements = emptyList(),
                        hierarchyPlacementGroupScopes = emptyList(),
                        hierarchyPlacementLinkedAppearances = emptyList(),
                    ),
                )

                val persisted = database.contextDao().getContextById(RETIRED_ID)
                assertTrue(persisted == null || persisted.isDeleted)
            } finally {
                database.close()
            }
        }

    @Test
    fun `retired Context tombstone remains valid transport evidence`() =
        runBlocking {
            val destination = database()
            try {
                val tombstone =
                    ordinaryContext(RETIRED_ID).copy(
                        isDeleted = true,
                        version = 7L,
                    )

                merge(destination).applySnapshotBundle(
                    canonicalBundle(
                        context = tombstone,
                        workspace = canonicalOnlyWorkspace(RETIRED_ID),
                    ),
                )

                val persisted = destination.contextDao().getContextById(RETIRED_ID)
                assertTrue(persisted != null)
                assertTrue(persisted!!.isDeleted)
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
    fun `sync selection and delta exclude live retired Context but keep Context-backed Context`() =
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
                assertFalse(selection.contexts.any { it.id == RETIRED_ID })
                assertTrue(selection.contexts.any { it.id == CONTEXT_BACKED_ID })

                val delta = sync.getChangesSince(0L)
                assertFalse(
                    delta.contexts.any {
                        it.id == RETIRED_ID && !it.isDeleted
                    },
                )
                assertTrue(
                    delta.contexts.any {
                        it.id == CONTEXT_BACKED_ID && !it.isDeleted
                    },
                )
                assertTrue(delta.contextParentLinks.isEmpty())
                assertTrue(delta.mainBeaconParentLinks.isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync acknowledge cannot rewrite retired live Context`() =
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
                sync.acknowledge(
                    baseline.copy(
                        contexts =
                            listOf(
                                LocalSyncVersion(
                                    id = RETIRED_ID,
                                    version = before.version,
                                ),
                            ),
                    ),
                )

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
                    MainBeaconDao::class.java to database.mainBeaconDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    CanonicalWorkspaceBootstrapper::class.java to bootstrapper,
                    ContextWorkspaceWriteThrough::class.java to ContextWorkspaceWriteThrough(bootstrapper),
                    DatabaseInitializer::class.java to initializer(database),
                    CanonicalWorkspaceProblemSyncStore::class.java to problemStore,
                    CanonicalWorkspaceTagTransportStore::class.java to
                        CanonicalWorkspaceTagTransportStore(database),
                    SystemWorkspaceTagSeed::class.java to
                        SystemWorkspaceTagSeed(database, database.contextDao()),
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
                    merge(database).applyCanonicalSnapshotBundle(it)
                },
            dayManagementRuntimeRepository =
                mockk<DayManagementRuntimeRepository>(relaxed = true),
        ).replaceWith(canonical)
    }

    private fun merge(
        database: AppDatabase,
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

    private fun sync(database: AppDatabase): SyncLocalDataSourceImpl =
        construct(
            type = SyncLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    ContextDao::class.java to database.contextDao(),
                    ActivityRecordDao::class.java to database.activityRecordDao(),
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
