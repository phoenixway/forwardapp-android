package com.romankozak.forwardappmobile.desktop.features.contexts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.romankozak.forwardappmobile.desktop.data.contexts.DesktopWorkspaceFileStore
import com.romankozak.forwardappmobile.desktop.data.contexts.FileBasedDesktopWorkspaceRepository
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveBacklogUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveContextTreeUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemContentUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemDoneUseCase

@Composable
fun rememberDesktopWorkspaceDependencies(): DesktopWorkspaceDependencies {
    val fileStore = remember { DesktopWorkspaceFileStore() }
    val repository = remember { FileBasedDesktopWorkspaceRepository(fileStore = fileStore) }
    val observeContextTree = remember { ObserveContextTreeUseCase(repository) }
    val observeBacklog = remember { ObserveBacklogUseCase(repository) }
    val createContext = remember { CreateContextUseCase(repository) }
    val updateContext = remember { UpdateContextUseCase(repository) }
    val deleteContext = remember { DeleteContextUseCase(repository) }
    val createBacklogItem = remember { CreateBacklogItemUseCase(repository) }
    val deleteBacklogItem = remember { DeleteBacklogItemUseCase(repository) }
    val updateBacklogItemContent = remember { UpdateBacklogItemContentUseCase(repository) }
    val updateBacklogItemDone = remember { UpdateBacklogItemDoneUseCase(repository) }
    return remember {
        DesktopWorkspaceDependencies(
            fileStore = fileStore,
            repository = repository,
            observeContextTree = observeContextTree,
            observeBacklog = observeBacklog,
            createContext = createContext,
            updateContext = updateContext,
            deleteContext = deleteContext,
            createBacklogItem = createBacklogItem,
            deleteBacklogItem = deleteBacklogItem,
            updateBacklogItemContent = updateBacklogItemContent,
            updateBacklogItemDone = updateBacklogItemDone,
        )
    }
}

data class DesktopWorkspaceDependencies(
    val fileStore: DesktopWorkspaceFileStore,
    val repository: DesktopWorkspaceRepository,
    val observeContextTree: ObserveContextTreeUseCase,
    val observeBacklog: ObserveBacklogUseCase,
    val createContext: CreateContextUseCase,
    val updateContext: UpdateContextUseCase,
    val deleteContext: DeleteContextUseCase,
    val createBacklogItem: CreateBacklogItemUseCase,
    val deleteBacklogItem: DeleteBacklogItemUseCase,
    val updateBacklogItemContent: UpdateBacklogItemContentUseCase,
    val updateBacklogItemDone: UpdateBacklogItemDoneUseCase,
)
