package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    val createdGoals: Int = 0,
    val createdDirectionItems: Int = 0,
    val createdAttachments: Int = 0,
    val skippedDuplicates: Int = 0,
    val skippedInvalid: Int = 0,
) {
    val changedCount: Int
        get() = moved + createdLinks + clonedGoals + createdGoals + createdDirectionItems + createdAttachments

    fun toUserMessage(): String {
        val parts = mutableListOf<String>()
        if (moved > 0) parts += "переміщено: $moved"
        if (createdLinks > 0) parts += "додано посилань: $createdLinks"
        if (clonedGoals > 0) parts += "клоновано цілей: $clonedGoals"
        if (createdGoals > 0) parts += "створено цілей: $createdGoals"
        if (createdDirectionItems > 0) parts += "додано елементів напрямку: $createdDirectionItems"
        if (createdAttachments > 0) parts += "додано вкладень: $createdAttachments"
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
        private val directionRepository: DirectionRepository,
        private val contextRepository: ContextRepository,
        private val checklistRepository: ChecklistRepository,
    ) {
        val clipboardPayload: StateFlow<EntityClipboardPayload?> get() = clipboardService.payload

        fun hasPayload(): Boolean = clipboardService.payload.value != null

        fun isCopyOperation(): Boolean = clipboardService.payload.value?.operation == ClipboardOperation.COPY

        fun isCutOperation(): Boolean = clipboardService.payload.value?.operation == ClipboardOperation.CUT

        fun clearClipboard() {
            clipboardService.clear()
        }

        private fun areAllEntityGroupsEmpty(vararg entityGroups: List<*>): Boolean = entityGroups.all(List<*>::isEmpty)

        private fun invalidPasteReport(): BacklogPasteReport =
            BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)

        private inline fun canPasteToTarget(
            targetContextId: String,
            block: (EntityClipboardPayload) -> Boolean,
        ): Boolean =
            targetContextId.isNotBlank() &&
                clipboardService.payload.value?.let(block) == true

        private fun EntityClipboardPayload.hasCopyableBacklogEntities(): Boolean =
            entities.any {
                it is ClipboardEntityRef.BacklogGoal ||
                    it is ClipboardEntityRef.BacklogContextLink ||
                    it is ClipboardEntityRef.DirectionItem ||
                    it is ClipboardEntityRef.BacklogAttachment ||
                    it is ClipboardEntityRef.ChecklistItem
            }

        fun canPasteIntoBacklog(targetContextId: String): Boolean {
            return canPasteToTarget(targetContextId) { payload ->
                val hasBacklogItemCut = payload.entities.any { it is ClipboardEntityRef.BacklogItem }
                val hasContextLinkRef = payload.entities.any { it is ClipboardEntityRef.BacklogContextLink }
                val hasDirectionItemCut = payload.entities.any { it is ClipboardEntityRef.DirectionItem }
                val hasAttachmentRef = payload.entities.any { it is ClipboardEntityRef.BacklogAttachment }
                val hasChecklistItem = payload.entities.any { it is ClipboardEntityRef.ChecklistItem }
                when (payload.operation) {
                    ClipboardOperation.COPY -> payload.hasCopyableBacklogEntities()
                    ClipboardOperation.CUT ->
                        (hasBacklogItemCut && payload.sourceContextId != targetContextId) ||
                            hasContextLinkRef ||
                            hasDirectionItemCut ||
                            hasAttachmentRef ||
                            hasChecklistItem
                }
            }
        }

        fun canPasteIntoDirection(targetContextId: String): Boolean {
            return canPasteToTarget(targetContextId) { payload ->
                val hasBacklogItemCut = payload.entities.any { it is ClipboardEntityRef.BacklogItem }
                val hasContextLinkRef = payload.entities.any { it is ClipboardEntityRef.BacklogContextLink }
                val hasDirectionItemCut = payload.entities.any { it is ClipboardEntityRef.DirectionItem }
                val hasAttachmentRef = payload.entities.any { it is ClipboardEntityRef.BacklogAttachment }
                val hasChecklistItem = payload.entities.any { it is ClipboardEntityRef.ChecklistItem }
                when (payload.operation) {
                    ClipboardOperation.COPY -> payload.hasCopyableBacklogEntities()
                    ClipboardOperation.CUT ->
                        hasBacklogItemCut ||
                            hasContextLinkRef ||
                            (hasDirectionItemCut && payload.sourceContextId != targetContextId) ||
                            hasAttachmentRef ||
                            hasChecklistItem
                }
            }
        }

        fun canPasteContextLinksIntoBacklog(targetContextId: String): Boolean {
            return canPasteToTarget(targetContextId) { payload ->
                payload.entities.any { it is ClipboardEntityRef.BacklogContextLink } &&
                    canPasteIntoBacklog(targetContextId)
            }
        }

        fun canPasteIntoAttachments(targetContextId: String): Boolean {
            return canPasteToTarget(targetContextId) { payload ->
                payload.entities.any {
                    it is ClipboardEntityRef.BacklogAttachment || it is ClipboardEntityRef.BacklogContextLink
                }
            }
        }

        fun hasAttachmentRefsInClipboard(): Boolean {
            return clipboardService.payload.value?.entities?.any { it is ClipboardEntityRef.BacklogAttachment } == true
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

        fun cutBacklogContextLinks(
            sourceContextId: String,
            contextIds: List<String>,
        ) {
            val refs = contextIds.distinct().map { ClipboardEntityRef.BacklogContextLink(contextId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.CUT,
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

        fun copyDirectionItems(
            sourceContextId: String,
            itemIds: List<String>,
        ) {
            val refs = itemIds.distinct().map { ClipboardEntityRef.DirectionItem(directionItemId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun cutDirectionItems(
            sourceContextId: String,
            itemIds: List<String>,
        ) {
            val refs = itemIds.distinct().map { ClipboardEntityRef.DirectionItem(directionItemId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.CUT,
                    entities = refs,
                ),
            )
        }

        fun copyAttachmentItems(
            sourceContextId: String,
            listItemIds: List<String>,
        ) {
            val refs = listItemIds.distinct().map { ClipboardEntityRef.BacklogAttachment(listItemId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun cutAttachmentItems(
            sourceContextId: String,
            listItemIds: List<String>,
        ) {
            val refs = listItemIds.distinct().map { ClipboardEntityRef.BacklogAttachment(listItemId = it) }
            if (refs.isEmpty()) return
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
            includeAttachments: Boolean = false,
            addSourceContextLinkForGoalLinks: Boolean = false,
        ): BacklogPasteReport {
            val payload = clipboardService.payload.value ?: return invalidPasteReport()

            return when (payload.operation) {
                ClipboardOperation.COPY -> {
                    val goalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    val checklistRefs = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>()
                    if (areAllEntityGroupsEmpty(goalRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopyToBacklog(
                            goalRefs = goalRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            targetContextId = targetContextId,
                            mode = mode,
                            includeAttachments = includeAttachments,
                            addSourceContextLinkForGoalLinks = addSourceContextLinkForGoalLinks,
                            sourceContextId = payload.sourceContextId,
                        )
                    }
                }

                ClipboardOperation.CUT -> {
                    val itemRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    val checklistRefs = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>()
                    if (areAllEntityGroupsEmpty(itemRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCutToBacklog(
                            itemRefs = itemRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            sourceContextId = payload.sourceContextId,
                            targetContextId = targetContextId,
                            mode = mode,
                            includeAttachments = includeAttachments,
                        )
                    }
                }
            }
        }

        suspend fun pasteIntoDirection(
            targetContextId: String,
            includeAttachments: Boolean = false,
        ): BacklogPasteReport {
            val payload = clipboardService.payload.value ?: return invalidPasteReport()

            return when (payload.operation) {
                ClipboardOperation.COPY -> {
                    val goalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    val checklistRefs = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>()
                    if (areAllEntityGroupsEmpty(goalRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopyToDirection(
                            goalRefs = goalRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            targetContextId = targetContextId,
                            includeAttachments = includeAttachments,
                        )
                    }
                }

                ClipboardOperation.CUT -> {
                    val itemRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    val checklistRefs = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>()
                    if (areAllEntityGroupsEmpty(itemRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCutToDirection(
                            itemRefs = itemRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            sourceContextId = payload.sourceContextId,
                            targetContextId = targetContextId,
                            includeAttachments = includeAttachments,
                        )
                    }
                }
            }
        }

        suspend fun pasteIntoAttachments(targetContextId: String): BacklogPasteReport {
            val payload = clipboardService.payload.value
            return payload?.let { currentPayload ->
                val attachmentRefs = currentPayload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                val contextRefs = currentPayload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                if (attachmentRefs.isEmpty() && contextRefs.isEmpty()) {
                    invalidPasteReport()
                } else {
                    when (currentPayload.operation) {
                        ClipboardOperation.COPY -> {
                            val attachmentCopyResult =
                                copyAttachmentRefsToContext(
                                    refs = attachmentRefs,
                                    targetContextId = targetContextId,
                                    sourceContextId = currentPayload.sourceContextId,
                                )
                            val contextLinkCopyResult =
                                copyContextLinkRefsToContextAttachments(
                                    refs = contextRefs,
                                    targetContextId = targetContextId,
                                )
                            buildAttachmentPasteReport(
                                attachmentRefs = attachmentRefs,
                                contextRefs = contextRefs,
                                attachmentCopyResult = attachmentCopyResult,
                                contextLinkCopyResult = contextLinkCopyResult,
                            )
                        }

                        ClipboardOperation.CUT -> {
                            val attachmentCopyResult =
                                copyAttachmentRefsToContext(
                                    refs = attachmentRefs,
                                    targetContextId = targetContextId,
                                    sourceContextId = currentPayload.sourceContextId,
                                )
                            val contextLinkCopyResult =
                                copyContextLinkRefsToContextAttachments(
                                    refs = contextRefs,
                                    targetContextId = targetContextId,
                                )
                            val movedCount =
                                finalizeAttachmentCut(currentPayload, attachmentCopyResult, contextLinkCopyResult)
                            buildAttachmentPasteReport(
                                attachmentRefs = attachmentRefs,
                                contextRefs = contextRefs,
                                attachmentCopyResult = attachmentCopyResult,
                                contextLinkCopyResult = contextLinkCopyResult,
                                moved = movedCount,
                            )
                        }
                    }
                }
            } ?: invalidPasteReport()
        }

        private suspend fun finalizeAttachmentCut(
            payload: EntityClipboardPayload,
            attachmentCopyResult: AttachmentCopyResult,
            contextLinkCopyResult: ContextLinkAttachmentCopyResult,
        ): Int {
            if (attachmentCopyResult.insertedSourceItemIds.isNotEmpty()) {
                listItemRepository.deleteListItems(attachmentCopyResult.insertedSourceItemIds)
            }
            if (payload.sourceContextId.isNotBlank() && attachmentCopyResult.sourceAttachmentIdsForCut.isNotEmpty()) {
                attachmentCopyResult.sourceAttachmentIdsForCut.forEach { attachmentId ->
                    contextRepository.unlinkAttachmentFromContext(
                        contextId = payload.sourceContextId,
                        attachmentId = attachmentId,
                    )
                }
            }
            val movedCount =
                attachmentCopyResult.insertedSourceItemIds.size + attachmentCopyResult.sourceAttachmentIdsForCut.size
            if (movedCount > 0 || contextLinkCopyResult.created > 0) {
                clipboardService.clear()
            }
            return movedCount
        }

        private fun buildAttachmentPasteReport(
            attachmentRefs: List<ClipboardEntityRef.BacklogAttachment>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            attachmentCopyResult: AttachmentCopyResult,
            contextLinkCopyResult: ContextLinkAttachmentCopyResult,
            moved: Int = 0,
        ): BacklogPasteReport =
            BacklogPasteReport(
                totalRequested = attachmentRefs.size + contextRefs.size,
                moved = moved,
                createdAttachments = attachmentCopyResult.created + contextLinkCopyResult.created,
                skippedDuplicates = attachmentCopyResult.duplicates + contextLinkCopyResult.duplicates,
                skippedInvalid = attachmentCopyResult.invalid + contextLinkCopyResult.invalid,
            )

        private suspend fun pasteCopyToBacklog(
            goalRefs: List<ClipboardEntityRef.BacklogGoal>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            directionRefs: List<ClipboardEntityRef.DirectionItem>,
            attachmentRefs: List<ClipboardEntityRef.BacklogAttachment>,
            checklistRefs: List<ClipboardEntityRef.ChecklistItem>,
            targetContextId: String,
            mode: BacklogPasteMode,
            includeAttachments: Boolean,
            addSourceContextLinkForGoalLinks: Boolean,
            sourceContextId: String,
        ): BacklogPasteReport {
            val sourceGoalIds = goalRefs.map { it.goalId }.distinct()
            val sourceContextIds = contextRefs.map { it.contextId }.distinct()
            val directionItems = loadDirectionItems(directionRefs)
            var createdLinks = 0
            var clonedGoals = 0
            var createdGoals = 0
            var createdAttachments = 0
            var skippedDuplicates = 0
            var skippedInvalid = directionRefs.size - directionItems.size
            val checklistItems = loadChecklistItems(checklistRefs)
            skippedInvalid += checklistRefs.size - checklistItems.size

            val contextIdsToLink =
                sourceContextIds
                    .filter { it != targetContextId }
                    .filterNot { listItemRepository.doesLinkExist(it, targetContextId) }
            val contextDuplicates = sourceContextIds.size - contextIdsToLink.size

            when (mode) {
                BacklogPasteMode.AS_CLONE -> {
                    goalRepository.copyGoalsToContext(sourceGoalIds, targetContextId)
                    clonedGoals += sourceGoalIds.size
                }

                BacklogPasteMode.AS_LINK -> {
                    val nonDuplicates =
                        sourceGoalIds.filterNot { listItemRepository.doesLinkExist(it, targetContextId) }
                    goalRepository.createGoalLinks(nonDuplicates, targetContextId)
                    createdLinks += nonDuplicates.size
                    skippedDuplicates += sourceGoalIds.size - nonDuplicates.size
                    if (addSourceContextLinkForGoalLinks) {
                        addSourceContextLinkToGoals(
                            goalIds = nonDuplicates,
                            sourceContextId = sourceContextId,
                            targetContextId = targetContextId,
                        )
                    }
                }
            }

            contextIdsToLink.forEach { contextId ->
                listItemRepository.addContextLinkToContext(
                    targetContextId = contextId,
                    currentContextId = targetContextId,
                )
                createdLinks += 1
            }
            skippedDuplicates += contextDuplicates

            val existingGoalTexts = getExistingBacklogGoalTexts(targetContextId).toMutableSet()
            for (directionItem in directionItems) {
                val linkedContextId = directionItem.linkedContextId
                if (!linkedContextId.isNullOrBlank()) {
                    val isDuplicateLink =
                        linkedContextId == targetContextId ||
                            listItemRepository.doesLinkExist(linkedContextId, targetContextId)
                    if (isDuplicateLink) {
                        skippedDuplicates += 1
                    } else {
                        listItemRepository.addContextLinkToContext(
                            targetContextId = linkedContextId,
                            currentContextId = targetContextId,
                        )
                        createdLinks += 1
                    }
                } else {
                    val normalizedText = normalizeText(directionItem.text)
                    if (normalizedText.isBlank()) {
                        skippedInvalid += 1
                    } else if (normalizedText in existingGoalTexts) {
                        skippedDuplicates += 1
                    } else {
                        goalRepository.addGoalToContext(directionItem.text.trim(), targetContextId)
                        existingGoalTexts += normalizedText
                        createdGoals += 1
                    }
                }
            }

            checklistItems.forEach { checklistItem ->
                val normalizedText = normalizeText(checklistItem.content)
                if (normalizedText.isBlank()) {
                    skippedInvalid += 1
                    return@forEach
                }
                if (normalizedText in existingGoalTexts) {
                    skippedDuplicates += 1
                    return@forEach
                }
                goalRepository.addGoalToContext(checklistItem.content.trim(), targetContextId)
                existingGoalTexts += normalizedText
                createdGoals += 1
            }

            if (includeAttachments && attachmentRefs.isNotEmpty()) {
                val attachmentCopy =
                    copyAttachmentRefsToContext(
                        refs = attachmentRefs,
                        targetContextId = targetContextId,
                        sourceContextId = null,
                    )
                createdAttachments += attachmentCopy.created
                skippedDuplicates += attachmentCopy.duplicates
                skippedInvalid += attachmentCopy.invalid
            }

            return BacklogPasteReport(
                totalRequested =
                    sourceGoalIds.size +
                        sourceContextIds.size +
                        directionRefs.size +
                        attachmentRefs.size +
                        checklistRefs.size,
                createdLinks = createdLinks,
                clonedGoals = clonedGoals,
                createdGoals = createdGoals,
                createdAttachments = createdAttachments,
                skippedDuplicates = skippedDuplicates,
                skippedInvalid = skippedInvalid,
            )
        }

        private suspend fun pasteCutToBacklog(
            itemRefs: List<ClipboardEntityRef.BacklogItem>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            directionRefs: List<ClipboardEntityRef.DirectionItem>,
            attachmentRefs: List<ClipboardEntityRef.BacklogAttachment>,
            checklistRefs: List<ClipboardEntityRef.ChecklistItem>,
            sourceContextId: String,
            targetContextId: String,
            mode: BacklogPasteMode,
            includeAttachments: Boolean,
        ): BacklogPasteReport {
            var moved = 0
            var createdLinks = 0
            var clonedGoals = 0
            var createdGoals = 0
            var createdAttachments = 0
            var skippedDuplicates = 0
            var skippedInvalid = 0
            val checklistItems = loadChecklistItems(checklistRefs)
            skippedInvalid += checklistRefs.size - checklistItems.size
            val checklistItemsToDelete = mutableListOf<String>()

            if (contextRefs.isNotEmpty()) {
                val sourceContextIds = contextRefs.map { it.contextId }.distinct()
                val contextIdsToLink =
                    sourceContextIds
                        .filter { it != targetContextId }
                        .filterNot { listItemRepository.doesLinkExist(it, targetContextId) }
                val contextDuplicates = sourceContextIds.size - contextIdsToLink.size

                contextIdsToLink.forEach { contextId ->
                    listItemRepository.addContextLinkToContext(
                        targetContextId = contextId,
                        currentContextId = targetContextId,
                    )
                    createdLinks += 1
                }
                skippedDuplicates += contextDuplicates
            }

            if (itemRefs.isNotEmpty()) {
                if (sourceContextId == targetContextId) {
                    skippedDuplicates += itemRefs.size
                } else {
                    val requestedItemIds = itemRefs.map { it.listItemId }
                    val itemsById = listItemRepository.getItemsByIds(requestedItemIds).associateBy { it.id }
                    val items = requestedItemIds.mapNotNull(itemsById::get)
                    skippedInvalid += requestedItemIds.size - items.size

                    val (movable, duplicateBlocked) =
                        partitionMovableBacklogItems(
                            items = items,
                            targetContextId = targetContextId,
                        )
                    listItemRepository.moveListItemsToContext(movable.map { it.id }, targetContextId)
                    moved += movable.size
                    skippedDuplicates += duplicateBlocked
                }
            }

            if (directionRefs.isNotEmpty()) {
                val directionItems = loadDirectionItems(directionRefs)
                skippedInvalid += directionRefs.size - directionItems.size
                val existingGoalTexts = getExistingBacklogGoalTexts(targetContextId).toMutableSet()
                val insertedDirectionIds = mutableListOf<String>()

                for (directionItem in directionItems) {
                    val linkedContextId = directionItem.linkedContextId
                    if (!linkedContextId.isNullOrBlank()) {
                        val isDuplicateLink =
                            linkedContextId == targetContextId ||
                                listItemRepository.doesLinkExist(linkedContextId, targetContextId)
                        if (isDuplicateLink) {
                            skippedDuplicates += 1
                        } else {
                            listItemRepository.addContextLinkToContext(
                                targetContextId = linkedContextId,
                                currentContextId = targetContextId,
                            )
                            createdLinks += 1
                            insertedDirectionIds += directionItem.id
                        }
                    } else {
                        val normalizedText = normalizeText(directionItem.text)
                        if (normalizedText.isBlank()) {
                            skippedInvalid += 1
                        } else if (normalizedText in existingGoalTexts) {
                            skippedDuplicates += 1
                        } else {
                            when (mode) {
                                BacklogPasteMode.AS_CLONE,
                                BacklogPasteMode.AS_LINK,
                                -> {
                                    goalRepository.addGoalToContext(directionItem.text.trim(), targetContextId)
                                    existingGoalTexts += normalizedText
                                    createdGoals += 1
                                    insertedDirectionIds += directionItem.id
                                }
                            }
                        }
                    }
                }

                if (insertedDirectionIds.isNotEmpty()) {
                    directionRepository.deleteDirectionItems(insertedDirectionIds)
                }
            }

            if (checklistItems.isNotEmpty()) {
                val existingGoalTexts = getExistingBacklogGoalTexts(targetContextId).toMutableSet()
                checklistItems.forEach { checklistItem ->
                    val normalizedText = normalizeText(checklistItem.content)
                    if (normalizedText.isBlank()) {
                        skippedInvalid += 1
                        return@forEach
                    }
                    if (normalizedText in existingGoalTexts) {
                        skippedDuplicates += 1
                        return@forEach
                    }
                    goalRepository.addGoalToContext(checklistItem.content.trim(), targetContextId)
                    existingGoalTexts += normalizedText
                    createdGoals += 1
                    checklistItemsToDelete += checklistItem.id
                }
            }

            if (includeAttachments && attachmentRefs.isNotEmpty()) {
                val attachmentCopy =
                    copyAttachmentRefsToContext(
                        refs = attachmentRefs,
                        targetContextId = targetContextId,
                        sourceContextId = sourceContextId,
                    )
                createdAttachments += attachmentCopy.created
                skippedDuplicates += attachmentCopy.duplicates
                skippedInvalid += attachmentCopy.invalid
                if (attachmentCopy.insertedSourceItemIds.isNotEmpty()) {
                    listItemRepository.deleteListItems(attachmentCopy.insertedSourceItemIds)
                    moved += attachmentCopy.insertedSourceItemIds.size
                }
                if (sourceContextId.isNotBlank() && attachmentCopy.sourceAttachmentIdsForCut.isNotEmpty()) {
                    attachmentCopy.sourceAttachmentIdsForCut.forEach { attachmentId ->
                        contextRepository.unlinkAttachmentFromContext(
                            contextId = sourceContextId,
                            attachmentId = attachmentId,
                        )
                    }
                    moved += attachmentCopy.sourceAttachmentIdsForCut.size
                }
            }

            if (moved + createdLinks + clonedGoals + createdGoals + createdAttachments > 0) {
                if (checklistItemsToDelete.isNotEmpty()) {
                    checklistRepository.deleteItems(checklistItemsToDelete)
                    moved += checklistItemsToDelete.size
                }
                clipboardService.clear()
            }

            return BacklogPasteReport(
                totalRequested =
                    itemRefs.size +
                        contextRefs.size +
                        directionRefs.size +
                        attachmentRefs.size +
                        checklistRefs.size,
                moved = moved,
                createdLinks = createdLinks,
                clonedGoals = clonedGoals,
                createdGoals = createdGoals,
                createdAttachments = createdAttachments,
                skippedDuplicates = skippedDuplicates,
                skippedInvalid = skippedInvalid,
            )
        }

        private suspend fun pasteCopyToDirection(
            goalRefs: List<ClipboardEntityRef.BacklogGoal>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            directionRefs: List<ClipboardEntityRef.DirectionItem>,
            attachmentRefs: List<ClipboardEntityRef.BacklogAttachment>,
            checklistRefs: List<ClipboardEntityRef.ChecklistItem>,
            targetContextId: String,
            includeAttachments: Boolean,
        ): BacklogPasteReport {
            val backlogGoalIds = goalRefs.map { it.goalId }.distinct()
            val backlogContextIds = contextRefs.map { it.contextId }.distinct()
            val sourceDirectionItems = loadDirectionItems(directionRefs)

            val existingItems = directionRepository.getDirectionItemsForContextSync(targetContextId)
            val existingLinked = existingItems.mapNotNull { it.linkedContextId }.toMutableSet()
            val existingTexts =
                existingItems
                    .filter { it.linkedContextId == null }
                    .map { normalizeText(it.text) }
                    .toMutableSet()

            val itemsToCreate = mutableListOf<Pair<String, String?>>()
            var createdAttachments = 0
            var skippedDuplicates = 0
            var skippedInvalid = directionRefs.size - sourceDirectionItems.size
            val checklistItems = loadChecklistItems(checklistRefs)
            skippedInvalid += checklistRefs.size - checklistItems.size

            for (goalId in backlogGoalIds) {
                val goal = goalRepository.getGoalById(goalId)
                val text = goal?.text?.trim().orEmpty()
                val normalized = normalizeText(text)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                } else if (normalized in existingTexts) {
                    skippedDuplicates += 1
                } else {
                    itemsToCreate += text to null
                    existingTexts += normalized
                }
            }

            for (contextId in backlogContextIds) {
                val isDuplicateLink = contextId == targetContextId || contextId in existingLinked
                if (isDuplicateLink) {
                    skippedDuplicates += 1
                } else {
                    val linkedName =
                        contextRepository.getContextById(contextId)?.name?.trim().orEmpty().ifBlank { "Контекст" }
                    itemsToCreate += linkedName to contextId
                    existingLinked += contextId
                }
            }

            for (item in sourceDirectionItems) {
                val linked = item.linkedContextId
                if (!linked.isNullOrBlank()) {
                    val isDuplicateLink = linked == targetContextId || linked in existingLinked
                    if (isDuplicateLink) {
                        skippedDuplicates += 1
                    } else {
                        itemsToCreate += item.text.trim() to linked
                        existingLinked += linked
                    }
                } else {
                    val normalized = normalizeText(item.text)
                    if (normalized.isBlank()) {
                        skippedInvalid += 1
                    } else if (normalized in existingTexts) {
                        skippedDuplicates += 1
                    } else {
                        itemsToCreate += item.text.trim() to null
                        existingTexts += normalized
                    }
                }
            }

            checklistItems.forEach { checklistItem ->
                val normalized = normalizeText(checklistItem.content)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                    return@forEach
                }
                if (normalized in existingTexts) {
                    skippedDuplicates += 1
                    return@forEach
                }
                itemsToCreate += checklistItem.content.trim() to null
                existingTexts += normalized
            }

            val created = directionRepository.addDirectionItems(targetContextId, itemsToCreate)
            if (includeAttachments && attachmentRefs.isNotEmpty()) {
                val attachmentCopy =
                    copyAttachmentRefsToContext(
                        refs = attachmentRefs,
                        targetContextId = targetContextId,
                        sourceContextId = null,
                    )
                createdAttachments += attachmentCopy.created
                skippedDuplicates += attachmentCopy.duplicates
                skippedInvalid += attachmentCopy.invalid
            }
            return BacklogPasteReport(
                totalRequested =
                    backlogGoalIds.size +
                        backlogContextIds.size +
                        directionRefs.size +
                        attachmentRefs.size +
                        checklistRefs.size,
                createdDirectionItems = created,
                createdAttachments = createdAttachments,
                skippedDuplicates = skippedDuplicates,
                skippedInvalid = skippedInvalid,
            )
        }

        private suspend fun pasteCutToDirection(
            itemRefs: List<ClipboardEntityRef.BacklogItem>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            directionRefs: List<ClipboardEntityRef.DirectionItem>,
            attachmentRefs: List<ClipboardEntityRef.BacklogAttachment>,
            checklistRefs: List<ClipboardEntityRef.ChecklistItem>,
            sourceContextId: String,
            targetContextId: String,
            includeAttachments: Boolean,
        ): BacklogPasteReport {
            var createdDirectionItems = 0
            var createdAttachments = 0
            var moved = 0
            var skippedDuplicates = 0
            var skippedInvalid = 0
            val checklistItems = loadChecklistItems(checklistRefs)
            skippedInvalid += checklistRefs.size - checklistItems.size
            val checklistItemsToDelete = mutableListOf<String>()

            val existingItems = directionRepository.getDirectionItemsForContextSync(targetContextId)
            val existingLinked = existingItems.mapNotNull { it.linkedContextId }.toMutableSet()
            val existingTexts =
                existingItems
                    .filter { it.linkedContextId == null }
                    .map { normalizeText(it.text) }
                    .toMutableSet()
            val itemsToCreate = mutableListOf<Pair<String, String?>>()

            if (contextRefs.isNotEmpty()) {
                val backlogContextIds = contextRefs.map { it.contextId }.distinct()
                for (contextId in backlogContextIds) {
                    val isDuplicateLink = contextId == targetContextId || contextId in existingLinked
                    if (isDuplicateLink) {
                        skippedDuplicates += 1
                    } else {
                        val linkedName =
                            contextRepository.getContextById(contextId)?.name?.trim().orEmpty().ifBlank { "Контекст" }
                        itemsToCreate += linkedName to contextId
                        existingLinked += contextId
                    }
                }
            }

            val backlogItemsRequested = itemRefs.map { it.listItemId }
            val backlogItemsById = listItemRepository.getItemsByIds(backlogItemsRequested).associateBy { it.id }
            val backlogItems = backlogItemsRequested.mapNotNull(backlogItemsById::get)
            skippedInvalid += backlogItemsRequested.size - backlogItems.size

            val backlogItemsToDelete = mutableListOf<String>()
            for (item in backlogItems) {
                when (item.itemType) {
                    BacklogItemTypeValues.GOAL -> {
                        val goalText = goalRepository.getGoalById(item.entityId)?.text?.trim().orEmpty()
                        val normalized = normalizeText(goalText)
                        if (normalized.isBlank()) {
                            skippedInvalid += 1
                        } else if (normalized in existingTexts) {
                            skippedDuplicates += 1
                        } else {
                            itemsToCreate += goalText to null
                            existingTexts += normalized
                            backlogItemsToDelete += item.id
                        }
                    }

                    BacklogItemTypeValues.SUBLIST -> {
                        val linkedContextId = item.entityId
                        val isDuplicateLink = linkedContextId == targetContextId || linkedContextId in existingLinked
                        if (isDuplicateLink) {
                            skippedDuplicates += 1
                        } else {
                            val linkedName =
                                contextRepository
                                    .getContextById(linkedContextId)
                                    ?.name
                                    ?.trim()
                                    .orEmpty()
                                    .ifBlank { "Контекст" }
                            itemsToCreate += linkedName to linkedContextId
                            existingLinked += linkedContextId
                            backlogItemsToDelete += item.id
                        }
                    }

                    else -> skippedInvalid += 1
                }
            }

            val sourceDirectionItems = loadDirectionItems(directionRefs)
            skippedInvalid += directionRefs.size - sourceDirectionItems.size
            val directionItemsToDelete = mutableListOf<String>()
            for (item in sourceDirectionItems) {
                val linked = item.linkedContextId
                if (!linked.isNullOrBlank()) {
                    val isDuplicateLink = linked == targetContextId || linked in existingLinked
                    if (isDuplicateLink) {
                        skippedDuplicates += 1
                    } else {
                        itemsToCreate += item.text.trim() to linked
                        existingLinked += linked
                        directionItemsToDelete += item.id
                    }
                } else {
                    val normalized = normalizeText(item.text)
                    if (normalized.isBlank()) {
                        skippedInvalid += 1
                    } else if (normalized in existingTexts) {
                        skippedDuplicates += 1
                    } else {
                        itemsToCreate += item.text.trim() to null
                        existingTexts += normalized
                        directionItemsToDelete += item.id
                    }
                }
            }

            checklistItems.forEach { checklistItem ->
                val normalized = normalizeText(checklistItem.content)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                    return@forEach
                }
                if (normalized in existingTexts) {
                    skippedDuplicates += 1
                    return@forEach
                }
                itemsToCreate += checklistItem.content.trim() to null
                existingTexts += normalized
                checklistItemsToDelete += checklistItem.id
            }

            if (itemsToCreate.isNotEmpty()) {
                createdDirectionItems = directionRepository.addDirectionItems(targetContextId, itemsToCreate)
            }

            if (backlogItemsToDelete.isNotEmpty()) {
                listItemRepository.deleteListItems(backlogItemsToDelete)
                moved += backlogItemsToDelete.size
            }

            if (directionItemsToDelete.isNotEmpty()) {
                val sameContextDirectionMove = sourceContextId == targetContextId
                if (!sameContextDirectionMove) {
                    directionRepository.deleteDirectionItems(directionItemsToDelete)
                    moved += directionItemsToDelete.size
                }
            }

            if (checklistItemsToDelete.isNotEmpty()) {
                checklistRepository.deleteItems(checklistItemsToDelete)
                moved += checklistItemsToDelete.size
            }

            if (includeAttachments && attachmentRefs.isNotEmpty()) {
                val attachmentCopy =
                    copyAttachmentRefsToContext(
                        refs = attachmentRefs,
                        targetContextId = targetContextId,
                        sourceContextId = sourceContextId,
                    )
                createdAttachments += attachmentCopy.created
                skippedDuplicates += attachmentCopy.duplicates
                skippedInvalid += attachmentCopy.invalid
                if (attachmentCopy.insertedSourceItemIds.isNotEmpty()) {
                    listItemRepository.deleteListItems(attachmentCopy.insertedSourceItemIds)
                    moved += attachmentCopy.insertedSourceItemIds.size
                }
                if (sourceContextId.isNotBlank() && attachmentCopy.sourceAttachmentIdsForCut.isNotEmpty()) {
                    attachmentCopy.sourceAttachmentIdsForCut.forEach { attachmentId ->
                        contextRepository.unlinkAttachmentFromContext(
                            contextId = sourceContextId,
                            attachmentId = attachmentId,
                        )
                    }
                    moved += attachmentCopy.sourceAttachmentIdsForCut.size
                }
            }

            if (createdDirectionItems > 0 || createdAttachments > 0 || moved > 0) {
                clipboardService.clear()
            }

            return BacklogPasteReport(
                totalRequested =
                    itemRefs.size +
                        contextRefs.size +
                        directionRefs.size +
                        attachmentRefs.size +
                        checklistRefs.size,
                moved = moved,
                createdDirectionItems = createdDirectionItems,
                createdAttachments = createdAttachments,
                skippedDuplicates = skippedDuplicates,
                skippedInvalid = skippedInvalid,
            )
        }

        private suspend fun partitionMovableBacklogItems(
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

        private data class AttachmentCopyResult(
            val created: Int,
            val duplicates: Int,
            val invalid: Int,
            val insertedSourceItemIds: List<String>,
            val sourceAttachmentIdsForCut: List<String>,
        )

        private data class ContextLinkAttachmentCopyResult(
            val created: Int,
            val duplicates: Int,
            val invalid: Int,
        )

        private suspend fun copyAttachmentRefsToContext(
            refs: List<ClipboardEntityRef.BacklogAttachment>,
            targetContextId: String,
            sourceContextId: String?,
        ): AttachmentCopyResult {
            if (refs.isEmpty()) return AttachmentCopyResult(0, 0, 0, emptyList(), emptyList())
            val requestedIds = refs.map { it.listItemId }
            val itemsById = listItemRepository.getItemsByIds(requestedIds).associateBy { it.id }
            val sourceAttachmentsById =
                sourceContextId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { contextId ->
                        contextRepository
                            .getAttachmentsForContextStream(contextId)
                            .first()
                            .associateBy { it.attachment.id }
                    }
                    ?: emptyMap()
            val existingTargetAttachmentIds =
                contextRepository
                    .getAttachmentsForContextStream(targetContextId)
                    .first()
                    .map { it.attachment.id }
                    .toMutableSet()
            var duplicates = 0
            var invalid = 0
            val insertedSourceItemIds = mutableListOf<String>()
            val sourceAttachmentIdsForCut = mutableListOf<String>()
            var created = 0

            for (requestedId in requestedIds) {
                var sourceListItemIdForCut: String? = null
                val item = itemsById[requestedId]
                val attachmentId =
                    if (item != null) {
                        val hasSupportedType = isAttachmentType(item.itemType)
                        if (!hasSupportedType) {
                            invalid += 1
                            null
                        } else {
                            val resolvedAttachmentId =
                                contextRepository.findAttachmentIdByEntity(
                                    attachmentType = item.itemType,
                                    entityId = item.entityId,
                                )
                            if (resolvedAttachmentId == null) {
                                invalid += 1
                                null
                            } else {
                                sourceListItemIdForCut = item.id
                                resolvedAttachmentId
                            }
                        }
                    } else {
                        // In Connections view we copy attachment IDs directly, not backlog list item IDs.
                        val attachmentWithContext = sourceAttachmentsById[requestedId]
                        if (attachmentWithContext == null) {
                            invalid += 1
                            null
                        } else if (!isAttachmentType(attachmentWithContext.attachment.attachmentType)) {
                            invalid += 1
                            null
                        } else {
                            requestedId
                        }
                    }

                if (attachmentId == null) {
                    continue
                } else if (attachmentId in existingTargetAttachmentIds) {
                    duplicates += 1
                } else {
                    contextRepository.linkAttachmentToContext(attachmentId, targetContextId)
                    existingTargetAttachmentIds += attachmentId
                    created += 1
                    if (sourceListItemIdForCut != null) {
                        insertedSourceItemIds += sourceListItemIdForCut
                    } else if (sourceContextId != null) {
                        sourceAttachmentIdsForCut += attachmentId
                    }
                }
            }
            return AttachmentCopyResult(
                created = created,
                duplicates = duplicates,
                invalid = invalid,
                insertedSourceItemIds = insertedSourceItemIds,
                sourceAttachmentIdsForCut = sourceAttachmentIdsForCut,
            )
        }

        private suspend fun copyContextLinkRefsToContextAttachments(
            refs: List<ClipboardEntityRef.BacklogContextLink>,
            targetContextId: String,
        ): ContextLinkAttachmentCopyResult {
            if (refs.isEmpty()) return ContextLinkAttachmentCopyResult(created = 0, duplicates = 0, invalid = 0)

            val requestedContextIds = refs.map { it.contextId }.distinct()
            val existingTargets = mutableSetOf<String>()
            val existingAttachments = contextRepository.getAttachmentsForContextStream(targetContextId).first()
            for (attachment in existingAttachments) {
                if (attachment.attachment.attachmentType == BacklogItemTypeValues.LINK_ITEM) {
                    val linkData = listItemRepository.getLinkItemById(attachment.attachment.entityId)?.linkData
                    if (linkData?.type == LinkType.CONTEXT) {
                        existingTargets += linkData.target
                    }
                }
            }

            var created = 0
            var duplicates = 0
            var invalid = 0

            for (contextId in requestedContextIds) {
                val isInvalidTarget = contextId.isBlank() || contextId == targetContextId
                if (isInvalidTarget) {
                    invalid += 1
                } else if (contextId in existingTargets) {
                    duplicates += 1
                } else {
                    val contextName = contextRepository.getContextById(contextId)?.name?.trim().orEmpty()
                    val link =
                        RelatedLink(
                            type = LinkType.CONTEXT,
                            target = contextId,
                            displayName = contextName.ifBlank { "Контекст" },
                        )
                    contextRepository.addLinkItemToContextFromLink(targetContextId, link)
                    existingTargets += contextId
                    created += 1
                }
            }

            return ContextLinkAttachmentCopyResult(
                created = created,
                duplicates = duplicates,
                invalid = invalid,
            )
        }

        private suspend fun loadDirectionItems(refs: List<ClipboardEntityRef.DirectionItem>) =
            refs.map { it.directionItemId }
                .let { requestedIds ->
                    val byId = directionRepository.getDirectionItemsByIds(requestedIds).associateBy { it.id }
                    requestedIds.mapNotNull(byId::get)
                }

        private suspend fun loadChecklistItems(refs: List<ClipboardEntityRef.ChecklistItem>) =
            refs.map { it.checklistItemId }
                .let { requestedIds ->
                    val byId = checklistRepository.getItemsByIds(requestedIds).associateBy { it.id }
                    requestedIds.mapNotNull(byId::get)
                }

        private suspend fun getExistingBacklogGoalTexts(contextId: String): Set<String> {
            val goalIds = listItemRepository.getGoalIdsForContext(contextId).distinct()
            return goalIds
                .mapNotNull { goalRepository.getGoalById(it)?.text }
                .map { normalizeText(it) }
                .filter { it.isNotBlank() }
                .toSet()
        }

        private fun normalizeText(text: String): String = text.trim().lowercase()

        private suspend fun addSourceContextLinkToGoals(
            goalIds: List<String>,
            sourceContextId: String,
            targetContextId: String,
        ) {
            if (sourceContextId.isBlank() || sourceContextId == targetContextId) return
            val sourceContextName = contextRepository.getContextById(sourceContextId)?.name?.trim().orEmpty()
            val sourceContextLink =
                RelatedLink(
                    type = LinkType.CONTEXT,
                    target = sourceContextId,
                    displayName = sourceContextName.ifBlank { "Контекст" },
                )
            goalIds.forEach { goalId ->
                val goal = goalRepository.getGoalById(goalId) ?: return@forEach
                val existingLinks = goal.relatedLinks.orEmpty()
                if (existingLinks.any { it.type == LinkType.CONTEXT && it.target == sourceContextId }) return@forEach
                goalRepository.updateGoal(
                    goal.copy(
                        relatedLinks = existingLinks + sourceContextLink,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        private fun isAttachmentType(itemType: String): Boolean =
            itemType == BacklogItemTypeValues.LINK_ITEM ||
                itemType == BacklogItemTypeValues.NOTE_DOCUMENT ||
                itemType == BacklogItemTypeValues.MUSIC_NOTE ||
                itemType == BacklogItemTypeValues.CHECKLIST ||
                itemType == BacklogItemTypeValues.SCRIPT
    }
