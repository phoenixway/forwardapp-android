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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration170To171TacticalMissionProjectWorkspaceRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `170 to 171 routes only exact reserved TacticalMission projects to Workspace branch`() {
        val dbName = "migration_170_171_tactical_project_workspace"
        createFixture(dbName) { db ->
            insertContext(db, RESERVED_ID)
            insertContext(db, ORDINARY_ID)
            insertContext(db, HISTORICAL_SYS_PREFIX_ID)
            insertCanonicalWorkspace(db, RESERVED_ID)

            insertMission(db, 1L, RESERVED_ID)
            insertMission(db, 2L, ORDINARY_ID)
            insertMission(db, 3L, HISTORICAL_SYS_PREFIX_ID)
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_170_171)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(171L, scalarLong(db, "PRAGMA user_version"))

            assertEquals(
                RESERVED_ID,
                scalarString(
                    db,
                    "SELECT project_workspace_id FROM tactical_missions WHERE id = 1",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT projectId IS NULL FROM tactical_missions WHERE id = 1",
                ),
            )

            assertEquals(
                ORDINARY_ID,
                scalarString(
                    db,
                    "SELECT projectId FROM tactical_missions WHERE id = 2",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT project_workspace_id IS NULL FROM tactical_missions WHERE id = 2",
                ),
            )

            assertEquals(
                HISTORICAL_SYS_PREFIX_ID,
                scalarString(
                    db,
                    "SELECT projectId FROM tactical_missions WHERE id = 3",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT project_workspace_id IS NULL FROM tactical_missions WHERE id = 3",
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
    fun `170 to 171 fails closed before moving reserved project without valid canonical Workspace`() {
        listOf(
            "missing" to null,
            "deleted" to WorkspaceShape(isDeleted = true),
            "context-backed" to WorkspaceShape(provenance = "CONTEXT_BACKED"),
            "malformed-source" to WorkspaceShape(sourceContextId = RESERVED_ID),
        ).forEach { (case, shape) ->
            val dbName = "migration_170_171_tactical_invalid_$case"

            createFixture(dbName) { db ->
                insertContext(db, RESERVED_ID)
                shape?.let { insertCanonicalWorkspace(db, RESERVED_ID, it) }
                insertMission(db, 1L, RESERVED_ID)
            }

            val helper = openFixture(dbName)
            try {
                val db = helper.writableDatabase
                val failure =
                    runCatching {
                        MIGRATION_170_171.migrate(db)
                    }.exceptionOrNull()

                assertTrue("$case must fail closed", failure is IllegalStateException)

                assertEquals(
                    RESERVED_ID,
                    scalarString(
                        db,
                        "SELECT projectId FROM tactical_missions WHERE id = 1",
                    ),
                )
                assertEquals(
                    1L,
                    scalarLong(
                        db,
                        "SELECT project_workspace_id IS NULL FROM tactical_missions WHERE id = 1",
                    ),
                )
            } finally {
                helper.close()
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
            // SupportSQLiteOpenHelper is lazy. Force schema creation and
            // fixture insertion before closing the historical database.
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
                    object : SupportSQLiteOpenHelper.Callback(170) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 170)
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
        ) { "Failed fixture insert into contexts: $id" }
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
        ) { "Failed fixture insert into workspaces: $id" }
    }

    private fun insertMission(
        db: SupportSQLiteDatabase,
        id: Long,
        projectId: String,
    ) {
        check(
            db.insert(
                "tactical_missions",
                0,
                ContentValues().apply {
                    put("id", id)
                    put("title", "Mission $id")
                    put("deadline", Long.MAX_VALUE)
                    put("status", "ACTIVE")
                    put("priority", "MEDIUM")
                    put("projectId", projectId)
                    put("mission_order", id)
                    put("week_key", "2026-W37")
                    put("order_in_week", id)
                    put("source_type", "MANUAL")
                    put("created_at", 10L)
                    put("is_deleted", 0)
                    put("version", 1L)
                },
            ) != -1L,
        ) { "Failed fixture insert into tactical_missions: $id" }
    }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            JsonParser.parseReader(
                File(
                    "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json",
                ).reader(),
            ).asJsonObject.getAsJsonObject("database")

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
        const val RESERVED_ID = "sys_today"
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
    }
}
