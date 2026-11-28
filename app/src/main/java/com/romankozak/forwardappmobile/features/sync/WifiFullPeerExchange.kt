package com.romankozak.forwardappmobile.features.sync

import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private const val STAGE_TIMEOUT_MILLIS = 90_000L

private val peerExchangeMutex = Mutex()

/** One peer must receive the post-merge full state; the manual global dirty ACK is not involved. */
internal suspend fun exchangeFullSnapshotWithPeer(
    repository: SyncRepository,
    peer: WifiAutoSyncPeer,
    onStage: suspend (String) -> Unit = {},
) = peerExchangeMutex.withLock {
    onStage("Отримання даних")
    val remoteJson = withTimeout(STAGE_TIMEOUT_MILLIS) {
        repository.fetchFullBackupFromPeer(peer.address, peer.key).getOrThrow()
    }
    onStage("Злиття даних")
    withTimeout(STAGE_TIMEOUT_MILLIS) {
        repository.importBackupJsonString(remoteJson, restoreSettings = false, allowEmpty = true).getOrThrow()
    }
    onStage("Підготовка даних")
    val localJson = withTimeout(STAGE_TIMEOUT_MILLIS) { repository.createFullBackupJsonString() }
    onStage("Надсилання даних")
    withTimeout(STAGE_TIMEOUT_MILLIS) {
        repository.pushFullBackupToPeer(peer.address, peer.key, localJson).getOrThrow()
    }
}
