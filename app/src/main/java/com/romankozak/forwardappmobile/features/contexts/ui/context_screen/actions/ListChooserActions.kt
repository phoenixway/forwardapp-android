package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType

class ListChooserActions(
    private val goalRepository: GoalRepository,
    private val listItemRepository: ListItemRepository,
    private val contextRepository: ContextRepository,
) {
    data class PendingActionResult(
        val newlyAddedItemId: String? = null,
    )

    suspend fun executePendingAction(
        actionType: GoalActionType,
        targetContextId: String,
        currentContextId: String,
        itemIds: List<String>,
        goalIds: List<String>,
    ): PendingActionResult {
        return when (actionType) {
            GoalActionType.CreateInstance -> {
                goalRepository.createGoalLinks(goalIds, targetContextId)
                PendingActionResult()
            }

            GoalActionType.MoveInstance -> {
                listItemRepository.moveListItemsToContext(itemIds, targetContextId)
                PendingActionResult()
            }

            GoalActionType.CopyGoal -> {
                goalRepository.copyGoalsToContext(goalIds, targetContextId)
                PendingActionResult()
            }

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
                        listItemRepository.addContextLinkToContext(subprojectToLinkId, targetContextId)
                    } else {
                        listItemRepository.addContextLinkToContext(targetContextId, currentContextId)
                    }
                PendingActionResult(newlyAddedItemId = newItemId)
            }
        }
    }
}
