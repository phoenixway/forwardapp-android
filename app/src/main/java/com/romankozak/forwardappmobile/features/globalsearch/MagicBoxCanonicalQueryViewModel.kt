package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityQueryRepository
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQueryRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.executeCanonicalEntityQuery
import com.romankozak.forwardappmobile.features.entitylibrary.canonicalPlacedRefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class MagicBoxCanonicalState(
    val input: String = "",
    val resolution: MagicBoxQueryResolution = MagicBoxQueryResolution(),
    val results: List<CanonicalEntitySummary> = emptyList(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class MagicBoxCanonicalQueryViewModel @Inject constructor(
    catalog: CanonicalEntityQueryRepository,
    saved: SavedEntityQueryRepository,
    presence: CanonicalHierarchyPlacementPresenceRepository,
) : ViewModel() {
    private val request = MutableStateFlow("" to null as String?)
    fun setInput(input: String) { if (input != request.value.first) request.value = input to null }
    fun selectSaved(id: String) { request.value = request.value.first to id }

    val state = combine(request.debounce(180), catalog.observeCatalog(), saved.observe(), presence.observeVisiblePlacedTargets()) {
        (input, selectedId), items, queries, placed ->
        val resolved = resolveMagicBoxQuery(input, queries, selectedId)
        MagicBoxCanonicalState(input, resolved, resolved.query?.let {
            executeCanonicalEntityQuery(items, it, canonicalPlacedRefs(items, placed))
        }.orEmpty())
    }.flowOn(Dispatchers.Default).catch {
        emit(MagicBoxCanonicalState(request.value.first, MagicBoxQueryResolution(
            issue = MagicBoxQueryIssue.SAVED_UNAVAILABLE, messages = listOf("Канонічний пошук тимчасово недоступний"))))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MagicBoxCanonicalState())
}
