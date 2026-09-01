package com.romankozak.forwardappmobile.shared.core.domain.workspace

data class DirectionCapabilityConfigurationV1(
    val autoLinkChildWorkspaces: Boolean,
)

object DirectionCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String =
        encode(DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = true))

    override fun validate(
        version: Int,
        raw: String,
    ) {
        decode(version, raw)
    }

    fun encode(configuration: DirectionCapabilityConfigurationV1): String =
        "{\"autoLinkChildWorkspaces\":${configuration.autoLinkChildWorkspaces}}"

    fun decode(
        version: Int,
        raw: String,
    ): DirectionCapabilityConfigurationV1 {
        require(version == CURRENT_VERSION) {
            "Unsupported DIRECTION configuration version: $version"
        }
        val match = V1_PATTERN.matchEntire(raw.trim())
        require(match != null) { "Invalid DIRECTION configuration v1" }
        return DirectionCapabilityConfigurationV1(
            autoLinkChildWorkspaces = match.groupValues[1].toBooleanStrict(),
        )
    }

    private val V1_PATTERN =
        Regex("""\{\s*"autoLinkChildWorkspaces"\s*:\s*(true|false)\s*\}""")
}

enum class LegacyDirectionRowKind {
    SEMANTIC_DIRECTION,
    LINKED_ENTRY_REQUIRES_REVIEW,
}

/**
 * A linked legacy row may be an auto-generated shortcut or a semantic
 * Direction that the user linked later. Persistence has no provenance field,
 * so migration must not guess between those meanings. Blank historical
 * targets are treated as absent until a real Workspace endpoint is present.
 */
fun classifyLegacyDirectionRow(linkedContextId: String?): LegacyDirectionRowKind =
    if (linkedContextId.isNullOrBlank()) {
        LegacyDirectionRowKind.SEMANTIC_DIRECTION
    } else {
        LegacyDirectionRowKind.LINKED_ENTRY_REQUIRES_REVIEW
    }
