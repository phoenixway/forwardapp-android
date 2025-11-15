package com.romankozak.forwardappmobile.shared.features.search.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.data.database.models.GlobalSearchResult
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.search.domain.repository.SearchRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.mappers.toDomain as recurringTaskToDomain
import com.romankozak.forwardappmobile.shared.features.goals.data.mappers.toDomain as goalToDomain
import com.romankozak.forwardappmobile.shared.features.projects.core.data.mappers.toDomain as projectToDomain
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.mappers.toDomain as noteToDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SearchRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : SearchRepository {

    override fun search(query: String): Flow<List<GlobalSearchResult>> {
        val goals = db.goalsQueries.searchGoals(query).asFlow().mapToList(dispatcher)
        val projects = db.projectsQueries.searchProjects(query).asFlow().mapToList(dispatcher)
        val notes = db.notesQueries.searchNotes(query).asFlow().mapToList(dispatcher)
        val recurringTasks = db.recurringTasksQueries.searchRecurringTasks(query).asFlow().mapToList(dispatcher)

        return combine(goals, projects, notes, recurringTasks) { goalsResult, projectsResult, notesResult, recurringTasksResult ->
            val results = mutableListOf<GlobalSearchResult>()
            results.addAll(goalsResult.map { GlobalSearchResult.GoalResult(it.goalToDomain()) })
            results.addAll(projectsResult.map { GlobalSearchResult.ProjectResult(it.projectToDomain()) })
            results.addAll(notesResult.map { GlobalSearchResult.NoteResult(it.noteToDomain()) })
            results.addAll(recurringTasksResult.map { GlobalSearchResult.RecurringTaskResult(it.recurringTaskToDomain()) })
            results
        }
    }
}
