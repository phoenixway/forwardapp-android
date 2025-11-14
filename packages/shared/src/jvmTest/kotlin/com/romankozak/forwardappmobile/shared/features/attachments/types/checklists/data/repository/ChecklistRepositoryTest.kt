package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.Checklist
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.ChecklistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ChecklistRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = ChecklistRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun checklist(projectId: String, suffix: Int) = Checklist(
        id = "c$suffix",
        projectId = projectId,
        name = "Checklist $suffix",
    )

    private fun item(checklistId: String, suffix: Int) = ChecklistItem(
        id = "item$suffix",
        checklistId = checklistId,
        content = "Item $suffix",
        isChecked = suffix % 2 == 0,
        order = suffix.toLong(),
    )

    @Test
    fun `observeChecklists filters by project`() = runTest {
        val c1 = checklist("p1", 1)
        val c2 = checklist("p1", 2)
        val c3 = checklist("p2", 3)
        repository.upsertChecklist(c1)
        repository.upsertChecklist(c2)
        repository.upsertChecklist(c3)

        val result = repository.observeChecklists("p1").first()

        assertEquals(setOf(c1, c2), result.toSet())
        assertTrue(result.none { it.projectId == "p2" })
    }

    @Test
    fun `observeChecklistItems returns sorted items`() = runTest {
        val c1 = checklist("p1", 1)
        repository.upsertChecklist(c1)
        val second = item(c1.id, 2).copy(order = 10)
        val first = item(c1.id, 1).copy(order = 5)
        repository.upsertChecklistItem(second)
        repository.upsertChecklistItem(first)

        val items = repository.observeChecklistItems(c1.id).first()

        assertEquals(listOf(first, second), items)
    }

    @Test
    fun `getChecklistById returns entity`() = runTest {
        val c1 = checklist("p1", 1)
        repository.upsertChecklist(c1)

        val loaded = repository.getChecklistById(c1.id)

        assertEquals(c1, loaded)
    }

    @Test
    fun `deleteChecklistById removes related items`() = runTest {
        val c1 = checklist("p1", 1)
        repository.upsertChecklist(c1)
        repository.upsertChecklistItem(item(c1.id, 1))
        repository.upsertChecklistItem(item(c1.id, 2))

        repository.deleteChecklistById(c1.id)

        assertTrue(repository.observeChecklists("p1").first().isEmpty())
        assertTrue(repository.observeChecklistItems(c1.id).first().isEmpty())
    }

    @Test
    fun `getChecklistItemById returns null when missing`() = runTest {
        assertNull(repository.getChecklistItemById("missing"))
    }

    @Test
    fun `deleteAllChecklists keeps other projects`() = runTest {
        val c1 = checklist("p1", 1)
        val c2 = checklist("p2", 2)
        repository.upsertChecklist(c1)
        repository.upsertChecklist(c2)

        repository.deleteAllChecklists(projectId = "p1")

        assertTrue(repository.observeChecklists("p1").first().isEmpty())
        assertEquals(listOf(c2), repository.observeChecklists("p2").first())
    }

    @Test
    fun `deleteAllChecklistItems with project cleans only related items`() = runTest {
        val p1Checklist = checklist("p1", 1)
        val p2Checklist = checklist("p2", 2)
        repository.upsertChecklist(p1Checklist)
        repository.upsertChecklist(p2Checklist)
        repository.upsertChecklistItem(item(p1Checklist.id, 1))
        repository.upsertChecklistItem(item(p2Checklist.id, 2))

        repository.deleteAllChecklistItems(projectId = "p1")

        assertTrue(repository.observeChecklistItems(p1Checklist.id).first().isEmpty())
        assertEquals(1, repository.observeChecklistItems(p2Checklist.id).first().size)
    }
}
