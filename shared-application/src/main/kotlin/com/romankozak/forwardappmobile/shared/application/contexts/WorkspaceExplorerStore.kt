package com.romankozak.forwardappmobile.shared.application.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextTreeNode
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveBacklogUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveContextTreeUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemContentUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemDoneUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateContextUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkspaceExplorerStore(
    private val observeContextTree: ObserveContextTreeUseCase,
    private val observeBacklog: ObserveBacklogUseCase,
    private val createContext: CreateContextUseCase,
    private val updateContext: UpdateContextUseCase,
    private val deleteContext: DeleteContextUseCase,
    private val createBacklogItem: CreateBacklogItemUseCase,
    private val deleteBacklogItem: DeleteBacklogItemUseCase,
    private val updateBacklogItemContent: UpdateBacklogItemContentUseCase,
    private val updateBacklogItemDone: UpdateBacklogItemDoneUseCase,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(WorkspaceExplorerState())
    private var reloadJob: Job? = null

    val state: StateFlow<WorkspaceExplorerState> = mutableState.asStateFlow()

    init {
        reload()
    }

    fun dispatch(intent: WorkspaceExplorerIntent) {
        when (intent) {
            is WorkspaceExplorerIntent.QueryChanged -> onQueryChange(intent.query)
            is WorkspaceExplorerIntent.ContextSelected -> onSelectContext(intent.contextId)
            WorkspaceExplorerIntent.StartCreatingContext -> onStartCreatingContext()
            WorkspaceExplorerIntent.StartEditingContext -> onStartEditingContext()
            WorkspaceExplorerIntent.CancelContextEditing -> onCancelCreatingContext()
            is WorkspaceExplorerIntent.ContextDraftNameChanged -> onContextDraftNameChange(intent.name)
            is WorkspaceExplorerIntent.ContextDraftDescriptionChanged -> onContextDraftDescriptionChange(intent.description)
            is WorkspaceExplorerIntent.ContextDraftStatusChanged -> onContextDraftStatusChange(intent.status)
            is WorkspaceExplorerIntent.ContextDraftViewChanged -> onContextDraftViewChange(intent.view)
            WorkspaceExplorerIntent.SaveContext -> onSaveContext()
            WorkspaceExplorerIntent.DeleteContext -> onDeleteContext()
            WorkspaceExplorerIntent.StartCreatingBacklogItem -> onStartCreatingBacklogItem()
            is WorkspaceExplorerIntent.StartEditingBacklogItem -> onStartEditingBacklogItem(intent.itemId)
            WorkspaceExplorerIntent.CancelBacklogEditing -> onCancelEditingBacklogItem()
            is WorkspaceExplorerIntent.BacklogDraftTitleChanged -> onBacklogDraftTitleChange(intent.title)
            is WorkspaceExplorerIntent.BacklogDraftDetailsChanged -> onBacklogDraftDetailsChange(intent.details)
            is WorkspaceExplorerIntent.BacklogDraftPriorityChanged -> onBacklogDraftPriorityChange(intent.priority)
            WorkspaceExplorerIntent.SaveBacklogItem -> onSaveBacklogItemEdits()
            is WorkspaceExplorerIntent.ToggleBacklogItemDone -> onToggleBacklogItem(intent.itemId, intent.isDone)
            is WorkspaceExplorerIntent.DeleteBacklogItem -> onDeleteBacklogItem(intent.itemId)
        }
    }

    private fun onQueryChange(query: String) {
        mutableState.update { current -> current.copy(query = query) }
        reload()
    }

    private fun onSelectContext(contextId: String) {
        mutableState.update { current -> current.copy(selectedContextId = contextId).clearEditor() }
        reloadBacklogOnly(contextId)
    }

    private fun onStartCreatingContext() {
        val selectedContextId = mutableState.value.selectedContextId
        mutableState.update { current ->
            current.copy(
                editingContextId = null,
                creatingContextParentId = selectedContextId,
                contextDraftName = "",
                contextDraftDescription = "",
                contextDraftStatus = SharedContextStatus.Planning,
                contextDraftView = SharedContextView.Backlog,
            )
        }
    }

    private fun onStartEditingContext() {
        val selectedNode = mutableState.value.nodes.firstOrNull { node -> node.context.id == mutableState.value.selectedContextId } ?: return
        mutableState.update { current ->
            current.copy(
                editingContextId = selectedNode.context.id,
                creatingContextParentId = null,
                contextDraftName = selectedNode.context.name,
                contextDraftDescription = selectedNode.context.description.orEmpty(),
                contextDraftStatus = selectedNode.context.status,
                contextDraftView = selectedNode.context.defaultView,
            )
        }
    }

    private fun onCancelCreatingContext() {
        mutableState.update { current -> current.clearContextDraft() }
    }

    private fun onContextDraftNameChange(name: String) {
        mutableState.update { current -> current.copy(contextDraftName = name) }
    }

    private fun onContextDraftDescriptionChange(description: String) {
        mutableState.update { current -> current.copy(contextDraftDescription = description) }
    }

    private fun onContextDraftStatusChange(status: SharedContextStatus) {
        mutableState.update { current -> current.copy(contextDraftStatus = status) }
    }

    private fun onContextDraftViewChange(view: SharedContextView) {
        mutableState.update { current -> current.copy(contextDraftView = view) }
    }

    private fun onSaveContext() {
        val draft = mutableState.value
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                mutableState.update { current -> current.copy(isSavingContext = true) }
                val savedContext =
                    if (draft.editingContextId != null) {
                        updateContext(
                            contextId = draft.editingContextId,
                            name = draft.contextDraftName,
                            description = draft.contextDraftDescription,
                            status = draft.contextDraftStatus,
                            defaultView = draft.contextDraftView,
                        )
                    } else {
                        createContext(
                            parentId = draft.creatingContextParentId,
                            name = draft.contextDraftName,
                            description = draft.contextDraftDescription,
                            status = draft.contextDraftStatus,
                            defaultView = draft.contextDraftView,
                        )
                    }
                val nodes = observeContextTree(mutableState.value.query)
                val selectedContextId = savedContext?.id ?: mutableState.value.selectedContextId
                val selectedNode = resolveSelectedNode(nodes, selectedContextId)
                val backlogItems = observeBacklog(selectedNode?.context?.id.orEmpty())
                mutableState.update { current ->
                    current.copy(
                        nodes = nodes,
                        selectedContextId = selectedNode?.context?.id,
                        selectedContextName = selectedNode?.context?.name ?: "No context selected",
                        backlogItems = backlogItems,
                        isSavingContext = false,
                    ).clearContextDraft()
                }
            }
    }

    private fun onDeleteContext() {
        val contextId = mutableState.value.selectedContextId ?: return
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                mutableState.update { current -> current.copy(deletingContextId = contextId) }
                deleteContext(contextId)
                val nodes = observeContextTree(mutableState.value.query)
                val selectedNode = nodes.firstOrNull()
                val backlogItems = observeBacklog(selectedNode?.context?.id.orEmpty())
                mutableState.update { current ->
                    current.copy(
                        nodes = nodes,
                        selectedContextId = selectedNode?.context?.id,
                        selectedContextName = selectedNode?.context?.name ?: "No context selected",
                        backlogItems = backlogItems,
                        deletingContextId = null,
                        isSavingContext = false,
                    ).clearContextDraft()
                }
            }
    }

    private fun onStartCreatingBacklogItem() {
        if (mutableState.value.selectedContextId == null) {
            return
        }
        mutableState.update { current ->
            current.copy(
                isCreatingBacklogItem = true,
                editingBacklogItemId = null,
                backlogDraftTitle = "",
                backlogDraftDetails = "",
                backlogDraftPriority = SharedBacklogPriority.Medium,
            )
        }
    }

    private fun onStartEditingBacklogItem(itemId: String) {
        val item = mutableState.value.backlogItems.firstOrNull { backlogItem -> backlogItem.id == itemId } ?: return
        mutableState.update { current ->
            current.copy(
                isCreatingBacklogItem = false,
                editingBacklogItemId = item.id,
                backlogDraftTitle = item.title,
                backlogDraftDetails = item.details.orEmpty(),
                backlogDraftPriority = item.priority,
            )
        }
    }

    private fun onCancelEditingBacklogItem() {
        clearBacklogEditor()
    }

    private fun onBacklogDraftTitleChange(title: String) {
        mutableState.update { current -> current.copy(backlogDraftTitle = title) }
    }

    private fun onBacklogDraftDetailsChange(details: String) {
        mutableState.update { current -> current.copy(backlogDraftDetails = details) }
    }

    private fun onBacklogDraftPriorityChange(priority: SharedBacklogPriority) {
        mutableState.update { current -> current.copy(backlogDraftPriority = priority) }
    }

    private fun onSaveBacklogItemEdits() {
        val currentState = mutableState.value
        val itemId = currentState.editingBacklogItemId
        val selectedContextId = currentState.selectedContextId
        if (itemId == null && !currentState.isCreatingBacklogItem) {
            return
        }
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                val savingKey = itemId ?: NEW_BACKLOG_ITEM_KEY
                mutableState.update { state -> state.copy(savingBacklogItemId = savingKey) }
                if (currentState.isCreatingBacklogItem) {
                    createBacklogItem(
                        contextId = selectedContextId.orEmpty(),
                        title = mutableState.value.backlogDraftTitle,
                        details = mutableState.value.backlogDraftDetails,
                        priority = mutableState.value.backlogDraftPriority,
                    )
                } else {
                    updateBacklogItemContent(
                        itemId = itemId.orEmpty(),
                        title = mutableState.value.backlogDraftTitle,
                        details = mutableState.value.backlogDraftDetails,
                        priority = mutableState.value.backlogDraftPriority,
                    )
                }
                reloadSelectedBacklogAndClearEditor(itemId ?: NEW_BACKLOG_ITEM_KEY)
            }
    }

    private fun onToggleBacklogItem(itemId: String, isDone: Boolean) {
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                mutableState.update { current -> current.copy(savingBacklogItemId = itemId) }
                updateBacklogItemDone(itemId = itemId, isDone = isDone)
                reloadSelectedBacklogAndClearEditor(itemId)
            }
    }

    private fun onDeleteBacklogItem(itemId: String) {
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                mutableState.update { current -> current.copy(deletingBacklogItemId = itemId) }
                deleteBacklogItem(itemId)
                reloadSelectedBacklogAndClearEditor(itemId)
            }
    }

    private fun reload() {
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                val nodes = observeContextTree(mutableState.value.query)
                val selectedNode = resolveSelectedNode(nodes, mutableState.value.selectedContextId)
                val backlogItems = observeBacklog(selectedNode?.context?.id.orEmpty())

                mutableState.update { current ->
                    current.copy(
                        nodes = nodes,
                        selectedContextId = selectedNode?.context?.id,
                        selectedContextName = selectedNode?.context?.name ?: "No context selected",
                        backlogItems = backlogItems,
                        editingContextId = null,
                        creatingContextParentId = null,
                        contextDraftName = "",
                        contextDraftDescription = "",
                        contextDraftStatus = SharedContextStatus.Planning,
                        contextDraftView = SharedContextView.Backlog,
                        isSavingContext = false,
                        deletingContextId = null,
                        savingBacklogItemId = null,
                        deletingBacklogItemId = null,
                        editingBacklogItemId = null,
                    )
                }
            }
    }

    private fun reloadBacklogOnly(contextId: String) {
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                val selectedNode = mutableState.value.nodes.firstOrNull { it.context.id == contextId }
                val backlogItems = observeBacklog(contextId)

                mutableState.update { current ->
                    current.copy(
                        selectedContextId = contextId,
                        selectedContextName = selectedNode?.context?.name ?: "No context selected",
                        backlogItems = backlogItems,
                        isSavingContext = false,
                        deletingContextId = null,
                        savingBacklogItemId = null,
                        deletingBacklogItemId = null,
                    ).withEditorAdjusted()
                }
            }
    }

    private suspend fun reloadSelectedBacklogAndClearEditor(savedItemId: String) {
        val selectedContextId = mutableState.value.selectedContextId.orEmpty()
        val backlogItems = observeBacklog(selectedContextId)

        mutableState.update { current ->
            current.copy(
                backlogItems = backlogItems,
                savingBacklogItemId = null,
                deletingBacklogItemId = null,
            ).clearEditorIfMatches(savedItemId)
        }
    }

    private fun resolveSelectedNode(
        nodes: List<SharedContextTreeNode>,
        selectedContextId: String?,
    ): SharedContextTreeNode? = nodes.firstOrNull { it.context.id == selectedContextId } ?: nodes.firstOrNull()

    private fun WorkspaceExplorerState.withEditorAdjusted(): WorkspaceExplorerState {
        if (isCreatingBacklogItem) {
            return this
        }
        val editingItem =
            backlogItems.firstOrNull { item -> item.id == editingBacklogItemId }
                ?: return clearEditor()
        return copy(
            backlogDraftTitle = editingItem.title,
            backlogDraftDetails = editingItem.details.orEmpty(),
            backlogDraftPriority = editingItem.priority,
        )
    }

    private fun WorkspaceExplorerState.clearEditorIfMatches(itemId: String): WorkspaceExplorerState =
        if (editingBacklogItemId == itemId || (isCreatingBacklogItem && itemId == NEW_BACKLOG_ITEM_KEY)) {
            clearEditor()
        } else {
            this
        }

    private fun clearBacklogEditor() {
        mutableState.update { current -> current.clearEditor() }
    }

    private fun WorkspaceExplorerState.clearContextDraft(): WorkspaceExplorerState =
        copy(
            editingContextId = null,
            creatingContextParentId = null,
            contextDraftName = "",
            contextDraftDescription = "",
            contextDraftStatus = SharedContextStatus.Planning,
            contextDraftView = SharedContextView.Backlog,
            isSavingContext = false,
            deletingContextId = null,
        )

    private fun WorkspaceExplorerState.clearEditor(): WorkspaceExplorerState =
        copy(
            isCreatingBacklogItem = false,
            editingBacklogItemId = null,
            backlogDraftTitle = "",
            backlogDraftDetails = "",
            backlogDraftPriority = SharedBacklogPriority.Medium,
        )

    private companion object {
        const val NEW_BACKLOG_ITEM_KEY = "__new_backlog_item__"
    }
}
