package com.romankozak.forwardappmobile.ui.screens.selectiveimport

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.sync.DatabaseContent

typealias Thought = LegacyNoteEntity
typealias Stats = ActivityRecord

data class SelectiveImportState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backupContent: SelectableDatabaseContent? = null
)

data class SelectableDatabaseContent(
    val projects: List<SelectableItem<Project>> = emptyList(),
    val goals: List<SelectableItem<Goal>> = emptyList(),
    val thoughts: List<SelectableItem<Thought>> = emptyList(),
    val stats: List<SelectableItem<Stats>> = emptyList()
)

data class SelectableItem<T>(
    val item: T,
    val isSelected: Boolean = true
)

fun DatabaseContent.toSelectable(): SelectableDatabaseContent {
    return SelectableDatabaseContent(
        projects = this.projects.map { SelectableItem(it) },
        goals = this.goals.map { SelectableItem(it) },
        thoughts = this.legacyNotes.map { SelectableItem(it) },
        stats = this.activityRecords.map { SelectableItem(it) }
    )
}
