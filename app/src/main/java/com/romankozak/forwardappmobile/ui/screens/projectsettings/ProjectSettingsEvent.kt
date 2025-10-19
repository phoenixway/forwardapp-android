package com.romankozak.forwardappmobile.ui.screens.projectsettings

sealed class ProjectSettingsEvent {
    data class NavigateBack(
        val message: String? = null,
    ) : ProjectSettingsEvent()

    data class Navigate(
        val route: String,
    ) : ProjectSettingsEvent()
}