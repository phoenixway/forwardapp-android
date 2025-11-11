package com.romankozak.forwardappmobile.shared.features.projects.data.repository

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
import com.romankozak.forwardappmobile.shared.database.stringAdapter
import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.projects.data.models.Project
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ProjectType
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ProjectRepositoryImpl

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
            projectLogLevel = "NORMAL",
            totalTimeSpentMinutes = 0L,
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
            isProjectManagementEnabled = project.isProjectManagementEnabled,
            projectStatus = project.projectStatus,
            projectStatusText = project.projectStatusText,
            projectLogLevel = project.projectLogLevel,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes,
            valueImportance = project.valueImportance.toDouble(),
            valueImpact = project.valueImpact.toDouble(),
            effort = project.effort.toDouble(),
            cost = project.cost.toDouble(),
            risk = project.risk.toDouble(),
            weightEffort = project.weightEffort.toDouble(),
            weightCost = project.weightCost.toDouble(),
            weightRisk = project.weightRisk.toDouble(),
            rawScore = project.rawScore.toDouble(),
            displayScore = project.displayScore,
            scoringStatus = project.scoringStatus,
            showCheckboxes = project.showCheckboxes,
            projectType = project.projectType,
            reservedGroup = project.reservedGroup
        )

        val retrievedProject = repository.getProjectById(project.id).first()
        assertNotNull(retrievedProject)
        assertEquals(project, retrievedProject)
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
            projectLogLevel = "NORMAL",
            totalTimeSpentMinutes = 0L,
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
            projectLogLevel = "NORMAL",
            totalTimeSpentMinutes = 0L,
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
            isProjectManagementEnabled = project1.isProjectManagementEnabled,
            projectStatus = project1.projectStatus,
            projectStatusText = project1.projectStatusText,
            projectLogLevel = project1.projectLogLevel,
            totalTimeSpentMinutes = project1.totalTimeSpentMinutes,
            valueImportance = project1.valueImportance.toDouble(),
            valueImpact = project1.valueImpact.toDouble(),
            effort = project1.effort.toDouble(),
            cost = project1.cost.toDouble(),
            risk = project1.risk.toDouble(),
            weightEffort = project1.weightEffort.toDouble(),
            weightCost = project1.weightCost.toDouble(),
            weightRisk = project1.weightRisk.toDouble(),
            rawScore = project1.rawScore.toDouble(),
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
            isProjectManagementEnabled = project2.isProjectManagementEnabled,
            projectStatus = project2.projectStatus,
            projectStatusText = project2.projectStatusText,
            projectLogLevel = project2.projectLogLevel,
            totalTimeSpentMinutes = project2.totalTimeSpentMinutes,
            valueImportance = project2.valueImportance.toDouble(),
            valueImpact = project2.valueImpact.toDouble(),
            effort = project2.effort.toDouble(),
            cost = project2.cost.toDouble(),
            risk = project2.risk.toDouble(),
            weightEffort = project2.weightEffort.toDouble(),
            weightCost = project2.weightCost.toDouble(),
            weightRisk = project2.weightRisk.toDouble(),
            rawScore = project2.rawScore.toDouble(),
            displayScore = project2.displayScore,
            scoringStatus = project2.scoringStatus,
            showCheckboxes = project2.showCheckboxes,
            projectType = project2.projectType,
            reservedGroup = project2.reservedGroup
        )

        val projects = repository.getAllProjects().first()
        assertEquals(2, projects.size)
        assertEquals(project1, projects[0])
        assertEquals(project2, projects[1])
    }
}
