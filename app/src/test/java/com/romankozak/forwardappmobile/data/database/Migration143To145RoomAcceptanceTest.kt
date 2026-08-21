package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Migration143To145RoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migrate144To145_preservesRowsAndRemovesTaskV1() {
        val dbName = "migration_144_145_room_acceptance"
        createFixtureDatabase(dbName, 144, dirtyCanonicalLinks = false)

        val room = openRoom(dbName, MIGRATION_144_145)
        try {
            val db = room.openHelper.writableDatabase
            assert145State(db)
            assertCanonicalTemplate(db, expectedVersion = 3L, expectSyncedAtNull = false)
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migrate143To145_runsRealChainAndSanitizesCanonicalTemplate() {
        val dbName = "migration_143_145_room_acceptance"
        createFixtureDatabase(dbName, 143, dirtyCanonicalLinks = true)

        val room = openRoom(dbName, MIGRATION_143_144, MIGRATION_144_145)
        try {
            val db = room.openHelper.writableDatabase
            assert145State(db)
            assertCanonicalTemplate(db, expectedVersion = 4L, expectSyncedAtNull = true)
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun openRoom(dbName: String, vararg migrations: Migration): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*migrations)
            .allowMainThreadQueries()
            .build()

    private fun createFixtureDatabase(
        dbName: String,
        version: Int,
        dirtyCanonicalLinks: Boolean,
    ) {
        context.deleteDatabase(dbName)
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchemaFromJson(db, version)
                            seedFixture(db, dirtyCanonicalLinks)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected fixture upgrade $oldVersion->$newVersion")
                    },
                ).build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            helper.writableDatabase
        } finally {
            helper.close()
        }
    }

    private fun createSchemaFromJson(db: SupportSQLiteDatabase, version: Int) {
        val databaseJson =
            schemaFile(version).reader().use {
                JsonParser.parseReader(it).asJsonObject.getAsJsonObject("database")
            }
        val entities = databaseJson.getAsJsonArray("entities").map { it.asJsonObject }

        entities.filterNot { it.has("ftsVersion") }.forEach {
            createEntity(db, it.get("tableName").asString, it.get("createSql").asString)
        }
        entities.filter { it.has("ftsVersion") }.forEach {
            createEntity(db, it.get("tableName").asString, it.get("createSql").asString)
        }

        entities.forEach { entity ->
            val tableName = entity.get("tableName").asString
            entity.get("indices")?.asJsonArray?.forEach { index ->
                createEntity(db, tableName, index.asJsonObject.get("createSql").asString)
            }
            entity.get("contentSyncTriggers")?.asJsonArray?.forEach { trigger ->
                db.execSQL(trigger.asString)
            }
        }

        databaseJson.get("views")?.asJsonArray?.forEach {
            db.execSQL(it.asJsonObject.get("createSql").asString)
        }
        databaseJson.get("setupQueries")?.asJsonArray?.forEach {
            db.execSQL(it.asString)
        }
    }

    private fun createEntity(db: SupportSQLiteDatabase, tableName: String, sql: String) {
        db.execSQL(sql.replace("\${TABLE_NAME}", tableName))
    }

    private fun seedFixture(db: SupportSQLiteDatabase, dirtyCanonicalLinks: Boolean) {
        insertMinimalRow(
            db,
            "day_plans",
            mapOf(
                "id" to "plan-1",
                "date" to 1_787_000_000_000L,
                "status" to "PLANNED",
            ),
        )

        db.execSQL(
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

        val templateJson =
            if (dirtyCanonicalLinks) {
                """{"title":"Canonical","description":null,"goalId":null,"linkedProjectIds":["","project-1","project-1"],"linkedAttachmentIds":[""],"priority":"MEDIUM","estimatedDurationMinutes":30,"points":7,"projectId":null,"taskType":null,"executionStrictness":"NORMAL"}"""
            } else {
                """{"title":"Canonical","description":null,"goalId":null,"linkedProjectIds":["project-1"],"linkedAttachmentIds":[],"priority":"MEDIUM","estimatedDurationMinutes":30,"points":7,"projectId":null,"taskType":null,"executionStrictness":"NORMAL"}"""
            }

        ContentValues().apply {
            put("id", "series-1")
            put("kind", "TASK")
            put("ruleFrequency", "DAILY")
            put("ruleInterval", 1)
            putNull("ruleDaysOfWeekCsv")
            put("startDayKey", "2026-08-18")
            putNull("endDayKey")
            put("templateJson", templateJson)
            put("createdAt", 1_787_000_000_000L)
            put("updatedAt", 1_787_000_000_000L)
            put("syncedAt", 1_787_000_000_000L)
            put("isDeleted", false)
            put("version", 3L)
            assertTrue(
                db.insert(
                    "canonical_recurring_series",
                    SQLiteDatabase.CONFLICT_ABORT,
                    this,
                ) != -1L,
            )
        }

        insertMinimalRow(
            db,
            "day_tasks",
            mapOf(
                "id" to "occurrence-1",
                "dayPlanId" to "plan-1",
                "title" to "Canonical occurrence",
                "priority" to "MEDIUM",
                "status" to "NOT_STARTED",
                "completed" to false,
                "order" to 1,
                "createdAt" to 1_787_000_001_000L,
                "isDeleted" to false,
                "version" to 2L,
                "points" to 7,
                "recurrenceSeriesId" to "series-1",
                "recurrenceOccurrenceDayKey" to "2026-08-20",
                "recurrenceSourceSeriesVersion" to 3L,
            ),
        )
        insertMinimalRow(
            db,
            "day_tasks",
            mapOf(
                "id" to "ordinary-1",
                "dayPlanId" to "plan-1",
                "title" to "Ordinary task",
                "priority" to "MEDIUM",
                "status" to "NOT_STARTED",
                "completed" to false,
                "order" to 2,
                "createdAt" to 1_787_000_002_000L,
                "isDeleted" to false,
                "version" to 1L,
                "points" to 0,
            ),
        )
    }

    private fun insertMinimalRow(
        db: SupportSQLiteDatabase,
        table: String,
        overrides: Map<String, Any?>,
    ) {
        val values = ContentValues()
        val seen = mutableSetOf<String>()

        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIx = cursor.getColumnIndexOrThrow("name")
            val typeIx = cursor.getColumnIndexOrThrow("type")
            val notNullIx = cursor.getColumnIndexOrThrow("notnull")
            val defaultIx = cursor.getColumnIndexOrThrow("dflt_value")
            val pkIx = cursor.getColumnIndexOrThrow("pk")

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIx)
                seen += name

                if (overrides.containsKey(name)) {
                    putValue(values, name, overrides[name])
                    continue
                }

                val required =
                    cursor.getInt(notNullIx) != 0 &&
                        cursor.isNull(defaultIx) &&
                        cursor.getInt(pkIx) == 0
                if (!required) continue

                val type = cursor.getString(typeIx).uppercase()
                when {
                    "INT" in type -> values.put(name, 0L)
                    "REAL" in type || "FLOA" in type || "DOUB" in type -> values.put(name, 0.0)
                    "BLOB" in type -> values.put(name, ByteArray(0))
                    else -> values.put(name, "")
                }
            }
        }

        assertTrue("Unknown fixture columns in $table: ${overrides.keys - seen}", (overrides.keys - seen).isEmpty())
        val columns = values.keySet().toList()
        val quotedColumns =
            columns.joinToString(",") { column ->
                "`${column.replace("`", "``")}`"
            }
        val placeholders = List(columns.size) { "?" }.joinToString(",")
        val bindArgs =
            columns.map { column ->
                when (val value = values.get(column)) {
                    is Boolean -> if (value) 1L else 0L
                    else -> value
                }
            }.toTypedArray()

        db.execSQL(
            "INSERT OR ABORT INTO `${table.replace("`", "``")}` ($quotedColumns) VALUES ($placeholders)",
            bindArgs,
        )
    }

    private fun putValue(values: ContentValues, key: String, value: Any?) {
        when (value) {
            null -> values.putNull(key)
            is String -> values.put(key, value)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Boolean -> values.put(key, value)
            is Float -> values.put(key, value)
            is Double -> values.put(key, value)
            is ByteArray -> values.put(key, value)
            else -> error("Unsupported fixture value for $key: ${value::class.java.name}")
        }
    }

    private fun assert145State(db: SupportSQLiteDatabase) {
        assertEquals(145L, scalarLong(db, "PRAGMA user_version"))
        assertEquals(2L, scalarLong(db, "SELECT COUNT(*) FROM day_tasks"))
        assertEquals(
            listOf("occurrence-1", "ordinary-1"),
            stringColumn(db, "SELECT id FROM day_tasks ORDER BY id"),
        )

        assertFalse(tableExists(db, "recurring_tasks"))
        assertFalse(tableExists(db, "recurring_tasks_fts"))

        val columns = stringColumn(db, "SELECT name FROM pragma_table_info('day_tasks')")
        assertEquals(38, columns.size)
        assertFalse(columns.contains("recurringTaskId"))
        assertFalse(columns.contains("nextOccurrenceTime"))

        db.query(
            """
            SELECT recurrenceSeriesId, recurrenceOccurrenceDayKey, recurrenceSourceSeriesVersion
            FROM day_tasks
            WHERE id = 'occurrence-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("series-1", cursor.getString(0))
            assertEquals("2026-08-20", cursor.getString(1))
            assertEquals(3L, cursor.getLong(2))
        }

        db.query(
            """
            SELECT recurrenceSeriesId, recurrenceOccurrenceDayKey, recurrenceSourceSeriesVersion
            FROM day_tasks
            WHERE id = 'ordinary-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }

        assertEquals(
            setOf(
                "index_day_tasks_dayPlanId",
                "index_day_tasks_goalId",
                "index_day_tasks_projectId",
                "index_day_tasks_activityRecordId",
                "index_day_tasks_scheduledTime",
                "index_day_tasks_recurrenceSeriesId_recurrenceOccurrenceDayKey",
            ),
            stringColumn(
                db,
                """
                SELECT name
                FROM sqlite_master
                WHERE type = 'index'
                  AND tbl_name = 'day_tasks'
                  AND sql IS NOT NULL
                """.trimIndent(),
            ).toSet(),
        )

        db.query("PRAGMA foreign_key_list('day_tasks')").use { assertEquals(4, it.count) }
        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
    }

    private fun assertCanonicalTemplate(
        db: SupportSQLiteDatabase,
        expectedVersion: Long,
        expectSyncedAtNull: Boolean,
    ) {
        db.query(
            """
            SELECT templateJson, version, syncedAt, endDayKey
            FROM canonical_recurring_series
            WHERE id = 'series-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val template = JsonParser.parseString(cursor.getString(0)).asJsonObject
            assertEquals(
                listOf("project-1"),
                template.getAsJsonArray("linkedProjectIds").map { it.asString },
            )
            assertTrue(template.getAsJsonArray("linkedAttachmentIds").size() == 0)
            assertEquals(expectedVersion, cursor.getLong(1))
            assertEquals(expectSyncedAtNull, cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }

    private fun schemaFile(version: Int): File {
        val relative =
            "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json"
        val userDir = File(System.getProperty("user.dir"))
        return listOf(
            File(relative),
            File("app/$relative"),
            File(userDir, relative),
            File(userDir, "app/$relative"),
        ).firstOrNull { it.isFile }
            ?: error("Room schema $version not found. user.dir=${userDir.absolutePath}")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$table'",
        ) != 0L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use {
            assertTrue("Query returned no rows: $sql", it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use {
            assertTrue("Query returned no rows: $sql", it.moveToFirst())
            it.getString(0)
        }

    private fun stringColumn(db: SupportSQLiteDatabase, sql: String): List<String> =
        db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
}
