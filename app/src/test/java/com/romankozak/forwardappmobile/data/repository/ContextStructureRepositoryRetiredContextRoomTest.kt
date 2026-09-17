package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogConfigurationAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextStructureRepositoryRetiredContextRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `preset application preserves inert historical enableAdvanced`() = runBlocking {
        val database = database()
        try {
            val id = "ordinary"
            database.contextDao().insert(contextEntity(id))
            database.contextStructureDao().insertStructure(
                ContextConfiguration.default(id).copy(enableAdvanced = true),
            )
            database.structurePresetDao().insertPreset(
                ContextRoleProfile(
                    id = "management-preset",
                    code = "management",
                    label = "Management",
                    description = null,
                    enableAdvanced = false,
                ),
            )

            repository(database).applyPresetToContext(id, "management")

            assertEquals(
                true,
                database.contextStructureDao().getStructureByContext(id)?.enableAdvanced,
            )
            assertEquals(false, database.structurePresetDao().getByCode("management")?.enableAdvanced)
        } finally {
            database.close()
        }
    }

    @Test
    fun `reserved preset materialization leaves new enableAdvanced null`() = runBlocking {
        val database = database()
        try {
            repository(database).ensureReservedBaseRolePresets()

            assertTrue(database.structurePresetDao().getAllSync().isNotEmpty())
            assertTrue(database.structurePresetDao().getAllSync().all { it.enableAdvanced == null })
        } finally {
            database.close()
        }
    }

    @Test
    fun `new Context structure does not mint enableAdvanced`() = runBlocking {
        val database = database()
        try {
            val id = "new-structure"
            database.contextDao().insert(contextEntity(id))

            val structure = repository(database).ensureStructure(id)

            assertNull(structure.enableAdvanced)
            assertNull(database.contextStructureDao().getStructureByContext(id)?.enableAdvanced)
        } finally {
            database.close()
        }
    }

    @Test
    fun `System Backlog stale save is reconciled and explicit preset routes canonical lifecycle`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val stale = ContextConfiguration.default(id).copy(enableBacklog = true, updatedAt = 5L)
                database.contextDao().insert(contextEntity(id))
                database.contextStructureDao().insertStructure(stale)
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))
                val backlog = canonicalBacklog(database)
                backlog.enable(id, now = 6L)
                backlog.disable(id, now = 7L)
                val canonicalBefore = requireNotNull(backlog.getState(id))

                repository(database).updateStructure(
                    stale.copy(basePresetCode = "stale-metadata", updatedAt = 10L, version = 9L),
                )

                assertEquals(canonicalBefore, requireNotNull(backlog.getState(id)))
                assertEquals(
                    false,
                    database.contextStructureDao().getStructureByContext(id)?.enableBacklog,
                )

                repository(database).applyPresetToContext(id, "management")

                assertEquals(
                    WorkspaceCapabilityState.ACTIVE,
                    requireNotNull(backlog.getState(id)).lifecycleState,
                )
                assertEquals(
                    true,
                    database.contextStructureDao().getStructureByContext(id)?.enableBacklog,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `promoted System preset boundary commands all eight canonical target lifecycles`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                database.contextDao().insert(contextEntity(id))
                database.contextStructureDao().insertStructure(
                    ContextConfiguration.default(id).copy(
                        basePresetCode = "before",
                        experimentalCapabilityIds =
                            listOf(
                                CapabilityId("unrelated"),
                                CapabilityId("inbox_sorting"),
                            ),
                    ),
                )
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))

                repository(database).applyPresetToContext(id, "crisis_case")

                val lifecycleByType =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == id && it.instanceKey == "default" }
                        .associate { it.capabilityType to it.state }
                assertEquals(8, lifecycleByType.size)
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, lifecycleByType["BACKLOG"])
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, lifecycleByType["INBOX"])
                assertEquals(WorkspaceCapabilityState.DISABLED.name, lifecycleByType["INBOX_SORTING"])
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, lifecycleByType["KEY_PROBLEMS"])
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, lifecycleByType["DIRECTION"])
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, lifecycleByType["DASHBOARD"])
                assertEquals(WorkspaceCapabilityState.ACTIVE.name, lifecycleByType["EXECUTION_LOG"])
                assertEquals(WorkspaceCapabilityState.DISABLED.name, lifecycleByType["CONNECTIONS"])

                val persisted = requireNotNull(database.contextStructureDao().getStructureByContext(id))
                assertEquals("crisis_case", persisted.basePresetCode)
                assertEquals("ADDITIVE", persisted.applyMode)
                assertTrue(CapabilityId("unrelated") in persisted.experimentalCapabilityIds)
                assertTrue(CapabilityId("key_problems") in persisted.experimentalCapabilityIds)
                assertTrue(CapabilityId("inbox_sorting") !in persisted.experimentalCapabilityIds)
            } finally {
                database.close()
            }
        }

    @Test
    fun `canonical preset command failure rolls back structure and capability mutations`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val initial =
                    ContextConfiguration.default(id).copy(
                        basePresetCode = "before",
                        enableInbox = false,
                        experimentalCapabilityIds = listOf(CapabilityId("unrelated")),
                        updatedAt = 5L,
                    )
                database.contextDao().insert(contextEntity(id))
                database.contextStructureDao().insertStructure(initial)
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))
                val dashboard = canonicalDashboard(database)
                dashboard.enable(id, now = 6L)
                dashboard.archive(id, now = 7L)
                val capabilitiesBefore = database.orientationDao().getAllWorkspaceCapabilities()

                try {
                    repository(database).applyPresetToContext(id, "crisis_case")
                    fail("Expected archived canonical Dashboard to reject preset mutation")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(expected.message.orEmpty().contains("Archived"))
                }

                assertEquals(initial, database.contextStructureDao().getStructureByContext(id))
                assertEquals(capabilitiesBefore, database.orientationDao().getAllWorkspaceCapabilities())
            } finally {
                database.close()
            }
        }

    @Test
    fun `stale System metadata save reconciles Backlog behavior from canonical v2`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            val stale =
                ContextConfiguration.default(id).copy(
                    removeBacklogEntryAfterTagAutocopy = false,
                    basePresetCode = "old-metadata",
                    updatedAt = 5L,
                )
            database.contextDao().insert(contextEntity(id))
            database.contextStructureDao().insertStructure(stale)
            database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))
            val access = backlogConfigurationAccess(database)
            val backlog = canonicalBacklog(database)
            backlog.enable(id, now = 8L)
            assertTrue(access.setRemoveEntryAfterTagAutocopy(id, true, now = 10L))

            repository(database).updateStructure(
                stale.copy(basePresetCode = "new-metadata", updatedAt = 20L, version = 9L),
            )

            val final = requireNotNull(database.contextStructureDao().getStructureByContext(id))
            assertEquals("new-metadata", final.basePresetCode)
            assertTrue(final.removeBacklogEntryAfterTagAutocopy == true)
            assertTrue(requireNotNull(access.getState(id)).removeEntryAfterTagAutocopy)
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing legacy structure remains readable after Context retirement`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("retired-readable")
            val structure =
                ContextConfiguration.default(source.id).copy(
                    enableInbox = false,
                    updatedAt = 10L,
                )

            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(structure)
            database.contextDao().insert(source.softDelete(20L))

            val result = repository(database).ensureStructure(source.id)

            assertEquals(structure, result)
            assertEquals(structure, database.contextStructureDao().getStructureByContext(source.id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `retired Context cannot recreate missing legacy structure`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("retired-missing")
            database.contextDao().insert(source)
            database.contextDao().insert(source.softDelete(20L))

            try {
                repository(database).ensureStructure(source.id)
                fail("Expected retired Context structure creation to fail")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains(source.id))
            }

            assertNull(database.contextStructureDao().getStructureByContext(source.id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `retired Context rejects legacy structure and item mutations`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("retired-writes")
            val structure =
                ContextConfiguration.default(source.id).copy(
                    enableInbox = false,
                    enableBacklog = false,
                    updatedAt = 10L,
                )
            val existingItem =
                ContextStructureItem(
                    id = "existing-item",
                    contextStructureId = structure.id,
                    entityType = "NOTE",
                    roleCode = "existing",
                    containerType = null,
                    title = "Existing",
                    isEnabled = true,
                    updatedAt = 10L,
                )
            val newItem =
                ContextStructureItem(
                    id = "new-item",
                    contextStructureId = structure.id,
                    entityType = "NOTE",
                    roleCode = "new",
                    containerType = null,
                    title = "New",
                    updatedAt = 30L,
                )

            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(structure)
            database.contextStructureDao().insertItems(listOf(existingItem))
            database.contextDao().insert(source.softDelete(20L))

            val repository = repository(database)

            repository.updateStructure(
                structure.copy(
                    enableInbox = true,
                    updatedAt = 30L,
                ),
            )
            repository.upsertStructure(
                structure.copy(
                    enableBacklog = true,
                    updatedAt = 31L,
                ),
            )
            repository.addOrUpdateItem(structure.id, newItem)
            repository.setItemEnabled(existingItem, enabled = false)

            assertEquals(
                structure,
                database.contextStructureDao().getStructureByContext(source.id),
            )
            assertEquals(
                listOf(existingItem),
                database.contextStructureDao().getItems(structure.id),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `stale System metadata save reconciles bounded compatibility fields from canonical state`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val source = contextEntity(id)
                val stale =
                    ContextConfiguration.default(id).copy(
                        basePresetCode = "before",
                        enableInbox = false,
                        removeInboxEntryAfterTagAutocopy = false,
                        experimentalCapabilityIds = listOf(CapabilityId("unrelated")),
                        enableAutoLinkSubprojects = true,
                        enableLog = false,
                        updatedAt = 5L,
                        version = 1L,
                    )
                database.contextDao().insert(source)
                database.contextStructureDao().insertStructure(stale)
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))
                val access = capabilityAccess(database)
                val repository = repository(database)

                // Ordering A: an ordinary metadata save completes before the
                // canonical command. The canonical command must win and write
                // its bounded compatibility output afterward.
                repository.updateStructure(
                    stale.copy(basePresetCode = "metadata-first", enableLog = true, updatedAt = 6L),
                )
                val capturedBeforeCanonicalCommand =
                    requireNotNull(database.contextStructureDao().getStructureByContext(id))

                access.setInboxEnabled(id, true, now = 10L)
                access.updateInboxConfiguration(
                    id,
                    InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED),
                    now = 11L,
                )
                access.setDirectionEnabled(id, true, now = 12L)
                access.updateDirectionConfiguration(
                    id,
                    DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false),
                    now = 13L,
                )
                val canonicalBefore =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .filter {
                            it.workspaceId == id &&
                                it.capabilityType in setOf("INBOX", "DIRECTION")
                        }
                        .associateBy { it.id }
                val compatibilityVersionAfterCanonical =
                    requireNotNull(database.contextStructureDao().getStructureByContext(id)).version

                // Ordering B: a stale snapshot captured before the canonical
                // command is saved afterward. Repository reconciliation must
                // keep canonical state authoritative.
                repository.updateStructure(
                    capturedBeforeCanonicalCommand.copy(
                        basePresetCode = "metadata-save",
                        enableLog = true,
                        enableInbox = false,
                        removeInboxEntryAfterTagAutocopy = false,
                        experimentalCapabilityIds = listOf(CapabilityId("unrelated")),
                        enableAutoLinkSubprojects = true,
                        updatedAt = 20L,
                        version = 2L,
                    ),
                )

                val canonicalAfter =
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .filter { it.workspaceId == id && it.id in canonicalBefore }
                        .associateBy { it.id }
                assertEquals(canonicalBefore, canonicalAfter)
                val final = requireNotNull(database.contextStructureDao().getStructureByContext(id))
                assertEquals("metadata-save", final.basePresetCode)
                assertTrue(final.enableLog == true)
                assertTrue(final.enableInbox == true)
                assertTrue(final.removeInboxEntryAfterTagAutocopy == true)
                assertTrue(CapabilityId("direction") in final.experimentalCapabilityIds)
                assertTrue(CapabilityId("unrelated") in final.experimentalCapabilityIds)
                assertEquals(false, final.enableAutoLinkSubprojects)
                assertTrue(final.version >= compatibilityVersionAfterCanonical)
            } finally {
                database.close()
            }
        }

    @Test
    fun `explicit missing System Inbox disable establishes durable canonical negative state`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val source = contextEntity(id)
                val stale = ContextConfiguration.default(id).copy(enableInbox = true, updatedAt = 5L)
                database.contextDao().insert(source)
                database.contextStructureDao().insertStructure(stale)
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))
                val access = capabilityAccess(database)

                assertTrue(access.setInboxEnabled(id, false, now = 10L))
                val established =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.workspaceId == id && it.capabilityType == "INBOX"
                    }
                assertEquals(WorkspaceCapabilityState.DISABLED.name, established.state)
                assertTrue(!established.isDeleted)
                assertEquals(1L, established.version)

                assertTrue(access.setInboxEnabled(id, false, now = 11L))
                assertEquals(
                    established,
                    database.orientationDao().getAllWorkspaceCapabilities().single { it.id == established.id },
                )

                repository(database).updateStructure(
                    stale.copy(basePresetCode = "stale-metadata", updatedAt = 20L, version = 9L),
                )

                assertEquals(
                    established,
                    database.orientationDao().getAllWorkspaceCapabilities().single { it.id == established.id },
                )
                assertEquals(
                    false,
                    database.contextStructureDao().getStructureByContext(id)?.enableInbox,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `explicit missing System Direction disable establishes durable canonical negative state`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                val direction = CapabilityId("direction")
                val source = contextEntity(id)
                val stale =
                    ContextConfiguration.default(id).copy(
                        experimentalCapabilityIds = listOf(direction),
                        updatedAt = 5L,
                    )
                database.contextDao().insert(source)
                database.contextStructureDao().insertStructure(stale)
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))
                val access = capabilityAccess(database)

                assertTrue(access.setDirectionEnabled(id, false, now = 10L))
                val established =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.workspaceId == id && it.capabilityType == "DIRECTION"
                    }
                assertEquals(WorkspaceCapabilityState.DISABLED.name, established.state)
                assertTrue(!established.isDeleted)
                assertEquals(1L, established.version)

                assertTrue(access.setDirectionEnabled(id, false, now = 11L))
                assertEquals(
                    established,
                    database.orientationDao().getAllWorkspaceCapabilities().single { it.id == established.id },
                )

                repository(database).updateStructure(
                    stale.copy(basePresetCode = "stale-metadata", updatedAt = 20L, version = 9L),
                )

                assertEquals(
                    established,
                    database.orientationDao().getAllWorkspaceCapabilities().single { it.id == established.id },
                )
                assertTrue(
                    direction !in
                        requireNotNull(database.contextStructureDao().getStructureByContext(id))
                            .experimentalCapabilityIds,
                )
            } finally {
                database.close()
            }
        }


    @Test
    fun `ordinary System structure write cannot seed formerly routed canonical capabilities`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                database.contextDao().insert(contextEntity(id))
                database.workspaceDao().upsert(listOf(promotedSystemWorkspace(id)))

                val stale =
                    ContextConfiguration.default(id).copy(
                        enableInbox = true,
                        enableAttachments = true,
                        experimentalCapabilityIds =
                            listOf(
                                CapabilityId("direction"),
                                CapabilityId("inbox_sorting"),
                                CapabilityId("key_problems"),
                            ),
                        updatedAt = 10L,
                        version = 1L,
                    )
                database.contextStructureDao().insertStructure(stale)

                val formerlyRouted =
                    setOf(
                        "INBOX",
                        "CONNECTIONS",
                        "DIRECTION",
                        "INBOX_SORTING",
                        "KEY_PROBLEMS",
                    )

                assertTrue(
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .none {
                            it.workspaceId == id &&
                                it.capabilityType in formerlyRouted
                        },
                )

                repository(database).updateStructure(
                    stale.copy(
                        basePresetCode = "stale-runtime-write",
                        updatedAt = 20L,
                        version = 2L,
                    ),
                )

                assertTrue(
                    database.orientationDao().getAllWorkspaceCapabilities()
                        .none {
                            it.workspaceId == id &&
                                it.capabilityType in formerlyRouted
                        },
                )
            } finally {
                database.close()
            }
        }

    private fun repository(database: AppDatabase) =
        ContextStructureRepository(
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
            structurePresetDao = database.structurePresetDao(),
            structurePresetItemDao = database.structurePresetItemDao(),
            workspaceWriteThrough = ContextWorkspaceWriteThrough(bootstrapper(database)),
            systemContextCanonicalInboxDirectionAccess = capabilityAccess(database),
            systemContextCanonicalRemainingCapabilityLifecycleAccess = remainingCapabilityAccess(database),
            systemContextCanonicalBacklogConfigurationAccess = backlogConfigurationAccess(database),
            systemContextCanonicalBacklogLifecycleAccess = backlogLifecycleAccess(database),
            canonicalDashboardCapabilityRepository =
                canonicalDashboard(database),
            canonicalExecutionLogRepository = canonicalExecutionLog(database),
        )

    private fun canonicalDashboard(database: AppDatabase) =
        CanonicalDashboardCapabilityRepository(capabilityStore(database))

    private fun canonicalExecutionLog(database: AppDatabase) =
        CanonicalExecutionLogRepository(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextManagementDao = database.contextManagementDao(),
            instanceStore = capabilityStore(database),
        )

    private fun backlogLifecycleAccess(database: AppDatabase) =
        SystemContextCanonicalBacklogLifecycleAccess(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextStructureDao = database.contextStructureDao(),
            backlogRepository = canonicalBacklog(database),
        )

    private fun backlogConfigurationAccess(
        database: AppDatabase,
    ) =
        SystemContextCanonicalBacklogConfigurationAccess(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextStructureDao = database.contextStructureDao(),
            backlogRepository = canonicalBacklog(database),
        )

    private fun canonicalBacklog(database: AppDatabase) =
        CanonicalBacklogRepository(
            database = database,
            instanceStore = capabilityStore(database),
            entryDao = database.workspaceBacklogEntryDao(),
            targetValidator = CanonicalBacklogTargetValidator(database),
        )

    private fun remainingCapabilityAccess(
        database: AppDatabase,
    ): SystemContextCanonicalRemainingCapabilityLifecycleAccess {
        val store = capabilityStore(database)
        return SystemContextCanonicalRemainingCapabilityLifecycleAccess(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextStructureDao = database.contextStructureDao(),
            connectionsRepository =
                CanonicalConnectionsRepository(database, store, database.workspaceConnectionDao()),
            inboxSortingRepository = CanonicalInboxSortingRepository(store, database.orientationDao()),
            keyProblemsRepository =
                CanonicalKeyProblemsRepository(
                    database,
                    store,
                    database.workspaceProblemDao(),
                    database.workspaceDao(),
                    database.attachmentDao(),
                ),
        )
    }

    private fun capabilityAccess(database: AppDatabase): SystemContextCanonicalInboxDirectionAccess {
        val store = capabilityStore(database)
        return SystemContextCanonicalInboxDirectionAccess(
            database = database,
            workspaceDao = database.workspaceDao(),
            contextStructureDao = database.contextStructureDao(),
            inboxRepository = inboxRepository(database, store),
            directionRepository = directionRepository(database, store),
        )
    }



    private fun capabilityStore(database: AppDatabase) =
        CanonicalCapabilityInstanceStore(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
        )

    private fun inboxRepository(
        database: AppDatabase,
        store: CanonicalCapabilityInstanceStore,
    ) =
        CanonicalInboxRepository(
            database = database,
            instanceStore = store,
            recordDao = database.workspaceInboxRecordDao(),
        )

    private fun directionRepository(
        database: AppDatabase,
        store: CanonicalCapabilityInstanceStore,
    ) =
        CanonicalDirectionRepository(
            database = database,
            instanceStore = store,
            entryDao = database.workspaceDirectionEntryDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            orientationRepository =
                CanonicalOrientationRepository(
                    database = database,
                    dao = database.orientationDao(),
                ),
        )

    private fun bootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

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

    private fun contextEntity(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 2L,
            roleCode = "default",
        )
}
