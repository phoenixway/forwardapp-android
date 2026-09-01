package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
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
    fun `canonical-only lifecycle preserves hierarchy and rejects Context-backed mutation`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            val parentId = repository.create("Operations", now = 10L)
            val childId = repository.create("Delivery", parentWorkspaceId = parentId, now = 11L)

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
}
