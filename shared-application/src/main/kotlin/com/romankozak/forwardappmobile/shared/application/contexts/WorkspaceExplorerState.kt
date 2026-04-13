package com.romankozak.forwardappmobile.shared.application.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextTreeNode
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

data class WorkspaceExplorerState(
    val query: String = "",
    val nodes: List<SharedContextTreeNode> = emptyList(),
    val selectedContextId: String? = null,
    val selectedContextName: String = "No context selected",
    val editingContextId: String? = null,
    val creatingContextParentId: String? = null,
    val contextDraftName: String = "",
    val contextDraftDescription: String = "",
    val contextDraftStatus: SharedContextStatus = SharedContextStatus.Planning,
    val contextDraftView: SharedContextView = SharedContextView.Backlog,
    val isSavingContext: Boolean = false,
    val deletingContextId: String? = null,
    val backlogItems: List<SharedBacklogItem> = emptyList(),
    val savingBacklogItemId: String? = null,
    val deletingBacklogItemId: String? = null,
    val editingBacklogItemId: String? = null,
    val isCreatingBacklogItem: Boolean = false,
    val backlogDraftTitle: String = "",
    val backlogDraftDetails: String = "",
    val backlogDraftPriority: SharedBacklogPriority = SharedBacklogPriority.Medium,
)
