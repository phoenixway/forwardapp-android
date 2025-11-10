package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.longAdapter
import com.romankozak.forwardappmobile.shared.database.doubleAdapter
import com.romankozak.forwardappmobile.shared.database.intAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem
import com.romankozak.forwardappmobile.shared.data.models.ListItemTypeValues
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
class ListItemRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ListItemRepositoryImpl

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
        repository = ListItemRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `getListItems returns empty list initially`() = runTest {
        val listItems = repository.getListItems("any_project_id").first()
        assertTrue(listItems.isEmpty())
    }

    @Test
    fun `insert and retrieve list item by project id`() = runTest {
        val projectId = "project_1"
        val listItem = ListItem(
            id = "item_1",
            projectId = projectId,
            itemType = ListItemTypeValues.TASK,
            entityId = "task_1",
            order = 0L
        )

        database.listItemsQueries.insertListItem(
            id = listItem.id,
            projectId = listItem.projectId,
            itemType = listItem.itemType,
            entityId = listItem.entityId,
            order = listItem.order
        )

        val retrievedListItems = repository.getListItems(projectId).first()
        assertEquals(1, retrievedListItems.size)
        assertEquals(listItem, retrievedListItems.first())
    }

    @Test
    fun `getListItems returns multiple items for a project`() = runTest {
        val projectId = "project_1"
        val listItem1 = ListItem(
            id = "item_1",
            projectId = projectId,
            itemType = ListItemTypeValues.TASK,
            entityId = "task_1",
            order = 0L
        )
        val listItem2 = ListItem(
            id = "item_2",
            projectId = projectId,
            itemType = ListItemTypeValues.GOAL,
            entityId = "goal_1",
            order = 1L
        )
        val listItem3 = ListItem(
            id = "item_3",
            projectId = "project_2", // Different project
            itemType = ListItemTypeValues.TASK,
            entityId = "task_2",
            order = 0L
        )

        database.listItemsQueries.insertListItem(
            id = listItem1.id,
            projectId = listItem1.projectId,
            itemType = listItem1.itemType,
            entityId = listItem1.entityId,
            order = listItem1.order
        )
        database.listItemsQueries.insertListItem(
            id = listItem2.id,
            projectId = listItem2.projectId,
            itemType = listItem2.itemType,
            entityId = listItem2.entityId,
            order = listItem2.order
        )
        database.listItemsQueries.insertListItem(
            id = listItem3.id,
            projectId = listItem3.projectId,
            itemType = listItem3.itemType,
            entityId = listItem3.entityId,
            order = listItem3.order
        )

        val retrievedListItems = repository.getListItems(projectId).first()
        assertEquals(2, retrievedListItems.size)
        assertEquals(listItem1, retrievedListItems[0])
        assertEquals(listItem2, retrievedListItems[1])
    }
}
