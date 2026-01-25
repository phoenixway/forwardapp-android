package com.romankozak.forwardappmobile.features.context_lab.hierarchy_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.features.context_lab.ContextLabController
import com.romankozak.forwardappmobile.features.context_lab.domain.SwitchContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExperimentalHierarchyViewModel
    @Inject
    constructor(
        private val labController: ContextLabController,
        private val switchContextUseCase: SwitchContextUseCase,
    ) : ViewModel() {
        // This is a bit of a hack. The labController is a singleton but not exposed as a flow.
        // For a real screen, this data should come from a repository that provides flows.
        // For this example, we'll just poll it. The ContextLabViewModel has a flow,
        // but we can't easily share it without a common repository.
        private val allContextsFlow = MutableStateFlow(labController.getAllContexts())
        private val activeContextIdFlow = MutableStateFlow(labController.getActiveContext()?.id)

        val uiState =
            combine(
                allContextsFlow,
                activeContextIdFlow,
            ) { contexts, activeId ->
                ExperimentalHierarchyUiState(
                    contexts = contexts,
                    activeContextId = activeId,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                ExperimentalHierarchyUiState(),
            )

        fun onEvent(event: ExperimentalHierarchyEvent) {
            when (event) {
                is ExperimentalHierarchyEvent.ActivateContext -> {
                    switchContextUseCase.execute(event.contextId)
                    // Refresh the flows after activation
                    activeContextIdFlow.value = labController.getActiveContext()?.id
                }
            }
        }

        // A simple way to refresh the data when the screen is shown
        fun onResume() {
            allContextsFlow.value = labController.getAllContexts()
            activeContextIdFlow.value = labController.getActiveContext()?.id
        }
    }

sealed class ExperimentalHierarchyEvent {
    data class ActivateContext(val contextId: ContextId) : ExperimentalHierarchyEvent()
}
