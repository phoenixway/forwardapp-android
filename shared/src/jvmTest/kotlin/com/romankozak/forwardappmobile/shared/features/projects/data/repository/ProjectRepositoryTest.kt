package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.features.projects.domain.model.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.*
import com.romankozak.forwardappmobile.shared.features.projects.domain.model.Project
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
        database.projectsQueries.insertProject(
            id = project.id,
            name = project.name,
            description = project.description,
            parentId = project.parentId,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            tags = project.tags,
            relatedLinks = project.relatedLinks,
            isExpanded = project.isExpanded,
            goalOrder = project.goalOrder,
            isAttachmentsExpanded = project.isAttachmentsExpanded,
            defaultViewMode = project.defaultViewMode,
            isCompleted = project.isCompleted,
            isProjectManagementEnabled = project.isProjectManagementEnabled ?: false,
            projectStatus = project.projectStatus,
            projectStatusText = project.projectStatusText,
            projectLogLevel = project.projectLogLevel,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes,
            valueImportance = project.valueImportance,
            valueImpact = project.valueImpact,
            effort = project.effort,
            cost = project.cost,
            risk = project.risk,
            weightEffort = project.weightEffort,
            weightCost = project.weightCost,
            weightRisk = project.weightRisk,
            rawScore = project.rawScore,
            displayScore = project.displayScore,
            scoringStatus = project.scoringStatus,
            showCheckboxes = project.showCheckboxes,
            projectType = project.projectType,
            reservedGroup = project.reservedGroup
        )

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
        database.projectsQueries.insertProject(
            id = project1.id,
            name = project1.name,
            description = project1.description,
            parentId = project1.parentId,
            createdAt = project1.createdAt,
            updatedAt = project1.updatedAt,
            tags = project1.tags,
            relatedLinks = project1.relatedLinks,
            isExpanded = project1.isExpanded,
            goalOrder = project1.goalOrder,
            isAttachmentsExpanded = project1.isAttachmentsExpanded,
            defaultViewMode = project1.defaultViewMode,
            isCompleted = project1.isCompleted,
            isProjectManagementEnabled = project1.isProjectManagementEnabled ?: false,
            projectStatus = project1.projectStatus,
            projectStatusText = project1.projectStatusText,
            projectLogLevel = project1.projectLogLevel,
            totalTimeSpentMinutes = project1.totalTimeSpentMinutes,
            valueImportance = project1.valueImportance,
            valueImpact = project1.valueImpact,
            effort = project1.effort,
            cost = project1.cost,
            risk = project1.risk,
            weightEffort = project1.weightEffort,
            weightCost = project1.weightCost,
            weightRisk = project1.weightRisk,
            rawScore = project1.rawScore,
            displayScore = project1.displayScore,
            scoringStatus = project1.scoringStatus,
            showCheckboxes = project1.showCheckboxes,
            projectType = project1.projectType,
            reservedGroup = project1.reservedGroup
        )
        database.projectsQueries.insertProject(
            id = project2.id,
            name = project2.name,
            description = project2.description,
            parentId = project2.parentId,
            createdAt = project2.createdAt,
            updatedAt = project2.updatedAt,
            tags = project2.tags,
            relatedLinks = project2.relatedLinks,
            isExpanded = project2.isExpanded,
            goalOrder = project2.goalOrder,
            isAttachmentsExpanded = project2.isAttachmentsExpanded,
            defaultViewMode = project2.defaultViewMode,
            isCompleted = project2.isCompleted,
            isProjectManagementEnabled = project2.isProjectManagementEnabled ?: false,
            projectStatus = project2.projectStatus,
            projectStatusText = project2.projectStatusText,
            projectLogLevel = project2.projectLogLevel,
            totalTimeSpentMinutes = project2.totalTimeSpentMinutes,
            valueImportance = project2.valueImportance,
            valueImpact = project2.valueImpact,
            effort = project2.effort,
            cost = project2.cost,
            risk = project2.risk,
            weightEffort = project2.weightEffort,
            weightCost = project2.weightCost,
            weightRisk = project2.weightRisk,
            rawScore = project2.rawScore,
            displayScore = project2.displayScore,
            scoringStatus = project2.scoringStatus,
            showCheckboxes = project2.showCheckboxes,
            projectType = project2.projectType,
            reservedGroup = project2.reservedGroup
        )

        val projects = repository.getAllProjects().first()
        assertEquals(2, projects.size)
        assertEquals(project1.id, projects[0].id)
        assertEquals(project2.id, projects[1].id)
    }
}
