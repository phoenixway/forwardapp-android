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
class Migration169To170MainBeaconWorkspaceRefsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `169 to 170 routes only exact reserved Main Beacon owners to Workspace refs`() {
        val dbName = "migration_169_170_main_beacon_workspace_refs"
        createFixture(dbName) { db ->
            insertBeacon(db)
            insertContext(db, RESERVED_ID)
            insertContext(db, ORDINARY_ID)
            insertContext(db, HISTORICAL_SYS_PREFIX_ID)
            insertCanonicalWorkspace(db, RESERVED_ID)
            insertSeedState(db, RESERVED_ID, seededAt = 41L)
            insertContextRef(db, RESERVED_ID, order = 3L)
            insertContextRef(db, ORDINARY_ID, order = 4L)
            insertContextRef(db, HISTORICAL_SYS_PREFIX_ID, order = 5L)
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_169_170)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(170L, scalarLong(db, "PRAGMA user_version"))
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM main_beacon_workspace_cross_ref
                    WHERE beacon_id = '$BEACON_ID'
                      AND workspace_id = '$RESERVED_ID'
                      AND ref_order = 3
                    """.trimIndent(),
                ),
            )
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM main_beacon_context_cross_ref
                    WHERE context_id = '$RESERVED_ID'
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM main_beacon_context_cross_ref
                    WHERE context_id = '$ORDINARY_ID' AND ref_order = 4
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM main_beacon_context_cross_ref
                    WHERE context_id = '$HISTORICAL_SYS_PREFIX_ID' AND ref_order = 5
                    """.trimIndent(),
                ),
            )
            assertEquals(
                41L,
                scalarLong(
                    db,
                    """
                    SELECT legacyIngressClosedAt
                    FROM system_workspace_tag_seed_states
                    WHERE workspaceId = '$RESERVED_ID'
                    """.trimIndent(),
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
    fun `169 to 170 fails closed before moving any reserved ref without a valid canonical owner`() {
        listOf(
            "missing" to null,
            "deleted" to WorkspaceShape(isDeleted = true),
            "malformed" to WorkspaceShape(sourceContextId = RESERVED_ID),
        ).forEach { (case, shape) ->
            val dbName = "migration_169_170_main_beacon_invalid_$case"
            createFixture(dbName) { db ->
                insertBeacon(db)
                insertContext(db, RESERVED_ID)
                shape?.let { insertCanonicalWorkspace(db, RESERVED_ID, it) }
                insertContextRef(db, RESERVED_ID, order = 8L)
            }
            val helper = openFixture(dbName)
            try {
                val db = helper.writableDatabase
                val failure = runCatching { MIGRATION_169_170.migrate(db) }.exceptionOrNull()
                assertTrue("$case must fail closed", failure is IllegalStateException)
                assertEquals(
                    1L,
                    scalarLong(
                        db,
                        """
                        SELECT COUNT(*) FROM main_beacon_context_cross_ref
                        WHERE beacon_id = '$BEACON_ID' AND context_id = '$RESERVED_ID'
                        """.trimIndent(),
                    ),
                )
                assertEquals(
                    0L,
                    scalarLong(
                        db,
                        """
                        SELECT COUNT(*) FROM main_beacon_workspace_cross_ref
                        WHERE beacon_id = '$BEACON_ID' AND workspace_id = '$RESERVED_ID'
                        """.trimIndent(),
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
            // SupportSQLiteOpenHelper is lazy: force opening so schema 169 and
            // fixture rows are actually created before the helper is closed.
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
                    object : SupportSQLiteOpenHelper.Callback(169) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 169)
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

    private fun insertBeacon(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "main_beacons",
                0,
            ContentValues().apply {
                put("id", BEACON_ID)
                put("title", "Beacon")
                put("readiness_status", "BLOCKED")
                put("updatedAt", 10L)
                    put("createdAt", 10L)
                },
            ) != -1L,
        ) { "Failed fixture insert into main_beacons" }
    }

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
                put("provenance", "CANONICAL_ONLY")
                    put("sourceContextId", shape.sourceContextId)
                },
            ) != -1L,
        ) { "Failed fixture insert into workspaces: $id" }
    }

    private fun insertSeedState(
        db: SupportSQLiteDatabase,
        workspaceId: String,
        seededAt: Long,
    ) {
        check(
            db.insert(
                "system_workspace_tag_seed_states",
                0,
            ContentValues().apply {
                put("workspaceId", workspaceId)
                    put("seededAt", seededAt)
                },
            ) != -1L,
        ) { "Failed fixture insert into system_workspace_tag_seed_states: $workspaceId" }
    }

    private fun insertContextRef(
        db: SupportSQLiteDatabase,
        contextId: String,
        order: Long,
    ) {
        check(
            db.insert(
                "main_beacon_context_cross_ref",
                0,
            ContentValues().apply {
                put("beacon_id", BEACON_ID)
                put("context_id", contextId)
                    put("ref_order", order)
                },
            ) != -1L,
        ) { "Failed fixture insert into main_beacon_context_cross_ref: $contextId" }
    }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            JsonParser.parseReader(
                File("schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json").reader(),
            ).asJsonObject.getAsJsonObject("database")
        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(entity.get("createSql").asString.replace("${'$'}{TABLE_NAME}", table))
            entity.getAsJsonArray("indices")?.forEach { index ->
                db.execSQL(index.asJsonObject.get("createSql").asString.replace("${'$'}{TABLE_NAME}", table))
            }
        }
        database.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
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
        val sourceContextId: String? = null,
    )

    private companion object {
        const val BEACON_ID = "beacon"
        const val RESERVED_ID = "sys_personal-management"
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
    }
}
