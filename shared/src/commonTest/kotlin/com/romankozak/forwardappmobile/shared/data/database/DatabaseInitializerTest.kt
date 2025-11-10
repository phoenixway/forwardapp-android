package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
package com.romankozak.forwardappmobile.shared.data.database

import com.romankozak.forwardappmobile.shared.data.database.adapter.ListToStringAdapter
import com.romankozak.forwardappmobile.shared.data.database.adapter.ProjectTypeAdapter
import com.romankozak.forwardappmobile.shared.data.database.adapter.RelatedLinkListAdapter
import com.romankozak.forwardappmobile.shared.data.database.adapter.ReservedGroupAdapter
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.database.Projects
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseInitializerTest {

    private lateinit var db: ForwardAppDatabase
    private lateinit var driver: app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
    private lateinit var initializer: DatabaseInitializer

    @BeforeTest
    fun setup() {
        driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        db = ForwardAppDatabase(
            driver = driver,
            ProjectsAdapter = Projects.Adapter(
                tagsAdapter = ListToStringAdapter(),
                relatedLinksAdapter = RelatedLinkListAdapter(),
                projectTypeAdapter = ProjectTypeAdapter(),
                reservedGroupAdapter = ReservedGroupAdapter()
            ),
            GoalsAdapter = Goals.Adapter(
                relatedLinksAdapter = RelatedLinkListAdapter()
            ),
            ListItemsAdapter = ListItems.Adapter()
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

        val listProject = db.projectsQueries.getById("main-beacon-list-id").executeAsOneOrNull()
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
        val countAfterFirstInit = db.projectsQueries.getAll().executeAsList().size

        initializer.initialize()
        val countAfterSecondInit = db.projectsQueries.getAll().executeAsList().size

        assertEquals(countAfterFirstInit, countAfterSecondInit)
    }
}
