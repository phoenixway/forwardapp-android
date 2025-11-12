package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Projects
import kotlinx.coroutines.test.runTest
import kotlin.test.*

expect fun createTestDriver(): SqlDriver
expect fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase
expect fun closeTestDriver(driver: SqlDriver)

class DatabaseInitializerTest {

    private lateinit var db: ForwardAppDatabase
    private lateinit var driver: SqlDriver
    private lateinit var initializer: DatabaseInitializer

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        db = createTestDatabase(driver)
        db.projectsQueries.deleteProjectsForReset()
        initializer = DatabaseInitializer(db)
    }

    @AfterTest
    fun tearDown() {
        closeTestDriver(driver)
    }

    @Test
    fun `initialize creates special projects when database is empty`() = runTest {
        try {
            initializer.initialize()
        } catch (e: Exception) {
            val logFile = java.io.File("/home/romankozak/.gemini/tmp/f0d5f14bc037e204b853dd06d685bc18bd58df1bfcb9976e722e3e26d7d98360/test_error.log")
            logFile.writeText("SQLiteException: ${e.message}\nStackTrace: ${e.stackTraceToString()}")
            throw e
        }

        val specialProject = db.projectsQueries.getProjectsByType(ProjectType.SYSTEM).executeAsOneOrNull()
        assertNotNull(specialProject)
        assertEquals("special", specialProject.name)

        val inbox = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.Inbox).executeAsOneOrNull()
        assertNotNull(inbox)
        assertEquals(specialProject.id, inbox.parentId)

        val strategicGroup = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.StrategicGroup).executeAsOneOrNull()
        assertNotNull(strategicGroup)
        assertEquals(specialProject.id, strategicGroup.parentId)

        val mainBeaconsGroup = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.MainBeaconsGroup).executeAsOneOrNull()
        assertNotNull(mainBeaconsGroup)
        assertEquals("main-beacon-realization", mainBeaconsGroup.name)
        assertEquals(specialProject.id, mainBeaconsGroup.parentId)

        val listProject = db.projectsQueries.getProjectById("main-beacon-list-id").executeAsOneOrNull()
        assertNotNull(listProject)
        assertEquals("list", listProject.name)
        assertEquals(mainBeaconsGroup.id, listProject.parentId)

        val missionProject = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.MainBeacons).executeAsOneOrNull()
        assertNotNull(missionProject)
        assertEquals("mission", missionProject.name)
        assertEquals(listProject.id, missionProject.parentId)
    }

    @Test
    fun `initialize does not create duplicates`() = runTest {
        initializer.initialize()
        val countAfterFirstInit = db.projectsQueries.getAllProjectsUnordered().executeAsList().size

        initializer.initialize()
        val countAfterSecondInit = db.projectsQueries.getAllProjectsUnordered().executeAsList().size

        assertEquals(countAfterFirstInit, countAfterSecondInit)
    }

    @Test
    fun `initialize recreates missing special projects`() = runTest {
        // Initial setup
        initializer.initialize()

        // Pre-condition check: Ensure Inbox exists
        val originalInbox = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.Inbox).executeAsOneOrNull()
        assertNotNull(originalInbox, "Inbox should exist after initial initialization.")

        // Action: Delete the Inbox project
        db.projectsQueries.deleteProject(originalInbox.id)

        // Pre-condition check: Ensure Inbox is deleted
        val deletedInbox = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.Inbox).executeAsOneOrNull()
        assertNull(deletedInbox, "Inbox should be deleted before re-initialization.")

        // Trigger: Re-run initialization
        initializer.initialize()

        // Verification: Check if Inbox is recreated
        val recreatedInbox = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.Inbox).executeAsOneOrNull()
        assertNotNull(recreatedInbox, "Inbox should be recreated after re-initialization.")

        val specialProject = db.projectsQueries.getProjectsByType(ProjectType.SYSTEM).executeAsOne()
        assertEquals(specialProject.id, recreatedInbox.parentId, "Recreated Inbox should be a child of the special project.")
    }

    @Test
    fun `initialize corrects misplaced special projects`() = runTest {
        // Initial setup
        initializer.initialize()

        // Pre-condition check: Ensure Inbox exists and get its original parent
        val specialProject = db.projectsQueries.getProjectsByType(ProjectType.SYSTEM).executeAsOne()
        val inbox = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.Inbox).executeAsOne()
        assertEquals(specialProject.id, inbox.parentId)

        // Action: Create a dummy project and move Inbox under it
        val dummyProject = Projects(
            id = "dummy-project-id",
            name = "Dummy Project",
            description = null,
            parentId = null,
            createdAt = 0L,
            updatedAt = null,
            tags = emptyList(),
            relatedLinks = emptyList(),
            isExpanded = false,
            goalOrder = 0L,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = null,
            projectStatusText = null,
            projectLogLevel = null,
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
            projectType = com.romankozak.forwardappmobile.shared.data.models.ProjectType.DEFAULT,
            reservedGroup = null
        )
        db.projectsQueries.insertProject(
            dummyProject.id,
            dummyProject.name,
            dummyProject.description,
            dummyProject.parentId,
            dummyProject.createdAt,
            dummyProject.updatedAt,
            dummyProject.tags,
            dummyProject.relatedLinks,
            dummyProject.isExpanded,
            dummyProject.goalOrder,
            dummyProject.isAttachmentsExpanded,
            dummyProject.defaultViewMode,
            dummyProject.isCompleted,
            dummyProject.isProjectManagementEnabled,
            dummyProject.projectStatus,
            dummyProject.projectStatusText,
            dummyProject.projectLogLevel,
            dummyProject.totalTimeSpentMinutes,
            dummyProject.valueImportance,
            dummyProject.valueImpact,
            dummyProject.effort,
            dummyProject.cost,
            dummyProject.risk,
            dummyProject.weightEffort,
            dummyProject.weightCost,
            dummyProject.weightRisk,
            dummyProject.rawScore,
            dummyProject.displayScore,
            dummyProject.scoringStatus,
            dummyProject.showCheckboxes,
            dummyProject.projectType,
            dummyProject.reservedGroup
        )

        db.projectsQueries.updateParent(
            parentId = dummyProject.id,
            id = inbox.id
        )

        // Pre-condition check: Ensure Inbox is misplaced
        val misplacedInbox = db.projectsQueries.getProjectById(inbox.id).executeAsOne()
        assertEquals(dummyProject.id, misplacedInbox.parentId, "Inbox should be a child of the dummy project before re-initialization.")

        // Trigger: Re-run initialization
        initializer.initialize()

        // Verification: Check if Inbox is moved back to its correct parent
        val correctedInbox = db.projectsQueries.getProjectById(inbox.id).executeAsOne()
        assertEquals(specialProject.id, correctedInbox.parentId, "Inbox should be moved back to be a child of the special project.")
    }
}
