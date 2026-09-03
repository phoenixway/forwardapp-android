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
    fun `cross-workspace move tombstones source and creates a new destination placement`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "source", withCapability = true)
            seedWorkspace(database, "destination", withCapability = true)
            seedWorkspace(database, "target")
            val repository = repository(database)
            val id = repository.addEntry("source", workspaceTarget("target"), now = 10L)

            assertEquals(1, repository.moveEntries(listOf(id), "destination", now = 20L))
            assertTrue(repository.getEntries("source").isEmpty())
            val destination = repository.getEntries("destination").single()
            assertTrue(destination.id != id)
            assertEquals("destination", destination.workspaceId)
            assertEquals("backlog-destination", destination.capabilityInstanceId)
            assertEquals(WorkspaceBacklogTargetKind.WORKSPACE.name, destination.targetKind)
            assertEquals("target", destination.targetId)
            assertEquals(1L, destination.version)
            val source = requireNotNull(repository.getEntry(id))
            assertTrue(source.isDeleted)
            assertEquals("source", source.workspaceId)
            assertEquals("backlog-source", source.capabilityInstanceId)
            assertEquals(2L, source.version)

            repository.disable("destination", now = 30L)
            assertEquals(
                1,
                repository.tombstoneOwnedContentForWorkspaces(listOf("destination"), now = 40L),
            )
            assertTrue(requireNotNull(repository.getEntry(destination.id)).isDeleted)
            assertFalse(requireNotNull(database.workspaceDao().getById("target")).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `cross-workspace move resurrects destination tombstone and preserves input order`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "source", withCapability = true)
            seedWorkspace(database, "destination", withCapability = true)
            seedWorkspace(database, "target-a")
            seedWorkspace(database, "target-b")
            val repository = repository(database)
            val historicalId = repository.addEntry("destination", workspaceTarget("target-a"), now = 5L)
            repository.tombstoneEntry(historicalId, now = 6L)
            val sourceA = repository.addEntry("source", workspaceTarget("target-a"), now = 10L)
            val sourceB = repository.addEntry("source", workspaceTarget("target-b"), now = 11L)

            repository.moveEntries(listOf(sourceB, sourceA), "destination", now = 20L)
            val destinationEntries = repository.getEntries("destination")
            assertEquals(listOf("target-b", "target-a"), destinationEntries.map { it.targetId })
            assertEquals(historicalId, destinationEntries.last().id)
            assertEquals(3L, requireNotNull(repository.getEntry(historicalId)).version)
            assertTrue(requireNotNull(repository.getEntry(sourceA)).isDeleted)
            assertTrue(requireNotNull(repository.getEntry(sourceB)).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `live destination duplicate rejects the whole cross-workspace move atomically`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "source", withCapability = true)
            seedWorkspace(database, "destination", withCapability = true)
            seedWorkspace(database, "target-a")
            seedWorkspace(database, "target-b")
            val repository = repository(database)
            val sourceA = repository.addEntry("source", workspaceTarget("target-a"), now = 10L)
            val sourceB = repository.addEntry("source", workspaceTarget("target-b"), now = 11L)
            repository.addEntry("destination", workspaceTarget("target-a"), now = 12L)

            assertTrue(runCatching { repository.moveEntries(listOf(sourceB, sourceA), "destination", now = 20L) }.isFailure)
            assertEquals(2, repository.getEntries("source").size)
            assertTrue(repository.getEntry(sourceA)?.isDeleted == false)
            assertTrue(repository.getEntry(sourceB)?.isDeleted == false)
        } finally {
            database.close()
        }
    }

    @Test
    fun `cross-workspace move compacts each source and fails before writes for invalid target`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "source-a", withCapability = true)
            seedWorkspace(database, "source-b", withCapability = true)
            seedWorkspace(database, "destination", withCapability = true)
            seedWorkspace(database, "target-a")
            seedWorkspace(database, "target-b")
            seedWorkspace(database, "target-c")
            val repository = repository(database)
            val retained = repository.addEntry("source-a", workspaceTarget("target-a"), now = 1L)
            val moved = repository.addEntry("source-a", workspaceTarget("target-b"), now = 2L)
            val invalid = repository.addEntry("source-b", workspaceTarget("target-c"), now = 3L)

            assertTrue(runCatching { repository.moveEntries(listOf(moved, invalid), "destination", now = 10L) }.isSuccess)
            assertEquals(0L, requireNotNull(repository.getEntry(retained)).entryOrder)
            assertTrue(requireNotNull(repository.getEntry(moved)).isDeleted)
            assertTrue(requireNotNull(repository.getEntry(invalid)).isDeleted)

            val secondInvalid = repository.addEntry("source-a", workspaceTarget("target-b"), now = 11L)
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = "target-b",
                        nameOverride = "target-b",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = null,
                        workspaceOrder = 0L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = true,
                        version = 2L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )
            assertTrue(runCatching { repository.moveEntries(listOf(secondInvalid), "destination", now = 12L) }.isFailure)
            assertFalse(requireNotNull(repository.getEntry(secondInvalid)).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `same-owner move preserves placement identities and existing append semantics`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner", withCapability = true)
            seedWorkspace(database, "target-a")
            seedWorkspace(database, "target-b")
            val repository = repository(database)
            val first = repository.addEntry("owner", workspaceTarget("target-a"), now = 10L)
            val second = repository.addEntry("owner", workspaceTarget("target-b"), now = 11L)

            repository.moveEntries(listOf(first), "owner", now = 20L)
            assertEquals(listOf(second, first), repository.getEntries("owner").map { it.id })
            assertEquals(first, repository.getEntry(first)?.id)
            assertFalse(requireNotNull(repository.getEntry(first)).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `cross-workspace move requires active source and destination capabilities`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "source", withCapability = true)
            seedWorkspace(database, "destination", withCapability = true)
            seedWorkspace(database, "target")
            val repository = repository(database)
            val id = repository.addEntry("source", workspaceTarget("target"), now = 1L)
            repository.disable("source", now = 2L)
            assertTrue(runCatching { repository.moveEntries(listOf(id), "destination", now = 3L) }.isFailure)
            assertFalse(requireNotNull(repository.getEntry(id)).isDeleted)
            repository.enable("source", now = 4L)
            repository.disable("destination", now = 5L)
            assertTrue(runCatching { repository.moveEntries(listOf(id), "destination", now = 6L) }.isFailure)
            assertFalse(requireNotNull(repository.getEntry(id)).isDeleted)
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
