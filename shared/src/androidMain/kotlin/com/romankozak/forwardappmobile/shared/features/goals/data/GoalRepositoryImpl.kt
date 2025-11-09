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

import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : GoalRepository {

    override suspend fun insertGoal(goal: Goal) {
        withContext(ioDispatcher) {
            db.goalQueries.insertGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = if (goal.completed) 1 else 0,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                valueImportance = goal.valueImportance.toDouble(),
                valueImpact = goal.valueImpact.toDouble(),
                effort = goal.effort.toDouble(),
                cost = goal.cost.toDouble(),
                risk = goal.risk.toDouble(),
                weightEffort = goal.weightEffort.toDouble(),
                weightCost = goal.weightCost.toDouble(),
                weightRisk = goal.weightRisk.toDouble(),
                rawScore = goal.rawScore.toDouble(),
                displayScore = goal.displayScore.toLong(),
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance?.toDouble(),
                impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                timeCost = goal.timeCost?.toDouble(),
                financialCost = goal.financialCost?.toDouble()
            )
        }
    }

    override suspend fun insertGoals(goals: List<Goal>) {
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                db.goalQueries.insertGoal(
                    id = goal.id,
                    text = goal.text,
                    description = goal.description,
                    completed = if (goal.completed) 1 else 0,
                    createdAt = goal.createdAt,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags,
                    relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                    valueImportance = goal.valueImportance.toDouble(),
                    valueImpact = goal.valueImpact.toDouble(),
                    effort = goal.effort.toDouble(),
                    cost = goal.cost.toDouble(),
                    risk = goal.risk.toDouble(),
                    weightEffort = goal.weightEffort.toDouble(),
                    weightCost = goal.weightCost.toDouble(),
                    weightRisk = goal.weightRisk.toDouble(),
                    rawScore = goal.rawScore.toDouble(),
                    displayScore = goal.displayScore.toLong(),
                    scoringStatus = goal.scoringStatus,
                    parentValueImportance = goal.parentValueImportance?.toDouble(),
                    impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                    timeCost = goal.timeCost?.toDouble(),
                    financialCost = goal.financialCost?.toDouble()
                )
            }
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        withContext(ioDispatcher) {
            db.goalQueries.updateGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = if (goal.completed) 1 else 0,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                valueImportance = goal.valueImportance.toDouble(),
                valueImpact = goal.valueImpact.toDouble(),
                effort = goal.effort.toDouble(),
                cost = goal.cost.toDouble(),
                risk = goal.risk.toDouble(),
                weightEffort = goal.weightEffort.toDouble(),
                weightCost = goal.weightCost.toDouble(),
                weightRisk = goal.weightRisk.toDouble(),
                rawScore = goal.rawScore.toDouble(),
                displayScore = goal.displayScore.toLong(),
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance?.toDouble(),
                impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                timeCost = goal.timeCost?.toDouble(),
                financialCost = goal.financialCost?.toDouble()
            )
        }
    }

    override suspend fun updateGoals(goals: List<Goal>) {
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                db.goalQueries.updateGoal(
                    id = goal.id,
                    text = goal.text,
                    description = goal.description,
                    completed = if (goal.completed) 1 else 0,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags,
                    relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                    valueImportance = goal.valueImportance.toDouble(),
                    valueImpact = goal.valueImpact.toDouble(),
                    effort = goal.effort.toDouble(),
                    cost = goal.cost.toDouble(),
                    risk = goal.risk.toDouble(),
                    weightEffort = goal.weightEffort.toDouble(),
                    weightCost = goal.weightCost.toDouble(),
                    weightRisk = goal.weightRisk.toDouble(),
                    rawScore = goal.rawScore.toDouble(),
                    displayScore = goal.displayScore.toLong(),
                    scoringStatus = goal.scoringStatus,
                    parentValueImportance = goal.parentValueImportance?.toDouble(),
                    impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                    timeCost = goal.timeCost?.toDouble(),
                    financialCost = goal.financialCost?.toDouble()
                )
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
