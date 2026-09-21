package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import android.content.Context
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as LegacyContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
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
    fun `live canonical Workspace ancestry presentation is shell-free and read-only`() =
        runBlocking {
            val database = database()
            try {
                val rootId = "workspace-root"
                val parentId = "workspace-parent"
                val systemId = SystemContexts.INBOX.raw
                val root = canonicalOnly(rootId, name = "Canonical root")
                val parent = canonicalOnly(parentId, name = "Canonical parent", parentId = rootId)
                val system =
                    canonicalOnly(
                        id = systemId,
                        name = "Canonical Inbox",
                        parentId = parentId,
                    )
                database.workspaceDao().upsert(listOf(root, parent, system))
                val repository = repository(database)

                val resolvedSystem =
                    requireNotNull(repository.getLiveCanonicalAncestryPresentation(systemId))
                val resolvedParent =
                    requireNotNull(repository.getLiveCanonicalAncestryPresentation(parentId))
                val resolvedRoot =
                    requireNotNull(repository.getLiveCanonicalAncestryPresentation(rootId))

                assertEquals("Canonical Inbox", resolvedSystem.name)
                assertEquals(parentId, resolvedSystem.parentWorkspaceId)
                assertEquals("Canonical parent", resolvedParent.name)
                assertEquals(rootId, resolvedParent.parentWorkspaceId)
                assertEquals("Canonical root", resolvedRoot.name)
                assertNull(resolvedRoot.parentWorkspaceId)
                assertNull(database.contextDao().getContextById(systemId))
                assertNull(database.contextDao().getContextById(parentId))
                assertNull(database.contextDao().getContextById(rootId))
                assertEquals(system, database.workspaceDao().getById(systemId))
                assertEquals(parent, database.workspaceDao().getById(parentId))
                assertEquals(root, database.workspaceDao().getById(rootId))
            } finally {
                database.close()
            }
        }

    @Test
    fun `canonical ancestry presentation fails closed for deleted malformed and Context-backed Workspaces`() =
        runBlocking {
            val database = database()
            try {
                val deleted = canonicalOnly("deleted").copy(isDeleted = true)
                val malformed = canonicalOnly("malformed").copy(sourceContextId = "legacy-context")
                val contextBacked = contextBacked("legacy")
                val nonReservedSystemShaped = canonicalOnly("sys_custom", name = "Standalone canonical")
                database.workspaceDao().upsert(
                    listOf(deleted, malformed, contextBacked, nonReservedSystemShaped),
                )
                val legacyContext = legacyContext(id = "legacy", name = "Legacy Context")
                database.contextDao().insert(legacyContext)
                val repository = repository(database)

                assertNull(repository.getLiveCanonicalAncestryPresentation("missing"))
                assertNull(repository.getLiveCanonicalAncestryPresentation("deleted"))
                assertNull(repository.getLiveCanonicalAncestryPresentation("malformed"))
                assertNull(repository.getLiveCanonicalAncestryPresentation("legacy"))
                assertEquals(
                    "Standalone canonical",
                    repository.getLiveCanonicalAncestryPresentation("sys_custom")?.name,
                )
                assertEquals(deleted, database.workspaceDao().getById("deleted"))
                assertEquals(malformed, database.workspaceDao().getById("malformed"))
                assertEquals(contextBacked, database.workspaceDao().getById("legacy"))
                assertNull(database.contextDao().getContextById("deleted"))
                assertNull(database.contextDao().getContextById("malformed"))
                assertEquals(legacyContext, database.contextDao().getContextById("legacy"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `standalone Workspace lifecycle preserves hierarchy and rejects Context-backed mutation`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val parentId = repository.create("Operations", now = 10L)
            val childId = repository.create("Delivery", parentWorkspaceId = parentId, now = 11L)

            assertEquals(
                WorkspaceProvenance.STANDALONE.name,
                database.workspaceDao().getById(parentId)?.provenance,
            )
            assertNull(database.workspaceDao().getById(parentId)?.sourceContextId)
            assertNull(database.contextDao().getContextById(parentId))
            val parentCapabilities = database.orientationDao().getAllWorkspaceCapabilities()
            assertTrue(
                parentCapabilities.none { it.workspaceId == parentId },
            )

            repository.updateDetails(childId, "Delivery desk", "Operational", "project", now = 20L)
            assertEquals("Delivery desk", database.workspaceDao().getById(childId)?.nameOverride)
            assertEquals(2L, database.workspaceDao().getById(childId)?.version)

            val cycleFailure =
                runCatching { repository.move(parentId, childId, now = 30L) }.exceptionOrNull()
            assertTrue(cycleFailure is IllegalArgumentException)
            assertNull(database.workspaceDao().getById(parentId)?.parentWorkspaceId)

            repository.tombstone(parentId, now = 40L)
            assertTrue(database.workspaceDao().getById(parentId)?.isDeleted == true)
            assertFalse(database.workspaceDao().getById(childId)?.isDeleted == true)
            assertNull(database.workspaceDao().getById(childId)?.parentWorkspaceId)

            database.workspaceDao().upsert(
                listOf(contextBacked("legacy")),
            )
            val ownershipFailure =
                runCatching {
                    repository.updateDetails("legacy", "Changed", null, null, now = 50L)
                }.exceptionOrNull()
            assertTrue(ownershipFailure is IllegalArgumentException)
            assertEquals("Legacy", database.workspaceDao().getById("legacy")?.nameOverride)
        } finally {
            database.close()
        }
    }

    @Test
    fun `move preserving order keeps canonical current order and validates hierarchy`() = runBlocking {
        val database = database()
        try {
            val root = canonicalOnly("root", order = 11L)
            val child = canonicalOnly("child", parentId = root.id, order = 37L, version = 4L)
            val target = canonicalOnly("target", order = 12L)
            val targetSibling = canonicalOnly("target-sibling", parentId = target.id, order = 5L)
            val cycleParent = canonicalOnly("cycle-parent")
            val cycleChild = canonicalOnly("cycle-child", parentId = cycleParent.id)
            database.workspaceDao().upsert(
                listOf(root, child, target, targetSibling, cycleParent, cycleChild),
            )
            val repository = repository(database)

            repository.movePreservingOrder(child.id, target.id, now = 20L)

            val moved = requireNotNull(database.workspaceDao().getById(child.id))
            assertEquals(target.id, moved.parentWorkspaceId)
            assertEquals(37L, moved.workspaceOrder)
            assertEquals(5L, moved.version)
            assertEquals(20L, moved.updatedAt)
            assertNull(moved.syncedAt)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, moved.provenance)

            val cycleFailure =
                runCatching {
                    repository.movePreservingOrder(cycleParent.id, cycleChild.id, now = 30L)
                }.exceptionOrNull()
            assertTrue(cycleFailure is IllegalArgumentException)
            assertNull(database.workspaceDao().getById(cycleParent.id)?.parentWorkspaceId)
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical-only tombstone refuses to rewrite Context-backed child`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val parentId = repository.create("Canonical parent", now = 10L)
            database.workspaceDao().upsert(
                listOf(
                    contextBacked("legacy-child").copy(
                        parentWorkspaceId = parentId,
                        workspaceOrder = 0L,
                    ),
                ),
            )

            val failure =
                runCatching { repository.tombstone(parentId, now = 20L) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertFalse(database.workspaceDao().getById(parentId)?.isDeleted == true)
            assertEquals(
                parentId,
                database.workspaceDao().getById("legacy-child")?.parentWorkspaceId,
            )
            assertEquals(1L, database.workspaceDao().getById("legacy-child")?.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `lazy embodiment is idempotent and tombstone removes owned links and capabilities`() = runBlocking {
        val database = database()
        try {
            insertOrientation(database, "orientation")
            val repository = repository(database)

            val first = repository.ensureEmbodiedWorkspace("orientation", now = 100L)
            val second = repository.ensureEmbodiedWorkspace("orientation", now = 110L)

            assertEquals(first, second)
            val workspace = database.workspaceDao().getById(first)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace?.provenance)
            assertNull(workspace?.sourceContextId)
            assertNull(workspace?.nameOverride)
            assertNull(workspace?.descriptionOverride)

            var bindings =
                database.orientationDao().getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            it.subjectId == "orientation" &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
            assertEquals(1, bindings.size)
            assertEquals(first, bindings.single().workspaceId)

            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    WorkspaceCapabilityInstanceEntity(
                        id = "capability",
                        workspaceId = first,
                        capabilityType = WorkspaceCapabilityType.BACKLOG.name,
                        instanceKey = "default",
                        capabilityOrder = 0L,
                        state = WorkspaceCapabilityState.ACTIVE.name,
                        configurationVersion = 1,
                        configuration = "{}",
                        createdAt = 120L,
                        updatedAt = 120L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )

            repository.tombstone(first, now = 130L)

            assertTrue(database.workspaceDao().getById(first)?.isDeleted == true)
            assertTrue(
                database.orientationDao().getAllWorkspaceBindings()
                    .filter { it.workspaceId == first }
                    .all { it.isDeleted },
            )
            assertTrue(
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == first }
                    .all { it.isDeleted },
            )

            val replacement = repository.ensureEmbodiedWorkspace("orientation", now = 140L)
            assertNotEquals(first, replacement)
            bindings =
                database.orientationDao().getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            it.subjectId == "orientation" &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
            assertEquals(1, bindings.size)
            assertEquals(replacement, bindings.single().workspaceId)
        } finally {
            database.close()
        }
    }

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
                hierarchyRepository.createPrimaryAppearance(
                    target =
                        com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef(
                            com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType.WORKSPACE,
                            ownerId,
                        ),
                    now = 11L,
                )
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
    fun `presentation batch updates canonical metadata and hierarchy as one projection`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val parentId = repository.create("Parent", now = 10L)
            val childId =
                repository.create(
                    nameOverride = "Child",
                    parentWorkspaceId = parentId,
                    roleCode = "development",
                    now = 11L,
                )

            val before = requireNotNull(database.workspaceDao().getById(childId))

            repository.updatePresentationBatchInCurrentTransaction(
                updates =
                    listOf(
                        CanonicalWorkspacePresentationUpdate(
                            id = childId,
                            nameOverride = "  Delivery desk  ",
                            descriptionOverride = "  Operational description  ",
                            parentWorkspaceId = null,
                            roleCode = "  project  ",
                            workspaceOrder = 7L,
                        ),
                    ),
                now = 20L,
            )

            val after = requireNotNull(database.workspaceDao().getById(childId))
            assertEquals("Delivery desk", after.nameOverride)
            assertEquals("Operational description", after.descriptionOverride)
            assertNull(after.parentWorkspaceId)
            assertEquals("project", after.roleCode)
            assertEquals(7L, after.workspaceOrder)
            assertEquals(before.version + 1L, after.version)
            assertEquals(20L, after.updatedAt)
            assertNull(after.syncedAt)
            assertEquals(before.createdAt, after.createdAt)
            assertEquals(WorkspaceProvenance.STANDALONE.name, before.provenance)
            assertEquals(before.provenance, after.provenance)
            assertNull(after.sourceContextId)
        } finally {
            database.close()
        }
    }

    @Test
    fun `presentation batch applies coherent sibling reorder together`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val parentId = repository.create("Parent", now = 10L)
            val firstId = repository.create("First", parentWorkspaceId = parentId, now = 11L)
            val secondId = repository.create("Second", parentWorkspaceId = parentId, now = 12L)

            val firstBefore = requireNotNull(database.workspaceDao().getById(firstId))
            val secondBefore = requireNotNull(database.workspaceDao().getById(secondId))
            assertEquals(0L, firstBefore.workspaceOrder)
            assertEquals(1L, secondBefore.workspaceOrder)

            repository.updatePresentationBatchInCurrentTransaction(
                updates =
                    listOf(
                        CanonicalWorkspacePresentationUpdate(
                            id = firstBefore.id,
                            nameOverride = firstBefore.nameOverride,
                            descriptionOverride = firstBefore.descriptionOverride,
                            parentWorkspaceId = parentId,
                            roleCode = firstBefore.roleCode,
                            workspaceOrder = 1L,
                        ),
                        CanonicalWorkspacePresentationUpdate(
                            id = secondBefore.id,
                            nameOverride = secondBefore.nameOverride,
                            descriptionOverride = secondBefore.descriptionOverride,
                            parentWorkspaceId = parentId,
                            roleCode = secondBefore.roleCode,
                            workspaceOrder = 0L,
                        ),
                    ),
                now = 30L,
            )

            val firstAfter = requireNotNull(database.workspaceDao().getById(firstId))
            val secondAfter = requireNotNull(database.workspaceDao().getById(secondId))

            assertEquals(parentId, firstAfter.parentWorkspaceId)
            assertEquals(parentId, secondAfter.parentWorkspaceId)
            assertEquals(1L, firstAfter.workspaceOrder)
            assertEquals(0L, secondAfter.workspaceOrder)
            assertEquals(firstBefore.version + 1L, firstAfter.version)
            assertEquals(secondBefore.version + 1L, secondAfter.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `invalid presentation batch persists nothing`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val parentId = repository.create("Parent", now = 10L)
            val childId = repository.create("Child", parentWorkspaceId = parentId, now = 11L)

            val parentBefore = requireNotNull(database.workspaceDao().getById(parentId))
            val childBefore = requireNotNull(database.workspaceDao().getById(childId))

            val failure =
                runCatching {
                    repository.updatePresentationBatchInCurrentTransaction(
                        updates =
                            listOf(
                                CanonicalWorkspacePresentationUpdate(
                                    id = parentId,
                                    nameOverride = parentBefore.nameOverride,
                                    descriptionOverride = parentBefore.descriptionOverride,
                                    parentWorkspaceId = childId,
                                    roleCode = parentBefore.roleCode,
                                    workspaceOrder = parentBefore.workspaceOrder,
                                ),
                                CanonicalWorkspacePresentationUpdate(
                                    id = childId,
                                    nameOverride = "Changed but must roll back",
                                    descriptionOverride = childBefore.descriptionOverride,
                                    parentWorkspaceId = parentId,
                                    roleCode = childBefore.roleCode,
                                    workspaceOrder = childBefore.workspaceOrder,
                                ),
                            ),
                        now = 30L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)

            val parentAfter = requireNotNull(database.workspaceDao().getById(parentId))
            val childAfter = requireNotNull(database.workspaceDao().getById(childId))

            assertEquals(parentBefore, parentAfter)
            assertEquals(childBefore, childAfter)
        } finally {
            database.close()
        }
    }


    private fun repository(database: AppDatabase) =
        CanonicalWorkspaceRepository(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            graphRepository =
                CanonicalOrientationGraphRepository(
                    database,
                    database.orientationDao(),
                    database.workspaceDao(),
                ),
            executionLogRepository = executionLogRepository(database),
            keyProblemsRepository = keyProblemsRepository(database),
            directionRepository = directionRepository(database),
            inboxRepository = inboxRepository(database),
            connectionsRepository = connectionsRepository(database),
            backlogRepository = backlogRepository(database),
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

    private suspend fun insertOrientation(database: AppDatabase, id: String) {
        database.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = "Autonomous home",
                    description = "Semantic description",
                    createdAt = 10L,
                    updatedAt = 10L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        database.orientationDao().upsertOrientations(
            listOf(OrientationEntity(id, "GOAL", null, "UNSET")),
        )
    }

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
        parentWorkspaceId = parentId,
        roleCode = roleCode,
        workspaceOrder = order,
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
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    @Test
    fun `workspace clipboard bulk move keeps selected descendants inside selected roots`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val sourceRoot = repository.create("Source root", now = 10L)
                val sourceChild =
                    repository.create(
                        "Source child",
                        parentWorkspaceId = sourceRoot,
                        now = 11L,
                    )
                val target = repository.create("Target", now = 12L)

                val moved =
                    repository.moveMany(
                        ids = linkedSetOf(sourceRoot, sourceChild),
                        newParentWorkspaceId = target,
                        now = 20L,
                    )

                assertEquals(listOf(sourceRoot), moved)
                assertEquals(
                    target,
                    database.workspaceDao().getById(sourceRoot)?.parentWorkspaceId,
                )
                assertEquals(
                    sourceRoot,
                    database.workspaceDao().getById(sourceChild)?.parentWorkspaceId,
                )
                assertNull(database.contextDao().getContextById(sourceRoot))
                assertNull(database.contextDao().getContextById(sourceChild))
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace clipboard bulk move rolls back all sources when hierarchy validation fails`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val first = repository.create("First", now = 10L)
                val firstChild =
                    repository.create(
                        "First child",
                        parentWorkspaceId = first,
                        now = 11L,
                    )
                val second = repository.create("Second", now = 12L)

                val failure =
                    runCatching {
                        repository.moveMany(
                            ids = linkedSetOf(first, second),
                            newParentWorkspaceId = firstChild,
                            now = 20L,
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertNull(database.workspaceDao().getById(first)?.parentWorkspaceId)
                assertNull(database.workspaceDao().getById(second)?.parentWorkspaceId)
                assertEquals(
                    first,
                    database.workspaceDao().getById(firstChild)?.parentWorkspaceId,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace clipboard copy is shallow standalone and Context free`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val source =
                    repository.create(
                        nameOverride = "Source",
                        roleCode = "project",
                        now = 10L,
                    )
                val child =
                    repository.create(
                        nameOverride = "Child",
                        parentWorkspaceId = source,
                        now = 11L,
                    )
                val target = repository.create("Target", now = 12L)

                val copyId =
                    repository.copyManyShallow(
                        ids = linkedSetOf(source),
                        targetParentWorkspaceId = target,
                        now = 20L,
                    ).single()

                assertNotEquals(source, copyId)

                val copy = requireNotNull(database.workspaceDao().getById(copyId))
                assertEquals("Source (копія)", copy.nameOverride)
                assertNull(copy.descriptionOverride)
                assertEquals("project", copy.roleCode)
                assertEquals(target, copy.parentWorkspaceId)
                assertEquals(WorkspaceProvenance.STANDALONE.name, copy.provenance)
                assertNull(copy.sourceContextId)
                assertNull(database.contextDao().getContextById(copyId))

                assertEquals(
                    source,
                    database.workspaceDao().getById(child)?.parentWorkspaceId,
                )
                assertTrue(
                    database.workspaceDao().getAll()
                        .none { it.parentWorkspaceId == copyId },
                )
                assertTrue(
                    database.orientationDao()
                        .getAllWorkspaceCapabilities()
                        .none { it.workspaceId == copyId },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace clipboard copy name collision increments deterministically`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val source = repository.create("Source", now = 10L)
                val target = repository.create("Target", now = 11L)

                repository.create(
                    nameOverride = "Source (копія)",
                    parentWorkspaceId = target,
                    now = 12L,
                )

                val copyId =
                    repository.copyManyShallow(
                        ids = setOf(source),
                        targetParentWorkspaceId = target,
                        now = 20L,
                    ).single()

                assertEquals(
                    "Source (копія 2)",
                    database.workspaceDao().getById(copyId)?.nameOverride,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace clipboard moves System Workspace without changing its identity`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val systemId = SystemContexts.INBOX.raw
                val target = repository.create("Target", now = 10L)

                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnly(
                            id = systemId,
                            name = "Inbox",
                        ),
                    ),
                )

                val moved =
                    repository.moveMany(
                        ids = setOf(systemId),
                        newParentWorkspaceId = target,
                        now = 20L,
                    )

                assertEquals(listOf(systemId), moved)

                val system = requireNotNull(database.workspaceDao().getById(systemId))
                assertEquals(systemId, system.id)
                assertEquals(target, system.parentWorkspaceId)
                assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, system.provenance)
                assertNull(system.sourceContextId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace clipboard copies System Workspace as ordinary standalone Workspace`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val systemId = SystemContexts.INBOX.raw
                val target = repository.create("Target", now = 10L)

                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnly(
                            id = systemId,
                            name = "Inbox",
                        ),
                    ),
                )

                val copyId =
                    repository.copyManyShallow(
                        ids = setOf(systemId),
                        targetParentWorkspaceId = target,
                        now = 20L,
                    ).single()

                assertNotEquals(systemId, copyId)
                assertFalse(
                    SystemContexts.isSystem(
                        com.romankozak.forwardappmobile.core.context.ContextId(copyId),
                    ),
                )

                val copy = requireNotNull(database.workspaceDao().getById(copyId))
                assertEquals("Inbox (копія)", copy.nameOverride)
                assertEquals(target, copy.parentWorkspaceId)
                assertEquals(WorkspaceProvenance.STANDALONE.name, copy.provenance)
                assertNull(copy.sourceContextId)

                val original = requireNotNull(database.workspaceDao().getById(systemId))
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    original.provenance,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace clipboard can paste ordinary Workspace into System Workspace`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val ordinary = repository.create("Ordinary", now = 10L)
                val systemId = SystemContexts.INBOX.raw

                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnly(
                            id = systemId,
                            name = "Inbox",
                        ),
                    ),
                )

                repository.moveMany(
                    ids = setOf(ordinary),
                    newParentWorkspaceId = systemId,
                    now = 20L,
                )

                assertEquals(
                    systemId,
                    database.workspaceDao().getById(ordinary)?.parentWorkspaceId,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace subtree tombstone deletes ordinary descendants`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val root = repository.create("Root", now = 10L)
                val child =
                    repository.create(
                        nameOverride = "Child",
                        parentWorkspaceId = root,
                        now = 11L,
                    )
                val grandchild =
                    repository.create(
                        nameOverride = "Grandchild",
                        parentWorkspaceId = child,
                        now = 12L,
                    )

                val deleted =
                    repository.tombstoneSubtree(
                        rootId = root,
                        now = 20L,
                    )

                assertEquals(
                    linkedSetOf(root, child, grandchild),
                    deleted.toCollection(linkedSetOf()),
                )
                assertTrue(requireNotNull(database.workspaceDao().getById(root)).isDeleted)
                assertTrue(requireNotNull(database.workspaceDao().getById(child)).isDeleted)
                assertTrue(requireNotNull(database.workspaceDao().getById(grandchild)).isDeleted)
            } finally {
                database.close()
            }
        }

    @Test
    fun `workspace subtree tombstone fails closed when it contains System Workspace`() =
        runBlocking {
            val database = database()
            try {
                val repository = repository(database)
                val root = repository.create("Root", now = 10L)
                val systemId = SystemContexts.INBOX.raw
                val system =
                    canonicalOnly(
                        id = systemId,
                        name = "Inbox",
                    ).copy(
                        parentWorkspaceId = root,
                    )

                database.workspaceDao().upsert(listOf(system))

                val failure =
                    runCatching {
                        repository.tombstoneSubtree(
                            rootId = root,
                            now = 20L,
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)

                val rootAfter = requireNotNull(database.workspaceDao().getById(root))
                val systemAfter = requireNotNull(database.workspaceDao().getById(systemId))

                assertFalse(rootAfter.isDeleted)
                assertFalse(systemAfter.isDeleted)
                assertEquals(root, systemAfter.parentWorkspaceId)
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
