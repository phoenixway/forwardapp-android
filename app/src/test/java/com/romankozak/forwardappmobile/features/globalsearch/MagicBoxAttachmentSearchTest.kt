package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.SearchRepository
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*

internal fun searchAttachment(id: String = "a", type: String = "NOTE_DOCUMENT", title: String = "Health notes", content: String = "Treatment plan") =
    AttachmentLibraryQueryResult(id, "entity-$id", type, "workspace", 1,
        if (type == "NOTE_DOCUMENT") title else null, if (type == "NOTE_DOCUMENT") content else null,
        if (type == "MUSIC_NOTE") title else null, if (type == "MUSIC_NOTE") content else null, 1,
        if (type == "CHECKLIST") title else null, if (type == "CHECKLIST") content else null,
        null, null, null, if (type == "SCRIPT") title else null, null, if (type == "SCRIPT") content else null,
        "Workspace", 1)

@OptIn(ExperimentalCoroutinesApi::class)
class MagicBoxAttachmentSearchTest {
    private val attachments = mockk<AttachmentsRepository>()
    private val rows = MutableStateFlow(listOf(searchAttachment()))
    private val repository = SearchRepository(mockk(), mockk(), mockk(), mockk(), attachments,
        mockk(), mockk(), mockk(), mockk(), mockk())
    private val models = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { attachments.getAttachmentLibraryItems() } returns rows
    }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }
    private fun vm() = MagicBoxAttachmentViewModel(repository).also { models.put("attachments", it) }
    private suspend fun MagicBoxAttachmentViewModel.await(predicate: (MagicBoxAttachmentState) -> Boolean) =
        withTimeout(5_000) { state.first(predicate) }

    @Test fun `title and content reuse existing case insensitive attachment search`() = runBlocking<Unit> {
        assertThat(repository.observeAttachmentSearch("HEALTH").first().single().searchResult.entityId).isEqualTo("entity-a")
        assertThat(repository.observeAttachmentSearch("Treatment").first()).hasSize(1)
        assertThat(repository.observeAttachmentSearch("unmatched").first()).isEmpty()
    }

    @Test fun `documents checklists music and scripts are all present with exact editor identities`() = runBlocking<Unit> {
        rows.value = listOf("NOTE_DOCUMENT", "CHECKLIST", "MUSIC_NOTE", "SCRIPT").map { searchAttachment(it, it) }
        val model = vm()
        model.setInput("Health")
        val found = model.await { it.results.size == 4 }.results
        val destinations = found.map { attachmentSearchDestination(it.searchResult) }
        assertThat(destinations).containsExactly(NavTarget.NoteDocument("entity-NOTE_DOCUMENT"),
            NavTarget.Checklist("entity-CHECKLIST"), NavTarget.MusicNote("entity-MUSIC_NOTE"),
            NavTarget.ScriptEditor(contextId = "workspace", scriptId = "entity-SCRIPT"))
    }

    @Test fun `rename content and live-library deletion update current query without refresh`() = runBlocking<Unit> {
        val model = vm()
        model.setInput("Health")
        model.await { it.results.size == 1 }
        rows.value = listOf(searchAttachment(title = "Renamed", content = "No match"))
        model.await { it.input == "Health" && it.results.isEmpty() }
        rows.value = listOf(searchAttachment(title = "Renamed", content = "Health details"))
        model.await { it.results.size == 1 }
        rows.value = emptyList() // Library authority excludes tombstoned/missing targets.
        model.await { it.results.isEmpty() }
        verify(exactly = 1) { attachments.getAttachmentLibraryItems() }
        confirmVerified(attachments)
    }

    @Test fun `structured operators and blank input never query attachments`() = runBlocking<Unit> {
        val model = vm()
        listOf("type:orientation", "#health", "saved:Health", "score:importance>=7", " ").forEach { query ->
            model.setInput(query)
            assertThat(model.await { it.input == query }.results).isEmpty()
        }
        verify { attachments wasNot Called }
    }

    @Test fun `read failure is visible and next query recovers`() = runBlocking<Unit> {
        every { attachments.getAttachmentLibraryItems() } returns flow { error("unavailable") }
        val model = vm()
        model.setInput("Health")
        model.await { it.error != null }
        every { attachments.getAttachmentLibraryItems() } returns rows
        model.setInput("Treatment")
        model.await { it.input == "Treatment" && it.results.size == 1 && it.error == null }
    }

    @Test fun `unsupported destination fails closed without Workspace fallback in normal results`() = runBlocking<Unit> {
        rows.value = listOf(searchAttachment().copy(attachmentType = "UNKNOWN"))
        val model = vm()
        model.setInput("Health")
        assertThat(model.await { it.input == "Health" }.results).isEmpty()
    }

    @Test fun `same raw IDs across attachment types remain distinct and duplicate library records collapse`() = runBlocking<Unit> {
        val note = searchAttachment()
        val checklist = searchAttachment("b", "CHECKLIST").copy(entityId = note.entityId)
        rows.value = listOf(note, note, checklist)
        val model = vm()
        model.setInput("Health")
        assertThat(model.await { it.input == "Health" }.results).hasSize(2)
    }
}
