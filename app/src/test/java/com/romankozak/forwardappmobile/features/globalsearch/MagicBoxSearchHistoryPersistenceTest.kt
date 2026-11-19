package com.romankozak.forwardappmobile.features.globalsearch

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MagicBoxSearchHistoryPersistenceTest {
    private val store = ViewModelStore()
    private lateinit var settings: SettingsRepository
    private var modelCount = 0

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
        settings.saveGlobalSearchCurrentMode(OmniboxMode.DataSearch)
        settings.saveGlobalSearchRecentInputs(OmniboxMode.DataSearch, emptyList())
    }

    @After fun cleanup() {
        store.clear()
        Dispatchers.resetMain()
    }

    private suspend fun createModel(repository: SettingsRepository = settings): GlobalSearchViewModel {
        val model = GlobalSearchViewModel(
            contextRepository = mockk(relaxed = true),
            settingsRepository = repository,
            inboxRepository = mockk(relaxed = true),
            activityRepository = mockk(relaxed = true),
            reminderRepository = mockk(relaxed = true),
            noteDocumentRepository = mockk(relaxed = true),
            musicNoteRepository = mockk(relaxed = true),
            checklistRepository = mockk(relaxed = true),
            canonicalWorkspaceRepository = mockk(relaxed = true),
            systemWorkspacePresentationContextProjector = mockk(relaxed = true),
            savedStateHandle = SavedStateHandle(),
        )
        store.put("search-${modelCount++}", model)
        withTimeout(5_000) {
            model.uiState.first { it.recentModeInputs.size == OmniboxMode.entries.size }
        }
        return model
    }

    @Test fun `clearing live search keeps the query immediately and after reopening`() = runBlocking {
        val model = createModel()
        model.onQueryChange("health")
        model.onQueryChange("")
        assertEquals(listOf("health"), model.uiState.value.searchHistory)
        assertEquals("", model.uiState.value.query)
        withTimeout(5_000) {
            settings.globalSearchRecentInputsFlow(OmniboxMode.DataSearch).first { it == listOf("health") }
        }
        store.clear()
        val reopened = createModel(SettingsRepository(ApplicationProvider.getApplicationContext<Context>()))
        assertEquals(listOf("health"), reopened.uiState.value.searchHistory)
    }

    @Test fun `submitted canonical search survives clear and fresh saved state`() = runBlocking {
        val model = createModel()
        model.onQueryChange("health")
        model.rememberCanonicalQuery()
        model.onQueryChange("")
        assertEquals(listOf("health"), model.uiState.value.searchHistory)
        withTimeout(5_000) {
            settings.globalSearchRecentInputsFlow(OmniboxMode.DataSearch).first { it == listOf("health") }
        }
        store.clear()
        assertEquals(listOf("health"), createModel().uiState.value.searchHistory)
    }

    @Test fun `clearing repeated searches keeps newest first without case insensitive duplicates`() = runBlocking {
        val model = createModel()
        for ((query, expected) in listOf(
            " health " to listOf("health"),
            "finance" to listOf("finance", "health"),
            "HEALTH" to listOf("HEALTH", "finance"),
        )) {
            model.onQueryChange(query)
            model.onQueryChange("")
            assertEquals(expected, model.uiState.value.searchHistory)
            assertEquals(expected, model.uiState.value.recentModeInputs[OmniboxMode.DataSearch])
            withTimeout(5_000) {
                settings.globalSearchRecentInputsFlow(OmniboxMode.DataSearch).first { it == expected }
                model.uiState.first { it.searchHistory == expected }
            }
        }
    }

    @Test fun `clearing other modes or whitespace does not create search history`() = runBlocking {
        val model = createModel()
        model.onQueryChange("  ")
        model.onQueryChange("")
        model.setMode(OmniboxMode.QuickCatchInbox)
        model.onQueryChange("capture this")
        model.onQueryChange("")
        assertEquals(emptyList<String>(), model.uiState.value.searchHistory)
        assertEquals(emptyList<String>(), settings.globalSearchRecentInputsFlow(OmniboxMode.DataSearch).first())
    }

    @Test fun `stale storage snapshot cannot overwrite newest query before write acknowledgement`() {
        val state = MutableStateFlow(GlobalSearchUiState())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        try {
            val history = MagicBoxSearchHistory(state, SavedStateHandle(), mockk(relaxed = true), scope)
            history.applyStoredRecentInputs(mapOf(OmniboxMode.DataSearch to listOf("old")))
            history.remember("new")
            history.applyStoredRecentInputs(mapOf(OmniboxMode.DataSearch to listOf("old")))
            assertEquals(listOf("new", "old"), state.value.searchHistory)
            history.applyStoredRecentInputs(mapOf(OmniboxMode.DataSearch to listOf("new", "old")))
            // Once the write is acknowledged, external settings restores remain observable.
            history.applyStoredRecentInputs(mapOf(OmniboxMode.DataSearch to listOf("restored")))
            assertEquals(listOf("restored"), state.value.searchHistory)
        } finally { scope.cancel() }
    }

    @Test fun `stale storage snapshot cannot resurrect removed or cleared queries`() {
        val state = MutableStateFlow(GlobalSearchUiState())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        try {
            val history = MagicBoxSearchHistory(state, SavedStateHandle(), mockk(relaxed = true), scope)
            val stored = mapOf(OmniboxMode.DataSearch to listOf("new", "old"))
            history.applyStoredRecentInputs(stored)
            history.remove("old")
            history.applyStoredRecentInputs(stored)
            assertEquals(listOf("new"), state.value.searchHistory)
            history.clear()
            history.applyStoredRecentInputs(stored)
            assertEquals(emptyList<String>(), state.value.searchHistory)
            assertEquals(emptyList<String>(), state.value.recentModeInputs[OmniboxMode.DataSearch])
        } finally { scope.cancel() }
    }
}
