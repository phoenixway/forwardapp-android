package com.romankozak.forwardappmobile.shared.core.domain.workspace

/**
 * EXECUTION_LOG capability configuration v1.
 *
 * v1 intentionally owns no configurable fields. Legacy Context.contextLogLevel
 * is not imported because repository evidence does not establish it as current
 * runtime/configuration authority.
 */
data object ExecutionLogCapabilityConfigurationV1

object ExecutionLogCapabilityConfigurationCodec : CapabilityConfigurationCodec {
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
        configuration: ExecutionLogCapabilityConfigurationV1 =
            ExecutionLogCapabilityConfigurationV1,
    ): String = "{}"

    fun decode(
        version: Int,
        raw: String,
    ): ExecutionLogCapabilityConfigurationV1 {
        require(version == CURRENT_VERSION) {
            "Unsupported EXECUTION_LOG configuration version: $version"
        }
        require(raw.trim() == "{}") {
            "Invalid EXECUTION_LOG configuration v1"
        }
        return ExecutionLogCapabilityConfigurationV1
    }
}
