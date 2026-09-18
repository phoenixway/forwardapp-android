package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemShellFreeMainScreenViewModelsTest {
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
    fun `Core Level can tag canonical Main Beacons without Context shell`() =
        runTest(dispatcher) {
            val id = SystemContexts.MAIN_BEACONS.raw
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val canonicalWorkspaceTagRepository = mockk<CanonicalWorkspaceTagRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val settingsRepository = mockk<SettingsRepository>()
            val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)
            val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            coEvery { contextRepository.getContextById(id) } returns null
            every {
                projector.observePresentationUniverse(any())
            } returns flowOf(emptyList())
            every {
                projector.observeOwnerLabels(any())
            } returns flowOf(emptyMap())
            coEvery {
                projector.resolvePresentation(id, null)
            } returns
                ContextPresentation(
                    id = id,
                    name = "Main Beacons",
                    description = null,
                    parentId = null,
                    roleCode = null,
                    order = 0L,
                    tags = emptyList(),
                )

            every { mainBeaconRepository.observeMainBeaconDetails() } returns flowOf(emptyList())
            every { mainBeaconRepository.observeGroups() } returns flowOf(emptyList())

            every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(emptyList())
            every { settingsRepository.coreBeaconCollapsedGroupIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.obsidianVaultNameFlow } returns flowOf("")
            every { settingsRepository.coreLinkedAttachmentIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.coreConnectionsOrderFlow } returns flowOf(emptyList())
            coEvery { tagAuthority.resolve(id) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())

            val viewModel =
                CoreLevelViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    canonicalWorkspaceTagRepository = canonicalWorkspaceTagRepository,
                    systemWorkspacePresentationContextProjector = projector,
                    systemWorkspaceTagAuthority = tagAuthority,
                    settingsRepository = settingsRepository,
                    attachmentsRepository = attachmentsRepository,
                    noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                    musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                    checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                    mainBeaconRepository = mainBeaconRepository,
                )

            viewModel.addCoreLink(id)
            advanceUntilIdle()

            coVerify(exactly = 0) { contextRepository.getContextById(id) }
            coVerify(exactly = 1) {
                contextRepository.updateContextTags(id, listOf("core"))
            }
        }

    @Test
    fun `Core Level tags shell free standalone Workspace through canonical tags`() =
        runTest(dispatcher) {
            val id = "standalone-core"
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceTagRepository = mockk<CanonicalWorkspaceTagRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val settingsRepository = mockk<SettingsRepository>()
            val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)
            val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            coEvery { contextRepository.getContextById(id) } returns null
            coEvery { tagAuthority.resolve(id) } returns SystemWorkspaceTagAuthority.Resolution.NotSystem
            coEvery { projector.resolvePresentation(id, null) } returns standalonePresentation(id)
            coEvery { canonicalWorkspaceTagRepository.getTags(id) } returns emptyList()
            every { projector.observePresentationUniverse(any()) } returns flowOf(emptyList())
            every { projector.observeOwnerLabels(any()) } returns flowOf(emptyMap())
            every { mainBeaconRepository.observeMainBeaconDetails() } returns flowOf(emptyList())
            every { mainBeaconRepository.observeGroups() } returns flowOf(emptyList())
            every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(emptyList())
            every { settingsRepository.coreBeaconCollapsedGroupIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.obsidianVaultNameFlow } returns flowOf("")
            every { settingsRepository.coreLinkedAttachmentIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.coreConnectionsOrderFlow } returns flowOf(emptyList())

            val viewModel =
                CoreLevelViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = mockk(relaxed = true),
                    canonicalWorkspaceTagRepository = canonicalWorkspaceTagRepository,
                    systemWorkspacePresentationContextProjector = projector,
                    systemWorkspaceTagAuthority = tagAuthority,
                    settingsRepository = settingsRepository,
                    attachmentsRepository = attachmentsRepository,
                    noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                    musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                    checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                    mainBeaconRepository = mainBeaconRepository,
                )

            viewModel.addCoreLink(id)
            advanceUntilIdle()

            coVerify(exactly = 0) { contextRepository.updateContextTags(any(), any()) }
            coVerify(exactly = 1) {
                canonicalWorkspaceTagRepository.replaceTags(id, listOf("core"), any())
            }
        }

    private fun standalonePresentation(id: String) =
        ContextPresentation(
            id = id,
            name = "Standalone",
            description = null,
            parentId = null,
            roleCode = null,
            order = 0L,
            tags = emptyList(),
        )

    @Test
    fun `Strategic Arc tags and creates quest from canonical System presentation without Context shell`() =
        runTest(dispatcher) {
            val id = SystemContexts.STRATEGIC.raw
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val canonicalWorkspaceTagRepository = mockk<CanonicalWorkspaceTagRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val settingsRepository = mockk<SettingsRepository>()
            val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)
            val arcQuestRepository = mockk<ArcQuestRepository>(relaxed = true)
            val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            coEvery { contextRepository.getContextById(id) } returns null

            every {
                projector.observePresentationUniverse(any())
            } returns flowOf(emptyList())
            coEvery {
                projector.resolvePresentation(id, null)
            } returns
                ContextPresentation(
                    id = id,
                    name = "Strategic",
                    description = null,
                    parentId = null,
                    roleCode = null,
                    order = 0L,
                    tags = emptyList(),
                )

            coEvery { tagAuthority.resolve(id) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())

            every { arcQuestRepository.observeNonEmptyArcKeys() } returns flowOf(emptyList())
            every { arcQuestRepository.observeArcQuests(any()) } returns flowOf(emptyList())
            every { mainBeaconRepository.observeMainBeaconDetails() } returns flowOf(emptyList())
            every { mainBeaconRepository.observeGroups() } returns flowOf(emptyList())

            every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(emptyList())
            every { settingsRepository.obsidianVaultNameFlow } returns flowOf("")
            every { settingsRepository.strategicArcLinkedAttachmentIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.strategicArcScopeContextsExpandedFlow } returns flowOf(true)
            every { settingsRepository.strategicArcScopeAttachmentsExpandedFlow } returns flowOf(true)
            every { settingsRepository.strategicArcConnectionsOrderFlow } returns flowOf(emptyList())

            val viewModel =
                StrategicArcViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    canonicalWorkspaceTagRepository = canonicalWorkspaceTagRepository,
                    systemWorkspacePresentationContextProjector = projector,
                    systemWorkspaceTagAuthority = tagAuthority,
                    settingsRepository = settingsRepository,
                    attachmentsRepository = attachmentsRepository,
                    noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                    musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                    checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                    arcQuestRepository = arcQuestRepository,
                    mainBeaconRepository = mainBeaconRepository,
                    missionRepository = mockk<MissionRepository>(relaxed = true),
                )

            viewModel.addArcLink(id)
            advanceUntilIdle()

            coVerify(exactly = 0) { contextRepository.getContextById(id) }
            coVerify(exactly = 1) {
                contextRepository.updateContextTags(id, listOf("arc"))
            }

            val questSlot = slot<ArcQuestEntity>()

            viewModel.addArcQuestFromContext(id)
            advanceUntilIdle()

            coVerify(exactly = 1) { contextRepository.getContextById(id) }
            coVerify(exactly = 1) { projector.resolvePresentation(id, null) }
            coVerify(exactly = 1) { arcQuestRepository.addQuest(capture(questSlot)) }

            assertEquals("Strategic", questSlot.captured.title)
            assertEquals(id, questSlot.captured.linkedContextId)
            assertEquals(ArcQuestSourceType.CONTEXT.name, questSlot.captured.sourceType)
            assertEquals(id, questSlot.captured.sourceId)
        }
}
