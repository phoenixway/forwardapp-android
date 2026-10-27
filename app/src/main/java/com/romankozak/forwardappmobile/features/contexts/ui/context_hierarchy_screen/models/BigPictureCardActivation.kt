package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

/** Sections are locations; for entity occurrences only non-leaves need hierarchy focus. */
internal fun activateBigPictureOccurrence(
    node: BigPictureNode,
    directChildCount: Int,
    onFocus: (String?) -> Unit,
    onOpen: (BigPictureNode) -> Unit,
) {
    if (directChildCount > 0) onFocus(node.placement.id.value) else onOpen(node)
}
