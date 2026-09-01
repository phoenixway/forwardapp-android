package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalBacklogRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `add reorder tombstone and resurrect preserve one logical placement`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner", withCapability = true)
            seedWorkspace(database, "target-a")
            seedWorkspace(database, "target-b")
            val repository = repository(database)

            val first = repository.addEntry("owner", workspaceTarget("target-a"), now = 10L)
            val second = repository.addEntry("owner", workspaceTarget("target-b"), now = 11L)
            assertEquals(listOf(first, second), repository.getEntries("owner").map { it.id })

            repository.reorder("owner", listOf(second, first), now = 20L)
            assertEquals(listOf(second, first), repository.getEntries("owner").map { it.id })

            repository.tombstoneEntry(second, now = 30L)
            assertEquals(listOf(first), repository.getEntries("owner").map { it.id })
            assertEquals(0L, repository.getEntry(first)?.entryOrder)

            val resurrected = repository.addEntry("owner", workspaceTarget("target-b"), now = 40L)
            assertEquals(second, resurrected)
            assertEquals(listOf(first, second), repository.getEntries("owner").map { it.id })
            assertFalse(requireNotNull(repository.getEntry(second)).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical-only lifecycle preserves placements and rejects invalid targets`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner", withCapability = true)
            seedWorkspace(database, "target")
            val repository = repository(database)
            val id = repository.addEntry("owner", workspaceTarget("target"), now = 10L)

            repository.disable("owner", now = 20L)
            assertFalse(requireNotNull(repository.getEntry(id)).isDeleted)
            assertTrue(runCatching { repository.addEntry("owner", workspaceTarget("target"), now = 21L) }.isFailure)

            repository.archive("owner", now = 22L)
            repository.restore("owner", now = 23L)
            repository.enable("owner", now = 24L)
            repository.deleteCapability("owner", now = 25L)
            assertFalse(requireNotNull(repository.getEntry(id)).isDeleted)

            seedWorkspace(database, "deleted-target", deleted = true)
            assertTrue(
                runCatching {
                    repository.enable("owner", now = 26L)
                    repository.addEntry("owner", workspaceTarget("deleted-target"), now = 27L)
                }.isFailure,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `move preserves placement id and owner tombstone bypasses disabled capability`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "source", withCapability = true)
            seedWorkspace(database, "destination", withCapability = true)
            seedWorkspace(database, "target")
            val repository = repository(database)
            val id = repository.addEntry("source", workspaceTarget("target"), now = 10L)

            assertEquals(1, repository.moveEntries(listOf(id), "destination", now = 20L))
            assertTrue(repository.getEntries("source").isEmpty())
            assertEquals(id, repository.getEntries("destination").single().id)

            repository.disable("destination", now = 30L)
            assertEquals(
                1,
                repository.tombstoneOwnedContentForWorkspaces(listOf("destination"), now = 40L),
            )
            assertTrue(requireNotNull(repository.getEntry(id)).isDeleted)
            assertFalse(requireNotNull(database.workspaceDao().getById("target")).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context-backed Workspace can activate post-cutover canonical Backlog`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "legacy", provenance = WorkspaceProvenance.CONTEXT_BACKED)
            val repository = repository(database)

            val capabilityId = repository.enable("legacy", now = 10L)

            val capability =
                database.orientationDao()
                    .getAllWorkspaceCapabilities()
                    .single { it.id == capabilityId }

            assertEquals("legacy", capability.workspaceId)
            assertEquals("BACKLOG", capability.capabilityType)
            assertEquals("ACTIVE", capability.state)
            assertFalse(capability.isDeleted)
            assertTrue(database.workspaceBacklogEntryDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical cleanup tombstones dangling and hierarchy projection entries only`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner", withCapability = true)
            seedWorkspace(database, "child", parentWorkspaceId = "owner")
            seedWorkspace(database, "reference")
            val repository = repository(database)

            val childPlacement = repository.addEntry("owner", workspaceTarget("child"), now = 10L)
            val referencePlacement = repository.addEntry("owner", workspaceTarget("reference"), now = 11L)
            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    WorkspaceBacklogEntryEntity(
                        id = "dangling",
                        workspaceId = "owner",
                        capabilityInstanceId = "backlog-owner",
                        targetKind = WorkspaceBacklogTargetKind.WORKSPACE.name,
                        targetId = "missing",
                        entryOrder = 2L,
                        createdAt = 12L,
                        updatedAt = 12L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )

            assertEquals(2, repository.tombstoneDanglingAndStructuralEntries(now = 20L))
            assertTrue(requireNotNull(repository.getEntry(childPlacement)).isDeleted)
            assertTrue(requireNotNull(repository.getEntry("dangling")).isDeleted)
            assertFalse(requireNotNull(repository.getEntry(referencePlacement)).isDeleted)
            assertEquals(0L, requireNotNull(repository.getEntry(referencePlacement)).entryOrder)
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalBacklogRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            entryDao = database.workspaceBacklogEntryDao(),
            targetValidator = CanonicalBacklogTargetValidator(database),
        )

    private suspend fun seedWorkspace(
        database: AppDatabase,
        id: String,
        withCapability: Boolean = false,
        deleted: Boolean = false,
        provenance: WorkspaceProvenance = WorkspaceProvenance.CANONICAL_ONLY,
        parentWorkspaceId: String? = null,
    ) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    parentWorkspaceId = parentWorkspaceId,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = deleted,
                    version = 1L,
                    provenance = provenance.name,
                    sourceContextId = if (provenance == WorkspaceProvenance.CONTEXT_BACKED) id else null,
                ),
            ),
        )
        if (withCapability) {
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    WorkspaceCapabilityInstanceEntity(
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
                    ),
                ),
            )
        }
    }

    private fun workspaceTarget(id: String) =
        WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.WORKSPACE, id)

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
