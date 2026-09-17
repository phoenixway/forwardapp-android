package com.romankozak.forwardappmobile.core.data.models.entities

import com.google.gson.annotations.SerializedName

/**
 * Read model for a project linked from a backlog item.
 *
 * Presentation fields are supplied by the canonical presentation authority.
 * Operational fields remain transitional read state and may be populated from
 * a legacy Context backing row while Context persistence is being retired.
 */
data class ContextLinkProjectReadModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("order") val order: Long = 0,
    @SerializedName("roleCode") val roleCode: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long = 0,
    @SerializedName("relatedLinks") val relatedLinks: List<RelatedLink>? = null,
    @SerializedName("scoringStatus") val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    @SerializedName("displayScore") val displayScore: Int = 0,
) {
    companion object {
        fun fromLegacyContext(context: Context): ContextLinkProjectReadModel =
            ContextLinkProjectReadModel(
                id = context.id,
                name = context.name,
                description = context.description,
                parentId = context.parentId,
                order = context.order,
                roleCode = context.roleCode,
                tags = context.tags,
                isCompleted = context.isCompleted,
                createdAt = context.createdAt,
                relatedLinks = context.relatedLinks,
                scoringStatus = context.scoringStatus,
                displayScore = context.displayScore,
            )
    }
}


sealed class BacklogItemContent {
    abstract val backlogItem: BacklogItem

    data class GoalItem(
        @SerializedName("goal") val goal: Goal,
        @SerializedName("reminders") val reminders: List<Reminder>,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem
    ) : BacklogItemContent()

    data class ContextLinkItem(
        @SerializedName("project") val project: ContextLinkProjectReadModel,
        @SerializedName("reminders") val reminders: List<Reminder>,
        @SerializedName("backlogItem") override val backlogItem: BacklogItem,
        @SerializedName("legacyProject") val legacyProject: Context? = null,
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
