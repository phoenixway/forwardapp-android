package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.sync.datasource.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Post-conversion target-state fixtures only. These do not test or activate a conversion command. */
@RunWith(RobolectricTestRunner::class)
class SubjectConversionHistoryRoomTest {
    private suspend fun seedTarget(f: SemanticDeletionRoomFixture): ManagedSubjectConversionEntity {
        val id = f.orientation(com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind.PROJECT)
        f.orientations.updateOrientationSemantics(id, "Target state", null, 1, 20)
        return ManagedSubjectConversionEntity(managedSubjectConversionId(id, 1), id, 1, "ASPECT", "ORIENTATION",
            "PROJECT", 1, 2, null, 3, false, 20, 20, 1, null)
    }

    @Test fun `immutable history survives store reload JSON snapshot exact Restore and Merge`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source ->
            val row = seedTarget(source)
            CanonicalSubjectConversionStore(source.db).merge(listOf(row))
            val payload = Gson().fromJson(Gson().toJson(source.snapshot()), SnapshotBundle::class.java)
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { target ->
                target.admit(payload, merge)
                assertThat(target.db.managedSubjectConversionDao().getAll()).containsExactly(row)
            }
            // Reconstruct the store, not its data: persistence owns the facts.
            CanonicalSubjectConversionStore(source.db).merge(listOf(row))
            assertThat(source.db.managedSubjectConversionDao().getAll()).containsExactly(row)
        }
    }

    @Test fun `identical replay preserves ACK and divergent facts fail closed`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val row = seedTarget(f)
            val store = CanonicalSubjectConversionStore(f.db)
            store.merge(listOf(row))
            f.db.managedSubjectConversionDao().acknowledge(row.id, 1, 30)
            store.merge(listOf(row))
            val acknowledged = f.db.managedSubjectConversionDao().getAll()
            assertThat(acknowledged.single().syncedAt).isEqualTo(30)
            for (bad in listOf(row.copy(previousAspectOrder = 8), row.copy(createdAt = 21, updatedAt = 21), row.copy(version = 2))) {
                assertThat(runCatching { store.merge(listOf(bad)) }.isFailure).isTrue()
                assertThat(f.db.managedSubjectConversionDao().getAll()).isEqualTo(acknowledged)
            }
            store.merge(null)
            assertThat(f.db.managedSubjectConversionDao().getAll()).isEqualTo(acknowledged)
        }
    }

    @Test fun `dirty changedSince and exact version ACK use the existing canonical sync boundary`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val row = seedTarget(f)
            CanonicalSubjectConversionStore(f.db).merge(listOf(row))
            val sync = CanonicalOrientationSyncStore(f.db, f.dao, mockk(relaxed = true), f.db.workspaceDao(),
                f.db.workspaceTagRefDao(), mockk(relaxed = true))
            assertThat(sync.loadUnsynced().subjectConversions).containsExactly(row)
            sync.markSynced(CanonicalOrientationSyncAck(subjectConversions = listOf(CanonicalOrientationSyncVersion(row.id, 0))))
            assertThat(f.db.managedSubjectConversionDao().getUnsynced()).hasSize(1)
            sync.markSynced(CanonicalOrientationSyncAck(subjectConversions = listOf(CanonicalOrientationSyncVersion(row.id, 1))))
            assertThat(f.db.managedSubjectConversionDao().getUnsynced()).isEmpty()
            assertThat(f.db.managedSubjectConversionDao().getChangedSince(19)).hasSize(1)
            assertThat(f.db.managedSubjectConversionDao().getChangedSince(20)).isEmpty()
        }
    }

    @Test fun `malformed unsupported and mismatched subject facts never write partial history`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val row = seedTarget(f)
            val store = CanonicalSubjectConversionStore(f.db)
            val before = f.rows()
            for (bad in listOf(row.copy(id = "wrong"), row.copy(fromType = "ORIENTATION"), row.copy(formatVersion = 2),
                row.copy(targetOrientationKind = "GOAL"), row.copy(toSubjectVersion = 99), row.copy(fromSubjectVersion = 0),
                row.copy(updatedAt = 21), row.copy(previousAspectParentId = row.subjectId), row.copy(syncedAt = -1))) {
                assertThat(runCatching { store.merge(listOf(bad)) }.isFailure).isTrue()
                assertThat(f.rows()).isEqualTo(before)
            }
        }
    }

    @Test fun `late insert failure rolls back store writes and Restore failure preserves existing history`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val row = seedTarget(f)
            val store = CanonicalSubjectConversionStore(f.db)
            f.db.openHelper.writableDatabase.execSQL("CREATE TEMP TRIGGER fail_conversion BEFORE INSERT ON managed_subject_conversions BEGIN SELECT RAISE(ABORT,'test failure'); END")
            val before = f.rows()
            assertThat(runCatching { store.merge(listOf(row)) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            f.db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_conversion")
            store.merge(listOf(row))
            val stored = f.rows()
            assertThat(runCatching { store.restore(listOf(row, row.copy(id = "bad"))) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(stored)
        }
    }
}
