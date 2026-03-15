package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextData

class ContextScreenDataMapper {
    fun map(
        contextId: String,
        snapshot: ContextScreenDataSnapshot,
    ): ContextData.Loaded {
        val support = snapshot.toMappingSupport()

        return ContextData.Loaded(
            context = snapshot.context,
            items = snapshot.toBacklogItems(support),
            attachmentItems = snapshot.toAttachmentItems(contextId, support),
            config = snapshot.config ?: ContextConfiguration.default(contextId),
            logs = snapshot.logs,
            checklists = snapshot.checklists,
            noteDocuments = snapshot.noteDocuments,
            directionItems = snapshot.directionItems,
            linkedContextNames = snapshot.buildLinkedContextNames(),
            reminders = snapshot.reminders,
            recentItems = snapshot.recentItems,
            notes = snapshot.notes,
        )
    }
}

data class ContextScreenDataSnapshot(
    val context: Context?,
    val rawItems: List<BacklogItem>,
    val config: ContextConfiguration?,
    val logs: List<ContextLog>,
    val checklists: List<ChecklistEntity>,
    val noteDocuments: List<NoteDocumentEntity>,
    val musicNotes: List<MusicNoteEntity>,
    val directionItems: List<DirectionItemEntity>,
    val allContexts: List<Context>,
    val attachments: List<AttachmentWithContext>,
    val linkItems: List<LinkItemEntity>,
    val reminders: List<Reminder>,
    val recentItems: List<RecentItem>,
    val notes: List<LegacyNoteEntity>,
    val goals: List<Goal>,
    val subprojects: List<Context>,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromArgs(args: Array<Any?>): ContextScreenDataSnapshot {
            return ContextScreenDataSnapshot(
                context = args[CONTEXT_INDEX] as? Context,
                rawItems = args.itemsAt<BacklogItem>(RAW_ITEMS_INDEX),
                config = args[CONFIG_INDEX] as? ContextConfiguration,
                logs = args.itemsAt<ContextLog>(LOGS_INDEX),
                checklists = args.itemsAt<ChecklistEntity>(CHECKLISTS_INDEX),
                noteDocuments = args.itemsAt<NoteDocumentEntity>(NOTE_DOCUMENTS_INDEX),
                musicNotes = args.itemsAt<MusicNoteEntity>(MUSIC_NOTES_INDEX),
                directionItems = args.itemsAt<DirectionItemEntity>(DIRECTION_ITEMS_INDEX),
                allContexts = args.itemsAt<Context>(ALL_CONTEXTS_INDEX),
                attachments = args.itemsAt<AttachmentWithContext>(ATTACHMENTS_INDEX),
                linkItems = args.itemsAt<LinkItemEntity>(LINK_ITEMS_INDEX),
                reminders = args.itemsAt<Reminder>(REMINDERS_INDEX),
                recentItems = args.itemsAt<RecentItem>(RECENT_ITEMS_INDEX),
                notes = args.itemsAt<LegacyNoteEntity>(NOTES_INDEX),
                goals = args.itemsAt<Goal>(GOALS_INDEX),
                subprojects = args.itemsAt<Context>(SUBPROJECTS_INDEX),
            )
        }
    }
}

private inline fun <reified T> Array<Any?>.itemsAt(index: Int): List<T> {
    return (getOrNull(index) as? List<*>)?.filterIsInstance<T>() ?: emptyList()
}

private data class ContextScreenMappingSupport(
    val linkItemsById: Map<String, LinkItemEntity>,
    val noteDocumentsById: Map<String, NoteDocumentEntity>,
    val musicNotesById: Map<String, MusicNoteEntity>,
    val checklistsById: Map<String, ChecklistEntity>,
    val goalsById: Map<String, Goal>,
    val contextsById: Map<String, Context>,
    val subprojectsById: Map<String, Context>,
    val remindersByEntityId: Map<String, List<Reminder>>,
)

private fun ContextScreenDataSnapshot.toMappingSupport(): ContextScreenMappingSupport {
    return ContextScreenMappingSupport(
        linkItemsById = linkItems.associateBy { it.id },
        noteDocumentsById = noteDocuments.associateBy { it.id },
        musicNotesById = musicNotes.associateBy { it.id },
        checklistsById = checklists.associateBy { it.id },
        goalsById = goals.associateBy { it.id },
        contextsById = allContexts.associateBy { it.id },
        subprojectsById = subprojects.associateBy { it.id },
        remindersByEntityId = reminders.groupBy { it.entityId },
    )
}

private fun ContextScreenDataSnapshot.toBacklogItems(
    support: ContextScreenMappingSupport,
): List<BacklogItemContent> {
    return rawItems.mapNotNull { item ->
        item.toBacklogItemContent(
            support = support,
            reminders = support.remindersByEntityId[item.entityId].orEmpty(),
        )
    }
}

private fun BacklogItem.toBacklogItemContent(
    support: ContextScreenMappingSupport,
    reminders: List<Reminder>,
): BacklogItemContent? {
    return when (itemType) {
        BacklogItemTypeValues.GOAL -> toGoalItemContent(support, reminders)
        BacklogItemTypeValues.SUBLIST, PROJECT_ITEM_TYPE -> toContextLinkItemContent(support, reminders)
        BacklogItemTypeValues.NOTE_DOCUMENT,
        BacklogItemTypeValues.CHECKLIST,
        BacklogItemTypeValues.MUSIC_NOTE,
        BacklogItemTypeValues.LINK_ITEM,
        -> toAttachmentBackedItemContent(support)
        LEGACY_LINK_ITEM_TYPE -> null
        else -> null
    }
}

private fun ContextScreenDataSnapshot.toAttachmentItems(
    contextId: String,
    support: ContextScreenMappingSupport,
): List<BacklogItemContent> {
    return attachments
        .sortedWith(compareBy<AttachmentWithContext> { it.attachmentOrder ?: -it.attachment.createdAt }
            .thenBy { it.attachment.id })
        .mapNotNull { attachment ->
            attachment.toAttachmentItem(
                contextId = contextId,
                support = support,
            )
        }
}

private fun AttachmentWithContext.toAttachmentItem(
    contextId: String,
    support: ContextScreenMappingSupport,
): BacklogItemContent? {
    val backlogItem =
        BacklogItem(
            id = attachment.id,
            contextId = contextId,
            itemType = attachment.attachmentType,
            entityId = attachment.entityId,
            order = attachmentOrder ?: -attachment.createdAt,
        )

    return when (attachment.attachmentType) {
        BacklogItemTypeValues.NOTE_DOCUMENT ->
            support.noteDocumentsById[attachment.entityId]?.let { document ->
                BacklogItemContent.NoteDocumentItem(document, backlogItem)
            }
        BacklogItemTypeValues.CHECKLIST ->
            support.checklistsById[attachment.entityId]?.let { checklist ->
                BacklogItemContent.ChecklistItem(checklist, backlogItem)
            }
        BacklogItemTypeValues.MUSIC_NOTE ->
            support.musicNotesById[attachment.entityId]?.let { musicNote ->
                BacklogItemContent.MusicNoteItem(musicNote, backlogItem)
            }
        BacklogItemTypeValues.LINK_ITEM ->
            support.linkItemsById[attachment.entityId]?.let { linkItem ->
                BacklogItemContent.LinkItem(linkItem, backlogItem)
            }
        else -> null
    }
}

private fun ContextScreenDataSnapshot.buildLinkedContextNames(): Map<String, String> {
    val linkedIds = directionItems.mapNotNull { it.linkedContextId }.toSet()
    val namesById = allContexts.associateBy({ it.id }, { it.name })
    return linkedIds.takeIf { it.isNotEmpty() }
        ?.associateWith { id -> namesById[id] ?: DEFAULT_CONTEXT_NAME }
        ?: emptyMap()
}

private fun BacklogItem.toGoalItemContent(
    support: ContextScreenMappingSupport,
    reminders: List<Reminder>,
): BacklogItemContent? {
    return support.goalsById[entityId]?.let { goal ->
        BacklogItemContent.GoalItem(
            goal = goal,
            backlogItem = this,
            reminders = reminders,
        )
    }
}

private fun BacklogItem.toContextLinkItemContent(
    support: ContextScreenMappingSupport,
    reminders: List<Reminder>,
): BacklogItemContent? {
    val linkedContext = support.contextsById[entityId] ?: support.subprojectsById[entityId]
    return linkedContext?.let { project ->
        BacklogItemContent.ContextLinkItem(
            project = project,
            backlogItem = this,
            reminders = reminders,
        )
    }
}

private fun BacklogItem.toAttachmentBackedItemContent(
    support: ContextScreenMappingSupport,
): BacklogItemContent? {
    return when (itemType) {
        BacklogItemTypeValues.NOTE_DOCUMENT ->
            support.noteDocumentsById[entityId]?.let { document ->
                BacklogItemContent.NoteDocumentItem(
                    document = document,
                    backlogItem = this,
                )
            }
        BacklogItemTypeValues.CHECKLIST ->
            support.checklistsById[entityId]?.let { checklist ->
                BacklogItemContent.ChecklistItem(
                    checklist = checklist,
                    backlogItem = this,
                )
            }
        BacklogItemTypeValues.MUSIC_NOTE ->
            support.musicNotesById[entityId]?.let { musicNote ->
                BacklogItemContent.MusicNoteItem(
                    musicNote = musicNote,
                    backlogItem = this,
                )
            }
        BacklogItemTypeValues.LINK_ITEM ->
            support.linkItemsById[entityId]?.let { linkItem ->
                BacklogItemContent.LinkItem(linkItem, this)
            }
        else -> null
    }
}

private const val CONTEXT_INDEX = 0
private const val RAW_ITEMS_INDEX = 1
private const val CONFIG_INDEX = 2
private const val LOGS_INDEX = 3
private const val CHECKLISTS_INDEX = 4
private const val NOTE_DOCUMENTS_INDEX = 5
private const val MUSIC_NOTES_INDEX = 6
private const val DIRECTION_ITEMS_INDEX = 7
private const val ALL_CONTEXTS_INDEX = 8
private const val ATTACHMENTS_INDEX = 9
private const val LINK_ITEMS_INDEX = 10
private const val REMINDERS_INDEX = 11
private const val RECENT_ITEMS_INDEX = 12
private const val NOTES_INDEX = 13
private const val GOALS_INDEX = 14
private const val SUBPROJECTS_INDEX = 15
private const val PROJECT_ITEM_TYPE = "PROJECT"
private const val LEGACY_LINK_ITEM_TYPE = "LINK"
private const val DEFAULT_CONTEXT_NAME = "Context"
