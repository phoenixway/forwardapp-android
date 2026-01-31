package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalService @Inject constructor(
    private val localDataSource: FullBackupLocalDataSource
) {
    suspend fun createFullSnapshotBundle(): SnapshotBundle {
        return localDataSource.loadFullSnapshotBundle()
    }

    suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
        // This will be a non-destructive operation, handled by the implementation
        localDataSource.applySnapshotBundle(bundle)
    }
}
