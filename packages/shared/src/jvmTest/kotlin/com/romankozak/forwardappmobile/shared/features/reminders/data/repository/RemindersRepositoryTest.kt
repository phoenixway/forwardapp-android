package com.romankozak.forwardappmobile.shared.features.reminders.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
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
class RemindersRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: RemindersRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = RemindersRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun reminder(
        id: String,
        entityId: String = "entity-$id",
        reminderTime: Long = 100L,
        status: String = "SCHEDULED",
        snoozeUntil: Long? = null,
    ) = Reminder(
        id = id,
        entityId = entityId,
        entityType = "PROJECT",
        reminderTime = reminderTime,
        status = status,
        creationTime = 50L,
        snoozeUntil = snoozeUntil,
    )

    @Test
    fun `observeReminders orders by reminderTime desc`() = runTest {
        repository.upsertReminder(reminder("r1", reminderTime = 10))
        repository.upsertReminder(reminder("r2", reminderTime = 20))

        val reminders = repository.observeReminders().first()

        assertEquals(listOf("r2", "r1"), reminders.map { it.id })
    }

    @Test
    fun `observeRemindersForEntity filters by entity`() = runTest {
        repository.upsertReminder(reminder("r1", entityId = "A"))
        repository.upsertReminder(reminder("r2", entityId = "B"))

        val reminders = repository.observeRemindersForEntity("A").first()

        assertEquals(listOf("r1"), reminders.map { it.id })
    }

    @Test
    fun `deleteReminder removes entry`() = runTest {
        repository.upsertReminder(reminder("r1"))

        repository.deleteReminder("r1")

        val reminders = repository.observeReminders().first()
        assertEquals(emptyList<String>(), reminders.map { it.id })
    }

    @Test
    fun `getReminderById returns null when absent`() = runTest {
        val result = repository.getReminderById("missing")

        assertNull(result)
    }
}
