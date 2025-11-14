package com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectArtifactRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ProjectArtifactRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = ProjectArtifactRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun artifact(
        id: String,
        projectId: String = "project-1",
        content: String = "Initial content",
        timestamp: Long = 1L,
    ) = ProjectArtifact(
        id = id,
        projectId = projectId,
        content = content,
        createdAt = timestamp,
        updatedAt = timestamp,
    )

    @Test
    fun `getProjectArtifactStream emits artifact for project`() = runTest {
        val projectArtifact = artifact("artifact-1")

        repository.createProjectArtifact(projectArtifact)

        val emitted = repository.getProjectArtifactStream(projectArtifact.projectId).first()
        assertEquals(projectArtifact, emitted)
    }

    @Test
    fun `updateProjectArtifact overwrites existing entry`() = runTest {
        val projectArtifact = artifact("artifact-1")
        repository.createProjectArtifact(projectArtifact)

        val updated = projectArtifact.copy(content = "Updated")
        repository.updateProjectArtifact(updated)

        val emitted = repository.getProjectArtifactStream(projectArtifact.projectId).first()
        assertEquals("Updated", emitted?.content)
    }

    @Test
    fun `deleteProjectArtifact removes record`() = runTest {
        val projectArtifact = artifact("artifact-1")
        repository.createProjectArtifact(projectArtifact)

        repository.deleteProjectArtifact(projectArtifact.id)

        val emitted = repository.getProjectArtifactStream(projectArtifact.projectId).first()
        assertNull(emitted)
    }
}
