package com.romankozak.forwardappmobile.core.sync

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.sync.*
import com.romankozak.forwardappmobile.sync.datasource.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubjectConversionFileImportTest {
    @Test fun `legacy raw Android snapshot marker retains canonical subjects instead of a desktop subset`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.orientation()
            val payload = f.snapshot().copy(managedSubjectConversions = null)
            val content = mockk<IContentProvider>()
            coEvery { content.readText("legacy") } returns Result.success(com.google.gson.Gson().toJson(payload))
            val service = SyncFileService(content, mockk<FullBackupLocalDataSource>(), mockk<MergeRepository>(),
                mockk<SnapshotRestoreCanonicalizer>(), mockk<SnapshotRestoreRepository>())
            val resolved = service.resolveSnapshotBundleForImport("legacy").getOrThrow()
            assertThat(resolved.descriptor.format).isEqualTo(WorkspaceSnapshotFormat.AndroidSnapshotBundleV2)
            assertThat(resolved.snapshotBundle.managedSubjects).isEqualTo(payload.managedSubjects)
            assertThat(resolved.snapshotBundle.managedSubjectConversions).isNull()
        }
    }
    @Test fun `raw and wrapped conversion snapshots never fall through the desktop subset resolver`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            val coordinator = CanonicalAspectConversionCoordinator(f.db, f.orientations, f.aspects, f.links,
                CanonicalSemanticTagRepository(f.db), CanonicalPlanningReferenceRepository(f.db))
            coordinator.convert(coordinator.preview(id), OrientationKind.PROJECT, 30)
            val payload = f.snapshot()
            val codec = GsonBuilder().registerTypeAdapterFactory(SubjectConversionSnapshotAdapter()).create()
            val content = mockk<IContentProvider>()
            val service = SyncFileService(content, mockk<FullBackupLocalDataSource>(), mockk<MergeRepository>(),
                mockk<SnapshotRestoreCanonicalizer>(), mockk<SnapshotRestoreRepository>())
            for (text in listOf(codec.toJson(payload), codec.toJson(FullAppBackup(snapshotBundle = payload)))) {
                coEvery { content.readText("memory") } returns Result.success(text)
                val resolved = service.resolveSnapshotBundleForImport("memory").getOrThrow()
                assertThat(resolved.descriptor.format).isEqualTo(WorkspaceSnapshotFormat.AndroidSnapshotBundleV2)
                assertThat(resolved.snapshotBundle.managedSubjectConversions).isEqualTo(payload.managedSubjectConversions)
                assertThat(resolved.snapshotBundle.managedSubjects).isEqualTo(payload.managedSubjects)
                assertThat(resolved.snapshotBundle.orientations!!.single().kind).isEqualTo("PROJECT")
            }
        }
    }
}
