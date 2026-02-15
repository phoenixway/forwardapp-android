package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class BacklogPasteMode {
    AS_LINK,
    AS_CLONE,
}

data class BacklogPasteReport(
    val totalRequested: Int,
    val moved: Int = 0,
    val createdLinks: Int = 0,
    val clonedGoals: Int = 0,
    val skippedDuplicates: Int = 0,
    val skippedInvalid: Int = 0,
) {
    val changedCount: Int get() = moved + createdLinks + clonedGoals

    fun toUserMessage(): String {
        val parts = mutableListOf<String>()
        if (moved > 0) parts += "переміщено: $moved"
        if (createdLinks > 0) parts += "додано посилань: $createdLinks"
        if (clonedGoals > 0) parts += "клоновано цілей: $clonedGoals"
        if (skippedDuplicates > 0) parts += "дублікати пропущено: $skippedDuplicates"
        if (skippedInvalid > 0) parts += "некоректних пропущено: $skippedInvalid"
        return if (parts.isEmpty()) "Немає змін" else parts.joinToString(", ")
    }
}

@Singleton
class BacklogClipboardUseCase
    @Inject
    constructor(
        private val clipboardService: EntityClipboardService,
        private val goalRepository: GoalRepository,
        private val listItemRepository: ListItemRepository,
    ) {
        val clipboardPayload: StateFlow<EntityClipboardPayload?> get() = clipboardService.payload

        fun hasPayload(): Boolean = clipboardService.payload.value != null

        fun isCopyOperation(): Boolean = clipboardService.payload.value?.operation == ClipboardOperation.COPY

        fun isCutOperation(): Boolean = clipboardService.payload.value?.operation == ClipboardOperation.CUT

        fun clearClipboard() {
            clipboardService.clear()
        }

        fun canPasteIntoBacklog(targetContextId: String): Boolean {
            if (targetContextId.isBlank()) return false
            val payload = clipboardService.payload.value ?: return false
            return when (payload.operation) {
                ClipboardOperation.COPY ->
                    payload.entities.any {
                        it is ClipboardEntityRef.BacklogGoal || it is ClipboardEntityRef.BacklogContextLink
                    }
                ClipboardOperation.CUT ->
                    payload.sourceContextId != targetContextId &&
                        payload.entities.any { it is ClipboardEntityRef.BacklogItem }
            }
        }

        fun copyPayloadHasGoals(): Boolean =
            clipboardService.payload.value
                ?.takeIf { it.operation == ClipboardOperation.COPY }
                ?.entities
                ?.any { it is ClipboardEntityRef.BacklogGoal } == true

        fun copyBacklogGoals(
            sourceContextId: String,
            goalIds: List<String>,
        ) {
            val refs = goalIds.distinct().map { ClipboardEntityRef.BacklogGoal(goalId = it) }
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun copyBacklogContextLinks(
            sourceContextId: String,
            contextIds: List<String>,
        ) {
            val refs = contextIds.distinct().map { ClipboardEntityRef.BacklogContextLink(contextId = it) }
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun copyBacklogEntities(
            sourceContextId: String,
            goalIds: List<String>,
            contextIds: List<String>,
        ) {
            val refs =
                buildList {
                    addAll(goalIds.distinct().map { ClipboardEntityRef.BacklogGoal(goalId = it) })
                    addAll(contextIds.distinct().map { ClipboardEntityRef.BacklogContextLink(contextId = it) })
                }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun cutBacklogGoals(
            sourceContextId: String,
            listItemIds: List<String>,
        ) {
            val refs = listItemIds.distinct().map { ClipboardEntityRef.BacklogItem(listItemId = it) }
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.CUT,
                    entities = refs,
                ),
            )
        }

        suspend fun pasteBacklogGoals(
            targetContextId: String,
            mode: BacklogPasteMode,
        ): BacklogPasteReport {
            val payload = clipboardService.payload.value ?: return BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)

            return when (payload.operation) {
                ClipboardOperation.COPY -> {
                    val goalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    if (goalRefs.isEmpty() && contextRefs.isEmpty()) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopy(goalRefs, contextRefs, targetContextId, mode)
                    }
                }
                ClipboardOperation.CUT -> {
                    val itemRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>()
                    if (itemRefs.isEmpty()) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCut(itemRefs, payload.sourceContextId, targetContextId)
                    }
                }
            }
        }

        private suspend fun pasteCopy(
            refs: List<ClipboardEntityRef.BacklogGoal>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            targetContextId: String,
            mode: BacklogPasteMode,
        ): BacklogPasteReport {
            val sourceGoalIds = refs.map { it.goalId }.distinct()
            val sourceContextIds = contextRefs.map { it.contextId }.distinct()
            val totalRequested = sourceGoalIds.size + sourceContextIds.size

            val contextIdsToLink = sourceContextIds.filterNot { listItemRepository.doesLinkExist(it, targetContextId) }
            val contextDuplicates = sourceContextIds.size - contextIdsToLink.size

            return when (mode) {
                BacklogPasteMode.AS_CLONE -> {
                    goalRepository.copyGoalsToContext(sourceGoalIds, targetContextId)
                    contextIdsToLink.forEach { contextId ->
                        listItemRepository.addContextLinkToContext(
                            targetContextId = contextId,
                            currentContextId = targetContextId,
                        )
                    }
                    BacklogPasteReport(
                        totalRequested = totalRequested,
                        clonedGoals = sourceGoalIds.size,
                        createdLinks = contextIdsToLink.size,
                        skippedDuplicates = contextDuplicates,
                    )
                }

                BacklogPasteMode.AS_LINK -> {
                    val nonDuplicates = sourceGoalIds.filterNot { listItemRepository.doesLinkExist(it, targetContextId) }
                    goalRepository.createGoalLinks(nonDuplicates, targetContextId)
                    contextIdsToLink.forEach { contextId ->
                        listItemRepository.addContextLinkToContext(
                            targetContextId = contextId,
                            currentContextId = targetContextId,
                        )
                    }
                    BacklogPasteReport(
                        totalRequested = totalRequested,
                        createdLinks = nonDuplicates.size + contextIdsToLink.size,
                        skippedDuplicates = (sourceGoalIds.size - nonDuplicates.size) + contextDuplicates,
                    )
                }
            }
        }

        private suspend fun pasteCut(
            refs: List<ClipboardEntityRef.BacklogItem>,
            sourceContextId: String,
            targetContextId: String,
        ): BacklogPasteReport {
            if (sourceContextId == targetContextId) {
                return BacklogPasteReport(totalRequested = refs.size, skippedDuplicates = refs.size)
            }

            val requestedItemIds = refs.map { it.listItemId }
            val itemsById = listItemRepository.getItemsByIds(requestedItemIds).associateBy { it.id }
            val items = requestedItemIds.mapNotNull(itemsById::get)
            if (items.isEmpty()) return BacklogPasteReport(totalRequested = requestedItemIds.size, skippedInvalid = requestedItemIds.size)

            val (movable, duplicateBlocked) =
                partitionMovableItems(
                    items = items,
                    targetContextId = targetContextId,
                )
            listItemRepository.moveListItemsToContext(movable.map { it.id }, targetContextId)
            clipboardService.clear()
            return BacklogPasteReport(
                totalRequested = requestedItemIds.size,
                moved = movable.size,
                skippedDuplicates = duplicateBlocked,
                skippedInvalid = requestedItemIds.size - items.size,
            )
        }

        private suspend fun partitionMovableItems(
            items: List<BacklogItem>,
            targetContextId: String,
        ): Pair<List<BacklogItem>, Int> {
            val movable = mutableListOf<BacklogItem>()
            var duplicateBlocked = 0
            for (item in items) {
                val duplicateExists = listItemRepository.doesLinkExist(item.entityId, targetContextId)
                if (duplicateExists) {
                    duplicateBlocked += 1
                } else {
                    movable += item
                }
            }
            return movable to duplicateBlocked
        }
    }
