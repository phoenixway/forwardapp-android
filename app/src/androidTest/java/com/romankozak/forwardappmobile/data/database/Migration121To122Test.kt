package com.romankozak.forwardappmobile.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.romankozak.forwardappmobile.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration121To122Test {
    private val dbName = "migration_121_122_test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate121To122_preservesDayTasksAndAddsExecutionStrictness() {
        helper
            .createDatabase(dbName, 121)
            .apply {
                execSQL(
                    """
                    INSERT INTO day_plans (
                        id, date, name, linkedProjectIds, linkedAttachmentIds, status,
                        reflection, energyLevel, mood, weatherConditions,
                        totalPlannedMinutes, totalCompletedMinutes, completionPercentage,
                        createdAt, updatedAt, syncedAt, isDeleted, version
                    ) VALUES (
                        'plan_121', 1710000000000, 'Legacy Plan', '[]', '[]', 'PLANNED',
                        NULL, NULL, NULL, NULL,
                        0, 0, 0.0,
                        1710000000000, 1710000000000, NULL, 0, 1
                    )
                    """.trimIndent(),
                )
                execSQL(
                    """
                    INSERT INTO day_tasks (
                        id, dayPlanId, title, description, goalId, projectId,
                        linkedProjectIds, linkedAttachmentIds, activityRecordId, recurringTaskId,
                        taskType, entityId, `order`, priority, status, completed,
                        scheduledTime, estimatedDurationMinutes, actualDurationMinutes, dueTime,
                        valueImportance, valueImpact, effort, cost, risk,
                        location, tags, notes, createdAt, updatedAt, syncedAt,
                        isDeleted, version, completedAt, nextOccurrenceTime, points
                    ) VALUES (
                        'task_121', 'plan_121', 'Legacy Task', 'Still here', NULL, NULL,
                        '[]', '[]', NULL, NULL,
                        'GOAL', NULL, 1, 'MEDIUM', 'NOT_STARTED', 0,
                        1710003600000, 45, NULL, 1710006300000,
                        0.0, 0.0, 0.0, 0.0, 0.0,
                        NULL, NULL, NULL, 1710000000000, 1710000000000, NULL,
                        0, 1, NULL, NULL, 7
                    )
                    """.trimIndent(),
                )
                close()
            }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 122, true, MIGRATION_121_122)

        migratedDb
            .query(
                """
                SELECT title, estimatedDurationMinutes, dueTime, points, executionStrictness
                FROM day_tasks
                WHERE id = 'task_121'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Legacy Task", cursor.getString(0))
                assertEquals(45L, cursor.getLong(1))
                assertEquals(1710006300000L, cursor.getLong(2))
                assertEquals(7, cursor.getInt(3))
                assertEquals("NORMAL", cursor.getString(4))
            }

        migratedDb.close()
    }
}
