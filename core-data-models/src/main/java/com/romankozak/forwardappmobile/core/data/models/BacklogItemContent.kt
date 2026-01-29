package com.romankozak.forwardappmobile.core.data.models

sealed class BacklogItemContent {
    abstract val backlogItem: BacklogItem

    data class GoalItem(val goal: Goal, val reminders: List<Reminder>, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class SublistItem(val project: Context, val reminders: List<Reminder>, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class LinkItem(val link: LinkItemEntity, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class NoteItem(val note: LegacyNoteEntity, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class NoteDocumentItem(val document: NoteDocumentEntity, override val backlogItem: BacklogItem) : BacklogItemContent()

    data class ChecklistItem(val checklist: ChecklistEntity, override val backlogItem: BacklogItem) : BacklogItemContent()
}