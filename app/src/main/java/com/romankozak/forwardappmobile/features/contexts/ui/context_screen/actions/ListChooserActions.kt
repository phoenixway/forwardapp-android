package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogPasteMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType

class ListChooserActions(
    private val listItemRepository: ListItemRepository,
    private val contextRepository: ContextRepository,
    private val backlogClipboardUseCase: BacklogClipboardUseCase,
) {
    data class PendingActionNavigation(
        val target: NavTarget.ListChooser,
    )

    data class PendingActionResult(
        val newlyAddedItemId: String? = null,
        val userMessage: String? = null,
    )

    fun buildPendingActionNavigation(
        actionType: GoalActionType,
        currentContextId: String,
    ): PendingActionNavigation {
        val title =
            when (actionType) {
                GoalActionType.CreateInstance -> "Create link in..."
                GoalActionType.MoveInstance -> "Move to..."
                GoalActionType.CopyGoal -> "Copy to..."
                GoalActionType.AddLinkToList -> "Add link to context..."
                GoalActionType.ADD_LIST_SHORTCUT -> "Add context shortcut..."
            }
        return PendingActionNavigation(
            target =
                NavTarget.ListChooser(
                    title = title,
                    disabledIds = currentContextId.ifBlank { null },
                ),
        )
    }

    fun buildAttachmentShareNavigation(currentContextId: String): NavTarget.ListChooser =
        NavTarget.ListChooser(
            title = "Select context for attachment",
            disabledIds = currentContextId.ifBlank { null },
        )

    suspend fun executePendingAction(
        actionType: GoalActionType,
        targetContextId: String,
        currentContextId: String,
        itemIds: List<String>,
        goalIds: List<String>,
    ): PendingActionResult {
        return when (actionType) {
            GoalActionType.CreateInstance ->
                copyAndPasteGoals(
                    sourceContextId = currentContextId,
                    goalIds = goalIds,
                    targetContextId = targetContextId,
                    mode = BacklogPasteMode.AS_LINK,
                )

            GoalActionType.MoveInstance ->
                cutAndPasteGoals(
                    sourceContextId = currentContextId,
                    itemIds = itemIds,
                    targetContextId = targetContextId,
                )

            GoalActionType.CopyGoal ->
                copyAndPasteGoals(
                    sourceContextId = currentContextId,
                    goalIds = goalIds,
                    targetContextId = targetContextId,
                    mode = BacklogPasteMode.AS_CLONE,
                )

            GoalActionType.AddLinkToList -> {
                val targetProject = contextRepository.getContextById(targetContextId)
                val link =
                    RelatedLink(
                        type = LinkType.CONTEXT,
                        target = targetContextId,
                        displayName = targetProject?.name ?: "Untitled context",
                    )
                val newItemId = contextRepository.addLinkItemToContextFromLink(currentContextId, link)
                PendingActionResult(newlyAddedItemId = newItemId)
            }

            GoalActionType.ADD_LIST_SHORTCUT -> {
                val newItemId =
                    if (goalIds.isNotEmpty()) {
                        val subprojectToLinkId = goalIds.first()
                        contextRepository.addContextLinkToContext(subprojectToLinkId, targetContextId)
                    } else {
                        contextRepository.addContextLinkToContext(targetContextId, currentContextId)
                    }
                PendingActionResult(newlyAddedItemId = newItemId)
            }
        }
    }

    private suspend fun copyAndPasteGoals(
        sourceContextId: String,
        goalIds: List<String>,
        targetContextId: String,
        mode: BacklogPasteMode,
    ): PendingActionResult {
        backlogClipboardUseCase.copyBacklogGoals(
            sourceContextId = sourceContextId,
            goalIds = goalIds,
        )
        return buildPasteResult(targetContextId = targetContextId, mode = mode)
    }

    private suspend fun cutAndPasteGoals(
        sourceContextId: String,
        itemIds: List<String>,
        targetContextId: String,
    ): PendingActionResult {
        backlogClipboardUseCase.cutBacklogGoals(
            sourceContextId = sourceContextId,
            listItemIds = itemIds,
        )
        return buildPasteResult(
            targetContextId = targetContextId,
            mode = BacklogPasteMode.AS_LINK,
        )
    }

    private suspend fun buildPasteResult(
        targetContextId: String,
        mode: BacklogPasteMode,
    ): PendingActionResult {
        val report =
            backlogClipboardUseCase.pasteBacklogGoals(
                targetContextId = targetContextId,
                mode = mode,
            )
        return PendingActionResult(userMessage = report.toUserMessage())
    }
}
