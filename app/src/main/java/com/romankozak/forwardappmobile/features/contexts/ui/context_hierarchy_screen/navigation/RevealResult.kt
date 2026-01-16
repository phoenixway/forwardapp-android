package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.navigation

sealed class RevealResult {
    data class Success(val projectId: String, val shouldFocus: Boolean) : RevealResult()
    object Failure : RevealResult()
}
