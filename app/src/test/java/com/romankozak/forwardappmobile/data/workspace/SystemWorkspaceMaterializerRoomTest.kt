package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
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
class SystemWorkspaceMaterializerRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `transient legacy evidence creates canonical owner without Context shell`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val materializer = materializer(database)

                materializer.materializeAll(
                    now = 100L,
                    seedMissingFactoryCapabilities = false,
                    legacyContextEvidence =
                        listOf(
                            SystemWorkspaceLegacyContextEvidence(
                                id = id,
                                name = "Imported Inbox",
                                description = "legacy evidence",
                                parentId = null,
                                roleCode = "legacy-role",
                                order = 42L,
                                createdAt = 10L,
                                updatedAt = 20L,
                                isDeleted = false,
                                version = 7L,
                            ),
                        ),
                )

                assertNull(database.contextDao().getContextById(id))

                val workspace = requireNotNull(database.workspaceDao().getById(id))
                assertTrue(workspace.nameOverride == "Imported Inbox")
                assertTrue(workspace.descriptionOverride == "legacy evidence")
                assertTrue(workspace.roleCode == "legacy-role")
                assertTrue(workspace.workspaceOrder == 42L)
                assertNull(workspace.sourceContextId)
                assertTrue(workspace.version == 7L)
            } finally {
                database.close()
            }
        }

    @Test
    fun `context-free materialization creates exact canonical System factory defaults`() =
        runBlocking {
            val database = database()
            try {
                assertTrue(database.contextDao().getAll().isEmpty())

                val result = materializer(database).materializeAll(now = 10L)

                assertTrue(result.changed)
                assertEquals(20, result.created)
                assertEquals(0, result.preservedCanonical)
                assertEquals(0, result.promotedLegacy)
                assertEquals(60, result.seededFactoryCapabilities)
                assertTrue(database.contextDao().getAll().isEmpty())

                val workspaces = database.workspaceDao().getAll().associateBy { it.id }
                assertEquals(20, workspaces.size)
                SystemOperationalDefinitions.all.forEach { definition ->
                    val workspace = requireNotNull(workspaces[definition.id])
                    assertEquals(definition.defaultName, workspace.nameOverride)
                    assertNull(workspace.descriptionOverride)
                    assertEquals(definition.defaultParentId, workspace.parentWorkspaceId)
                    assertNull(workspace.roleCode)
                    assertEquals(0L, workspace.workspaceOrder)
                    assertEquals(10L, workspace.createdAt)
                    assertEquals(10L, workspace.updatedAt)
                    assertNull(workspace.syncedAt)
                    assertFalse(workspace.isDeleted)
                    assertEquals(1L, workspace.version)
                    assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
                    assertNull(workspace.sourceContextId)
                }

                val capabilities =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .groupBy { it.workspaceId }
                assertEquals(20, capabilities.size)
                assertEquals(60, capabilities.values.sumOf { it.size })

                SystemOperationalDefinitions.all.forEach { definition ->
                    val byType =
                        requireNotNull(capabilities[definition.id])
                            .associateBy { WorkspaceCapabilityType.valueOf(it.capabilityType) }

                    assertEquals(
                        setOf(
                            WorkspaceCapabilityType.DASHBOARD,
                            WorkspaceCapabilityType.EXECUTION_LOG,
                            WorkspaceCapabilityType.BACKLOG,
                        ),
                        byType.keys,
                    )

                    val dashboard = byType.getValue(WorkspaceCapabilityType.DASHBOARD)
                    assertEquals(WorkspaceCapabilityState.ACTIVE.name, dashboard.state)
                    assertEquals(
                        DashboardCapabilityConfigurationCodec.CURRENT_VERSION,
                        dashboard.configurationVersion,
                    )
                    assertEquals(
                        DashboardCapabilityConfigurationCodec.encodeDefault(),
                        dashboard.configuration,
                    )

                    val log = byType.getValue(WorkspaceCapabilityType.EXECUTION_LOG)
                    assertEquals(WorkspaceCapabilityState.DISABLED.name, log.state)
                    assertEquals(
                        ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION,
                        log.configurationVersion,
                    )
                    assertEquals(
                        ExecutionLogCapabilityConfigurationCodec.encodeDefault(),
                        log.configuration,
                    )

                    val backlog = byType.getValue(WorkspaceCapabilityType.BACKLOG)
                    assertEquals(WorkspaceCapabilityState.DISABLED.name, backlog.state)
                    assertEquals(
                        BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                        backlog.configurationVersion,
                    )
                    assertEquals(
                        BacklogCapabilityConfigurationCodec.encode(
                            BacklogCapabilityConfigurationV2(
                                removeEntryAfterTagAutocopy = false,
                            ),
                        ),
                        backlog.configuration,
                    )
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun `repeated materialization preserves customized canonical metadata without churn`() =
        runBlocking {
            val database = database()
            try {
                val materializer = materializer(database)
                materializer.materializeAll(now = 10L)

                val inbox = requireNotNull(database.workspaceDao().getById(SystemContexts.INBOX.raw))
                database.workspaceDao().upsert(
                    listOf(
                        inbox.copy(
                            nameOverride = "Customized Inbox",
                            descriptionOverride = "Canonical description",
                            parentWorkspaceId = SystemContexts.STRATEGIC.raw,
                            roleCode = "canonical-role",
                            workspaceOrder = 37L,
                            updatedAt = 20L,
                            syncedAt = 21L,
                            version = 9L,
                        ),
                    ),
                )
                val before = database.workspaceDao().getAll().associateBy { it.id }

                val result = materializer.materializeAll(now = 999L)

                assertFalse(result.changed)
                assertEquals(0, result.created)
                assertEquals(20, result.preservedCanonical)
                assertEquals(0, result.promotedLegacy)
                assertEquals(0, result.seededFactoryCapabilities)
                assertEquals(before, database.workspaceDao().getAll().associateBy { it.id })
            } finally {
                database.close()
            }
        }

    @Test
    fun `legacy Context-backed row is validated and promoted directly`() = runBlocking {
        val database = database()
        try {
            val target =
                SystemOperationalDefinitions.all.first {
                    it.id == SystemContexts.INBOX.raw
                }
            val legacyContext =
                target.context(
                    name = "Historical Inbox",
                    createdAt = 3L,
                )
            database.contextDao().insert(legacyContext)

            val legacyWorkspace =
                target.workspace(
                    name = legacyContext.name,
                    createdAt = legacyContext.createdAt,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = target.id,
                )
            database.workspaceDao().upsert(listOf(legacyWorkspace))

            val result =
                materializer(database).materializeAll(now = 10L)

            assertTrue(result.changed)
            assertEquals(19, result.created)
            assertEquals(0, result.preservedCanonical)
            assertEquals(1, result.promotedLegacy)
            assertEquals(60, result.seededFactoryCapabilities)

            val promoted =
                requireNotNull(database.workspaceDao().getById(target.id))

            assertEquals(legacyWorkspace.nameOverride, promoted.nameOverride)
            assertEquals(
                legacyWorkspace.descriptionOverride,
                promoted.descriptionOverride,
            )
            assertEquals(
                legacyWorkspace.parentWorkspaceId,
                promoted.parentWorkspaceId,
            )
            assertEquals(legacyWorkspace.roleCode, promoted.roleCode)
            assertEquals(
                legacyWorkspace.workspaceOrder,
                promoted.workspaceOrder,
            )
            assertEquals(legacyWorkspace.createdAt, promoted.createdAt)
            assertEquals(10L, promoted.updatedAt)
            assertNull(promoted.syncedAt)
            assertEquals(legacyWorkspace.version + 1L, promoted.version)
            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                promoted.provenance,
            )
            assertNull(promoted.sourceContextId)

            val second =
                materializer(database).materializeAll(now = 20L)

            assertFalse(second.changed)
            assertEquals(0, second.created)
            assertEquals(20, second.preservedCanonical)
            assertEquals(0, second.promotedLegacy)
            assertEquals(0, second.seededFactoryCapabilities)
        } finally {
            database.close()
        }
    }

    @Test
    fun `stale Context-backed System projection fails closed`() = runBlocking {
        val database = database()
        try {
            val target =
                SystemOperationalDefinitions.all.first {
                    it.id == SystemContexts.INBOX.raw
                }
            val legacyContext =
                target.context(
                    name = "Historical Inbox",
                    createdAt = 3L,
                )
            database.contextDao().insert(legacyContext)

            val staleWorkspace =
                target.workspace(
                    name = "Stale Inbox",
                    createdAt = legacyContext.createdAt,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = target.id,
                )
            database.workspaceDao().upsert(listOf(staleWorkspace))

            val failure =
                runCatching {
                    materializer(database).materializeAll(now = 10L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(failure?.message?.contains(target.id) == true)
            assertTrue(failure?.message?.contains("stale") == true)
            assertTrue(failure?.message?.contains("name") == true)

            assertEquals(
                listOf(staleWorkspace),
                database.workspaceDao().getAll(),
            )
            assertTrue(
                database.orientationDao()
                    .getAllWorkspaceCapabilities()
                    .isEmpty(),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context-backed System Workspace without live Context fails closed`() =
        runBlocking {
            val database = database()
            try {
                val target =
                    SystemOperationalDefinitions.all.first {
                        it.id == SystemContexts.INBOX.raw
                    }
                val orphan =
                    target.workspace(
                        provenance = WorkspaceProvenance.CONTEXT_BACKED,
                        sourceContextId = target.id,
                    )
                database.workspaceDao().upsert(listOf(orphan))

                val failure =
                    runCatching {
                        materializer(database).materializeAll(now = 10L)
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(failure?.message?.contains(target.id) == true)
                assertEquals(
                    listOf(orphan),
                    database.workspaceDao().getAll(),
                )
                assertTrue(
                    database.orientationDao()
                        .getAllWorkspaceCapabilities()
                        .isEmpty(),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `malformed ownership fails before any missing Workspace is created`() = runBlocking {
        val database = database()
        try {
            val invalidId = SystemContexts.INBOX.raw
            database.workspaceDao().upsert(
                listOf(
                    SystemOperationalDefinitions.all.first { it.id == invalidId }.workspace(
                        provenance = WorkspaceProvenance.CANONICAL_ONLY,
                        sourceContextId = invalidId,
                    ),
                ),
            )

            val failure = runCatching { materializer(database).materializeAll(now = 10L) }

            assertTrue(failure.isFailure)
            assertEquals(setOf(invalidId), database.workspaceDao().getAll().map { it.id }.toSet())
        } finally {
            database.close()
        }
    }

    @Test
    fun `deleted canonical System Workspace fails closed without resurrection`() = runBlocking {
        val database = database()
        try {
            val deleted =
                SystemOperationalDefinitions.all.first().workspace(
                    isDeleted = true,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY,
                    sourceContextId = null,
                )
            database.workspaceDao().upsert(listOf(deleted))

            val failure = runCatching { materializer(database).materializeAll(now = 10L) }

            assertTrue(failure.isFailure)
            assertEquals(listOf(deleted), database.workspaceDao().getAll())
        } finally {
            database.close()
        }
    }

    @Test
    fun `mixed ownership converges every reserved identity directly`() =
        runBlocking {
            val database = database()
            try {
                val canonicalDefinition = SystemOperationalDefinitions.all[0]
                val legacyDefinition = SystemOperationalDefinitions.all[1]
                val historicalDefinition = SystemOperationalDefinitions.all[2]

                val canonical =
                    canonicalDefinition.workspace(
                        name = "Canonical custom",
                    )
                val legacyContext =
                    legacyDefinition.context(
                        name = "Legacy custom",
                    )
                val legacyWorkspace =
                    legacyDefinition.workspace(
                        name = legacyContext.name,
                        provenance = WorkspaceProvenance.CONTEXT_BACKED,
                        sourceContextId = legacyDefinition.id,
                    )
                val historicalContext =
                    historicalDefinition.context(
                        name = "Historical custom",
                    )

                database.workspaceDao().upsert(
                    listOf(canonical, legacyWorkspace),
                )
                database.contextDao().insert(legacyContext)
                database.contextDao().insert(historicalContext)

                val result =
                    materializer(database).materializeAll(now = 10L)

                assertEquals(18, result.created)
                assertEquals(1, result.preservedCanonical)
                assertEquals(1, result.promotedLegacy)
                assertEquals(60, result.seededFactoryCapabilities)
                assertEquals(20, database.workspaceDao().getAll().size)

                assertEquals(
                    canonical,
                    database.workspaceDao()
                        .getById(canonicalDefinition.id),
                )

                val promotedLegacy =
                    requireNotNull(
                        database.workspaceDao()
                            .getById(legacyDefinition.id),
                    )
                assertEquals(
                    "Legacy custom",
                    promotedLegacy.nameOverride,
                )
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    promotedLegacy.provenance,
                )
                assertNull(promotedLegacy.sourceContextId)

                val adoptedHistorical =
                    requireNotNull(
                        database.workspaceDao()
                            .getById(historicalDefinition.id),
                    )
                assertEquals(
                    "Historical custom",
                    adoptedHistorical.nameOverride,
                )
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    adoptedHistorical.provenance,
                )
                assertNull(adoptedHistorical.sourceContextId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `historical parent metadata is adopted without deferring canonical descendants`() =
        runBlocking {
            val database = database()
            try {
                val levels =
                    SystemOperationalDefinitions.all.first {
                        it.id == SystemContexts.LEVELS.raw
                    }
                database.contextDao().insert(
                    levels.context(
                        name = "Historical Levels",
                    ),
                )

                val result =
                    materializer(database).materializeAll(now = 10L)

                assertEquals(20, result.created)
                assertEquals(0, result.preservedCanonical)
                assertEquals(0, result.promotedLegacy)
                assertEquals(60, result.seededFactoryCapabilities)

                val adoptedLevels =
                    requireNotNull(
                        database.workspaceDao()
                            .getById(SystemContexts.LEVELS.raw),
                    )
                assertEquals(
                    "Historical Levels",
                    adoptedLevels.nameOverride,
                )
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    adoptedLevels.provenance,
                )
                assertNull(adoptedLevels.sourceContextId)

                assertTrue(
                    database.workspaceDao()
                        .getById(SystemContexts.TODAY.raw) != null,
                )
                assertTrue(
                    database.workspaceDao()
                        .getById(SystemContexts.INBOX.raw) != null,
                )

                val liveIds =
                    database.workspaceDao()
                        .getAll()
                        .mapTo(hashSetOf()) { it.id }

                database.workspaceDao().getAll().forEach { workspace ->
                    workspace.parentWorkspaceId?.let { parentId ->
                        assertTrue(
                            "Direct materialization left dangling parent " +
                                "$parentId for ${workspace.id}",
                            parentId in liveIds,
                        )
                    }
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun `ownership-only materialization leaves factory capabilities absent until final convergence`() =
        runBlocking {
            val database = database()
            try {
                val historical =
                    SystemOperationalDefinitions.all.first {
                        it.id == SystemContexts.INBOX.raw
                    }.context(
                        name = "Historical Inbox",
                        createdAt = 3L,
                    )
                database.contextDao().insert(historical)

                val ownershipOnly =
                    materializer(database).materializeAll(
                        now = 10L,
                        seedMissingFactoryCapabilities = false,
                    )

                assertEquals(20, ownershipOnly.created)
                assertEquals(0, ownershipOnly.preservedCanonical)
                assertEquals(0, ownershipOnly.promotedLegacy)
                assertEquals(
                    0,
                    ownershipOnly.seededFactoryCapabilities,
                )
                assertTrue(
                    database.orientationDao()
                        .getAllWorkspaceCapabilities()
                        .isEmpty(),
                )

                val inbox =
                    requireNotNull(
                        database.workspaceDao()
                            .getById(SystemContexts.INBOX.raw),
                    )
                assertEquals(
                    "Historical Inbox",
                    inbox.nameOverride,
                )
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    inbox.provenance,
                )
                assertNull(inbox.sourceContextId)

                val finalConvergence =
                    materializer(database).materializeAll(now = 20L)

                assertEquals(0, finalConvergence.created)
                assertEquals(
                    20,
                    finalConvergence.preservedCanonical,
                )
                assertEquals(0, finalConvergence.promotedLegacy)
                assertEquals(
                    60,
                    finalConvergence.seededFactoryCapabilities,
                )
                assertEquals(
                    60,
                    database.orientationDao()
                        .getAllWorkspaceCapabilities()
                        .size,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `canonical Inbox capability works without a Context row`() = runBlocking {
        val database = database()
        try {
            materializer(database).materializeAll(now = 10L)
            val store =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                )
            val inbox =
                CanonicalInboxRepository(
                    database = database,
                    instanceStore = store,
                    recordDao = database.workspaceInboxRecordDao(),
                )

            inbox.enable(SystemContexts.INBOX.raw, now = 20L)

            assertEquals(
                WorkspaceCapabilityState.ACTIVE,
                requireNotNull(inbox.getState(SystemContexts.INBOX.raw)).lifecycleState,
            )
            assertTrue(database.contextDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }


    @Test
    fun `normal bootstrap cannot derive promoted System capability from contradictory legacy configuration`() =
        runBlocking {
            val database = database()
            try {
                materializer(database).materializeAll(now = 10L)

                val definition =
                    SystemOperationalDefinitions.all.first {
                        it.id == SystemContexts.INBOX.raw
                    }
                database.contextDao().insert(
                    definition.context(
                        name = "Legacy compatibility Inbox",
                        createdAt = 1L,
                    ),
                )
                database.contextStructureDao().insertStructure(
                    ContextConfiguration.default(definition.id).copy(
                        enableDashboard = false,
                        enableBacklog = true,
                        experimentalCapabilityIds = listOf(CapabilityId("direction")),
                        updatedAt = 20L,
                    ),
                )

                val before =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == definition.id }

                assertEquals(
                    setOf(
                        WorkspaceCapabilityType.DASHBOARD,
                        WorkspaceCapabilityType.EXECUTION_LOG,
                        WorkspaceCapabilityType.BACKLOG,
                    ),
                    before.map { WorkspaceCapabilityType.valueOf(it.capabilityType) }.toSet(),
                )

                CanonicalWorkspaceBootstrapper(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                    contextDao = database.contextDao(),
                    contextStructureDao = database.contextStructureDao(),
                ).ensureBootstrapped(now = 30L)

                val after =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == definition.id }

                assertEquals(before, after)
                assertTrue(
                    after.none {
                        it.capabilityType == WorkspaceCapabilityType.DIRECTION.name
                    },
                )
            } finally {
                database.close()
            }
        }

    private fun materializer(database: AppDatabase) =
        SystemWorkspaceMaterializer(
            database = database,
            contextDao = database.contextDao(),
            workspaceDao = database.workspaceDao(),
        )

    private fun com.romankozak.forwardappmobile.core.context.SystemOperationalDefinition.context(
        name: String = defaultName,
        createdAt: Long = 1L,
    ) = ContextEntity(
        id = id,
        name = name,
        description = null,
        parentId = defaultParentId,
        createdAt = createdAt,
        updatedAt = null,
    )

    private fun com.romankozak.forwardappmobile.core.context.SystemOperationalDefinition.workspace(
        name: String = defaultName,
        createdAt: Long = 1L,
        isDeleted: Boolean = false,
        provenance: WorkspaceProvenance = WorkspaceProvenance.CANONICAL_ONLY,
        sourceContextId: String? = null,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = name,
        descriptionOverride = null,
        parentWorkspaceId = defaultParentId,
        roleCode = null,
        workspaceOrder = 0L,
        createdAt = createdAt,
        updatedAt = createdAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
        provenance = provenance.name,
        sourceContextId = sourceContextId,
    )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
