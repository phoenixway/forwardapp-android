package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
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
            contextManagementDao = database.contextManagementDao(),
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
