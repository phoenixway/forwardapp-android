package com.romankozak.forwardappmobile.features.contexts.shared

import io.mockk.Runs
import io.mockk.just

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspacePresentation
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.SystemRemainingCapabilityLifecycleState
import com.romankozak.forwardappmobile.data.workspace.WorkspaceBootstrapReport
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityReadSnapshot
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.DirectionCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.InboxCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.ConnectionsCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.InboxSortingCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.KeyProblemsCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCapabilityState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.KeyProblemsCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidWorkspaceRepositoryAdapterSystemCapabilityTest {
    @Test
    fun `System read uses canonical Workspace presentation over stale Context shell`() = runTest {
        val id = SystemContexts.INBOX.raw
        val stale =
            context(id).copy(
                name = "Stale Inbox",
                description = "Stale description",
                parentId = "stale-parent",
                order = 99L,
            )
        val canonical =
            CanonicalWorkspacePresentation(
                id = id,
                nameOverride = "Canonical Inbox",
                descriptionOverride = "Canonical description",
                parentWorkspaceId = "canonical-parent",
                roleCode = "canonical-role",
                workspaceOrder = 3L,
                isDeleted = false,
            )
        val fixture =
            fixture(
                contexts = listOf(stale),
                configurations = emptyMap(),
                states = mutableMapOf(),
                canonicalPresentations = mapOf(id to canonical),
            )

        val result = fixture.adapter.getContexts().single()

        assertEquals("Canonical Inbox", result.name)
        assertEquals("Canonical description", result.description)
        assertEquals("canonical-parent", result.parentId)
    }

    @Test
    fun `System read uses canonical Backlog lifecycle over contradictory legacy flags`() = runTest {
        val activeId = SystemContexts.INBOX.raw
        val disabledId = SystemContexts.SESSION_IMPROVE.raw
        val archivedId = SystemContexts.SESSION_CONTROL.raw
        val deletedId = SystemContexts.SESSION_RECOVERY.raw
        val fixture =
            fixture(
                contexts =
                    listOf(
                        context(activeId),
                        context(disabledId),
                        context(archivedId),
                        context(deletedId),
                    ),
                configurations =
                    mapOf(
                        activeId to ContextConfiguration.default(activeId).copy(enableBacklog = false),
                        disabledId to ContextConfiguration.default(disabledId).copy(enableBacklog = true),
                        archivedId to ContextConfiguration.default(archivedId).copy(enableBacklog = true),
                        deletedId to ContextConfiguration.default(deletedId).copy(enableBacklog = true),
                    ),
                states =
                    mutableMapOf(
                        activeId to systemState(),
                        disabledId to systemState(),
                        archivedId to systemState(),
                        deletedId to systemState(),
                    ),
                backlogStates =
                    mutableMapOf(
                        activeId to backlogState(WorkspaceCapabilityState.ACTIVE),
                        disabledId to backlogState(WorkspaceCapabilityState.DISABLED),
                        archivedId to backlogState(WorkspaceCapabilityState.ARCHIVED),
                        deletedId to backlogState(WorkspaceCapabilityState.ACTIVE, isDeleted = true),
                    ),
            )

        val summaries = fixture.adapter.getContexts().associateBy { it.id }

        assertTrue("backlog" in summaries.getValue(activeId).enabledCapabilityIds)
        assertFalse("backlog" in summaries.getValue(disabledId).enabledCapabilityIds)
        assertFalse("backlog" in summaries.getValue(archivedId).enabledCapabilityIds)
        assertFalse("backlog" in summaries.getValue(deletedId).enabledCapabilityIds)
    }

    @Test
    fun `malformed System Backlog owner fails closed despite Backlog default view`() = runTest {
        val id = SystemContexts.INBOX.raw
        val fixture =
            fixture(
                contexts = listOf(context(id, defaultView = ContextViewMode.BACKLOG)),
                configurations = mapOf(id to ContextConfiguration.default(id).copy(enableBacklog = true)),
                states = mutableMapOf(id to systemState()),
                backlogStates =
                    mutableMapOf(
                        id to
                            SystemBacklogLifecycleState(
                                backlog = null,
                                isEstablished = true,
                                isCanonicalOwnerAvailable = false,
                            ),
                    ),
            )

        val summary = fixture.adapter.getContexts().single()

        assertFalse("backlog" in summary.enabledCapabilityIds)
        assertFalse("backlog" in summary.experimentalCapabilityIds)
    }

    @Test
    fun `System read exposes active remaining canonical lifecycle over legacy absence`() = runTest {
        val id = SystemContexts.INBOX.raw
        val fixture =
            fixture(
                contexts = listOf(context(id)),
                configurations = mapOf(id to ContextConfiguration.default(id).copy(enableAttachments = false)),
                states = mutableMapOf(id to systemState()),
                remainingStates =
                    mutableMapOf(
                        id to
                            remainingState(
                                connections = WorkspaceCapabilityState.ACTIVE,
                                inboxSorting = WorkspaceCapabilityState.ACTIVE,
                                keyProblems = WorkspaceCapabilityState.ACTIVE,
                            ),
                    ),
            )

        val summary = fixture.adapter.getContexts().single()

        assertTrue("connections" in summary.enabledCapabilityIds)
        assertTrue("inbox_sorting" in summary.experimentalCapabilityIds)
        assertTrue("key_problems" in summary.experimentalCapabilityIds)
        assertFalse("inbox_sorting" in summary.enabledCapabilityIds)
        assertFalse("key_problems" in summary.enabledCapabilityIds)
    }

    @Test
    fun `System read uses remaining canonical lifecycle without changing shared buckets`() = runTest {
        val id = SystemContexts.INBOX.raw
        val fixture =
            fixture(
                contexts = listOf(context(id, defaultView = ContextViewMode.KEY_PROBLEMS)),
                configurations =
                    mapOf(
                        id to
                            ContextConfiguration.default(id).copy(
                                enableAttachments = true,
                                experimentalCapabilityIds =
                                    listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                            ),
                    ),
                states = mutableMapOf(id to systemState()),
                remainingStates =
                    mutableMapOf(
                        id to
                            remainingState(
                                connections = WorkspaceCapabilityState.DISABLED,
                                inboxSorting = WorkspaceCapabilityState.ACTIVE,
                                keyProblems = WorkspaceCapabilityState.DISABLED,
                            ),
                    ),
            )

        val summary = fixture.adapter.getContexts().single()

        assertFalse("connections" in summary.enabledCapabilityIds)
        assertFalse("key_problems" in summary.enabledCapabilityIds)
        assertTrue("inbox_sorting" in summary.experimentalCapabilityIds)
        assertFalse("key_problems" in summary.experimentalCapabilityIds)
        assertFalse("inbox_sorting" in summary.enabledCapabilityIds)
    }

    @Test
    fun `malformed remaining System owner fails closed without hiding unrelated capabilities`() = runTest {
        val id = SystemContexts.INBOX.raw
        val fixture =
            fixture(
                contexts = listOf(context(id, defaultView = ContextViewMode.CONNECTIONS)),
                configurations =
                    mapOf(
                        id to
                            ContextConfiguration.default(id).copy(
                                enableAttachments = true,
                                experimentalCapabilityIds =
                                    listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                            ),
                    ),
                states = mutableMapOf(id to systemState()),
                remainingStates =
                    mutableMapOf(
                        id to remainingState(isCanonicalOwnerAvailable = false),
                    ),
                dashboardEnabled = true,
                executionLogEnabled = true,
            )

        val summary = fixture.adapter.getContexts().single()

        assertFalse("connections" in summary.enabledCapabilityIds)
        assertFalse("inbox_sorting" in summary.experimentalCapabilityIds)
        assertFalse("key_problems" in summary.experimentalCapabilityIds)
        assertTrue("dashboard" in summary.enabledCapabilityIds)
        assertTrue("log" in summary.enabledCapabilityIds)
    }

    @Test
    fun `System read uses canonical Inbox lifecycle over contradictory legacy flag`() = runTest {
        val disabledId = SystemContexts.INBOX.raw
        val activeId = SystemContexts.SESSION_IMPROVE.raw
        val fixture =
            fixture(
                contexts = listOf(context(disabledId), context(activeId)),
                configurations =
                    mapOf(
                        disabledId to ContextConfiguration.default(disabledId).copy(enableInbox = true),
                        activeId to ContextConfiguration.default(activeId).copy(enableInbox = false),
                    ),
                states =
                    mutableMapOf(
                        disabledId to systemState(inboxState = WorkspaceCapabilityState.DISABLED),
                        activeId to systemState(inboxState = WorkspaceCapabilityState.ACTIVE),
                    ),
            )

        val summaries = fixture.adapter.getContexts().associateBy { it.id }

        assertFalse("inbox" in summaries.getValue(disabledId).enabledCapabilityIds)
        assertTrue("inbox" in summaries.getValue(activeId).enabledCapabilityIds)
    }

    @Test
    fun `System Direction keeps shared representation while canonical lifecycle wins`() = runTest {
        val activeId = SystemContexts.INBOX.raw
        val disabledId = SystemContexts.SESSION_IMPROVE.raw
        val fixture =
            fixture(
                contexts =
                    listOf(
                        context(activeId),
                        context(disabledId, defaultView = ContextViewMode.DIRECTION),
                    ),
                configurations =
                    mapOf(
                        activeId to ContextConfiguration.default(activeId),
                        disabledId to
                            ContextConfiguration.default(disabledId).copy(
                                experimentalCapabilityIds = listOf(CapabilityId("direction")),
                            ),
                    ),
                states =
                    mutableMapOf(
                        activeId to systemState(directionState = WorkspaceCapabilityState.ACTIVE),
                        disabledId to systemState(directionState = WorkspaceCapabilityState.DISABLED),
                    ),
            )

        val summaries = fixture.adapter.getContexts().associateBy { it.id }
        val active = summaries.getValue(activeId)
        val disabled = summaries.getValue(disabledId)

        assertFalse("direction" in active.enabledCapabilityIds)
        assertTrue("direction" in active.experimentalCapabilityIds)
        assertFalse("direction" in disabled.enabledCapabilityIds)
        assertFalse("direction" in disabled.experimentalCapabilityIds)
    }

    @Test
    fun `malformed System owner cannot be resurrected by legacy flags or default view`() = runTest {
        val id = SystemContexts.INBOX.raw
        val fixture =
            fixture(
                contexts = listOf(context(id, defaultView = ContextViewMode.INBOX)),
                configurations =
                    mapOf(
                        id to
                            ContextConfiguration.default(id).copy(
                                enableInbox = true,
                                experimentalCapabilityIds = listOf(CapabilityId("direction")),
                            ),
                    ),
                states =
                    mutableMapOf(
                        id to SystemInboxDirectionState(null, null, isCanonicalOwnerAvailable = false),
                    ),
                dashboardEnabled = true,
                executionLogEnabled = true,
            )

        val summary = fixture.adapter.getContexts().single()

        assertFalse("inbox" in summary.enabledCapabilityIds)
        assertFalse("inbox" in summary.experimentalCapabilityIds)
        assertFalse("direction" in summary.enabledCapabilityIds)
        assertFalse("direction" in summary.experimentalCapabilityIds)
        assertTrue("dashboard" in summary.enabledCapabilityIds)
        assertTrue("log" in summary.enabledCapabilityIds)
    }

    @Test
    fun `System update routes complete request through canonical commands and returns canonical winner`() =
        runTest {
            val id = SystemContexts.INBOX.raw
            val states =
                mutableMapOf(
                    id to
                        systemState(
                            inboxState = WorkspaceCapabilityState.ACTIVE,
                            directionState = WorkspaceCapabilityState.ACTIVE,
                        ),
                )
            val fixture =
                fixture(
                    contexts = listOf(context(id)),
                    configurations =
                        mapOf(
                            id to
                                ContextConfiguration.default(id).copy(
                                    enableInbox = true,
                                    experimentalCapabilityIds = listOf(CapabilityId("direction")),
                                ),
                        ),
                    states = states,
                    remainingStates =
                        mutableMapOf(
                            id to remainingState(
                                connections = WorkspaceCapabilityState.ACTIVE,
                                inboxSorting = WorkspaceCapabilityState.ACTIVE,
                                keyProblems = WorkspaceCapabilityState.ACTIVE,
                            ),
                        ),
                    backlogStates =
                        mutableMapOf(id to backlogState(WorkspaceCapabilityState.DISABLED)),
                    persistedTransform = { candidate ->
                        candidate.copy(
                            enableAttachments = true,
                            experimentalCapabilityIds =
                                candidate.experimentalCapabilityIds +
                                    listOf(CapabilityId("inbox_sorting"), CapabilityId("key_problems")),
                        )
                    },
                )

            val summary =
                requireNotNull(
                    fixture.adapter.updateContext(
                        contextId = id,
                        name = "Updated",
                        description = null,
                        status = SharedContextStatus.NoPlan,
                        defaultView = SharedContextView.Backlog,
                        enabledCapabilityIds = listOf("backlog"),
                        experimentalCapabilityIds = emptyList(),
                    ),
                )

            coVerify(exactly = 1) { fixture.access.setInboxEnabled(id, false, any()) }
            coVerify(exactly = 1) { fixture.access.setDirectionEnabled(id, false, any()) }
            coVerify(exactly = 1) { fixture.remainingAccess.setConnectionsEnabled(id, false, any()) }
            coVerify(exactly = 1) { fixture.remainingAccess.setInboxSortingEnabled(id, false, any()) }
            coVerify(exactly = 1) { fixture.remainingAccess.setKeyProblemsEnabled(id, false, any()) }
            coVerify(exactly = 1) { fixture.backlogAccess.setEnabled(id, true, any()) }
            coVerify(exactly = 0) {
                fixture.contextRepository.updateContextSharedState(any(), any())
            }
            assertFalse("inbox" in summary.enabledCapabilityIds)
            assertFalse("direction" in summary.enabledCapabilityIds)
            assertFalse("direction" in summary.experimentalCapabilityIds)
            assertFalse("connections" in summary.enabledCapabilityIds)
            assertFalse("inbox_sorting" in summary.experimentalCapabilityIds)
            assertFalse("key_problems" in summary.experimentalCapabilityIds)
            assertTrue("backlog" in summary.enabledCapabilityIds)
            coVerify(exactly = 0) {
                fixture.structureRepository.upsertStructure(any())
            }
            assertTrue(fixture.configurations.getValue(id).enableInbox == true)
            assertTrue(
                CapabilityId("direction") in
                    fixture.configurations.getValue(id).experimentalCapabilityIds,
            )
        }

    @Test
    fun `System update fails closed before legacy write when canonical owner is unavailable`() = runTest {
        val id = SystemContexts.INBOX.raw
        val original = ContextConfiguration.default(id).copy(enableInbox = true)
        val fixture =
            fixture(
                contexts = listOf(context(id)),
                configurations = mapOf(id to original),
                states =
                    mutableMapOf(
                        id to SystemInboxDirectionState(null, null, isCanonicalOwnerAvailable = false),
                    ),
                canonicalCommandFailure = IllegalStateException("Canonical System Workspace unavailable"),
            )

        val result =
            runCatching {
                fixture.adapter.updateContext(
                    contextId = id,
                    name = "Must not persist",
                    description = null,
                    status = SharedContextStatus.NoPlan,
                    defaultView = SharedContextView.Inbox,
                    enabledCapabilityIds = listOf("inbox"),
                    experimentalCapabilityIds = listOf("direction"),
                )
            }

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { fixture.structureRepository.upsertStructure(any()) }
        assertTrue(fixture.configurations.getValue(id) == original)
    }

    @Test
    fun `failed System Backlog command cannot fall through to legacy authority`() = runTest {
        val id = SystemContexts.INBOX.raw
        val original = ContextConfiguration.default(id).copy(enableBacklog = false)
        val fixture =
            fixture(
                contexts = listOf(context(id)),
                configurations = mapOf(id to original),
                states = mutableMapOf(id to systemState()),
                remainingStates = mutableMapOf(id to remainingState()),
                backlogStates = mutableMapOf(id to backlogState(WorkspaceCapabilityState.DISABLED)),
                backlogCommandFailure = IllegalStateException("Canonical System BACKLOG rejected"),
            )

        val result =
            runCatching {
                fixture.adapter.updateContext(
                    contextId = id,
                    name = "Must not persist",
                    description = null,
                    status = SharedContextStatus.NoPlan,
                    defaultView = SharedContextView.Backlog,
                    enabledCapabilityIds = listOf("backlog"),
                    experimentalCapabilityIds = emptyList(),
                )
            }

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { fixture.backlogAccess.setEnabled(id, true, any()) }
        coVerify(exactly = 0) { fixture.structureRepository.upsertStructure(any()) }
        assertTrue(fixture.configurations.getValue(id) == original)
    }

    @Test
    fun `restored canonical Workspace update ignores deleted Context tombstone and legacy configuration`() =
        runTest {
            val id = "ordinary-restored"
            val tombstone = context(id).copy(isDeleted = true)
            val canonical =
                CanonicalWorkspacePresentation(
                    id = id,
                    nameOverride = "Restored Workspace",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 7L,
                    isDeleted = false,
                )
            val legacyConfiguration =
                ContextConfiguration.default(id).copy(
                    enableInbox = true,
                    enableBacklog = false,
                    enableAttachments = true,
                    experimentalCapabilityIds = listOf(CapabilityId("direction")),
                )
            val fixture =
                fixture(
                    contexts = listOf(tombstone),
                    configurations = mapOf(id to legacyConfiguration),
                    states = mutableMapOf(),
                    canonicalPresentations = mapOf(id to canonical),
                )

            every { fixture.canonicalBacklogRepository.getState(id, any()) } returns
                BacklogCapabilityState(
                    lifecycleState = WorkspaceCapabilityState.ACTIVE,
                    isDeleted = false,
                    configuration = BacklogCapabilityConfigurationV2(false),
                )

            val initial = fixture.adapter.getContexts().single()

            assertEquals(id, initial.id)
            assertTrue("backlog" in initial.enabledCapabilityIds)
            assertFalse("inbox" in initial.enabledCapabilityIds)
            assertFalse("connections" in initial.enabledCapabilityIds)
            assertFalse("direction" in initial.experimentalCapabilityIds)

            val updated =
                requireNotNull(
                    fixture.adapter.updateContext(
                        contextId = id,
                        name = "Canonical updated",
                        description = "Canonical description",
                        status = SharedContextStatus.Completed,
                        defaultView = SharedContextView.Connections,
                        enabledCapabilityIds = listOf("backlog"),
                        experimentalCapabilityIds = emptyList(),
                    ),
                )

            assertTrue("backlog" in updated.enabledCapabilityIds)

            coVerify(exactly = 1) {
                fixture.canonicalWorkspaceRepository.updateNameAndDescription(
                    id,
                    "Canonical updated",
                    "Canonical description",
                    any(),
                )
            }
            coVerify(exactly = 1) {
                fixture.canonicalBacklogRepository.setEnabled(id, true, any())
            }
            coVerify(exactly = 1) {
                fixture.canonicalConnectionsRepository.setEnabled(id, false, any())
            }

            coVerify(exactly = 0) {
                fixture.contextRepository.updateContextSharedState(any(), any())
            }
            coVerify(exactly = 0) {
                fixture.structureRepository.upsertStructure(any())
            }

            assertEquals(legacyConfiguration, fixture.configurations.getValue(id))
        }

    private fun fixture(
        contexts: List<Context>,
        configurations: Map<String, ContextConfiguration>,
        states: MutableMap<String, SystemInboxDirectionState>,
        remainingStates: MutableMap<String, SystemRemainingCapabilityLifecycleState> = mutableMapOf(),
        backlogStates: MutableMap<String, SystemBacklogLifecycleState> = mutableMapOf(),
        persistedTransform: (ContextConfiguration) -> ContextConfiguration = { it },
        dashboardEnabled: Boolean = false,
        executionLogEnabled: Boolean = false,
        canonicalCommandFailure: Throwable? = null,
        backlogCommandFailure: Throwable? = null,
        canonicalPresentations: Map<String, CanonicalWorkspacePresentation> = emptyMap(),
    ): Fixture {
        val contextsById = contexts.associateBy { it.id }.toMutableMap()
        val configurationsById = configurations.toMutableMap()
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        every { contextRepository.getAllContextsFlow() } returns flowOf(contexts)
        coEvery { contextRepository.getContextById(any()) } answers {
            contextsById[firstArg<String>()]
        }
        val structureRepository = mockk<ContextStructureRepository>()
        coEvery { structureRepository.getStructureByContext(any()) } answers {
            configurationsById[firstArg<String>()]
        }
        coEvery { structureRepository.upsertStructure(any()) } answers {
            val persisted = persistedTransform(firstArg())
            configurationsById[persisted.contextId] = persisted
        }

        val access = mockk<SystemContextCanonicalInboxDirectionAccess>()
        every { access.handles(any()) } answers {
            firstArg<String>() in states || firstArg<String>().startsWith("sys_")
        }
        coEvery { access.getState(any()) } answers { states[firstArg<String>()] }
        coEvery { access.setInboxEnabled(any(), any(), any()) } answers {
            canonicalCommandFailure?.let { throw it }
            val id = firstArg<String>()
            val enabled = secondArg<Boolean>()
            val current = states.getValue(id)
            states[id] = current.copy(inbox = inboxState(enabled.toLifecycleState()))
            true
        }
        coEvery { access.setDirectionEnabled(any(), any(), any()) } answers {
            canonicalCommandFailure?.let { throw it }
            val id = firstArg<String>()
            val enabled = secondArg<Boolean>()
            val current = states.getValue(id)
            states[id] = current.copy(direction = directionState(enabled.toLifecycleState()))
            true
        }
        val remainingAccess = mockk<SystemContextCanonicalRemainingCapabilityLifecycleAccess>()
        every { remainingAccess.handles(any()) } answers {
            firstArg<String>() in remainingStates || firstArg<String>().startsWith("sys_")
        }
        coEvery { remainingAccess.getState(any()) } answers { remainingStates[firstArg<String>()] }
        coEvery { remainingAccess.setConnectionsEnabled(any(), any(), any()) } answers {
            updateRemainingState(remainingStates, firstArg(), RemainingCapability.CONNECTIONS, secondArg())
            true
        }
        coEvery { remainingAccess.setInboxSortingEnabled(any(), any(), any()) } answers {
            updateRemainingState(remainingStates, firstArg(), RemainingCapability.INBOX_SORTING, secondArg())
            true
        }
        coEvery { remainingAccess.setKeyProblemsEnabled(any(), any(), any()) } answers {
            updateRemainingState(remainingStates, firstArg(), RemainingCapability.KEY_PROBLEMS, secondArg())
            true
        }
        val backlogAccess = mockk<SystemContextCanonicalBacklogLifecycleAccess>()
        every { backlogAccess.handles(any()) } answers {
            firstArg<String>() in backlogStates || firstArg<String>().startsWith("sys_")
        }
        coEvery { backlogAccess.getState(any()) } answers { backlogStates[firstArg<String>()] }
        coEvery { backlogAccess.setEnabled(any(), any(), any()) } answers {
            backlogCommandFailure?.let { throw it }
            val id = firstArg<String>()
            val enabled = secondArg<Boolean>()
            backlogStates[id] = backlogState(enabled.toLifecycleState())
            true
        }

        val bootstrapper = mockk<CanonicalWorkspaceBootstrapper>()
        coEvery { bootstrapper.ensureBootstrapped(any()) } returns
            WorkspaceBootstrapReport(0, 0, emptyList(), performed = false)

        val canonicalWorkspaceRepository =
            mockk<CanonicalWorkspaceRepository>(relaxed = true)
        coEvery { canonicalWorkspaceRepository.getCanonicalPresentation(any()) } answers {
            canonicalPresentations[firstArg<String>()]
        }
        coEvery { canonicalWorkspaceRepository.getCanonicalPresentations() } returns
            canonicalPresentations

        val canonicalCapabilityInstanceStore =
            mockk<CanonicalCapabilityInstanceStore>(relaxed = true)
        coEvery { canonicalCapabilityInstanceStore.loadReadSnapshot() } returns
            CanonicalCapabilityReadSnapshot(
                workspacesById = emptyMap(),
                instances = emptyList(),
            )

        val canonicalBacklogRepository =
            mockk<CanonicalBacklogRepository>(relaxed = true)
        val canonicalInboxRepository =
            mockk<CanonicalInboxRepository>(relaxed = true)
        val canonicalDirectionRepository =
            mockk<CanonicalDirectionRepository>(relaxed = true)
        val canonicalConnectionsRepository =
            mockk<CanonicalConnectionsRepository>(relaxed = true)
        val canonicalInboxSortingRepository =
            mockk<CanonicalInboxSortingRepository>(relaxed = true)
        val canonicalKeyProblemsRepository =
            mockk<CanonicalKeyProblemsRepository>(relaxed = true)

        coEvery { canonicalBacklogRepository.getState(any()) } returns null
        every { canonicalBacklogRepository.getState(any(), any()) } returns null
        coEvery { canonicalInboxRepository.getState(any()) } returns null
        every { canonicalInboxRepository.getState(any(), any()) } returns null
        coEvery { canonicalDirectionRepository.getState(any()) } returns null
        every { canonicalDirectionRepository.getState(any(), any()) } returns null
        coEvery { canonicalConnectionsRepository.getState(any()) } returns null
        every { canonicalConnectionsRepository.getState(any(), any()) } returns null
        coEvery { canonicalInboxSortingRepository.getState(any()) } returns null
        every { canonicalInboxSortingRepository.getState(any(), any()) } returns null
        coEvery { canonicalKeyProblemsRepository.getState(any()) } returns null
        every { canonicalKeyProblemsRepository.getState(any(), any()) } returns null
        val presentationProjector = mockk<SystemWorkspacePresentationContextProjector>()

        fun presentation(context: Context?): ContextPresentation? {
            if (context == null) return null
            val canonical = canonicalPresentations[context.id]
            return ContextPresentation(
                id = context.id,
                name = canonical?.nameOverride ?: context.name,
                description = canonical?.descriptionOverride ?: context.description,
                parentId = canonical?.parentWorkspaceId ?: context.parentId,
                roleCode = canonical?.roleCode ?: context.roleCode,
                order = canonical?.workspaceOrder ?: context.order,
                tags = context.tags,
            )
        }

        coEvery { presentationProjector.projectPresentationUniverse(any()) } answers {
            val raw = firstArg<List<Context>>()
            val rawIds = raw.mapTo(hashSetOf()) { it.id }
            buildList {
                raw.mapNotNullTo(this) { presentation(it) }
                canonicalPresentations
                    .filterKeys { it !in rawIds }
                    .values
                    .mapTo(this) { canonical ->
                        ContextPresentation(
                            id = canonical.id,
                            name = requireNotNull(canonical.nameOverride),
                            description = canonical.descriptionOverride,
                            parentId = canonical.parentWorkspaceId,
                            roleCode = canonical.roleCode,
                            order = canonical.workspaceOrder,
                            tags = emptyList(),
                        )
                    }
            }
        }
        coEvery { presentationProjector.resolvePresentation(any(), any()) } answers {
            val id = firstArg<String>()
            val raw = secondArg<Context?>()
            raw?.let(::presentation)
                ?: canonicalPresentations[id]?.let { canonical ->
                    ContextPresentation(
                        id = canonical.id,
                        name = requireNotNull(canonical.nameOverride),
                        description = canonical.descriptionOverride,
                        parentId = canonical.parentWorkspaceId,
                        roleCode = canonical.roleCode,
                        order = canonical.workspaceOrder,
                        tags = emptyList(),
                    )
                }
                ?: contexts.firstOrNull { it.id == id }?.let(::presentation)
        }
        coEvery {
            contextRepository.updateContextPresentation(
                any<String>(),
                any<String>(),
                any(),
            )
        } just Runs
        val dashboard = mockk<CanonicalDashboardCapabilityRepository>(relaxed = true)
        val executionLog = mockk<CanonicalExecutionLogRepository>(relaxed = true)
        coEvery { dashboard.isEnabled(any()) } returns dashboardEnabled
        coEvery { executionLog.isEnabled(any()) } returns executionLogEnabled

        return Fixture(
            adapter =
                AndroidWorkspaceRepositoryAdapter(
                    contextRepository = contextRepository,
                    goalRepository = mockk<GoalRepository>(relaxed = true),
                    contextStructureRepository = structureRepository,
                    canonicalWorkspaceBootstrapper = bootstrapper,
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    canonicalCapabilityInstanceStore = canonicalCapabilityInstanceStore,
                    systemWorkspacePresentationContextProjector = presentationProjector,
                    canonicalBacklogRepository = canonicalBacklogRepository,
                    canonicalInboxRepository = canonicalInboxRepository,
                    canonicalDirectionRepository = canonicalDirectionRepository,
                    canonicalConnectionsRepository = canonicalConnectionsRepository,
                    canonicalInboxSortingRepository = canonicalInboxSortingRepository,
                    canonicalKeyProblemsRepository = canonicalKeyProblemsRepository,
                    canonicalDashboardCapabilityRepository = dashboard,
                    canonicalExecutionLogRepository = executionLog,
                    systemInboxDirectionAccess = access,
                    systemRemainingCapabilityAccess = remainingAccess,
                    systemBacklogLifecycleAccess = backlogAccess,
                ),
            contextRepository = contextRepository,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            canonicalBacklogRepository = canonicalBacklogRepository,
            canonicalConnectionsRepository = canonicalConnectionsRepository,
            access = access,
            remainingAccess = remainingAccess,
            backlogAccess = backlogAccess,
            structureRepository = structureRepository,
            configurations = configurationsById,
        )
    }

    private fun context(
        id: String,
        defaultView: ContextViewMode = ContextViewMode.BACKLOG,
    ) =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
            defaultViewModeName = defaultView.name,
        )

    private fun systemState(
        inboxState: WorkspaceCapabilityState? = null,
        directionState: WorkspaceCapabilityState? = null,
    ) =
        SystemInboxDirectionState(
            inbox = inboxState?.let(::inboxState),
            direction = directionState?.let(::directionState),
        )

    private fun inboxState(state: WorkspaceCapabilityState) =
        InboxCapabilityState(
            lifecycleState = state,
            isDeleted = false,
            configuration = InboxCapabilityConfigurationV1(InboxOwnerVisibility.KEEP_VISIBLE),
        )

    private fun directionState(state: WorkspaceCapabilityState) =
        DirectionCapabilityState(
            lifecycleState = state,
            isDeleted = false,
            configuration = DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = true),
        )

    private fun Boolean.toLifecycleState() =
        if (this) WorkspaceCapabilityState.ACTIVE else WorkspaceCapabilityState.DISABLED

    private fun remainingState(
        connections: WorkspaceCapabilityState? = null,
        inboxSorting: WorkspaceCapabilityState? = null,
        keyProblems: WorkspaceCapabilityState? = null,
        isCanonicalOwnerAvailable: Boolean = true,
    ) =
        SystemRemainingCapabilityLifecycleState(
            connections = connections?.let(::connectionsState),
            inboxSorting = inboxSorting?.let(::inboxSortingState),
            keyProblems = keyProblems?.let(::keyProblemsState),
            connectionsEstablished = connections != null || !isCanonicalOwnerAvailable,
            inboxSortingEstablished = inboxSorting != null || !isCanonicalOwnerAvailable,
            keyProblemsEstablished = keyProblems != null || !isCanonicalOwnerAvailable,
            isCanonicalOwnerAvailable = isCanonicalOwnerAvailable,
        )

    private fun connectionsState(state: WorkspaceCapabilityState) =
        ConnectionsCapabilityState(state, false, ConnectionsCapabilityConfigurationV1)

    private fun inboxSortingState(state: WorkspaceCapabilityState) =
        InboxSortingCapabilityState(state, false, InboxSortingCapabilityConfigurationV1(emptyList()))

    private fun keyProblemsState(state: WorkspaceCapabilityState) =
        KeyProblemsCapabilityState(state, false, KeyProblemsCapabilityConfigurationV1)

    private fun backlogState(
        state: WorkspaceCapabilityState,
        isDeleted: Boolean = false,
    ) =
        SystemBacklogLifecycleState(
            backlog =
                BacklogCapabilityState(
                    lifecycleState = state,
                    isDeleted = isDeleted,
                    configuration = BacklogCapabilityConfigurationV2(false),
                ),
            isEstablished = true,
        )

    private fun updateRemainingState(
        states: MutableMap<String, SystemRemainingCapabilityLifecycleState>,
        id: String,
        capability: RemainingCapability,
        enabled: Boolean,
    ) {
        val current = states.getValue(id)
        val lifecycle = enabled.toLifecycleState()
        states[id] =
            when (capability) {
                RemainingCapability.CONNECTIONS ->
                    current.copy(connections = connectionsState(lifecycle), connectionsEstablished = true)
                RemainingCapability.INBOX_SORTING ->
                    current.copy(inboxSorting = inboxSortingState(lifecycle), inboxSortingEstablished = true)
                RemainingCapability.KEY_PROBLEMS ->
                    current.copy(keyProblems = keyProblemsState(lifecycle), keyProblemsEstablished = true)
            }
    }

    private enum class RemainingCapability { CONNECTIONS, INBOX_SORTING, KEY_PROBLEMS }

    private data class Fixture(
        val adapter: AndroidWorkspaceRepositoryAdapter,
        val contextRepository: ContextRepository,
        val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
        val canonicalBacklogRepository: CanonicalBacklogRepository,
        val canonicalConnectionsRepository: CanonicalConnectionsRepository,
        val access: SystemContextCanonicalInboxDirectionAccess,
        val remainingAccess: SystemContextCanonicalRemainingCapabilityLifecycleAccess,
        val backlogAccess: SystemContextCanonicalBacklogLifecycleAccess,
        val structureRepository: ContextStructureRepository,
        val configurations: MutableMap<String, ContextConfiguration>,
    )
}
