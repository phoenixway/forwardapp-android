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
    @Suppress("UNCHECKED_CAST")
    fun map(
        contextId: String,
        args: Array<Any?>,
    ): ContextData.Loaded {
        val context = args[0] as? Context
        val rawItems = (args[1] as? List<*>)?.filterIsInstance<BacklogItem>() ?: emptyList()
        val checklists = (args[4] as? List<*>)?.filterIsInstance<ChecklistEntity>() ?: emptyList()
        val noteDocuments = (args[5] as? List<*>)?.filterIsInstance<NoteDocumentEntity>() ?: emptyList()
        val musicNotes = (args[6] as? List<*>)?.filterIsInstance<MusicNoteEntity>() ?: emptyList()
        val directionItems = (args[7] as? List<*>)?.filterIsInstance<DirectionItemEntity>() ?: emptyList()
        val allContexts = (args[8] as? List<*>)?.filterIsInstance<Context>() ?: emptyList()
        val attachments = (args[9] as? List<*>)?.filterIsInstance<AttachmentWithContext>() ?: emptyList()
        val linkItems = (args[10] as? List<*>)?.filterIsInstance<LinkItemEntity>() ?: emptyList()
        val reminders = (args[11] as? List<*>)?.filterIsInstance<Reminder>() ?: emptyList()
        val goals = (args[14] as? List<*>)?.filterIsInstance<Goal>() ?: emptyList()
        val subprojects = (args[15] as? List<*>)?.filterIsInstance<Context>() ?: emptyList()

        val linkItemsMap = linkItems.associateBy { it.id }

        val items: List<BacklogItemContent> =
            rawItems.mapNotNull { item ->
                val itemReminders = reminders.filter { it.entityId == item.entityId }
                when (item.itemType) {
                    BacklogItemTypeValues.GOAL -> {
                        goals.find { it.id == item.entityId }?.let { foundGoal ->
                            BacklogItemContent.GoalItem(
                                goal = foundGoal,
                                backlogItem = item,
                                reminders = itemReminders,
                            )
                        }
                    }
                    BacklogItemTypeValues.SUBLIST, "PROJECT" -> {
                        // Context links in backlog can target any context, not only direct children.
                        (allContexts.find { it.id == item.entityId } ?: subprojects.find { it.id == item.entityId })?.let {
                                foundSubProject ->
                            BacklogItemContent.ContextLinkItem(
                                project = foundSubProject,
                                backlogItem = item,
                                reminders = itemReminders,
                            )
                        }
                    }
                    BacklogItemTypeValues.NOTE_DOCUMENT -> {
                        noteDocuments.find { it.id == item.entityId }?.let { foundDoc ->
                            BacklogItemContent.NoteDocumentItem(
                                document = foundDoc,
                                backlogItem = item,
                            )
                        }
                    }
                    BacklogItemTypeValues.CHECKLIST -> {
                        checklists.find { it.id == item.entityId }?.let { foundChecklist ->
                            BacklogItemContent.ChecklistItem(
                                checklist = foundChecklist,
                                backlogItem = item,
                            )
                        }
                    }
                    BacklogItemTypeValues.MUSIC_NOTE -> {
                        musicNotes.find { it.id == item.entityId }?.let { foundMusicNote ->
                            BacklogItemContent.MusicNoteItem(
                                musicNote = foundMusicNote,
                                backlogItem = item,
                            )
                        }
                    }
                    BacklogItemTypeValues.LINK_ITEM -> {
                        linkItemsMap[item.entityId]?.let { linkItem ->
                            BacklogItemContent.LinkItem(linkItem, item)
                        }
                    }
                    "LINK" -> null
                    else -> null
                }
            }

        val noteDocumentsMap = noteDocuments.associateBy { it.id }
        val musicNotesMap = musicNotes.associateBy { it.id }
        val checklistsMap = checklists.associateBy { it.id }

        val attachmentItems =
            attachments
                .sortedWith { a, b ->
                    val orderA = a.attachmentOrder ?: -a.attachment.createdAt
                    val orderB = b.attachmentOrder ?: -b.attachment.createdAt
                    val orderCompare = orderA.compareTo(orderB)
                    if (orderCompare != 0) orderCompare else a.attachment.id.compareTo(b.attachment.id)
                }
                .mapNotNull { attachment ->
                    val backlogItem =
                        BacklogItem(
                            id = attachment.attachment.id,
                            contextId = contextId,
                            itemType = attachment.attachment.attachmentType,
                            entityId = attachment.attachment.entityId,
                            order = attachment.attachmentOrder ?: -attachment.attachment.createdAt,
                        )
                    when (attachment.attachment.attachmentType) {
                        BacklogItemTypeValues.NOTE_DOCUMENT ->
                            noteDocumentsMap[attachment.attachment.entityId]?.let { doc ->
                                BacklogItemContent.NoteDocumentItem(doc, backlogItem)
                            }
                        BacklogItemTypeValues.CHECKLIST ->
                            checklistsMap[attachment.attachment.entityId]?.let { checklist ->
                                BacklogItemContent.ChecklistItem(checklist, backlogItem)
                            }
                        BacklogItemTypeValues.MUSIC_NOTE ->
                            musicNotesMap[attachment.attachment.entityId]?.let { musicNote ->
                                BacklogItemContent.MusicNoteItem(musicNote, backlogItem)
                            }
                        BacklogItemTypeValues.LINK_ITEM ->
                            linkItemsMap[attachment.attachment.entityId]?.let { linkItem ->
                                BacklogItemContent.LinkItem(linkItem, backlogItem)
                            }
                        else -> null
                    }
                }

        val config = args[2] as? ContextConfiguration
        val logs = (args[3] as? List<*>)?.filterIsInstance<ContextLog>() ?: emptyList()
        val recentItems = (args[12] as? List<*>)?.filterIsInstance<RecentItem>() ?: emptyList()
        val notes = (args[13] as? List<*>)?.filterIsInstance<LegacyNoteEntity>() ?: emptyList()

        val linkedContextNames =
            if (directionItems.isEmpty()) {
                emptyMap()
            } else {
                val linkedIds = directionItems.mapNotNull { it.linkedContextId }.toSet()
                if (linkedIds.isEmpty()) {
                    emptyMap()
                } else {
                    val nameById = allContexts.associateBy({ it.id }, { it.name })
                    linkedIds.associateWith { id -> nameById[id] ?: "Context" }
                }
            }

        return ContextData.Loaded(
            context = context,
            items = items,
            attachmentItems = attachmentItems,
            config = config ?: ContextConfiguration.default(contextId),
            logs = logs,
            checklists = checklists,
            noteDocuments = noteDocuments,
            directionItems = directionItems,
            linkedContextNames = linkedContextNames,
            reminders = reminders,
            recentItems = recentItems,
            notes = notes,
        )
    }
}
