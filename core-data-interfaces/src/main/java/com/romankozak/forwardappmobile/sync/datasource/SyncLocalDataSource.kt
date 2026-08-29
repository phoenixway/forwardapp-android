package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection

interface SyncLocalDataSource {
    suspend fun getUnsyncedSelection(): LocalSyncSelection
    suspend fun acknowledge(selection: LocalSyncSelection)
    suspend fun getChangesSince(timestamp: Long): SnapshotBundle
    suspend fun clearAllTables()
}