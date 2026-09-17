package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository

/** Creates the root standalone Workspace returned by the shared picker callback. */
internal suspend fun CanonicalWorkspaceRepository.createRootWorkspaceForPicker(name: String): String? {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return null
    return create(
        nameOverride = trimmed,
        descriptionOverride = null,
        parentWorkspaceId = null,
        roleCode = null,
    )
}
