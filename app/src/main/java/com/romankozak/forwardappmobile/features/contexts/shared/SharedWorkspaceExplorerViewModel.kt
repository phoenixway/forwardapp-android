package com.romankozak.forwardappmobile.features.contexts.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerState
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerStore
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveBacklogUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveContextTreeUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemContentUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemDoneUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SharedWorkspaceExplorerViewModel
    @Inject
    constructor(
        contextRepository: ContextRepository,
        goalRepository: GoalRepository,
        contextStructureRepository: ContextStructureRepository,
    ) : ViewModel() {
        private val adapter =
            AndroidWorkspaceRepositoryAdapter(
                contextRepository = contextRepository,
                goalRepository = goalRepository,
                contextStructureRepository = contextStructureRepository,
            )

        private val store =
            WorkspaceExplorerStore(
                observeContextTree = ObserveContextTreeUseCase(adapter),
                observeBacklog = ObserveBacklogUseCase(adapter),
                createContext = CreateContextUseCase(adapter),
                updateContext = UpdateContextUseCase(adapter),
                deleteContext = DeleteContextUseCase(adapter),
                createBacklogItem = CreateBacklogItemUseCase(adapter),
                deleteBacklogItem = DeleteBacklogItemUseCase(adapter),
                updateBacklogItemContent = UpdateBacklogItemContentUseCase(adapter),
                updateBacklogItemDone = UpdateBacklogItemDoneUseCase(adapter),
                scope = viewModelScope,
            )

        val state: StateFlow<WorkspaceExplorerState> = store.state

        fun dispatch(intent: WorkspaceExplorerIntent) {
            store.dispatch(intent)
        }
    }
