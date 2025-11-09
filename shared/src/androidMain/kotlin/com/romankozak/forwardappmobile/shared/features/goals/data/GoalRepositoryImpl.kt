package com.romankozak.forwardappmobile.shared.features.goals.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.Goal
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : GoalRepository {

    override suspend fun insertGoal(goal: Goal) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.insertGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = goal.completed,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                tags = goal.tags?.let { Json.encodeToString(it) },
                relatedLinks = goal.relatedLinks,
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
                financialCost = goal.financialCost?.toDouble(),
                markdown = goal.markdown
            )
        }
    }

    override suspend fun insertGoals(goals: List<Goal>) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                queries.insertGoal(
                    id = goal.id,
                    text = goal.text,
                    description = goal.description,
                    completed = goal.completed,
                    createdAt = goal.createdAt,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags?.let { Json.encodeToString(it) },
                    relatedLinks = goal.relatedLinks,
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
                    financialCost = goal.financialCost?.toDouble(),
                    markdown = goal.markdown
                )
            }
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.updateGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = goal.completed,
                updatedAt = goal.updatedAt,
                tags = goal.tags?.let { Json.encodeToString(it) },
                relatedLinks = goal.relatedLinks,
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
                financialCost = goal.financialCost?.toDouble(),
                markdown = goal.markdown
            )
        }
    }

    override suspend fun updateGoals(goals: List<Goal>) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                queries.updateGoal(
                    id = goal.id,
                    text = goal.text,
                    description = goal.description,
                    completed = goal.completed,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags?.let { Json.encodeToString(it) },
                    relatedLinks = goal.relatedLinks,
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
                    financialCost = goal.financialCost?.toDouble(),
                    markdown = goal.markdown
                )
            }
        }
    }

    override suspend fun deleteGoalById(id: String) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.deleteGoal(id)
        }
    }

    override suspend fun getGoalById(id: String): Goal? {
        val queries = db.goalQueries
        return withContext(ioDispatcher) {
            queries.getGoalById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        val queries = db.goalQueries
        return queries.getGoalsByIds(ids)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal> {
        val queries = db.goalQueries
        return withContext(ioDispatcher) {
            queries.getGoalsByIds(ids).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<Goal> {
        val queries = db.goalQueries
        return withContext(ioDispatcher) {
            queries.getAllGoals().executeAsList().map { it.toDomain() }
        }
    }

    override fun getAllGoalsFlow(): Flow<List<Goal>> {
        val queries = db.goalQueries
        return queries.getAllGoals()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun searchGoalsByText(query: String): Flow<List<Goal>> {
        val queries = db.goalQueries
        return queries.searchGoalsByText(query)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun getAllGoalsCountFlow(): Flow<Int> {
        val queries = db.goalQueries
        return queries.getAllGoalsCount()
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }
    }

    override suspend fun updateMarkdown(
        goalId: String,
        markdown: String,
    ) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.updateMarkdown(goalId, markdown)
        }
    }

    override suspend fun deleteAll() {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.deleteAll()
        }
    }
}

