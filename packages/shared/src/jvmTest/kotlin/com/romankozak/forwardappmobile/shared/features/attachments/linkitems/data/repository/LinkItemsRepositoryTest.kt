package com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.domain.repository.LinkItemsRepository
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
class LinkItemsRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: LinkItemsRepository

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = LinkItemsRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun link(idSuffix: Int, createdAt: Long = idSuffix.toLong()) = LinkItemEntity(
        id = "link_$idSuffix",
        linkData = RelatedLink(
            type = null,
            target = "https://example.com/$idSuffix",
            displayName = "Link $idSuffix",
        ),
        createdAt = createdAt,
    )

    @Test
    fun `observeLinkItems emits items ordered by createdAt`() = runTest {
        val first = link(1, createdAt = 10)
        val second = link(2, createdAt = 20)
        repository.upsertLinkItem(first)
        repository.upsertLinkItem(second)

        val observed = repository.observeLinkItems().first()

        assertEquals(listOf(second, first), observed)
    }

    @Test
    fun `searchLinkItems returns project path`() = runTest {
        val projectRoot = "project_root"
        val childProject = "project_child"
        database.projectsQueries.insertProject(
            id = projectRoot,
            name = "Root",
            description = null,
            parentId = null,
            createdAt = 1,
            updatedAt = null,
            tags = emptyList(),
            relatedLinks = emptyList(),
            isExpanded = true,
            goalOrder = 0,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = null,
            projectStatusText = null,
            projectLogLevel = null,
            totalTimeSpentMinutes = 0,
            valueImportance = 1.0,
            valueImpact = 1.0,
            effort = 1.0,
            cost = 1.0,
            risk = 1.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0,
            scoringStatus = null,
            showCheckboxes = false,
            projectType = null,
            reservedGroup = null,
        )
        database.projectsQueries.insertProject(
            id = childProject,
            name = "Child",
            description = null,
            parentId = projectRoot,
            createdAt = 2,
            updatedAt = null,
            tags = emptyList(),
            relatedLinks = emptyList(),
            isExpanded = true,
            goalOrder = 0,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = null,
            projectStatusText = null,
            projectLogLevel = null,
            totalTimeSpentMinutes = 0,
            valueImportance = 1.0,
            valueImpact = 1.0,
            effort = 1.0,
            cost = 1.0,
            risk = 1.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0,
            scoringStatus = null,
            showCheckboxes = false,
            projectType = null,
            reservedGroup = null,
        )

        val linkItem = link(1, createdAt = 50)
        repository.upsertLinkItem(linkItem)
        database.listItemsQueries.insertListItem(
            id = "list_item_1",
            projectId = childProject,
            itemOrder = 0,
            entityId = linkItem.id,
            itemType = "LINK_ITEM",
        )

        val results = repository.searchLinkItems("Link")
        val result = results.single()

        assertEquals(listOf("Root", "Child"), result.pathSegments)
        assertEquals(linkItem.id, result.link.id)
        assertEquals(childProject, result.projectId)
    }

    @Test
    fun `deleteLinkItem removes entity`() = runTest {
        val linkItem = link(3)
        repository.upsertLinkItem(linkItem)

        repository.deleteLinkItemById(linkItem.id)

        assertTrue(repository.observeLinkItems().first().isEmpty())
    }
}
