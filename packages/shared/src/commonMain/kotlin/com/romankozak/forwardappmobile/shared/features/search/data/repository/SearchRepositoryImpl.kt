package com.romankozak.forwardappmobile.shared.features.search.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.core.platform.Platform
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
        val goals = if (Platform.isAndroid) {
            db.goalsQueries.searchGoalsFts(query)
        } else {
            db.goalsQueries.searchGoalsFallback(query)
        }
        val projects = if (Platform.isAndroid) {
            db.projectsQueries.searchProjectsFts(query)
        } else {
            db.projectsQueries.searchProjectsFallback(query)
        }
        val notes = if (Platform.isAndroid) {
            db.notesQueries.searchNotesFts(query)
        } else {
            db.notesQueries.searchNotesFallback(query)
        }
        val recurringTasks = if (Platform.isAndroid) {
            db.recurringTasksQueries.searchRecurringTasksFts(query)
        } else {
            db.recurringTasksQueries.searchRecurringTasksFallback(query)
        }

        return combine(
            goals.asFlow().mapToList(dispatcher),
            projects.asFlow().mapToList(dispatcher),
            notes.asFlow().mapToList(dispatcher),
            recurringTasks.asFlow().mapToList(dispatcher)
        ) { goalsResult, projectsResult, notesResult, recurringTasksResult ->
            val results = mutableListOf<GlobalSearchResult>()
            results.addAll(goalsResult.map { GlobalSearchResult.GoalResult(it.goalToDomain()) })
            results.addAll(projectsResult.map { GlobalSearchResult.ProjectResult(it.projectToDomain()) })
            results.addAll(notesResult.map { GlobalSearchResult.NoteResult(it.noteToDomain()) })
            results.addAll(recurringTasksResult.map { GlobalSearchResult.RecurringTaskResult(it.recurringTaskToDomain()) })
            results
        }
    }
}
