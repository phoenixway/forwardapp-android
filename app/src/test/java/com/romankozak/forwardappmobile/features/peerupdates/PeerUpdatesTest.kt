package com.romankozak.forwardappmobile.features.peerupdates

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class PeerUpdatesTest {
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
}
