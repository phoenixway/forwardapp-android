
package com.romankozak.forwardappmobile.ui.screens.mainscreen.models


sealed interface ProjectUiEvent {
    
    data class NavigateToDetails(val projectId: String) : ProjectUiEvent

    data class NavigateToSyncScreenWithData(val json: String) : ProjectUiEvent

    data class NavigateToGlobalSearch(val query: String) : ProjectUiEvent

    data object NavigateToSettings : ProjectUiEvent

    data class NavigateToEditProjectScreen(val projectId: String) : ProjectUiEvent

    data class Navigate(val route: String) : ProjectUiEvent

    data class NavigateToDayPlan(val date: Long, val startTab: String? = null) : ProjectUiEvent

    
    data class ShowToast(val message: String) : ProjectUiEvent

    data object FocusSearchField : ProjectUiEvent
    data object HideKeyboard : ProjectUiEvent

    data class OpenUri(val uri: String) : ProjectUiEvent
    data class ScrollToIndex(val index: Int) : ProjectUiEvent
    data object NavigateToStrategicManagement : ProjectUiEvent
}
