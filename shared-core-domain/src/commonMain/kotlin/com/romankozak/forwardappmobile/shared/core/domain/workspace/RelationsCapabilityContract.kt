package com.romankozak.forwardappmobile.shared.core.domain.workspace

/** Read/navigation presentation only; no owned relation data or configuration fields in V1. */
object RelationsCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    override val currentVersion: Int = 1
    override fun encodeDefault(): String = "{}"
    override fun validate(version: Int, raw: String) {
        require(version == currentVersion) { "Unsupported RELATIONS configuration version: $version" }
        require(raw.trim() == "{}") { "Invalid RELATIONS configuration" }
    }
}
