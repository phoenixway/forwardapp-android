package com.romankozak.forwardappmobile.features.mainscreen

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StrategicArcWorkspaceCreationTest {
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
    fun `root picker creates standalone Workspace and Arc link uses canonical tags`() =
        runTest(dispatcher) {
            val workspaceId = "strategic-arc-workspace"
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>()
            val canonicalWorkspaceTagRepository = mockk<CanonicalWorkspaceTagRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val settingsRepository = mockk<SettingsRepository>()
            val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)
            val arcQuestRepository = mockk<ArcQuestRepository>(relaxed = true)
            val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            every { projector.observePresentationUniverse(any()) } returns flowOf(emptyList())
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
            coEvery {
                canonicalWorkspaceRepository.create(
                    nameOverride = "Arc owner",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    now = any(),
                )
            } returns workspaceId
            coEvery { contextRepository.getContextById(workspaceId) } returns null
            coEvery { tagAuthority.resolve(workspaceId) } returns
                SystemWorkspaceTagAuthority.Resolution.NotSystem
            coEvery {
                projector.resolvePresentation(workspaceId, null)
            } returns standalonePresentation(workspaceId)
            coEvery { canonicalWorkspaceTagRepository.getTags(workspaceId) } returns emptyList()

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

            assertEquals(workspaceId, viewModel.createRootContextForPicker("  Arc owner  "))
            assertNull(viewModel.createRootContextForPicker("   "))

            coVerify(exactly = 1) {
                canonicalWorkspaceRepository.create(
                    nameOverride = "Arc owner",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    now = any(),
                )
            }
            viewModel.addArcLink(workspaceId)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                canonicalWorkspaceTagRepository.replaceTags(workspaceId, listOf("arc"), any())
            }
            coVerify(exactly = 0) {
                contextRepository.updateContextTags(any(), any())
            }

            val questSlot = slot<ArcQuestEntity>()
            viewModel.addArcQuestFromContext(workspaceId)
            advanceUntilIdle()

            coVerify(exactly = 1) { arcQuestRepository.addQuest(capture(questSlot)) }
            assertEquals(workspaceId, questSlot.captured.linkedContextId)
            assertEquals(ArcQuestSourceType.CONTEXT.name, questSlot.captured.sourceType)
            assertEquals(workspaceId, questSlot.captured.sourceId)
        }

    private fun standalonePresentation(id: String) =
        ContextPresentation(
            id = id,
            name = "Arc owner",
            description = null,
            parentId = null,
            roleCode = null,
            order = 0L,
            tags = emptyList(),
        )
}
