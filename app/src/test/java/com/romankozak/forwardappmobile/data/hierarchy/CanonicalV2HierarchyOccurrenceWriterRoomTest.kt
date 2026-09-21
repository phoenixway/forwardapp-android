package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalV2HierarchyOccurrenceWriterRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `CUT between Groups mutates exact occurrence scope and PART_OF`() = runBlocking {
        val db = database()
        try {
            group(db, GROUP_A, "legacy-a")
            group(db, GROUP_B, "legacy-b")
            subject(db, TARGET)
            placement(db, "root", TARGET)
            scope(db, "root", GROUP_A)
            partOf(db, "part-a", TARGET, GROUP_A)

            writer(db).executeBeaconPlan(
                plan =
                    HierarchyActionPlan(
                        structural =
                            listOf(
                                HierarchyStructuralOperand.MoveOccurrence(
                                    placementId = PlacementId("root"),
                                    newParentPlacementId = null,
                                ),
                            ),
                        occurrenceScope =
                            listOf(
                                HierarchyOccurrenceScopeOperand.SetRootGroupScope(
                                    placementId = PlacementId("root"),
                                    groupSubjectId = GROUP_B,
                                ),
                            ),
                        semantic =
                            listOf(
                                HierarchySemanticOperand
                                    .ReconcileBeaconGroupMembershipFromRootScopes(
                                        subjectTarget(TARGET),
                                    ),
                            ),
                    ),
                now = 20L,
            )

            assertEquals(
                GROUP_B,
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("root"),
                ).groupSubjectId,
            )

            val relations = db.orientationDao().getAllOrientationRelations()
            assertTrue(relations.single { it.toOrientationId == GROUP_A }.isDeleted)
            assertFalse(relations.single { it.toOrientationId == GROUP_B }.isDeleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun `LINK into another Group creates fresh root occurrence and union membership`() = runBlocking {
        val db = database()
        try {
            group(db, GROUP_A, "legacy-a")
            group(db, GROUP_B, "legacy-b")
            subject(db, TARGET)
            placement(db, "root-a", TARGET)
            scope(db, "root-a", GROUP_A)
            partOf(db, "part-a", TARGET, GROUP_A)

            val result =
                writer(db).executeBeaconPlan(
                    plan =
                        HierarchyActionPlan(
                            occurrenceScope =
                                listOf(
                                    HierarchyOccurrenceScopeOperand.CreateRootLinkAppearance(
                                        sourceOccurrenceId = PlacementId("root-a"),
                                        target = subjectTarget(TARGET),
                                        groupSubjectId = GROUP_B,
                                    ),
                                ),
                            semantic =
                                listOf(
                                    HierarchySemanticOperand
                                        .ReconcileBeaconGroupMembershipFromRootScopes(
                                            subjectTarget(TARGET),
                                        ),
                                ),
                        ),
                    now = 20L,
                )

            val created = result.createdPlacementIds.single()
            assertNotEquals(PlacementId("root-a"), created)

            val newPlacement =
                requireNotNull(db.hierarchyPlacementDao().getById(created.value))
            assertEquals("LINK", newPlacement.placementKind)
            assertNull(newPlacement.parentPlacementId)

            assertEquals(
                GROUP_B,
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId(created.value),
                ).groupSubjectId,
            )

            assertEquals(
                setOf(GROUP_A, GROUP_B),
                db.orientationDao()
                    .getAllOrientationRelations()
                    .filter {
                        !it.isDeleted &&
                            it.relationType == OrientationRelationType.PART_OF.name &&
                            it.fromOrientationId == TARGET
                    }
                    .mapTo(linkedSetOf()) { it.toOrientationId },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `CUT Group root under Beacon retires exact scope and final membership`() = runBlocking {
        val db = database()
        try {
            group(db, GROUP_A, "legacy-a")
            subject(db, TARGET)
            subject(db, DESTINATION)

            placement(db, "source-root", TARGET, kind = "LINK")
            placement(db, "destination-root", DESTINATION)
            scope(db, "source-root", GROUP_A)
            scope(db, "destination-root", null)
            partOf(db, "part-a", TARGET, GROUP_A)

            writer(db).executeBeaconPlan(
                plan =
                    HierarchyActionPlan(
                        structural =
                            listOf(
                                HierarchyStructuralOperand.MoveOccurrence(
                                    placementId = PlacementId("source-root"),
                                    newParentPlacementId = PlacementId("destination-root"),
                                ),
                            ),
                        occurrenceScope =
                            listOf(
                                HierarchyOccurrenceScopeOperand.RetireRootGroupScope(
                                    placementId = PlacementId("source-root"),
                                ),
                            ),
                        semantic =
                            listOf(
                                HierarchySemanticOperand
                                    .ReconcileBeaconGroupMembershipFromRootScopes(
                                        subjectTarget(TARGET),
                                    ),
                            ),
                    ),
                now = 20L,
            )

            assertEquals(
                "destination-root",
                requireNotNull(
                    db.hierarchyPlacementDao().getById("source-root"),
                ).parentPlacementId,
            )
            assertTrue(
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("source-root"),
                ).isDeleted,
            )
            assertTrue(
                db.orientationDao()
                    .getAllOrientationRelations()
                    .single { it.toOrientationId == GROUP_A }
                    .isDeleted,
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `invalid Group identity rolls back fused mutation`() = runBlocking {
        val db = database()
        try {
            group(db, GROUP_A, "legacy-a")
            subject(db, TARGET)
            placement(db, "root", TARGET)
            scope(db, "root", GROUP_A)
            partOf(db, "part-a", TARGET, GROUP_A)

            val failure =
                runCatching {
                    writer(db).executeBeaconPlan(
                        plan =
                            HierarchyActionPlan(
                                structural =
                                    listOf(
                                        HierarchyStructuralOperand.MoveOccurrence(
                                            placementId = PlacementId("root"),
                                            newParentPlacementId = null,
                                        ),
                                    ),
                                occurrenceScope =
                                    listOf(
                                        HierarchyOccurrenceScopeOperand.SetRootGroupScope(
                                            placementId = PlacementId("root"),
                                            groupSubjectId = "missing-group",
                                        ),
                                    ),
                                semantic =
                                    listOf(
                                        HierarchySemanticOperand
                                            .ReconcileBeaconGroupMembershipFromRootScopes(
                                                subjectTarget(TARGET),
                                            ),
                                    ),
                            ),
                        now = 20L,
                    )
                }

            assertTrue(failure.isFailure)
            assertEquals(
                GROUP_A,
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("root"),
                ).groupSubjectId,
            )
            assertFalse(
                db.orientationDao()
                    .getAllOrientationRelations()
                    .single { it.toOrientationId == GROUP_A }
                    .isDeleted,
            )
        } finally {
            db.close()
        }
    }

    private fun writer(db: AppDatabase) =
        CanonicalV2HierarchyOccurrenceWriter(
            database = db,
            repository = CanonicalHierarchyPlacementRepository(db),
            groupScopeCoordinator = HierarchyPlacementGroupScopeMutationCoordinator(db),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun subject(
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

    private suspend fun group(
        db: AppDatabase,
        subjectId: String,
        legacyId: String,
    ) {
        subject(db, subjectId)
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
                    id = "mapping-$legacyId",
                    sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                    sourceId = legacyId,
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

    private suspend fun placement(
        db: AppDatabase,
        id: String,
        targetId: String,
        parent: String? = null,
        kind: String = "PRIMARY",
        order: Long = 0L,
    ) {
        db.hierarchyPlacementDao().upsert(
            HierarchyPlacementEntity(
                id = id,
                hierarchyId = "GENERAL",
                targetType = "MANAGED_SUBJECT",
                targetId = targetId,
                parentPlacementId = parent,
                placementKind = kind,
                siblingOrder = order,
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            ),
        )
    }

    private suspend fun scope(
        db: AppDatabase,
        placementId: String,
        groupSubjectId: String?,
    ) {
        db.hierarchyPlacementGroupScopeDao().upsert(
            HierarchyPlacementGroupScopeEntity(
                placementId = placementId,
                hierarchyId = "GENERAL",
                groupSubjectId = groupSubjectId,
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            ),
        )
    }

    private suspend fun partOf(
        db: AppDatabase,
        id: String,
        from: String,
        to: String,
    ) {
        db.orientationDao().upsertOrientationRelations(
            listOf(
                OrientationRelationEntity(
                    id = id,
                    fromOrientationId = from,
                    toOrientationId = to,
                    relationType = OrientationRelationType.PART_OF.name,
                    relationOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.MANAGED_SUBJECT,
            id = id,
        )

    private companion object {
        const val TARGET = "beacon-target"
        const val DESTINATION = "beacon-destination"
        const val GROUP_A = "group-a"
        const val GROUP_B = "group-b"
    }
}
