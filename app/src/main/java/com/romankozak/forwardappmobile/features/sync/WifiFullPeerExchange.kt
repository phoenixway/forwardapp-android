package com.romankozak.forwardappmobile.features.sync

import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val peerExchangeMutex = Mutex()

/** One peer must receive the post-merge full state; the manual global dirty ACK is not involved. */
internal suspend fun exchangeFullSnapshotWithPeer(
    repository: SyncRepository,
    peer: WifiAutoSyncPeer,
    onStage: suspend (String) -> Unit = {},
) = peerExchangeMutex.withLock {
    onStage("Отримання даних")
    val remoteJson = repository.fetchFullBackupFromPeer(peer.address, peer.key).getOrThrow()
    onStage("Злиття даних")
    repository.importBackupJsonString(remoteJson, restoreSettings = false, allowEmpty = true).getOrThrow()
    onStage("Надсилання даних")
    val localJson = repository.createFullBackupJsonString()
    repository.pushFullBackupToPeer(peer.address, peer.key, localJson).getOrThrow()
}
