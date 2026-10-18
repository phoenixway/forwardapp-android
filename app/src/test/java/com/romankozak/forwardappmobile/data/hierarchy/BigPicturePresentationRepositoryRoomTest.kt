package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigPicturePresentationRepositoryRoomTest {
    private inline fun <T> AppDatabase.use(block: (AppDatabase) -> T): T =
        try { block(this) } finally { close() }

    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @Test fun `create rename reorder observe and exact version ACK`() = runBlocking {
        database().use { db ->
            val repo = BigPicturePresentationRepository(db)
            val id = repo.create("  WIP  ", now = 10)
            assertThat(repo.observe().first().containers.single().title).isEqualTo("WIP")
            repo.markSynced(BigPicturePresentationAck(containers = listOf(BigPictureRecordVersion(id, 1))), 11)
            assertThat(repo.snapshot().containers.single().syncedAt).isEqualTo(11)
            repo.update(id, "Next", null, 50, expectedVersion = 1, now = 20)
            repo.markSynced(BigPicturePresentationAck(containers = listOf(BigPictureRecordVersion(id, 1))), 21)
            val updated = repo.snapshot().containers.single()
            assertThat(updated.version).isEqualTo(2)
            assertThat(updated.syncedAt).isNull()
            assertThat(updated.siblingOrder).isEqualTo(50)
            assertThat(runCatching { repo.update(id, "stale", null, 0, 1, 22) }.isFailure).isTrue()
            assertThat(repo.snapshot().containers.single()).isEqualTo(updated)
        }
    }

    @Test fun `nested sections reject cycles and deleting a parent with live sections`() = runBlocking {
        database().use { db ->
            val repo = BigPicturePresentationRepository(db)
            val a = repo.create("A", now = 1)
            val b = repo.create("B", parentContainerId = a, now = 2)
            val before = repo.snapshot()
            assertThat(runCatching { repo.update(a, "A", b, 0, 1, 3) }.isFailure).isTrue()
            assertThat(runCatching { repo.tombstone(a, 1, 4) }.isFailure).isTrue()
            assertThat(repo.snapshot()).isEqualTo(before)
            repo.update(b, "B", null, 1, 1, 5)
            repo.tombstone(a, 1, 6)
            assertThat(repo.snapshot().containers.single { it.id == a }.isDeleted).isTrue()
        }
    }

    @Test fun `mixed membership unassign and container deletion preserve every semantic authority`() = runBlocking {
        database().use { db ->
            val dao = db.orientationDao()
            val o = CanonicalOrientationRepository(db, dao).createOrientation(OrientationKind.INTENT, "O", now = 1)
            val a = CanonicalAspectRepository(db, dao, HierarchyPlacementLifecycleCoordinator(db)).create("A", now = 2)
            db.workspaceDao().upsert(listOf(WorkspaceEntity("w", "W", null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)))
            val placements = CanonicalHierarchyPlacementRepository(db)
            val occurrences = listOf(
                placements.createPrimaryAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, o), now = 3),
                placements.createPrimaryAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, a), now = 4),
                placements.createPrimaryAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"), now = 5),
            )
            val subjectsBefore = dao.getAllManagedSubjects()
            val placementsBefore = db.hierarchyPlacementDao().getAll()
            val repo = BigPicturePresentationRepository(db)
            val section = repo.create("Mixed", now = 10)
            occurrences.forEach { repo.assign(it.value, section, now = 11) }
            assertThat(repo.snapshot().memberships).hasSize(3)
            repo.assign(occurrences.first().value, section, now = 12)
            assertThat(repo.snapshot().memberships.first().version).isEqualTo(1)
            repo.unassign(occurrences.first().value, expectedVersion = 1, now = 13)
            repo.tombstone(section, expectedVersion = 1, now = 14)
            assertThat(repo.snapshot().memberships.count { !it.isDeleted }).isEqualTo(2)
            assertThat(dao.getAllManagedSubjects()).containsExactlyElementsIn(subjectsBefore)
            assertThat(db.hierarchyPlacementDao().getAll()).containsExactlyElementsIn(placementsBefore)
            assertThat(dao.getAllOrientationRelations()).isEmpty()
            assertThat(dao.getAllAspectOrientationRefs()).isEmpty()
            assertThat(dao.getAllWorkspaceBindings()).isEmpty()
            assertThat(db.planningDao().getAllScopes()).isEmpty()
            assertThat(db.planningDao().getAllCommitments()).isEmpty()
            assertThat(db.contextDao().getAllRaw()).isEmpty()
            assertThat(db.mainBeaconDao().getAllGroupsSync()).isEmpty()
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
        }
    }

    @Test fun `wire roundtrip exact restore stale merge and equal version conflict`() = runBlocking {
        database().use { db ->
            val repo = BigPicturePresentationRepository(db)
            val id = repo.create("Section", now = 1)
            val original = repo.snapshot()
            val decoded = Gson().fromJson(Gson().toJson(original), BigPicturePresentationSnapshot::class.java)
            repo.restoreExact(decoded)
            assertThat(repo.snapshot()).isEqualTo(original)
            assertThat(runCatching {
                repo.merge(original.copy(containers = original.containers.map { it.copy(isDeleted = true, title = "divergent") }))
            }.isFailure).isTrue()
            assertThat(repo.snapshot()).isEqualTo(original)
            repo.tombstone(id, 1, 2)
            val deleted = repo.snapshot()
            repo.merge(original)
            assertThat(repo.snapshot()).isEqualTo(deleted)
            assertThat(runCatching {
                repo.merge(deleted.copy(containers = deleted.containers.map { it.copy(title = "divergent") }))
            }.isFailure).isTrue()
            assertThat(repo.snapshot()).isEqualTo(deleted)
        }
    }

    @Test fun `membership at arbitrary H1 depth persists but cross level assignment rolls back`() = runBlocking {
        database().use { db ->
            db.workspaceDao().upsert(listOf("parent", "child").map { id ->
                WorkspaceEntity(id, id, null, null, 1, 1, null, false, 1,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)
            })
            val hierarchy = CanonicalHierarchyPlacementRepository(db)
            val parent = hierarchy.createPrimaryAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "parent"), now = 1)
            val child = hierarchy.createPrimaryAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "child"), parentPlacementId = parent, now = 2)
            val repo = BigPicturePresentationRepository(db)
            val section = repo.create("Inside parent", anchorPlacementId = parent.value, now = 3)
            repo.assign(child.value, section, now = 4)
            val before = repo.snapshot()
            assertThat(runCatching { repo.assign(parent.value, section, now = 5) }.isFailure).isTrue()
            assertThat(repo.snapshot()).isEqualTo(before)
            assertThat(bigPicturePresentationEntries(before, hierarchy.getLiveHierarchy(), parent.value, section).single().id)
                .isEqualTo(child.value)
            repo.unassign(child.value, 1, 6)
            assertThat(runCatching { repo.assign(child.value, section, now = 7) }.isFailure).isTrue()
            repo.assign(child.value, section, now = 8, expectedVersion = 2)
            assertThat(repo.snapshot().memberships.single().version).isEqualTo(3)
            assertThat(hierarchy.getPlacement(child)?.parentPlacementId).isEqualTo(parent)
        }
    }


    @Test
    fun `R2F6A dirty delta retains container and membership tombstones until exact ACK`() =
        runBlocking {
            database().use { db ->
                db.workspaceDao().upsert(
                    listOf(
                        WorkspaceEntity(
                            "r2f6a-owner", "Owner", null, null,
                            1, 1, null, false, 1,
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val placement = CanonicalHierarchyPlacementRepository(db)
                    .createPrimaryAppearance(
                        HierarchyTargetRef(
                            HierarchyTargetType.WORKSPACE,
                            "r2f6a-owner",
                        ),
                        now = 5,
                    )

                val repo = BigPicturePresentationRepository(db)
                val containerId = repo.create("For ACK", now = 10)
                repo.assign(placement.value, containerId, now = 11)

                val sentVersionOne = BigPicturePresentationAck(
                    containers = listOf(BigPictureRecordVersion(containerId, 1)),
                    memberships = listOf(BigPictureRecordVersion(placement.value, 1)),
                )

                repo.markSynced(sentVersionOne, now = 12)
                assertThat(repo.dirty().containers).isEmpty()
                assertThat(repo.dirty().memberships).isEmpty()

                repo.unassign(placement.value, expectedVersion = 1, now = 30)
                repo.tombstone(containerId, expectedVersion = 1, now = 31)

                // ACK from the previous transmission arrives after new edits.
                repo.markSynced(sentVersionOne, now = 40)

                val dirty = repo.dirty()
                assertThat(dirty.containers).hasSize(1)
                assertThat(dirty.memberships).hasSize(1)
                assertThat(dirty.containers.single().isDeleted).isTrue()
                assertThat(dirty.memberships.single().isDeleted).isTrue()
                assertThat(dirty.containers.single().version).isEqualTo(2)
                assertThat(dirty.memberships.single().version).isEqualTo(2)
                assertThat(dirty.containers.single().syncedAt).isNull()
                assertThat(dirty.memberships.single().syncedAt).isNull()

                val delta = repo.changedSince(29)
                assertThat(delta.containers).containsExactlyElementsIn(dirty.containers)
                assertThat(delta.memberships).containsExactlyElementsIn(dirty.memberships)
                assertThat(repo.changedSince(31).containers).isEmpty()
                assertThat(repo.changedSince(31).memberships).isEmpty()

                repo.markSynced(
                    BigPicturePresentationAck(
                        containers = listOf(BigPictureRecordVersion(containerId, 2)),
                        memberships = listOf(BigPictureRecordVersion(placement.value, 2)),
                    ),
                    now = 41,
                )
                assertThat(repo.dirty().containers).isEmpty()
                assertThat(repo.dirty().memberships).isEmpty()

                // ACK never erases the tombstones themselves.
                assertThat(repo.snapshot().containers.single().isDeleted).isTrue()
                assertThat(repo.snapshot().memberships.single().isDeleted).isTrue()
            }
        }

    @Test fun `failed restore rolls back and rejects foreign hierarchy and unsupported format`() = runBlocking {
        database().use { db ->
            val repo = BigPicturePresentationRepository(db)
            repo.create("Keep", now = 1)
            val before = repo.snapshot()
            for (invalid in listOf(
                before.copy(formatVersion = 99),
                before.copy(containers = before.containers.map { it.copy(hierarchyId = "OTHER") }),
                before.copy(containers = before.containers.map { it.copy(title = " ") }),
            )) {
                assertThat(runCatching { repo.restoreExact(invalid) }.isFailure).isTrue()
                assertThat(runCatching { repo.merge(invalid) }.isFailure).isTrue()
                assertThat(repo.snapshot()).isEqualTo(before)
            }
        }
    }
}
