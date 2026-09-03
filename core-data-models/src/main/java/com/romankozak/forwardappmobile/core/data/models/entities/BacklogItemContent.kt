package com.romankozak.forwardappmobile.core.data.models.entities

import com.google.gson.annotations.SerializedName

sealed class BacklogItemContent {
    abstract val backlogItem: BacklogItem

    data class GoalItem(
        @SerializedName("goal") val goal: Goal,
        @SerializedName("reminders") val reminders: List<Reminder>,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class ContextLinkItem(
        @SerializedName("project") val project: Context,
        @SerializedName("reminders") val reminders: List<Reminder>,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class LinkItem(
        @SerializedName("link") val link: LinkItemEntity,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class NoteItem(
        @SerializedName("note") val note: LegacyNoteEntity,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class NoteDocumentItem(
        @SerializedName("document") val document: NoteDocumentEntity,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class ChecklistItem(
        @SerializedName("checklist") val checklist: ChecklistEntity,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class MusicNoteItem(
        @SerializedName("musicNote") val musicNote: MusicNoteEntity,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()
}
