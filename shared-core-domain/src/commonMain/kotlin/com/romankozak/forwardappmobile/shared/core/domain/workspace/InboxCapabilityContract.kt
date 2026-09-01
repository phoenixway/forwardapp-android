package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.domain.inbox.inboxOwnerVisible
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceInboxRecord

enum class InboxOwnerVisibility {
    KEEP_VISIBLE,
    HIDE_WHEN_ASSOCIATED,
}

data class InboxCapabilityConfigurationV1(
    val ownerVisibility: InboxOwnerVisibility,
)

object InboxCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1
    private val pattern = Regex("""\{\s*"ownerVisibility"\s*:\s*"([A-Z_]+)"\s*\}""")

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String = encode(InboxCapabilityConfigurationV1(InboxOwnerVisibility.KEEP_VISIBLE))

    override fun validate(
        version: Int,
        raw: String,
    ) {
        decode(version, raw)
    }

    fun encode(configuration: InboxCapabilityConfigurationV1): String =
        "{\"ownerVisibility\":\"${configuration.ownerVisibility.name}\"}"

    fun decode(
        version: Int,
        raw: String,
    ): InboxCapabilityConfigurationV1 {
        require(version == CURRENT_VERSION) {
            "Unsupported INBOX configuration version: $version"
        }
        val match = pattern.matchEntire(raw.trim())
        require(match != null) { "Invalid INBOX configuration v1" }
        val visibility =
            InboxOwnerVisibility.entries.firstOrNull { it.name == match.groupValues[1] }
        require(visibility != null) { "Unknown INBOX owner visibility" }
        return InboxCapabilityConfigurationV1(visibility)
    }
}

fun inboxOwnerVisible(
    configuration: InboxCapabilityConfigurationV1,
    hasForeignAssociation: Boolean,
): Boolean =
    inboxOwnerVisible(
        removeAfterTagAutocopy = configuration.ownerVisibility == InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED,
        hasForeignAssociation = hasForeignAssociation,
    )

data class InboxContractViolation(
    val path: String,
    val code: String,
    val message: String,
)

fun validateInboxContract(records: List<WorkspaceInboxRecord>): List<InboxContractViolation> {
    val violations = mutableListOf<InboxContractViolation>()

    records.groupBy { it.id }.filterValues { it.size > 1 }.forEach { (id, _) ->
        violations += InboxContractViolation("inbox.$id", "DUPLICATE_ID", "Inbox identity must be unique")
    }

    records.filterNot { it.isDeleted }
        .groupBy { it.capabilityInstanceId }
        .forEach { (capabilityId, owned) ->
            owned.groupBy { it.order }.filterValues { it.size > 1 }.forEach { (order, _) ->
                violations +=
                    InboxContractViolation(
                        "inbox.$capabilityId.$order",
                        "DUPLICATE_ORDER",
                        "Live Inbox records must have unique order within a capability instance",
                    )
            }
        }

    records.filterNot { it.isDeleted }.forEach { record ->
        if (record.workspaceId.isBlank() || record.capabilityInstanceId.isBlank()) {
            violations +=
                InboxContractViolation(
                    "inbox.${record.id}.owner",
                    "INVALID_OWNER",
                    "Inbox record requires Workspace and capability-instance ownership",
                )
        }
        if (record.order < 0L) {
            violations +=
                InboxContractViolation(
                    "inbox.${record.id}.order",
                    "NEGATIVE_ORDER",
                    "Canonical Inbox order must not be negative",
                )
        }
    }
    return violations
}
