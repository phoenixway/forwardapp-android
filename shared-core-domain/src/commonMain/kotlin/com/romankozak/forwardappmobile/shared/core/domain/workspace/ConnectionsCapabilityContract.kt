@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceConnection
import kotlin.js.JsExport

/** CONNECTIONS v1 intentionally owns no configurable fields. */
data object ConnectionsCapabilityConfigurationV1

object ConnectionsCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String = "{}"

    override fun validate(
        version: Int,
        raw: String,
    ) {
        require(version == CURRENT_VERSION) {
            "Unsupported CONNECTIONS configuration version: $version"
        }
        require(raw.trim() == "{}") {
            "Invalid CONNECTIONS configuration v1"
        }
    }

    fun encode(
        configuration: ConnectionsCapabilityConfigurationV1 =
            ConnectionsCapabilityConfigurationV1,
    ): String = encodeDefault()

    fun decode(
        version: Int,
        raw: String,
    ): ConnectionsCapabilityConfigurationV1 {
        validate(version, raw)
        return ConnectionsCapabilityConfigurationV1
    }
}

/** Kotlin/JS boundary: CONNECTIONS configuration semantics remain shared-owned. */
@JsExport
fun validateConnectionsCapabilityConfigurationWire(
    version: Int,
    raw: String,
): Array<String> =
    runCatching {
        ConnectionsCapabilityConfigurationCodec.validate(version, raw)
    }.exceptionOrNull()?.message?.let(::arrayOf) ?: emptyArray()

data class ConnectionsContractViolation(
    val path: String,
    val code: String,
    val message: String,
)

/**
 * Whole-contract validation for canonical CONNECTIONS placement state.
 *
 * Attachment existence/deletion is an external-domain invariant and is
 * therefore enforced by migration/runtime boundaries, not by this pure
 * placement-only validator.
 */
fun validateConnectionsContract(
    connections: List<WorkspaceConnection>,
): List<ConnectionsContractViolation> {
    val violations = mutableListOf<ConnectionsContractViolation>()

    connections.groupingBy { it.id }
        .eachCount()
        .filterValues { it > 1 }
        .forEach { (id, _) ->
            violations +=
                violation(
                    path = "connections.$id",
                    code = "DUPLICATE_ID",
                    message = "Canonical connection identity must be unique",
                )
        }

    connections.forEach { connection ->
        if (connection.workspaceId.isBlank()) {
            violations +=
                violation(
                    path = "connections.${connection.id}.workspaceId",
                    code = "BLANK_WORKSPACE",
                    message = "Connection requires Workspace ownership",
                )
        }
        if (connection.capabilityInstanceId.isBlank()) {
            violations +=
                violation(
                    path = "connections.${connection.id}.capabilityInstanceId",
                    code = "BLANK_CAPABILITY",
                    message = "Connection requires capability-instance ownership",
                )
        }
        if (connection.attachmentId.isBlank()) {
            violations +=
                violation(
                    path = "connections.${connection.id}.attachmentId",
                    code = "BLANK_ATTACHMENT",
                    message = "Connection requires an Attachment target",
                )
        }
        if (connection.version < 0L) {
            violations +=
                violation(
                    path = "connections.${connection.id}.version",
                    code = "INVALID_VERSION",
                    message = "Connection version must not be negative",
                )
        }
        if (connection.createdAt < 0L || connection.updatedAt < 0L) {
            violations +=
                violation(
                    path = "connections.${connection.id}.timestamps",
                    code = "INVALID_TIMESTAMP",
                    message = "Connection timestamps must not be negative",
                )
        }
    }

    val live = connections.filterNot { it.isDeleted }

    live.filter { it.order < 0L }
        .forEach { connection ->
            violations +=
                violation(
                    path = "connections.${connection.id}.order",
                    code = "NEGATIVE_ORDER",
                    message = "Live connection order must not be negative",
                )
        }

    live.groupBy { it.capabilityInstanceId }
        .forEach { (capabilityId, owned) ->
            owned.groupBy { it.order }
                .filterValues { it.size > 1 }
                .forEach { (order, _) ->
                    violations +=
                        violation(
                            path = "connections.$capabilityId.$order",
                            code = "DUPLICATE_ORDER",
                            message = "Live connections must have unique order within a capability instance",
                        )
                }
        }

    connections
        .groupBy { it.capabilityInstanceId to it.attachmentId }
        .filterValues { it.size > 1 }
        .forEach { (logicalKey, _) ->
            violations +=
                violation(
                    path = "connections.$logicalKey",
                    code = "DUPLICATE_ATTACHMENT_PLACEMENT",
                    message = "One Attachment may appear only once in one CONNECTIONS capability instance",
                )
        }

    return violations
}

private fun violation(
    path: String,
    code: String,
    message: String,
) = ConnectionsContractViolation(path, code, message)
