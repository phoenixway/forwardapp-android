package com.romankozak.forwardappmobile.features.contexts.data.models

import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder

sealed class BacklogItemContent {
    abstract val backlogItem: BacklogItem

    data class GoalItem(val goal: Goal, val reminders: List<Reminder>, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class SublistItem(val project: Context, val reminders: List<Reminder>, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class LinkItem(val link: LinkItemEntity, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class NoteItem(val note: LegacyNoteEntity, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class NoteDocumentItem(val document: NoteDocumentEntity, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class ChecklistItem(val checklist: ChecklistEntity, override val backlogItem: BacklogItem) : BacklogItemContent()
}
