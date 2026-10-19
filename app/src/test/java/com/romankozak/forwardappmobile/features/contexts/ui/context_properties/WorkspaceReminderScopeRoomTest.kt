package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceReminderScopeRoomTest {
    @Test fun `clearing Workspace reminders preserves other typed entities with the same raw id`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val dao = db.reminderDao()
            fun row(id: String, type: String) = Reminder(id, "same", type, 1000, "SCHEDULED", 1, version = 1)
            val workspace = row("workspace-alarm", "PROJECT")
            val other = row("goal-alarm", "GOAL")
            dao.insert(workspace); dao.insert(other)
            val repository = ReminderRepository(dao, mockk(relaxed = true), mockk(relaxed = true), Dispatchers.Unconfined)

            repository.clearRemindersForEntityType("same", "PROJECT")

            assertThat(dao.getReminderById("workspace-alarm")!!.isDeleted).isTrue()
            assertThat(dao.getReminderById("goal-alarm")).isEqualTo(other)
        } finally { db.close() }
    }
}
