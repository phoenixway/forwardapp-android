package com.romankozak.forwardappmobile.features.contexts.data.models

import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity

sealed class BacklogItemContent {


    abstract val listItem: ListItem

    data class GoalItem(val goal: Goal, val reminders: List<Reminder>, override val listItem: ListItem) : BacklogItemContent()

    data class SublistItem(val project: Context, val reminders: List<Reminder>, override val listItem: ListItem) : BacklogItemContent()

    data class LinkItem(val link: LinkItemEntity, override val listItem: ListItem) : BacklogItemContent()

    data class NoteItem(val note: LegacyNoteEntity, override val listItem: ListItem) : BacklogItemContent()

    data class NoteDocumentItem(val document: NoteDocumentEntity, override val listItem: ListItem) : BacklogItemContent()

    data class ChecklistItem(val checklist: ChecklistEntity, override val listItem: ListItem) : BacklogItemContent()
}
