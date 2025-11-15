package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.core.platform.Platform
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.features.goals.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : GoalRepository {

    private val queries = db.goalsQueries

    override fun getAllGoals(): Flow<List<Goal>> =
        queries.getAllGoals()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toDomain() } }

    override fun searchGoals(query: String): Flow<List<Goal>> {
        val goals = if (Platform.isAndroid) {
            queries.searchGoalsFts(query)
        } else {
            queries.searchGoalsFallback(query)
        }
        return goals.asFlow().mapToList(dispatcher).map { list -> list.map { it.toDomain() } }
    }

    override fun getGoalById(id: String): Flow<Goal?> =
        queries.getGoalById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }

    override suspend fun insertGoal(
        goal: Goal
    ): String =
        withContext(dispatcher) {
            queries.insertGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = goal.completed,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks,
                valueImportance = goal.valueImportance,
                valueImpact = goal.valueImpact,
                effort = goal.effort,
                cost = goal.cost,
                risk = goal.risk,
                weightEffort = goal.weightEffort,
                weightCost = goal.weightCost,
                weightRisk = goal.weightRisk,
                rawScore = goal.rawScore,
                displayScore = goal.displayScore,
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance,
                impactOnParentGoal = goal.impactOnParentGoal,
                timeCost = goal.timeCost,
                financialCost = goal.financialCost,
                markdown = goal.markdown
            )
            goal.id
        }

    override suspend fun updateGoal(
        goal: Goal
    ) {
        withContext(dispatcher) {
            queries.updateGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = goal.completed,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks,
                valueImportance = goal.valueImportance,
                valueImpact = goal.valueImpact,
                effort = goal.effort,
                cost = goal.cost,
                risk = goal.risk,
                weightEffort = goal.weightEffort,
                weightCost = goal.weightCost,
                weightRisk = goal.weightRisk,
                rawScore = goal.rawScore,
                displayScore = goal.displayScore,
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance,
                impactOnParentGoal = goal.impactOnParentGoal,
                timeCost = goal.timeCost,
                financialCost = goal.financialCost,
                markdown = goal.markdown
            )
        }
    }

    override suspend fun deleteGoal(id: String) {
        withContext(dispatcher) {
            queries.deleteGoal(id)
        }
    }

    override suspend fun deleteAllGoals() {
        withContext(dispatcher) {
            queries.deleteAll()
        }
    }

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        return queries.getGoalsByIds(ids)
            .asFlow()
            .mapToList(dispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }
}