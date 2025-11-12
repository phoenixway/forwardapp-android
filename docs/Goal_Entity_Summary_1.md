# Goal Entity Summary

This file contains all the code related to the `Goal` entity.

---

## `Goals.sq`

```kotlin
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink;
import kotlin.collections.List;

CREATE TABLE Goals (
    id TEXT NOT NULL PRIMARY KEY,
    text TEXT NOT NULL,
    description TEXT,
    completed INTEGER AS Boolean NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER,
    tags TEXT AS List<String>,
    relatedLinks TEXT AS List<RelatedLink>,
    valueImportance REAL NOT NULL DEFAULT 0.0,
    valueImpact REAL NOT NULL DEFAULT 0.0,
    effort REAL NOT NULL DEFAULT 0.0,
    cost REAL NOT NULL DEFAULT 0.0,
    risk REAL NOT NULL DEFAULT 0.0,
    weightEffort REAL NOT NULL DEFAULT 1.0,
    weightCost REAL NOT NULL DEFAULT 1.0,
    weightRisk REAL NOT NULL DEFAULT 1.0,
    rawScore REAL NOT NULL DEFAULT 0.0,
    displayScore INTEGER NOT NULL DEFAULT 0,
    scoring_status TEXT NOT NULL DEFAULT 'NOT_ASSESSED',
    parentValueImportance REAL,
    impactOnParentGoal REAL,
    timeCost REAL,
    financialCost REAL
);

getAllGoals:
SELECT * FROM Goals;

getGoalById:
SELECT * FROM Goals WHERE id = ?;

getGoalsByIds:
SELECT * FROM Goals WHERE id IN ?;
```

---

## `Goal.kt` (Domain Model)

**Path:** `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/models/Goal.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.models

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues

@Serializable
data class Goal(
    val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val parentValueImportance: Float? = null,
    val impactOnParentGoal: Float? = null,
    val timeCost: Float? = null,
    val financialCost: Float? = null,
)
```

---

## `Goal.kt` (Old/Duplicate)

**Path:** `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/data/database/models/Goal.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.data.database.models

data class Goal(
    val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: RelatedLinkList? = null,
    val valueImportance: Float = 0.0f,
    val valueImpact: Float = 0.0f,
    val effort: Float = 0.0f,
    val cost: Float = 0.0f,
    val risk: Float = 0.0f,
    val weightEffort: Float = 1.0f,
    val weightCost: Float = 1.0f,
    val weightRisk: Float = 1.0f,
    val rawScore: Float = 0.0f,
    val displayScore: Int = 0,
    val scoringStatus: String,
    val parentValueImportance: Float? = null,
    val impactOnParentGoal: Float? = null,
    val timeCost: Float? = null,
    val financialCost: Float? = null,
    val markdown: String? = null
)
```

---

## `GoalMapper.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.mappers

import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal

fun Goals.toDomain(): Goal = Goal(
    id = id,
    text = text,
    description = description,
    completed = completed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
    relatedLinks = relatedLinks,
    valueImportance = valueImportance.toFloat(),
    valueImpact = valueImpact.toFloat(),
    effort = effort.toFloat(),
    cost = cost.toFloat(),
    risk = risk.toFloat(),
    weightEffort = weightEffort.toFloat(),
    weightCost = weightCost.toFloat(),
    weightRisk = weightRisk.toFloat(),
    rawScore = rawScore.toFloat(),
    displayScore = displayScore,
    scoringStatus = scoring_status,
    parentValueImportance = parentValueImportance?.toFloat(),
    impactOnParentGoal = impactOnParentGoal?.toFloat(),
    timeCost = timeCost?.toFloat(),
    financialCost = financialCost?.toFloat()
)
```

---

## `GoalRepository.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>
}
```

---

## `GoalRepositoryImpl.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.goals.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : GoalRepository {

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        return db.goalsQueries.getGoalsByIds(ids)
            .asFlow()
            .mapToList(dispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }
}
```

---

## `GoalRepositoryTest.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.longAdapter
import com.romankozak.forwardappmobile.shared.database.doubleAdapter
import com.romankozak.forwardappmobile.shared.database.intAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
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
            ProjectsAdapter = ForwardAppDatabase.Projects.Adapter(
                createdAtAdapter = longAdapter,
                tagsAdapter = stringListAdapter,
                relatedLinksAdapter = relatedLinksListAdapter,
                orderAdapter = longAdapter,
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
                projectTypeAdapter = com.romankozak.forwardappmobile.shared.database.projectTypeAdapter,
                reservedGroupAdapter = com.romankozak.forwardappmobile.shared.database.reservedGroupAdapter
            ),
            GoalsAdapter = ForwardAppDatabase.Goals.Adapter(
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
                scoringStatusAdapter = com.romankozak.forwardappmobile.shared.database.scoringStatusValuesAdapter,
                parentValueImportanceAdapter = doubleAdapter,
                impactOnParentGoalAdapter = doubleAdapter,
                timeCostAdapter = doubleAdapter,
                financialCostAdapter = doubleAdapter
            ),
            ListItemsAdapter = ForwardAppDatabase.ListItems.Adapter(
                orderAdapter = longAdapter
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
            relatedLinks = listOf(RelatedLink("link1", "url1")),
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
```
