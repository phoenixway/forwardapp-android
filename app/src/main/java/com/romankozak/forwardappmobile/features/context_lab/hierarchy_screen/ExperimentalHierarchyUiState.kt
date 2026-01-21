package com.romankozak.forwardappmobile.features.context_lab.hierarchy_screen

import com.romankozak.forwardappmobile.core.context.Context
import com.romankozak.forwardappmobile.core.context.ContextId

data class ExperimentalHierarchyUiState(
    val contexts: List<Context> = emptyList(),
    val activeContextId: ContextId? = null
)
