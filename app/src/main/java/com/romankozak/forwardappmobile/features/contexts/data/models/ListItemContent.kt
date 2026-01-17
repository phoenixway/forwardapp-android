package com.romankozak.forwardappmobile.features.contexts.data.models

import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.Reminder

sealed class ListItemContent {


    abstract val listItem: ListItem

    data class GoalItem(val goal: Goal, val reminders: List<Reminder>, override val listItem: ListItem) : ListItemContent()

    data class SublistItem(val project: Project, val reminders: List<Reminder>, override val listItem: ListItem) : ListItemContent()

    data class LinkItem(val link: LinkItemEntity, override val listItem: ListItem) : ListItemContent()

    data class NoteItem(val note: LegacyNoteEntity, override val listItem: ListItem) : ListItemContent()

    data class NoteDocumentItem(val document: NoteDocumentEntity, override val listItem: ListItem) : ListItemContent()

    data class ChecklistItem(val checklist: ChecklistEntity, override val listItem: ListItem) : ListItemContent()
}
