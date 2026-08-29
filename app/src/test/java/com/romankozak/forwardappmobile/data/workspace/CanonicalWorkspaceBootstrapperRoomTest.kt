package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceBootstrapperRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `projects Context hierarchy and effective capabilities without changing Context`() = runBlocking {
        val database = database()
        try {
            val parent = contextEntity("parent", "Operations", null, "management")
            val child = contextEntity("child", "Engineering", parent.id, "development")
            database.contextDao().insertContexts(listOf(parent, child))
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(child.id).copy(
                    basePresetCode = child.roleCode,
                    experimentalCapabilityIds = listOf(CapabilityId("future_capability")),
                    updatedAt = 10L,
                ),
            )
            val bootstrapper = bootstrapper(database)

            val first = bootstrapper.ensureBootstrapped(now = 100L)
            val second = bootstrapper.ensureBootstrapped(now = 110L)

            assertTrue(first.performed)
            assertFalse(second.performed)
            assertEquals(parent.name, database.contextDao().getContextById(parent.id)?.name)
            assertEquals(parent.id, database.workspaceDao().getById(child.id)?.parentWorkspaceId)
            assertEquals(child.name, database.workspaceDao().getById(child.id)?.nameOverride)
            val capabilities =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filterNot { it.isDeleted }
                    .groupBy { it.workspaceId }
                    .mapValues { (_, values) -> values.map { it.capabilityType }.toSet() }
            assertEquals(
                setOf(WorkspaceCapabilityType.BACKLOG.name, WorkspaceCapabilityType.INBOX.name),
                capabilities.getValue(parent.id),
            )
            assertEquals(
                setOf(WorkspaceCapabilityType.BACKLOG.name, WorkspaceCapabilityType.EXECUTION_LOG.name),
                capabilities.getValue(child.id),
            )
            assertTrue(first.issues.any { it.contextId == child.id && it.code == "UNKNOWN_CAPABILITY" })
        } finally {
            database.close()
        }
    }

    @Test
    fun `projects effective Direction auto-link setting into typed capability config`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("direction-config", "Direction", null, "direction")
            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(source.id).copy(
                    basePresetCode = source.roleCode,
                    enableAutoLinkSubprojects = false,
                    updatedAt = 10L,
                ),
            )
            val bootstrapper = bootstrapper(database)

            bootstrapper.ensureBootstrapped(now = 100L)

            val capability =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.DIRECTION.name &&
                        !it.isDeleted
                }
            assertFalse(
                DirectionCapabilityConfigurationCodec.decode(
                    capability.configurationVersion,
                    capability.configuration,
                ).autoLinkChildWorkspaces,
            )

            val structure = requireNotNull(database.contextStructureDao().getStructureByContext(source.id))
            database.contextStructureDao().updateStructure(
                structure.copy(enableAutoLinkSubprojects = true, updatedAt = 20L),
            )
            bootstrapper.ensureBootstrapped(now = 200L)

            val updated =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.DIRECTION.name &&
                        !it.isDeleted
                }
            assertTrue(
                DirectionCapabilityConfigurationCodec.decode(
                    updated.configurationVersion,
                    updated.configuration,
                ).autoLinkChildWorkspaces,
            )
            assertEquals(capability.version + 1L, updated.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `repairs only shadow hierarchy and tombstones migrated capabilities with deleted Context`() = runBlocking {
        val database = database()
        try {
            val first = contextEntity("first", "First", "second", "management")
            val second = contextEntity("second", "Second", "first", "development")
            database.contextDao().insertContexts(listOf(first, second))
            val bootstrapper = bootstrapper(database)

            val initial = bootstrapper.ensureBootstrapped(now = 100L)

            assertEquals(2, initial.issues.count { it.code == "HIERARCHY_CYCLE" })
            assertNull(database.workspaceDao().getById(first.id)?.parentWorkspaceId)
            assertNull(database.workspaceDao().getById(second.id)?.parentWorkspaceId)
            assertEquals("second", database.contextDao().getContextById(first.id)?.parentId)

            database.contextDao().update(first.copy(name = "First updated", updatedAt = 200L, version = 2L))
            bootstrapper.ensureBootstrapped(now = 200L)
            val updatedWorkspace = database.workspaceDao().getById(first.id)
            assertEquals("First updated", updatedWorkspace?.nameOverride)
            assertEquals(2L, updatedWorkspace?.version)

            database.contextDao().update(first.copy(isDeleted = true, updatedAt = 300L, version = 3L))
            bootstrapper.ensureBootstrapped(now = 300L)
            assertTrue(database.workspaceDao().getById(first.id)?.isDeleted == true)
            assertTrue(
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == first.id }
                    .all { it.isDeleted },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `write through projects nested Context and configuration mutations atomically`() = runBlocking {
        val database = database()
        try {
            val bootstrapper = bootstrapper(database)
            val writeThrough = ContextWorkspaceWriteThrough(bootstrapper)
            val context = contextEntity("immediate", "Immediate", null, "management")

            writeThrough.mutate(now = 100L) {
                database.contextDao().insert(context)
                writeThrough.mutate(now = 101L) {
                    database.contextStructureDao().insertStructure(
                        ContextConfiguration.default(context.id).copy(
                            basePresetCode = "management",
                            enableInbox = false,
                            enableBacklog = true,
                            updatedAt = 100L,
                        ),
                    )
                }
            }

            assertEquals("Immediate", database.workspaceDao().getById(context.id)?.nameOverride)
            val capabilities =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == context.id && !it.isDeleted }
                    .map { it.capabilityType }
                    .toSet()
            assertEquals(setOf(WorkspaceCapabilityType.BACKLOG.name), capabilities)

            runCatching {
                writeThrough.mutate(now = 200L) {
                    database.contextDao().update(context.copy(name = "Must roll back"))
                    error("rollback")
                }
            }
            assertEquals("Immediate", database.contextDao().getContextById(context.id)?.name)
            assertEquals("Immediate", database.workspaceDao().getById(context.id)?.nameOverride)
        } finally {
            database.close()
        }
    }

    @Test
    fun `physical Context deletion tombstones Context-backed Workspace and projected capabilities`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("physical-delete", "Legacy", null, "management")
            database.contextDao().insert(source)
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 100L)

            database.contextDao().delete(source.id)
            bootstrapper.ensureBootstrapped(now = 200L)

            val workspace = database.workspaceDao().getById(source.id)
            assertTrue(workspace?.isDeleted == true)
            assertEquals(WorkspaceProvenance.CONTEXT_BACKED.name, workspace?.provenance)
            assertEquals(source.id, workspace?.sourceContextId)
            assertTrue(
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == source.id }
                    .all { it.isDeleted },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical-only Workspace survives bootstrap without Context`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = "canonical-only",
                        nameOverride = "Canonical",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = null,
                        workspaceOrder = 0L,
                        createdAt = 10L,
                        updatedAt = 10L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )

            bootstrapper(database).ensureBootstrapped(now = 200L)

            val workspace = database.workspaceDao().getById("canonical-only")
            assertTrue(workspace?.isDeleted == false)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace?.provenance)
            assertNull(workspace?.sourceContextId)
            assertEquals(1L, workspace?.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context collision with canonical-only Workspace is quarantined and diagnosed`() = runBlocking {
        val database = database()
        try {
            val id = "collision"
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = id,
                        nameOverride = "Canonical owner",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = null,
                        workspaceOrder = 0L,
                        createdAt = 10L,
                        updatedAt = 10L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 7L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )
            database.contextDao().insert(contextEntity(id, "Legacy collision", null, "management"))

            val report = bootstrapper(database).ensureBootstrapped(now = 200L)

            val workspace = database.workspaceDao().getById(id)
            assertEquals("Canonical owner", workspace?.nameOverride)
            assertEquals(7L, workspace?.version)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace?.provenance)
            assertNull(workspace?.sourceContextId)

            assertTrue(
                database.orientationDao().getAllWorkspaceCapabilities()
                    .none { it.workspaceId == id },
            )
            assertTrue(
                report.issues.any {
                    it.contextId == id && it.code == "WORKSPACE_ID_COLLISION"
                },
            )
            assertTrue(
                database.workspaceDao().getOpenBootstrapIssues().any {
                    it.contextId == id && it.code == "WORKSPACE_ID_COLLISION"
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context child does not attach to canonical-only Workspace through colliding parent id`() = runBlocking {
        val database = database()
        try {
            val parentId = "collision-parent"
            val childId = "legacy-child"
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = parentId,
                        nameOverride = "Canonical parent",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = null,
                        workspaceOrder = 0L,
                        createdAt = 10L,
                        updatedAt = 10L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )
            database.contextDao().insertContexts(
                listOf(
                    contextEntity(parentId, "Legacy parent", null, "management"),
                    contextEntity(childId, "Legacy child", parentId, "development"),
                ),
            )

            val report = bootstrapper(database).ensureBootstrapped(now = 200L)

            assertNull(database.workspaceDao().getById(childId)?.parentWorkspaceId)
            assertTrue(
                report.issues.any {
                    it.contextId == childId && it.code == "WORKSPACE_PARENT_COLLISION"
                },
            )
            assertEquals(
                "Canonical parent",
                database.workspaceDao().getById(parentId)?.nameOverride,
            )
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun bootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun contextEntity(id: String, name: String, parentId: String?, roleCode: String) =
        ContextEntity(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            createdAt = 1L,
            updatedAt = 2L,
            roleCode = roleCode,
        )
}
