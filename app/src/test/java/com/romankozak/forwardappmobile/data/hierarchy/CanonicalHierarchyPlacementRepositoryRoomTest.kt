package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalHierarchyPlacementRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `A create primary root and B second primary rejected`() = runBlocking {
        val db = database()
        try {
            subject(db, "subject")
            val repo = CanonicalHierarchyPlacementRepository(db)
            val target = subjectTarget("subject")

            val primary = repo.createPrimaryAppearance(target, now = 10L)

            val stored = requireNotNull(repo.getPlacement(primary))
            assertNull(stored.parentPlacementId)
            assertEquals(PlacementKind.PRIMARY, stored.placementKind)
            assertEquals(1L, stored.version)
            assertEquals(10L, stored.createdAt)
            assertEquals(10L, stored.updatedAt)

            val failure =
                runCatching {
                    repo.createPrimaryAppearance(target, now = 20L)
                }.exceptionOrNull()
            assertTrue(failure is PrimaryAppearanceConflictException)
            assertEquals(primary, repo.getPrimaryAppearance(target)?.id)
        } finally {
            db.close()
        }
    }

    @Test
    fun `C multiple links D duplicate same-target siblings E child below link and I repeated target ancestry are legal`() =
        runBlocking {
            val db = database()
            try {
                subject(db, "same")
                subject(db, "child")
                val repo = CanonicalHierarchyPlacementRepository(db)
                val same = subjectTarget("same")

                val primary = repo.createPrimaryAppearance(same, now = 10L)
                val link1 = repo.createLinkAppearance(same, now = 11L)
                val link2 = repo.createLinkAppearance(same, now = 12L)
                val nestedSame =
                    repo.createLinkAppearance(
                        target = same,
                        parentPlacementId = primary,
                        now = 13L,
                    )
                val child =
                    repo.createLinkAppearance(
                        target = subjectTarget("child"),
                        parentPlacementId = link1,
                        now = 14L,
                    )

                assertEquals(
                    listOf(primary, link1, link2),
                    repo.getLiveChildren(null).map { it.id }.filter { it != nestedSame },
                )
                assertEquals(4, repo.getLiveAppearances(same).size)
                assertEquals(primary, repo.getPlacement(nestedSame)?.parentPlacementId)
                assertEquals(link1, repo.getPlacement(child)?.parentPlacementId)
            } finally {
                db.close()
            }
        }

    @Test
    fun `F move placement and G batch move commit one final graph`() = runBlocking {
        val db = database()
        try {
            listOf("p1", "p2", "c1", "c2").forEach { subject(db, it) }
            val repo = CanonicalHierarchyPlacementRepository(db)

            val p1 = repo.createPrimaryAppearance(subjectTarget("p1"), now = 10L)
            val p2 = repo.createPrimaryAppearance(subjectTarget("p2"), now = 11L)
            val c1 =
                repo.createPrimaryAppearance(
                    subjectTarget("c1"),
                    parentPlacementId = p1,
                    now = 12L,
                )
            val c2 =
                repo.createPrimaryAppearance(
                    subjectTarget("c2"),
                    parentPlacementId = p2,
                    now = 13L,
                )

            repo.movePlacement(
                placementId = c1,
                newParentPlacementId = p2,
                position = HierarchyPlacementPosition.FIRST,
                now = 20L,
            )
            assertEquals(p2, repo.getPlacement(c1)?.parentPlacementId)

            repo.movePlacements(
                listOf(
                    HierarchyPlacementMove(
                        placementId = c1,
                        newParentPlacementId = p1,
                        position = HierarchyPlacementPosition.FIRST,
                    ),
                    HierarchyPlacementMove(
                        placementId = c2,
                        newParentPlacementId = p1,
                        position = HierarchyPlacementPosition.AFTER(c1),
                    ),
                ),
                now = 30L,
            )

            assertEquals(listOf(c1, c2), repo.getLiveChildren(p1).map { it.id })
            assertTrue(repo.getLiveChildren(p2).isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `H cyclic batch is rejected atomically and J actual placement cycle is rejected`() = runBlocking {
        val db = database()
        try {
            listOf("a", "b", "child").forEach { subject(db, it) }
            val repo = CanonicalHierarchyPlacementRepository(db)

            val a = repo.createPrimaryAppearance(subjectTarget("a"), now = 10L)
            val b = repo.createPrimaryAppearance(subjectTarget("b"), now = 11L)

            val batchFailure =
                runCatching {
                    repo.movePlacements(
                        listOf(
                            HierarchyPlacementMove(a, b),
                            HierarchyPlacementMove(b, a),
                        ),
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(batchFailure is HierarchyPlacementCycleException)
            assertNull(repo.getPlacement(a)?.parentPlacementId)
            assertNull(repo.getPlacement(b)?.parentPlacementId)

            val child =
                repo.createPrimaryAppearance(
                    subjectTarget("child"),
                    parentPlacementId = a,
                    now = 30L,
                )
            val singleFailure =
                runCatching {
                    repo.movePlacement(a, child, now = 31L)
                }.exceptionOrNull()

            assertTrue(singleFailure is HierarchyPlacementCycleException)
            assertNull(repo.getPlacement(a)?.parentPlacementId)
            assertEquals(a, repo.getPlacement(child)?.parentPlacementId)
        } finally {
            db.close()
        }
    }

    @Test
    fun `K move under tombstoned parent rejected`() = runBlocking {
        val db = database()
        try {
            subject(db, "parent")
            subject(db, "mover")
            val repo = CanonicalHierarchyPlacementRepository(db)

            val parent = repo.createPrimaryAppearance(subjectTarget("parent"), now = 10L)
            val mover = repo.createPrimaryAppearance(subjectTarget("mover"), now = 11L)
            repo.removePlacement(parent, now = 12L)

            val failure =
                runCatching {
                    repo.movePlacement(mover, parent, now = 13L)
                }.exceptionOrNull()

            assertTrue(failure is HierarchyParentDeletedException)
            assertNull(repo.getPlacement(mover)?.parentPlacementId)
        } finally {
            db.close()
        }
    }

    @Test
    fun `L cross hierarchy prospective parent is rejected with typed failure`() = runBlocking {
        val db = database()
        try {
            subject(db, "child")
            subject(db, "parent")

            val child =
                placement(
                    id = "child-placement",
                    hierarchyId = HierarchyId.GENERAL,
                    targetId = "child",
                    parentId = "parent-placement",
                )
            val parent =
                placement(
                    id = "parent-placement",
                    hierarchyId = HierarchyId("OTHER"),
                    targetId = "parent",
                )

            val failure =
                runCatching {
                    db.requireValidProspectiveHierarchy(listOf(child, parent))
                }.exceptionOrNull()

            assertTrue(failure is CrossHierarchyParentException)
        } finally {
            db.close()
        }
    }

    @Test
    fun `M remove leaf only N removing primary leaves links and zero primary O non-leaf rejected`() = runBlocking {
        val db = database()
        try {
            subject(db, "target")
            subject(db, "parent")
            subject(db, "child")
            val repo = CanonicalHierarchyPlacementRepository(db)
            val target = subjectTarget("target")

            val primary = repo.createPrimaryAppearance(target, now = 10L)
            val link = repo.createLinkAppearance(target, now = 11L)
            repo.removePlacement(primary, now = 12L)

            assertNull(repo.getPrimaryAppearance(target))
            assertEquals(listOf(link), repo.getLiveAppearances(target).map { it.id })

            val parent = repo.createPrimaryAppearance(subjectTarget("parent"), now = 20L)
            repo.createPrimaryAppearance(
                subjectTarget("child"),
                parentPlacementId = parent,
                now = 21L,
            )

            val failure =
                runCatching {
                    repo.removePlacement(parent, now = 22L)
                }.exceptionOrNull()

            assertTrue(failure is HierarchyChildPolicyRejectedException)
            assertTrue(repo.getPlacement(parent) != null)

            repo.removePlacement(link, now = 30L)
            assertNull(repo.getPlacement(link))
            assertTrue(db.hierarchyPlacementDao().getById(link.value)?.isDeleted == true)
        } finally {
            db.close()
        }
    }

    @Test
    fun `P restore valid Q dead target rejected R primary conflict rejected`() = runBlocking {
        val db = database()
        try {
            subject(db, "restore")
            subject(db, "dead")
            val repo = CanonicalHierarchyPlacementRepository(db)

            val restoreTarget = subjectTarget("restore")
            val first = repo.createPrimaryAppearance(restoreTarget, now = 10L)
            repo.removePlacement(first, now = 11L)
            repo.restorePlacement(first, now = 12L)
            assertEquals(first, repo.getPrimaryAppearance(restoreTarget)?.id)

            repo.removePlacement(first, now = 13L)
            val competing = repo.createPrimaryAppearance(restoreTarget, now = 14L)
            val conflict =
                runCatching {
                    repo.restorePlacement(first, now = 15L)
                }.exceptionOrNull()
            assertTrue(conflict is PrimaryAppearanceConflictException)
            assertEquals(competing, repo.getPrimaryAppearance(restoreTarget)?.id)

            val deadTarget = subjectTarget("dead")
            val deadPlacement = repo.createPrimaryAppearance(deadTarget, now = 20L)
            repo.removePlacement(deadPlacement, now = 21L)
            tombstoneSubject(db, "dead", 22L)

            val deadFailure =
                runCatching {
                    repo.restorePlacement(deadPlacement, now = 23L)
                }.exceptionOrNull()
            assertTrue(deadFailure is HierarchyTargetDeletedException)
            assertTrue(db.hierarchyPlacementDao().getById(deadPlacement.value)?.isDeleted == true)
        } finally {
            db.close()
        }
    }

    @Test
    fun `S sibling and hierarchy reads use sibling order then placement id deterministically`() = runBlocking {
        val db = database()
        try {
            subject(db, "a")
            subject(db, "b")
            subject(db, "c")
            val dao = db.hierarchyPlacementDao()

            dao.upsertAll(
                listOf(
                    entity("z", "a", siblingOrder = 5L),
                    entity("a", "b", siblingOrder = 5L),
                    entity("m", "c", siblingOrder = 2L),
                ),
            )

            val repo = CanonicalHierarchyPlacementRepository(db)
            assertEquals(
                listOf("m", "a", "z"),
                repo.getLiveChildren(null).map { it.id.value },
            )
            assertEquals(
                listOf("m", "a", "z"),
                repo.getLiveHierarchy().map { it.id.value },
            )
        } finally {
            db.close()
        }
    }

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
                    subjectType = ManagedSubjectType.ASPECT.name,
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

    private suspend fun tombstoneSubject(
        db: AppDatabase,
        id: String,
        now: Long,
    ) {
        val current = requireNotNull(db.orientationDao().getManagedSubject(id))
        db.orientationDao().upsertManagedSubjects(
            listOf(
                current.copy(
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = true,
                    version = current.version + 1L,
                ),
            ),
        )
    }

    @Suppress("unused")
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

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun placement(
        id: String,
        hierarchyId: HierarchyId,
        targetId: String,
        parentId: String? = null,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = hierarchyId,
        target = subjectTarget(targetId),
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = PlacementKind.PRIMARY,
        siblingOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun entity(
        id: String,
        targetId: String,
        siblingOrder: Long,
    ) = HierarchyPlacementEntity(
        id = id,
        hierarchyId = HierarchyId.GENERAL.value,
        targetType = HierarchyTargetType.MANAGED_SUBJECT.name,
        targetId = targetId,
        parentPlacementId = null,
        placementKind = PlacementKind.PRIMARY.name,
        siblingOrder = siblingOrder,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )
}
