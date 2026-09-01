package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogEntry

/** BACKLOG v1 intentionally owns no configurable fields. */
data object BacklogCapabilityConfigurationV1

object BacklogCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String = "{}"

    override fun validate(
        version: Int,
        raw: String,
    ) {
        require(version == CURRENT_VERSION) {
            "Unsupported BACKLOG configuration version: $version"
        }
        require(raw.trim() == "{}") { "Invalid BACKLOG configuration v1" }
    }

    fun encode(
        configuration: BacklogCapabilityConfigurationV1 =
            BacklogCapabilityConfigurationV1,
    ): String = encodeDefault()

    fun decode(
        version: Int,
        raw: String,
    ): BacklogCapabilityConfigurationV1 {
        validate(version, raw)
        return BacklogCapabilityConfigurationV1
    }
}

data class BacklogContractViolation(
    val path: String,
    val code: String,
    val message: String,
)

/**
 * Pure whole-collection validation.
 *
 * Target existence and target lifecycle belong to the typed repository and
 * migration boundary because target content is external to BACKLOG.
 */
fun validateBacklogContract(
    entries: List<WorkspaceBacklogEntry>,
): List<BacklogContractViolation> {
    val violations = mutableListOf<BacklogContractViolation>()

    entries.groupBy { it.id }
        .filterValues { it.size > 1 }
        .forEach { (id, _) ->
            violations += violation("backlog.$id", "DUPLICATE_ID", "Backlog entry identity must be unique")
        }

    entries.forEach { entry ->
        if (entry.workspaceId.isBlank()) {
            violations += violation(entry.path("workspaceId"), "BLANK_WORKSPACE", "Workspace ownership is required")
        }
        if (entry.capabilityInstanceId.isBlank()) {
            violations += violation(entry.path("capabilityInstanceId"), "BLANK_CAPABILITY", "Capability ownership is required")
        }
        if (entry.target.id.isBlank()) {
            violations += violation(entry.path("target.id"), "BLANK_TARGET", "Backlog target identity is required")
        }
        if (entry.version < 0L) {
            violations += violation(entry.path("version"), "INVALID_VERSION", "Version must not be negative")
        }
        if (entry.createdAt < 0L || entry.updatedAt < 0L) {
            violations += violation(entry.path("timestamps"), "INVALID_TIMESTAMP", "Timestamps must not be negative")
        }
    }

    entries.groupBy { it.capabilityInstanceId }
        .filterValues { owned -> owned.map { it.workspaceId }.distinct().size > 1 }
        .forEach { (capabilityId, _) ->
            violations +=
                violation(
                    "backlog.$capabilityId.owner",
                    "CAPABILITY_OWNER_MISMATCH",
                    "One capability instance cannot belong to several Workspaces",
                )
        }

    val live = entries.filterNot { it.isDeleted }
    live.filter { it.order < 0L }.forEach { entry ->
        violations += violation(entry.path("order"), "NEGATIVE_ORDER", "Live Backlog order must not be negative")
    }

    live.groupBy { it.capabilityInstanceId }.forEach { (capabilityId, owned) ->
        owned.groupBy { it.order }.filterValues { it.size > 1 }.forEach { (order, _) ->
            violations +=
                violation(
                    "backlog.$capabilityId.$order",
                    "DUPLICATE_ORDER",
                    "Live entries must have unique order within a capability instance",
                )
        }
        owned.groupBy { it.target }.filterValues { it.size > 1 }.forEach { (target, _) ->
            violations +=
                violation(
                    "backlog.$capabilityId.${target.kind}.${target.id}",
                    "DUPLICATE_LIVE_TARGET",
                    "One target may have only one live explicit placement in a capability instance",
                )
        }
    }

    return violations
}

private fun WorkspaceBacklogEntry.path(field: String): String = "backlog.$id.$field"

private fun violation(
    path: String,
    code: String,
    message: String,
) = BacklogContractViolation(path, code, message)
