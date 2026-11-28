package com.romankozak.forwardappmobile.features.peerupdates

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO as ServerCio
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

internal const val PEER_PORT = 8765
internal const val PEER_SERVICE = "_fa-update._tcp."

internal data class PeerAddress(val host: String, val port: Int) {
    override fun toString(): String = "$host:$port"

    companion object {
        fun parse(text: String): PeerAddress {
            val raw = text.trim().split(':')
            require(raw.size in 1..2 && raw[0].matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))) { "Enter a local IPv4 address and port" }
            val host = InetAddress.getByName(raw[0])
            require(host is Inet4Address && (host.isSiteLocalAddress || host.isLinkLocalAddress)) { "Enter a private LAN address" }
            val port = if (raw.size == 2) raw[1].toIntOrNull() else PEER_PORT
            require(port != null && port in 1..65535) { "Invalid port" }
            return PeerAddress(host.hostAddress!!, port)
        }
    }
}

internal class PeerTransfer : Closeable {
    private val gson = Gson()
    private val client = HttpClient(CIO) {
        followRedirects = false
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
        }
    }

    suspend fun manifest(address: PeerAddress): PeerManifest = withTimeout(8_000) {
        client.prepareGet("http://$address/v1/package").execute {
            check(it.status == HttpStatusCode.OK) { "Peer unavailable" }
            val bytes = it.bodyAsChannel().toInputStream().use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(4096)
                while (true) {
                    val count = input.read(chunk)
                    if (count < 0) break
                    check(buffer.size() + count <= 8192) { "Peer metadata is too large" }
                    buffer.write(chunk, 0, count)
                }
                buffer.toByteArray()
            }
            requireNotNull(gson.fromJson(bytes.toString(Charsets.UTF_8), PeerManifest::class.java))
        }
    }

    suspend fun download(address: PeerAddress, code: String, manifest: PeerManifest, file: File, progress: (Long) -> Unit): String {
        require(code.matches(Regex("[0-9a-f]{24}"))) { "Enter the share code shown on the other device" }
        return client.prepareGet("http://$address/v1/apk/$code").execute {
            check(it.status != HttpStatusCode.Forbidden) { "The share code is incorrect or has expired" }
            check(it.status == HttpStatusCode.OK) { "The peer is unavailable or busy" }
            val length = it.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            check(length == null || length == manifest.size) { "APK size changed" }
            it.bodyAsChannel().toInputStream().use { input ->
                file.outputStream().use { output -> copyApk(input, output, manifest.size, progress) }
            }
        }
    }

    override fun close() = client.close()
}

internal class PeerShare(val address: PeerAddress, val code: String, private val engine: ApplicationEngine) : Closeable {
    override fun close() { engine.stop(0, 500) }
}

internal suspend fun sharePeerPackage(pkg: InstalledPeerPackage, address: PeerAddress): PeerShare {
    check(pkg.standalone) { "Split APK installations cannot be shared" }
    val hash = pkg.file.inputStream().use { copyApk(it, object : OutputStream() {
        override fun write(value: Int) = Unit
        override fun write(bytes: ByteArray, offset: Int, length: Int) = Unit
    }, pkg.manifest.size) }
    val manifest = pkg.manifest.copy(sha256 = hash)
    check(manifest.valid()) { "Installed APK metadata is incomplete" }
    val code = ByteArray(12).apply { SecureRandom().nextBytes(this) }.hex()
    val sending = AtomicBoolean(false)
    val metadata = Gson().toJson(manifest)
    val engine = embeddedServer(ServerCio, host = address.host, port = address.port) {
        routing {
            get("/v1/package") { call.respondText(metadata, ContentType.Application.Json) }
            get("/v1/apk/{code}") {
                val supplied = call.parameters["code"].orEmpty()
                if (!MessageDigest.isEqual(code.toByteArray(), supplied.toByteArray())) {
                    call.respond(HttpStatusCode.Forbidden)
                } else if (!sending.compareAndSet(false, true)) {
                    call.respond(HttpStatusCode.ServiceUnavailable)
                } else {
                    try {
                        call.response.header(HttpHeaders.CacheControl, "no-store")
                        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=ForwardApp.apk")
                        call.respondFile(pkg.file)
                    } finally {
                        sending.set(false)
                    }
                }
            }
        }
    }
    return try {
        engine.start(wait = false)
        PeerShare(address, code, engine)
    } catch (error: Throwable) {
        engine.stop(0, 500)
        throw error
    }
}
