package com.romankozak.forwardappmobile.features.context.ui.contextproperties

import com.romankozak.forwardappmobile.features.navigation.NavTarget

sealed class ProjectSettingsEvent {
    data class NavigateBack(
        val message: String? = null,
    ) : ProjectSettingsEvent()

    data class Navigate(
        val target: NavTarget,
    ) : ProjectSettingsEvent()
}
