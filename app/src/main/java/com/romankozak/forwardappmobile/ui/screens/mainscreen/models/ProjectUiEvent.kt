// File: ProjectUiEvent.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

/**
 * Одноразові події, що надсилаються з ViewModel до UI
 * (навігація, показ тостів, фокус на елементах тощо)
 */
sealed interface ProjectUiEvent {

    // Події навігації
    data class NavigateToDetails(val projectId: String) : ProjectUiEvent
    data class NavigateToSyncScreenWithData(val json: String) : ProjectUiEvent
    data class NavigateToGlobalSearch(val query: String) : ProjectUiEvent
    data object NavigateToSettings : ProjectUiEvent
    data class NavigateToEditProjectScreen(val projectId: String) : ProjectUiEvent
    data class Navigate(val route: String) : ProjectUiEvent
    data class NavigateToDayPlan(val date: Long) : ProjectUiEvent

    // Події UI
    data class ShowToast(val message: String) : ProjectUiEvent
    data object FocusSearchField : ProjectUiEvent
    data class ScrollToIndex(val index: Int) : ProjectUiEvent
}