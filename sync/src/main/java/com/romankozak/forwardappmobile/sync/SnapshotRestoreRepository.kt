package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.sync.datasource.SnapshotRestoreLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

/** Public replace operation. The supplied bundle has already crossed restore canonicalization. */
@Singleton
class SnapshotRestoreRepository
    @Inject
    constructor(
        private val localDataSource: SnapshotRestoreLocalDataSource,
    ) {
        suspend fun replaceWith(canonicalBundle: SnapshotBundle): Result<Unit> =
            runCatching { localDataSource.replaceWith(canonicalBundle) }
    }
