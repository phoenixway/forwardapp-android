package com.romankozak.forwardappmobile.features.peerupdates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

internal const val APK_MIME = "application/vnd.android.package-archive"
internal const val MAX_APK_BYTES = 1_073_741_824L
private const val VARIANT_METADATA = "forwardapp.peer.variant"

internal data class PeerManifest(
    @SerializedName("protocol") val protocol: Int,
    @SerializedName("applicationId") val applicationId: String,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("versionCode") val versionCode: Long,
    @SerializedName("variant") val variant: String,
    @SerializedName("size") val size: Long,
    @SerializedName("sha256") val sha256: String,
    @SerializedName("certificates") val certificates: List<String>,
    @SerializedName("minSdk") val minSdk: Int,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("abis") val abis: List<String>,
) {
    fun valid(): Boolean = protocol == 1 && size in 1..MAX_APK_BYTES && versionCode > 0 &&
        minSdk > 0 && HEX.matches(sha256) && certificates.isNotEmpty() && certificates.size <= 16 &&
        certificates.distinct() == certificates && certificates.all(HEX::matches) &&
        applicationId.length in 1..255 && deviceName.length in 1..80 && versionName.length in 1..80 &&
        variant.length in 1..40 && abis.size <= 16 && abis.all { it.length in 1..32 }

    private companion object {
        val HEX = Regex("[0-9a-f]{64}")
    }
}

internal enum class PeerCompatibility(val label: String) {
    NEWER("Newer version available"),
    SAME("Same version"),
    OLDER("Older version"),
    INCOMPATIBLE("Incompatible build"),
    UNREACHABLE("Unreachable"),
}

internal fun compatibility(local: PeerManifest, peer: PeerManifest, sdk: Int, abis: List<String>): PeerCompatibility = when {
    !peer.valid() || peer.applicationId != local.applicationId || peer.variant != local.variant ||
        peer.certificates.toSet() != local.certificates.toSet() || peer.minSdk > sdk ||
        (peer.abis.isNotEmpty() && peer.abis.none { it in abis }) -> PeerCompatibility.INCOMPATIBLE
    peer.versionCode > local.versionCode -> PeerCompatibility.NEWER
    peer.versionCode == local.versionCode -> PeerCompatibility.SAME
    else -> PeerCompatibility.OLDER
}

internal data class InstalledPeerPackage(val file: File, val manifest: PeerManifest, val standalone: Boolean)

internal class PeerPackages(private val context: Context) {
    private val manager = context.packageManager
    private val flags = PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA
    private val installed get() = manager.getPackageInfo(context.packageName, flags)

    fun own(): InstalledPeerPackage {
        val pkg = installed
        val app = requireNotNull(pkg.applicationInfo)
        val file = File(app.sourceDir)
        return InstalledPeerPackage(file, describe(pkg, file, ""), app.splitSourceDirs.isNullOrEmpty())
    }

    fun status(local: PeerManifest, peer: PeerManifest): PeerCompatibility =
        compatibility(local, peer, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.toList())

    fun verify(file: File, advertisement: PeerManifest, actualDigest: String) {
        check(file.length() == advertisement.size && actualDigest == advertisement.sha256) { "APK integrity check failed" }
        val pkg = requireNotNull(manager.getPackageArchiveInfo(file.absolutePath, flags)) { "Invalid or damaged APK" }
        check(pkg.splitNames.isNullOrEmpty()) { "Split APK updates are unsupported" }
        val actual = describe(pkg, file, actualDigest).copy(deviceName = advertisement.deviceName)
        check(actual == advertisement) { "APK differs from the advertised package" }
        check(status(own().manifest, actual) == PeerCompatibility.NEWER) { "APK is no longer a compatible update" }
    }

    private fun describe(pkg: PackageInfo, file: File, hash: String): PeerManifest {
        val app = requireNotNull(pkg.applicationInfo)
        val signers = requireNotNull(pkg.signingInfo?.apkContentsSigners).map {
            MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex()
        }.distinct().sorted()
        val abis = ZipFile(file).use { zip ->
            zip.entries().asSequence().map { it.name }.filter { it.startsWith("lib/") && it.endsWith(".so") }
                .map { it.substringAfter('/').substringBefore('/') }.distinct().sorted().toList()
        }
        return PeerManifest(1, pkg.packageName, pkg.versionName.orEmpty(), pkg.longVersionCode,
            app.metaData?.getString(VARIANT_METADATA).orEmpty(), file.length(), hash, signers,
            app.minSdkVersion, Build.MODEL.take(80).ifBlank { "Android" }, abis)
    }
}

internal suspend fun copyApk(
    input: InputStream,
    output: OutputStream,
    size: Long,
    progress: (Long) -> Unit = {},
): String {
    val sha = MessageDigest.getInstance("SHA-256")
    val bytes = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(bytes)
        if (count < 0) break
        total += count
        check(total <= size) { "APK exceeds the advertised size" }
        sha.update(bytes, 0, count)
        output.write(bytes, 0, count)
        progress(total)
    }
    check(total == size) { "APK transfer was incomplete" }
    return sha.digest().hex()
}

internal fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

class PeerApkFileProvider : androidx.core.content.FileProvider()
