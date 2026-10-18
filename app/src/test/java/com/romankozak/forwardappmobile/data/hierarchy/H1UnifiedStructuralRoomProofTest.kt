package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Schema-199 physical feasibility only.
 * Direct DAO writes are test-owned, not production structural authority.
 */
@RunWith(RobolectricTestRunner::class)
class H1UnifiedStructuralRoomProofTest {
    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private data class Fixture(
        val a: PlacementId,
        val d: PlacementId,
        val b: PlacementId,
        val child: PlacementId,
        val x: String,
        val y: String,
    )

    private suspend fun occurrence(
        repo: CanonicalHierarchyPlacementRepository,
        id: String,
        parent: PlacementId? = null,
        now: Long,
    ): PlacementId = repo.createPrimaryAppearance(
        target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id),
        parentPlacementId = parent,
        now = now,
    )

    private suspend fun fixture(db: AppDatabase): Fixture {
        db.workspaceDao().upsert(
            listOf("a", "d", "b", "child", "sibling").map { id ->
                WorkspaceEntity(
                    id, id, null, null, 1, 1, null, false, 1,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                )
            },
        )
        val h1 = CanonicalHierarchyPlacementRepository(db)
        val a = occurrence(h1, "a", now = 1)
        val d = occurrence(h1, "d", now = 2)
        val b = occurrence(h1, "b", parent = a, now = 3)
        val child = occurrence(h1, "child", parent = b, now = 4)

        val presentation = BigPicturePresentationRepository(db)
        val x = presentation.create("X", a.value, now = 10, id = "container-x")
        val y = presentation.create("Y", d.value, now = 11, id = "container-y")
        presentation.assign(b.value, x, now = 12)

        return Fixture(a, d, b, child, x, y)
    }

    private suspend fun graph(db: AppDatabase): H1UnifiedStructuralGraph =
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() },
            BigPicturePresentationRepository(db).snapshot(),
        )

    private fun occurrenceNode(id: PlacementId) = H1TypedNode.Occurrence(id.value)
    private fun containerNode(id: String) = H1TypedNode.Container(id)

    @Test
    fun `final ingress gate rejects retired live anchor and rolls back peer H1 delta`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val store = CanonicalHierarchyPlacementSyncStore(db)
                val presentation = BigPicturePresentationRepository(db)

                val beforeH1 = store.loadAll()
                val beforePresentation = presentation.snapshot()
                val original = beforeH1.single { it.id == f.d.value }

                // Existing H1 admission can retire this occurrence because
                // it has no H1 child. The existing presentation validator
                // still sees its anchor record and accepts the old container.
                // The COMBINED live typed graph must reject that state.
                val failure = runCatching {
                    db.withTransaction {
                        store.mergeIncoming(
                            listOf(
                                original.copy(
                                    isDeleted = true,
                                    version = original.version + 1,
                                    updatedAt = original.updatedAt + 100,
                                    syncedAt = null,
                                ),
                            ),
                        )
                        presentation.merge(BigPicturePresentationSnapshot())
                        presentation.requireCurrentUnifiedStructuralGraphValid()
                    }
                }.exceptionOrNull()

                assertThat(failure)
                    .isInstanceOf(IllegalArgumentException::class.java)
                assertThat(store.loadAll())
                    .containsExactlyElementsIn(beforeH1)
                assertThat(presentation.snapshot())
                    .isEqualTo(beforePresentation)

                assertThat(graph(db).parentOf(containerNode(f.y)))
                    .isEqualTo(occurrenceNode(f.d))
            } finally {
                db.close()
            }
        }

    @Test
    fun `coherent H1 and membership peer delta passes final typed graph gate`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val store = CanonicalHierarchyPlacementSyncStore(db)
                val presentation = BigPicturePresentationRepository(db)

                val oldOccurrence = store.loadAll()
                    .single { it.id == f.b.value }
                val oldMembership = presentation.snapshot().memberships
                    .single { it.placementId == f.b.value }

                // Both records represent one logical mixed Move.
                // H1 retains nearest-occurrence ancestry, while the
                // membership supplies the immediate container parent.
                db.withTransaction {
                    store.mergeIncoming(
                        listOf(
                            oldOccurrence.copy(
                                parentPlacementId = f.d.value,
                                version = oldOccurrence.version + 1,
                                updatedAt = oldOccurrence.updatedAt + 100,
                                syncedAt = null,
                            ),
                        ),
                    )
                    presentation.merge(
                        BigPicturePresentationSnapshot(
                            memberships = listOf(
                                oldMembership.copy(
                                    containerId = f.y,
                                    version = oldMembership.version + 1,
                                    updatedAt = oldMembership.updatedAt + 100,
                                    syncedAt = null,
                                ),
                            ),
                        ),
                    )
                    presentation.requireCurrentUnifiedStructuralGraphValid()
                }

                val accepted = graph(db)
                assertThat(accepted.parentOf(occurrenceNode(f.b)))
                    .isEqualTo(containerNode(f.y))
                assertThat(accepted.parentOf(occurrenceNode(f.child)))
                    .isEqualTo(occurrenceNode(f.b))
                assertThat(
                    accepted.nearestOccurrenceAncestor(occurrenceNode(f.b)),
                ).isEqualTo(f.d.value)

                val moved = store.loadAll()
                    .single { it.id == f.b.value }
                assertThat(moved.version)
                    .isEqualTo(oldOccurrence.version + 1)
                assertThat(moved.syncedAt).isNull()

                val membership = presentation.snapshot().memberships
                    .single { it.placementId == f.b.value }
                assertThat(membership.version)
                    .isEqualTo(oldMembership.version + 1)
                assertThat(membership.syncedAt).isNull()
            } finally {
                db.close()
            }
        }

    @Test
    fun `mixed sibling reorder persists across occurrence and container tables`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val presentation = BigPicturePresentationRepository(db)
            presentation.create("R1", now = 13, id = "root-1")
            presentation.create("R2", now = 14, id = "root-2")

            db.withTransaction {
                val placements = db.hierarchyPlacementDao().getAll()
                    .map { it.toHierarchyPlacementStrict() }
                val changedPlacements = placements.map { row ->
                    when (row.id) {
                        f.a -> row.copy(
                            siblingOrder = 1, version = row.version + 1,
                            updatedAt = 30, syncedAt = null,
                        )
                        f.d -> row.copy(
                            siblingOrder = 3, version = row.version + 1,
                            updatedAt = 30, syncedAt = null,
                        )
                        else -> row
                    }
                }

                val before = presentation.snapshot()
                val changedContainers = before.containers.map { row ->
                    when (row.id) {
                        "root-1" -> row.copy(
                            siblingOrder = 0, version = row.version + 1,
                            updatedAt = 30, syncedAt = null,
                        )
                        "root-2" -> row.copy(
                            siblingOrder = 2, version = row.version + 1,
                            updatedAt = 30, syncedAt = null,
                        )
                        else -> row
                    }
                }

                projectH1UnifiedStructuralGraph(
                    changedPlacements,
                    before.copy(containers = changedContainers),
                )

                db.hierarchyPlacementDao().upsertAll(
                    changedPlacements.filter { row ->
                        row != placements.first { it.id == row.id }
                    }.map { it.toHierarchyPlacementEntity() },
                )
                db.bigPicturePresentationDao().upsertContainers(
                    changedContainers.filter { row ->
                        row != before.containers.first { it.id == row.id }
                    }.map(::BigPictureContainerEntity),
                )

                assertThat(graph(db).childrenOf(null).map { it.node }).containsExactly(
                    containerNode("root-1"),
                    occurrenceNode(f.a),
                    containerNode("root-2"),
                    occurrenceNode(f.d),
                ).inOrder()
            }

            assertThat(graph(db).childrenOf(null).map { it.node }).containsExactly(
                containerNode("root-1"),
                occurrenceNode(f.a),
                containerNode("root-2"),
                occurrenceNode(f.d),
            ).inOrder()
        } finally {
            db.close()
        }
    }

    @Test
    fun `within container reorder and same anchor container move preserve ancestry`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val h1 = CanonicalHierarchyPlacementRepository(db)
            val presentation = BigPicturePresentationRepository(db)
            val sibling = H1StructuralMutationCoordinator(db).createOccurrence(
                HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "sibling"),
                PlacementKind.PRIMARY,
                H1StructuralParent.Node(H1TypedNode.Container(f.x)), now = 15,
            )
            val another = presentation.create(
                "Another", anchorPlacementId = f.a.value,
                now = 17, id = "container-another",
            )

            db.withTransaction {
                val dao = db.hierarchyPlacementDao()
                val b = requireNotNull(dao.getById(f.b.value)).toHierarchyPlacementStrict()
                val s = requireNotNull(dao.getById(sibling.value)).toHierarchyPlacementStrict()

                dao.upsertAll(listOf(
                    b.copy(siblingOrder = 1, version = b.version + 1,
                        updatedAt = 30, syncedAt = null).toHierarchyPlacementEntity(),
                    s.copy(siblingOrder = 0, version = s.version + 1,
                        updatedAt = 30, syncedAt = null).toHierarchyPlacementEntity(),
                ))
                val current = graph(db)
                assertThat(current.childrenOf(containerNode(f.x)).map { it.node })
                    .containsExactly(occurrenceNode(sibling), occurrenceNode(f.b)).inOrder()
            }

            db.withTransaction {
                val before = presentation.snapshot()
                val member = before.memberships.single { it.placementId == f.b.value }
                val moved = member.copy(
                    containerId = another, version = member.version + 1,
                    updatedAt = 31, syncedAt = null,
                )
                val proposed = before.copy(memberships = before.memberships.map {
                    if (it.placementId == f.b.value) moved else it
                })
                projectH1UnifiedStructuralGraph(
                    db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() },
                    proposed,
                )
                db.bigPicturePresentationDao().upsertMemberships(
                    listOf(BigPictureMembershipEntity(moved)),
                )
            }

            assertThat(graph(db).parentOf(occurrenceNode(f.b)))
                .isEqualTo(containerNode(another))
            assertThat(graph(db).nearestOccurrenceAncestor(occurrenceNode(f.b)))
                .isEqualTo(f.a.value)
            assertThat(graph(db).parentOf(occurrenceNode(f.child)))
                .isEqualTo(occurrenceNode(f.b))
        } finally {
            db.close()
        }
    }

    @Test
    fun `cross ancestor move coordinates placement membership and descendants`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val presentation = BigPicturePresentationRepository(db)
            val nested = presentation.create(
                "Nested", anchorPlacementId = f.b.value,
                now = 16, id = "nested-under-b",
            )

            db.withTransaction {
                val placementDao = db.hierarchyPlacementDao()
                val existing = requireNotNull(placementDao.getById(f.b.value))
                    .toHierarchyPlacementStrict()
                val moved = existing.copy(
                    parentPlacementId = f.d,
                    version = existing.version + 1,
                    updatedAt = 30, syncedAt = null,
                )
                val before = presentation.snapshot()
                val member = before.memberships.single { it.placementId == f.b.value }
                val movedMember = member.copy(
                    containerId = f.y, version = member.version + 1,
                    updatedAt = 30, syncedAt = null,
                )
                val proposedPlacements = placementDao.getAll()
                    .map { it.toHierarchyPlacementStrict() }
                    .map { if (it.id == f.b) moved else it }
                val proposedPresentation = before.copy(
                    memberships = before.memberships.map {
                        if (it.placementId == f.b.value) movedMember else it
                    },
                )

                projectH1UnifiedStructuralGraph(
                    proposedPlacements, proposedPresentation,
                )

                placementDao.upsert(moved.toHierarchyPlacementEntity())
                db.bigPicturePresentationDao().upsertMemberships(
                    listOf(BigPictureMembershipEntity(movedMember)),
                )
                assertThat(graph(db).parentOf(occurrenceNode(f.b)))
                    .isEqualTo(containerNode(f.y))
            }

            val result = graph(db)
            assertThat(result.nearestOccurrenceAncestor(occurrenceNode(f.b)))
                .isEqualTo(f.d.value)
            assertThat(result.parentOf(occurrenceNode(f.child)))
                .isEqualTo(occurrenceNode(f.b))
            assertThat(result.parentOf(containerNode(nested)))
                .isEqualTo(occurrenceNode(f.b))
            assertThat(result.nearestOccurrenceAncestor(containerNode(nested)))
                .isEqualTo(f.b.value)
            assertThat(requireNotNull(db.hierarchyPlacementDao().getById(f.child.value))
                .parentPlacementId).isEqualTo(f.b.value)
        } finally {
            db.close()
        }
    }

    @Test
    fun `invalid partially coordinated move rolls back all physical writes`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val presentation = BigPicturePresentationRepository(db)
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforePresentation = presentation.snapshot()

            val result = runCatching {
                db.withTransaction {
                    val dao = db.hierarchyPlacementDao()
                    val b = requireNotNull(dao.getById(f.b.value))
                        .toHierarchyPlacementStrict()
                    dao.upsert(
                        b.copy(
                            parentPlacementId = f.d,
                            version = b.version + 1,
                            updatedAt = 40, syncedAt = null,
                        ).toHierarchyPlacementEntity(),
                    )
                    val container = beforePresentation.containers.single { it.id == f.y }
                    db.bigPicturePresentationDao().upsertContainers(
                        listOf(BigPictureContainerEntity(
                            container.copy(title = "Uncommitted", version = container.version + 1,
                                updatedAt = 40, syncedAt = null),
                        )),
                    )

                    // Membership still targets X under A. The prospective
                    // mixed graph is invalid, so the whole transaction fails.
                    graph(db)
                }
            }

            assertThat(result.exceptionOrNull())
                .isInstanceOf(IllegalArgumentException::class.java)
            assertThat(db.hierarchyPlacementDao().getAll())
                .containsExactlyElementsIn(beforePlacements)
            assertThat(presentation.snapshot()).isEqualTo(beforePresentation)
        } finally {
            db.close()
        }
    }
}
