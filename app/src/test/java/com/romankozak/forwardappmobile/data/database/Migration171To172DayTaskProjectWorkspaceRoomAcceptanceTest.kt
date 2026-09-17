package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration171To172DayTaskProjectWorkspaceRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `171 to 172 routes only exact reserved DayTask projects to Workspace branch`() {
        val dbName = "migration_171_172_daytask_project_workspace"
        createFixture(dbName) { db ->
            insertContext(db, RESERVED_ID)
            insertContext(db, ORDINARY_ID)
            insertContext(db, HISTORICAL_SYS_PREFIX_ID)
            insertCanonicalWorkspace(db, RESERVED_ID)
            insertDayPlan(db)

            insertTask(db, "system-task", RESERVED_ID)
            insertTask(db, "ordinary-task", ORDINARY_ID)
            insertTask(db, "historical-task", HISTORICAL_SYS_PREFIX_ID)
            insertTask(db, "unowned-task", null)
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_171_172)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(172L, scalarLong(db, "PRAGMA user_version"))

            assertEquals(
                RESERVED_ID,
                scalarString(
                    db,
                    "SELECT project_workspace_id FROM day_tasks WHERE id = 'system-task'",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT projectId IS NULL FROM day_tasks WHERE id = 'system-task'",
                ),
            )

            assertEquals(
                ORDINARY_ID,
                scalarString(
                    db,
                    "SELECT projectId FROM day_tasks WHERE id = 'ordinary-task'",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT project_workspace_id IS NULL FROM day_tasks WHERE id = 'ordinary-task'",
                ),
            )

            assertEquals(
                HISTORICAL_SYS_PREFIX_ID,
                scalarString(
                    db,
                    "SELECT projectId FROM day_tasks WHERE id = 'historical-task'",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT project_workspace_id IS NULL FROM day_tasks WHERE id = 'historical-task'",
                ),
            )

            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT projectId IS NULL AND project_workspace_id IS NULL " +
                        "FROM day_tasks WHERE id = 'unowned-task'",
                ),
            )

            db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `171 to 172 detaches legacy reserved owner when canonical Workspace is invalid`() {
        listOf(
            "missing" to null,
            "deleted" to WorkspaceShape(isDeleted = true),
            "context-backed" to WorkspaceShape(provenance = "CONTEXT_BACKED"),
            "malformed-source" to WorkspaceShape(sourceContextId = RESERVED_ID),
        ).forEach { (case, shape) ->
            val dbName = "migration_171_172_daytask_invalid_$case"

            createFixture(dbName) { db ->
                insertContext(db, RESERVED_ID)
                shape?.let { insertCanonicalWorkspace(db, RESERVED_ID, it) }
                insertDayPlan(db)
                insertTask(db, "system-task", RESERVED_ID)
            }

            val room =
                Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_171_172)
                    .allowMainThreadQueries()
                    .build()

            try {
                val db = room.openHelper.writableDatabase

                assertEquals(
                    1L,
                    scalarLong(
                        db,
                        "SELECT projectId IS NULL FROM day_tasks WHERE id = 'system-task'",
                    ),
                )
                assertEquals(
                    1L,
                    scalarLong(
                        db,
                        "SELECT project_workspace_id IS NULL FROM day_tasks WHERE id = 'system-task'",
                    ),
                )

                db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
                assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
            } finally {
                room.close()
                context.deleteDatabase(dbName)
            }
        }
    }

    private fun createFixture(
        dbName: String,
        populate: (SupportSQLiteDatabase) -> Unit,
    ) {
        context.deleteDatabase(dbName)
        val helper = openFixture(dbName, populate)
        try {
            helper.writableDatabase
        } finally {
            helper.close()
        }
    }

    private fun openFixture(
        dbName: String,
        populate: ((SupportSQLiteDatabase) -> Unit)? = null,
    ) =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(171) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 171)
                            populate?.invoke(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        )

    private fun insertContext(
        db: SupportSQLiteDatabase,
        id: String,
    ) {
        check(
            db.insert(
                "contexts",
                0,
                ContentValues().apply {
                    put("id", id)
                    put("name", id)
                    put("createdAt", 10L)
                    put("is_deleted", 0)
                    put("version", 1L)
                    put("scoring_status", "NOT_ASSESSED")
                },
            ) != -1L,
        )
    }

    private fun insertCanonicalWorkspace(
        db: SupportSQLiteDatabase,
        id: String,
        shape: WorkspaceShape = WorkspaceShape(),
    ) {
        check(
            db.insert(
                "workspaces",
                0,
                ContentValues().apply {
                    put("id", id)
                    put("workspaceOrder", 0L)
                    put("createdAt", 10L)
                    put("updatedAt", 10L)
                    put("isDeleted", if (shape.isDeleted) 1 else 0)
                    put("version", 1L)
                    put("provenance", shape.provenance)
                    put("sourceContextId", shape.sourceContextId)
                },
            ) != -1L,
        )
    }

    private fun insertDayPlan(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "day_plans",
                0,
                ContentValues().apply {
                    put("id", DAY_PLAN_ID)
                    put("date", 1L)
                    put("status", "PLANNED")
                    put("totalPlannedMinutes", 0L)
                    put("totalCompletedMinutes", 0L)
                    put("completionPercentage", 0.0)
                    put("createdAt", 1L)
                    put("isDeleted", 0)
                    put("version", 1L)
                },
            ) != -1L,
        )
    }

    private fun insertTask(
        db: SupportSQLiteDatabase,
        id: String,
        projectId: String?,
    ) {
        // FrameworkSQLiteDatabase.insert(ContentValues) does not quote column
        // names. `order` is an SQLite keyword, so this historical fixture must
        // use explicit quoted SQL.
        db.execSQL(
            """
            INSERT INTO `day_tasks`(
                `id`,
                `dayPlanId`,
                `title`,
                `projectId`,
                `order`,
                `priority`,
                `status`,
                `completed`,
                `createdAt`,
                `isDeleted`,
                `version`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                id,
                DAY_PLAN_ID,
                id,
                projectId,
                0L,
                "MEDIUM",
                "PENDING",
                0,
                1L,
                0,
                1L,
            ),
        )
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
            ?: error("Room schema $version not found from ${userDir.absolutePath}")
    }


    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            schemaFile(version).reader().use {
                JsonParser.parseReader(it).asJsonObject.getAsJsonObject("database")
            }

        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(
                entity.get("createSql").asString.replace("${'$'}{TABLE_NAME}", table),
            )
            entity.getAsJsonArray("indices")?.forEach { index ->
                db.execSQL(
                    index.asJsonObject
                        .get("createSql")
                        .asString
                        .replace("${'$'}{TABLE_NAME}", table),
                )
            }
        }

        database.getAsJsonArray("views")?.forEach {
            db.execSQL(it.asJsonObject.get("createSql").asString)
        }
    }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getString(0)
        }

    private data class WorkspaceShape(
        val isDeleted: Boolean = false,
        val provenance: String = "CANONICAL_ONLY",
        val sourceContextId: String? = null,
    )

    private companion object {
        const val DAY_PLAN_ID = "migration-day-plan"
        const val RESERVED_ID = "sys_today"
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
    }
}
