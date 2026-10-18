package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test-only physical feasibility proof for deletion with mixed child promotion.
 * Does NOT modify or implement the production deletion command.
 */
@RunWith(RobolectricTestRunner::class)
class H1UnifiedContainerPromotionRoomTest {
    private fun database() =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private data class Fixture(
        val anchor: PlacementId,
        val one: PlacementId,
        val two: PlacementId,
        val deep: PlacementId,
        val outside: PlacementId,
        val outer: String?,
        val deleting: String,
        val nested: String,
    )

    private fun o(id: PlacementId) = H1TypedNode.Occurrence(id.value)
    private fun c(id: String) = H1TypedNode.Container(id)

    private suspend fun graph(db: AppDatabase) =
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll().map {
                it.toHierarchyPlacementStrict()
            },
            BigPicturePresentationRepository(db).snapshot(),
        )

    private suspend fun fixture(db: AppDatabase, nestedDeletion: Boolean): Fixture {
        val names = listOf("anchor", "one", "two", "deep", "outside")
        db.workspaceDao().upsert(names.map { name ->
            WorkspaceEntity(
                name, name, null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )
        })

        val h1 = CanonicalHierarchyPlacementRepository(db)
        suspend fun occurrence(name: String, parent: PlacementId?, now: Long) =
            h1.createPrimaryAppearance(
                HierarchyTargetRef(HierarchyTargetType.WORKSPACE, name),
                parentPlacementId = parent,
                now = now,
            )

        val anchor = occurrence("anchor", null, 1)
        val one = occurrence("one", anchor, 2)
        val two = occurrence("two", anchor, 3)
        val deep = occurrence("deep", anchor, 4)
        val outside = occurrence("outside", anchor, 5)

        val repo = BigPicturePresentationRepository(db)
        val outer = if (nestedDeletion) {
            repo.create(
                "Outer", anchorPlacementId = anchor.value,
                now = 10, id = "outer",
            )
        } else null

        val deleting = repo.create(
            "Deleting",
            anchorPlacementId = anchor.value,
            parentContainerId = outer,
            now = 11, id = "deleting",
        )
        val nested = repo.create(
            "Nested",
            anchorPlacementId = anchor.value,
            parentContainerId = deleting,
            now = 12, id = "nested",
        )

        repo.assign(one.value, deleting, now = 15)
        repo.assign(two.value, deleting, now = 16)
        repo.assign(deep.value, nested, now = 17)
        if (outer != null) repo.assign(outside.value, outer, now = 18)

        // Give the existing graph an explicit alternating mixed order.
        db.withTransaction {
            val placementOrders = mapOf(
                one.value to 0L,
                two.value to 2L,
                deep.value to 0L,
                outside.value to if (nestedDeletion) 1L else 2L,
            )
            val placements = db.hierarchyPlacementDao().getAll()
            db.hierarchyPlacementDao().upsertAll(
                placements.filter { it.id in placementOrders }.map { row ->
                    row.copy(
                        siblingOrder = placementOrders.getValue(row.id),
                        updatedAt = 30,
                        version = row.version + 1,
                        syncedAt = null,
                    )
                },
            )

            val containerOrders = mapOf(
                deleting to if (nestedDeletion) 0L else 1L,
                nested to 1L,
            )
            val rows = repo.snapshot().containers
            db.bigPicturePresentationDao().upsertContainers(
                rows.filter { it.id in containerOrders }.map { row ->
                    BigPictureContainerEntity(
                        row.copy(
                            siblingOrder = containerOrders.getValue(row.id),
                            updatedAt = 30,
                            version = row.version + 1,
                            syncedAt = null,
                        ),
                    )
                },
            )
        }

        graph(db)
        return Fixture(anchor, one, two, deep, outside, outer, deleting, nested)
    }

    /**
     * Prospective graph is validated before committing. One Room transaction
     * changes placement order, container parent/order and memberships together.
     *
     * Injected failure occurs after the first physical write for rollback proof.
     */
    private suspend fun testOnlyPromote(
        db: AppDatabase,
        containerId: String,
        injectFailure: Boolean = false,
    ) = db.withTransaction {
        val placementDao = db.hierarchyPlacementDao()
        val containerDao = db.bigPicturePresentationDao()
        val originalPlacements = placementDao.getAll()
            .map { it.toHierarchyPlacementStrict() }
        val originalPresentation = BigPicturePresentationRepository(db).snapshot()
        val original = projectH1UnifiedStructuralGraph(
            originalPlacements, originalPresentation,
        )

        val target = c(containerId)
        val parent = original.parentOf(target)
        val directChildren = original.childrenOf(target).map { it.node }
        val oldSiblings = original.childrenOf(parent).map { it.node }
        require(target in oldSiblings)

        val expanded = oldSiblings.flatMap { node ->
            if (node == target) directChildren else listOf(node)
        }
        val newOrders = expanded.withIndex().associate { it.value to it.index.toLong() }
        val liftedOccurrences = directChildren.filterIsInstance<H1TypedNode.Occurrence>()
            .mapTo(hashSetOf()) { it.id }
        val liftedContainers = directChildren.filterIsInstance<H1TypedNode.Container>()
            .mapTo(hashSetOf()) { it.id }

        val changedPlacements = originalPlacements.map { row ->
            val order = newOrders[o(row.id)]
            if (order == null) row else row.copy(
                siblingOrder = order,
                updatedAt = 100,
                version = row.version + 1,
                syncedAt = null,
            )
        }

        val changedContainers = originalPresentation.containers.map { row ->
            val order = newOrders[c(row.id)]
            when {
                row.id == containerId -> row.copy(
                    isDeleted = true,
                    updatedAt = 100,
                    version = row.version + 1,
                    syncedAt = null,
                )
                order != null -> row.copy(
                    parentContainerId = if (row.id in liftedContainers) {
                        (parent as? H1TypedNode.Container)?.id
                    } else row.parentContainerId,
                    siblingOrder = order,
                    updatedAt = 100,
                    version = row.version + 1,
                    syncedAt = null,
                )
                else -> row
            }
        }

        val changedMemberships = originalPresentation.memberships.map { row ->
            if (row.placementId !in liftedOccurrences) row
            else when (parent) {
                is H1TypedNode.Container -> row.copy(
                    containerId = parent.id,
                    updatedAt = 100,
                    version = row.version + 1,
                    syncedAt = null,
                )
                else -> row.copy(
                    isDeleted = true,
                    updatedAt = 100,
                    version = row.version + 1,
                    syncedAt = null,
                )
            }
        }
        require(
            liftedOccurrences.all { child ->
                originalPresentation.memberships.any {
                    it.placementId == child && !it.isDeleted
                }
            },
        ) { "Every promoted occurrence must have an existing live membership" }

        val prospective = originalPresentation.copy(
            containers = changedContainers,
            memberships = changedMemberships,
        )
        projectH1UnifiedStructuralGraph(changedPlacements, prospective)

        val oldPlacementsById = originalPlacements.associateBy { it.id }
        val changedPlacementRows = changedPlacements.filter {
            it != oldPlacementsById[it.id]
        }
        if (changedPlacementRows.isNotEmpty()) {
            placementDao.upsertAll(
                changedPlacementRows.map { it.toHierarchyPlacementEntity() },
            )
        }

        if (injectFailure) error("Injected promotion failure")

        val oldContainers = originalPresentation.containers.associateBy { it.id }
        val changedContainerRows = changedContainers.filter {
            it != oldContainers[it.id]
        }
        if (changedContainerRows.isNotEmpty()) {
            containerDao.upsertContainers(
                changedContainerRows.map(::BigPictureContainerEntity),
            )
        }

        val oldMemberships = originalPresentation.memberships.associateBy {
            it.placementId
        }
        val changedMembershipRows = changedMemberships.filter {
            it != oldMemberships[it.placementId]
        }
        if (changedMembershipRows.isNotEmpty()) {
            containerDao.upsertMemberships(
                changedMembershipRows.map(::BigPictureMembershipEntity),
            )
        }

        // Verify the actual physical final state inside the transaction.
        graph(db)
    }

    private suspend fun assertPromotion(db: AppDatabase, f: Fixture) {
        val result = graph(db)
        val parent = f.outer?.let(::c) ?: o(f.anchor)

        assertThat(result.childrenOf(parent).map { it.node }).containsExactly(
            o(f.one), c(f.nested), o(f.two), o(f.outside),
        ).inOrder()
        assertThat(result.parentOf(o(f.deep))).isEqualTo(c(f.nested))
        assertThat(result.parentOf(c(f.nested))).isEqualTo(parent)
        assertThat(result.nearestOccurrenceAncestor(o(f.one)))
            .isEqualTo(f.anchor.value)
        assertThat(result.nearestOccurrenceAncestor(o(f.two)))
            .isEqualTo(f.anchor.value)
        assertThat(result.edges.map { it.node }).doesNotContain(c(f.deleting))

        val presentation = BigPicturePresentationRepository(db).snapshot()
        val retired = presentation.containers.single { it.id == f.deleting }
        assertThat(retired.isDeleted).isTrue()
        assertThat(retired.syncedAt).isNull()

        for (id in listOf(f.one.value, f.two.value)) {
            val membership = presentation.memberships.single {
                it.placementId == id
            }
            assertThat(membership.version).isEqualTo(2L)
            assertThat(membership.syncedAt).isNull()
            if (f.outer == null) {
                assertThat(membership.isDeleted).isTrue()
            } else {
                assertThat(membership.isDeleted).isFalse()
                assertThat(membership.containerId).isEqualTo(f.outer)
            }
        }

        val innerMember = presentation.memberships.single {
            it.placementId == f.deep.value
        }
        assertThat(innerMember.containerId).isEqualTo(f.nested)
        assertThat(innerMember.version).isEqualTo(1L)
    }

    @Test
    fun `deleting occurrence anchored section promotes all mixed children`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db, nestedDeletion = false)
                testOnlyPromote(db, f.deleting)
                assertPromotion(db, f)
            } finally {
                db.close()
            }
        }

    @Test
    fun `deleting nested section promotes members into parent container`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db, nestedDeletion = true)
                testOnlyPromote(db, f.deleting)
                assertPromotion(db, f)
            } finally {
                db.close()
            }
        }

    @Test
    fun `failure after placement writes restores every physical table`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db, nestedDeletion = true)
                val beforePlacements = db.hierarchyPlacementDao().getAll()
                val beforePresentation = BigPicturePresentationRepository(db).snapshot()
                val beforeGraph = graph(db).edges

                val failure = runCatching {
                    testOnlyPromote(db, f.deleting, injectFailure = true)
                }.exceptionOrNull()

                assertThat(failure).isInstanceOf(IllegalStateException::class.java)
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforePlacements)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(beforePresentation)
                assertThat(graph(db).edges).containsExactlyElementsIn(beforeGraph)
            } finally {
                db.close()
            }
        }
}
