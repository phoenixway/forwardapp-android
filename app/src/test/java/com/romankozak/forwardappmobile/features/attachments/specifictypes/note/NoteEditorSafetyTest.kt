package com.romankozak.forwardappmobile.features.attachments.specifictypes.note

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * FK.10B.2a: Editor must never derive ownership from historical Context,
 * resurrect deleted Notes, or report a save before persistence succeeds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorSafetyTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `live Note loads standalone and historical Context is not current location`() = runTest {
        val repository = mockk<LegacyNoteRepository>()
        val stored = note("live").copy(contextId = "deleted-historical-context")

        coEvery { repository.getNoteById("live") } returns stored

        val editor = editor(repository)
        editor.loadNote("live")
        advanceUntilIdle()

        assertEquals(stored.content, editor.universalEditorViewModel.uiState.value.content.text)
        assertEquals(null, editor.universalEditorViewModel.uiState.value.projectId)
    }

    @Test
    fun `missing Note is rejected and never loaded as editable content`() = runTest {
        val repository = mockk<LegacyNoteRepository>()
        coEvery { repository.getNoteById("missing") } returns null

        val editor = editor(repository)
        val errors = captureErrors(editor)

        editor.loadNote("missing")
        advanceUntilIdle()

        assertEquals("", editor.universalEditorViewModel.uiState.value.content.text)
        assertEquals(listOf("Note is unavailable or deleted"), errors)
        coVerify(exactly = 0) { repository.saveNote(any(), null) }
    }

    @Test
    fun `tombstoned Note fails closed on load and save`() = runTest {
        val repository = mockk<LegacyNoteRepository>()
        val deleted = note("deleted").copy(isDeleted = true)

        coEvery { repository.getNoteById("deleted") } returns deleted

        val editor = editor(repository)
        val errors = captureErrors(editor)

        editor.loadNote("deleted")
        advanceUntilIdle()

        assertEquals("", editor.universalEditorViewModel.uiState.value.content.text)
        assertEquals(null, editor.universalEditorViewModel.uiState.value.projectId)

        var callbacks = 0
        editor.saveNote("Attempt to resurrect") { callbacks++ }
        advanceUntilIdle()

        assertEquals(0, callbacks)
        assertEquals(2, errors.size)
        assertTrue(errors.all { it == "Note is unavailable or deleted" })
        coVerify(exactly = 0) { repository.saveNote(any(), null) }
    }

    @Test
    fun `save without Note identity cannot create a phantom Note`() = runTest {
        val repository = mockk<LegacyNoteRepository>()
        val editor = editor(repository)
        val errors = captureErrors(editor)

        var callbacks = 0
        editor.saveNote("Unbound content") { callbacks++ }
        advanceUntilIdle()

        assertEquals(0, callbacks)
        assertEquals(listOf("Note identity is missing"), errors)
        coVerify(exactly = 0) { repository.getNoteById(any()) }
        coVerify(exactly = 0) { repository.saveNote(any(), null) }
    }

    @Test
    fun `failed persistence leaves editor open and surfaces error`() = runTest {
        val repository = mockk<LegacyNoteRepository>()
        coEvery { repository.getNoteById("failure") } returns note("failure")
        coEvery { repository.saveNote(any(), null) } throws
            IllegalStateException("Injected write failure")

        val editor = editor(repository)
        val errors = captureErrors(editor)

        editor.loadNote("failure")
        advanceUntilIdle()

        var callbacks = 0
        editor.saveNote("Updated title\nUpdated content") { callbacks++ }
        advanceUntilIdle()

        assertEquals(0, callbacks)
        assertTrue(errors.contains("Injected write failure"))
        coVerify(exactly = 1) { repository.saveNote(any(), null) }
    }

    @Test
    fun `two queued saves serialize and second reads latest Note version`() = runTest {
        val repository = mockk<LegacyNoteRepository>()
        var persisted = note("serial").copy(version = 1L)
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val writes = mutableListOf<Pair<String, Long>>()

        coEvery { repository.getNoteById("serial") } coAnswers {
            persisted
        }
        coEvery { repository.saveNote(any(), null) } coAnswers {
            val incoming = firstArg<LegacyNoteEntity>()

            if (writes.isEmpty()) {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }

            assertEquals(persisted.version, incoming.version)
            writes += incoming.title to incoming.version
            persisted = incoming.copy(version = incoming.version + 1L)
        }

        val editor = editor(repository)
        editor.loadNote("serial")
        advanceUntilIdle()

        editor.saveNote("First")
        runCurrent()
        assertTrue(firstEntered.isCompleted)

        var completedCallbacks = 0
        editor.saveNote("Second") { completedCallbacks++ }
        runCurrent()

        assertTrue(writes.isEmpty())
        assertEquals(0, completedCallbacks)

        releaseFirst.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("First", "Second"), writes.map { it.first })
        assertEquals(listOf(1L, 2L), writes.map { it.second })
        assertEquals(3L, persisted.version)
        assertEquals("Second", persisted.content)
        assertEquals(1, completedCallbacks)
        assertFalse(firstEntered.isCancelled)
    }

    private fun editor(repository: LegacyNoteRepository): NoteEditorViewModel =
        NoteEditorViewModel(
            noteRepository = repository,
            application = ApplicationProvider.getApplicationContext<Application>(),
        )

    private fun TestScope.captureErrors(
        editor: NoteEditorViewModel,
    ): MutableList<String> {
        val messages = mutableListOf<String>()

        // Deliver error events immediately; advanceUntilIdle excludes background work.
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler),
            start = CoroutineStart.UNDISPATCHED,
        ) {
            editor.universalEditorViewModel.events.collect { event ->
                if (event is UniversalEditorEvent.ShowError) {
                    messages += event.message
                }
            }
        }

        return messages
    }

    private fun note(id: String) =
        LegacyNoteEntity(
            id = id,
            contextId = null,
            title = "Title $id",
            content = "Content $id",
        )
}
