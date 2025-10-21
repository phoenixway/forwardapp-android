package com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation

sealed class RevealResult {
    data class Success(val projectId: String, val shouldFocus: Boolean) : RevealResult()
    object Failure : RevealResult()
}
