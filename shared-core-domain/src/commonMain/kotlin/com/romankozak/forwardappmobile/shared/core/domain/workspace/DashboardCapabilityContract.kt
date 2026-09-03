@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.workspace

import kotlin.js.JsExport

/**
 * DASHBOARD capability configuration v1.
 *
 * DASHBOARD v1 intentionally owns no configurable fields. The typed value
 * still establishes an explicit capability-owned version/codec boundary.
 */
data object DashboardCapabilityConfigurationV1

object DashboardCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String = encode()

    override fun validate(
        version: Int,
        raw: String,
    ) {
        decode(version, raw)
    }

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

/** Kotlin/JS adapter for the typed DASHBOARD capability configuration contract. */
@JsExport
fun validateDashboardCapabilityConfigurationWire(
    version: Int,
    raw: String,
): Array<String> =
    try {
        DashboardCapabilityConfigurationCodec.validate(version, raw)
        emptyArray()
    } catch (error: IllegalArgumentException) {
        arrayOf(error.message ?: "Invalid DASHBOARD configuration")
    }
