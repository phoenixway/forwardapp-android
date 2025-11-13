package com.romankozak.forwardappmobile.shared.features.projects.listitems.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.*
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver

@OptIn(ExperimentalCoroutinesApi::class)
class ListItemRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ListItemRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
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
            itemType = "TASK",
            entityId = "task_1",
            itemOrder = 0L
        )

        database.listItemsQueries.insertListItem(
            id = listItem.id,
            projectId = listItem.projectId,
            itemType = listItem.itemType,
            entityId = listItem.entityId,
            itemOrder = listItem.itemOrder
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
            itemType = "TASK",
            entityId = "task_1",
            itemOrder = 0L
        )
        val listItem2 = ListItem(
            id = "item_2",
            projectId = projectId,
            itemType = "GOAL",
            entityId = "goal_1",
            itemOrder = 1L
        )
        val listItem3 = ListItem(
            id = "item_3",
            projectId = "project_2", // Different project
            itemType = "TASK",
            entityId = "task_2",
            itemOrder = 0L
        )

        database.listItemsQueries.insertListItem(
            id = listItem1.id,
            projectId = listItem1.projectId,
            itemType = listItem1.itemType,
            entityId = listItem1.entityId,
            itemOrder = listItem1.itemOrder
        )
        database.listItemsQueries.insertListItem(
            id = listItem2.id,
            projectId = listItem2.projectId,
            itemType = listItem2.itemType,
            entityId = listItem2.entityId,
            itemOrder = listItem2.itemOrder
        )
        database.listItemsQueries.insertListItem(
            id = listItem3.id,
            projectId = listItem3.projectId,
            itemType = listItem3.itemType,
            entityId = listItem3.entityId,
            itemOrder = listItem3.itemOrder
        )

        val retrievedListItems = repository.getListItems(projectId).first()
        assertEquals(2, retrievedListItems.size)
        assertEquals(listItem1, retrievedListItems[0])
        assertEquals(listItem2, retrievedListItems[1])
    }
}
