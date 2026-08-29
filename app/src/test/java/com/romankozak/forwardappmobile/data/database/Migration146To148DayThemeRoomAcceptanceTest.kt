package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Migration146To148DayThemeRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migrate146To150_thenBootstrapLegacyDocumentIntoCanonicalAuthority() {
        val dbName = "migration_146_148_day_theme_room_acceptance"

        createFixtureDatabase(dbName)

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(
                    MIGRATION_146_147,
                    MIGRATION_147_148,
                    MIGRATION_148_149,
                    MIGRATION_149_150,
                    MIGRATION_150_151,
                    MIGRATION_151_152,
                )
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(152L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "day_theme_documents"))
            assertTrue(tableExists(db, "theme_definitions"))
            assertTrue(tableExists(db, "day_themes"))
            assertTrue(tableExists(db, "day_theme_assignment_documents"))
            assertTrue(tableExists(db, "day_theme_canonical_bootstrap_state"))
            assertTrue(tableExists(db, "managed_subjects"))
            assertTrue(tableExists(db, "orientations"))
            assertTrue(tableExists(db, "orientation_assessments"))
            assertTrue(tableExists(db, "orientation_assessment_revisions"))
            assertTrue(tableExists(db, "legacy_subject_mappings"))
            assertTrue(tableExists(db, "aspects"))
            assertTrue(tableExists(db, "workspace_bindings"))
            assertTrue(tableExists(db, "workspaces"))
            assertTrue(tableExists(db, "workspace_bootstrap_state"))
            assertTrue(tableExists(db, "orientation_bootstrap_state"))

            db.query(
                """
                SELECT contentJson, version
                FROM day_theme_documents
                WHERE dayPlanId = 'day-1'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(legacyJson, cursor.getString(0))
                assertEquals(7L, cursor.getLong(1))
            }

            val bootstrapper =
                CanonicalDayThemeBootstrapper(
                    database = room,
                    legacyDao = room.dayThemeDocumentDao(),
                    canonicalDao = room.canonicalDayThemeDao(),
                )

            runBlocking {
                val first = bootstrapper.ensureBootstrapped()

                assertTrue(first.performed)
                assertEquals(1, first.insertedThemeDefinitions)
                assertEquals(1, first.insertedDayThemes)
                assertEquals(1, first.insertedAssignmentDocuments)
                assertTrue(first.diagnostics.isEmpty())

                val canonicalDao = room.canonicalDayThemeDao()

                val definition = canonicalDao.getThemeDefinitionById("theme-a")
                assertNotNull(definition)
                assertEquals("Focus", definition!!.title)
                assertEquals("Migrated description", definition.description)

                val expectedDayThemeId = canonicalDayThemeId("day-1", "theme-a")
                val dayTheme = canonicalDao.getDayThemeById(expectedDayThemeId)
                assertNotNull(dayTheme)
                assertEquals("day-1", dayTheme!!.dayPlanId)
                assertEquals("theme-a", dayTheme.themeId)
                assertEquals(70, dayTheme.budgetPercent)
                assertEquals(2L, dayTheme.order)
                assertTrue(dayTheme.isActive)

                val assignmentDocument =
                    canonicalDao.getAssignmentDocumentByDayPlanId("day-1")
                assertNotNull(assignmentDocument)
                assertEquals(7L, assignmentDocument!!.version)
                assertTrue(assignmentDocument.syncedAt == null)

                val assignments =
                    JsonParser
                        .parseString(assignmentDocument.assignmentsJson)
                        .asJsonArray

                assertEquals(1, assignments.size())

                val assignment = assignments[0].asJsonObject
                assertEquals("task-1", assignment.get("entityId").asString)

                val assignedDayThemeIds = assignment.getAsJsonArray("dayThemeIds")
                assertEquals(1, assignedDayThemeIds.size())
                assertEquals(expectedDayThemeId, assignedDayThemeIds[0].asString)

                assertEquals(
                    CanonicalDayThemeBootstrapper.CURRENT_BOOTSTRAP_VERSION,
                    canonicalDao.getBootstrapVersion(),
                )

                val second = bootstrapper.ensureBootstrapped()

                assertFalse(second.performed)
                assertEquals(0, second.insertedThemeDefinitions)
                assertEquals(0, second.insertedDayThemes)
                assertEquals(0, second.insertedAssignmentDocuments)

                assertEquals(1, canonicalDao.getAllThemeDefinitionsSync().size)
                assertEquals(1, canonicalDao.getAllDayThemesSync().size)
                assertEquals(1, canonicalDao.getAllAssignmentDocumentsSync().size)

                // Legacy storage remains a quarantined migration/bootstrap boundary.
                assertEquals(1, room.dayThemeDocumentDao().getAllSync().size)
            }

            db.query("PRAGMA foreign_key_check").use {
                assertEquals(0, it.count)
            }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createFixtureDatabase(dbName: String) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(146) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchemaFromJson(db, 146)

                            insertMinimalRow(
                                db,
                                "day_plans",
                                mapOf(
                                    "id" to "day-1",
                                    "date" to 1_787_000_000_000L,
                                    "status" to "PLANNED",
                                    "createdAt" to 1_787_000_000_000L,
                                    "isDeleted" to false,
                                    "version" to 1L,
                                ),
                            )

                            insertMinimalRow(
                                db,
                                "day_theme_documents",
                                mapOf(
                                    "dayPlanId" to "day-1",
                                    "contentJson" to legacyJson,
                                    "createdAt" to 10L,
                                    "updatedAt" to 100L,
                                    "syncedAt" to null,
                                    "isDeleted" to false,
                                    "version" to 7L,
                                ),
                            )
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

    private fun createSchemaFromJson(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val databaseJson =
            schemaFile(version)
                .reader()
                .use {
                    JsonParser
                        .parseReader(it)
                        .asJsonObject
                        .getAsJsonObject("database")
                }

        val entities =
            databaseJson
                .getAsJsonArray("entities")
                .map { it.asJsonObject }

        entities
            .filterNot { it.has("ftsVersion") }
            .forEach {
                createEntity(
                    db,
                    it.get("tableName").asString,
                    it.get("createSql").asString,
                )
            }

        entities
            .filter { it.has("ftsVersion") }
            .forEach {
                createEntity(
                    db,
                    it.get("tableName").asString,
                    it.get("createSql").asString,
                )
            }

        entities.forEach { entity ->
            val tableName = entity.get("tableName").asString

            entity.get("indices")?.asJsonArray?.forEach { index ->
                createEntity(
                    db,
                    tableName,
                    index.asJsonObject.get("createSql").asString,
                )
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

    private fun createEntity(
        db: SupportSQLiteDatabase,
        tableName: String,
        sql: String,
    ) {
        db.execSQL(sql.replace("\${TABLE_NAME}", tableName))
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
                    "REAL" in type || "FLOA" in type || "DOUB" in type ->
                        values.put(name, 0.0)
                    "BLOB" in type -> values.put(name, ByteArray(0))
                    else -> values.put(name, "")
                }
            }
        }

        assertTrue(
            "Unknown fixture columns in $table: ${overrides.keys - seen}",
            (overrides.keys - seen).isEmpty(),
        )

        val columns = values.keySet().toList()
        val quotedColumns =
            columns.joinToString(",") { column ->
                "`${column.replace("`", "``")}`"
            }
        val placeholders = List(columns.size) { "?" }.joinToString(",")
        val bindArgs =
            columns
                .map { column ->
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

    private fun putValue(
        values: ContentValues,
        key: String,
        value: Any?,
    ) {
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
            ?: error(
                "Room schema $version not found. user.dir=${userDir.absolutePath}",
            )
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$table'",
        ) != 0L

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use {
            assertTrue("Query returned no rows: $sql", it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use {
            assertTrue("Query returned no rows: $sql", it.moveToFirst())
            it.getString(0)
        }

    private companion object {
        val legacyJson =
            """
            {
              "themes": [
                {
                  "id": "theme-a",
                  "dayPlanId": "day-1",
                  "title": "Focus",
                  "colorArgb": 4280644591,
                  "iconKey": "target",
                  "comment": "Migrated description",
                  "budgetPercent": 70,
                  "order": 2,
                  "isActive": true,
                  "createdAt": 10,
                  "updatedAt": 20
                }
              ],
              "assignments": [
                {
                  "dayPlanId": "day-1",
                  "entityId": "task-1",
                  "themeIds": ["theme-a"]
                }
              ]
            }
            """.trimIndent()
    }
}
