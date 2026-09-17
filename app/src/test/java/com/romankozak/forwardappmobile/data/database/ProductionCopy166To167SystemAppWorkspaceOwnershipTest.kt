package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Temporary production-copy acceptance test.
 *
 * Reads FORWARDAPP_PRODUCTION_DB_FILE, copies it into Robolectric's private
 * database directory, and lets the real Room AppDatabase migrate 166 -> 167.
 * The supplied source file is never modified.
 */
@RunWith(RobolectricTestRunner::class)
class ProductionCopy166To167SystemAppWorkspaceOwnershipTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `production copy migrates 166 to 167 and completes System Workspace ownership rehearsal`() =
        runBlocking {
        val sourcePath =
            requireNotNull(System.getenv("FORWARDAPP_PRODUCTION_DB_FILE")) {
                "FORWARDAPP_PRODUCTION_DB_FILE is required"
            }
        val source = File(sourcePath)
        require(source.isFile) {
            "Production-copy database does not exist: ${source.absolutePath}"
        }

        val dbName = "production_copy_166_167_system_app_workspace"
        context.deleteDatabase(dbName)

        val target = context.getDatabasePath(dbName)
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)

        val room =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                dbName,
            )
                .addMigrations(MIGRATION_166_167, MIGRATION_167_168)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(168L, scalarLong(db, "PRAGMA user_version"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))

            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }

            assertTrue(columnExists(db, "system_apps", "workspace_id"))
            assertFalse(columnExists(db, "system_apps", "context_id"))

            val invalidSystemAppOwners =
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM system_apps AS app
                    LEFT JOIN workspaces AS workspace
                        ON workspace.id = app.workspace_id
                    WHERE workspace.id IS NULL
                       OR workspace.isDeleted != 0
                    """.trimIndent(),
                )
            assertEquals(0L, invalidSystemAppOwners)

            val definitions = SystemOperationalDefinitions.all
            assertEquals(20, definitions.size)
            assertEquals(20, definitions.map { it.id }.distinct().size)

            var pending = 0

            definitions.forEach { definition ->
                val id = definition.id

                db.query(
                    """
                    SELECT
                        c.name,
                        c.description,
                        c.parentId,
                        c.goal_order,
                        c.role_code,
                        c.is_deleted,
                        w.nameOverride,
                        w.descriptionOverride,
                        w.parentWorkspaceId,
                        w.workspaceOrder,
                        w.roleCode,
                        w.isDeleted,
                        w.provenance,
                        w.sourceContextId
                    FROM contexts AS c
                    LEFT JOIN workspaces AS w
                        ON w.id = c.id
                    WHERE c.id = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(id),
                ).use { cursor ->
                    assertTrue("Missing reserved System Context: $id", cursor.moveToFirst())

                    assertEquals("Deleted reserved System Context: $id", 0, cursor.getInt(5))

                    assertFalse(
                        "Missing same-id System Workspace: $id",
                        cursor.isNull(6),
                    )
                    assertEquals("Deleted System Workspace: $id", 0, cursor.getInt(11))

                    assertEquals(
                        "System Workspace name projection is stale: $id",
                        cursor.nullableString(0),
                        cursor.nullableString(6),
                    )
                    assertEquals(
                        "System Workspace description projection is stale: $id",
                        cursor.nullableString(1),
                        cursor.nullableString(7),
                    )
                    assertEquals(
                        "System Workspace parent projection is stale: $id",
                        cursor.nullableString(2),
                        cursor.nullableString(8),
                    )
                    assertEquals(
                        "System Workspace order projection is stale: $id",
                        cursor.getLong(3),
                        cursor.getLong(9),
                    )
                    assertEquals(
                        "System Workspace role projection is stale: $id",
                        cursor.nullableString(4),
                        cursor.nullableString(10),
                    )

                    assertEquals(
                        "Unexpected System Workspace provenance: $id",
                        "CONTEXT_BACKED",
                        cursor.getString(12),
                    )
                    assertEquals(
                        "Unexpected System Workspace sourceContextId: $id",
                        id,
                        cursor.getString(13),
                    )

                    assertFalse(cursor.moveToNext())
                    pending += 1
                }
            }

            assertEquals(20, pending)

            val systemIds = definitions.mapTo(hashSetOf()) { it.id }
            val presentationBeforePromotion =
                definitions.associate { definition ->
                    val workspace =
                        requireNotNull(room.workspaceDao().getById(definition.id))
                    definition.id to
                        listOf(
                            workspace.nameOverride,
                            workspace.descriptionOverride,
                            workspace.parentWorkspaceId,
                            workspace.roleCode,
                            workspace.workspaceOrder,
                            workspace.createdAt,
                            workspace.isDeleted,
                        )
                }

            val capabilitiesBeforePromotion =
                room.orientationDao()
                    .getAllWorkspaceCapabilities()
                    .filter { it.workspaceId in systemIds }
                    .sortedWith(
                        compareBy(
                            { it.workspaceId },
                            { it.capabilityType },
                            { it.instanceKey },
                            { it.id },
                        ),
                    )

            val materializer =
                SystemWorkspaceMaterializer(
                    database = room,
                    contextDao = room.contextDao(),
                    workspaceDao = room.workspaceDao(),
                )

            val promotion =
                materializer.materializeAll(
                    now = 2_000_000_000_000L,
                    seedMissingFactoryCapabilities = false,
                )

            assertTrue(promotion.changed)
            assertEquals(0, promotion.created)
            assertEquals(0, promotion.preservedCanonical)
            assertEquals(20, promotion.promotedLegacy)
            assertEquals(0, promotion.seededFactoryCapabilities)

            val capabilitiesImmediatelyAfterPromotion =
                room.orientationDao()
                    .getAllWorkspaceCapabilities()
                    .filter { it.workspaceId in systemIds }
                    .sortedWith(
                        compareBy(
                            { it.workspaceId },
                            { it.capabilityType },
                            { it.instanceKey },
                            { it.id },
                        ),
                    )

            assertEquals(
                "System capability rows changed during ownership convergence",
                capabilitiesBeforePromotion,
                capabilitiesImmediatelyAfterPromotion,
            )

            definitions.forEach { definition ->
                val id = definition.id
                val workspace =
                    requireNotNull(room.workspaceDao().getById(id))
                val legacyContext =
                    requireNotNull(room.contextDao().getContextById(id))

                assertFalse("System Context deleted during promotion: $id", legacyContext.isDeleted)
                assertFalse("System Workspace deleted during promotion: $id", workspace.isDeleted)
                assertEquals(
                    "Unexpected promoted provenance: $id",
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    workspace.provenance,
                )
                assertEquals(
                    "Promoted System Workspace retained sourceContextId: $id",
                    null,
                    workspace.sourceContextId,
                )

                assertEquals(
                    "Presentation changed during ownership promotion: $id",
                    presentationBeforePromotion.getValue(id),
                    listOf(
                        workspace.nameOverride,
                        workspace.descriptionOverride,
                        workspace.parentWorkspaceId,
                        workspace.roleCode,
                        workspace.workspaceOrder,
                        workspace.createdAt,
                        workspace.isDeleted,
                    ),
                )
            }

            val bootstrapper =
                CanonicalWorkspaceBootstrapper(
                    database = room,
                    workspaceDao = room.workspaceDao(),
                    orientationDao = room.orientationDao(),
                    contextDao = room.contextDao(),
                    contextStructureDao = room.contextStructureDao(),
                )

            val bootstrapReport =
                bootstrapper.ensureBootstrapped(now = 2_000_000_000_100L)

            val forbiddenSystemIssues =
                bootstrapReport.issues.filter {
                    it.contextId in systemIds &&
                        it.code in
                            setOf(
                                "WORKSPACE_ID_COLLISION",
                                "WORKSPACE_PARENT_COLLISION",
                                "UNKNOWN_PARENT",
                            )
                }

            assertTrue(
                "Promoted System Workspace bootstrap issues: $forbiddenSystemIssues",
                forbiddenSystemIssues.isEmpty(),
            )

            definitions.forEach { definition ->
                val id = definition.id
                val workspace =
                    requireNotNull(room.workspaceDao().getById(id))
                val legacyContext =
                    requireNotNull(room.contextDao().getContextById(id))

                assertFalse("System Context deleted by bootstrap: $id", legacyContext.isDeleted)
                assertEquals(
                    "Bootstrap reverted promoted provenance: $id",
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    workspace.provenance,
                )
                assertEquals(
                    "Bootstrap restored sourceContextId: $id",
                    null,
                    workspace.sourceContextId,
                )
                assertEquals(
                    "Bootstrap rewrote canonical System Workspace presentation: $id",
                    presentationBeforePromotion.getValue(id),
                    listOf(
                        workspace.nameOverride,
                        workspace.descriptionOverride,
                        workspace.parentWorkspaceId,
                        workspace.roleCode,
                        workspace.workspaceOrder,
                        workspace.createdAt,
                        workspace.isDeleted,
                    ),
                )
            }

            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }

            val systemAppCount = scalarLong(db, "SELECT COUNT(*) FROM system_apps")

            println(
                "PRODUCTION_COPY_FULL_SYSTEM_WORKSPACE_CUTOVER_PASS " +
                    "userVersion=168 " +
                    "systemApps=$systemAppCount " +
                    "canonicalSystemWorkspaces=20 " +
                    "liveSystemContexts=20 " +
                    "bootstrapIssues=${bootstrapReport.issues.size} " +
                    "integrity=ok fkViolations=0",
            )
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun android.database.Cursor.nullableString(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private fun columnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return@use true
            }
            false
        }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            assertTrue("Query returned no rows: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            assertTrue("Query returned no rows: $sql", cursor.moveToFirst())
            cursor.getString(0)
        }
}
