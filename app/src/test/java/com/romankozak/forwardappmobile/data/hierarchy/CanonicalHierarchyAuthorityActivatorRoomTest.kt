package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context as AndroidContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.data.database.HierarchyAuthorityActivationStateEntity
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOriginEntity
import com.romankozak.forwardappmobile.data.database.HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalHierarchyAuthorityActivatorRoomTest {
    private val context: AndroidContext = ApplicationProvider.getApplicationContext()

    @Test
    fun `fresh V2 System owners establish factory hierarchy only in H1`() =
        runBlocking {
            val db = database()
            try {
                SystemWorkspaceMaterializer(
                    database = db,
                    contextDao = db.contextDao(),
                    workspaceDao = db.workspaceDao(),
                ).also { materializer ->
                }.materializeAll(
                    now = 10L,
                    seedMissingFactoryCapabilities = false,
                )
                SystemWorkspaceTagSeed(
                    database = db,
                    contextDao = db.contextDao(),
                ).seedMissingCanonicalCollections(now = 11L)

                val expectedSnapshot =
                    CanonicalV1HierarchySnapshotBuilder().build(
                        CanonicalFreshHierarchyEstablishmentSource(db).read(),
                    )
                val expectedPlacements =
                    expectedSnapshot.toDeterministicHierarchyPlacements(now = 20L)

                val result = activator(db).ensureEstablished(now = 20L)

                assertTrue(result.performed)
                val live = db.hierarchyPlacementDao().getAll().filterNot { it.isDeleted }
                assertEquals(
                    expectedPlacements
                        .map { it.toHierarchyPlacementEntity() }
                        .sortedBy { it.id },
                    live.sortedBy { it.id },
                )
                val byTarget = live.associateBy { it.targetId }
                val inbox = requireNotNull(byTarget[SystemContexts.INBOX.raw])
                val today = requireNotNull(byTarget[SystemContexts.TODAY.raw])
                assertEquals(today.id, inbox.parentPlacementId)
                assertEquals(SystemOperationalDefinitions.all.size, live.size)
                assertTrue(live.all { it.placementKind == "PRIMARY" })
                assertTrue(db.hierarchyPlacementGroupScopeDao().getAll().isEmpty())
                assertTrue(db.hierarchyPlacementLinkedAppearanceDao().getAll().isEmpty())
                assertEquals(
                    CanonicalHierarchyAuthorityActivator.CURRENT_ACTIVATION_VERSION,
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value)
                        ?.version,
                )
                assertEquals(
                    HierarchyEstablishmentOrigin.ESTABLISHED.name,
                    db.hierarchyEstablishmentOriginDao().get(HierarchyId.GENERAL.value)?.origin,
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `supported older markers converge to v3 without reading or mutating H1`() =
        runBlocking {
            listOf(1, 2).forEach { oldVersion ->
                val db = database()
                try {
                    val placement =
                        HierarchyPlacementEntity(
                            id = "stable-placement-$oldVersion",
                            hierarchyId = HierarchyId.GENERAL.value,
                            targetType = "WORKSPACE",
                            targetId = "stable-target-$oldVersion",
                            parentPlacementId = null,
                            placementKind = "PRIMARY",
                            siblingOrder = 9L,
                            createdAt = 10L,
                            updatedAt = 20L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 7L,
                        )
                    db.hierarchyPlacementDao().upsert(placement)
                    db.hierarchyAuthorityActivationStateDao().upsert(
                        HierarchyAuthorityActivationStateEntity(
                            hierarchyId = HierarchyId.GENERAL.value,
                            version = oldVersion,
                            activatedAt = 50L,
                        ),
                    )
                    db.hierarchyEstablishmentOriginDao().upsert(
                        HierarchyEstablishmentOriginEntity(
                            hierarchyId = HierarchyId.GENERAL.value,
                            origin = HierarchyEstablishmentOrigin.FRESH_NATIVE.name,
                        ),
                    )

                    val first = activator(db).ensureEstablished(now = 100L)

                    assertFalse(first.performed)
                    assertNull(first.materialization)
                    assertEquals(listOf(placement), db.hierarchyPlacementDao().getAll())

                    val marker =
                        requireNotNull(
                            db.hierarchyAuthorityActivationStateDao()
                                .get(HierarchyId.GENERAL.value),
                        )
                    assertEquals(
                        CanonicalHierarchyAuthorityActivator.CURRENT_ACTIVATION_VERSION,
                        marker.version,
                    )
                    assertEquals(50L, marker.activatedAt)
                    assertEquals(
                        HierarchyEstablishmentOrigin.ESTABLISHED.name,
                        db.hierarchyEstablishmentOriginDao()
                            .get(HierarchyId.GENERAL.value)
                            ?.origin,
                    )

                    val second = activator(db).ensureEstablished(now = 200L)
                    assertFalse(second.performed)
                    assertNull(second.materialization)
                    assertEquals(listOf(placement), db.hierarchyPlacementDao().getAll())
                    assertEquals(
                        marker,
                        db.hierarchyAuthorityActivationStateDao()
                            .get(HierarchyId.GENERAL.value),
                    )
                } finally {
                    db.close()
                }
            }
        }

    @Test
    fun `legacy upgrade origin without marker fails closed because physical source is retired`() =
        runBlocking {
            val db = database()
            try {
                classifyAsLegacyUpgrade(db)

                val failure =
                    runCatching { activator(db).ensureEstablished(now = 100L) }
                        .exceptionOrNull()

                assertTrue(failure is IllegalStateException)
                assertTrue(
                    failure?.message.orEmpty()
                        .contains("cannot establish hierarchy from retired legacy storage"),
                )
                assertNull(
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value),
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
                assertEquals(
                    HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE.name,
                    db.hierarchyEstablishmentOriginDao()
                        .get(HierarchyId.GENERAL.value)
                        ?.origin,
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `fresh native origin rejects ordinary Workspace state before establishment`() =
        runBlocking {
            val db = database()
            try {
                workspace(db, "unexpected-user-workspace", null, 0L)

                val failure =
                    runCatching { activator(db).ensureEstablished(now = 100L) }
                        .exceptionOrNull()

                assertTrue(failure is CanonicalFreshHierarchyEstablishmentException)
                assertTrue(failure?.message.orEmpty().contains("unexpected Workspaces"))
                assertNull(
                    db.hierarchyAuthorityActivationStateDao().get(HierarchyId.GENERAL.value),
                )
                assertEquals(
                    HierarchyEstablishmentOrigin.FRESH_NATIVE.name,
                    db.hierarchyEstablishmentOriginDao().get(HierarchyId.GENERAL.value)?.origin,
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `fresh native origin rejects MainBeacon state before establishment`() =
        runBlocking {
            val db = database()
            try {
                SystemWorkspaceMaterializer(
                    database = db,
                    contextDao = db.contextDao(),
                    workspaceDao = db.workspaceDao(),
                ).also { materializer ->
                }.materializeAll(
                    now = 10L,
                    seedMissingFactoryCapabilities = false,
                )

                db.mainBeaconDao().insertBeacon(
                    MainBeacon(
                        id = "unexpected-user-beacon",
                        title = "Unexpected",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )

                val failure =
                    runCatching { activator(db).ensureEstablished(now = 100L) }
                        .exceptionOrNull()

                assertTrue(failure is CanonicalFreshHierarchyEstablishmentException)
                assertEquals(
                    "Fresh hierarchy establishment found MainBeacon state",
                    failure?.message,
                )
                assertNull(
                    db.hierarchyAuthorityActivationStateDao().get(HierarchyId.GENERAL.value),
                )
                assertEquals(
                    HierarchyEstablishmentOrigin.FRESH_NATIVE.name,
                    db.hierarchyEstablishmentOriginDao().get(HierarchyId.GENERAL.value)?.origin,
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `established origin without marker fails closed before establishment source`() =
        runBlocking {
            val db = database()
            try {
                db.hierarchyEstablishmentOriginDao().upsert(
                    HierarchyEstablishmentOriginEntity(
                        hierarchyId = HierarchyId.GENERAL.value,
                        origin = HierarchyEstablishmentOrigin.ESTABLISHED.name,
                    ),
                )
                db.mainBeaconDao().insertBeacon(
                    MainBeacon(
                        id = "must-not-be-read",
                        title = "must-not-be-read",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )

                val failure =
                    runCatching { activator(db).ensureEstablished(now = 100L) }
                        .exceptionOrNull()

                assertTrue(failure is IllegalStateException)
                assertTrue(
                    failure?.message.orEmpty()
                        .contains("ESTABLISHED without an activation marker"),
                )
                assertNull(
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value),
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
                assertEquals(
                    HierarchyEstablishmentOrigin.ESTABLISHED.name,
                    db.hierarchyEstablishmentOriginDao()
                        .get(HierarchyId.GENERAL.value)
                        ?.origin,
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `missing origin without marker fails closed before establishment source`() =
        runBlocking {
            val db = database()
            try {
                db.openHelper.writableDatabase.execSQL(
                    "DELETE FROM hierarchy_establishment_origin WHERE hierarchyId = ?",
                    arrayOf(HierarchyId.GENERAL.value),
                )
                db.mainBeaconDao().insertBeacon(
                    MainBeacon(
                        id = "must-not-be-read",
                        title = "must-not-be-read",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )

                val failure =
                    runCatching { activator(db).ensureEstablished(now = 100L) }
                        .exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(
                    failure?.message.orEmpty()
                        .contains("Missing durable hierarchy establishment origin"),
                )
                assertNull(
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value),
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
                assertNull(
                    db.hierarchyEstablishmentOriginDao()
                        .get(HierarchyId.GENERAL.value),
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `invalid origin without marker fails closed before establishment source`() =
        runBlocking {
            val db = database()
            try {
                db.hierarchyEstablishmentOriginDao().upsert(
                    HierarchyEstablishmentOriginEntity(
                        hierarchyId = HierarchyId.GENERAL.value,
                        origin = "CORRUPT_ORIGIN",
                    ),
                )
                db.mainBeaconDao().insertBeacon(
                    MainBeacon(
                        id = "must-not-be-read",
                        title = "must-not-be-read",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )

                val failure =
                    runCatching { activator(db).ensureEstablished(now = 100L) }
                        .exceptionOrNull()

                assertTrue(failure is IllegalStateException)
                assertTrue(
                    failure?.message.orEmpty()
                        .contains("Unsupported hierarchy establishment origin CORRUPT_ORIGIN"),
                )
                assertNull(
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value),
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
                assertEquals(
                    "CORRUPT_ORIGIN",
                    db.hierarchyEstablishmentOriginDao()
                        .get(HierarchyId.GENERAL.value)
                        ?.origin,
                )
            } finally {
                db.close()
            }
        }

    private suspend fun classifyAsLegacyUpgrade(db: AppDatabase) {
        db.hierarchyEstablishmentOriginDao().upsert(
            HierarchyEstablishmentOriginEntity(
                hierarchyId = HierarchyId.GENERAL.value,
                origin = HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE.name,
            ),
        )
    }

    private fun activator(db: AppDatabase): CanonicalHierarchyAuthorityActivator =
        CanonicalHierarchyAuthorityActivator(
            database = db,
            freshSource = CanonicalFreshHierarchyEstablishmentSource(db),
            snapshotBuilder = CanonicalV1HierarchySnapshotBuilder(),
            materializer = CanonicalV1HierarchyMaterializer(db),
        )

    private suspend fun context(
        db: AppDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.contextDao().insertContexts(
            listOf(
                Context(
                    id = id,
                    name = id,
                    description = null,
                    parentId = parentId,
                    createdAt = 1L,
                    updatedAt = 1L,
                    isDeleted = false,
                    version = 1L,
                    order = order,
                ),
            ),
        )
    }

    private suspend fun workspace(
        db: AppDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    roleCode = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                    sourceContextId = id,
                ),
            ),
        )
    }

    private suspend fun beaconWithCutOverTarget(
        db: AppDatabase,
        beaconId: String,
        subjectId: String,
        parentBeaconId: String?,
        order: Long,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = subjectId,
                    subjectType = "ASPECT",
                    title = "canonical-$beaconId",
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "mapping-$beaconId",
                    sourceType = LegacyOrientationSourceType.MAIN_BEACON.name,
                    sourceId = beaconId,
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.mainBeaconDao().insertBeacon(
            MainBeacon(
                id = beaconId,
                title = beaconId,
                order = order,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK)
            .allowMainThreadQueries()
            .build()
}
