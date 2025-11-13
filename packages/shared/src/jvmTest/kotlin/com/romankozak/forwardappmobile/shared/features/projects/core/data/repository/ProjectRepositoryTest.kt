package com.romankozak.forwardappmobile.shared.features.projects.core.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ProjectRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = ProjectRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `getAllProjects returns empty list initially`() = runTest {
        val projects = repository.getAllProjects().first()
        assertEquals(0, projects.size)
    }

    @Test
    fun `getProjectById returns null for non-existent project`() = runTest {
        val project = repository.getProjectById("non_existent_id").first()
        assertNull(project)
    }

    @Test
    fun `insert and retrieve project`() = runTest {
        val project = Project(
            id = "project_1",
            name = "Test Project",
            description = "Description",
            parentId = null,
            createdAt = 1L,
            updatedAt = null,
            tags = listOf("tag1", "tag2"),
            relatedLinks = emptyList(),
            isExpanded = true,
            goalOrder = 0L,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = 0L,
            totalTimeSpentMinutes = 0L,
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
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        repository.upsertProject(project)

        val retrievedProject = repository.getProjectById(project.id).first()
        assertNotNull(retrievedProject)
        assertEquals(project.id, retrievedProject.id)
        assertEquals(project.name, retrievedProject.name)
    }

    @Test
    fun `getAllProjects returns all inserted projects`() = runTest {
        val project1 = Project(
            id = "project_1",
            name = "Test Project 1",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            isExpanded = true,
            goalOrder = 0L,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = 0L,
            totalTimeSpentMinutes = 0L,
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
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        val project2 = Project(
            id = "project_2",
            name = "Test Project 2",
            description = null,
            parentId = null,
            createdAt = 2L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            isExpanded = true,
            goalOrder = 1L,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = 0L,
            totalTimeSpentMinutes = 0L,
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
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        repository.upsertProject(project1)
        repository.upsertProject(project2)

        val projects = repository.getAllProjects().first()
        assertEquals(2, projects.size)
        assertEquals(project1.id, projects[0].id)
        assertEquals(project2.id, projects[1].id)
    }

    @Test
    fun `deleteProject removes data`() = runTest {
        val project = Project(
            id = "to_delete",
            name = "Temporary",
            description = null,
            parentId = null,
            createdAt = 3L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            isExpanded = true,
            goalOrder = 0L,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = 0L,
            totalTimeSpentMinutes = 0L,
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
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        repository.upsertProject(project)
        repository.deleteProject(project.id)

        val projects = repository.getAllProjects().first()
        assertTrue(projects.isEmpty())
    }
}
