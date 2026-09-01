package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Migration156To157KeyProblemsCutoverRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `156 to 157 migrates legacy and tracker problems into typed canonical rows`() {
        val dbName = "migration_156_157_key_problems_cutover"
        createFixtureDatabase(dbName) { db ->
            insertContext(db, id = "owner", roleCode = "crisis_case")
            insertContext(db, id = "related", roleCode = "default")

            insertContextConfiguration(
                db = db,
                contextId = "owner",
                basePresetCode = "crisis_case",
                experimentalCapabilities = "[]",
            )

            insertAttachment(db, id = "attachment")

            insertLegacyPayload(
                db = db,
                contextId = "owner",
                payload =
                    """
                    {
                      "issues": [
                        {
                          "id": "later",
                          "title": " Later ",
                          "description": " Details ",
                          "dateTime": null,
                          "status": "BLOCKED",
                          "relatedContextIds": ["related", "related"],
                          "relatedAttachmentIds": ["attachment", "attachment"],
                          "order": 5,
                          "createdAt": 10,
                          "updatedAt": 20
                        },
                        {
                          "id": "first",
                          "title": "First",
                          "status": "OPEN",
                          "order": 1
                        }
                      ]
                    }
                    """.trimIndent(),
                updatedAt = 100L,
            )
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_156_157, MIGRATION_157_158, MIGRATION_158_159)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(159L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "context_key_problems"))

            assertEquals(2L, scalarLong(db, "SELECT COUNT(*) FROM workspace_problems"))
            assertEquals(
                listOf("first", "later"),
                queryStrings(
                    db,
                    """
                    SELECT id
                    FROM workspace_problems
                    WHERE workspaceId = 'owner'
                    ORDER BY problemOrder
                    """.trimIndent(),
                ),
            )
            assertEquals(
                listOf(0L, 1L),
                queryLongs(
                    db,
                    """
                    SELECT problemOrder
                    FROM workspace_problems
                    WHERE workspaceId = 'owner'
                    ORDER BY problemOrder
                    """.trimIndent(),
                ),
            )

            db.query(
                """
                SELECT title, description, status, createdAt, updatedAt, syncedAt, isDeleted, version
                FROM workspace_problems
                WHERE id = 'later'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Later", cursor.getString(0))
                assertEquals("Details", cursor.getString(1))
                assertEquals("BLOCKED", cursor.getString(2))
                assertEquals(10L, cursor.getLong(3))
                assertEquals(20L, cursor.getLong(4))
                assertTrue(cursor.isNull(5))
                assertEquals(0, cursor.getInt(6))
                assertEquals(1L, cursor.getLong(7))
            }

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_problem_workspace_refs
                    WHERE problemId = 'later'
                      AND targetWorkspaceId = 'related'
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_problem_attachment_refs
                    WHERE problemId = 'later'
                      AND attachmentId = 'attachment'
                    """.trimIndent(),
                ),
            )

            db.query(
                """
                SELECT id, state, configurationVersion, configuration, isDeleted
                FROM workspace_capability_instances
                WHERE workspaceId = 'owner'
                  AND capabilityType = 'KEY_PROBLEMS'
                  AND instanceKey = 'default'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(0).isNotBlank())
                assertEquals("ACTIVE", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals("{}", cursor.getString(3))
                assertEquals(0, cursor.getInt(4))
                assertFalse(cursor.moveToNext())
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

    @Test
    fun `156 to 157 migrates old description shape without inventing dateTime`() {
        val dbName = "migration_156_157_key_problems_old_shape"
        createFixtureDatabase(dbName) { db ->
            insertContext(db, id = "owner", roleCode = "default")
            insertContext(db, id = "related", roleCode = "default")

            insertContextConfiguration(
                db = db,
                contextId = "owner",
                basePresetCode = "default",
                experimentalCapabilities = """["key_problems"]""",
            )

            insertLegacyPayload(
                db = db,
                contextId = "owner",
                payload =
                    """{"description":"  Broken pump\nNeeds inspection  ","focusContextIds":["related"]}""",
                updatedAt = 100L,
            )
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_156_157, MIGRATION_157_158, MIGRATION_158_159)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            db.query(
                """
                SELECT id, title, description, createdAt, updatedAt
                FROM workspace_problems
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-owner", cursor.getString(0))
                assertEquals("Broken pump", cursor.getString(1))
                assertEquals("Broken pump\nNeeds inspection", cursor.getString(2))
                assertEquals(100L, cursor.getLong(3))
                assertEquals(100L, cursor.getLong(4))
                assertFalse(cursor.moveToNext())
            }

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_problem_workspace_refs
                    WHERE problemId = 'legacy-owner'
                      AND targetWorkspaceId = 'related'
                    """.trimIndent(),
                ),
            )
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `156 to 157 rolls back when legacy dateTime has real value`() {
        val dbName = "migration_156_157_key_problems_datetime_fail"
        createFixtureDatabase(dbName) { db ->
            insertContext(db, id = "owner", roleCode = "crisis_case")
            insertLegacyPayload(
                db = db,
                contextId = "owner",
                payload = """{"issues":[{"id":"dated","title":"Dated","dateTime":1234}]}""",
                updatedAt = 100L,
            )
        }

        assertMigrationFailsClosed(
            dbName = dbName,
            expectedLegacyContextId = "owner",
        )
    }

    @Test
    fun `156 to 157 rolls back when live problem belongs to deleted owner`() {
        val dbName = "migration_156_157_key_problems_deleted_owner_fail"
        createFixtureDatabase(dbName) { db ->
            insertContext(
                db = db,
                id = "owner",
                roleCode = "crisis_case",
                isDeleted = true,
            )
            insertLegacyPayload(
                db = db,
                contextId = "owner",
                payload = """{"issues":[{"id":"live","title":"Still live"}]}""",
                updatedAt = 100L,
            )
        }

        assertMigrationFailsClosed(
            dbName = dbName,
            expectedLegacyContextId = "owner",
        )
    }

    private fun assertMigrationFailsClosed(
        dbName: String,
        expectedLegacyContextId: String,
    ) {
        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_156_157, MIGRATION_157_158)
                .allowMainThreadQueries()
                .build()

        val failure =
            try {
                runCatching { room.openHelper.writableDatabase }.exceptionOrNull()
            } finally {
                room.close()
            }

        assertNotNull(failure)

        inspectSchema156Database(dbName) { db ->
            assertEquals(156L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "context_key_problems"))
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM context_key_problems
                    WHERE context_id = '${sqlLiteral(expectedLegacyContextId)}'
                    """.trimIndent(),
                ),
            )
            assertFalse(tableExists(db, "workspace_problems"))
            assertFalse(tableExists(db, "workspace_problem_workspace_refs"))
            assertFalse(tableExists(db, "workspace_problem_attachment_refs"))
        }

        context.deleteDatabase(dbName)
    }

    private fun createFixtureDatabase(
        dbName: String,
        populate: (SupportSQLiteDatabase) -> Unit,
    ) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(156) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchemaFromJson(db, 156)
                            populate(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    private fun inspectSchema156Database(
        dbName: String,
        block: (SupportSQLiteDatabase) -> Unit,
    ) {
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(156) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            block(helper.writableDatabase)
        }
    }

    private fun insertContext(
        db: SupportSQLiteDatabase,
        id: String,
        roleCode: String?,
        isDeleted: Boolean = false,
    ) {
        val values =
            ContentValues().apply {
                put("id", id)
                put("name", id)
                put("createdAt", 10L)
                put("updatedAt", 20L)
                put("is_deleted", if (isDeleted) 1 else 0)
                put("version", 1L)
                put("role_code", roleCode)
                put("scoring_status", "UNASSESSED")
            }
        db.insert("contexts", 0, values)

        val workspaceValues =
            ContentValues().apply {
                put("id", id)
                putNull("nameOverride")
                putNull("descriptionOverride")
                putNull("parentWorkspaceId")
                put("roleCode", roleCode)
                put("workspaceOrder", 0L)
                put("createdAt", 10L)
                put("updatedAt", 20L)
                putNull("syncedAt")
                put("isDeleted", if (isDeleted) 1 else 0)
                put("version", 1L)
                put("provenance", "CONTEXT_BACKED")
                put("sourceContextId", id)
            }
        db.insert("workspaces", 0, workspaceValues)
    }

    private fun insertContextConfiguration(
        db: SupportSQLiteDatabase,
        contextId: String,
        basePresetCode: String,
        experimentalCapabilities: String,
    ) {
        val values =
            ContentValues().apply {
                put("id", "config-$contextId")
                put("contextId", contextId)
                put("base_preset_code", basePresetCode)
                put("experimental_capability_ids", experimentalCapabilities)
                put("apply_mode", "ADDITIVE")
                put("updatedAt", 20L)
                put("version", 1L)
                put("isDeleted", 0)
            }
        db.insert("context_structures", 0, values)
    }

    private fun insertLegacyPayload(
        db: SupportSQLiteDatabase,
        contextId: String,
        payload: String,
        updatedAt: Long,
    ) {
        val values =
            ContentValues().apply {
                put("context_id", contextId)
                put("payload_json", payload)
                put("updated_at", updatedAt)
            }
        db.insert("context_key_problems", 0, values)
    }

    private fun insertAttachment(
        db: SupportSQLiteDatabase,
        id: String,
    ) {
        val schema =
            loadSchemaJson(156)
                .getAsJsonObject("database")
                .getAsJsonArray("entities")
                .map { it.asJsonObject }
                .single { it.get("tableName").asString == "attachments" }

        val createSql = schema.get("createSql").asString
        val columns =
            Regex("""`([^`]+)`""")
                .findAll(createSql)
                .map { it.groupValues[1] }
                .toList()

        val values = ContentValues()
        columns.forEach { column ->
            when (column) {
                "id" -> values.put(column, id)
                "attachment_type" -> values.put(column, "FILE")
                "entity_id" -> values.put(column, "entity")
                "owner_context_id", "role_code" -> values.putNull(column)
                "is_system" -> values.put(column, 0)
                "createdAt", "updatedAt" -> values.put(column, 20L)
                "version" -> values.put(column, 1L)
                "isDeleted" -> values.put(column, 0)
            }
        }

        db.insert("attachments", 0, values)
    }

    private fun createSchemaFromJson(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database = loadSchemaJson(version).getAsJsonObject("database")
        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            db.execSQL(
                entity.get("createSql").asString.replace("\${TABLE_NAME}", entity.get("tableName").asString),
            )
            entity.getAsJsonArray("indices")?.forEach { indexElement ->
                val index = indexElement.asJsonObject
                db.execSQL(
                    index.get("createSql").asString.replace("\${TABLE_NAME}", entity.get("tableName").asString),
                )
            }
        }
        database.getAsJsonArray("views")?.forEach { element ->
            db.execSQL(element.asJsonObject.get("createSql").asString)
        }
    }

    private fun loadSchemaJson(version: Int) =
        com.google.gson.JsonParser.parseReader(
            File(
                "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json",
            ).reader(),
        ).asJsonObject

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            """
            SELECT COUNT(*)
            FROM sqlite_master
            WHERE type = 'table'
              AND name = '${sqlLiteral(table)}'
            """.trimIndent(),
        ) == 1L

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getString(0)
        }

    private fun queryStrings(
        db: SupportSQLiteDatabase,
        sql: String,
    ): List<String> =
        db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun queryLongs(
        db: SupportSQLiteDatabase,
        sql: String,
    ): List<Long> =
        db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(0))
            }
        }

    private fun sqlLiteral(value: String): String = value.replace("'", "''")
}
