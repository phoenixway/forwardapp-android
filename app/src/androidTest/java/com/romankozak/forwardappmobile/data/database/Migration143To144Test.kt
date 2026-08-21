package com.romankozak.forwardappmobile.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration143To144Test {
    private val dbName = "migration_143_144_test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate143To144_acceptsStaleLegacyEndDateAndSanitizesCanonicalLinkIds() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(dbName)

        helper
            .createDatabase(dbName, 143)
            .apply {
                execSQL(
                    """
                    INSERT INTO recurring_tasks (
                        id, title, description, goalId,
                        linkedProjectIds, linkedAttachmentIds,
                        duration, priority, points,
                        startDate, endDate,
                        frequency, interval, daysOfWeek
                    ) VALUES (
                        'series-1', 'Legacy shadow', NULL, NULL,
                        '', '',
                        30, 'MEDIUM', 7,
                        1787000000000, 1787086400000,
                        'DAILY', 1, NULL
                    )
                    """.trimIndent(),
                )

                execSQL(
                    """
                    INSERT INTO canonical_recurring_series (
                        id, kind,
                        ruleFrequency, ruleInterval, ruleDaysOfWeekCsv,
                        startDayKey, endDayKey,
                        templateJson,
                        createdAt, updatedAt, syncedAt,
                        isDeleted, version
                    ) VALUES (
                        'series-1', 'TASK',
                        'DAILY', 1, NULL,
                        '2026-08-18', NULL,
                        '{"title":"Canonical","description":null,"goalId":null,"linkedProjectIds":["", "project-1", "project-1"],"linkedAttachmentIds":[""],"priority":"MEDIUM","estimatedDurationMinutes":30,"points":7,"projectId":null,"taskType":null,"executionStrictness":"NORMAL"}',
                        1787000000000, 1787000000000, 1787000000000,
                        0, 3
                    )
                    """.trimIndent(),
                )
                close()
            }

        val migratedDb =
            helper.runMigrationsAndValidate(
                dbName,
                144,
                true,
                MIGRATION_143_144,
            )

        migratedDb
            .query(
                """
                SELECT templateJson, version, syncedAt, endDayKey
                FROM canonical_recurring_series
                WHERE id = 'series-1'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                val template = JsonParser.parseString(cursor.getString(0)).asJsonObject
                val projects = template.getAsJsonArray("linkedProjectIds")
                val attachments = template.getAsJsonArray("linkedAttachmentIds")

                assertEquals(1, projects.size())
                assertEquals("project-1", projects[0].asString)
                assertEquals(0, attachments.size())
                assertEquals(4L, cursor.getLong(1))
                assertTrue(cursor.isNull(2))

                // Legacy endDate is intentionally not copied back over canonical state.
                assertTrue(cursor.isNull(3))
            }

        migratedDb.close()
    }
}
