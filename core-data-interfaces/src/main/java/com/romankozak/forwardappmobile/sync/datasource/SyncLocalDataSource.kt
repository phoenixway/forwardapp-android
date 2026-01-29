package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent

interface SyncLocalDataSource {
    suspend fun getUnsyncedChanges(): DatabaseContent
    suspend fun getChangesSince(timestamp: Long): DatabaseContent
    suspend fun markSyncedNow(content: DatabaseContent)
    suspend fun loadLocalDatabaseContent(): DatabaseContent
    suspend fun clearAllTables()
}