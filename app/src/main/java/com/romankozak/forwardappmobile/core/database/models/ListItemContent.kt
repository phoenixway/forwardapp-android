package com.romankozak.forwardappmobile.core.database.models

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder

sealed class ListItemContent {
    
    
    abstract val listItem: ListItem

    data class GoalItem(val goal: Goal, val reminders: List<Reminder>, override val listItem: ListItem) : ListItemContent()

    data class SublistItem(val project: Project, val reminders: List<Reminder>, override val listItem: ListItem) : ListItemContent()

    data class LinkItem(val link: RelatedLink, override val listItem: ListItem) : ListItemContent()

    data class NoteItem(val note: LegacyNoteEntity, override val listItem: ListItem) : ListItemContent()

    data class NoteDocumentItem(val document: NoteDocumentEntity, override val listItem: ListItem) : ListItemContent()

    data class ChecklistItem(val checklist: ChecklistEntity, override val listItem: ListItem) : ListItemContent()
}
