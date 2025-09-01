package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType

/**
 * Базовий інтерфейс-слухач для всіх обробників у GoalDetailViewModel.
 * Містить спільні методи для зворотного зв'язку з ViewModel.
 */
interface BaseHandlerResultListener {
    fun showSnackbar(message: String, action: String?)
    fun forceRefresh()
    fun requestNavigation(route: String)
    fun setPendingAction(actionType: GoalActionType, itemIds: Set<String>, goalIds: Set<String>)
    fun copyToClipboard(text: String, label: String = "Copied Text")

}