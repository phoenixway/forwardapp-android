package com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model

import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocument
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.Checklist

sealed class ListItemContent {
    abstract val listItem: ListItem

    data class GoalItem(
        val goal: Goal,
        override val listItem: ListItem
    ) : ListItemContent()

    data class SublistItem(
        val project: Project,
        override val listItem: ListItem
    ) : ListItemContent()

    data class LinkItem(
        val link: com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink,
        override val listItem: ListItem
    ) : ListItemContent()

    data class NoteItem(
        val note: LegacyNote,
        override val listItem: ListItem
    ) : ListItemContent()

    data class NoteDocumentItem(
        val document: NoteDocument,
        override val listItem: ListItem
    ) : ListItemContent()

    data class ChecklistItem(
        val checklist: Checklist,
        override val listItem: ListItem
    ) : ListItemContent()
}
