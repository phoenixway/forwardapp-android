package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsRegistry
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspacePresentation
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemRemainingCapabilityLifecycleState
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCapabilityState
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.InboxCapabilityState
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextSettingsViewModelSystemCapabilityFailureTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `System settings presentation uses canonical Workspace over stale Context shell`() =
        runTest(dispatcher) {
            val id = SystemContexts.INBOX.raw
            val fixture =
                fixture(
                    canonical = SystemInboxDirectionState(null, null),
                    canonicalPresentation =
                        CanonicalWorkspacePresentation(
                            id = id,
                            nameOverride = "Canonical Inbox",
                            descriptionOverride = "Canonical description",
                            parentWorkspaceId = "canonical-parent",
                            roleCode = "canonical-role",
                            workspaceOrder = 7L,
                            isDeleted = false,
                        ),
                )

            advanceUntilIdle()

            assertEquals("Canonical Inbox", fixture.viewModel.uiState.value.title.text)
            assertEquals("Canonical description", fixture.viewModel.uiState.value.description.text)
            val option =
                fixture.viewModel.uiState.value.availableContexts.single { it.id == id }
            assertEquals("Canonical Inbox", option.name)
            assertEquals("canonical-parent", option.parentId)
        }

    @Test
    fun `malformed System owner loads fail-closed without calling other canonical repositories`() =
        runTest(dispatcher) {
            val fixture = fixture(SystemInboxDirectionState(null, null, isCanonicalOwnerAvailable = false))

            advanceUntilIdle()

            assertFalse(CapabilityId("inbox") in fixture.viewModel.uiState.value.enabledCapabilityIds)
            assertFalse(CapabilityId("direction") in fixture.viewModel.uiState.value.enabledCapabilityIds)
            coVerify(exactly = 0) { fixture.dashboard.isEnabled(any()) }
            coVerify(exactly = 0) { fixture.executionLog.isEnabled(any()) }
        }

    @Test
    fun `failed explicit System Inbox toggle reloads canonical visible state`() =
        runTest(dispatcher) {
            val canonical =
                SystemInboxDirectionState(
                    inbox =
                        InboxCapabilityState(
                            lifecycleState = WorkspaceCapabilityState.ACTIVE,
                            isDeleted = false,
                            configuration =
                                InboxCapabilityConfigurationV1(InboxOwnerVisibility.KEEP_VISIBLE),
                        ),
                    direction = null,
                )
            val fixture = fixture(canonical)
            coEvery {
                fixture.access.setInboxEnabled(SystemContexts.INBOX.raw, false, any())
            } throws IllegalStateException("canonical write rejected")
            advanceUntilIdle()

            fixture.viewModel.onToggleFeature("Inbox", enabled = false)
            advanceUntilIdle()

            assertTrue(CapabilityId("inbox") in fixture.viewModel.uiState.value.enabledCapabilityIds)
        }

    @Test
    fun `failed explicit System Connections toggle reloads canonical visible state`() =
        runTest(dispatcher) {
            val fixture = fixture(SystemInboxDirectionState(null, null))
            coEvery {
                fixture.remainingAccess.setConnectionsEnabled(SystemContexts.INBOX.raw, false, any())
            } throws IllegalStateException("canonical write rejected")
            coEvery { fixture.remainingAccess.getState(SystemContexts.INBOX.raw) } returns
                missingRemainingState().copy(
                    connections =
                        com.romankozak.forwardappmobile.data.workspace.capability.ConnectionsCapabilityState(
                            WorkspaceCapabilityState.ACTIVE,
                            false,
                            com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationV1,
                        ),
                    connectionsEstablished = true,
                )
            advanceUntilIdle()

            fixture.viewModel.onToggleFeature("Connections", enabled = false)
            advanceUntilIdle()

            assertTrue(CapabilityId("connections") in fixture.viewModel.uiState.value.enabledCapabilityIds)
        }

    @Test
    fun `System Backlog toggle routes through canonical lifecycle command`() =
        runTest(dispatcher) {
            val fixture = fixture(SystemInboxDirectionState(null, null))
            advanceUntilIdle()

            fixture.viewModel.onToggleFeature("Backlog", enabled = false)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                fixture.backlogAccess.setEnabled(SystemContexts.INBOX.raw, false, any())
            }
        }

    @Test
    fun `failed System Backlog toggle reloads canonical winner`() =
        runTest(dispatcher) {
            val fixture =
                fixture(
                    canonical = SystemInboxDirectionState(null, null),
                    backlogState = backlogState(WorkspaceCapabilityState.ACTIVE),
                )
            coEvery {
                fixture.backlogAccess.setEnabled(SystemContexts.INBOX.raw, false, any())
            } throws IllegalStateException("canonical write rejected")
            advanceUntilIdle()

            fixture.viewModel.onToggleFeature("Backlog", enabled = false)
            advanceUntilIdle()

            assertTrue(CapabilityId("backlog") in fixture.viewModel.uiState.value.enabledCapabilityIds)
            coVerify(exactly = 0) {
                fixture.structureRepository.updateStructure(match { it.enableBacklog == false })
            }
        }

    @Test
    fun `System save does not persist legacy Project Management state`() =
        runTest(dispatcher) {
            listOf(false to true, true to false).forEach { (historical, projectManagement) ->
                val fixture =
                    fixture(
                        canonical = SystemInboxDirectionState(null, null),
                        historicalEnableAdvanced = historical,
                    )
                advanceUntilIdle()

                fixture.viewModel.onProjectManagementChange(projectManagement)
                fixture.viewModel.onSave()
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    fixture.contextRepository.updateContextPresentation(
                        contextId = SystemContexts.INBOX.raw,
                        name = any(),
                        description = any(),
                    )
                }
                coVerify(exactly = 0) {
                    fixture.contextRepository.updateContextSettings(any(), any())
                }
                coVerify(exactly = 0) {
                    fixture.structureRepository.updateStructure(any())
                }
            }
        }

    @Test
    fun `System preset delegates once without second structure or lifecycle writes`() =
        runTest(dispatcher) {
            val fixture = fixture(SystemInboxDirectionState(null, null))
            advanceUntilIdle()

            fixture.viewModel.onApplyPreset("crisis_case")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                fixture.presetService.applyPresetToContext(SystemContexts.INBOX.raw, "crisis_case")
            }
            coVerify(exactly = 0) { fixture.structureRepository.updateStructure(any()) }
            coVerify(exactly = 0) { fixture.dashboard.setEnabled(any(), any(), any()) }
            coVerify(exactly = 0) { fixture.executionLog.setEnabled(any(), any(), any()) }
        }

    @Test
    fun `ordinary canonical save writes typed capabilities without resurrecting ContextStructure`() =
        runTest(dispatcher) {
            val id = "ordinary-canonical"
            val fixture =
                fixture(
                    canonical = SystemInboxDirectionState(null, null),
                    id = id,
                    canonicalOrdinaryOwner = true,
                )
            advanceUntilIdle()

            fixture.viewModel.onToggleFeature("Backlog", enabled = true)
            fixture.viewModel.onToggleFeature("Inbox", enabled = true)
            fixture.viewModel.onToggleFeature("Connections", enabled = true)
            fixture.viewModel.onToggleFeature("Directions", enabled = true)
            fixture.viewModel.onToggleFeature("Issues", enabled = true)
            fixture.viewModel.onSave()
            advanceUntilIdle()

            coVerify(exactly = 1) { fixture.canonicalBacklog.setEnabled(id, true, any()) }
            coVerify(exactly = 1) { fixture.canonicalInbox.setEnabled(id, true, any()) }
            coVerify(exactly = 1) { fixture.canonicalConnections.setEnabled(id, true, any()) }
            coVerify(exactly = 1) { fixture.canonicalDirection.setEnabled(id, true, any()) }
            coVerify(exactly = 1) { fixture.canonicalKeyProblems.setEnabled(id, true, any()) }
            coVerify(exactly = 0) { fixture.structureRepository.ensureStructure(id) }
            coVerify(exactly = 0) { fixture.structureRepository.updateStructure(any()) }
        }

    private fun fixture(
        canonical: SystemInboxDirectionState,
        historicalEnableAdvanced: Boolean? = null,
        backlogState: SystemBacklogLifecycleState = SystemBacklogLifecycleState(null, false),
        canonicalPresentation: CanonicalWorkspacePresentation? = null,
        id: String = SystemContexts.INBOX.raw,
        canonicalOrdinaryOwner: Boolean = false,
    ): Fixture {
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        val context =
            Context(
                id = id,
                name = "Inbox",
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 1L,
            )
        every { contextRepository.getAllContextsFlow() } returns flowOf(listOf(context))
        coEvery { contextRepository.getContextById(id) } returns context

        val structureRepository = mockk<ContextStructureRepository>(relaxed = true)
        val structure =
            ContextConfiguration.default(id).copy(
                enableInbox = true,
                enableAdvanced = historicalEnableAdvanced,
                experimentalCapabilityIds = listOf(CapabilityId("direction")),
            )
        coEvery { structureRepository.getStructureByContext(id) } returns structure
        coEvery { structureRepository.ensureStructure(id) } returns structure
        val structurePresetDao = mockk<StructurePresetDao>(relaxed = true)
        every { structurePresetDao.getAll() } returns flowOf(emptyList())
        val reminderRepository = mockk<ReminderRepository>(relaxed = true)
        every { reminderRepository.getRemindersForEntityFlow(id) } returns flowOf(emptyList())
        val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)
        every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(emptyList())
        val access = mockk<SystemContextCanonicalInboxDirectionAccess>(relaxed = true)
        every { access.handles(id) } returns SystemContexts.isSystem(ContextId(id))
        coEvery { access.getState(id) } returns canonical
        val remainingAccess = mockk<SystemContextCanonicalRemainingCapabilityLifecycleAccess>(relaxed = true)
        every { remainingAccess.handles(id) } returns
            SystemContexts.isSystem(ContextId(id))
        coEvery { remainingAccess.getState(id) } returns missingRemainingState()
        val dashboard = mockk<CanonicalDashboardCapabilityRepository>(relaxed = true)
        val executionLog = mockk<CanonicalExecutionLogRepository>(relaxed = true)
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)
        val canonicalBacklog = mockk<CanonicalBacklogRepository>(relaxed = true)
        val canonicalInbox = mockk<CanonicalInboxRepository>(relaxed = true)
        val canonicalDirection = mockk<CanonicalDirectionRepository>(relaxed = true)
        val canonicalConnections = mockk<CanonicalConnectionsRepository>(relaxed = true)
        val canonicalInboxSorting = mockk<CanonicalInboxSortingRepository>(relaxed = true)
        val canonicalKeyProblems = mockk<CanonicalKeyProblemsRepository>(relaxed = true)
        coEvery { canonicalWorkspaceRepository.getCanonicalPresentation(id) } returns
            if (canonicalOrdinaryOwner) {
                CanonicalWorkspacePresentation(
                    id = id,
                    nameOverride = "Canonical ordinary",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    isDeleted = false,
                )
            } else {
                canonicalPresentation
            }
        val canonicalOrdinaryCapabilitySettings =
            CanonicalOrdinaryCapabilitySettings(
                workspaceRepository = canonicalWorkspaceRepository,
                backlogRepository = canonicalBacklog,
                inboxRepository = canonicalInbox,
                directionRepository = canonicalDirection,
                connectionsRepository = canonicalConnections,
                inboxSortingRepository = canonicalInboxSorting,
                keyProblemsRepository = canonicalKeyProblems,
                dashboardRepository = dashboard,
                executionLogRepository = executionLog,
            )
        val backlogAccess = mockk<SystemContextCanonicalBacklogLifecycleAccess>(relaxed = true)
        every { backlogAccess.handles(id) } returns
            SystemContexts.isSystem(ContextId(id))
        coEvery { backlogAccess.getState(id) } returns backlogState
        val presetService = mockk<StructurePresetService>(relaxed = true)
        val presentationProjector = mockk<SystemWorkspacePresentationContextProjector>()
        val presentedContext =
            canonicalPresentation?.let { presentation ->
                context.copy(
                    name = requireNotNull(presentation.nameOverride),
                    description = presentation.descriptionOverride,
                    parentId = presentation.parentWorkspaceId,
                    roleCode = presentation.roleCode,
                    order = presentation.workspaceOrder,
                )
            } ?: context
        val presented =
            ContextPresentation(
                id = presentedContext.id,
                name = presentedContext.name,
                description = presentedContext.description,
                parentId = presentedContext.parentId,
                roleCode = presentedContext.roleCode,
                order = presentedContext.order,
                tags = presentedContext.tags,
            )
        coEvery { presentationProjector.resolvePresentation(id, any()) } returns presented
        every { presentationProjector.observePresentationUniverse(any()) } returns flowOf(listOf(presented))

        val viewModel =
            ContextSettingsViewModel(
                contextRepository = contextRepository,
                reminderRepository = reminderRepository,
                savedStateHandle = SavedStateHandle(mapOf("projectId" to id)),
                structurePresetDao = structurePresetDao,
                contextStructureRepository = structureRepository,
                structurePresetService = presetService,
                systemWorkspacePresentationContextProjector = presentationProjector,
                systemWorkspaceTagAuthority = mockk(relaxed = true),
                capabilityRegistry = mockk<CapabilityRegistry>(relaxed = true),
                contextCapabilitiesResolver = ContextCapabilitiesResolver(),
                capabilitySettingsRegistry = mockk<CapabilitySettingsRegistry>(relaxed = true),
                attachmentsRepository = attachmentsRepository,
                noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                canonicalOrdinaryCapabilitySettings = canonicalOrdinaryCapabilitySettings,
                canonicalDashboardCapabilityRepository = dashboard,
                canonicalExecutionLogRepository = executionLog,
                systemCapabilityAccess = access,
                systemRemainingCapabilityAccess = remainingAccess,
                systemBacklogLifecycleAccess = backlogAccess,
            )
        return Fixture(
            viewModel,
            contextRepository,
            structureRepository,
            access,
            remainingAccess,
            dashboard,
            executionLog,
            backlogAccess,
            presetService,
            canonicalBacklog,
            canonicalInbox,
            canonicalDirection,
            canonicalConnections,
            canonicalKeyProblems,
        )
    }

    private fun missingRemainingState() =
        SystemRemainingCapabilityLifecycleState(
            connections = null,
            inboxSorting = null,
            keyProblems = null,
            connectionsEstablished = false,
            inboxSortingEstablished = false,
            keyProblemsEstablished = false,
        )

    private data class Fixture(
        val viewModel: ContextSettingsViewModel,
        val contextRepository: ContextRepository,
        val structureRepository: ContextStructureRepository,
        val access: SystemContextCanonicalInboxDirectionAccess,
        val remainingAccess: SystemContextCanonicalRemainingCapabilityLifecycleAccess,
        val dashboard: CanonicalDashboardCapabilityRepository,
        val executionLog: CanonicalExecutionLogRepository,
        val backlogAccess: SystemContextCanonicalBacklogLifecycleAccess,
        val presetService: StructurePresetService,
        val canonicalBacklog: CanonicalBacklogRepository,
        val canonicalInbox: CanonicalInboxRepository,
        val canonicalDirection: CanonicalDirectionRepository,
        val canonicalConnections: CanonicalConnectionsRepository,
        val canonicalKeyProblems: CanonicalKeyProblemsRepository,
    )

    private fun backlogState(state: WorkspaceCapabilityState) =
        SystemBacklogLifecycleState(
            backlog =
                BacklogCapabilityState(
                    lifecycleState = state,
                    isDeleted = false,
                    configuration =
                        com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2(
                            false,
                        ),
                ),
            isEstablished = true,
        )
}
