package com.romankozak.forwardappmobile.shared.features.goals.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.Goal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : GoalRepository {

    override suspend fun insertGoal(goal: Goal) {
        withContext(ioDispatcher) {
            db.goalsQueries.insertGoal(goal.toSqlDelight())
        }
    }

    override suspend fun insertGoals(goals: List<Goal>) {
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                db.goalsQueries.insertGoal(goal.toSqlDelight())
            }
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        withContext(ioDispatcher) {
            db.goalsQueries.updateGoal(goal.toSqlDelight())
        }
    }

    override suspend fun updateGoals(goals: List<Goal>) {
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                db.goalsQueries.updateGoal(goal.toSqlDelight())
            }
        }
    }

    override suspend fun deleteGoalById(id: String) {
        withContext(ioDispatcher) {
            db.goalsQueries.deleteGoalById(id)
        }
    }

    override suspend fun getGoalById(id: String): Goal? {
        return withContext(ioDispatcher) {
            db.goalsQueries.getGoalById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        return db.goalsQueries.getGoalsByIds(ids)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal> {
        return withContext(ioDispatcher) {
            db.goalsQueries.getGoalsByIds(ids).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<Goal> {
        return withContext(ioDispatcher) {
            db.goalsQueries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override fun getAllGoalsFlow(): Flow<List<Goal>> {
        return db.goalsQueries.getAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun searchGoalsByText(query: String): Flow<List<Goal>> {
        return db.goalsQueries.searchGoalsByText(query)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun getAllGoalsCountFlow(): Flow<Int> {
        return db.goalsQueries.getAllGoalsCount()
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }
    }

    override suspend fun updateMarkdown(
        goalId: String,
        markdown: String,
    ) {
        withContext(ioDispatcher) {
            db.goalsQueries.updateMarkdown(goalId, markdown)
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            db.goalsQueries.deleteAll()
        }
    }
}
