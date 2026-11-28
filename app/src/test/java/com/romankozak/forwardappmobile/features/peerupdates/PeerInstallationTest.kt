package com.romankozak.forwardappmobile.features.peerupdates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PeerInstallationTest {
    @Test fun missingInstallPermissionPreventsOpeningASession() = runTest {
        val context = mockk<Context>()
        val manager = mockk<PackageManager>()
        every { context.packageManager } returns manager
        every { manager.canRequestPackageInstalls() } returns false
        val manifest = PeerManifest(1, "test.app", "2", 2, "prod:release", 1, "", emptyList(), 29, "Peer", emptyList())
        val result = runCatching { PeerInstallation(context).stage(File("unused.apk"), manifest) }
        assertThat(result.exceptionOrNull()?.message).contains("permission")
    }

    @Test fun reportsAndroidRefusalAndDoesNotClaimSuccessFromAStaleCallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("peer-install", Context.MODE_PRIVATE)
        prefs.edit().clear().putInt("session", 7).putLong("expected-version", 60).commit()
        val receiver = PeerInstallReceiver()
        receiver.onReceive(context, Intent().putExtra("session", 8)
            .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_SUCCESS))
        assertThat(PeerInstallation(context).awaitingConfirmation()).isTrue()
        assertThat(PeerInstallation(context).outcome(59)).isNull()

        receiver.onReceive(context, Intent().putExtra("session", 7)
            .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE_ABORTED))
        assertThat(PeerInstallation(context).awaitingConfirmation()).isFalse()
        assertThat(PeerInstallation(context).outcome(59)).isEqualTo("Installation declined")
    }

    @Test fun checksInstalledVersionWhenOpeningAfterProcessReplacement() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("peer-install", Context.MODE_PRIVATE)
        prefs.edit().clear().putInt("session", 7).putLong("expected-version", 60).commit()
        assertThat(PeerInstallation(context).outcome(59)).isNull()
        assertThat(PeerInstallation(context).outcome(60)).isEqualTo("Updated to version 60")
        assertThat(PeerInstallation(context).awaitingConfirmation()).isFalse()
    }
}
