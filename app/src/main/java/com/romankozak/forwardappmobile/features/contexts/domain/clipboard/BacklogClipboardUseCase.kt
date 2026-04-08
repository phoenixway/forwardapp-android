package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
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

data class TacticalMissionPasteReport(
    val totalRequested: Int,
    val createdMissions: Int = 0,
    val moved: Int = 0,
    val skippedInvalid: Int = 0,
) {
    val changedCount: Int
        get() = createdMissions + moved

    fun toUserMessage(): String {
        val parts = mutableListOf<String>()
        if (createdMissions > 0) parts += "створено місій: $createdMissions"
        if (moved > 0) parts += "переміщено з джерела: $moved"
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
        private val dayManagementRepository: DayManagementRepository,
        private val missionRepository: MissionRepository,
        private val inboxRepository: InboxRepository,
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
                    it is ClipboardEntityRef.ChecklistItem ||
                    it is ClipboardEntityRef.DayTask ||
                    it is ClipboardEntityRef.TacticalMission ||
                    it is ClipboardEntityRef.InboxRecord
            }

        fun canPasteIntoBacklog(targetContextId: String): Boolean {
            return canPasteToTarget(targetContextId) { payload ->
                val hasBacklogItemCut = payload.entities.any { it is ClipboardEntityRef.BacklogItem }
                val hasContextLinkRef = payload.entities.any { it is ClipboardEntityRef.BacklogContextLink }
                val hasDirectionItemCut = payload.entities.any { it is ClipboardEntityRef.DirectionItem }
                val hasAttachmentRef = payload.entities.any { it is ClipboardEntityRef.BacklogAttachment }
                val hasChecklistItem = payload.entities.any { it is ClipboardEntityRef.ChecklistItem }
                val hasDayTask = payload.entities.any { it is ClipboardEntityRef.DayTask }
                val hasTacticalMission = payload.entities.any { it is ClipboardEntityRef.TacticalMission }
                when (payload.operation) {
                    ClipboardOperation.COPY -> payload.hasCopyableBacklogEntities()
                    ClipboardOperation.CUT ->
                        (hasBacklogItemCut && payload.sourceContextId != targetContextId) ||
                            hasContextLinkRef ||
                            hasDirectionItemCut ||
                            hasAttachmentRef ||
                            hasChecklistItem ||
                            hasDayTask ||
                            hasTacticalMission
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
                val hasDayTask = payload.entities.any { it is ClipboardEntityRef.DayTask }
                val hasTacticalMission = payload.entities.any { it is ClipboardEntityRef.TacticalMission }
                when (payload.operation) {
                    ClipboardOperation.COPY -> payload.hasCopyableBacklogEntities()
                    ClipboardOperation.CUT ->
                        hasBacklogItemCut ||
                            hasContextLinkRef ||
                            (hasDirectionItemCut && payload.sourceContextId != targetContextId) ||
                            hasAttachmentRef ||
                            hasChecklistItem ||
                            hasDayTask ||
                            hasTacticalMission
                }
            }
        }

        fun canPasteIntoInbox(targetContextId: String): Boolean {
            return canPasteToTarget(targetContextId) { payload ->
                payload.entities.any {
                    it is ClipboardEntityRef.BacklogGoal ||
                        it is ClipboardEntityRef.BacklogItem ||
                        it is ClipboardEntityRef.BacklogContextLink ||
                        it is ClipboardEntityRef.DirectionItem ||
                        it is ClipboardEntityRef.ChecklistItem ||
                        it is ClipboardEntityRef.DayTask ||
                        it is ClipboardEntityRef.TacticalMission ||
                        it is ClipboardEntityRef.InboxRecord
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

        fun canPasteIntoTacticalMissions(): Boolean =
            clipboardService.payload.value?.entities?.any {
                it is ClipboardEntityRef.BacklogGoal ||
                    it is ClipboardEntityRef.BacklogItem ||
                    it is ClipboardEntityRef.BacklogContextLink ||
                    it is ClipboardEntityRef.DirectionItem ||
                    it is ClipboardEntityRef.ChecklistItem ||
                    it is ClipboardEntityRef.DayTask ||
                    it is ClipboardEntityRef.TacticalMission ||
                    it is ClipboardEntityRef.InboxRecord
            } == true

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

        fun copyInboxRecords(
            sourceContextId: String,
            recordIds: List<String>,
        ) {
            val refs = recordIds.distinct().map { ClipboardEntityRef.InboxRecord(recordId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun cutInboxRecords(
            sourceContextId: String,
            recordIds: List<String>,
        ) {
            val refs = recordIds.distinct().map { ClipboardEntityRef.InboxRecord(recordId = it) }
            if (refs.isEmpty()) return
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

        fun copyDayTasks(
            sourceContextId: String,
            taskIds: List<String>,
        ) {
            val refs = taskIds.distinct().map { ClipboardEntityRef.DayTask(taskId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun cutDayTasks(
            sourceContextId: String,
            taskIds: List<String>,
        ) {
            val refs = taskIds.distinct().map { ClipboardEntityRef.DayTask(taskId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.CUT,
                    entities = refs,
                ),
            )
        }

        fun copyTacticalMissions(
            sourceContextId: String,
            missionIds: List<Long>,
        ) {
            val refs = missionIds.distinct().map { ClipboardEntityRef.TacticalMission(missionId = it) }
            if (refs.isEmpty()) return
            clipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = sourceContextId,
                    operation = ClipboardOperation.COPY,
                    entities = refs,
                ),
            )
        }

        fun cutTacticalMissions(
            sourceContextId: String,
            missionIds: List<Long>,
        ) {
            val refs = missionIds.distinct().map { ClipboardEntityRef.TacticalMission(missionId = it) }
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
                    val dayTaskRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>()
                    val tacticalMissionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>()
                    if (areAllEntityGroupsEmpty(goalRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs, dayTaskRefs, tacticalMissionRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopyToBacklog(
                            goalRefs = goalRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            dayTaskRefs = dayTaskRefs,
                            tacticalMissionRefs = tacticalMissionRefs,
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
                    val dayTaskRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>()
                    val tacticalMissionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>()
                    if (areAllEntityGroupsEmpty(itemRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs, dayTaskRefs, tacticalMissionRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCutToBacklog(
                            itemRefs = itemRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            dayTaskRefs = dayTaskRefs,
                            tacticalMissionRefs = tacticalMissionRefs,
                            sourceContextId = payload.sourceContextId,
                            targetContextId = targetContextId,
                            mode = mode,
                            includeAttachments = includeAttachments,
                        )
                    }
                }
            }
        }

        suspend fun pasteIntoTacticalMissions(): TacticalMissionPasteReport {
            val payload = clipboardService.payload.value ?: return TacticalMissionPasteReport(totalRequested = 0, skippedInvalid = 1)
            val now = System.currentTimeMillis()
            val missionsToInsert = mutableListOf<TacticalMission>()
            val backlogItemsToDelete = mutableListOf<String>()
            val directionItemsToDelete = mutableListOf<String>()
            val checklistItemsToDelete = mutableListOf<String>()
            val dayTaskIdsToDelete = mutableListOf<String>()
            val missionIdsToDelete = mutableListOf<Long>()
            val inboxRecordIdsToDelete = mutableListOf<String>()
            var skippedInvalid = 0

            val copiedGoalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>()
            copiedGoalRefs.forEach { ref ->
                val goal = goalRepository.getGoalById(ref.goalId)
                val title = goal?.text?.trim().orEmpty()
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = null,
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = payload.sourceContextId.ifBlank { null },
                            linkedProjectIds = payload.sourceContextId.takeIf { it.isNotBlank() }?.let(::listOf) ?: emptyList(),
                            linkedAttachmentIds = emptyList(),
                        )
                }
            }

            val contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>()
            contextRefs.forEach { ref ->
                val context = contextRepository.getContextById(ref.contextId)
                val title = context?.name?.trim().orEmpty()
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = null,
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = ref.contextId,
                            linkedProjectIds = listOf(ref.contextId),
                            linkedAttachmentIds = emptyList(),
                        )
                }
            }

            val directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>()
            val directionItems = loadDirectionItems(directionRefs)
            skippedInvalid += directionRefs.size - directionItems.size
            directionItems.forEach { item ->
                val title = item.text.trim()
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = null,
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = item.linkedContextId,
                            linkedProjectIds = item.linkedContextId?.let(::listOf) ?: emptyList(),
                            linkedAttachmentIds = emptyList(),
                        )
                    if (payload.operation == ClipboardOperation.CUT) {
                        directionItemsToDelete += item.id
                    }
                }
            }

            val checklistRefs = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>()
            val checklistItems = loadChecklistItems(checklistRefs)
            skippedInvalid += checklistRefs.size - checklistItems.size
            checklistItems.forEach { item ->
                val title = item.content.trim()
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = null,
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = payload.sourceContextId.ifBlank { null },
                            linkedProjectIds = payload.sourceContextId.takeIf { it.isNotBlank() }?.let(::listOf) ?: emptyList(),
                            linkedAttachmentIds = emptyList(),
                        )
                    if (payload.operation == ClipboardOperation.CUT) {
                        checklistItemsToDelete += item.id
                    }
                }
            }

            val dayTaskRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>()
            dayTaskRefs.forEach { ref ->
                val task = dayManagementRepository.getTaskById(ref.taskId)
                val title = task?.title?.trim().orEmpty().ifBlank { task?.description?.trim().orEmpty() }
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = task?.description?.takeIf { !it.isNullOrBlank() },
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = task?.projectId,
                            linkedProjectIds = task?.linkedProjectIds.orEmpty(),
                            linkedAttachmentIds = emptyList(),
                        )
                    if (payload.operation == ClipboardOperation.CUT) {
                        dayTaskIdsToDelete += ref.taskId
                    }
                }
            }

            val missionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>()
            missionRefs.forEach { ref ->
                val mission = missionRepository.getMissionById(ref.missionId)
                if (mission == null) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        mission.copy(
                            id = 0,
                            order = 0L,
                        )
                    if (payload.operation == ClipboardOperation.CUT) {
                        missionIdsToDelete += mission.id
                    }
                }
            }

            val inboxRefs = payload.entities.filterIsInstance<ClipboardEntityRef.InboxRecord>()
            loadInboxRecords(inboxRefs).forEach { record ->
                val title = record.text.trim()
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = null,
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = payload.sourceContextId.ifBlank { null },
                            linkedProjectIds = payload.sourceContextId.takeIf { it.isNotBlank() }?.let(::listOf) ?: emptyList(),
                            linkedAttachmentIds = emptyList(),
                        )
                    if (payload.operation == ClipboardOperation.CUT) {
                        inboxRecordIdsToDelete += record.id
                    }
                }
            }

            val backlogItemRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>()
            val backlogItemsById = listItemRepository.getItemsByIds(backlogItemRefs.map { it.listItemId }).associateBy { it.id }
            backlogItemRefs.forEach { ref ->
                val item = backlogItemsById[ref.listItemId]
                if (item == null) {
                    skippedInvalid += 1
                    return@forEach
                }
                val title =
                    when (item.itemType) {
                        BacklogItemTypeValues.GOAL -> goalRepository.getGoalById(item.entityId)?.text?.trim().orEmpty()
                        BacklogItemTypeValues.SUBLIST -> contextRepository.getContextById(item.entityId)?.name?.trim().orEmpty()
                        else -> ""
                    }
                if (title.isBlank()) {
                    skippedInvalid += 1
                } else {
                    val projectId =
                        if (item.itemType == BacklogItemTypeValues.SUBLIST) {
                            item.entityId
                        } else {
                            item.contextId.takeIf { it.isNotBlank() }
                        }
                    missionsToInsert +=
                        TacticalMission(
                            title = title,
                            description = null,
                            deadline = now,
                            status = MissionStatus.ACTIVE,
                            projectId = projectId,
                            linkedProjectIds = projectId?.let(::listOf) ?: emptyList(),
                            linkedAttachmentIds = emptyList(),
                        )
                    if (payload.operation == ClipboardOperation.CUT) {
                        backlogItemsToDelete += item.id
                    }
                }
            }

            var createdMissions = 0
            missionsToInsert.forEach { mission ->
                val insertedId = missionRepository.insertMissionWithAutoOrder(mission)
                missionRepository.setAttachments(insertedId, mission.linkedAttachmentIds.orEmpty())
                createdMissions += 1
            }

            var moved = 0
            if (payload.operation == ClipboardOperation.CUT && createdMissions > 0) {
                if (backlogItemsToDelete.isNotEmpty()) {
                    listItemRepository.deleteListItems(backlogItemsToDelete.distinct())
                    moved += backlogItemsToDelete.distinct().size
                }
                if (directionItemsToDelete.isNotEmpty()) {
                    directionRepository.deleteDirectionItems(directionItemsToDelete.distinct())
                    moved += directionItemsToDelete.distinct().size
                }
                if (checklistItemsToDelete.isNotEmpty()) {
                    checklistRepository.deleteItems(checklistItemsToDelete.distinct())
                    moved += checklistItemsToDelete.distinct().size
                }
                dayTaskIdsToDelete.distinct().forEach { taskId ->
                    dayManagementRepository.deleteTask(taskId)
                    moved += 1
                }
                missionIdsToDelete.distinct().forEach { missionId ->
                    missionRepository.deleteMissionById(missionId)
                    moved += 1
                }
                inboxRecordIdsToDelete.distinct().forEach { recordId ->
                    inboxRepository.deleteInboxRecordById(recordId)
                    moved += 1
                }
                clipboardService.clear()
            }

            return TacticalMissionPasteReport(
                totalRequested = payload.entities.size,
                createdMissions = createdMissions,
                moved = moved,
                skippedInvalid = skippedInvalid,
            )
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
                    val dayTaskRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>()
                    val tacticalMissionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>()
                    if (areAllEntityGroupsEmpty(goalRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs, dayTaskRefs, tacticalMissionRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCopyToDirection(
                            goalRefs = goalRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            dayTaskRefs = dayTaskRefs,
                            tacticalMissionRefs = tacticalMissionRefs,
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
                    val dayTaskRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>()
                    val tacticalMissionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>()
                    if (areAllEntityGroupsEmpty(itemRefs, contextRefs, directionRefs, attachmentRefs, checklistRefs, dayTaskRefs, tacticalMissionRefs)) {
                        BacklogPasteReport(totalRequested = 0, skippedInvalid = 1)
                    } else {
                        pasteCutToDirection(
                            itemRefs = itemRefs,
                            contextRefs = contextRefs,
                            directionRefs = directionRefs,
                            attachmentRefs = attachmentRefs,
                            checklistRefs = checklistRefs,
                            dayTaskRefs = dayTaskRefs,
                            tacticalMissionRefs = tacticalMissionRefs,
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

        suspend fun pasteIntoInbox(targetContextId: String): Int {
            val payload = clipboardService.payload.value ?: return 0
            if (targetContextId.isBlank()) return 0
            val textItems = resolveStructuredTextClipboardItems(
                dayTaskRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>(),
                tacticalMissionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>(),
                directionRefs = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>(),
                checklistRefs = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>(),
                backlogGoalRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>(),
                backlogItemRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>(),
                contextRefs = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>(),
                inboxRefs = payload.entities.filterIsInstance<ClipboardEntityRef.InboxRecord>(),
                operation = payload.operation,
            )
            if (textItems.isEmpty()) return 0
            textItems.forEach { item ->
                inboxRepository.addInboxRecord(item.text, targetContextId)
            }
            if (payload.operation == ClipboardOperation.CUT) {
                finalizeStructuredTextCut(textItems)
                clipboardService.clear()
            }
            return textItems.size
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
            dayTaskRefs: List<ClipboardEntityRef.DayTask>,
            tacticalMissionRefs: List<ClipboardEntityRef.TacticalMission>,
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
            val structuredTextItems =
                resolveStructuredTextClipboardItems(
                    dayTaskRefs = dayTaskRefs,
                    tacticalMissionRefs = tacticalMissionRefs,
                    directionRefs = emptyList(),
                    checklistRefs = emptyList(),
                    backlogGoalRefs = emptyList(),
                    backlogItemRefs = emptyList(),
                    contextRefs = emptyList(),
                    inboxRefs = emptyList(),
                    operation = ClipboardOperation.COPY,
                )

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

            structuredTextItems.forEach { textItem ->
                val normalizedText = normalizeText(textItem.text)
                if (normalizedText.isBlank()) {
                    skippedInvalid += 1
                } else if (normalizedText in existingGoalTexts) {
                    skippedDuplicates += 1
                } else {
                    goalRepository.addGoalToContext(textItem.text.trim(), targetContextId)
                    existingGoalTexts += normalizedText
                    createdGoals += 1
                }
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
                        checklistRefs.size +
                        dayTaskRefs.size +
                        tacticalMissionRefs.size,
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
            dayTaskRefs: List<ClipboardEntityRef.DayTask>,
            tacticalMissionRefs: List<ClipboardEntityRef.TacticalMission>,
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
            val structuredTextItems =
                resolveStructuredTextClipboardItems(
                    dayTaskRefs = dayTaskRefs,
                    tacticalMissionRefs = tacticalMissionRefs,
                    directionRefs = emptyList(),
                    checklistRefs = emptyList(),
                    backlogGoalRefs = emptyList(),
                    backlogItemRefs = emptyList(),
                    contextRefs = emptyList(),
                    inboxRefs = emptyList(),
                    operation = ClipboardOperation.CUT,
                )

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

            val existingGoalTextsFromStructured = getExistingBacklogGoalTexts(targetContextId).toMutableSet()
            structuredTextItems.forEach { textItem ->
                val normalizedText = normalizeText(textItem.text)
                if (normalizedText.isBlank()) {
                    skippedInvalid += 1
                } else if (normalizedText in existingGoalTextsFromStructured) {
                    skippedDuplicates += 1
                } else {
                    goalRepository.addGoalToContext(textItem.text.trim(), targetContextId)
                    existingGoalTextsFromStructured += normalizedText
                    createdGoals += 1
                    moved += finalizeStructuredTextCut(listOf(textItem))
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
                        checklistRefs.size +
                        dayTaskRefs.size +
                        tacticalMissionRefs.size,
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
            dayTaskRefs: List<ClipboardEntityRef.DayTask>,
            tacticalMissionRefs: List<ClipboardEntityRef.TacticalMission>,
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
            val structuredTextItems =
                resolveStructuredTextClipboardItems(
                    dayTaskRefs = dayTaskRefs,
                    tacticalMissionRefs = tacticalMissionRefs,
                    directionRefs = emptyList(),
                    checklistRefs = emptyList(),
                    backlogGoalRefs = emptyList(),
                    backlogItemRefs = emptyList(),
                    contextRefs = emptyList(),
                    inboxRefs = emptyList(),
                    operation = ClipboardOperation.COPY,
                )

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

            structuredTextItems.forEach { textItem ->
                val normalized = normalizeText(textItem.text)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                } else if (normalized in existingTexts) {
                    skippedDuplicates += 1
                } else {
                    itemsToCreate += textItem.text.trim() to null
                    existingTexts += normalized
                }
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
                        checklistRefs.size +
                        dayTaskRefs.size +
                        tacticalMissionRefs.size,
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
            dayTaskRefs: List<ClipboardEntityRef.DayTask>,
            tacticalMissionRefs: List<ClipboardEntityRef.TacticalMission>,
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
            val structuredTextItems =
                resolveStructuredTextClipboardItems(
                    dayTaskRefs = dayTaskRefs,
                    tacticalMissionRefs = tacticalMissionRefs,
                    directionRefs = emptyList(),
                    checklistRefs = emptyList(),
                    backlogGoalRefs = emptyList(),
                    backlogItemRefs = emptyList(),
                    contextRefs = emptyList(),
                    inboxRefs = emptyList(),
                    operation = ClipboardOperation.CUT,
                )
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

            val structuredTextItemsToDelete = mutableListOf<StructuredTextClipboardItem>()
            structuredTextItems.forEach { textItem ->
                val normalized = normalizeText(textItem.text)
                if (normalized.isBlank()) {
                    skippedInvalid += 1
                } else if (normalized in existingTexts) {
                    skippedDuplicates += 1
                } else {
                    itemsToCreate += textItem.text.trim() to null
                    existingTexts += normalized
                    structuredTextItemsToDelete += textItem
                }
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
            if (structuredTextItemsToDelete.isNotEmpty()) {
                moved += finalizeStructuredTextCut(structuredTextItemsToDelete)
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
                        checklistRefs.size +
                        dayTaskRefs.size +
                        tacticalMissionRefs.size,
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

        private suspend fun loadInboxRecords(refs: List<ClipboardEntityRef.InboxRecord>) =
            refs.map { it.recordId }
                .let { recordIds -> inboxRepository.getInboxRecordsByIds(recordIds) }

        private data class StructuredTextClipboardItem(
            val text: String,
            val dayTaskIdToDelete: String? = null,
            val tacticalMissionIdToDelete: Long? = null,
            val inboxRecordIdToDelete: String? = null,
        )

        private suspend fun resolveStructuredTextClipboardItems(
            dayTaskRefs: List<ClipboardEntityRef.DayTask>,
            tacticalMissionRefs: List<ClipboardEntityRef.TacticalMission>,
            directionRefs: List<ClipboardEntityRef.DirectionItem>,
            checklistRefs: List<ClipboardEntityRef.ChecklistItem>,
            backlogGoalRefs: List<ClipboardEntityRef.BacklogGoal>,
            backlogItemRefs: List<ClipboardEntityRef.BacklogItem>,
            contextRefs: List<ClipboardEntityRef.BacklogContextLink>,
            inboxRefs: List<ClipboardEntityRef.InboxRecord> = emptyList(),
            operation: ClipboardOperation,
        ): List<StructuredTextClipboardItem> {
            val result = mutableListOf<StructuredTextClipboardItem>()
            val directionById = loadDirectionItems(directionRefs).associateBy { it.id }
            val checklistById = loadChecklistItems(checklistRefs).associateBy { it.id }
            val inboxById = loadInboxRecords(inboxRefs).associateBy { it.id }
            val backlogItemsById = listItemRepository.getItemsByIds(backlogItemRefs.map { it.listItemId }).associateBy { it.id }
            val goalTexts = backlogGoalRefs.associate { it.goalId to goalRepository.getGoalById(it.goalId)?.text.orEmpty() }
            val contextNames = contextRefs.associate { it.contextId to contextRepository.getContextById(it.contextId)?.name.orEmpty() }

            dayTaskRefs.forEach { ref ->
                val task = dayManagementRepository.getTaskById(ref.taskId) ?: return@forEach
                val text = task.title.trim().ifBlank { task.description?.trim().orEmpty() }
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text, dayTaskIdToDelete = if (operation == ClipboardOperation.CUT) task.id else null)
                }
            }
            tacticalMissionRefs.forEach { ref ->
                val mission = missionRepository.getMissionById(ref.missionId) ?: return@forEach
                val text = mission.title.trim().ifBlank { mission.description?.trim().orEmpty() }
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text, tacticalMissionIdToDelete = if (operation == ClipboardOperation.CUT) mission.id else null)
                }
            }
            inboxRefs.forEach { ref ->
                val record = inboxById[ref.recordId] ?: return@forEach
                val text = record.text.trim()
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text, inboxRecordIdToDelete = if (operation == ClipboardOperation.CUT) record.id else null)
                }
            }
            directionRefs.forEach { ref ->
                val item = directionById[ref.directionItemId] ?: return@forEach
                val text = item.text.trim()
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text)
                }
            }
            checklistRefs.forEach { ref ->
                val item = checklistById[ref.checklistItemId] ?: return@forEach
                val text = item.content.trim()
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text)
                }
            }
            backlogGoalRefs.forEach { ref ->
                val text = goalTexts[ref.goalId].orEmpty().trim()
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text)
                }
            }
            backlogItemRefs.forEach { ref ->
                val item = backlogItemsById[ref.listItemId] ?: return@forEach
                when (item.itemType) {
                    BacklogItemTypeValues.GOAL -> {
                        val text = goalRepository.getGoalById(item.entityId)?.text?.trim().orEmpty()
                        if (text.isNotBlank()) result += StructuredTextClipboardItem(text = text)
                    }
                    BacklogItemTypeValues.SUBLIST -> {
                        val text = contextRepository.getContextById(item.entityId)?.name?.trim().orEmpty()
                        if (text.isNotBlank()) result += StructuredTextClipboardItem(text = text)
                    }
                }
            }
            contextRefs.forEach { ref ->
                val text = contextNames[ref.contextId].orEmpty().trim()
                if (text.isNotBlank()) {
                    result += StructuredTextClipboardItem(text = text)
                }
            }
            return result
        }

        private suspend fun finalizeStructuredTextCut(items: List<StructuredTextClipboardItem>): Int {
            var moved = 0
            items.mapNotNull { it.dayTaskIdToDelete }.distinct().forEach { taskId ->
                dayManagementRepository.deleteTask(taskId)
                moved += 1
            }
            items.mapNotNull { it.tacticalMissionIdToDelete }.distinct().forEach { missionId ->
                missionRepository.deleteMissionById(missionId)
                moved += 1
            }
            items.mapNotNull { it.inboxRecordIdToDelete }.distinct().forEach { recordId ->
                inboxRepository.deleteInboxRecordById(recordId)
                moved += 1
            }
            return moved
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
