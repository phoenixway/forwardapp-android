package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import kotlinx.coroutines.flow.first
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
    val createdGoals: Int = 0,
    val createdDirectionItems: Int = 0,
    val createdAttachments: Int = 0,
    val skippedDuplicates: Int = 0,
    val skippedInvalid: Int = 0,
) {
    val changedCount: Int get() = moved + createdLinks + clonedGoals + createdGoals + createdDirectionItems + createdAttachments

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
            val hasBacklogItemCut = payload.entities.any { it is ClipboardEntityRef.BacklogItem }
            val hasContextLinkRef = payload.entities.any { it is ClipboardEntityRef.BacklogContextLink }
            val hasDirectionItemCut = payload.entities.any { it is ClipboardEntityRef.DirectionItem }
            val hasAttachmentRef = payload.entities.any { it is ClipboardEntityRef.BacklogAttachment }
            return when (payload.operation) {
                ClipboardOperation.COPY ->
                    payload.entities.any {
                        it is ClipboardEntityRef.BacklogGoal ||
                            it is ClipboardEntityRef.BacklogContextLink ||
                            it is ClipboardEntityRef.DirectionItem ||
                            it is ClipboardEntityRef.BacklogAttachment
                    }

                ClipboardOperation.CUT ->
                    (hasBacklogItemCut && payload.sourceContextId != targetContextId) ||
                        hasContextLinkRef ||
                        hasDirectionItemCut ||
                        hasAttachmentRef
            }
        }

        fun canPasteIntoDirection(targetContextId: String): Boolean {
            if (targetContextId.isBlank()) return false
            val payload = clipboardService.payload.value ?: return false
            val hasBacklogItemCut = payload.entities.any { it is ClipboardEntityRef.BacklogItem }
            val hasContextLinkRef = payload.entities.any { it is ClipboardEntityRef.BacklogContextLink }
            val hasDirectionItemCut = payload.entities.any { it is ClipboardEntityRef.DirectionItem }
            val hasAttachmentRef = payload.entities.any { it is ClipboardEntityRef.BacklogAttachment }
            return when (payload.operation) {
                ClipboardOperation.COPY ->
                    payload.entities.any {
                        it is ClipboardEntityRef.BacklogGoal ||
                            it is ClipboardEntityRef.BacklogContextLink ||
                            it is ClipboardEntityRef.DirectionItem ||
                            it is ClipboardEntityRef.BacklogAttachment
                    }

                ClipboardOperation.CUT ->
                    hasBacklogItemCut ||
                        hasContextLinkRef ||
                        (hasDirectionItemCut && payload.sourceContextId != targetContextId) ||
                        hasAttachmentRef
            }
        }

        fun canPasteIntoAttachments(targetContextId: String): Boolean {
            if (targetContextId.isBlank()) return false
            val payload = clipboardService.payload.value ?: return false
            return payload.entities.any { it is ClipboardEntityRef.BacklogAttachment }
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
        ): BacklogPasteReport {
            val payload = clipboardService.payload.value ?: return BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)

            return when (payload.operation) {
                ClipboardOperation.COPY -> {
                    val goalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    if (goalRefs.isEmpty() && contextRefs.isEmpty() && directionRefs.isEmpty() && attachmentRefs.isEmpty()) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopyToBacklog(
                            goalRefs = goalRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            targetContextId = targetContextId,
                            mode = mode,
                            includeAttachments = includeAttachments,
                        )
                    }
                }

                ClipboardOperation.CUT -> {
                    val itemRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    if (itemRefs.isEmpty() && contextRefs.isEmpty() && directionRefs.isEmpty() && attachmentRefs.isEmpty()) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCutToBacklog(
                            itemRefs = itemRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
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
            val payload = clipboardService.payload.value ?: return BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)

            return when (payload.operation) {
                ClipboardOperation.COPY -> {
                    val goalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>()
                    val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
                    val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
                    val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
                    if (goalRefs.isEmpty() && contextRefs.isEmpty() && directionRefs.isEmpty() && attachmentRefs.isEmpty()) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopyToDirection(
                            goalRefs = goalRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
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
                    if (itemRefs.isEmpty() && contextRefs.isEmpty() && directionRefs.isEmpty() && attachmentRefs.isEmpty()) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCutToDirection(
                            itemRefs = itemRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            sourceContextId = payload.sourceContextId,
                            targetContextId = targetContextId,
                            includeAttachments = includeAttachments,
                        )
                    }
                }
            }
        }

        suspend fun pasteIntoAttachments(targetContextId: String): BacklogPasteReport {
            val payload = clipboardService.payload.value ?: return BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
            val attachmentRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogAttachment>()
            if (attachmentRefs.isEmpty()) return BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
            return when (payload.operation) {
                ClipboardOperation.COPY -> {
                    val copyResult =
                        copyAttachmentRefsToContext(
                            refs = attachmentRefs,
                            targetContextId = targetContextId,
                            sourceContextId = payload.sourceContextId,
                        )
                    BacklogPasteReport(
                        totalRequested = attachmentRefs.size,
                        createdAttachments = copyResult.created,
                        skippedDuplicates = copyResult.duplicates,
                        skippedInvalid = copyResult.invalid,
                    )
                }

                ClipboardOperation.CUT -> {
                    val copyResult =
                        copyAttachmentRefsToContext(
                            refs = attachmentRefs,
                            targetContextId = targetContextId,
                            sourceContextId = payload.sourceContextId,
                        )
                    if (copyResult.insertedSourceItemIds.isNotEmpty()) {
                        listItemRepository.deleteListItems(copyResult.insertedSourceItemIds)
                    }
                    if (payload.sourceContextId.isNotBlank() && copyResult.sourceAttachmentIdsForCut.isNotEmpty()) {
                        copyResult.sourceAttachmentIdsForCut.forEach { attachmentId ->
                            contextRepository.unlinkAttachmentFromContext(
                                contextId = payload.sourceContextId,
                                attachmentId = attachmentId,
                            )
                        }
                    }
                    if (copyResult.insertedSourceItemIds.isNotEmpty() || copyResult.sourceAttachmentIdsForCut.isNotEmpty()) {
                        clipboardService.clear()
                    }
                    BacklogPasteReport(
                        totalRequested = attachmentRefs.size,
                        moved = copyResult.insertedSourceItemIds.size + copyResult.sourceAttachmentIdsForCut.size,
                        createdAttachments = copyResult.created,
                        skippedDuplicates = copyResult.duplicates,
                        skippedInvalid = copyResult.invalid,
                    )
                }
            }
        }

        private suspend fun pasteCopyToBacklog(
            goalRefs: List<ClipboardEntityRef.BacklogGoal>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            directionRefs: List<ClipboardEntityRef.DirectionItem>,
            attachmentRefs: List<ClipboardEntityRef.BacklogAttachment>,
            targetContextId: String,
            mode: BacklogPasteMode,
            includeAttachments: Boolean,
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
                    val nonDuplicates = sourceGoalIds.filterNot { listItemRepository.doesLinkExist(it, targetContextId) }
                    goalRepository.createGoalLinks(nonDuplicates, targetContextId)
                    createdLinks += nonDuplicates.size
                    skippedDuplicates += sourceGoalIds.size - nonDuplicates.size
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
                    if (linkedContextId == targetContextId || listItemRepository.doesLinkExist(linkedContextId, targetContextId)) {
                        skippedDuplicates += 1
                        continue
                    }
                    listItemRepository.addContextLinkToContext(
                        targetContextId = linkedContextId,
                        currentContextId = targetContextId,
                    )
                    createdLinks += 1
                    continue
                }

                val normalizedText = normalizeText(directionItem.text)
                if (normalizedText.isBlank()) {
                    skippedInvalid += 1
                    continue
                }
                if (normalizedText in existingGoalTexts) {
                    skippedDuplicates += 1
                    continue
                }

                goalRepository.addGoalToContext(directionItem.text.trim(), targetContextId)
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
                totalRequested = sourceGoalIds.size + sourceContextIds.size + directionRefs.size + attachmentRefs.size,
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
                        if (linkedContextId == targetContextId || listItemRepository.doesLinkExist(linkedContextId, targetContextId)) {
                            skippedDuplicates += 1
                            continue
                        }
                        listItemRepository.addContextLinkToContext(
                            targetContextId = linkedContextId,
                            currentContextId = targetContextId,
                        )
                        createdLinks += 1
                        insertedDirectionIds += directionItem.id
                        continue
                    }

                    val normalizedText = normalizeText(directionItem.text)
                    if (normalizedText.isBlank()) {
                        skippedInvalid += 1
                        continue
                    }
                    if (normalizedText in existingGoalTexts) {
                        skippedDuplicates += 1
                        continue
                    }

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

                if (insertedDirectionIds.isNotEmpty()) {
                    directionRepository.deleteDirectionItems(insertedDirectionIds)
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
                clipboardService.clear()
            }

            return BacklogPasteReport(
                totalRequested = itemRefs.size + contextRefs.size + directionRefs.size + attachmentRefs.size,
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
            targetContextId: String,
            includeAttachments: Boolean,
        ): BacklogPasteReport {
            val backlogGoalIds = goalRefs.map { it.goalId }.distinct()
            val backlogContextIds = contextRefs.map { it.contextId }.distinct()
            val sourceDirectionItems = loadDirectionItems(directionRefs)

            val existingItems = directionRepository.getDirectionItemsForContextSync(targetContextId)
            val existingLinked = existingItems.mapNotNull { it.linkedContextId }.toMutableSet()
            val existingTexts = existingItems.filter { it.linkedContextId == null }.map { normalizeText(it.text) }.toMutableSet()

            val itemsToCreate = mutableListOf<Pair<String, String?>>()
            var createdAttachments = 0
            var skippedDuplicates = 0
            var skippedInvalid = directionRefs.size - sourceDirectionItems.size

            for (goalId in backlogGoalIds) {
                val goal = goalRepository.getGoalById(goalId)
                val text = goal?.text?.trim().orEmpty()
                val normalized = normalizeText(text)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                    continue
                }
                if (normalized in existingTexts) {
                    skippedDuplicates += 1
                    continue
                }
                itemsToCreate += text to null
                existingTexts += normalized
            }

            for (contextId in backlogContextIds) {
                if (contextId == targetContextId || contextId in existingLinked) {
                    skippedDuplicates += 1
                    continue
                }
                val linkedName = contextRepository.getContextById(contextId)?.name?.trim().orEmpty().ifBlank { "Контекст" }
                itemsToCreate += linkedName to contextId
                existingLinked += contextId
            }

            for (item in sourceDirectionItems) {
                val linked = item.linkedContextId
                if (!linked.isNullOrBlank()) {
                    if (linked == targetContextId || linked in existingLinked) {
                        skippedDuplicates += 1
                        continue
                    }
                    itemsToCreate += item.text.trim() to linked
                    existingLinked += linked
                    continue
                }

                val normalized = normalizeText(item.text)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                    continue
                }
                if (normalized in existingTexts) {
                    skippedDuplicates += 1
                    continue
                }
                itemsToCreate += item.text.trim() to null
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
                totalRequested = backlogGoalIds.size + backlogContextIds.size + directionRefs.size + attachmentRefs.size,
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
            sourceContextId: String,
            targetContextId: String,
            includeAttachments: Boolean,
        ): BacklogPasteReport {
            var createdDirectionItems = 0
            var createdAttachments = 0
            var moved = 0
            var skippedDuplicates = 0
            var skippedInvalid = 0

            val existingItems = directionRepository.getDirectionItemsForContextSync(targetContextId)
            val existingLinked = existingItems.mapNotNull { it.linkedContextId }.toMutableSet()
            val existingTexts = existingItems.filter { it.linkedContextId == null }.map { normalizeText(it.text) }.toMutableSet()
            val itemsToCreate = mutableListOf<Pair<String, String?>>()

            if (contextRefs.isNotEmpty()) {
                val backlogContextIds = contextRefs.map { it.contextId }.distinct()
                for (contextId in backlogContextIds) {
                    if (contextId == targetContextId || contextId in existingLinked) {
                        skippedDuplicates += 1
                        continue
                    }
                    val linkedName = contextRepository.getContextById(contextId)?.name?.trim().orEmpty().ifBlank { "Контекст" }
                    itemsToCreate += linkedName to contextId
                    existingLinked += contextId
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
                            continue
                        }
                        if (normalized in existingTexts) {
                            skippedDuplicates += 1
                            continue
                        }
                        itemsToCreate += goalText to null
                        existingTexts += normalized
                        backlogItemsToDelete += item.id
                    }

                    BacklogItemTypeValues.SUBLIST -> {
                        val linkedContextId = item.entityId
                        if (linkedContextId == targetContextId || linkedContextId in existingLinked) {
                            skippedDuplicates += 1
                            continue
                        }
                        val linkedName = contextRepository.getContextById(linkedContextId)?.name?.trim().orEmpty().ifBlank { "Контекст" }
                        itemsToCreate += linkedName to linkedContextId
                        existingLinked += linkedContextId
                        backlogItemsToDelete += item.id
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
                    if (linked == targetContextId || linked in existingLinked) {
                        skippedDuplicates += 1
                        continue
                    }
                    itemsToCreate += item.text.trim() to linked
                    existingLinked += linked
                    directionItemsToDelete += item.id
                    continue
                }

                val normalized = normalizeText(item.text)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                    continue
                }
                if (normalized in existingTexts) {
                    skippedDuplicates += 1
                    continue
                }
                itemsToCreate += item.text.trim() to null
                existingTexts += normalized
                directionItemsToDelete += item.id
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
                totalRequested = itemRefs.size + contextRefs.size + directionRefs.size + attachmentRefs.size,
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

            refLoop@ for (requestedId in requestedIds) {
                var sourceListItemIdForCut: String? = null
                val item = itemsById[requestedId]
                val attachmentId =
                    if (item != null) {
                        if (!isAttachmentType(item.itemType)) {
                            invalid += 1
                            continue@refLoop
                        }
                        val resolvedAttachmentId =
                            contextRepository.findAttachmentIdByEntity(
                                attachmentType = item.itemType,
                                entityId = item.entityId,
                            )
                        if (resolvedAttachmentId == null) {
                            invalid += 1
                            continue@refLoop
                        }
                        sourceListItemIdForCut = item.id
                        resolvedAttachmentId
                    } else {
                        // In Connections view we copy attachment IDs directly, not backlog list item IDs.
                        val attachmentWithContext = sourceAttachmentsById[requestedId]
                        if (attachmentWithContext == null) {
                            invalid += 1
                            continue@refLoop
                        }
                        if (!isAttachmentType(attachmentWithContext.attachment.attachmentType)) {
                            invalid += 1
                            continue@refLoop
                        }
                        requestedId
                    }

                if (attachmentId in existingTargetAttachmentIds) {
                    duplicates += 1
                    continue
                }
                contextRepository.linkAttachmentToContext(attachmentId, targetContextId)
                existingTargetAttachmentIds += attachmentId
                created += 1
                if (sourceListItemIdForCut != null) {
                    insertedSourceItemIds += sourceListItemIdForCut
                } else if (sourceContextId != null) {
                    sourceAttachmentIdsForCut += attachmentId
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

        private suspend fun loadDirectionItems(refs: List<ClipboardEntityRef.DirectionItem>) =
            refs.map { it.directionItemId }
                .let { requestedIds ->
                    val byId = directionRepository.getDirectionItemsByIds(requestedIds).associateBy { it.id }
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

        private fun isAttachmentType(itemType: String): Boolean =
            itemType == BacklogItemTypeValues.LINK_ITEM ||
                itemType == BacklogItemTypeValues.NOTE_DOCUMENT ||
                itemType == BacklogItemTypeValues.MUSIC_NOTE ||
                itemType == BacklogItemTypeValues.CHECKLIST ||
                itemType == BacklogItemTypeValues.SCRIPT
    }
