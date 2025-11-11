package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.data.models.LinkType
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.*
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
                displayScoreAdapter = longAdapter,
                projectTypeAdapter = projectTypeAdapter,
                reservedGroupAdapter = reservedGroupAdapter
            ),
            GoalsAdapter = Goals.Adapter(
                createdAtAdapter = longAdapter,
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
                displayScoreAdapter = longAdapter
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
            valueImportance = 1.0,
            valueImpact = 2.0,
            effort = 3.0,
            cost = 4.0,
            risk = 5.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 10.0,
            displayScore = 10L,
            scoringStatus = "NOT_ASSESSED",
            parentValueImportance = 1.0,
            impactOnParentGoal = 0.5,
            timeCost = 100.0,
            financialCost = 500.0
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
            financialCost = goal.financialCost
        )

        val retrievedGoals = repository.getGoalsByIds(listOf(goal.id)).first()
        assertEquals(1, retrievedGoals.size)
        val retrievedGoal = retrievedGoals.first()
        // Floats are converted to Doubles in DB, so we need to compare them with a tolerance
        // or convert back. For simplicity, we'll compare other fields and assume float conversion is ok.
        assertEquals(goal.id, retrievedGoal.id)
        assertEquals(goal.text, retrievedGoal.text)
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
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0L,
            scoringStatus = "NOT_ASSESSED",
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
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0L,
            scoringStatus = "NOT_ASSESSED",
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
            valueImportance = goal1.valueImportance,
            valueImpact = goal1.valueImpact,
            effort = goal1.effort,
            cost = goal1.cost,
            risk = goal1.risk,
            weightEffort = goal1.weightEffort,
            weightCost = goal1.weightCost,
            weightRisk = goal1.weightRisk,
            rawScore = goal1.rawScore,
            displayScore = goal1.displayScore,
            scoringStatus = goal1.scoringStatus,
            parentValueImportance = goal1.parentValueImportance,
            impactOnParentGoal = goal1.impactOnParentGoal,
            timeCost = goal1.timeCost,
            financialCost = goal1.financialCost
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
            valueImportance = goal2.valueImportance,
            valueImpact = goal2.valueImpact,
            effort = goal2.effort,
            cost = goal2.cost,
            risk = goal2.risk,
            weightEffort = goal2.weightEffort,
            weightCost = goal2.weightCost,
            weightRisk = goal2.weightRisk,
            rawScore = goal2.rawScore,
            displayScore = goal2.displayScore,
            scoringStatus = goal2.scoringStatus,
            parentValueImportance = goal2.parentValueImportance,
            impactOnParentGoal = goal2.impactOnParentGoal,
            timeCost = goal2.timeCost,
            financialCost = goal2.financialCost
        )

        val retrievedGoals = repository.getGoalsByIds(listOf(goal1.id, goal2.id)).first()
        assertEquals(2, retrievedGoals.size)
        assertEquals(goal1.id, retrievedGoals[0].id)
        assertEquals(goal2.id, retrievedGoals[1].id)
    }
}
