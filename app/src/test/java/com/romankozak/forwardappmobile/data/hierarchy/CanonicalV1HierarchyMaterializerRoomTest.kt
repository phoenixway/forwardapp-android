package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalV1HierarchyMaterializerRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `deterministic placement id is frozen`() {
        assertEquals(
            "04c2614e-b2f0-5c48-8307-3388556dfbd2",
            CanonicalV1HierarchyMaterializer
                .deterministicPlacementId(
                    hierarchyId = "GENERAL",
                    occurrenceKey = "no-beacon/workspace:root",
                ).value,
        )
    }

    @Test
    fun `first materialization creates atomically and exact rerun preserves metadata`() =
        runBlocking {
            val db = database()
            try {
                workspace(db, "root")
                workspace(db, "child")
                val materializer = CanonicalV1HierarchyMaterializer(db)
                val snapshot =
                    CanonicalV1HierarchySnapshot(
                        occurrences =
                            listOf(
                                occurrence(
                                    key = "root",
                                    targetId = "root",
                                    parentKey = null,
                                    order = 0,
                                ),
                                occurrence(
                                    key = "root/child",
                                    targetId = "child",
                                    parentKey = "root",
                                    order = 0,
                                ),
                            ),
                    )

                val first = materializer.materialize(snapshot, now = 100L)
                val rowsAfterFirst = db.hierarchyPlacementDao().getAll()

                val second = materializer.materialize(snapshot, now = 200L)
                val rowsAfterSecond = db.hierarchyPlacementDao().getAll()

                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.CREATED,
                    first.outcome,
                )
                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.EXACT_RERUN,
                    second.outcome,
                )
                assertEquals(rowsAfterFirst, rowsAfterSecond)
                assertEquals(setOf(100L), rowsAfterSecond.map { it.createdAt }.toSet())
                assertEquals(setOf(100L), rowsAfterSecond.map { it.updatedAt }.toSet())
                assertEquals(setOf(1L), rowsAfterSecond.map { it.version }.toSet())
            } finally {
                db.close()
            }
        }

    @Test
    fun `group scope materializes separately and exact rerun preserves metadata`() =
        runBlocking {
            val db = database()
            try {
                managedSubject(db, "beacon-subject")
                canonicalGroup(db, legacyGroupId = "legacy-group", subjectId = "group-subject")

                val snapshot =
                    CanonicalV1HierarchySnapshot(
                        occurrences =
                            listOf(
                                CanonicalV1HierarchyOccurrence(
                                    occurrenceKey = "group:12:legacy-group/beacon:6:beacon",
                                    target =
                                        HierarchyTargetRef(
                                            HierarchyTargetType.MANAGED_SUBJECT,
                                            "beacon-subject",
                                        ),
                                    parentOccurrenceKey = null,
                                    siblingOrder = 0L,
                                    primaryEvidence = CanonicalV1PrimaryEvidence.CANONICAL_ROOT,
                                    sourceAuthority =
                                        CanonicalV1HierarchySourceAuthority.MAIN_BEACON_GROUP_ROOT_PROJECTION,
                                    placementKind = PlacementKind.PRIMARY,
                                    rootGroupScope =
                                        CanonicalV1RootGroupScope.group("group-subject"),
                                ),
                            ),
                    )

                val first = materializer(db).materialize(snapshot, now = 100L)
                val placementsAfterFirst = db.hierarchyPlacementDao().getAll()
                val scopesAfterFirst = db.hierarchyPlacementGroupScopeDao().getAll()

                assertEquals(CanonicalV1HierarchyMaterializationOutcome.CREATED, first.outcome)
                assertEquals(1, placementsAfterFirst.size)
                assertEquals(1, scopesAfterFirst.size)
                assertEquals(
                    placementsAfterFirst.single().id,
                    scopesAfterFirst.single().placementId,
                )
                assertEquals("GENERAL", scopesAfterFirst.single().hierarchyId)
                assertEquals("group-subject", scopesAfterFirst.single().groupSubjectId)
                assertEquals(100L, scopesAfterFirst.single().createdAt)
                assertEquals(100L, scopesAfterFirst.single().updatedAt)
                assertEquals(1L, scopesAfterFirst.single().version)

                val second = materializer(db).materialize(snapshot, now = 200L)
                val placementsAfterSecond = db.hierarchyPlacementDao().getAll()
                val scopesAfterSecond = db.hierarchyPlacementGroupScopeDao().getAll()

                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.EXACT_RERUN,
                    second.outcome,
                )
                assertEquals(placementsAfterFirst, placementsAfterSecond)
                assertEquals(scopesAfterFirst, scopesAfterSecond)
            } finally {
                db.close()
            }
        }

    @Test
    fun `exact pre-v176 H1 rerun fills missing group scope without rewriting placements`() =
        runBlocking {
            val db = database()
            try {
                managedSubject(db, "beacon-subject")
                canonicalGroup(
                    db,
                    legacyGroupId = "legacy-group",
                    subjectId = "group-subject",
                )

                val snapshot =
                    CanonicalV1HierarchySnapshot(
                        occurrences =
                            listOf(
                                CanonicalV1HierarchyOccurrence(
                                    occurrenceKey = "group:12:legacy-group/beacon:6:beacon",
                                    target =
                                        HierarchyTargetRef(
                                            HierarchyTargetType.MANAGED_SUBJECT,
                                            "beacon-subject",
                                        ),
                                    parentOccurrenceKey = null,
                                    siblingOrder = 0L,
                                    primaryEvidence =
                                        CanonicalV1PrimaryEvidence.CANONICAL_ROOT,
                                    sourceAuthority =
                                        CanonicalV1HierarchySourceAuthority
                                            .MAIN_BEACON_GROUP_ROOT_PROJECTION,
                                    placementKind = PlacementKind.PRIMARY,
                                    rootGroupScope =
                                        CanonicalV1RootGroupScope.group(
                                            "group-subject",
                                        ),
                                ),
                            ),
                    )

                val preV176Placements =
                    snapshot
                        .toDeterministicHierarchyPlacements(now = 100L)
                        .map { it.toHierarchyPlacementEntity() }
                db.hierarchyPlacementDao().upsertAll(preV176Placements)

                assertTrue(
                    db.hierarchyPlacementGroupScopeDao().getAll().isEmpty(),
                )

                val report =
                    materializer(db).materialize(
                        snapshot = snapshot,
                        now = 200L,
                    )

                val placementsAfter =
                    db.hierarchyPlacementDao().getAll()
                val scopesAfter =
                    db.hierarchyPlacementGroupScopeDao().getAll()

                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.EXACT_RERUN,
                    report.outcome,
                )
                assertEquals(preV176Placements, placementsAfter)
                assertEquals(1, scopesAfter.size)
                assertEquals(
                    preV176Placements.single().id,
                    scopesAfter.single().placementId,
                )
                assertEquals(
                    "group-subject",
                    scopesAfter.single().groupSubjectId,
                )
                assertEquals(200L, scopesAfter.single().createdAt)
                assertEquals(200L, scopesAfter.single().updatedAt)
                assertEquals(1L, scopesAfter.single().version)
            } finally {
                db.close()
            }
        }

    @Test
    fun `group scope without live CUT_OVER Group mapping fails before H1 write`() =
        runBlocking {
            val db = database()
            try {
                managedSubject(db, "beacon-subject")
                managedSubject(db, "fake-group-subject")
                db.orientationDao().upsertOrientations(
                    listOf(
                        OrientationEntity(
                            subjectId = "fake-group-subject",
                            kind = OrientationKind.MAIN_BEACON_GROUP.name,
                            lifecycle = null,
                            lifecycleOrigin = "UNSET",
                        ),
                    ),
                )

                val snapshot =
                    CanonicalV1HierarchySnapshot(
                        occurrences =
                            listOf(
                                CanonicalV1HierarchyOccurrence(
                                    occurrenceKey = "group:10:fake/beacon:6:beacon",
                                    target =
                                        HierarchyTargetRef(
                                            HierarchyTargetType.MANAGED_SUBJECT,
                                            "beacon-subject",
                                        ),
                                    parentOccurrenceKey = null,
                                    siblingOrder = 0L,
                                    primaryEvidence =
                                        CanonicalV1PrimaryEvidence.CANONICAL_ROOT,
                                    sourceAuthority =
                                        CanonicalV1HierarchySourceAuthority
                                            .MAIN_BEACON_GROUP_ROOT_PROJECTION,
                                    placementKind = PlacementKind.PRIMARY,
                                    rootGroupScope =
                                        CanonicalV1RootGroupScope.group(
                                            "fake-group-subject",
                                        ),
                                ),
                            ),
                    )

                val failure =
                    runCatching {
                        materializer(db).materialize(
                            snapshot = snapshot,
                            now = 100L,
                        )
                    }.exceptionOrNull()

                assertNotNull(failure)
                assertTrue(
                    failure?.message?.contains(
                        "no live CUT_OVER MAIN_BEACON_GROUP mapping",
                    ) == true,
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
                assertTrue(
                    db.hierarchyPlacementGroupScopeDao().getAll().isEmpty(),
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `changed snapshot after first materialization fails closed without partial write`() =
        runBlocking {
            val db = database()
            try {
                workspace(db, "root")
                workspace(db, "child")
                val materializer = CanonicalV1HierarchyMaterializer(db)

                materializer.materialize(
                    CanonicalV1HierarchySnapshot(
                        occurrences =
                            listOf(
                                occurrence("root", "root", null, 0),
                            ),
                    ),
                    now = 100L,
                )

                val failure =
                    runCatching {
                        materializer.materialize(
                            CanonicalV1HierarchySnapshot(
                                occurrences =
                                    listOf(
                                        occurrence("root", "root", null, 0),
                                        occurrence("root/child", "child", "root", 0),
                                    ),
                            ),
                            now = 200L,
                        )
                    }.exceptionOrNull()

                assertTrue(failure is CanonicalV1HierarchyMaterializationConflictException)
                val stored = db.hierarchyPlacementDao().getAll()
                assertEquals(1, stored.size)
                assertEquals("root", stored.single().targetId)
                assertEquals(100L, stored.single().updatedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `missing canonical target rolls back entire first materialization`() =
        runBlocking {
            val db = database()
            try {
                workspace(db, "root")
                val materializer = CanonicalV1HierarchyMaterializer(db)

                val failure =
                    runCatching {
                        materializer.materialize(
                            CanonicalV1HierarchySnapshot(
                                occurrences =
                                    listOf(
                                        occurrence("root", "root", null, 0),
                                        occurrence("root/missing", "missing", "root", 0),
                                    ),
                            ),
                            now = 100L,
                        )
                    }.exceptionOrNull()

                assertNotNull(failure)
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    private fun materializer(db: AppDatabase) =
        CanonicalV1HierarchyMaterializer(db)

    private suspend fun managedSubject(
        db: AppDatabase,
        id: String,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = id,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun canonicalGroup(
        db: AppDatabase,
        legacyGroupId: String,
        subjectId: String,
    ) {
        managedSubject(db, subjectId)
        db.orientationDao().upsertOrientations(
            listOf(
                OrientationEntity(
                    subjectId = subjectId,
                    kind = OrientationKind.MAIN_BEACON_GROUP.name,
                    lifecycle = null,
                    lifecycleOrigin = "UNSET",
                ),
            ),
        )
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "mapping-group-$legacyGroupId",
                    sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                    sourceId = legacyGroupId,
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
    }

    private fun occurrence(
        key: String,
        targetId: String,
        parentKey: String?,
        order: Long,
    ) = CanonicalV1HierarchyOccurrence(
        occurrenceKey = key,
        target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, targetId),
        parentOccurrenceKey = parentKey,
        siblingOrder = order,
        primaryEvidence =
            if (parentKey == null) {
                CanonicalV1PrimaryEvidence.CANONICAL_ROOT
            } else {
                CanonicalV1PrimaryEvidence.CANONICAL_PARENT
            },
        sourceAuthority =
            if (parentKey == null) {
                CanonicalV1HierarchySourceAuthority.WORKSPACE_ROOT
            } else {
                CanonicalV1HierarchySourceAuthority.WORKSPACE_PARENT
            },
        placementKind = PlacementKind.PRIMARY,
    )

    private suspend fun workspace(
        db: AppDatabase,
        id: String,
    ) {
        db.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
