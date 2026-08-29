package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalService @Inject constructor() {
    suspend fun getChangesSince(since: Long): SnapshotBundle = SnapshotBundle()
    suspend fun clearAllTables() { /* no-op */ }
}
