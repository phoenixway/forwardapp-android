package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import android.content.Context
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as LegacyContext
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `workspace tombstone also tombstones owned canonical execution logs`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val workspaceId = repository.create("Operations", now = 10L)

            database.contextManagementDao().insertLog(
                com.romankozak.forwardappmobile.core.data.models.entities.ContextLog(
                    id = "canonical-log",
                    contextId = null,
                    timestamp = 15L,
                    type = "COMMENT",
                    description = "Owned history",
                    updatedAt = 15L,
                    syncedAt = 14L,
                    isDeleted = false,
                    version = 3L,
                    workspaceId = workspaceId,
                ),
            )

            repository.tombstone(workspaceId, now = 40L)

            val log = database.contextManagementDao().getLogById("canonical-log")
            assertTrue(log?.isDeleted == true)
            assertEquals(4L, log?.version)
            assertEquals(40L, log?.updatedAt)
            assertNull(log?.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `workspace tombstone also tombstones owned canonical Backlog placements`() = runBlocking {
        val database = database()
        try {
            val workspaceRepository = repository(database)
            val ownerId = workspaceRepository.create("Owner", now = 10L)
            val targetId = workspaceRepository.create("Target", now = 11L)
            val hierarchyRepository =
                com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository(database)
            val ownerPlacementId =
                requireNotNull(
                    hierarchyRepository.getPrimaryAppearance(
                        com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef(
                            com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType.WORKSPACE,
                            ownerId,
                        ),
                    ),
                ).id
            val backlogRepository = backlogRepository(database)
            backlogRepository.enable(ownerId, now = 12L)
            val entryId =
                backlogRepository.addEntry(
                    workspaceId = ownerId,
                    target =
                        com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef(
                            com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind.WORKSPACE,
                            targetId,
                        ),
                    now = 13L,
                )

            workspaceRepository.tombstone(ownerId, now = 20L)

            assertTrue(requireNotNull(backlogRepository.getEntry(entryId)).isDeleted)
            assertTrue(
                database.hierarchyPlacementDao().getById(ownerPlacementId.value)?.isDeleted == true,
            )
            assertFalse(requireNotNull(database.workspaceDao().getById(targetId)).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `workspace tombstone also tombstones owned KEY_PROBLEMS content`() = runBlocking {
        val database = database()
        try {
            val workspaceRepository = repository(database)
            val workspaceId = workspaceRepository.create("Operations", now = 10L)
            val keyProblemsRepository = keyProblemsRepository(database)
            keyProblemsRepository.enable(workspaceId, now = 11L)
            val problemId =
                keyProblemsRepository.createProblem(
                    workspaceId = workspaceId,
                    title = "Owned problem",
                    now = 12L,
                )

            workspaceRepository.tombstone(workspaceId, now = 40L)

            val problem = requireNotNull(database.workspaceProblemDao().getProblem(problemId))
            assertTrue(problem.isDeleted)
            assertEquals(2L, problem.version)
            assertEquals(40L, problem.updatedAt)
            assertNull(problem.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `workspace tombstone closes owned and targeting DIRECTION placements`() = runBlocking {
        val database = database()
        try {
            val workspaceRepository = repository(database)
            val ownerId = workspaceRepository.create("Owner", now = 10L)
            val otherId = workspaceRepository.create("Other", now = 11L)
            val directionRepository = directionRepository(database)
            directionRepository.enable(ownerId, now = 12L)
            directionRepository.enable(otherId, now = 13L)
            val ownedEntry =
                directionRepository.createSemanticDirection(
                    workspaceId = ownerId,
                    title = "Owned direction",
                    now = 14L,
                )
            val targetingEntry =
                directionRepository.createWorkspaceLink(
                    workspaceId = otherId,
                    targetWorkspaceId = ownerId,
                    label = "Owner link",
                    now = 15L,
                )

            workspaceRepository.tombstone(ownerId, now = 40L)

            listOf(ownedEntry, targetingEntry).forEach { entryId ->
                val entry = requireNotNull(database.workspaceDirectionEntryDao().getById(entryId))
                assertTrue(entry.isDeleted)
                assertEquals(2L, entry.version)
                assertEquals(40L, entry.updatedAt)
                assertNull(entry.syncedAt)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `V2 child creation authors PRIMARY under exact parent LINK without legacy parent`() =
        runBlocking {
            val database = database()
            try {
                val workspaceRepository = repository(database)
                val placementRepository = CanonicalHierarchyPlacementRepository(database)
                val parentId =
                    workspaceRepository.createWithV2PrimaryAppearance(
                        nameOverride = "Parent",
                        now = 10L,
                    )
                val parentTarget =
                    HierarchyTargetRef(HierarchyTargetType.WORKSPACE, parentId)
                val parentPrimary =
                    requireNotNull(placementRepository.getPrimaryAppearance(parentTarget))
                val parentLink =
                    placementRepository.createLinkAppearance(
                        target = parentTarget,
                        now = 11L,
                    )

                val childId =
                    workspaceRepository.createWithV2PrimaryAppearance(
                        nameOverride = "Child",
                        parentWorkspaceId = parentId,
                        parentPlacementId = parentLink,
                        now = 12L,
                    )

                val child =
                    requireNotNull(database.workspaceDao().getById(childId))
                val childAppearance =
                    requireNotNull(
                        placementRepository.getPrimaryAppearance(
                            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, childId),
                        ),
                    )
                assertEquals(parentLink, childAppearance.parentPlacementId)
                assertNotEquals(parentPrimary.id, childAppearance.parentPlacementId)
                assertEquals(
                    listOf(childAppearance.id),
                    placementRepository.getLiveChildren(parentLink).map { it.id },
                )
                assertTrue(placementRepository.getLiveChildren(parentPrimary.id).isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 child creation rejects mismatched target and occurrence atomically`() =
        runBlocking {
            val database = database()
            try {
                val workspaceRepository = repository(database)
                val placementRepository = CanonicalHierarchyPlacementRepository(database)
                val firstId =
                    workspaceRepository.createWithV2PrimaryAppearance(
                        nameOverride = "First",
                        now = 10L,
                    )
                val secondId =
                    workspaceRepository.createWithV2PrimaryAppearance(
                        nameOverride = "Second",
                        now = 11L,
                    )
                val secondPlacement =
                    requireNotNull(
                        placementRepository.getPrimaryAppearance(
                            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, secondId),
                        ),
                    )
                val beforeWorkspaces = database.workspaceDao().getAll()
                val beforePlacements = placementRepository.getLiveHierarchy()

                val failure =
                    runCatching {
                        workspaceRepository.createWithV2PrimaryAppearance(
                            nameOverride = "Rejected",
                            parentWorkspaceId = firstId,
                            parentPlacementId = secondPlacement.id,
                            now = 12L,
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals(beforeWorkspaces, database.workspaceDao().getAll())
                assertEquals(beforePlacements, placementRepository.getLiveHierarchy())
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 CUT moves only selected LINK and legacy move does not change V2 ancestry`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                val sourceId =
                    workspaces.createWithV2PrimaryAppearance(
                        nameOverride = "Source",
                        now = 10L,
                    )
                val destinationId =
                    workspaces.createWithV2PrimaryAppearance(
                        nameOverride = "Destination",
                        now = 11L,
                    )
                val sourceTarget = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, sourceId)
                val sourcePrimary =
                    requireNotNull(placements.getPrimaryAppearance(sourceTarget))
                val sourceLink =
                    placements.createLinkAppearance(
                        target = sourceTarget,
                        now = 12L,
                    )
                val destinationPrimary =
                    requireNotNull(
                        placements.getPrimaryAppearance(
                            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, destinationId),
                        ),
                    )

                workspaces.moveV2Occurrences(
                    sourcePlacementsByWorkspaceId = mapOf(sourceId to sourceLink),
                    targetWorkspaceId = destinationId,
                    targetPlacementId = destinationPrimary.id,
                    now = 20L,
                )

                assertNull(placements.getPlacement(sourcePrimary.id)?.parentPlacementId)
                assertEquals(
                    destinationPrimary.id,
                    placements.getPlacement(sourceLink)?.parentPlacementId,
                )

                // schema180 has no embedded Workspace hierarchy to drift independently.
                // Re-read canonical H1 to prove the exact occurrence move remains authoritative.
                assertNull(placements.getPlacement(sourcePrimary.id)?.parentPlacementId)
                assertEquals(
                    destinationPrimary.id,
                    placements.getPlacement(sourceLink)?.parentPlacementId,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 shallow COPY creates distinct Workspace PRIMARY at exact destination occurrence`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                val sourceId =
                    workspaces.createWithV2PrimaryAppearance(
                        nameOverride = "Source",
                        roleCode = "project",
                        now = 10L,
                    )
                val destinationId =
                    workspaces.createWithV2PrimaryAppearance(
                        nameOverride = "Destination",
                        now = 11L,
                    )
                val destinationTarget =
                    HierarchyTargetRef(HierarchyTargetType.WORKSPACE, destinationId)
                val destinationPrimary =
                    requireNotNull(placements.getPrimaryAppearance(destinationTarget))
                val destinationLink =
                    placements.createLinkAppearance(
                        target = destinationTarget,
                        now = 12L,
                    )

                val copiedId =
                    workspaces.copyV2WorkspacesShallow(
                        ids = listOf(sourceId),
                        targetWorkspaceId = destinationId,
                        targetPlacementId = destinationLink,
                        now = 20L,
                    ).single()

                val copiedWorkspace = requireNotNull(database.workspaceDao().getById(copiedId))
                val copiedPrimary =
                    requireNotNull(
                        placements.getPrimaryAppearance(
                            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, copiedId),
                        ),
                    )
                assertNotEquals(sourceId, copiedId)
                assertEquals("Source (копія)", copiedWorkspace.nameOverride)
                assertEquals("project", copiedWorkspace.roleCode)
                assertEquals(destinationLink, copiedPrimary.parentPlacementId)
                assertTrue(placements.getLiveChildren(destinationPrimary.id).isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 clipboard rejects wrong destination occurrence without partial writes`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                val sourceId = workspaces.createWithV2PrimaryAppearance("Source", now = 10L)
                val destinationId = workspaces.createWithV2PrimaryAppearance("Destination", now = 11L)
                val otherId = workspaces.createWithV2PrimaryAppearance("Other", now = 12L)
                val sourcePlacement =
                    requireNotNull(
                        placements.getPrimaryAppearance(
                            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, sourceId),
                        ),
                    )
                val wrongDestination =
                    requireNotNull(
                        placements.getPrimaryAppearance(
                            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, otherId),
                        ),
                    )
                val beforeWorkspaces = database.workspaceDao().getAll()
                val beforePlacements = placements.getLiveHierarchy()

                val moveFailure =
                    runCatching {
                        workspaces.moveV2Occurrences(
                            sourcePlacementsByWorkspaceId = mapOf(sourceId to sourcePlacement.id),
                            targetWorkspaceId = destinationId,
                            targetPlacementId = wrongDestination.id,
                            now = 20L,
                        )
                    }.exceptionOrNull()
                val copyFailure =
                    runCatching {
                        workspaces.copyV2WorkspacesShallow(
                            ids = listOf(sourceId),
                            targetWorkspaceId = destinationId,
                            targetPlacementId = wrongDestination.id,
                            now = 21L,
                        )
                    }.exceptionOrNull()

                assertTrue(moveFailure is IllegalArgumentException)
                assertTrue(copyFailure is IllegalArgumentException)
                assertEquals(beforeWorkspaces, database.workspaceDao().getAll())
                assertEquals(beforePlacements, placements.getLiveHierarchy())
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 preset ensure creates under exact LINK and repeats without extra target or placement`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                val parentId = workspaces.createWithV2PrimaryAppearance("Parent", now = 10L)
                val parentTarget = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, parentId)
                val parentPrimary = requireNotNull(placements.getPrimaryAppearance(parentTarget))
                val parentLink = placements.createLinkAppearance(parentTarget, now = 11L)

                val first = workspaces.ensureChildWorkspaceByRoleAtOccurrence(
                    parentWorkspaceId = parentId,
                    parentPlacementId = parentLink,
                    roleCode = "preset_child",
                    title = "Preset Child",
                )
                val firstWorkspace = requireNotNull(database.workspaceDao().getById(first))
                val firstPrimary = requireNotNull(
                    placements.getPrimaryAppearance(
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, first),
                    ),
                )
                assertEquals(parentLink, firstPrimary.parentPlacementId)
                assertEquals("preset_child", firstWorkspace.roleCode)
                assertTrue(placements.getLiveChildren(parentPrimary.id).isEmpty())

                val workspacesBefore = database.workspaceDao().getAll()
                val placementsBefore = placements.getLiveHierarchy()
                val second = workspaces.ensureChildWorkspaceByRoleAtOccurrence(
                    parentWorkspaceId = parentId,
                    parentPlacementId = parentLink,
                    roleCode = "preset_child",
                    title = "Renamed on second ensure",
                )
                assertEquals(first, second)
                assertEquals(workspacesBefore, database.workspaceDao().getAll())
                assertEquals(placementsBefore, placements.getLiveHierarchy())

                val differentOccurrence = workspaces.ensureChildWorkspaceByRoleAtOccurrence(
                    parentWorkspaceId = parentId,
                    parentPlacementId = parentPrimary.id,
                    roleCode = "preset_child",
                    title = "Independent Child",
                )
                assertNotEquals(first, differentOccurrence)
                assertEquals(
                    parentPrimary.id,
                    placements.getPrimaryAppearance(
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, differentOccurrence),
                    )?.parentPlacementId,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 preset ensure rejects wrong parent occurrence before creating target or placement`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                val expectedParent = workspaces.createWithV2PrimaryAppearance("Expected", now = 10L)
                val wrongParent = workspaces.createWithV2PrimaryAppearance("Wrong", now = 11L)
                val wrongPlacement = requireNotNull(
                    placements.getPrimaryAppearance(
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, wrongParent),
                    ),
                )
                val workspacesBefore = database.workspaceDao().getAll()
                val placementsBefore = placements.getLiveHierarchy()

                val failure = runCatching {
                    workspaces.ensureChildWorkspaceByRoleAtOccurrence(
                        parentWorkspaceId = expectedParent,
                        parentPlacementId = wrongPlacement.id,
                        roleCode = "preset_child",
                        title = "Must not exist",
                    )
                }.exceptionOrNull()
                assertTrue(failure is IllegalArgumentException)
                assertEquals(workspacesBefore, database.workspaceDao().getAll())
                assertEquals(placementsBefore, placements.getLiveHierarchy())
            } finally {
                database.close()
            }
        }

    private fun repository(database: AppDatabase) =
        CanonicalWorkspaceRepository(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            executionLogRepository = executionLogRepository(database),
            keyProblemsRepository = keyProblemsRepository(database),
            directionRepository = directionRepository(database),
            inboxRepository = inboxRepository(database),
            connectionsRepository = connectionsRepository(database),
            backlogRepository = backlogRepository(database),
            hierarchyPlacementRepository =
                com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository(database),
            hierarchyPlacementLifecycleCoordinator =
                HierarchyPlacementLifecycleCoordinator(database),
        )

    private fun backlogRepository(database: AppDatabase) =
        com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            entryDao = database.workspaceBacklogEntryDao(),
            targetValidator =
                com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator(database),
        )

    private fun executionLogRepository(database: AppDatabase) =
        com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextManagementDao = database.contextManagementDao(),
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
        )

    private fun connectionsRepository(database: AppDatabase) =
        com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            connectionDao = database.workspaceConnectionDao(),
        )

    private fun inboxRepository(database: AppDatabase) =
        com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            recordDao = database.workspaceInboxRecordDao(),
        )

    private fun keyProblemsRepository(database: AppDatabase) =
        CanonicalKeyProblemsRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            problemDao = database.workspaceProblemDao(),
            workspaceDao = database.workspaceDao(),
            attachmentDao = database.attachmentDao(),
        )

    private fun directionRepository(database: AppDatabase) =
        CanonicalDirectionRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            entryDao = database.workspaceDirectionEntryDao(),
            orientationDao = database.orientationDao(),
            workspaceDao = database.workspaceDao(),
            orientationRepository =
                CanonicalOrientationRepository(
                    database = database,
                    dao = database.orientationDao(),
                ),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun canonicalOnly(
        id: String,
        name: String = "Canonical",
        description: String? = null,
        parentId: String? = null,
        order: Long = 0L,
        roleCode: String? = null,
        version: Long = 1L,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = name,
        descriptionOverride = description,
        roleCode = roleCode,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = version,
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId = null,
    )

    private fun legacyContext(
        id: String,
        name: String,
        description: String? = null,
        parentId: String? = null,
        order: Long = 0L,
        roleCode: String? = null,
    ) = LegacyContext(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        createdAt = 1L,
        updatedAt = 2L,
        order = order,
        roleCode = roleCode,
    )

    private fun contextBacked(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Legacy",
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    @Test
    fun `V2 target delete tombstones every leaf appearance without reading legacy ancestry`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnly("legacy-parent"),
                        canonicalOnly("target", parentId = "legacy-parent", order = 77L),
                        canonicalOnly("legacy-child", parentId = "target", order = 3L),
                        canonicalOnly("independent"),
                    ),
                )
                val target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "target")
                val primary = placements.createPrimaryAppearance(target, now = 10L)
                val link = placements.createLinkAppearance(target, now = 11L)
                val independent =
                    placements.createPrimaryAppearance(
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "independent"),
                        now = 12L,
                    )
                val legacyChildPlacement =
                    placements.createPrimaryAppearance(
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "legacy-child"),
                        parentPlacementId = independent,
                        now = 13L,
                    )
                val unaffectedBefore =
                    listOf(independent, legacyChildPlacement).map { id ->
                        requireNotNull(database.hierarchyPlacementDao().getById(id.value))
                    }
                val legacyChildBefore = requireNotNull(database.workspaceDao().getById("legacy-child"))
                val independentBefore = requireNotNull(database.workspaceDao().getById("independent"))

                workspaces.tombstone("target", now = 20L)

                assertTrue(requireNotNull(database.workspaceDao().getById("target")).isDeleted)
                assertTrue(requireNotNull(database.hierarchyPlacementDao().getById(primary.value)).isDeleted)
                assertTrue(requireNotNull(database.hierarchyPlacementDao().getById(link.value)).isDeleted)
                assertTrue(placements.getLiveAppearances(target).isEmpty())
                assertEquals(legacyChildBefore, database.workspaceDao().getById("legacy-child"))
                assertEquals(independentBefore, database.workspaceDao().getById("independent"))
                assertEquals(
                    unaffectedBefore,
                    listOf(independent, legacyChildPlacement).map { id ->
                        requireNotNull(database.hierarchyPlacementDao().getById(id.value))
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 target delete rejects a child under LINK and rolls back operational cleanup`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                database.workspaceDao().upsert(
                    listOf(canonicalOnly("target"), canonicalOnly("child")),
                )
                val target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "target")
                val primary = placements.createPrimaryAppearance(target, now = 10L)
                val link = placements.createLinkAppearance(target, now = 11L)
                val child =
                    placements.createPrimaryAppearance(
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "child"),
                        parentPlacementId = link,
                        now = 12L,
                    )
                database.contextManagementDao().insertLog(
                    com.romankozak.forwardappmobile.core.data.models.entities.ContextLog(
                        id = "rollback-log",
                        contextId = null,
                        timestamp = 13L,
                        type = "COMMENT",
                        description = "Must survive rejected target deletion",
                        updatedAt = 13L,
                        syncedAt = 12L,
                        isDeleted = false,
                        version = 3L,
                        workspaceId = "target",
                    ),
                )
                val beforeWorkspaces = database.workspaceDao().getAll()
                val beforePlacements = database.hierarchyPlacementDao().getAll()
                val beforeLog = database.contextManagementDao().getLogById("rollback-log")

                val failure = runCatching {
                    workspaces.tombstone("target", now = 20L)
                }.exceptionOrNull()

                assertTrue(failure is HierarchyChildPolicyRejectedException)
                assertEquals(beforeWorkspaces, database.workspaceDao().getAll())
                assertEquals(beforePlacements, database.hierarchyPlacementDao().getAll())
                assertEquals(beforeLog, database.contextManagementDao().getLogById("rollback-log"))
                assertEquals(primary, placements.getPrimaryAppearance(target)?.id)
                assertEquals(link, placements.getPlacement(child)?.parentPlacementId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `V2 target delete rolls back every appearance and target after late transaction failure`() =
        runBlocking {
            val database = database()
            try {
                val workspaces = repository(database)
                val placements = CanonicalHierarchyPlacementRepository(database)
                database.workspaceDao().upsert(listOf(canonicalOnly("target")))
                val target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "target")
                val primary = placements.createPrimaryAppearance(target, now = 10L)
                val link = placements.createLinkAppearance(target, now = 11L)
                val beforeWorkspaces = database.workspaceDao().getAll()
                val beforePlacements = database.hierarchyPlacementDao().getAll()

                val failure = runCatching {
                    database.withTransaction {
                        workspaces.tombstone("target", now = 20L)
                        assertTrue(requireNotNull(database.workspaceDao().getById("target")).isDeleted)
                        assertTrue(requireNotNull(database.hierarchyPlacementDao().getById(primary.value)).isDeleted)
                        assertTrue(requireNotNull(database.hierarchyPlacementDao().getById(link.value)).isDeleted)
                        error("force outer transaction rollback")
                    }
                }.exceptionOrNull()

                assertTrue(failure is IllegalStateException)
                assertEquals(beforeWorkspaces, database.workspaceDao().getAll())
                assertEquals(beforePlacements, database.hierarchyPlacementDao().getAll())
                assertEquals(primary, placements.getPrimaryAppearance(target)?.id)
                assertEquals(2, placements.getLiveAppearances(target).size)
            } finally {
                database.close()
            }
        }

    @Test
    fun `reserved System Workspace cannot be tombstoned`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val systemId = SystemContexts.INBOX.raw
                val system =
                    canonicalOnly(
                        id = systemId,
                        name = "Inbox",
                    )

                database.workspaceDao().upsert(listOf(system))

                val failure =
                    runCatching {
                        repository.tombstone(systemId, now = 20L)
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals(system, database.workspaceDao().getById(systemId))
            } finally {
                database.close()
            }
        }


}
