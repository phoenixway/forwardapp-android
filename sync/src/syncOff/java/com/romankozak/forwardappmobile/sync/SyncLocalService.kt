package com.romankozak.forwardappmobile.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalService @Inject constructor() {
    suspend fun loadLocalDatabaseContent(): DatabaseContent = DatabaseContent()
    suspend fun getUnsyncedChanges(): DatabaseContent = DatabaseContent()
    suspend fun getChangesSince(since: Long): DatabaseContent = DatabaseContent()
    suspend fun clearAllTables() { /* no-op */ }
    suspend fun markSyncedNow(content: DatabaseContent) { /* no-op */ }
}
