package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Search history for both explicitly submitted and completed live searches. */
internal class MagicBoxSearchHistory(
    private val state: MutableStateFlow<GlobalSearchUiState>,
    private val savedState: SavedStateHandle,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val lock = Any()
    private var pendingHistory: List<String>? = null

    companion object {
        const val SAVED_STATE_KEY = "global_search_history"
        const val MAX_ENTRIES = 12
        const val LIVE_QUERY_IDLE_MILLIS = 4_000L
    }

    init {
        scope.launch {
            state.map { it.mode to it.query.trim() }
                .distinctUntilChanged()
                .collectLatest { (mode, query) ->
                    if (mode == OmniboxMode.DataSearch && query.isNotBlank()) {
                        delay(LIVE_QUERY_IDLE_MILLIS)
                        remember(query)
                    }
                }
        }
    }

    fun rememberBeforeClearing(nextQuery: String) {
        val current = state.value
        if (current.mode == OmniboxMode.DataSearch && nextQuery.isBlank()) {
            remember(current.query)
        }
    }

    fun remember(rawQuery: String) = synchronized(lock) {
        val query = rawQuery.trim()
        if (query.isBlank()) return@synchronized
        val previous = state.value.searchHistory
        val updated = (listOf(query) + previous.filterNot { it.equals(query, ignoreCase = true) }).take(MAX_ENTRIES)
        if (updated == previous) return@synchronized
        save(updated)
    }

    fun remove(query: String) = synchronized(lock) {
        if (query.isBlank()) return@synchronized
        save(state.value.searchHistory.filterNot { it.equals(query, ignoreCase = true) })
    }

    fun clear() = synchronized(lock) { save(emptyList()) }

    fun applyStoredRecentInputs(recentInputs: Map<OmniboxMode, List<String>>) = synchronized(lock) {
        val storedHistory = recentInputs[OmniboxMode.DataSearch].orEmpty()
        // DataStore can still emit the previous snapshot while a local edit is being saved.
        val history = pendingHistory ?: storedHistory
        if (pendingHistory == storedHistory) pendingHistory = null
        state.update {
            it.copy(
                searchHistory = history,
                recentModeInputs = recentInputs + (OmniboxMode.DataSearch to history),
            )
        }
    }

    private fun save(history: List<String>) {
        pendingHistory = history
        state.update {
            it.copy(
                searchHistory = history,
                recentModeInputs = it.recentModeInputs + (OmniboxMode.DataSearch to history),
            )
        }
        savedState[SAVED_STATE_KEY] = history
        scope.launch { settings.saveGlobalSearchRecentInputs(OmniboxMode.DataSearch, history) }
    }
}
