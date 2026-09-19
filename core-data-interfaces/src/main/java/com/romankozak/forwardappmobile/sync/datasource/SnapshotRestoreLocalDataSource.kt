package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle

/** Atomic Room replacement boundary for an already-canonical SnapshotBundle. */
interface SnapshotRestoreLocalDataSource {
    suspend fun replaceWith(bundle: SnapshotBundle)
}

/** Restore-only compatibility boundary. Never use this contract for merge or sync ingress. */
interface SnapshotRestoreCanonicalizer {
    fun canonicalize(bundle: SnapshotBundle): SnapshotBundle
}
