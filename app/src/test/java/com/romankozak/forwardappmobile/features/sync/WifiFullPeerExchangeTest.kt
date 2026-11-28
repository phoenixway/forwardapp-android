package com.romankozak.forwardappmobile.features.sync

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import com.romankozak.forwardappmobile.sync.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WifiFullPeerExchangeTest {
    private val repository = mockk<SyncRepository>()
    private val peer = WifiAutoSyncPeer("phone-b", "Phone B", "192.168.1.20:8080", "peer-key")

    @Test fun `peer receives post-merge full state without global dirty ACK`() = runBlocking {
        coEvery { repository.fetchFullBackupFromPeer(peer.address, peer.key) } returns Result.success("remote")
        coEvery { repository.importBackupJsonString("remote", false, true) } returns Result.success(1)
        coEvery { repository.createFullBackupJsonString() } returns "merged"
        coEvery { repository.pushFullBackupToPeer(peer.address, peer.key, "merged") } returns Result.success(Unit)

        exchangeFullSnapshotWithPeer(repository, peer)

        coVerifyOrder {
            repository.fetchFullBackupFromPeer(peer.address, peer.key)
            repository.importBackupJsonString("remote", false, true)
            repository.createFullBackupJsonString()
            repository.pushFullBackupToPeer(peer.address, peer.key, "merged")
        }
        coVerify(exactly = 0) { repository.pushUnsyncedToWifi(any()) }
    }

    @Test fun `successful stages can together take longer than one stage timeout`() = runTest {
        coEvery { repository.fetchFullBackupFromPeer(peer.address, peer.key) } coAnswers {
            delay(20_000)
            Result.success("remote")
        }
        coEvery { repository.importBackupJsonString("remote", false, true) } coAnswers {
            delay(52_000)
            Result.success(1)
        }
        coEvery { repository.createFullBackupJsonString() } coAnswers {
            delay(14_000)
            "merged"
        }
        coEvery { repository.pushFullBackupToPeer(peer.address, peer.key, "merged") } coAnswers {
            delay(50_000)
            Result.success(Unit)
        }
        val stages = mutableListOf<String>()
        exchangeFullSnapshotWithPeer(repository, peer) { stages.add(it) }
        assertThat(stages).containsExactly("Отримання даних", "Злиття даних", "Підготовка даних", "Надсилання даних").inOrder()
    }

    @Test fun `merge failure never sends a snapshot to the peer`() = runBlocking {
        coEvery { repository.fetchFullBackupFromPeer(peer.address, peer.key) } returns Result.success("remote")
        coEvery { repository.importBackupJsonString("remote", false, true) } returns Result.failure(IllegalStateException("conflict"))

        val error = runCatching { exchangeFullSnapshotWithPeer(repository, peer) }.exceptionOrNull()

        assertThat(error?.message).contains("conflict")
        coVerify(exactly = 0) { repository.pushFullBackupToPeer(any(), any(), any()) }
    }
}
