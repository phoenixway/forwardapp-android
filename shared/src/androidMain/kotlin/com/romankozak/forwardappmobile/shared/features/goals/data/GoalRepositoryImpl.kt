package com.romankozak.forwardappmobile.shared.features.goals.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.Goal
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : GoalRepository {

    private val queries = db.goalsQueries

    override suspend fun insertGoal(goal: Goal) {
        withContext(ioDispatcher) {
            queries.insertGoal(goal.toSqlDelight())
        }
    }

    override suspend fun insertGoals(goals: List<Goal>) {
        withContext(ioDispatcher) {
            db.transaction {
                goals.forEach { goal ->
                    queries.insertGoal(goal.toSqlDelight())
                }
            }
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        withContext(ioDispatcher) {
            queries.updateGoal(goal.toSqlDelight())
        }
    }

    override suspend fun updateGoals(goals: List<Goal>) {
        withContext(ioDispatcher) {
            db.transaction {
                goals.forEach { goal ->
                    queries.updateGoal(goal.toSqlDelight())
                }
            }
        }
    }

    override suspend fun deleteGoalById(id: String) {
        withContext(ioDispatcher) {
            queries.deleteGoal(id)
        }
    }

    override suspend fun getGoalById(id: String): Goal? {
        return withContext(ioDispatcher) {
            queries.getGoalById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        return queries.getGoalsByIds(ids)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal> {
        return withContext(ioDispatcher) {
            queries.getGoalsByIds(ids).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<Goal> {
        return withContext(ioDispatcher) {
            queries.getAllGoals().executeAsList().map { it.toDomain() }
        }
    }

    override fun getAllGoalsFlow(): Flow<List<Goal>> {
        return queries.getAllGoals()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun searchGoalsByText(query: String): Flow<List<Goal>> {
        return queries.searchGoalsByText(query)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun getAllGoalsCountFlow(): Flow<Int> {
        return queries.getAllGoalsCount()
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }
    }

    override suspend fun updateMarkdown(
        goalId: String,
        markdown: String,
    ) {
        withContext(ioDispatcher) {
            queries.updateMarkdown(markdown, goalId)
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            queries.deleteAll()
        }
    }
}