package com.romankozak.forwardappmobile.shared.data.database.models

import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurringTask
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote

sealed class GlobalSearchResult {
    data class GoalResult(val goal: Goal) : GlobalSearchResult()
    data class ProjectResult(val project: Project) : GlobalSearchResult()
    data class NoteResult(val note: LegacyNote) : GlobalSearchResult()
    data class RecurringTaskResult(val task: RecurringTask) : GlobalSearchResult()
}
