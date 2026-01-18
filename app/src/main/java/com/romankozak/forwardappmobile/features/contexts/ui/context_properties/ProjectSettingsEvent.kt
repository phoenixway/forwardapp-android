package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.romankozak.forwardappmobile.core.navigation.NavTarget

sealed class ProjectSettingsEvent {
    data class NavigateBack(
        val message: String? = null,
    ) : ProjectSettingsEvent()

    data class Navigate(
        val target: NavTarget,
    ) : ProjectSettingsEvent()
}
