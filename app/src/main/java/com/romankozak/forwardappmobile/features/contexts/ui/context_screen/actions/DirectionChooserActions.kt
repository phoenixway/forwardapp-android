package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.navigation.NavTarget

class DirectionChooserActions {
    data class AddDirectionRequest(
        val pendingAddDirectionFromContextChooser: Boolean,
        val savedPendingAddDirectionFromContextChooser: Boolean,
        val navigationTarget: NavTarget.ListChooser,
    )

    data class LinkDirectionRequest(
        val pendingDirectionLinkItemId: String,
        val savedPendingDirectionLink: Boolean,
        val navigationTarget: NavTarget.ListChooser,
    )

    data class ClearDirectionPendingResult(
        val pendingDirectionLinkItemId: String? = null,
        val pendingAddDirectionFromContextChooser: Boolean = false,
    )

    fun createAddDirectionRequest(disabledIds: String?): AddDirectionRequest =
        AddDirectionRequest(
            pendingAddDirectionFromContextChooser = true,
            savedPendingAddDirectionFromContextChooser = true,
            navigationTarget =
                NavTarget.ListChooser(
                    title = "Add direction to...",
                    disabledIds = disabledIds,
                ),
        )

    fun createLinkDirectionRequest(
        itemId: String,
        disabledIds: String?,
    ): LinkDirectionRequest =
        LinkDirectionRequest(
            pendingDirectionLinkItemId = itemId,
            savedPendingDirectionLink = true,
            navigationTarget =
                NavTarget.ListChooser(
                    title = "Link direction to...",
                    disabledIds = disabledIds,
                ),
        )

    fun clearPendingDirection(): ClearDirectionPendingResult = ClearDirectionPendingResult()
}
