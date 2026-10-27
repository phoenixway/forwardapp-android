package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.sync.CanonicalSnapshotTransactionWriter
import com.romankozak.forwardappmobile.core.sync.SnapshotRestoreLocalDataSourceImpl
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import com.romankozak.forwardappmobile.sync.datasource.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalSemanticTagTransportRoomTest {
    private suspend fun fullSnapshot(f: SemanticDeletionRoomFixture) = f.snapshot().copy(
        workspaceBacklogEntries = emptyList(), workspaceInboxRecords = emptyList())
    private fun sync(f: SemanticDeletionRoomFixture) = CanonicalOrientationSyncStore(f.db, f.dao,
        mockk(relaxed = true), f.db.workspaceDao(), f.db.workspaceTagRefDao(), mockk(relaxed = true))
    private fun restorer(f: SemanticDeletionRoomFixture) = SnapshotRestoreLocalDataSourceImpl(
        database = f.db, writer = CanonicalSnapshotTransactionWriter { payload ->
            assertThat(payload.managedSubjectTagRefs).isNull()
            f.dao.storeCanonicalPayload(payload, false, f.db.workspaceDao(), false)
        }, dayManagementRuntimeRepository = mockk(relaxed = true), dayPlanningAuthorityActivator = mockk(relaxed = true),
        tacticalPlanningAuthorityActivator = mockk(relaxed = true), strategicPlanningAuthorityActivator = mockk(relaxed = true),
        canonicalScoringAuthorityActivator = mockk(relaxed = true),
    )
    @Test fun `production exact Restore preserves tag state and exact syncedAt for both classes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source -> SemanticDeletionRoomFixture().use { target ->
            val repo = CanonicalSemanticTagRepository(source.db)
            repo.addTag(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, source.orientation()), "health", 20)
            repo.addTag(ManagedSubjectRef(ManagedSubjectType.ASPECT, source.aspect()), "facet", 21)
            val first = source.db.managedSubjectTagDao().getAll().first()
            source.db.managedSubjectTagDao().markSynced(first.id, first.version, 30)
            val snapshot = Gson().fromJson(Gson().toJson(fullSnapshot(source)), SnapshotBundle::class.java)
            restorer(target).replaceWith(snapshot)
            assertThat(target.db.managedSubjectTagDao().getAll()).isEqualTo(snapshot.managedSubjectTagRefs)
        } }
    }
    @Test fun `peer Merge preserves typed membership and leaves accepted rows dirty`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source -> SemanticDeletionRoomFixture().use { target ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, source.orientation())
            CanonicalSemanticTagRepository(source.db).addTag(ref, "health", 20)
            val row = source.db.managedSubjectTagDao().getAll().single()
            source.db.managedSubjectTagDao().markSynced(row.id, 1, 25)
            target.admit(source.snapshot(), merge = true)
            assertThat(target.db.managedSubjectTagDao().getAll().single()).isEqualTo(row)
        } }
    }
    @Test fun `stale live membership cannot resurrect tombstone even with a later timestamp`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            repo.addTag(ref, "health", 20)
            val live = f.db.managedSubjectTagDao().getAll().single()
            repo.removeTag(ref, "health", 30)
            CanonicalSemanticTagSyncStore(f.db).merge(listOf(live.copy(updatedAt = 1000)))
            assertThat(f.db.managedSubjectTagDao().getAll().single().isDeleted).isTrue()
            assertThat(f.db.managedSubjectTagDao().getAll().single().version).isEqualTo(2)
        }
    }
    @Test fun `identical equal-version replay is inert and divergent equal-version payload fails closed`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            CanonicalSemanticTagRepository(f.db).addTag(ref, "health", 20)
            val dao = f.db.managedSubjectTagDao()
            val row = dao.getAll().single()
            dao.markSynced(row.id, 1, 25)
            val acknowledged = dao.getAll()
            val store = CanonicalSemanticTagSyncStore(f.db)
            store.merge(listOf(row))
            assertThat(dao.getAll()).isEqualTo(acknowledged)
            for (different in listOf(row.copy(updatedAt = 999), row.copy(isDeleted = true)))
                assertThat(runCatching { store.merge(listOf(different)) }.isFailure).isTrue()
            assertThat(dao.getAll()).isEqualTo(acknowledged)
            store.merge(listOf(row.copy(version = 2, updatedAt = 30, isDeleted = true)))
            assertThat(dao.getAll().single().isDeleted).isTrue()
        }
    }
    @Test fun `exact-version canonical ACK cannot clear a newer tag removal`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            repo.addTag(ref, "health", 20)
            val payload = sync(f).loadUnsynced()
            val row = payload.semanticTagRefs.single()
            repo.removeTag(ref, "health", 30)
            sync(f).markSynced(CanonicalOrientationSyncAck(semanticTagRefs = listOf(CanonicalOrientationSyncVersion(row.id, row.version))))
            assertThat(f.db.managedSubjectTagDao().getAll().single().syncedAt).isNull()
            sync(f).markSynced(CanonicalOrientationSyncAck(semanticTagRefs = listOf(CanonicalOrientationSyncVersion(row.id, 2))))
            assertThat(f.db.managedSubjectTagDao().getAll().single().syncedAt).isNotNull()
            assertThat(f.db.managedSubjectTagDao().getUnsynced()).isEmpty()
            assertThat(f.db.managedSubjectTagDao().getChangedSince(25).single().isDeleted).isTrue()
        }
    }
    @Test fun `invalid imported tag rolls back entire production Restore including existing semantic state`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            CanonicalSemanticTagRepository(f.db).addTag(ref, "health", 20)
            val before = f.rows()
            val snapshot = fullSnapshot(f)
            val bad = snapshot.copy(managedSubjectTagRefs = snapshot.managedSubjectTagRefs!!.map { it.copy(subjectType = "ASPECT") })
            val failed = runCatching { restorer(f).replaceWith(bad) }
            assertThat(failed.isFailure).isTrue()
            assertThat(failed.exceptionOrNull()!!.message).contains("Semantic tag identity/tuple mismatch")
            assertThat(f.rows()).isEqualTo(before)
        }
    }
    @Test fun `older absent tag stream creates no tags and current empty stream restores empty`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            CanonicalSemanticTagRepository(f.db).addTag(ref, "health", 20)
            val saved = fullSnapshot(f)
            restorer(f).replaceWith(saved.copy(managedSubjectTagRefs = null))
            assertThat(f.db.managedSubjectTagDao().getAll()).isEmpty()
            restorer(f).replaceWith(saved)
            CanonicalSemanticTagSyncStore(f.db).merge(null)
            assertThat(f.db.managedSubjectTagDao().getAll()).hasSize(1)
            restorer(f).replaceWith(saved.copy(managedSubjectTagRefs = emptyList()))
            assertThat(f.db.managedSubjectTagDao().getAll()).isEmpty()
        }
    }
    @Test fun `divergent peer sidecar rolls back a preceding semantic update in the same transaction`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            CanonicalSemanticTagRepository(f.db).addTag(ref, "health", 20)
            val before = f.rows()
            val row = f.db.managedSubjectTagDao().getAll().single()
            assertThat(runCatching {
                f.db.withTransaction {
                    val owner = f.dao.getManagedSubject(ref.id)!!
                    f.dao.upsertManagedSubjects(listOf(owner.copy(title = "Rejected", version = 2)))
                    CanonicalSemanticTagSyncStore(f.db).merge(listOf(row.copy(updatedAt = 40)))
                }
            }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }
}
