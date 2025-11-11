package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.database.*
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DatabaseInitializerTest {

    private lateinit var db: ForwardAppDatabase
    private lateinit var driver: SqlDriver
    private lateinit var initializer: DatabaseInitializer

    @BeforeTest
    fun setup() {
        // Використовуємо in-memory базу для тестів
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Створюємо схему (SQLDelight)
        ForwardAppDatabase.Schema.create(driver)

        // Створюємо адаптери, як у реальному Database.kt
        db = ForwardAppDatabase(
            driver = driver,
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
            ),
            projectsAdapter = Projects.Adapter(
                idAdapter = stringAdapter,
                nameAdapter = stringAdapter,
                createdAtAdapter = longAdapter,
                goalOrderAdapter = longAdapter,
                tagsAdapter = stringListAdapter,
                relatedLinksAdapter = relatedLinksListAdapter,
                projectTypeAdapter = projectTypeAdapter,
                reservedGroupAdapter = reservedGroupAdapter,
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

        initializer = DatabaseInitializer(db)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `initialize creates special projects when database is empty`() = runBlocking {
        initializer.initialize()

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
    fun `initialize does not create duplicates`() = runBlocking {
        initializer.initialize()
        val countAfterFirstInit = db.projectsQueries.getAllProjectsUnordered().executeAsList().size

        initializer.initialize()
        val countAfterSecondInit = db.projectsQueries.getAllProjectsUnordered().executeAsList().size

        assertEquals(countAfterFirstInit, countAfterSecondInit)
    }
}

