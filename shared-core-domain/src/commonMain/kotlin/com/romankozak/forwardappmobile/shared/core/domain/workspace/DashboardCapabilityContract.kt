package com.romankozak.forwardappmobile.shared.core.domain.workspace

/**
 * DASHBOARD capability configuration v1.
 *
 * DASHBOARD v1 intentionally owns no configurable fields. The typed value
 * still establishes an explicit capability-owned version/codec boundary.
 */
data object DashboardCapabilityConfigurationV1

object DashboardCapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1

    fun encode(
        configuration: DashboardCapabilityConfigurationV1 =
            DashboardCapabilityConfigurationV1,
    ): String = "{}"

    fun decode(
        version: Int,
        raw: String,
    ): DashboardCapabilityConfigurationV1 {
        require(version == CURRENT_VERSION) {
            "Unsupported DASHBOARD configuration version: $version"
        }
        require(raw.trim() == "{}") {
            "Invalid DASHBOARD configuration v1"
        }
        return DashboardCapabilityConfigurationV1
    }
}
