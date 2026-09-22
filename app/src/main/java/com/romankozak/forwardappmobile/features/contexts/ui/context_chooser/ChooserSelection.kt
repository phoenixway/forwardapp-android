package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef

data class ChooserSelection(
    val id: String,
    val occurrence: HierarchyOccurrenceRef?,
)
