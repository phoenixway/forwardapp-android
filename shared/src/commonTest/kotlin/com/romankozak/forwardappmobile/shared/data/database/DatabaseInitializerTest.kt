package com.romankozak.forwardappmobile.shared.data.database

import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.*

expect fun createTestDriver(): Any
expect fun createTestDatabase(driver: Any): ForwardAppDatabase
expect fun closeTestDriver(driver: Any)

class DatabaseInitializerTest {

    private lateinit var db: ForwardAppDatabase
    private lateinit var driver: Any
    private lateinit var initializer: DatabaseInitializer

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        db = createTestDatabase(driver)
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
}
