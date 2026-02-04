package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.NavTarget

/**
 * Sealed class для UI подій
 */
sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val action: String? = null
    ) : UiEvent()

    data class Navigate(val target: NavTarget) : UiEvent()

    data class ResetSwipeState(val itemId: String) : UiEvent()

    data class ScrollTo(val index: Int) : UiEvent()

    data class NavigateBackAndReveal(val contextId: String) : UiEvent()

    data class HandleLinkClick(val link: RelatedLink) : UiEvent()

    data class OpenUri(val uri: String) : UiEvent()

    data object ScrollToLatestInboxRecord : UiEvent()

    data object NavigateBack : UiEvent()

    data class ShowToast(val message: String) : UiEvent()

    data class CopyToClipboard(val text: String, val label: String = "Copied") : UiEvent()

    data class ShareText(val text: String) : UiEvent()
}
