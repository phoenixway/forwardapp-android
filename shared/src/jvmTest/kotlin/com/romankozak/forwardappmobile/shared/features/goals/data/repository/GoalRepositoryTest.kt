package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.longAdapter
import com.romankozak.forwardappmobile.shared.database.doubleAdapter
import com.romankozak.forwardappmobile.shared.database.intAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.database.projectTypeAdapter
import com.romankozak.forwardappmobile.shared.database.reservedGroupAdapter
import com.romankozak.forwardappmobile.shared.database.scoringStatusValuesAdapter
import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: GoalRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        database = ForwardAppDatabase(
            driver = driver,
            projectsAdapter = Projects.Adapter(
                createdAtAdapter = longAdapter,
                tagsAdapter = stringListAdapter,
                relatedLinksAdapter = relatedLinksListAdapter,
                goalOrderAdapter = longAdapter,
                valueImportanceAdapter = doubleAdapter,
                valueImpactAdapter = doubleAdapter,
                effortAdapter = doubleAdapter,
                costAdapter = doubleAdapter,
                riskAdapter = doubleAdapter,
                weightEffortAdapter = doubleAdapter,
                weightCostAdapter = doubleAdapter,
                weightRiskAdapter = doubleAdapter,
                rawScoreAdapter = doubleAdapter,
                displayScoreAdapter = intAdapter,
                projectTypeAdapter = projectTypeAdapter,
                reservedGroupAdapter = reservedGroupAdapter
            ),
            goalsAdapter = Goals.Adapter(
                createdAtAdapter = longAdapter,
                updatedAtAdapter = longAdapter,
                tagsAdapter = stringListAdapter,
                relatedLinksAdapter = relatedLinksListAdapter,
                valueImportanceAdapter = doubleAdapter,
                valueImpactAdapter = doubleAdapter,
                effortAdapter = doubleAdapter,
                costAdapter = doubleAdapter,
                riskAdapter = doubleAdapter,
                weightEffortAdapter = doubleAdapter,
                weightCostAdapter = doubleAdapter,
                weightRiskAdapter = doubleAdapter,
                rawScoreAdapter = doubleAdapter,
                displayScoreAdapter = intAdapter,
                scoringStatusAdapter = scoringStatusValuesAdapter,
                parentValueImportanceAdapter = doubleAdapter,
                impactOnParentGoalAdapter = doubleAdapter,
                timeCostAdapter = doubleAdapter,
                financialCostAdapter = doubleAdapter
            ),
            listItemsAdapter = ListItems.Adapter(
                idAdapter = stringAdapter,
                projectIdAdapter = stringAdapter,
                orderIndexAdapter = longAdapter
            )
        )
        repository = GoalRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `getGoalsByIds returns empty list initially`() = runTest {
        val goals = repository.getGoalsByIds(emptyList()).first()
        assertTrue(goals.isEmpty())
    }

    @Test
    fun `insert and retrieve goal by id`() = runTest {
        val goal = Goal(
            id = "goal_1",
            text = "Test Goal",
            description = "Description",
            completed = false,
            createdAt = 1L,
            updatedAt = null,
            tags = listOf("tag1", "tag2"),
            relatedLinks = listOf(RelatedLink(LinkType.URL, "url1")),
            valueImportance = 1.0f,
            valueImpact = 2.0f,
            effort = 3.0f,
            cost = 4.0f,
            risk = 5.0f,
            weightEffort = 1.0f,
            weightCost = 1.0f,
            weightRisk = 1.0f,
            rawScore = 10.0f,
            displayScore = 10,
            scoringStatus = ScoringStatusValues.NOT_ASSESSED,
            parentValueImportance = 1.0f,
            impactOnParentGoal = 0.5f,
            timeCost = 100.0f,
            financialCost = 500.0f
        )

        database.goalsQueries.insertGoal(
            id = goal.id,
            text = goal.text,
            description = goal.description,
            completed = goal.completed,
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt,
            tags = goal.tags,
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
            displayScore = goal.displayScore,
            scoringStatus = goal.scoringStatus,
            parentValueImportance = goal.parentValueImportance?.toDouble(),
            impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
            timeCost = goal.timeCost?.toDouble(),
            financialCost = goal.financialCost?.toDouble()
        )

        val retrievedGoals = repository.getGoalsByIds(listOf(goal.id)).first()
        assertEquals(1, retrievedGoals.size)
        assertEquals(goal, retrievedGoals.first())
    }

    @Test
    fun `getGoalsByIds returns multiple goals`() = runTest {
        val goal1 = Goal(
            id = "goal_1",
            text = "Test Goal 1",
            description = null,
            completed = false,
            createdAt = 1L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = ScoringStatusValues.NOT_ASSESSED,
            parentValueImportance = null,
            impactOnParentGoal = null,
            timeCost = null,
            financialCost = null
        )
        val goal2 = Goal(
            id = "goal_2",
            text = "Test Goal 2",
            description = null,
            completed = false,
            createdAt = 2L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = ScoringStatusValues.NOT_ASSESSED,
            parentValueImportance = null,
            impactOnParentGoal = null,
            timeCost = null,
            financialCost = null
        )

        database.goalsQueries.insertGoal(
            id = goal1.id,
            text = goal1.text,
            description = goal1.description,
            completed = goal1.completed,
            createdAt = goal1.createdAt,
            updatedAt = goal1.updatedAt,
            tags = goal1.tags,
            relatedLinks = goal1.relatedLinks,
            valueImportance = goal1.valueImportance.toDouble(),
            valueImpact = goal1.valueImpact.toDouble(),
            effort = goal1.effort.toDouble(),
            cost = goal1.cost.toDouble(),
            risk = goal1.risk.toDouble(),
            weightEffort = goal1.weightEffort.toDouble(),
            weightCost = goal1.weightCost.toDouble(),
            weightRisk = goal1.weightRisk.toDouble(),
            rawScore = goal1.rawScore.toDouble(),
            displayScore = goal1.displayScore,
            scoringStatus = goal1.scoringStatus,
            parentValueImportance = goal1.parentValueImportance?.toDouble(),
            impactOnParentGoal = goal1.impactOnParentGoal?.toDouble(),
            timeCost = goal1.timeCost?.toDouble(),
            financialCost = goal1.financialCost?.toDouble()
        )
        database.goalsQueries.insertGoal(
            id = goal2.id,
            text = goal2.text,
            description = goal2.description,
            completed = goal2.completed,
            createdAt = goal2.createdAt,
            updatedAt = goal2.updatedAt,
            tags = goal2.tags,
            relatedLinks = goal2.relatedLinks,
            valueImportance = goal2.valueImportance.toDouble(),
            valueImpact = goal2.valueImpact.toDouble(),
            effort = goal2.effort.toDouble(),
            cost = goal2.cost.toDouble(),
            risk = goal2.risk.toDouble(),
            weightEffort = goal2.weightEffort.toDouble(),
            weightCost = goal2.weightCost.toDouble(),
            weightRisk = goal2.weightRisk.toDouble(),
            rawScore = goal2.rawScore.toDouble(),
            displayScore = goal2.displayScore,
            scoringStatus = goal2.scoringStatus,
            parentValueImportance = goal2.parentValueImportance?.toDouble(),
            impactOnParentGoal = goal2.impactOnParentGoal?.toDouble(),
            timeCost = goal2.timeCost?.toDouble(),
            financialCost = goal2.financialCost?.toDouble()
        )

        val retrievedGoals = repository.getGoalsByIds(listOf(goal1.id, goal2.id)).first()
        assertEquals(2, retrievedGoals.size)
        assertEquals(goal1, retrievedGoals[0])
        assertEquals(goal2, retrievedGoals[1])
    }
}
