package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MagicBoxSearchHistoryLiveQueryTest {
    @Test fun `live query is saved after four seconds without Send`() = runTest {
        val state = MutableStateFlow(GlobalSearchUiState(query = "health"))
        val settings = mockk<SettingsRepository>(relaxed = true)
        MagicBoxSearchHistory(state, SavedStateHandle(), settings, backgroundScope)
        runCurrent()
        advanceTimeBy(3_999)
        runCurrent()
        assertEquals(emptyList<String>(), state.value.searchHistory)
        coVerify(exactly = 0) { settings.saveGlobalSearchRecentInputs(any(), any()) }
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("health"), state.value.searchHistory)
        coVerify(exactly = 1) { settings.saveGlobalSearchRecentInputs(OmniboxMode.DataSearch, listOf("health")) }
        advanceTimeBy(8_000)
        runCurrent()
        coVerify(exactly = 1) { settings.saveGlobalSearchRecentInputs(any(), any()) }
    }

    @Test fun `continued typing restarts timer and skips unfinished prefix`() = runTest {
        val state = MutableStateFlow(GlobalSearchUiState(query = "hea"))
        MagicBoxSearchHistory(state, SavedStateHandle(), mockk(relaxed = true), backgroundScope)
        runCurrent()
        advanceTimeBy(3_000)
        state.value = state.value.copy(query = "health")
        runCurrent()
        advanceTimeBy(3_999)
        runCurrent()
        assertEquals(emptyList<String>(), state.value.searchHistory)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("health"), state.value.searchHistory)
    }

    @Test fun `clearing or leaving search mode cancels pending automatic save`() = runTest {
        val state = MutableStateFlow(GlobalSearchUiState(query = "health"))
        val settings = mockk<SettingsRepository>(relaxed = true)
        MagicBoxSearchHistory(state, SavedStateHandle(), settings, backgroundScope)
        runCurrent()
        advanceTimeBy(2_000)
        state.value = state.value.copy(query = "")
        runCurrent()
        advanceTimeBy(4_000)
        state.value = state.value.copy(query = "capture", mode = OmniboxMode.QuickCatchInbox)
        runCurrent()
        advanceTimeBy(4_000)
        state.value = state.value.copy(query = "finance", mode = OmniboxMode.DataSearch)
        runCurrent()
        advanceTimeBy(2_000)
        state.value = state.value.copy(mode = OmniboxMode.Command)
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(emptyList<String>(), state.value.searchHistory)
        coVerify(exactly = 0) { settings.saveGlobalSearchRecentInputs(any(), any()) }
    }
}
