package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.romankozak.forwardappmobile.core.navigation.NavTarget

sealed class ContextSettingsEvent {
    data class NavigateBack(
        val message: String? = null,
    ) : ContextSettingsEvent()

    data class Navigate(
        val target: NavTarget,
    ) : ContextSettingsEvent()
}
