package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.navigation

sealed class RevealResult {
    data class Success(
        val projectId: String,
        val shouldFocus: Boolean,
        val placementId: String? = null,
    ) : RevealResult()

    object Failure : RevealResult()
}
