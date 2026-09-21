package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalV2HierarchyMaterializedParityRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `materialized H2 snapshot has exhaustive occurrence parity with V2 projection`() =
        runBlocking {
            val db = database()
            try {
                val subjectRoot = subjectTarget("subject-root")
                val subjectChild = subjectTarget("subject-child")
                val repeatedWorkspace = workspaceTarget("shared")
                val linkedChild = workspaceTarget("linked-child")
                val structuralRoot = workspaceTarget("workspace-root")
                val freeRoot = workspaceTarget("free-root")

                insertSubject(db, subjectRoot.id)
                insertSubject(db, subjectChild.id)
                insertWorkspace(db, repeatedWorkspace.id)
                insertWorkspace(db, linkedChild.id)
                insertWorkspace(db, structuralRoot.id)
                insertWorkspace(db, freeRoot.id)

                val snapshot =
                    CanonicalV1HierarchySnapshot(
                        occurrences =
                            listOf(
                                occurrence(
                                    key = "group/subject-root",
                                    target = subjectRoot,
                                    parentKey = null,
                                    order = 0,
                                    kind = PlacementKind.LINK,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.MAIN_BEACON_ROOT,
                                    rootGroupScope = CanonicalV1RootGroupScope.NoGroup,
                                ),
                                occurrence(
                                    key = "group/subject-root/subject-child",
                                    target = subjectChild,
                                    parentKey = "group/subject-root",
                                    order = 0,
                                    kind = PlacementKind.LINK,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT,
                                ),
                                occurrence(
                                    key = "group/subject-root/shared",
                                    target = repeatedWorkspace,
                                    parentKey = "group/subject-root",
                                    order = 1,
                                    kind = PlacementKind.LINK,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.BEACON_OPERATIONAL_OWNER_PROJECTION,
                                ),
                                occurrence(
                                    key = "group/subject-root/shared/linked-child",
                                    target = linkedChild,
                                    parentKey = "group/subject-root/shared",
                                    order = 0,
                                    kind = PlacementKind.LINK,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.WORKSPACE_PARENT,
                                ),
                                occurrence(
                                    key = "no-beacon/workspace-root",
                                    target = structuralRoot,
                                    parentKey = null,
                                    order = 1,
                                    kind = PlacementKind.PRIMARY,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.WORKSPACE_ROOT,
                                ),
                                occurrence(
                                    key = "no-beacon/workspace-root/shared",
                                    target = repeatedWorkspace,
                                    parentKey = "no-beacon/workspace-root",
                                    order = 0,
                                    kind = PlacementKind.PRIMARY,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.WORKSPACE_PARENT,
                                ),
                                occurrence(
                                    key = "no-beacon/free-root",
                                    target = freeRoot,
                                    parentKey = null,
                                    order = 2,
                                    kind = PlacementKind.PRIMARY,
                                    authority =
                                        CanonicalV1HierarchySourceAuthority.WORKSPACE_ROOT,
                                ),
                            ),
                    )

                val materializer = CanonicalV1HierarchyMaterializer(db)

                val first =
                    materializer.materialize(
                        snapshot = snapshot,
                        now = 100L,
                    )
                val storedAfterFirst = db.hierarchyPlacementDao().getAll()
                val groupScopesAfterFirst =
                    db.hierarchyPlacementGroupScopeDao().getAll()
                val linkedAppearancesAfterFirst =
                    db.hierarchyPlacementLinkedAppearanceDao().getAll()

                assertEquals(1, groupScopesAfterFirst.size)
                assertEquals(
                    CanonicalV1HierarchyMaterializer
                        .deterministicPlacementId(
                            hierarchyId = "GENERAL",
                            occurrenceKey = "group/subject-root",
                        ).value,
                    groupScopesAfterFirst.single().placementId,
                )
                assertEquals(null, groupScopesAfterFirst.single().groupSubjectId)
                assertTrue(!groupScopesAfterFirst.single().isDeleted)

                assertEquals(1, linkedAppearancesAfterFirst.size)
                assertEquals(
                    CanonicalV1HierarchyMaterializer
                        .deterministicPlacementId(
                            hierarchyId = "GENERAL",
                            occurrenceKey = "group/subject-root/shared",
                        ).value,
                    linkedAppearancesAfterFirst.single().placementId,
                )
                assertTrue(!linkedAppearancesAfterFirst.single().isDeleted)
                assertTrue(
                    linkedAppearancesAfterFirst.none { linked ->
                        linked.placementId ==
                            CanonicalV1HierarchyMaterializer
                                .deterministicPlacementId(
                                    hierarchyId = "GENERAL",
                                    occurrenceKey = "group/subject-root/shared/linked-child",
                                ).value
                    },
                )

                val structuralFrame =
                    db.hierarchyPlacementDao()
                        .observeLiveStructuralReadFrame("GENERAL")
                        .first()
                val frameByPlacementId =
                    structuralFrame.associateBy { it.placement.id }

                val subjectRootPlacementId =
                    CanonicalV1HierarchyMaterializer
                        .deterministicPlacementId(
                            hierarchyId = "GENERAL",
                            occurrenceKey = "group/subject-root",
                        ).value
                val linkedWorkspacePlacementId =
                    CanonicalV1HierarchyMaterializer
                        .deterministicPlacementId(
                            hierarchyId = "GENERAL",
                            occurrenceKey = "group/subject-root/shared",
                        ).value
                val linkedChildPlacementId =
                    CanonicalV1HierarchyMaterializer
                        .deterministicPlacementId(
                            hierarchyId = "GENERAL",
                            occurrenceKey = "group/subject-root/shared/linked-child",
                        ).value

                assertEquals(
                    subjectRootPlacementId,
                    frameByPlacementId.getValue(subjectRootPlacementId)
                        .groupScope
                        ?.placementId,
                )
                assertEquals(
                    linkedWorkspacePlacementId,
                    frameByPlacementId.getValue(linkedWorkspacePlacementId)
                        .linkedAppearance
                        ?.placementId,
                )
                assertEquals(
                    null,
                    frameByPlacementId.getValue(linkedChildPlacementId)
                        .linkedAppearance,
                )

                val second =
                    materializer.materialize(
                        snapshot = snapshot,
                        now = 200L,
                    )
                val storedAfterSecond = db.hierarchyPlacementDao().getAll()
                val groupScopesAfterSecond =
                    db.hierarchyPlacementGroupScopeDao().getAll()
                val linkedAppearancesAfterSecond =
                    db.hierarchyPlacementLinkedAppearanceDao().getAll()

                assertEquals(groupScopesAfterFirst, groupScopesAfterSecond)
                assertEquals(linkedAppearancesAfterFirst, linkedAppearancesAfterSecond)

                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.CREATED,
                    first.outcome,
                )
                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.EXACT_RERUN,
                    second.outcome,
                )
                assertEquals(storedAfterFirst, storedAfterSecond)

                val persisted =
                    storedAfterSecond.map { it.toHierarchyPlacementStrict() }

                val admittedTargets =
                    snapshot.occurrences
                        .map { it.target }
                        .distinct()
                        .associateWith { target ->
                            CanonicalV2HierarchyTargetPresentation(
                                target = target,
                                title = target.id,
                            )
                        }

                val projection =
                    CanonicalV2HierarchyProjector().project(
                        placements = persisted,
                        admittedTargets = admittedTargets,
                    )

                val expected =
                    CanonicalV1ToV2HierarchyParityNormalizer.expected(snapshot)
                val actual =
                    CanonicalV1ToV2HierarchyParityNormalizer.actual(projection)

                assertEquals(expected, actual)
                assertEquals(snapshot.occurrences.size, actual.size)

                assertEquals(
                    snapshot.occurrences
                        .groupingBy { it.target }
                        .eachCount(),
                    actual
                        .groupingBy { it.target }
                        .eachCount(),
                )

                val repeated =
                    actual.filter { it.target == repeatedWorkspace }

                assertEquals(2, repeated.size)
                assertEquals(
                    setOf(PlacementKind.PRIMARY, PlacementKind.LINK),
                    repeated.map { it.placementKind }.toSet(),
                )

                val linkedOccurrence =
                    repeated.single { it.placementKind == PlacementKind.LINK }
                val linkedChildOccurrence =
                    actual.single { it.target == linkedChild }

                assertEquals(
                    linkedOccurrence.placementId,
                    linkedChildOccurrence.parentPlacementId,
                )
                assertEquals(
                    linkedOccurrence.occurrencePath + linkedChildOccurrence.placementId,
                    linkedChildOccurrence.occurrencePath,
                )

                assertEquals(
                    listOf(0, 1, 2),
                    projection.occurrences
                        .filter { it.parentPlacementId == null }
                        .map { it.rootOrder },
                )

                assertTrue(
                    projection.occurrences
                        .zipWithNext()
                        .any { (left, right) ->
                            left.target != right.target ||
                                left.placementId != right.placementId
                        },
                )
            } finally {
                db.close()
            }
        }

    private fun occurrence(
        key: String,
        target: HierarchyTargetRef,
        parentKey: String?,
        order: Long,
        kind: PlacementKind,
        authority: CanonicalV1HierarchySourceAuthority,
        rootGroupScope: CanonicalV1RootGroupScope? = null,
    ) = CanonicalV1HierarchyOccurrence(
        occurrenceKey = key,
        target = target,
        parentOccurrenceKey = parentKey,
        siblingOrder = order,
        primaryEvidence =
            if (kind == PlacementKind.PRIMARY) {
                if (parentKey == null) {
                    CanonicalV1PrimaryEvidence.CANONICAL_ROOT
                } else {
                    CanonicalV1PrimaryEvidence.CANONICAL_PARENT
                }
            } else {
                CanonicalV1PrimaryEvidence.NONE
            },
        sourceAuthority = authority,
        placementKind = kind,
        rootGroupScope = rootGroupScope,
    )

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.WORKSPACE,
            id = id,
        )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.MANAGED_SUBJECT,
            id = id,
        )

    private suspend fun insertWorkspace(
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
                    workspaceOrder = 0,
                    createdAt = 1,
                    updatedAt = 1,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private suspend fun insertSubject(
        db: AppDatabase,
        id: String,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = "ORIENTATION",
                    title = id,
                    description = null,
                    createdAt = 1,
                    updatedAt = 1,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                ),
            ),
        )
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).allowMainThreadQueries()
            .build()
}
