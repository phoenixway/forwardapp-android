package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import me.tatarka.inject.annotations.Inject

// TODO: [GM-31] This file needs to be refactored with the new KMP architecture.
interface BacklogMarkdownHandlerResultListener {
    fun copyToClipboard(text: String, label: String)
    fun showSnackbar(message: String, action: String?)
    fun forceRefresh()
}

@Inject
class BacklogMarkdownHandler()