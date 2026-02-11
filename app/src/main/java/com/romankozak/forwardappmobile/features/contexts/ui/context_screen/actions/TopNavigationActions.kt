package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.UiEvent

class TopNavigationActions {
    data class BackResult(
        val shouldClearOriginContext: Boolean,
        val events: List<UiEvent>,
    )

    fun resolveBack(
        originContextId: String?,
        currentContextId: String,
    ): BackResult {
        if (!originContextId.isNullOrBlank() && originContextId != currentContextId) {
            return BackResult(
                shouldClearOriginContext = true,
                events = listOf(UiEvent.Navigate(NavTarget.ContextDetail(contextId = originContextId))),
            )
        }

        return BackResult(
            shouldClearOriginContext = false,
            events =
                listOf(
                    UiEvent.ShowSnackbar("Повернення..."),
                    UiEvent.NavigateBack,
                ),
        )
    }

    fun homeEvent(): UiEvent = UiEvent.Navigate(NavTarget.ContextHierarchy)
}
