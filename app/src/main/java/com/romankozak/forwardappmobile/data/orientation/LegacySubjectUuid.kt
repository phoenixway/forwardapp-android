package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/** Stable RFC 4122 UUIDv5 identity for legacy Orientation sources. */
object LegacySubjectUuid : LegacySubjectIdResolver {
    private val namespace = UUID.fromString(NAMESPACE_UUID)

    override fun resolve(source: LegacySubjectRef): String =
        uuidV5(namespace, "${source.sourceType.name}:${source.sourceId}").toString()

    internal fun uuidV5(namespace: UUID, name: String): UUID {
        val namespaceBytes =
            ByteBuffer.allocate(16)
                .putLong(namespace.mostSignificantBits)
                .putLong(namespace.leastSignificantBits)
                .array()
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(namespaceBytes)
        val hash = digest.digest(name.toByteArray(StandardCharsets.UTF_8))
        hash[6] = ((hash[6].toInt() and 0x0f) or 0x50).toByte()
        hash[8] = ((hash[8].toInt() and 0x3f) or 0x80).toByte()
        val bytes = ByteBuffer.wrap(hash)
        return UUID(bytes.long, bytes.long)
    }

    /** Immutable namespace; changing it would break durable legacy mappings. */
    const val NAMESPACE_UUID: String = "1ae36c1a-cb9d-5e7c-8b3a-3bca70de4830"
}
