package com.romankozak.forwardappmobile.features.entitypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserQuery
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.queryCanonicalEntities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface CanonicalEntityPickerUiState {
    data object Loading : CanonicalEntityPickerUiState

    data class Ready(
        val items: List<CanonicalEntitySummary>,
        val searchText: String,
        val visibleClasses: Set<CanonicalEntityClass>,
        val selectedRefs: Set<CanonicalEntityRef>,
        val request: CanonicalEntityPickerRequest,
    ) : CanonicalEntityPickerUiState

    data class Error(val message: String) : CanonicalEntityPickerUiState
}

sealed interface CanonicalEntityPickerEvent {
    data class Complete(val selected: Set<CanonicalEntityRef>) : CanonicalEntityPickerEvent
}

@HiltViewModel
class CanonicalEntityPickerViewModel
    @Inject
    constructor(
        catalogRepository: CanonicalEntityCatalogRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val request = requestFrom(savedStateHandle)
        private val search = MutableStateFlow("")
        private val visibleClasses = MutableStateFlow(request.allowedClasses)
        private val selection = MutableStateFlow(request.initiallySelected)
        private val _events = MutableSharedFlow<CanonicalEntityPickerEvent>(extraBufferCapacity = 1)
        val events = _events.asSharedFlow()

        val uiState =
            combine(
                catalogRepository.observeCatalog(),
                search,
                visibleClasses,
                selection,
            ) { catalog, text, classes, selected ->
                CanonicalEntityPickerUiState.Ready(
                    items =
                        queryCanonicalEntities(
                            catalog,
                            CanonicalEntityBrowserQuery(
                                text = text,
                                allowedClasses = classes,
                                excludedRefs = request.excludedRefs,
                            ),
                        ),
                    searchText = text,
                    visibleClasses = classes,
                    selectedRefs = selected,
                    request = request,
                ) as CanonicalEntityPickerUiState
            }.catch { emit(CanonicalEntityPickerUiState.Error(it.message ?: "Не вдалося завантажити сутності")) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CanonicalEntityPickerUiState.Loading)

        fun setSearch(value: String) {
            search.value = value
        }

        fun toggleClass(entityClass: CanonicalEntityClass) {
            if (entityClass !in request.allowedClasses) return
            visibleClasses.value =
                if (entityClass in visibleClasses.value) {
                    visibleClasses.value - entityClass
                } else {
                    visibleClasses.value + entityClass
                }
        }

        fun select(ref: CanonicalEntityRef) {
            selection.value = togglePickerSelection(request, selection.value, ref)
            if (request.selectionMode == CanonicalEntitySelectionMode.SINGLE) {
                _events.tryEmit(CanonicalEntityPickerEvent.Complete(selection.value))
            }
        }

        fun confirm() {
            if (request.selectionMode == CanonicalEntitySelectionMode.MULTI) {
                _events.tryEmit(CanonicalEntityPickerEvent.Complete(selection.value))
            }
        }

        private fun requestFrom(handle: SavedStateHandle): CanonicalEntityPickerRequest {
            fun refs(key: String): Set<CanonicalEntityRef> =
                handle.get<String>(key)
                    ?.takeIf(String::isNotBlank)
                    ?.split(';')
                    ?.map(CanonicalEntityRefCodec::decode)
                    ?.toSet()
                    .orEmpty()

            val allowed =
                requireNotNull(handle.get<String>("allowedClasses"))
                    .split(',')
                    .map(CanonicalEntityClass::valueOf)
                    .toSet()
            return CanonicalEntityPickerRequest(
                allowedClasses = allowed,
                excludedRefs = refs("excludedRefs"),
                selectionMode = CanonicalEntitySelectionMode.valueOf(requireNotNull(handle["selectionMode"])),
                initiallySelected = refs("initiallySelected"),
            )
        }
    }
