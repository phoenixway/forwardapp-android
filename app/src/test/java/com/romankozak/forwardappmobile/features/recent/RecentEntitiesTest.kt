package com.romankozak.forwardappmobile.features.recent

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test

class RecentEntitiesTest {
    @Test fun `recent navigation resolves exact typed identity without Context fallback`() {
        val expected = mapOf(RecentItemType.PROJECT to NavTarget.Workspace("same"),
            RecentItemType.SEMANTIC_ENTITY to NavTarget.SemanticEntityEditor("same"),
            RecentItemType.NOTE_DOCUMENT to NavTarget.NoteDocument("same"),
            RecentItemType.CHECKLIST to NavTarget.Checklist("same"),
            RecentItemType.MUSIC_NOTE to NavTarget.MusicNote("same"),
            RecentItemType.SCRIPT to NavTarget.ScriptEditor(scriptId = "same"))
        expected.forEach { (type, destination) ->
            assertThat(recentEntityDestination(RecentItem("row", type, 1, "Title", "same"))).isEqualTo(destination)
        }
    }

    @Test fun `semantic and script access preserve pin and use different stable IDs for same raw identity`() = runTest {
        val dao = mockk<RecentItemDao>(relaxed = true)
        val stored = mutableMapOf<String, RecentItem>()
        coEvery { dao.getRecentItemById(any()) } answers { stored[firstArg()] }
        coEvery { dao.logAccess(any()) } answers { val row = firstArg<RecentItem>(); stored[row.id] = row }
        val repository = RecentItemsRepository(dao)
        repository.logEntityAccess(RecentItemType.SEMANTIC_ENTITY, "same", "First")
        val semantic = stored.values.single()
        stored[semantic.id] = semantic.copy(isPinned = true)
        repository.logEntityAccess(RecentItemType.SCRIPT, "same", "Script")
        repository.logEntityAccess(RecentItemType.SEMANTIC_ENTITY, "same", "Renamed")
        assertThat(stored).hasSize(2)
        assertThat(stored[semantic.id]!!.isPinned).isTrue()
        assertThat(stored[semantic.id]!!.displayName).isEqualTo("Renamed")
    }

    @Test fun `project-only historical transport rejects non-Workspace recent vocabulary`() {
        RecentItemType.entries.filter { it != RecentItemType.PROJECT }.forEach { type ->
            assertThat(runCatching { RecentItem("row", type, 1, "Title", "id").toSnapshot() }.isFailure).isTrue()
        }
        assertThat(RecentItem("row", RecentItemType.PROJECT, 1, "Title", "id").toSnapshot().contextId).isEqualTo("id")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `semantic history failure does not crash or block editor scope`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val repository = mockk<RecentItemsRepository>()
            coEvery { repository.logEntityAccess(any(), any(), any()) } throws IllegalStateException("Unavailable")
            val model = RecentSemanticAccessViewModel(repository)
            store.put("recorder", model)
            model.record("subject", "Title")
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.logEntityAccess(RecentItemType.SEMANTIC_ENTITY, "subject", "Title") }
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}
