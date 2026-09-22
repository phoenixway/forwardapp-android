package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context as LegacyContext
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.domain.lifecontext.StartContextTrackingUseCase
import com.romankozak.forwardappmobile.domain.lifecontext.SubmitContextInputUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeRepository
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommandDeckViewModelSystemInboxWriteTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `Inbox attachment creation uses stable system owner without Context discovery`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            every { contextRepository.getAllContextsFlow() } returns flowOf(listOf(legacyInboxContext()))
            val noteDocumentRepository = mockk<NoteDocumentRepository>()
            coEvery {
                noteDocumentRepository.createDocument(any(), any(), any(), any(), any(), any())
            } returns "document"
            coEvery {
                contextRepository.findAttachmentIdByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, "document")
            } returns "attachment"

            val attachmentId =
                createViewModel(
                    contextRepository = contextRepository,
                    noteDocumentRepository = noteDocumentRepository,
                ).createAttachmentFromCommandDeck(NewDocumentDraft.Note("Captured note"))

            org.junit.Assert.assertEquals("attachment", attachmentId)
            coVerify(exactly = 1) {
                noteDocumentRepository.createDocument(
                    name = "Captured note",
                    contextId = SystemContexts.INBOX.raw,
                    content = null,
                    attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                    roleCode = null,
                    isSystem = false,
                )
            }
            verify(exactly = 0) { contextRepository.getAllContextsFlow() }
        }

    @Test
    fun `quick create authors a standalone Workspace without Context persistence`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>()
            coEvery {
                canonicalWorkspaceRepository.create(any(), any(), any(), any(), any())
            } returns "standalone-workspace"

            createViewModel(
                contextRepository = contextRepository,
                noteDocumentRepository = mockk(relaxed = true),
                canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            ).createContext("  Operations  ")

            coVerify(exactly = 1) {
                canonicalWorkspaceRepository.create(
                    nameOverride = "Operations",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    now = any(),
                )
            }
        }

    private fun createViewModel(
        contextRepository: ContextRepository,
        noteDocumentRepository: NoteDocumentRepository,
        canonicalWorkspaceRepository: CanonicalWorkspaceRepository = mockk(relaxed = true),
    ): CommandDeckViewModel {
        val application = mockk<Application>()
        every { application.getSharedPreferences(any(), Context.MODE_PRIVATE) } returns
            mockk<SharedPreferences>(relaxed = true)
        val importExportHandler = mockk<CommandDeckImportExportHandler>(relaxed = true)
        every { importExportHandler.importChoiceUri } returns MutableStateFlow<Uri?>(null)
        every { importExportHandler.exportChoiceVisible } returns MutableStateFlow(false)
        every { importExportHandler.syncUiState } returns MutableStateFlow(SyncUseCase.SyncUiState())
        every { importExportHandler.showWifiImportDialog } returns MutableStateFlow(false)
        every { importExportHandler.uiEvents } returns MutableSharedFlow<CommandDeckUiEvent>()
        val sessionModeRepository = mockk<SessionModeRepository>()
        every { sessionModeRepository.sessionModeState } returns flowOf(SessionModeState())

        return CommandDeckViewModel(
            application = application,
            submitContextInputUseCase = mockk<SubmitContextInputUseCase>(relaxed = true),
            startContextTrackingUseCase = mockk<StartContextTrackingUseCase>(relaxed = true),
            sessionModeRepository = sessionModeRepository,
            contextLogRepository = mockk<ContextLogRepository>(relaxed = true),
            importExportHandler = importExportHandler,
            contextRepository = contextRepository,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            noteDocumentRepository = noteDocumentRepository,
            musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
            checklistRepository = mockk<ChecklistRepository>(relaxed = true),
            reminderRepository = mockk<ReminderRepository>(relaxed = true),
        )
    }

    private fun legacyInboxContext() =
        LegacyContext(
            id = "legacy-inbox",
            name = "Inbox",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
}
