package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.data.database.HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK
import com.romankozak.forwardappmobile.data.database.MIGRATION_179_180
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalHierarchyMigrationEstablisherRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val harness = CanonicalHierarchyMigrationFixtureHarness(context)

    @Test
    fun `schema 150 skipped release reaches exact schema180 canonical state`() =
        runBlocking {
            val registeredName = "migration-hierarchy-150-registered.db"
            val directName = "migration-hierarchy-150-direct.db"
            try {
                harness.migrateTo179(registeredName, 150, ::seedSchema150Fixture)
                harness.migrateTo179(directName, 150, ::seedSchema150Fixture)

                migrate179To180Direct(directName)

                val registeredState = readCurrentState(registeredName)
                val directState = readCurrentStateRaw(directName)

                assertEquals(
                    registeredState.copy(markerActivatedAt = null),
                    directState.copy(markerActivatedAt = null),
                )
                assertEquals(3, registeredState.markerVersion)
                assertEquals("ESTABLISHED", registeredState.origin)
                assertTrue(registeredState.placements.isNotEmpty())
            } finally {
                context.deleteDatabase(registeredName)
                context.deleteDatabase(directName)
            }
        }

    @Test
    fun `rich schema 178 fixture reaches exact schema180 placement provenance and target state`() =
        runBlocking {
            val registeredName = "migration-hierarchy-rich-registered.db"
            val directName = "migration-hierarchy-rich-direct.db"
            try {
                harness.migrateTo179(registeredName, 178, ::seedRichSchema178Fixture)
                harness.migrateTo179(directName, 178, ::seedRichSchema178Fixture)

                migrate179To180Direct(directName)

                val registeredState = readCurrentState(registeredName)
                val directState = readCurrentStateRaw(directName)

                assertEquals(
                    registeredState.copy(markerActivatedAt = null),
                    directState.copy(markerActivatedAt = null),
                )
                assertTrue(registeredState.placements.any { it.placementKind == "PRIMARY" })
                assertTrue(registeredState.placements.any { it.placementKind == "LINK" })
                assertTrue(registeredState.groupScopes.isNotEmpty())
                assertTrue(registeredState.linkedAppearances.isNotEmpty())
                assertTrue(registeredState.targetMappings.all { it.state == "CUT_OVER" })
                assertEquals(
                    mapOf("beacon-child" to 5L, "beacon-parent" to 8L, "beacon-peer" to 2L),
                    registeredState.beacons.associate { it.id to it.order },
                )
            } finally {
                context.deleteDatabase(registeredName)
                context.deleteDatabase(directName)
            }
        }

    @Test
    fun `production migration evidence reader is deterministic on converged schema179`() {
        val dbName = "migration-hierarchy-reader-determinism.db"
        try {
            harness.migrateTo179(dbName, 178, ::seedRichSchema178Fixture)
            harness.openSchema179Raw(dbName).use { helper ->
                val db = helper.writableDatabase
                db.beginTransaction()
                try {
                    CanonicalHierarchyMigrationPrerequisiteAdapter().converge(
                        db = db,
                        origin = com.romankozak.forwardappmobile.data.database
                            .HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE,
                        now = MIGRATION_TEST_NOW,
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }

                val first = CanonicalHierarchyMigrationEvidenceReader().read(db)
                val second = CanonicalHierarchyMigrationEvidenceReader().read(db)
                assertEquals(first, second)

                val firstSnapshot = CanonicalV1HierarchySnapshotBuilder().build(first)
                val secondSnapshot = CanonicalV1HierarchySnapshotBuilder().build(second)
                assertEquals(firstSnapshot, secondSnapshot)
                assertTrue(firstSnapshot.occurrences.isNotEmpty())
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `fresh native schema179 migration produces exact runtime parity`() =
        runBlocking {
            val runtimeName = "migration-hierarchy-fresh-runtime.db"
            val migrationName = "migration-hierarchy-fresh-kernel.db"
            try {
                createFreshDatabase(runtimeName).let { db ->
                    try {
                        convergeRuntimeHierarchyPrerequisites(db)
                        runtimeHierarchyActivator(db).ensureEstablished(now = MIGRATION_TEST_NOW)
                    } finally {
                        db.close()
                    }
                }

                harness.migrateTo179(
                    dbName = migrationName,
                    startVersion = 178,
                    seed = {},
                    afterMigrations = { db ->
                        db.execSQL(
                            """
                            UPDATE hierarchy_establishment_origin
                            SET origin = 'FRESH_NATIVE'
                            WHERE hierarchyId = 'GENERAL'
                            """.trimIndent(),
                        )
                    },
                )

                val runtimeState = readCurrentState(runtimeName)
                val migrationState = readCurrentState(migrationName)

                assertEquals(
                    runtimeState.copy(markerActivatedAt = null),
                    migrationState.copy(markerActivatedAt = null),
                )
                assertEquals(3, migrationState.markerVersion)
                assertEquals("ESTABLISHED", migrationState.origin)
            } finally {
                context.deleteDatabase(runtimeName)
                context.deleteDatabase(migrationName)
            }
        }

    @Test
    fun `migration persistence accepts exact rerun and rejects divergent H1`() {
        val dbName = "migration-hierarchy-rerun.db"
        try {
            harness.migrateTo179(dbName, 178, ::seedRichSchema178Fixture)
            harness.openSchema179Raw(dbName).use { helper ->
                val db = helper.writableDatabase
                db.beginTransaction()
                try {
                    CanonicalHierarchyMigrationPrerequisiteAdapter().converge(
                        db,
                        com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
                            .LEGACY_UPGRADE_REQUIRES_CAPTURE,
                        MIGRATION_TEST_NOW,
                    )
                    val snapshot =
                        CanonicalV1HierarchySnapshotBuilder().build(
                            CanonicalHierarchyMigrationEvidenceReader().read(db),
                        )
                    val persistence = CanonicalHierarchyMigrationPersistenceAdapter()
                    persistence.persist(db, snapshot, MIGRATION_TEST_NOW)
                    persistence.persist(db, snapshot, MIGRATION_TEST_NOW)

                    db.execSQL(
                        "UPDATE hierarchy_placements SET placementKind = 'LINK' " +
                            "WHERE id = (SELECT id FROM hierarchy_placements LIMIT 1)",
                    )
                    val failure = runCatching { persistence.persist(db, snapshot, MIGRATION_TEST_NOW) }
                        .exceptionOrNull()
                    assertTrue(failure is CanonicalV1HierarchyMaterializationConflictException)
                } finally {
                    db.endTransaction()
                }
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `migration callback rollback preserves legacy evidence and control plane`() =
        runBlocking {
            listOf(
                CanonicalHierarchyMigrationCheckpoint.AFTER_PLACEMENTS,
                CanonicalHierarchyMigrationCheckpoint.AFTER_NEUTRALIZATION,
            ).forEach { failurePoint ->
                val dbName = "migration-hierarchy-rollback-${failurePoint.name}.db"
                try {
                    harness.migrateTo179(dbName, 178, ::seedRichSchema178Fixture)
                    val failure = harness.runFailingUpgradeFrom179(dbName, failurePoint, MIGRATION_TEST_NOW)
                    assertNotNull(failure)

                    harness.openSchema179Raw(dbName).use { helper ->
                        val db = helper.writableDatabase
                        assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM hierarchy_placements"))
                        assertEquals(
                            0L,
                            scalarLong(
                                db,
                                "SELECT COUNT(*) FROM hierarchy_authority_activation_state " +
                                    "WHERE hierarchyId = 'GENERAL'",
                            ),
                        )
                        assertEquals(
                            "LEGACY_UPGRADE_REQUIRES_CAPTURE",
                            scalarString(
                                db,
                                "SELECT origin FROM hierarchy_establishment_origin " +
                                    "WHERE hierarchyId = 'GENERAL'",
                            ),
                        )
                        assertEquals(
                            "beacon-parent",
                            scalarString(
                                db,
                                "SELECT parent_beacon_id FROM main_beacons " +
                                    "WHERE id = 'beacon-child'",
                            ),
                        )
                        assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM main_beacon_parent_links"))
                        assertTrue(
                            scalarLong(
                                db,
                                "SELECT COUNT(*) FROM workspaces WHERE parentWorkspaceId IS NOT NULL",
                            ) > 0L,
                        )
                    }
                } finally {
                    context.deleteDatabase(dbName)
                }
            }
        }

    @Test
    fun `marker v1 converges full v3 storage invariant without recapture`() =
        runBlocking {
            val dbName = "migration-hierarchy-marker-v1-convergence.db"
            try {
                harness.migrateTo179(
                    dbName,
                    178,
                    ::seedRichSchema178Fixture,
                ) { db ->
                    CanonicalHierarchyMigrationEstablisher().establish(db, MIGRATION_TEST_NOW)
                }
                lateinit var beforePlacements: List<HierarchyPlacement>
                lateinit var beforeScopes: List<HierarchyPlacementGroupScopeEntity>
                lateinit var beforeLinks: List<HierarchyPlacementLinkedAppearanceEntity>
                lateinit var beforeBeaconOrder: Map<String, Long>

                harness.openSchema179Raw(dbName).use { helper ->
                    val db = helper.writableDatabase
                    val persistence = CanonicalHierarchyMigrationPersistenceAdapter()
                    beforePlacements = persistence.readPlacements(db)
                    beforeScopes = persistence.readGroupScopes(db)
                    beforeLinks = persistence.readLinkedAppearances(db)
                    beforeBeaconOrder = readBeaconOrderRaw(db)
                    db.execSQL(
                        "UPDATE hierarchy_authority_activation_state SET version = 1 " +
                            "WHERE hierarchyId = 'GENERAL'",
                    )
                    db.execSQL(
                        "UPDATE workspaces SET parentWorkspaceId = 'workspace-a', " +
                            "workspaceOrder = 91 WHERE id = 'workspace-shared'",
                    )
                    db.execSQL(
                        "UPDATE main_beacons SET parent_beacon_id = 'beacon-parent' " +
                            "WHERE id = 'beacon-child'",
                    )
                    db.execSQL(
                        "INSERT INTO main_beacon_parent_links " +
                            "(parent_beacon_id, child_beacon_id, link_order, updatedAt, createdAt) " +
                            "VALUES ('beacon-peer', 'beacon-child', 6, 10, 10)",
                    )

                    db.beginTransaction()
                    try {
                        val report =
                            CanonicalHierarchyMigrationEstablisher().establish(
                                db,
                                MIGRATION_TEST_NOW + 1,
                            )
                        assertTrue(!report.performed)
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }

                    assertEquals(beforePlacements, persistence.readPlacements(db))
                    assertEquals(beforeScopes, persistence.readGroupScopes(db))
                    assertEquals(beforeLinks, persistence.readLinkedAppearances(db))
                    assertEquals(beforeBeaconOrder, readBeaconOrderRaw(db))
                    assertEquals(
                        3L,
                        scalarLong(
                            db,
                            "SELECT version FROM hierarchy_authority_activation_state " +
                                "WHERE hierarchyId = 'GENERAL'",
                        ),
                    )
                    assertEquals(
                        "ESTABLISHED",
                        scalarString(
                            db,
                            "SELECT origin FROM hierarchy_establishment_origin " +
                                "WHERE hierarchyId = 'GENERAL'",
                        ),
                    )
                }

                val converged = readCurrentState(dbName)
                assertEquals(
                    beforePlacements.map { it.toPlacementState() }.sortedBy { it.id },
                    converged.placements,
                )
                assertEquals(
                    beforeScopes.map {
                        GroupScopeState(
                            it.placementId,
                            it.hierarchyId,
                            it.groupSubjectId,
                            it.isDeleted,
                            it.version,
                        )
                    }.sortedBy { it.placementId },
                    converged.groupScopes,
                )
                assertEquals(
                    beforeLinks.map {
                        LinkedAppearanceState(
                            it.placementId,
                            it.hierarchyId,
                            it.isDeleted,
                            it.version,
                        )
                    }.sortedBy { it.placementId },
                    converged.linkedAppearances,
                )
                assertEquals(3, converged.markerVersion)
                assertEquals(MIGRATION_TEST_NOW + 1, converged.markerActivatedAt)
                assertEquals("ESTABLISHED", converged.origin)
                assertEquals(
                    beforeBeaconOrder,
                    converged.beacons.associate { it.id to it.order },
                )
            } finally {
                context.deleteDatabase(dbName)
            }
        }

    @Test
    fun `marker v2 converges invariants without recapture and marker v3 rerun is inert`() =
        runBlocking {
            val dbName = "migration-hierarchy-marker-convergence.db"
            try {
                harness.migrateTo179(
                    dbName,
                    178,
                    ::seedRichSchema178Fixture,
                ) { db ->
                    CanonicalHierarchyMigrationEstablisher().establish(db, MIGRATION_TEST_NOW)
                }
                lateinit var beforePlacements: List<HierarchyPlacement>
                lateinit var beforeScopes: List<HierarchyPlacementGroupScopeEntity>
                lateinit var beforeLinks: List<HierarchyPlacementLinkedAppearanceEntity>
                lateinit var beforeBeaconOrder: Map<String, Long>

                harness.openSchema179Raw(dbName).use { helper ->
                    val db = helper.writableDatabase
                    val persistence = CanonicalHierarchyMigrationPersistenceAdapter()
                    beforePlacements = persistence.readPlacements(db)
                    beforeScopes = persistence.readGroupScopes(db)
                    beforeLinks = persistence.readLinkedAppearances(db)
                    beforeBeaconOrder = readBeaconOrderRaw(db)
                    db.execSQL(
                        "UPDATE hierarchy_authority_activation_state SET version = 2 " +
                            "WHERE hierarchyId = 'GENERAL'",
                    )
                    db.execSQL(
                        "UPDATE workspaces SET parentWorkspaceId = 'workspace-a', " +
                            "workspaceOrder = 91 WHERE id = 'workspace-shared'",
                    )
                    db.execSQL(
                        "UPDATE main_beacons SET parent_beacon_id = 'beacon-parent' " +
                            "WHERE id = 'beacon-child'",
                    )
                    db.execSQL(
                        "INSERT INTO main_beacon_parent_links " +
                            "(parent_beacon_id, child_beacon_id, link_order, updatedAt, createdAt) " +
                            "VALUES ('beacon-peer', 'beacon-child', 6, 10, 10)",
                    )

                    db.beginTransaction()
                    try {
                        val report =
                            CanonicalHierarchyMigrationEstablisher().establish(
                                db,
                                MIGRATION_TEST_NOW + 1,
                            )
                        assertTrue(!report.performed)
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }

                    assertEquals(beforePlacements, persistence.readPlacements(db))
                    assertEquals(beforeScopes, persistence.readGroupScopes(db))
                    assertEquals(beforeLinks, persistence.readLinkedAppearances(db))
                    assertEquals(beforeBeaconOrder, readBeaconOrderRaw(db))
                    assertEquals(
                        3L,
                        scalarLong(
                            db,
                            "SELECT version FROM hierarchy_authority_activation_state " +
                                "WHERE hierarchyId = 'GENERAL'",
                        ),
                    )
                    assertEquals(
                        "ESTABLISHED",
                        scalarString(
                            db,
                            "SELECT origin FROM hierarchy_establishment_origin " +
                                "WHERE hierarchyId = 'GENERAL'",
                        ),
                    )
                }

                val converged = readCurrentState(dbName)
                assertEquals(
                    beforePlacements.map { it.toPlacementState() }.sortedBy { it.id },
                    converged.placements,
                )
                assertEquals(
                    beforeScopes.map {
                        GroupScopeState(
                            it.placementId,
                            it.hierarchyId,
                            it.groupSubjectId,
                            it.isDeleted,
                            it.version,
                        )
                    }.sortedBy { it.placementId },
                    converged.groupScopes,
                )
                assertEquals(
                    beforeLinks.map {
                        LinkedAppearanceState(
                            it.placementId,
                            it.hierarchyId,
                            it.isDeleted,
                            it.version,
                        )
                    }.sortedBy { it.placementId },
                    converged.linkedAppearances,
                )
                assertEquals(3, converged.markerVersion)
                assertEquals(MIGRATION_TEST_NOW + 1, converged.markerActivatedAt)

                harness.openCurrentRoom(dbName).let { room ->
                    try {
                        val db = room.openHelper.writableDatabase
                        db.beginTransaction()
                        try {
                            val report =
                                CanonicalHierarchyMigrationEstablisher().establish(
                                    db,
                                    MIGRATION_TEST_NOW + 2,
                                )
                            assertTrue(!report.performed)
                            db.setTransactionSuccessful()
                        } finally {
                            db.endTransaction()
                        }
                    } finally {
                        room.close()
                    }
                }
                assertEquals(converged, readCurrentState(dbName))
            } finally {
                context.deleteDatabase(dbName)
            }
        }

    private suspend fun readCurrentState(dbName: String): HierarchyMigrationState {
        val db = harness.openCurrentRoom(dbName)
        return try {
            hierarchyMigrationState(db)
        } finally {
            db.close()
        }
    }

    private fun readCurrentStateRaw(dbName: String): HierarchyMigrationState {
        harness.openSchema180Raw(dbName).use { helper ->
            return hierarchyMigrationStateRaw(helper.writableDatabase)
        }
    }

    private fun migrate179To180Direct(dbName: String) {
        harness.openSchema179Raw(dbName).use { helper ->
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                MIGRATION_179_180.migrate(db)
                db.execSQL("PRAGMA user_version = 180")
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun readBeaconOrderRaw(db: SupportSQLiteDatabase): Map<String, Long> =
        buildMap {
            db.query("SELECT id, beacon_order FROM main_beacons ORDER BY id ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    put(cursor.getString(0), cursor.getLong(1))
                }
            }
        }

    private fun HierarchyPlacement.toPlacementState(): PlacementState =
        PlacementState(
            id = id.value,
            hierarchyId = hierarchyId.value,
            targetType = target.type.name,
            targetId = target.id,
            parentPlacementId = parentPlacementId?.value,
            placementKind = placementKind.name,
            siblingOrder = siblingOrder,
            isDeleted = isDeleted,
            version = version,
        )

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getString(0)
        }

    private fun createFreshDatabase(dbName: String): AppDatabase {
        context.deleteDatabase(dbName)
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .addCallback(HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK)
            .build()
            .also { it.openHelper.writableDatabase }
    }

    private fun seedSchema150Fixture(db: SupportSQLiteDatabase) {
        insertContext(db, "root", null, 4L)
        insertContext(db, "child", "root", 9L)
    }

    private fun seedRichSchema178Fixture(db: SupportSQLiteDatabase) {
        insertContext(db, "workspace-a", null, 4L)
        insertContext(db, "workspace-b", null, 1L)
        insertContext(db, "workspace-shared", "workspace-a", 7L)
        insertContextBackedWorkspace(db, "workspace-a", null, 4L)
        insertContextBackedWorkspace(db, "workspace-b", null, 1L)
        insertContextBackedWorkspace(db, "workspace-shared", "workspace-a", 7L)
        db.execSQL(
            "INSERT INTO context_parent_links " +
                "(parent_context_id, child_context_id, link_order, createdAt, is_deleted, version) " +
                "VALUES ('workspace-b', 'workspace-shared', 3, 10, 0, 1)",
        )

        insertBeacon(db, "beacon-parent", null, 8L)
        insertBeacon(db, "beacon-peer", null, 2L)
        insertBeacon(db, "beacon-child", "beacon-parent", 5L)
        db.execSQL(
            "INSERT INTO main_beacon_parent_links " +
                "(parent_beacon_id, child_beacon_id, link_order, updatedAt, createdAt) " +
                "VALUES ('beacon-peer', 'beacon-child', 6, 10, 10)",
        )
        db.execSQL(
            "INSERT INTO main_beacon_groups " +
                "(id, title, group_order, updatedAt, createdAt) " +
                "VALUES ('group-a', 'Group A', 3, 10, 10)",
        )
        db.execSQL(
            "INSERT INTO main_beacon_group_members (group_id, beacon_id, member_order) " +
                "VALUES ('group-a', 'beacon-parent', 4)",
        )
        db.execSQL(
            "INSERT INTO main_beacon_context_cross_ref (beacon_id, context_id, ref_order) " +
                "VALUES ('beacon-parent', 'workspace-a', 6)",
        )
    }

    private fun insertContext(
        db: SupportSQLiteDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.execSQL(
            "INSERT INTO contexts " +
                "(id, name, parentId, createdAt, is_deleted, version, goal_order, scoring_status) " +
                "VALUES (?, ?, ?, 10, 0, 1, ?, 'NOT_ASSESSED')",
            arrayOf<Any?>(id, id, parentId, order),
        )
    }

    private fun insertBeacon(
        db: SupportSQLiteDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.execSQL(
            "INSERT INTO main_beacons " +
                "(id, title, readiness_status, parent_beacon_id, beacon_order, updatedAt, createdAt) " +
                "VALUES (?, ?, 'BLOCKED', ?, ?, 10, 10)",
            arrayOf<Any?>(id, id, parentId, order),
        )
    }

    private fun insertContextBackedWorkspace(
        db: SupportSQLiteDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.execSQL(
            "INSERT INTO workspaces " +
                "(id, nameOverride, parentWorkspaceId, workspaceOrder, createdAt, updatedAt, " +
                "isDeleted, version, provenance, sourceContextId) " +
                "VALUES (?, ?, ?, ?, 10, 10, 0, 1, 'CONTEXT_BACKED', ?)",
            arrayOf<Any?>(id, id, parentId, order, id),
        )
    }
}
