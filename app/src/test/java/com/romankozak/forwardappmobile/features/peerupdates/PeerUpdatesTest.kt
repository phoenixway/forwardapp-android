package com.romankozak.forwardappmobile.features.peerupdates

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.net.ServerSocket
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class PeerUpdatesTest {
    @get:Rule val folder = TemporaryFolder()

    private val hash = "a".repeat(64)
    private val local = PeerManifest(1, "com.example.forward", "1.0", 10, "prod:release", 3, hash, listOf(hash), 29, "Test phone", listOf("arm64-v8a"))
    private val newer = local.copy(versionCode = 11)

    private fun status(peer: PeerManifest) = compatibility(local, peer, 35, listOf("arm64-v8a"))

    @Test fun classifiesVersionAndRefusesDowngrade() {
        assertThat(status(newer)).isEqualTo(PeerCompatibility.NEWER)
        assertThat(status(local)).isEqualTo(PeerCompatibility.SAME)
        assertThat(status(local.copy(versionCode = 9))).isEqualTo(PeerCompatibility.OLDER)
    }

    @Test fun rejectsWrongApplicationSignerChannelAndPlatform() {
        val incompatible = listOf(
            newer.copy(applicationId = "com.example.forward.debug"),
            newer.copy(certificates = listOf("b".repeat(64))),
            newer.copy(variant = "exp:release"),
            newer.copy(variant = "prod:local"),
            newer.copy(minSdk = 36),
            newer.copy(abis = listOf("x86")),
            newer.copy(protocol = 2),
            newer.copy(sha256 = "not a digest"),
            newer.copy(size = MAX_APK_BYTES + 1),
        )
        incompatible.forEach { assertThat(status(it)).isEqualTo(PeerCompatibility.INCOMPATIBLE) }
    }

    @Test fun rejectsPublicAndInvalidAddresses() {
        assertThat(PeerAddress.parse("192.168.1.20").port).isEqualTo(PEER_PORT)
        listOf("8.8.8.8", "example.com", "127.0.0.1", "192.168.1.20:0", "192.168.1.20:abc")
            .forEach { assertThat(runCatching { PeerAddress.parse(it) }.isFailure).isTrue() }
    }

    @Test fun streamingReportsExactDigestAndDetectsPartialOrOversizeBody() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val output = ByteArrayOutputStream()
        val progress = mutableListOf<Long>()
        val digest = copyApk(ByteArrayInputStream(bytes), output, 3) { progress.add(it) }
        assertThat(digest).isEqualTo(MessageDigest.getInstance("SHA-256").digest(bytes).hex())
        assertThat(digest).isNotEqualTo(hash)
        assertThat(output.toByteArray()).isEqualTo(bytes)
        assertThat(progress).containsExactly(3L)
        for (size in listOf(2L, 4L)) {
            assertThat(runCatching { copyApk(ByteArrayInputStream(bytes), ByteArrayOutputStream(), size) }.isFailure).isTrue()
        }
    }

    @Test fun cancelledTransferStopsBeforeNextChunk() = runTest {
        val output = ByteArrayOutputStream()
        var wasCancelled = false
        launch {
            val job = currentCoroutineContext().job
            try {
                copyApk(ByteArrayInputStream(ByteArray(130_000)), output, 130_000) { job.cancel() }
            } catch (_: CancellationException) {
                wasCancelled = true
            }
        }.join()
        assertThat(wasCancelled).isTrue()
        assertThat(output.size()).isEqualTo(64 * 1024)
    }

    @Test fun localServerAuthorizesStreamsAndShutsDown() = runBlocking {
        val source = folder.newFile("installed.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val destination = folder.newFile("received.apk")
        val port = ServerSocket(0).use { it.localPort }
        val address = PeerAddress("127.0.0.1", port)
        val packageFile = InstalledPeerPackage(source, newer, standalone = true)
        withContext(Dispatchers.IO) {
            PeerTransfer().use { transport ->
                sharePeerPackage(packageFile, address).use { sharing ->
                    val metadata = transport.manifest(address)
                    assertThat(status(metadata)).isEqualTo(PeerCompatibility.NEWER)
                    val wrong = if (sharing.code == "f".repeat(24)) "0".repeat(24) else "f".repeat(24)
                    assertThat(runCatching { transport.download(address, wrong, metadata, destination) {} }.isFailure).isTrue()
                    assertThat(destination.length()).isEqualTo(0)
                    val digest = transport.download(address, sharing.code, metadata, destination) {}
                    assertThat(digest).isEqualTo(metadata.sha256)
                    assertThat(destination.readBytes()).isEqualTo(source.readBytes())
                    source.writeBytes(byteArrayOf(1, 2, 4))
                    val damagedDigest = transport.download(address, sharing.code, metadata, destination) {}
                    assertThat(damagedDigest).isNotEqualTo(metadata.sha256)
                }
                assertThat(runCatching { transport.manifest(address) }.isFailure).isTrue()
                val unsupported = runCatching { sharePeerPackage(packageFile.copy(standalone = false), address) }
                assertThat(unsupported.isFailure).isTrue()
            }
        }
    }


    @Test fun interruptedAndRejectedDownloadsLeaveNoPartialFile() = runTest {
        for (failure in listOf(java.io.IOException("Disconnected"), CancellationException(), IllegalArgumentException("SHA-256 mismatch"))) {
            val directory = folder.newFolder()
            val result = runCatching {
                withVerifiedDownload(directory) { file ->
                    file.writeText("partial APK")
                    throw failure
                }
            }
            assertThat(result.exceptionOrNull()).isSameInstanceAs(failure)
            assertThat(directory.list().orEmpty().toList()).isEmpty()
        }
    }

}
