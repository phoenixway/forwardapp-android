package com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel

import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType

interface BaseHandlerResultListener {
    fun showSnackbar(
        message: String,
        action: String?,
    )

    fun forceRefresh()

    fun requestNavigation(route: String)

    fun setPendingAction(
        actionType: GoalActionType,
        itemIds: Set<String>,
        goalIds: Set<String>,
    )

    fun copyToClipboard(
        text: String,
        label: String = "Copied Text",
    )
}
