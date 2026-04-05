package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel

interface BaseHandlerResultListener {
    fun showSnackbar(
        message: String,
        action: String?,
    )

    fun applyOptimisticDeletion(items: List<com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent>)

    fun forceRefresh()

    fun copyToClipboard(
        text: String,
        label: String = "Copied Text",
    )
}

typealias BaseHandler = BaseHandlerResultListener
