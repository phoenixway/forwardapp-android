package com.romankozak.forwardappmobile.sync

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-facing sync repository for the compile-time disabled sync source set.
 *
 * The normal sync source set exposes a concrete SyncRepository in the same package.
 * Keep this disabled counterpart deliberately inert so app code can depend on the
 * same type without pulling syncOn implementation classes into syncOff builds.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val delegate: NoOpSyncApi,
) : SyncApi by delegate {
    suspend fun applyServerChanges(changes: SnapshotBundle): Result<Unit> =
        Result.failure(IllegalStateException("Sync is disabled"))

    suspend fun importBackupJsonString(jsonString: String): Result<Int> =
        Result.failure(IllegalStateException("Sync is disabled"))

    suspend fun loadSelectiveImportPreview(
        uri: Uri,
    ): Result<SelectiveImportPreviewBundle> =
        Result.failure(IllegalStateException("Sync is disabled"))

    suspend fun importSelectedSnapshotBundle(bundle: SnapshotBundle): Result<String> =
        Result.failure(IllegalStateException("Sync is disabled"))

    fun filterSnapshotBundleForSelectiveImport(
        bundle: SnapshotBundle,
        selection: WorkspaceSelectiveImportSelection,
    ): SnapshotBundle = bundle
}
