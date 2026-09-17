package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.context.SystemContexts

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.KeyProblemsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.KeyProblemsCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
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
    fun `legacy import may seed promoted System Backlog once then preserves canonical configuration`() = runBlocking {
        val database = database()
        try {
            val seededId = SystemContexts.INBOX.raw
            val upgradedId = SystemContexts.STRATEGIC.raw
            val falseUpgradeId = SystemContexts.WEEK.raw
            val deletedV2Id = SystemContexts.TODAY.raw
            database.contextDao().insertContexts(
                listOf(
                    contextEntity(seededId, "Inbox", null, "default"),
                    contextEntity(upgradedId, "Strategic", null, "default"),
                    contextEntity(falseUpgradeId, "Week", null, "default"),
                    contextEntity(deletedV2Id, "Today", null, "default"),
                ),
            )
            database.workspaceDao().upsert(
                listOf(
                    promotedSystemWorkspace(seededId),
                    promotedSystemWorkspace(upgradedId),
                    promotedSystemWorkspace(falseUpgradeId),
                    promotedSystemWorkspace(deletedV2Id),
                ),
            )
            database.contextStructureDao().insertAll(
                listOf(
                    ContextConfiguration.default(seededId).copy(removeBacklogEntryAfterTagAutocopy = true),
                    ContextConfiguration.default(upgradedId).copy(removeBacklogEntryAfterTagAutocopy = true),
                    ContextConfiguration.default(falseUpgradeId).copy(removeBacklogEntryAfterTagAutocopy = false),
                    ContextConfiguration.default(deletedV2Id).copy(removeBacklogEntryAfterTagAutocopy = false),
                ),
            )
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    systemCapability(
                        id = "backlog-v1",
                        workspaceId = upgradedId,
                        type = WorkspaceCapabilityType.BACKLOG,
                        state = WorkspaceCapabilityState.ARCHIVED,
                    ),
                    systemCapability(
                        id = "backlog-v1-false",
                        workspaceId = falseUpgradeId,
                        type = WorkspaceCapabilityType.BACKLOG,
                        state = WorkspaceCapabilityState.DISABLED,
                    ),
                    systemCapability(
                        id = "backlog-v2-deleted",
                        workspaceId = deletedV2Id,
                        type = WorkspaceCapabilityType.BACKLOG,
                        state = WorkspaceCapabilityState.ACTIVE,
                    ).copy(
                        configurationVersion = 2,
                        configuration = BacklogCapabilityConfigurationCodec.encode(BacklogCapabilityConfigurationV2(true)),
                        isDeleted = true,
                    ),
                ),
            )

            bootstrapper(database).ingestLegacySystemCapabilityProjection(
                importedContextIds =
                    setOf(seededId, upgradedId, falseUpgradeId, deletedV2Id),
                now = 100L,
            )

            val rows = database.orientationDao().getAllWorkspaceCapabilities()
                .filter { it.capabilityType == WorkspaceCapabilityType.BACKLOG.name }
                .associateBy { it.workspaceId }
            listOf(seededId, upgradedId).forEach { id ->
                val row = rows.getValue(id)
                assertEquals(2, row.configurationVersion)
                assertEquals(
                    BacklogCapabilityConfigurationV2(true),
                    BacklogCapabilityConfigurationCodec.decode(row.configurationVersion, row.configuration),
                )
            }
            val falseUpgrade = rows.getValue(falseUpgradeId)
            assertEquals(
                BacklogCapabilityConfigurationV2(false),
                BacklogCapabilityConfigurationCodec.decode(
                    falseUpgrade.configurationVersion,
                    falseUpgrade.configuration,
                ),
            )
            assertEquals(WorkspaceCapabilityState.DISABLED.name, falseUpgrade.state)
            val deletedBefore = rows.getValue(deletedV2Id)
            assertTrue(deletedBefore.isDeleted)
            assertEquals(
                BacklogCapabilityConfigurationV2(true),
                BacklogCapabilityConfigurationCodec.decode(
                    deletedBefore.configurationVersion,
                    deletedBefore.configuration,
                ),
            )
            assertEquals(WorkspaceCapabilityState.ARCHIVED.name, rows.getValue(upgradedId).state)
            assertEquals("backlog-v1", rows.getValue(upgradedId).id)

            val canonicalBefore = rows.getValue(upgradedId)
            val deletedCanonicalBefore = rows.getValue(deletedV2Id)
            database.contextStructureDao().insertStructure(
                requireNotNull(database.contextStructureDao().getStructureByContext(upgradedId)).copy(
                    removeBacklogEntryAfterTagAutocopy = false,
                    updatedAt = 101L,
                    version = 2L,
                ),
            )
            bootstrapper(database).ingestLegacySystemCapabilityProjection(
                importedContextIds =
                    setOf(seededId, upgradedId, falseUpgradeId, deletedV2Id),
                now = 102L,
            )
            assertEquals(
                canonicalBefore,
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == upgradedId && it.capabilityType == WorkspaceCapabilityType.BACKLOG.name
                },
            )
            assertEquals(
                deletedCanonicalBefore,
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == deletedV2Id && it.capabilityType == WorkspaceCapabilityType.BACKLOG.name
                },
            )
        } finally {
            database.close()
        }
    }

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
                    .filter {
                        !it.isDeleted &&
                            it.state == WorkspaceCapabilityState.ACTIVE.name
                    }
                    .groupBy { it.workspaceId }
                    .mapValues { (_, values) -> values.map { it.capabilityType }.toSet() }
            assertEquals(
                setOf(
                    WorkspaceCapabilityType.BACKLOG.name,
                    WorkspaceCapabilityType.INBOX.name,
                ),
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
            assertEquals(
                setOf(
                    WorkspaceCapabilityType.BACKLOG.name,
                    WorkspaceCapabilityType.DASHBOARD.name,
                    WorkspaceCapabilityType.EXECUTION_LOG.name,
                ),
                capabilities,
            )

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
    fun `Dashboard disabled seed blocks later legacy resurrection`() = runBlocking {
        val database = database()
        try {
            val source =
                contextEntity(
                    "dashboard-disabled-owner",
                    "Dashboard disabled",
                    null,
                    "default",
                )
            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(source.id).copy(
                    enableDashboard = false,
                    updatedAt = 90L,
                ),
            )
            val bootstrapper = bootstrapper(database)

            bootstrapper.ensureBootstrapped(now = 100L)

            val initial =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name
                }

            assertEquals(WorkspaceCapabilityState.DISABLED.name, initial.state)
            assertEquals(1L, initial.version)
            assertFalse(initial.isDeleted)

            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(source.id).copy(
                    enableDashboard = true,
                    updatedAt = 130L,
                ),
            )

            bootstrapper.ensureBootstrapped(now = 200L)

            val preserved =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name
                }

            assertEquals(WorkspaceCapabilityState.DISABLED.name, preserved.state)
            assertEquals(1L, preserved.version)
            assertFalse(preserved.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Dashboard canonical instance is not overwritten by legacy Context config after cutover`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("dashboard-owner", "Dashboard owner", null, "default")
            database.contextDao().insert(source)
            val bootstrapper = bootstrapper(database)

            bootstrapper.ensureBootstrapped(now = 100L)
            val initial =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name
                }
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    initial.copy(
                        state = "DISABLED",
                        updatedAt = 120L,
                        syncedAt = null,
                        version = initial.version + 1L,
                    ),
                ),
            )
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(source.id).copy(
                    enableDashboard = true,
                    updatedAt = 130L,
                ),
            )

            bootstrapper.ensureBootstrapped(now = 200L)

            val preserved =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name
                }
            assertEquals("DISABLED", preserved.state)
            assertFalse(preserved.isDeleted)
            assertEquals(initial.version + 1L, preserved.version)
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

    @Test
    fun `EXECUTION_LOG disabled canonical state survives later legacy enableLog change`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("execution-log-owner", "Execution log owner", null, "development")
            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(source.id).copy(
                    applyMode = "OVERRIDE",
                    enableLog = false,
                    updatedAt = 90L,
                ),
            )

            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 100L)

            val initial =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.EXECUTION_LOG.name
                }

            assertEquals("DISABLED", initial.state)
            assertFalse(initial.isDeleted)
            assertEquals(1L, initial.version)

            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(source.id).copy(
                    applyMode = "OVERRIDE",
                    enableLog = true,
                    updatedAt = 130L,
                ),
            )

            bootstrapper.ensureBootstrapped(now = 200L)

            val preserved =
                database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.workspaceId == source.id &&
                        it.capabilityType == WorkspaceCapabilityType.EXECUTION_LOG.name
                }

            assertEquals("DISABLED", preserved.state)
            assertFalse(preserved.isDeleted)
            assertEquals(1L, preserved.version)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun capability(
        workspaceId: String,
        type: WorkspaceCapabilityType,
        state: WorkspaceCapabilityState,
        configurationVersion: Int,
        configuration: String,
        isDeleted: Boolean = false,
    ) =
        com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity(
            id = "${workspaceId}_${type.name.lowercase()}",
            workspaceId = workspaceId,
            capabilityType = type.name,
            instanceKey = "default",
            capabilityOrder = type.ordinal.toLong(),
            state = state.name,
            configurationVersion = configurationVersion,
            configuration = configuration,
            createdAt = 1L,
            updatedAt = 10L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 7L,
        )

    @Test
    fun `legacy import seeds missing promoted System Direction then preserves canonical state`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val source = contextEntity(id, "Legacy Inbox", null, "direction")
                database.contextDao().insert(source)
                database.contextStructureDao().insertStructure(
                    ContextConfiguration.default(id).copy(
                        basePresetCode = "direction",
                        enableInbox = true,
                        enableAttachments = true,
                        experimentalCapabilityIds =
                            listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                        enableAutoLinkSubprojects = false,
                        updatedAt = 10L,
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        WorkspaceEntity(
                            id = id,
                            nameOverride = "Canonical Inbox",
                            descriptionOverride = "Canonical metadata",
                            parentWorkspaceId = null,
                            roleCode = "canonical-role",
                            workspaceOrder = 42L,
                            createdAt = 5L,
                            updatedAt = 5L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 7L,
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val bootstrapper = bootstrapper(database)
                val first = bootstrapper.ingestLegacySystemCapabilityProjection(
                    importedContextIds = setOf(id),
                    now = 100L,
                )

                val firstWorkspace = requireNotNull(database.workspaceDao().getById(id))
                assertEquals("Canonical Inbox", firstWorkspace.nameOverride)
                assertEquals("Canonical metadata", firstWorkspace.descriptionOverride)
                assertEquals("canonical-role", firstWorkspace.roleCode)
                assertEquals(42L, firstWorkspace.workspaceOrder)
                assertEquals(7L, firstWorkspace.version)
                assertFalse(
                    first.issues.any {
                        it.contextId == id && it.code == "WORKSPACE_ID_COLLISION"
                    },
                )

                val firstDirection =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.workspaceId == id &&
                            it.capabilityType == WorkspaceCapabilityType.DIRECTION.name &&
                            !it.isDeleted
                    }
                val firstInbox =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.workspaceId == id &&
                            it.capabilityType == WorkspaceCapabilityType.INBOX.name &&
                            !it.isDeleted
                    }
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, firstInbox.state)
                val firstTypes =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == id && !it.isDeleted }
                        .map { WorkspaceCapabilityType.valueOf(it.capabilityType) }
                        .toSet()
                assertTrue(WorkspaceCapabilityType.CONNECTIONS in firstTypes)
                assertTrue(WorkspaceCapabilityType.INBOX_SORTING in firstTypes)
                assertTrue(WorkspaceCapabilityType.KEY_PROBLEMS in firstTypes)
                assertFalse(
                    DirectionCapabilityConfigurationCodec.decode(
                        firstDirection.configurationVersion,
                        firstDirection.configuration,
                    ).autoLinkChildWorkspaces,
                )

                database.contextDao().update(
                    source.copy(
                        name = "Legacy rename must not win",
                        description = "Legacy description must not win",
                        order = 99L,
                        updatedAt = 110L,
                        version = 2L,
                    ),
                )
                val structure =
                    requireNotNull(
                        database.contextStructureDao().getStructureByContext(id),
                    )
                database.contextStructureDao().updateStructure(
                    structure.copy(
                        enableAutoLinkSubprojects = true,
                        updatedAt = 110L,
                        version = structure.version + 1L,
                    ),
                )

                val second = bootstrapper.ingestLegacySystemCapabilityProjection(
                    importedContextIds = setOf(id),
                    now = 200L,
                )

                val secondWorkspace = requireNotNull(database.workspaceDao().getById(id))
                assertEquals("Canonical Inbox", secondWorkspace.nameOverride)
                assertEquals("Canonical metadata", secondWorkspace.descriptionOverride)
                assertEquals("canonical-role", secondWorkspace.roleCode)
                assertEquals(42L, secondWorkspace.workspaceOrder)
                assertEquals(7L, secondWorkspace.version)
                assertFalse(
                    second.issues.any {
                        it.contextId == id && it.code == "WORKSPACE_ID_COLLISION"
                    },
                )

                val secondDirection =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.workspaceId == id &&
                            it.capabilityType == WorkspaceCapabilityType.DIRECTION.name &&
                            !it.isDeleted
                    }
                assertFalse(
                    DirectionCapabilityConfigurationCodec.decode(
                        secondDirection.configurationVersion,
                        secondDirection.configuration,
                    ).autoLinkChildWorkspaces,
                )
                assertEquals(firstDirection.version, secondDirection.version)
            } finally {
                database.close()
            }
        }

    @Test
    fun `promoted System Inbox archive and typed configuration survive contradictory legacy projection`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                database.contextDao().insert(contextEntity(id, "Inbox", null, "management"))
                database.contextStructureDao().insertStructure(
                    ContextConfiguration.default(id).copy(enableInbox = true, removeInboxEntryAfterTagAutocopy = false),
                )
                database.workspaceDao().upsert(
                    listOf(
                        WorkspaceEntity(
                            id = id,
                            nameOverride = "Inbox",
                            descriptionOverride = null,
                            parentWorkspaceId = null,
                            roleCode = null,
                            workspaceOrder = 0L,
                            createdAt = 1L,
                            updatedAt = 2L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 2L,
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )
                val canonical =
                    com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity(
                        id = "system-inbox",
                        workspaceId = id,
                        capabilityType = WorkspaceCapabilityType.INBOX.name,
                        instanceKey = "default",
                        capabilityOrder = 0L,
                        state = WorkspaceCapabilityState.ARCHIVED.name,
                        configurationVersion = InboxCapabilityConfigurationCodec.CURRENT_VERSION,
                        configuration = InboxCapabilityConfigurationCodec.encode(
                            InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED),
                        ),
                        createdAt = 1L,
                        updatedAt = 10L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 7L,
                    )
                database.orientationDao().upsertWorkspaceCapabilities(listOf(canonical))

                bootstrapper(database).ensureBootstrapped(now = 100L)

                val after = database.orientationDao().getAllWorkspaceCapabilities().single {
                    it.id == canonical.id
                }
                assertEquals(canonical, after)
            } finally {
                database.close()
            }
        }

    @Test
    fun `promoted System remaining established lifecycle survives contradictory legacy projection`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            database.contextDao().insert(contextEntity(id, "Inbox", null, "management"))
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(
                    enableAttachments = true,
                    experimentalCapabilityIds =
                        listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                ),
            )
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = id,
                        nameOverride = "Inbox",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = null,
                        workspaceOrder = 0L,
                        createdAt = 1L,
                        updatedAt = 2L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 2L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )
            val canonical =
                listOf(
                    capability(
                        id,
                        WorkspaceCapabilityType.CONNECTIONS,
                        WorkspaceCapabilityState.DISABLED,
                        ConnectionsCapabilityConfigurationCodec.CURRENT_VERSION,
                        ConnectionsCapabilityConfigurationCodec.encode(ConnectionsCapabilityConfigurationV1),
                    ),
                    capability(
                        id,
                        WorkspaceCapabilityType.INBOX_SORTING,
                        WorkspaceCapabilityState.ARCHIVED,
                        InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION,
                        InboxSortingCapabilityConfigurationCodec.encode(InboxSortingCapabilityConfigurationV1(emptyList())),
                    ),
                    capability(
                        id,
                        WorkspaceCapabilityType.KEY_PROBLEMS,
                        WorkspaceCapabilityState.DISABLED,
                        KeyProblemsCapabilityConfigurationCodec.CURRENT_VERSION,
                        KeyProblemsCapabilityConfigurationCodec.encode(KeyProblemsCapabilityConfigurationV1),
                        isDeleted = true,
                    ),
                )
            database.orientationDao().upsertWorkspaceCapabilities(canonical)

            bootstrapper(database).ensureBootstrapped(now = 100L)

            val after = database.orientationDao().getAllWorkspaceCapabilities()
                .filter { it.id in canonical.map { row -> row.id } }
            assertEquals(canonical, after)
        } finally {
            database.close()
        }
    }

    @Test
    fun `tombstoned System compatibility Context preserves established Inbox and Direction without reseeding missing state`() =
        runBlocking {
            val database = database()
            try {
                val establishedId = SystemContexts.INBOX.raw
                val missingId = SystemContexts.SESSION_IMPROVE.raw
                database.contextDao().insertContexts(
                    listOf(
                        contextEntity(establishedId, "Retired Inbox", null, "direction").copy(isDeleted = true),
                        contextEntity(missingId, "Retired Improve", null, "direction").copy(isDeleted = true),
                    ),
                )
                database.contextStructureDao().insertAll(
                    listOf(
                        ContextConfiguration.default(establishedId).copy(
                            enableInbox = true,
                            experimentalCapabilityIds = listOf(CapabilityId("direction")),
                        ),
                        ContextConfiguration.default(missingId).copy(
                            enableInbox = true,
                            experimentalCapabilityIds = listOf(CapabilityId("direction")),
                        ),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        promotedSystemWorkspace(establishedId),
                        promotedSystemWorkspace(missingId),
                    ),
                )
                val establishedInbox =
                    systemCapability(
                        id = "retired-system-inbox",
                        workspaceId = establishedId,
                        type = WorkspaceCapabilityType.INBOX,
                        state = WorkspaceCapabilityState.ARCHIVED,
                    )
                val establishedDirection =
                    systemCapability(
                        id = "retired-system-direction",
                        workspaceId = establishedId,
                        type = WorkspaceCapabilityType.DIRECTION,
                        state = WorkspaceCapabilityState.DISABLED,
                    ).copy(isDeleted = true)
                database.orientationDao().upsertWorkspaceCapabilities(
                    listOf(establishedInbox, establishedDirection),
                )

                bootstrapper(database).ensureBootstrapped(now = 100L)

                val after = database.orientationDao().getAllWorkspaceCapabilities()
                assertEquals(establishedInbox, after.single { it.id == establishedInbox.id })
                assertEquals(establishedDirection, after.single { it.id == establishedDirection.id })
                assertTrue(
                    after.none {
                        it.workspaceId == missingId &&
                            it.capabilityType in
                            setOf(
                                WorkspaceCapabilityType.INBOX.name,
                                WorkspaceCapabilityType.DIRECTION.name,
                            )
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `normal bootstrap ignores live and tombstoned legacy capability configuration for promoted System`() =
        runBlocking {
            val database = database()
            try {
                val deletedConfigurationId = SystemContexts.INBOX.raw
                val liveConfigurationId = SystemContexts.SESSION_IMPROVE.raw
                database.contextDao().insertContexts(
                    listOf(
                        contextEntity(deletedConfigurationId, "Inbox", null, "direction"),
                        contextEntity(liveConfigurationId, "Improve", null, "direction"),
                    ),
                )
                database.contextStructureDao().insertAll(
                    listOf(
                        ContextConfiguration.default(deletedConfigurationId).copy(
                            enableInbox = true,
                            experimentalCapabilityIds = listOf(CapabilityId("direction")),
                            isDeleted = true,
                        ),
                        ContextConfiguration.default(liveConfigurationId).copy(
                            enableInbox = true,
                            experimentalCapabilityIds = listOf(CapabilityId("direction")),
                        ),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        promotedSystemWorkspace(deletedConfigurationId),
                        promotedSystemWorkspace(liveConfigurationId),
                    ),
                )

                bootstrapper(database).ensureBootstrapped(now = 100L)

                val capabilities = database.orientationDao().getAllWorkspaceCapabilities()
                val legacyRoutedTypes =
                    setOf(
                        WorkspaceCapabilityType.INBOX.name,
                        WorkspaceCapabilityType.DIRECTION.name,
                    )

                assertTrue(
                    capabilities.none {
                        it.workspaceId in
                            setOf(deletedConfigurationId, liveConfigurationId) &&
                            it.capabilityType in legacyRoutedTypes
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `legacy exact System child is not projected beside canonical System parent`() = runBlocking {
        val database = database()
        try {
            val parentId = SystemContexts.PERSONAL_MANAGEMENT.raw
            val childId = SystemContexts.SESSION_IMPROVE.raw

            database.contextDao().insertContexts(
                listOf(
                    contextEntity(parentId, "Legacy promoted parent", null, "management"),
                    contextEntity(childId, "Legacy child", parentId, "development"),
                ),
            )
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = parentId,
                        nameOverride = "Canonical promoted parent",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = "management",
                        workspaceOrder = 0L,
                        createdAt = 5L,
                        updatedAt = 5L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 3L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )

            val report = bootstrapper(database).ensureBootstrapped(now = 100L)

            val parent = requireNotNull(database.workspaceDao().getById(parentId))
            val child = database.workspaceDao().getById(childId)

            assertEquals("Canonical promoted parent", parent.nameOverride)
            assertEquals(3L, parent.version)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, parent.provenance)
            assertNull(parent.sourceContextId)

            // Exact System Context shells are compatibility evidence, not Workspace projection authority.
            assertNull(child)

            assertFalse(
                report.issues.any {
                    it.contextId == parentId && it.code == "WORKSPACE_ID_COLLISION"
                },
            )
            assertFalse(
                report.issues.any {
                    it.contextId == childId &&
                        (it.code == "UNKNOWN_PARENT" ||
                            it.code == "WORKSPACE_PARENT_COLLISION")
                },
            )
        } finally {
            database.close()
        }
    }

    private fun bootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun promotedSystemWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 2L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun systemCapability(
        id: String,
        workspaceId: String,
        type: WorkspaceCapabilityType,
        state: WorkspaceCapabilityState,
    ) =
        com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity(
            id = id,
            workspaceId = workspaceId,
            capabilityType = type.name,
            instanceKey = "default",
            capabilityOrder = 0L,
            state = state.name,
            configurationVersion = 1,
            configuration =
                when (type) {
                    WorkspaceCapabilityType.INBOX -> InboxCapabilityConfigurationCodec.encodeDefault()
                    WorkspaceCapabilityType.DIRECTION -> DirectionCapabilityConfigurationCodec.encodeDefault()
                    else -> "{}"
                },
            createdAt = 1L,
            updatedAt = 10L,
            syncedAt = null,
            isDeleted = false,
            version = 7L,
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
