package com.romankozak.forwardappmobile.features.peerupdates

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

private const val PREFS = "peer-install"
private const val VERSION = "expected-version"
private const val SESSION = "session"
private const val RESULT = "result"

internal data class PeerInstallPrompt(val sessionId: Int, val intent: Intent)
internal object PeerInstallEvents {
    val prompt = MutableStateFlow<PeerInstallPrompt?>(null)
    val revision = MutableStateFlow(0L)
}

internal class PeerInstallation(private val context: Context) {
    private val installer get() = context.packageManager.packageInstaller
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun permitted(): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun stage(file: File, manifest: PeerManifest) {
        check(permitted()) { "Android permission to install apps is required" }
        check(prefs.getInt(SESSION, -1) < 0) { "An installation is already pending" }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(file.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        }
        val id = installer.createSession(params)
        try {
            installer.openSession(id).use { session ->
                session.openWrite("base.apk", 0, file.length()).use { output ->
                    file.inputStream().use { input ->
                        check(copyApk(input, output, manifest.size) == manifest.sha256) { "APK changed before installation" }
                    }
                    session.fsync(output)
                }
                currentCoroutineContext().ensureActive()
                check(prefs.edit().putInt(SESSION, id).putLong(VERSION, manifest.versionCode).remove(RESULT).commit())
                val callback = Intent(context, PeerInstallReceiver::class.java).putExtra(SESSION, id)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                session.commit(PendingIntent.getBroadcast(context, id, callback, flags).intentSender)
            }
        } catch (error: Throwable) {
            runCatching { installer.abandonSession(id) }
            prefs.edit().remove(SESSION).remove(VERSION).apply()
            throw error
        }
    }

    fun cancel() {
        val id = prefs.getInt(SESSION, -1)
        if (id < 0) return
        prefs.edit().remove(SESSION).remove(VERSION).putString(RESULT, "Installation cancelled").apply()
        PeerInstallEvents.prompt.value = null
        runCatching { installer.abandonSession(id) }
    }

    fun outcome(installedVersion: Long): String? {
        val expected = prefs.getLong(VERSION, -1)
        if (expected > 0 && installedVersion >= expected) {
            prefs.edit().remove(SESSION).remove(VERSION).putString(RESULT, "Updated to version $installedVersion").apply()
        }
        return prefs.getString(RESULT, null)
    }

    fun awaitingConfirmation(): Boolean = prefs.getInt(SESSION, -1) >= 0
}

class PeerInstallReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = intent.getIntExtra(SESSION, -1)
        if (id < 0 || id != prefs.getInt(SESSION, -1)) return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val prompt = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (prompt != null) {
                    PeerInstallEvents.prompt.value = PeerInstallPrompt(id, prompt)
                } else {
                    PeerInstallation(context).cancel()
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // The installed version is checked on next entry, including after process replacement.
                prefs.edit().remove(SESSION).apply()
                PeerInstallEvents.prompt.value = null
            }
            else -> {
                val message = if (status == PackageInstaller.STATUS_FAILURE_ABORTED) "Installation declined"
                else "Android rejected the installation (status $status)"
                prefs.edit().remove(SESSION).remove(VERSION).putString(RESULT, message).apply()
                PeerInstallEvents.prompt.value = null
            }
        }
        PeerInstallEvents.revision.value += 1
    }
}
